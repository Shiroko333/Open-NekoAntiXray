package sakuyaayane.nekoantixray.detection;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.WitherSkull;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.bukkit.World;
import sakuyaayane.nekoantixray.config.ConfigManager;
import sakuyaayane.nekoantixray.detection.ai.OnnxInferenceEngine;
import sakuyaayane.nekoantixray.NekoAntiXray;
import sakuyaayane.nekoantixray.notification.DetectionNotificationManager;
import sakuyaayane.nekoantixray.replay.ReplayManager;

public class MiningBehaviorDetector {
   private final NekoAntiXray plugin;
   private final ConfigManager configManager;
   private final BanInventoryCleaner banInventoryCleaner;
   private final ReplayManager replayManager;
   private final Map<UUID, Map<String, List<Location>>> playerMiningPaths = new ConcurrentHashMap<>();
   private final Map<UUID, Long> lastMiningTime = new ConcurrentHashMap<>();
   private final Map<UUID, Integer> playerViolationLevels = new ConcurrentHashMap<>();
   private final Map<UUID, Long> lastViolationTimes = new ConcurrentHashMap<>();
   private final Map<UUID, Long> zeroViolationSinceTimes = new ConcurrentHashMap<>();
   private final Map<UUID, Integer> playerVeinCounts = new ConcurrentHashMap<>();
   private final Map<UUID, Map<String, Location>> lastVeinLocations = new ConcurrentHashMap<>();
   private final Map<UUID, Map<String, Set<Location>>> currentVeinClusters = new ConcurrentHashMap<>();
   private final Map<UUID, Map<Location, Long>> playerPlacedOres = new ConcurrentHashMap<>();
   private final Map<Location, Long> explodedOreExpirations = new ConcurrentHashMap<>();
   private final Map<UUID, Integer> fakeCaveTriggerCounts = new ConcurrentHashMap<>();
   private final Map<UUID, Long> lastFakeCaveTriggerTimes = new ConcurrentHashMap<>();
   private static final long DEFAULT_CLEANUP_INTERVAL_MS = 300000L;
   private boolean analyzerActive;
   private int pathMaxBlocks;
   private int veinClusterTriggerCount;
   private int veinClusterDistance;
   private int veinClusterSmallSize;
   private int turningLimit;
   private int branchingLimit;
   private int yShiftLimit;
   private double yShiftMin;
   private boolean applyEnvironmentFilters;
   private int caveAirMultiplier;
   private int caveAirCountThreshold;
   private int caveScanRadius;
   private int waterCountThreshold;
   private int lavaCountThreshold;
   private boolean includeFlowingWater;
   private boolean caveReducesVl;
   private boolean waterReducesVl;
   private boolean lavaReducesVl;
   private int fakeCaveMaxTriggers;
   private long cleanupAfterMinutes;
   private int explosionTrackingRetentionSeconds;
   private double explosionSuspiciousRate;
   private double explosionPenaltyMultiplier;

   public MiningBehaviorDetector(NekoAntiXray plugin) {
      this.plugin = plugin;
      this.configManager = plugin.getConfigManager();
      this.banInventoryCleaner = plugin.getBanInventoryCleaner();
      this.replayManager = plugin.getReplayManager();
      this.loadConfig();
      plugin.getFoliaScheduler().getImpl().runTimer(wrappedTask -> {
         this.cleanupZeroViolationPlayers();
         this.cleanupInactiveMiningData();
         this.cleanupExpiredExplosions();
         this.cleanupExpiredPlacedBlocks();
         this.cleanupExpiredFakeCaveTriggers();
         this.decayViolationLevels();
      }, 1200L, 1200L);
   }

   private boolean isFilteredByEnvironment(Player player, Location location) {
      if (!this.applyEnvironmentFilters) {
         return false;
      }

      int airCount = 0;
      int waterCount = 0;
      int lavaCount = 0;
      int scanRadius = this.caveScanRadius;
      int blockX = location.getBlockX();
      int blockY = location.getBlockY();
      int blockZ = location.getBlockZ();
      World world = location.getWorld();

      for (int dx = -scanRadius; dx <= scanRadius; dx++) {
         for (int dy = -scanRadius; dy <= scanRadius; dy++) {
            for (int dz = -scanRadius; dz <= scanRadius; dz++) {
               int targetX = blockX + dx;
               int targetZ = blockZ + dz;
               if (this.isBlockChunkLoaded(world, targetX, targetZ)) {
                  Material blockType = world.getBlockAt(targetX, blockY + dy, targetZ).getType();
                  switch (blockType) {
                     case CAVE_AIR:
                        airCount += this.caveAirMultiplier;
                        break;
                     case AIR:
                        airCount++;
                        break;
                     case WATER:
                        if (this.includeFlowingWater || this.isWaterSourceBlock(world, blockX + dx, blockY + dy, blockZ + dz)) {
                           waterCount++;
                        }
                        break;
                     case LAVA:
                        lavaCount++;
                  }
               }
            }
         }
      }

      if (airCount > this.caveAirCountThreshold && this.caveReducesVl && this.fakeCaveTriggerCounts.getOrDefault(player.getUniqueId(), 0) < this.fakeCaveMaxTriggers) {
         return true;
      } else {
         return waterCount > this.waterCountThreshold && this.waterReducesVl ? true : lavaCount > this.lavaCountThreshold && this.lavaReducesVl;
      }
   }

   private void cleanupExpiredExplosions() {
      long nowMs = System.currentTimeMillis();
      this.explodedOreExpirations.entrySet().removeIf(entry -> nowMs > entry.getValue());
   }

   public int getViolationLevel(UUID uuid) {
      return this.playerViolationLevels.getOrDefault(uuid, 0);
   }

   private static boolean isPlacedBlockExpired(long nowMs, long expirationMs, Entry entry) {
      return nowMs - (Long)entry.getValue() > expirationMs;
   }

   private static List createEmptyPathList() {
      return new ArrayList();
   }

   public void loadConfig() {
      this.analyzerActive = this.configManager.isCheckMiningBehaviorAnalyzerActive();
      this.pathMaxBlocks = this.configManager.getCheckMiningBehaviorAnalyzerDataCollectionPathMaxBlocks();
      this.veinClusterTriggerCount = this.configManager.getCheckMiningBehaviorAnalyzerDataCollectionVeinClusterTriggerCount();
      this.veinClusterDistance = this.configManager.getCheckMiningBehaviorAnalyzerDataCollectionVeinClusterDistance();
      this.veinClusterSmallSize = this.configManager.getCheckMiningBehaviorAnalyzerDataCollectionVeinClusterSmallSize();
      this.turningLimit = this.configManager.getCheckMiningBehaviorAnalyzerPatternAnalyzerTurningLimit();
      this.branchingLimit = this.configManager.getCheckMiningBehaviorAnalyzerPatternAnalyzerBranchingLimit();
      this.yShiftLimit = this.configManager.getCheckMiningBehaviorAnalyzerPatternAnalyzerYShiftLimit();
      this.yShiftMin = this.configManager.getCheckMiningBehaviorAnalyzerPatternAnalyzerYShiftMin();
      this.applyEnvironmentFilters = this.configManager.isCheckMiningBehaviorAnalyzerEnvironmentFiltersApplyFilters();
      this.caveAirMultiplier = this.configManager.getCheckMiningBehaviorAnalyzerEnvironmentFiltersCaveCaveAirMultiplier();
      this.caveAirCountThreshold = this.configManager.getCheckMiningBehaviorAnalyzerEnvironmentFiltersCaveAirCount();
      this.caveScanRadius = this.configManager.getCheckMiningBehaviorAnalyzerEnvironmentFiltersCaveRadius();
      this.waterCountThreshold = this.configManager.getCheckMiningBehaviorAnalyzerEnvironmentFiltersWaterWaterCount();
      this.lavaCountThreshold = this.configManager.getCheckMiningBehaviorAnalyzerEnvironmentFiltersLavaLavaCount();
      this.includeFlowingWater = this.configManager.isCheckMiningBehaviorAnalyzerEnvironmentFiltersWaterIncludeFlowing();
      this.caveReducesVl = this.configManager.isCheckMiningBehaviorAnalyzerEnvironmentFiltersCaveReduceVl();
      this.waterReducesVl = this.configManager.isCheckMiningBehaviorAnalyzerEnvironmentFiltersWaterReduceVl();
      this.lavaReducesVl = this.configManager.isCheckMiningBehaviorAnalyzerEnvironmentFiltersLavaReduceVl();
      this.fakeCaveMaxTriggers = this.configManager.getCheckMiningBehaviorAnalyzerEnvironmentFiltersCaveFakeCaveDetectionMaxTriggers();
      this.cleanupAfterMinutes = this.configManager.getCheckMiningBehaviorAnalyzerDataCollectionCleanupAfterMinutes();
      this.explosionTrackingRetentionSeconds = this.configManager.getCheckMiningBehaviorAnalyzerDataCollectionExplosionTrackingRetentionSeconds();
      this.explosionSuspiciousRate = this.configManager.getCheckMiningBehaviorAnalyzerDataCollectionExplosionTrackingSuspiciousRate();
      this.explosionPenaltyMultiplier = this.configManager.getCheckMiningBehaviorAnalyzerDataCollectionExplosionTrackingPenaltyMultiplier();
   }

   public void handleBlockBreak(Player player, Block block) {
      if (this.analyzerActive) {
         if (player != null && block != null) {
            try {
               boolean hasPermissionBypass = player.hasPermission("nekoantixray.bypass");
               boolean hasInternalBypass = this.plugin.getInternalBypassManager() != null && this.plugin.getInternalBypassManager().hasBypass(player);
               if (hasPermissionBypass || hasInternalBypass) {
                  return;
               }
            } catch (Throwable ignored) {
            }

            Material blockType = block.getType();
            Location blockLocation = block.getLocation();
            UUID playerUuid = player.getUniqueId();
            String worldName = blockLocation.getWorld().getName();
            if (this.configManager.isCheckMiningBehaviorAnalyzerWorldConfigsDefaultEnabled(worldName)) {
               int yLimit = this.configManager.getCheckMiningBehaviorAnalyzerWorldConfigsDefaultYLimit(worldName);
               if ((yLimit == -1 || !(blockLocation.getY() > yLimit)) && this.isTargetOreMaterial(blockType) && !this.isPlayerPlacedOre(blockLocation) && !this.isExplodedOre(blockLocation)) {
                  if (this.playerViolationLevels.getOrDefault(playerUuid, 0) == 0) {
                     this.zeroViolationSinceTimes.put(playerUuid, System.currentTimeMillis());
                  }

                  Map<String, List<Location>> worldPaths = this.playerMiningPaths.computeIfAbsent(playerUuid, ignoredUuid -> new ConcurrentHashMap<>());
                  List<Location> miningPath = worldPaths.computeIfAbsent(worldName, ignoredWorld -> createEmptyPathList());

                  try {
                     OnnxInferenceEngine onnxInferenceEngine = this.plugin.getOnnxInferenceEngine();
                     if (onnxInferenceEngine != null) {
                        onnxInferenceEngine.onPlayerBlockBreak(player, block);
                     }
                  } catch (Exception ex) {
                     this.plugin.getLogger().warning("Deep learning detection error: " + ex.getMessage());
                     if (this.plugin.getConfig().getBoolean("deeplearn.debug", false)) {
                        ex.printStackTrace();
                     }
                  }

                  miningPath.add(blockLocation);
                  this.lastMiningTime.put(playerUuid, System.currentTimeMillis());
                  if (miningPath.size() > this.pathMaxBlocks) {
                     miningPath.remove(0);
                  }

                  this.detectFakeCaveMining(player, miningPath);
                  if (!this.isFilteredByEnvironment(player, blockLocation) && !this.isWithinNormalPatternLimits(miningPath) && this.isNewOreVein(playerUuid, worldName, blockLocation, blockType)) {
                     this.playerVeinCounts.put(playerUuid, this.playerVeinCounts.getOrDefault(playerUuid, 0) + 1);
                     this.lastVeinLocations.computeIfAbsent(playerUuid, ignoredKey -> new ConcurrentHashMap<>()).put(worldName, blockLocation);
                     int veinCount = this.playerVeinCounts.getOrDefault(playerUuid, 0);
                     if (veinCount >= this.veinClusterTriggerCount) {
                        int veinSize = this.calculateVeinSize(blockLocation, blockType);
                        this.analyzeMiningPath(player, miningPath, blockType, veinSize, blockLocation);
                     }
                  }
               }
            }
         } else {
            this.plugin.logDebug("handleBlockBreak: null parameter detected");
         }
      }
   }

   public void handleBlockPlace(Player player, Block block) {
      UUID playerUuid = player.getUniqueId();
      Material blockType = block.getType();
      if (this.isTargetOreMaterial(blockType)) {
         this.playerPlacedOres.computeIfAbsent(playerUuid, ignoredKey -> new ConcurrentHashMap<>()).put(block.getLocation(), System.currentTimeMillis());
         String playerName = player.getName();
         this.plugin.logDebug("Player " + playerName + " placed ore: " + blockType + " at " + block.getLocation());
      }
   }

   public void handleExplosion(List blocks, Entity entity) {
      Player sourcePlayer = null;
      if (entity instanceof TNTPrimed tntPrimed) {
         if (tntPrimed.getSource() instanceof Player tntSource) {
            sourcePlayer = tntSource;
         }
      } else if (entity instanceof ExplosiveMinecart) {
         sourcePlayer = null;
      } else if (entity instanceof EnderCrystal || entity instanceof WitherSkull || entity instanceof Creeper) {
         return;
      }

      if (sourcePlayer != null) {
         sourcePlayer.getUniqueId();
         long nowMs = System.currentTimeMillis();
         int retentionMs = this.explosionTrackingRetentionSeconds * 1000;
         int totalBlocks = 0;
         int oreBlocks = 0;
         List<Block> explodedBlocks = blocks;

         for (Block block : explodedBlocks) {
            Location blockLocation = block.getLocation();
            if (!this.isPlayerPlacedOre(blockLocation)) {
               totalBlocks++;
               if (this.isTargetOreMaterial(block.getType())) {
                  oreBlocks++;
                  this.explodedOreExpirations.put(blockLocation, nowMs + retentionMs);
               }
            }
         }

         if (totalBlocks > 0) {
            double hitRate = (double)oreBlocks / totalBlocks;
            if (hitRate > this.explosionSuspiciousRate) {
               this.recordSuspiciousExplosion(sourcePlayer, oreBlocks, hitRate);
            }
         }
      }
   }

   private void recordSuspiciousExplosion(Player player, int oreCount, double hitRate) {
      UUID playerUuid = player.getUniqueId();
      int previousVl = this.playerViolationLevels.getOrDefault(playerUuid, 0);
      int vlIncrease = (int)Math.ceil(oreCount * hitRate * this.explosionPenaltyMultiplier);
      int updatedVl = previousVl + vlIncrease;
      this.playerViolationLevels.put(playerUuid, updatedVl);
      this.lastViolationTimes.put(playerUuid, System.currentTimeMillis());
      this.zeroViolationSinceTimes.remove(playerUuid);
      String playerName = player.getName();
      String formattedHitRate = String.format("%.2f%%", hitRate * 100.0);
      this.plugin.logDebug(playerName + "'s explosion suspicious! Ores: " + oreCount + ", Hit rate: " + formattedHitRate + " (VL +" + vlIncrease + ")");
      this.checkViolationActions(player, updatedVl);
   }

   private boolean isPlayerPlacedOre(Location location) {
      long nowMs = System.currentTimeMillis();
      long expirationMs = this.cleanupAfterMinutes * 60L * 1000L;

      for (Entry<UUID, Map<Location, Long>> entry : this.playerPlacedOres.entrySet()) {
         Map<Location, Long> placedLocations = entry.getValue();
         Long placedTimestamp = placedLocations.get(location);
         if (placedTimestamp != null) {
            if (nowMs - placedTimestamp <= expirationMs) {
               return true;
            }

            placedLocations.remove(location);
         }
      }

      return false;
   }

   private boolean isExplodedOre(Location location) {
      Long expirationTime = this.explodedOreExpirations.get(location);
      if (expirationTime != null) {
         if (System.currentTimeMillis() < expirationTime) {
            return true;
         }

         this.explodedOreExpirations.remove(location);
      }

      return false;
   }

   private Set findConnectedVeinBlocks(Location origin, Material material, int maxDistance) {
      HashSet<Location> visited = new HashSet<>();
      LinkedList<Location> queue = new LinkedList<>();
      if (origin == null) {
         return visited;
      }

      int searchRadius = Math.max(1, maxDistance);
      double maxDistanceSquared = (double)searchRadius * searchRadius;
      World world = origin.getWorld();
      if (this.isBlockChunkLoaded(world, origin.getBlockX(), origin.getBlockZ()) && origin.getBlock().getType().equals(material)) {
         queue.add(origin);
      } else {
         int blockX = origin.getBlockX();
         int blockY = origin.getBlockY();
         int blockZ = origin.getBlockZ();

         for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dy = -searchRadius; dy <= searchRadius; dy++) {
               for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                  int candidateX = blockX + dx;
                  int candidateZ = blockZ + dz;
                  if (this.isBlockChunkLoaded(world, candidateX, candidateZ)) {
                     Location candidate = new Location(world, candidateX, blockY + dy, candidateZ);
                     if (candidate.getWorld().equals(origin.getWorld())) {
                        double distanceSquared = candidate.distanceSquared(origin);
                        if (distanceSquared <= maxDistanceSquared && candidate.getBlock().getType().equals(material)) {
                           queue.add(candidate);
                        }
                     }
                  }
               }
            }
         }

         if (queue.isEmpty()) {
            return visited;
         }

         visited.add(origin);
      }

      short maxVisitedBlocks = 2000;

      while (!queue.isEmpty() && visited.size() < maxVisitedBlocks) {
         Location current = queue.poll();
         if (!visited.contains(current) && this.isBlockChunkLoaded(world, current.getBlockX(), current.getBlockZ()) && current.getBlock().getType().equals(material)) {
            visited.add(current);

            for (int offsetX = -1; offsetX <= 1; offsetX++) {
               for (int offsetY = -1; offsetY <= 1; offsetY++) {
                  for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                     if (offsetX != 0 || offsetY != 0 || offsetZ != 0) {
                        Location neighbor = current.clone().add(offsetX, offsetY, offsetZ);
                        if (!visited.contains(neighbor)
                           && this.isBlockChunkLoaded(world, neighbor.getBlockX(), neighbor.getBlockZ())
                           && neighbor.getBlock().getType().equals(material)
                           && neighbor.getWorld().equals(origin.getWorld())
                           && neighbor.distanceSquared(origin) <= maxDistanceSquared) {
                           queue.add(neighbor);
                        }
                     }
                  }
               }
            }
         }
      }

      return visited;
   }

   private boolean isBlockChunkLoaded(World world, int blockX, int blockZ) {
      return world.isChunkLoaded(blockX >> 4, blockZ >> 4);
   }

   public int calculateVeinSize(Location location, Material material) {
      int searchDistance = Math.max(1, this.veinClusterDistance);
      Set connectedBlocks = this.findConnectedVeinBlocks(location, material, searchDistance);
      return connectedBlocks.size();
   }

   private boolean isWithinNormalPatternLimits(List miningPath) {
      if (miningPath.size() < 2) {
         return true;
      }

      int turnCount = 0;
      int branchCount = 0;
      int yShiftCount = 0;
      Location prevLocation = null;
      Vector prevDirection = null;

      for (int i = 0; i < miningPath.size(); i++) {
         Location currentLocation = (Location)miningPath.get(i);
         if (prevLocation != null) {
            Vector currentDirection = currentLocation.toVector().subtract(prevLocation.toVector()).normalize();
            if (prevDirection != null) {
               double dotProduct = prevDirection.dot(currentDirection);
               if (dotProduct < Math.cos(Math.toRadians(30.0))) {
                  turnCount++;
               }
            }

            if (Math.abs(currentLocation.getY() - prevLocation.getY()) > this.yShiftMin) {
               yShiftCount++;
            }

            if (i > 1) {
               Location priorLocation = (Location)miningPath.get(i - 1);
               Vector priorDirection = priorLocation.toVector().subtract(prevLocation.toVector()).normalize();
               if (priorDirection.lengthSquared() > 0.001 && currentDirection.lengthSquared() > 0.001) {
                  double branchAngle = Math.acos(Math.max(-1.0, Math.min(1.0, currentDirection.dot(priorDirection))));
                  if (branchAngle > Math.toRadians(60.0)) {
                     branchCount++;
                  }
               }
            }

            prevDirection = currentDirection;
         }

         prevLocation = currentLocation;
      }

      return turnCount < this.turningLimit && branchCount < this.branchingLimit && yShiftCount < this.yShiftLimit;
   }

   private boolean isWaterSourceBlock(World world, int blockX, int blockY, int blockZ) {
      Block blockAt = world.getBlockAt(blockX, blockY, blockZ);
      return blockAt.getType() != Material.WATER ? false : blockAt.getBlockData() instanceof Levelled && ((Levelled)blockAt.getBlockData()).getLevel() == 0;
   }

   private void detectFakeCaveMining(Player player, List miningPath) {
      if (this.configManager.isCheckMiningBehaviorAnalyzerEnvironmentFiltersCaveFakeCaveDetectionEnabled()) {
         int minBlocks = this.configManager.getCheckMiningBehaviorAnalyzerEnvironmentFiltersCaveFakeCaveDetectionMinBlocks();
         if (miningPath.size() >= minBlocks) {
            int airBlockCount = 0;
            int loadedBlockCount = 0;
            List<Location> pathLocations = miningPath;

            for (Location location : pathLocations) {
               World world = location.getWorld();
               if (this.isBlockChunkLoaded(world, location.getBlockX(), location.getBlockZ())) {
                  loadedBlockCount++;
                  Material blockType = location.getBlock().getType();
                  if (blockType == Material.AIR || blockType == Material.CAVE_AIR) {
                     airBlockCount++;
                  }
               }
            }

            if (loadedBlockCount >= minBlocks) {
               double airRatio = (double)airBlockCount / loadedBlockCount;
               double maxAirRatio = this.configManager.getCheckMiningBehaviorAnalyzerEnvironmentFiltersCaveFakeCaveDetectionAirRatio();
               if (airRatio > maxAirRatio) {
                  UUID playerUuid = player.getUniqueId();
                  int vlPerTrigger = this.configManager.getCheckMiningBehaviorAnalyzerEnvironmentFiltersCaveFakeCaveDetectionVlPerTrigger();
                  this.fakeCaveTriggerCounts.put(playerUuid, this.fakeCaveTriggerCounts.getOrDefault(playerUuid, 0) + vlPerTrigger);
                  this.lastFakeCaveTriggerTimes.put(playerUuid, System.currentTimeMillis());
               }
            }
         }
      }
   }

   private void analyzeMiningPath(Player player, List miningPath, Material material, int veinSize, Location location) {
      UUID playerUuid = player.getUniqueId();
      if (player == null || miningPath == null || material == null || location == null) {
         this.plugin.logDebug("analyzeMiningPath: null parameter detected");
      } else if (!miningPath.isEmpty()) {
         Location previousLocation = null;
         List<Location> pathLocations = miningPath;

         for (Location pathLocation : pathLocations) {
            if (pathLocation != null) {
               if (previousLocation != null) {
                  double distanceSquared = pathLocation.distanceSquared(previousLocation);
                  if (distanceSquared > 9.0) {
                  }
               }

               previousLocation = pathLocation;
            }
         }

         int veinsMined = this.playerVeinCounts.getOrDefault(playerUuid, 0);
         this.addMiningViolation(player, 1, material.name(), veinSize, veinsMined, location);
      }
   }

   private void addMiningViolation(Player player, int vlIncrease, String oreName, int veinSize, int veinsMined, Location location) {
      UUID playerUuid = player.getUniqueId();
      int previousVl = this.playerViolationLevels.getOrDefault(playerUuid, 0);
      int updatedVl = previousVl + vlIncrease;
      this.playerViolationLevels.put(playerUuid, updatedVl);
      this.lastViolationTimes.put(playerUuid, System.currentTimeMillis());
      this.zeroViolationSinceTimes.remove(playerUuid);
      String playerName = player.getName();
      int blockX = location.getBlockX();
      int blockY = location.getBlockY();
      int blockZ = location.getBlockZ();
      String debugMessage = "Player "
         + playerName
         + " detected possible XRay! Ore: "
         + oreName
         + ", Vein size: "
         + veinSize
         + ", Veins mined: "
         + veinsMined
         + ", VL increase: "
         + vlIncrease
         + ", Current VL: "
         + updatedVl
         + ", Position: "
         + blockX
         + ","
         + blockY
         + ","
         + blockZ;
      this.plugin.logDebug(debugMessage);

      try {
         DetectionNotificationManager detectionNotificationManager = this.plugin.getDetectionNotificationManager();
         String detail = oreName + " x" + veinSize;
         detectionNotificationManager.notifyDetection(player, "algo", "Algo", updatedVl, detail);
      } catch (Throwable ignored) {
      }

      this.checkViolationActions(player, updatedVl);
   }

   public void addDeepLearningViolation(Player player, float probability) {
      int vlIncrease = (int)Math.ceil((probability - 0.5) * 10.0);
      if (vlIncrease < 1) {
         vlIncrease = 1;
      }

      UUID playerUuid = player.getUniqueId();
      int previousVl = this.playerViolationLevels.getOrDefault(playerUuid, 0);
      int updatedVl = previousVl + vlIncrease;
      this.playerViolationLevels.put(playerUuid, updatedVl);
      this.lastViolationTimes.put(playerUuid, System.currentTimeMillis());
      String reason = "AI detected suspicious behavior, probability: " + String.format("%.2f", probability);
      this.plugin.logDetectionEvent(player, "DeepLearning", updatedVl, reason);
      this.plugin.logDebug(String.format("Player %s VL increased by AI %d -> %d (probability: %.2f)", player.getName(), previousVl, updatedVl, probability));
      this.checkViolationActions(player, updatedVl);
   }

   private void checkViolationActions(Player player, int currentVl) {
      UUID playerUuid = player.getUniqueId();
      int previousVl = currentVl - this.playerViolationLevels.getOrDefault(playerUuid, 0) + this.playerViolationLevels.getOrDefault(playerUuid, 0) - 1;
      ConfigurationSection configurationSection = this.configManager.getAlgoThresholdCommandsSection();
      if (configurationSection != null) {
         for (String thresholdKey : configurationSection.getKeys(false)) {
            try {
               int thresholdVl = Integer.parseInt(thresholdKey);
               if (thresholdVl > previousVl && thresholdVl <= currentVl) {
                  Object rawCommands = configurationSection.get(thresholdKey);
                  ArrayList<String> commands = new ArrayList<>();
                  if (rawCommands instanceof List) {
                     for (Object commandEntry : (List)rawCommands) {
                        if (commandEntry != null) {
                           commands.add(commandEntry.toString());
                        }
                     }
                  } else if (rawCommands != null) {
                     commands.add(rawCommands.toString());
                  }

                  for (String commandTemplate : commands) {
                     if (commandTemplate != null && !commandTemplate.trim().isEmpty()) {
                        String resolvedCommand = commandTemplate.replace("%player%", player.getName());
                        this.plugin.getFoliaScheduler().getImpl().runNextTick(wrappedTask -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolvedCommand));
                        this.plugin.logDebug("Executed command at VL " + thresholdVl + ": " + resolvedCommand);
                     }
                  }
               }
            } catch (NumberFormatException ignored) {
            }
         }
      }
   }

   private void decayViolationLevels() {
      long nowMs = System.currentTimeMillis();
      long decayIntervalMs = this.configManager.getCheckMiningBehaviorAnalyzerViolationSystemAutoDecayIntervalMinutes() * 60 * 1000L;

      for (UUID playerUuid : new HashSet<>(this.playerViolationLevels.keySet())) {
         int currentVl = this.playerViolationLevels.getOrDefault(playerUuid, 0);
         if (currentVl > 0) {
            Long lastViolationMs = this.lastViolationTimes.get(playerUuid);
            if (lastViolationMs != null && nowMs - lastViolationMs > decayIntervalMs) {
               int decayAmount = this.configManager.getCheckMiningBehaviorAnalyzerViolationSystemAutoDecayAmount();
               double exponentialFactor = this.configManager.getCheckMiningBehaviorAnalyzerViolationSystemExponentialDecayFactor();
               boolean useExponentialDecay = this.configManager.isCheckMiningBehaviorAnalyzerViolationSystemExponentialDecayEnabled();
               int decayedVl = useExponentialDecay ? (int)Math.ceil(currentVl * exponentialFactor) : Math.max(0, currentVl - decayAmount);
               this.playerViolationLevels.put(playerUuid, decayedVl);
               this.lastViolationTimes.put(playerUuid, nowMs);
               if (decayedVl == 0) {
                  this.zeroViolationSinceTimes.put(playerUuid, nowMs);
               }
            }
         } else {
            this.zeroViolationSinceTimes.putIfAbsent(playerUuid, nowMs);
         }
      }
   }

   private void cleanupInactiveMiningData() {
      long nowMs = System.currentTimeMillis();
      long inactivityThresholdMs = this.cleanupAfterMinutes * 60L * 1000L;
      HashSet<UUID> expiredPathPlayers = new HashSet<>();

      for (Entry<UUID, Map<String, List<Location>>> entry : this.playerMiningPaths.entrySet()) {
         UUID playerUuid = entry.getKey();
         long lastMinedMs = this.lastMiningTime.getOrDefault(playerUuid, 0L);
         if (lastMinedMs > 0L && nowMs - lastMinedMs > inactivityThresholdMs) {
            expiredPathPlayers.add(playerUuid);
         }
      }

      for (UUID playerUuid : expiredPathPlayers) {
         this.playerMiningPaths.remove(playerUuid);
         this.lastMiningTime.remove(playerUuid);
      }

      HashSet<UUID> expiredVeinPlayers = new HashSet<>();

      for (Entry<UUID, Map<String, Set<Location>>> entry : this.currentVeinClusters.entrySet()) {
         UUID playerUuid = entry.getKey();
         long lastMinedMs = this.lastMiningTime.getOrDefault(playerUuid, 0L);
         if (lastMinedMs > 0L && nowMs - lastMinedMs > inactivityThresholdMs) {
            expiredVeinPlayers.add(playerUuid);
         }
      }

      for (UUID playerUuid : expiredVeinPlayers) {
         this.currentVeinClusters.remove(playerUuid);
         this.lastVeinLocations.remove(playerUuid);
         this.playerVeinCounts.remove(playerUuid);
      }
   }

   private void cleanupExpiredPlacedBlocks() {
      long nowMs = System.currentTimeMillis();
      long expirationMs = this.cleanupAfterMinutes * 60L * 1000L;
      ArrayList<UUID> emptyPlayers = new ArrayList<>();

      for (Entry<UUID, Map<Location, Long>> entry : this.playerPlacedOres.entrySet()) {
         UUID playerUuid = entry.getKey();
         Map<Location, Long> placedLocations = entry.getValue();
         placedLocations.entrySet().removeIf(placedEntry -> isPlacedBlockExpired(nowMs, expirationMs, placedEntry));
         if (placedLocations.isEmpty()) {
            emptyPlayers.add(playerUuid);
         }
      }

      for (UUID playerUuid : emptyPlayers) {
         this.playerPlacedOres.remove(playerUuid);
      }
   }

   private void cleanupExpiredFakeCaveTriggers() {
      long nowMs = System.currentTimeMillis();
      long resetIntervalMs = this.configManager.getCheckMiningBehaviorAnalyzerEnvironmentFiltersCaveFakeCaveDetectionResetMinutes() * 60L * 1000L;
      ArrayList<UUID> expiredPlayers = new ArrayList<>();

      for (Entry<UUID, Long> entry : this.lastFakeCaveTriggerTimes.entrySet()) {
         if (nowMs - entry.getValue() > resetIntervalMs) {
            expiredPlayers.add(entry.getKey());
         }
      }

      for (UUID playerUuid : expiredPlayers) {
         this.fakeCaveTriggerCounts.remove(playerUuid);
         this.lastFakeCaveTriggerTimes.remove(playerUuid);
      }
   }

   private void cleanupZeroViolationPlayers() {
      long nowMs = System.currentTimeMillis();
      long expirationMs = this.cleanupAfterMinutes * 60L * 1000L;

      for (UUID playerUuid : new HashSet<>(this.zeroViolationSinceTimes.keySet())) {
         Long zeroSinceMs = this.zeroViolationSinceTimes.get(playerUuid);
         int currentVl = this.playerViolationLevels.getOrDefault(playerUuid, 0);
         if (zeroSinceMs != null && currentVl == 0 && nowMs - zeroSinceMs > expirationMs) {
            this.playerMiningPaths.remove(playerUuid);
            this.playerVeinCounts.remove(playerUuid);
            this.zeroViolationSinceTimes.remove(playerUuid);
         }
      }
   }

   private boolean isTargetOreMaterial(Material material) {
      try {
         List targetBlocks = this.configManager.getCheckMiningBehaviorAnalyzerTargetBlocks();
         if (targetBlocks != null && !targetBlocks.isEmpty()) {
            return targetBlocks.contains(material.name());
         }
      } catch (Throwable ignored) {
      }

      try {
         List<String> valuableMaterials = this.plugin.getConfig().getStringList("deeplearn.valuable_materials");
         if (valuableMaterials != null && !valuableMaterials.isEmpty()) {
            return valuableMaterials.contains(material.name());
         }
      } catch (Throwable ignored) {
      }

      return OreMaterialMatcher.isOreMaterial(material);
   }

   public List getPlayerMiningPath(UUID playerUuid, String worldName) {
      return this.playerMiningPaths.getOrDefault(playerUuid, new HashMap<>()).get(worldName);
   }

   public Location getLastMinedLocation(UUID playerUuid, String worldName) {
      Map<String, List<Location>> worldPaths = this.playerMiningPaths.get(playerUuid);
      if (worldPaths == null) {
         return null;
      }

      List<Location> miningPath = worldPaths.get(worldName);
      return miningPath != null && !miningPath.isEmpty() ? miningPath.get(miningPath.size() - 1) : null;
   }

   private boolean isNewOreVein(UUID playerUuid, String worldName, Location location, Material material) {
      Map<String, Location> playerLastVeins = this.lastVeinLocations.getOrDefault(playerUuid, new ConcurrentHashMap<>());
      Location lastVeinLocation = playerLastVeins.get(worldName);
      int clusterDistance = this.veinClusterDistance;
      double clusterDistanceSquared = (double)clusterDistance * clusterDistance;
      int smallVeinSize = this.veinClusterSmallSize;
      Set<Location> connectedBlocks = this.findConnectedVeinBlocks(location, material, clusterDistance);
      this.currentVeinClusters.putIfAbsent(playerUuid, new ConcurrentHashMap<>());
      Map<String, Set<Location>> playerClusters = this.currentVeinClusters.get(playerUuid);
      Set<Location> existingCluster = playerClusters.get(worldName);
      if (existingCluster != null && !existingCluster.isEmpty()) {
         if (lastVeinLocation != null
            && lastVeinLocation.getBlockX() == location.getBlockX()
            && lastVeinLocation.getBlockY() == location.getBlockY()
            && lastVeinLocation.getBlockZ() == location.getBlockZ()
            && lastVeinLocation.getWorld().equals(location.getWorld())) {
            return false;
         }

         for (Location blockLoc : connectedBlocks) {
            if (existingCluster.contains(blockLoc)) {
               existingCluster.addAll(connectedBlocks);
               playerClusters.put(worldName, existingCluster);
               return false;
            }
         }

         double minDistanceSquared = Double.MAX_VALUE;

         for (Location newBlockLoc : connectedBlocks) {
            for (Location existingBlockLoc : existingCluster) {
               double distanceSquared = newBlockLoc.distanceSquared(existingBlockLoc);
               if (distanceSquared < minDistanceSquared) {
                  minDistanceSquared = distanceSquared;
               }
            }
         }

         if (minDistanceSquared <= clusterDistanceSquared) {
            existingCluster.addAll(connectedBlocks);
            playerClusters.put(worldName, existingCluster);
            return false;
         } else {
            int newClusterSize = connectedBlocks.size();
            int existingClusterSize = existingCluster.size();
            if (newClusterSize > 0 && newClusterSize <= smallVeinSize && existingClusterSize > 0 && existingClusterSize <= smallVeinSize) {
               playerClusters.put(worldName, new HashSet<>(connectedBlocks));
               playerLastVeins.put(worldName, location);
               this.lastVeinLocations.put(playerUuid, playerLastVeins);
               return true;
            } else if (minDistanceSquared > clusterDistanceSquared) {
               playerClusters.put(worldName, new HashSet<>(connectedBlocks));
               playerLastVeins.put(worldName, location);
               this.lastVeinLocations.put(playerUuid, playerLastVeins);
               return true;
            } else {
               return false;
            }
         }
      } else {
         playerClusters.put(worldName, new HashSet<>(connectedBlocks));
         playerLastVeins.put(worldName, location);
         this.lastVeinLocations.put(playerUuid, playerLastVeins);
         return true;
      }
   }

   public static final class MaterialSwitchMap {
      static final int[] MATERIAL_ORDINALS = new int[Material.values().length];

      static {
         try {
            MATERIAL_ORDINALS[Material.CAVE_AIR.ordinal()] = 1;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINALS[Material.AIR.ordinal()] = 2;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINALS[Material.WATER.ordinal()] = 3;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINALS[Material.LAVA.ordinal()] = 4;
         } catch (NoSuchFieldError ignored) {
         }
      }
   }
}
