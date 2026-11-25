package sakuyaayane.nekoantixray;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import sakuyaayane.nekoantixray.lib.folialib.FoliaLib;

public class XrayDetector {
   private final Map<UUID, List<Location>> oreMap = new ConcurrentHashMap<>();
   private final Set<Location> playerPlacedOres = ConcurrentHashMap.newKeySet();
   private final Map<UUID, Deque<Location>> blockBreak = new ConcurrentHashMap<>();
   private final Map<UUID, Double> playerViolations = new ConcurrentHashMap<>();
   private final XrayDetector.ViolationTracker violationTracker = new XrayDetector.ViolationTracker();
   private final Map<Location, Material> fakeOreTypes = new ConcurrentHashMap<>();
   private final Map<Location, UUID> decoyOreOwners = new ConcurrentHashMap<>();
   private final Map<UUID, XrayDetector.PendingDecoyRequest> pendingDecoyRequests = new ConcurrentHashMap<>();
   private final Map<UUID, Integer> playerMiningCount = new ConcurrentHashMap<>();
   private final Map<UUID, LocalDateTime> lastMiningTime = new ConcurrentHashMap<>();
   private final Map<UUID, LocalDateTime> lastViolationTime = new ConcurrentHashMap<>();
   private final Map<UUID, LocalDateTime> lastFakeOreGenerationTime = new ConcurrentHashMap<>();
   private final Map<UUID, Integer> fakeOreBreakCount = new ConcurrentHashMap<>();
   private final Map<UUID, LocalDateTime> lastFakeOreBreakTime = new ConcurrentHashMap<>();
   private final ExecutorService executorService = Executors.newFixedThreadPool(4);
   private final int miningCountThreshold;
   private final int lineBreakLength;
   private final long fakeOreGenerationCooldown;
   private final NekoAntiXray plugin;
   private final Map<UUID, Deque<Location>> recentlyMinedBlocks = new ConcurrentHashMap<>();
   private final long recentlyMinedExpirationMillis = 300000L;
   private final Map<UUID, Chunk> lastMinedChunk = new ConcurrentHashMap<>();
   private final FoliaLib foliaLib;
   private final VerticalDistributionCheck verticalDistributionCheck;
   private final ExplosionTracker explosionTracker;
   private final ExplosionAttributionListener explosionAttributionListener;
   private final InternalBypassManager internalBypassManager;
   private ConfigManager configManager;

   public XrayDetector() {
      this.plugin = NekoAntiXray.getInstance();
      this.foliaLib = new FoliaLib(this.plugin);
      this.configManager = this.plugin.getConfigManager();
      this.miningCountThreshold = this.plugin.getConfigManager().getCheckXrayMiningCountThreshold();
      this.lineBreakLength = this.plugin.getConfigManager().getCheckXrayLineBreakLength();
      this.fakeOreGenerationCooldown = this.plugin.getConfigManager().getFakeOreGenerationCooldown();
      this.verticalDistributionCheck = new VerticalDistributionCheck(this.plugin, this.foliaLib);
      this.explosionAttributionListener = this.plugin.getExplosionAttributionListener();
      this.explosionTracker = this.explosionAttributionListener.getExplosionTracker();
      this.internalBypassManager = new InternalBypassManager(this.plugin, this.foliaLib);
   }

   private void generateFakeOresAsync(Player player, Block block, int clusterCount) {
      Location originLocation = block.getLocation();
      List<CompletableFuture<XrayDetector.OreCluster>> clusterFutures = new ArrayList<>();
      AtomicInteger successCounter = new AtomicInteger(0);
      this.plugin.logDebug(String.format("开始异步生成假矿石 - 玩家: %s, 计划生成数量: %d", player.getName(), clusterCount));

      for (int i = 0; i < clusterCount; i++) {
         Material fakeOreMaterial = this.selectFakeOreMaterial(block);
         CompletableFuture<XrayDetector.OreCluster> clusterFuture = new CompletableFuture<>();
         clusterFutures.add(clusterFuture);
         this.foliaLib
            .getImpl()
            .runAtLocation(
               originLocation,
               wrappedTask -> {
                  try {
                     int checkXraySpawnXChangeMin = this.plugin.getConfigManager().getCheckXraySpawnXChangeMin();
                     int checkXraySpawnXChangeMax = this.plugin.getConfigManager().getCheckXraySpawnXChangeMax();
                     int checkXraySpawnZChangeMin = this.plugin.getConfigManager().getCheckXraySpawnZChangeMin();
                     int checkXraySpawnZChangeMax = this.plugin.getConfigManager().getCheckXraySpawnZChangeMax();
                     int checkXraySpawnYChangeMin = this.plugin.getConfigManager().getCheckXraySpawnYChangeMin();
                     int checkXraySpawnYChangeMax = this.plugin.getConfigManager().getCheckXraySpawnYChangeMax();
                     Location candidateLocation = FakeOreProtectionListener.generateRandomOffsetLocation(originLocation, checkXraySpawnXChangeMin, checkXraySpawnXChangeMax, checkXraySpawnZChangeMin, checkXraySpawnZChangeMax, checkXraySpawnYChangeMin, checkXraySpawnYChangeMax);
                     this.isValidFakeOreLocation(candidateLocation, player, fakeOreMaterial)
                        .thenAccept(isValid -> this.onFakeOreLocationValidated(candidateLocation, fakeOreMaterial, player, successCounter, clusterFuture, (Boolean)isValid));
                  } catch (Exception ex) {
                     this.plugin.getLogger().warning("生成群集时发生错误: " + ex.getMessage());
                     clusterFuture.complete(new XrayDetector.OreCluster(Collections.emptyList(), fakeOreMaterial));
                  }
               }
            );
      }

      CompletableFuture.allOf(clusterFutures.toArray(new CompletableFuture[0]))
         .thenAccept(ignored -> this.onAllFakeOreClustersGenerated(clusterFutures, successCounter, clusterCount, originLocation, player));
   }

   public Map getFakeOreTypes() {
      return this.fakeOreTypes;
   }

   public InternalBypassManager getInternalBypassManager() {
      return this.internalBypassManager;
   }

   private void logReplacedClusterMaterialCount(Material material, Integer count) {
      this.plugin.logDebug(String.format("- %s: %d", material, count));
   }

   private CompletionStage buildAncientDebrisClusterFromStartLocation(int attemptIndex, int totalAttempts, Player player, Material material, Location startLocation) {
      if (startLocation == null) {
         this.plugin.logDebug(String.format("尝试 [%d/%d]: 未能找到有效的集群起始位置", attemptIndex, totalAttempts));
         return CompletableFuture.completedFuture(Collections.emptyList());
      } else {
         CompletableFuture clusterFuture = new CompletableFuture();
         this.foliaLib.getImpl().runAtLocation(startLocation, wrappedTask -> {
            try {
               List clusterLocations = this.buildAncientDebrisClusterSync(player, startLocation, material);
               clusterFuture.complete(clusterLocations);
            } catch (Exception ex) {
               this.plugin.getLogger().warning(String.format("尝试 [%d/%d]: 构建集群时发生错误: %s", attemptIndex, totalAttempts, ex.getMessage()));
               clusterFuture.complete(Collections.emptyList());
            }
         });
         return clusterFuture;
      }
   }

   private List collectValidAncientDebrisClusters(List clusterFutures) {
      return (List)clusterFutures.stream()
         .map(clusterFuture -> this.resolveAncientDebrisClusterFuture((CompletableFuture)clusterFuture))
         .filter(cluster -> isNonEmptyClusterList((List)cluster))
         .collect(Collectors.toList());
   }

   private static boolean isNonEmptyClusterList(List clusterLocations) {
      return !clusterLocations.isEmpty();
   }

   private List resolveAncientDebrisClusterFuture(CompletableFuture clusterFuture) {
      try {
         return (List)clusterFuture.getNow(Collections.emptyList());
      } catch (Exception ex) {
         this.plugin.getLogger().warning("获取远古残骸集群结果时发生错误: " + ex.getMessage());
         return Collections.emptyList();
      }
   }

   private static List createLocationList() {
      return new ArrayList();
   }

   private void logReplacedCleanupMaterialCount(Material material, Integer count) {
      this.plugin.logDebug(String.format("- %s: %d", material, count));
   }

   private static Stream streamClusterLocationsForStats(XrayDetector.OreCluster oreCluster) {
      return oreCluster.locations.stream();
   }

   private static Stream streamClusterLocationsForRemoval(XrayDetector.OreCluster oreCluster) {
      return oreCluster.locations.stream();
   }

   private void onAllFakeOreClustersGenerated(List clusterFutures, AtomicInteger successCounter, int plannedCount, Location originLocation, Player player) {
      List validClusters = (List)clusterFutures.stream()
         .map(clusterFuture -> this.resolveOreClusterFuture((CompletableFuture)clusterFuture))
         .filter(cluster -> isNonEmptyOreCluster((XrayDetector.OreCluster)cluster))
         .collect(Collectors.toList());
      this.plugin.logDebug(String.format("群集生成完成 - 成功: %d/%d", successCounter.get(), plannedCount));
      if (!validClusters.isEmpty()) {
         this.foliaLib.getImpl().runAtLocation(originLocation, wrappedTask -> this.finalizeFakeOreGeneration(player, validClusters));
      } else {
         this.plugin.logDebug("没有生成有效的群集");
      }
   }

   private static boolean isNonEmptyOreCluster(XrayDetector.OreCluster oreCluster) {
      return !oreCluster.locations.isEmpty();
   }

   private XrayDetector.OreCluster resolveOreClusterFuture(CompletableFuture clusterFuture) {
      try {
         return (XrayDetector.OreCluster)clusterFuture.get(5L, TimeUnit.SECONDS);
      } catch (Exception ex) {
         this.plugin.getLogger().warning("获取群集结果时发生错误: " + ex.getMessage());
         return new XrayDetector.OreCluster(Collections.emptyList(), Material.STONE);
      }
   }

   private void onFakeOreLocationValidated(Location candidateLocation, Material material, Player player, AtomicInteger successCounter, CompletableFuture clusterFuture, Boolean isValid) {
      if (isValid) {
         this.generateOreCluster(candidateLocation, material, 3, 8, player)
            .thenAccept(cluster -> this.onOreClusterGenerated(candidateLocation, successCounter, clusterFuture, material, (XrayDetector.OreCluster)cluster));
      } else {
         this.plugin.logDebug("位置无效 - " + this.formatLocation(candidateLocation));
         clusterFuture.complete(new XrayDetector.OreCluster(Collections.emptyList(), material));
      }
   }

   private void onOreClusterGenerated(Location centerLocation, AtomicInteger successCounter, CompletableFuture clusterFuture, Material material, XrayDetector.OreCluster oreCluster) {
      if (!oreCluster.locations.isEmpty()) {
         this.plugin.logDebug(String.format("成功生成群集 - 中心位置: %s, 大小: %d, 类型: %s", this.formatLocation(centerLocation), oreCluster.locations.size(), oreCluster.type));
         successCounter.incrementAndGet();
         clusterFuture.complete(oreCluster);
      } else {
         this.plugin.logDebug("生成空群集 - 位置: " + this.formatLocation(centerLocation));
         clusterFuture.complete(new XrayDetector.OreCluster(Collections.emptyList(), material));
      }
   }

   private void onCandidateLocationChecked(
      Location candidateLocation, CompletableFuture locationFuture, AtomicInteger attemptCounter, Player player, Location originLocation, ConfigManager config, int maxAttempts, Material material, Boolean isValid
   ) {
      if (isValid) {
         this.plugin.logDebug("找到有效位置: " + this.formatLocation(candidateLocation));
         locationFuture.complete(candidateLocation);
      } else {
         attemptCounter.incrementAndGet();
         this.tryGenerateLocation(player, originLocation, config, locationFuture, attemptCounter, maxAttempts, material);
      }
   }

   private void onSingleFakeOreLocationsReady(List locationFutures, Block block, Player player, Material material) {
      List validLocations = (List)locationFutures.stream()
         .map(locationFuture -> this.resolveLocationFuture((CompletableFuture)locationFuture))
         .filter(location -> Objects.nonNull(location))
         .collect(Collectors.toList());
      if (!validLocations.isEmpty()) {
         this.foliaLib.getImpl().runAtLocation(block.getLocation(), wrappedTask -> this.finalizeSingleFakeOreGeneration(player, validLocations, material));
      } else {
         this.plugin.logDebug("没有生成有效的位置");
      }
   }

   private Location resolveLocationFuture(CompletableFuture locationFuture) {
      try {
         return (Location)locationFuture.get(5L, TimeUnit.SECONDS);
      } catch (Exception ex) {
         this.plugin.getLogger().warning("获取位置结果时发生错误: " + ex.getMessage());
         return null;
      }
   }

   public void checkXRay(Player player, Block block) {
      if (this.configManager.isCheckXrayEnable()) {
         if (this.hasDetectionBypass(player)) {
            this.plugin.logDebug("玩家 " + player.getName() + " 具有bypass权限,跳过检查");
         } else if (!this.isBlacklistedMaterial(block.getType()) && !this.isBlacklistedWorld(block.getWorld().getName())) {
            if (this.explosionTracker.isNearExplosion(player, block.getLocation())) {
               this.plugin.logDebug("玩家 " + player.getName() + " 破坏的矿物在TNT爆炸范围内,跳过检测");
            } else {
               String playerName = player.getName();
               String blockType = String.valueOf(block.getType());
               String blockLocation = String.valueOf(block.getLocation());
               this.plugin.logDebug("检查玩家 " + playerName + " 破坏方块 " + blockType + " 在位置 " + blockLocation);
               UUID playerId = player.getUniqueId();
               this.updateRecentlyMinedBlocks(playerId, block);
               boolean sameChunkDetectionEnabled = NekoAntiXray.getInstance().getConfigManager().isCheckXraySameChunkDetection();
               if (sameChunkDetectionEnabled) {
                  Chunk currentChunk = block.getChunk();
                  Chunk previousChunk = this.lastMinedChunk.get(playerId);
                  if (currentChunk.equals(previousChunk)) {
                     int chunkX = currentChunk.getX();
                     int chunkZ = currentChunk.getZ();
                     this.plugin.logDebug("同区块豁免: 玩家 " + player.getName() + " 仍在区块 " + chunkX + "," + chunkZ + " 内挖掘, 不生成假矿石(移动到其他区块即可触发)");
                     return;
                  }

                  this.lastMinedChunk.put(playerId, currentChunk);
               }

               if (FakeOreProtectionListener.isTrackedOre(block.getLocation())) {
                  this.plugin.logDebug("检测到玩家破坏的是玩家放置的矿物,跳过检测");
                  FakeOreProtectionListener.untrackOreLocation(block.getLocation());
               } else if (this.isLineBreaking(player, block)) {
                  this.plugin.logDebug("检测到连续挖矿行为");
               } else if (this.isFakeOre(block)) {
                  this.plugin.logDebug("检测到破坏假矿石");
                  this.handleFakeOreBreakInCheck(player, block);
               } else {
                  int minedCount = this.playerMiningCount.compute(playerId, (key, currentCount) -> currentCount == null ? 1 : currentCount + 1);
                  if (minedCount >= this.miningCountThreshold) {
                     this.generateFakeOresForPlayer(player, block);
                     this.playerMiningCount.put(playerId, 0);
                  }
               }
            }
         } else {
            this.plugin.logDebug("玩家 " + player.getName() + " 破坏的方块在黑名单中或在黑名单世界中,跳过检测");
         }
      }
   }

   private void updateRecentlyMinedBlocks(UUID playerId, Block block) {
      this.foliaLib.getImpl().runAtLocation(block.getLocation(), wrappedTask -> {
         Deque<Location> minedQueue = this.recentlyMinedBlocks.computeIfAbsent(playerId, key -> new LinkedList<>());
         long nowMs = System.currentTimeMillis();

         while (!minedQueue.isEmpty() && nowMs - minedQueue.peekFirst().getBlock().getMetadata("mineTime").get(0).asLong() > 300000L) {
            minedQueue.pollFirst();
         }

         block.setMetadata("mineTime", new FixedMetadataValue(this.plugin, nowMs));
         minedQueue.addLast(block.getLocation());
      });
   }

   public boolean isFakeOre(Block block) {
      if (block == null) {
         this.plugin.logDebug("isFakeOre: 方块为null");
         return false;
      }

      Location blockLocation = block.getLocation();
      boolean fakeOre = this.fakeOreTypes.containsKey(blockLocation);
      if (!fakeOre) {
         fakeOre = this.fakeOreTypes.containsKey(new Location(blockLocation.getWorld(), blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockZ()));
      }

      if (fakeOre) {
         this.plugin.logDebug("假矿石数据:");
         this.plugin.logDebug("存储的类型: " + this.fakeOreTypes.get(blockLocation));
         this.plugin.logDebug("当前fakeOreTypes大小: " + this.fakeOreTypes.size());
      }

      return fakeOre;
   }

   public void addPlayerPlacedOre(Location location) {
      this.playerPlacedOres.add(location);
   }

   public static Location normalizeBlockLocation(Location location) {
      return location != null && location.getWorld() != null ? new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ()) : location;
   }

   private boolean isLineBreaking(Player player, Block block) {
      UUID playerId = player.getUniqueId();
      Deque<Location> recentBreaks = this.blockBreak.computeIfAbsent(playerId, key -> new LinkedList<>());
      int requiredPoints = 2 * this.lineBreakLength;

      while (recentBreaks.size() >= requiredPoints) {
         recentBreaks.pollFirst();
      }

      recentBreaks.addLast(block.getLocation());
      return recentBreaks.size() < requiredPoints ? false : FakeOreProtectionListener.areLocationsCollinear(new ArrayList<>(recentBreaks));
   }

   private void handleFakeOreBreakInCheck(Player player, Block block) {
      Location normalizedLocation = normalizeBlockLocation(block.getLocation());
      UUID playerId = player.getUniqueId();
      if (this.fakeOreTypes.containsKey(normalizedLocation)) {
         UUID decoyOwnerId = this.decoyOreOwners.get(normalizedLocation);
         if (decoyOwnerId != null && !decoyOwnerId.equals(playerId)) {
            this.plugin.logDebug("玩家 " + player.getName() + " 挖到他人诱饵(属于 " + decoyOwnerId + "), 按正常探索处理, 静默清理");
            this.removeFakeOreSilently(normalizedLocation);
         } else {
            try {
               if (this.hasDetectionBypass(player)) {
                  this.plugin.logDebug("玩家 " + player.getName() + " 具有bypass权限,跳过假矿石破坏处理");
                  this.removeFakeOreSilently(normalizedLocation);
                  return;
               }

               LocalDateTime now = LocalDateTime.now();
               if (this.isInViolationCooldown(playerId, now)) {
                  this.plugin.logDebug("玩家 " + player.getName() + " 在冷却时间内，跳过违规值增加");
                  this.removeFakeOreSilently(normalizedLocation);
                  return;
               }

               double violationIncrement = this.calculateViolationIncrement(playerId, normalizedLocation);
               this.recordAndNotifyViolation(player, violationIncrement, normalizedLocation);
               this.updateViolationData(playerId, now);
               this.removeFakeOreSilently(normalizedLocation);
            } catch (Exception ex) {
               this.plugin.getLogger().log(Level.SEVERE, "处理假矿石破坏时发生错误: " + ex.getMessage(), ex);
            }
         }
      }
   }

   private boolean isInViolationCooldown(UUID playerId, LocalDateTime currentTime) {
      LocalDateTime previousViolationTime = this.lastViolationTime.getOrDefault(playerId, LocalDateTime.MIN);
      int violationCooldown = this.plugin.getConfigManager().getViolationCooldown();
      return ChronoUnit.SECONDS.between(previousViolationTime, currentTime) < violationCooldown;
   }

   private double calculateViolationIncrement(UUID playerId, Location oreLocation) {
      boolean ownFakeOre = this.isOwnFakeOre(playerId, oreLocation);
      return ownFakeOre ? this.plugin.getConfigManager().getCheckXrayVlAddBreakSelf() : this.plugin.getConfigManager().getCheckXrayVlAddBreakOther();
   }

   private void recordAndNotifyViolation(Player player, double violationIncrement, Location oreLocation) {
      this.plugin.logDebug("\n=== 记录违规信息 ===");
      this.addViolation(player, violationIncrement);
      this.plugin.logDebug(String.format("已添加违规记录 - 玩家: %s, 违规值: %.2f", player.getName(), violationIncrement));
      double totalViolationLevel = this.playerViolations.getOrDefault(player.getUniqueId(), 0.0);
      this.plugin.logDebug(String.format("当前总违规值: %.2f", totalViolationLevel));
      DetectionNotificationManager detectionNotificationManager = this.plugin.getDetectionNotificationManager();
      String formattedLocation = this.formatLocation(oreLocation);
      detectionNotificationManager.notifyDetection(player, "xray", "XRay", totalViolationLevel, formattedLocation);
      this.plugin.logDebug("已发送检测通知");
   }

   private void updateViolationData(UUID playerId, LocalDateTime violationTime) {
      this.lastViolationTime.put(playerId, violationTime);
      this.fakeOreBreakCount.compute(playerId, (key, count) -> count == null ? 1 : count + 1);
      this.lastFakeOreBreakTime.put(playerId, violationTime);
   }

   private boolean isOwnFakeOre(UUID playerId, Location oreLocation) {
      List<Location> playerOres = this.oreMap.getOrDefault(playerId, new ArrayList<>());
      return playerOres.contains(oreLocation);
   }

   private void addViolation(Player player, double violationIncrement) {
      UUID playerId = player.getUniqueId();
      LocalDateTime now = LocalDateTime.now();
      this.plugin.logDebug("addViolation: 开始处理玩家 " + player.getName() + " (UUID: " + playerId + ") 增加违规值: " + violationIncrement);
      this.plugin.logDebug("addViolation: 检查前 playerViolations 包含 key " + playerId + ": " + this.playerViolations.containsKey(playerId));
      double existingViolationBefore = this.playerViolations.getOrDefault(playerId, -1.0);
      this.plugin.logDebug("addViolation: 调用 getOrDefault 前的值 (或-1.0如果不存在): " + existingViolationBefore);
      LocalDateTime previousViolationTime = this.lastViolationTime.getOrDefault(playerId, LocalDateTime.MIN);
      long violationCooldown = this.plugin.getConfigManager().getViolationCooldown();
      if (ChronoUnit.SECONDS.between(previousViolationTime, now) < violationCooldown) {
         this.plugin.logDebug("玩家 " + player.getName() + " 在冷却时间内，跳过违规值增加");
      } else {
         double currentViolationLevel = this.playerViolations.getOrDefault(playerId, 0.0);
         this.plugin.logDebug("addViolation: getOrDefault 返回的 currentVL: " + currentViolationLevel);
         double updatedViolationLevel = currentViolationLevel + violationIncrement;
         this.plugin.logDebug("addViolation: 计算出的 newVL: " + updatedViolationLevel + " (来自 " + currentViolationLevel + " + " + violationIncrement + ")");
         this.playerViolations.put(playerId, updatedViolationLevel);
         this.lastViolationTime.put(playerId, now);
         double storedViolationAfter = this.playerViolations.getOrDefault(playerId, -2.0);
         this.plugin.logDebug("addViolation: put 操作后的 playerViolations 值 (或-2.0如果失败): " + storedViolationAfter);
         this.plugin.logDebug("玩家 " + player.getName() + " 的违规值从 " + currentViolationLevel + " 增加到 " + updatedViolationLevel);
         this.checkBanThreshold(player, updatedViolationLevel);
         this.violationTracker.addViolation(player, violationIncrement);
      }
   }

   private void checkBanThreshold(Player player, double violationLevel) {
      NekoAntiXray instance = NekoAntiXray.getInstance();
      String playerName = player.getName();
      instance.logDebug("检查玩家 " + playerName + " 的封禁阈值，当前违规值: " + violationLevel);
      if (instance.getConfigManager().isNotificationSystemXrayBanEnabled() && violationLevel >= instance.getConfigManager().getNotificationSystemXrayBanThreshold()) {
         String banReason = instance.getConfigManager().getNotificationSystemBanMessagesBanReason().replace("%vl%", String.format("%.2f", violationLevel));
         instance.banPlayerOrCommand(player, "xray", banReason, violationLevel);
         UUID playerId = player.getUniqueId();
         instance.getFoliaScheduler().getImpl().runLater(wrappedTask -> this.clearPlayerData(playerId), 20L);
         instance.logDebug("玩家 " + player.getName() + " 已被封禁，其违规值记录已清空");
      }
   }

   private void generateFakeOresForPlayer(Player player, Block block) {
      UUID playerId = player.getUniqueId();
      LocalDateTime now = LocalDateTime.now();
      this.plugin.logDebug("\n=== 开始生成假矿石流程 ===");
      this.plugin.logDebug(String.format("玩家: %s", player.getName()));
      this.plugin.logDebug(String.format("原始方块位置: %s", this.formatLocation(block.getLocation())));
      this.plugin.logDebug(String.format("原始方块类型: %s", block.getType()));
      if (this.isInFakeOreGenerationCooldown(playerId, now)) {
         this.plugin.logDebug("生成被冷却时间限制，跳过生成");
      } else {
         this.throwDecoyOre(player, block);
         Material material = this.selectFakeOreMaterial(block);
         int checkXraySpawnOre = this.plugin.getConfigManager().getCheckXraySpawnOre();
         this.plugin.logDebug(String.format("计划生成数量: %d, 类型: %s", checkXraySpawnOre, material));
         this.lastFakeOreGenerationTime.put(playerId, now);
         boolean randomOreEnabled = this.plugin.getConfigManager().isCheckXraySpawnRandomOreEnabled();
         if (!randomOreEnabled && material == Material.ANCIENT_DEBRIS) {
            this.plugin.logDebug("检测到远古残骸，使用特定多集群生成模式");
            this.generateMultipleAncientDebrisClustersAsync(player, block, material);
         } else if (randomOreEnabled || material != Material.EMERALD_ORE && material != Material.DEEPSLATE_EMERALD_ORE) {
            this.plugin.logDebug(randomOreEnabled ? "使用通用群集生成模式(随机模式: 每个矿脉独立随机矿石类型)" : "使用通用群集生成模式");
            this.generateFakeOresAsync(player, block, checkXraySpawnOre);
         } else {
            this.plugin.logDebug("检测到单个类型矿物（绿宝石），使用单个生成模式");
            this.generateSingleFakeOre(player, block, material);
         }
      }
   }

   private Material selectFakeOreMaterial(Block block) {
      try {
         ConfigManager config = this.plugin.getConfigManager();
         if (!config.isCheckXraySpawnRandomOreEnabled()) {
            return block.getType();
         }

         Environment environment = block.getWorld().getEnvironment();
         int blockY = block.getY();
         List randomFakeOrePool = config.getRandomFakeOrePool(environment, blockY);
         if (randomFakeOrePool.isEmpty()) {
            return block.getType();
         }

         Material selectedMaterial = (Material)randomFakeOrePool.get(ThreadLocalRandom.current().nextInt(randomFakeOrePool.size()));
         String originalType = String.valueOf(block.getType());
         String selectedType = String.valueOf(selectedMaterial);
         String worldEnvironment = String.valueOf(block.getWorld().getEnvironment());
         this.plugin.logDebug("随机假矿石: " + originalType + " -> " + selectedType + " (world=" + worldEnvironment + ", y=" + blockY + ")");
         return selectedMaterial;
      } catch (Throwable ex) {
         return block.getType();
      }
   }

   private void throwDecoyOre(Player player, Block block) {
      try {
         ConfigManager config = this.plugin.getConfigManager();
         if (!config.isFakeOreDecoyThrowEnabled()) {
            return;
         }

         UUID playerId = player.getUniqueId();
         Material decoyMaterial = this.selectFakeOreMaterial(block);
         int minDistance = config.getFakeOreDecoyThrowMinDistance();
         int maxDistance = config.getFakeOreDecoyThrowMaxDistance();
         int requiredWallLayers = config.getFakeOreDecoyThrowBehindWallLayers();
         Location eyeLocation = player.getEyeLocation();
         Vector direction = eyeLocation.getDirection();
         if (direction.lengthSquared() < 1.0E-4) {
            return;
         }

         Vector stepDirection = direction.clone().normalize();
         World world = eyeLocation.getWorld();
         if (world == null) {
            return;
         }

         int minHeight = world.getMinHeight();
         int maxHeight = world.getMaxHeight();
         Location rayLocation = eyeLocation.clone();
         int solidWallLayers = 0;
         Location targetLocation = null;
         int placedDistance = -1;

         for (int i = 1; i <= maxDistance; i++) {
            rayLocation.add(stepDirection);
            int blockY = rayLocation.getBlockY();
            if (blockY < minHeight || blockY >= maxHeight) {
               break;
            }

            Block rayBlock = rayLocation.getBlock();
            if (rayBlock.getType().isOccluding()) {
               solidWallLayers++;
            } else {
               if (i >= minDistance && solidWallLayers >= requiredWallLayers) {
                  targetLocation = new Location(world, rayLocation.getBlockX(), rayLocation.getBlockY(), rayLocation.getBlockZ());
                  placedDistance = i;
                  break;
               }

               solidWallLayers = 0;
            }
         }

         if (targetLocation == null) {
            this.plugin.logDebug("抛投诱饵: 未找到有效落点(视线前方无连续实心墙)");
            return;
         }

         Block targetBlock = targetLocation.getBlock();
         if (!targetBlock.getType().isAir()) {
            this.plugin.logDebug("抛投诱饵: 落点非空气, 跳过 " + this.formatLocation(targetLocation));
            return;
         }

         if (this.fakeOreTypes.containsKey(targetLocation) || this.playerPlacedOres.contains(targetLocation)) {
            return;
         }

         targetBlock.setType(decoyMaterial, false);
         this.fakeOreTypes.put(targetLocation, decoyMaterial);
         this.decoyOreOwners.put(targetLocation, playerId);
         List<Location> playerOres = this.oreMap.computeIfAbsent(playerId, key -> new ArrayList<>());
         playerOres.add(targetLocation);
         this.setupSingleOreCleanupTask(playerId, Collections.singletonList(targetLocation));
         String formattedLocation = this.formatLocation(targetLocation);
         this.plugin.logDebug("抛投诱饵矿石: " + formattedLocation + " 距离=" + placedDistance + " 穿透层数=" + solidWallLayers);
      } catch (Exception ex) {
         this.plugin.logDebug("抛投诱饵矿石异常: " + ex);
      }
   }

   public boolean tryPlaceDecoyOre(Player player, Vector direction, int maxDistanceOverride, Material oreMaterial) {
      return this.placeDecoyOre(player, direction, maxDistanceOverride, oreMaterial, null) != null;
   }

   public XrayDetector.DecoyPlacementResult placeDecoyOre(Player player, Vector direction, int maxDistanceOverride, Material oreMaterial, String directionLabel) {
      try {
         ConfigManager config = this.plugin.getConfigManager();
         UUID playerId = player.getUniqueId();
         Material effectiveMaterial = oreMaterial != null ? oreMaterial : Material.DIAMOND_ORE;
         int minDistance = config.getFakeOreDecoyThrowMinDistance();
         int maxDistance = maxDistanceOverride > 0 ? Math.min(maxDistanceOverride, 128) : config.getFakeOreDecoyThrowMaxDistance();
         int requiredWallLayers = config.getFakeOreDecoyThrowBehindWallLayers();
         Location eyeLocation = player.getEyeLocation();
         Vector baseDirection = direction != null ? direction.clone().normalize() : eyeLocation.getDirection();
         if (baseDirection.lengthSquared() < 1.0E-4) {
            return null;
         }

         Vector stepDirection = baseDirection.clone().normalize();
         World world = eyeLocation.getWorld();
         if (world == null) {
            return null;
         }

         int minHeight = world.getMinHeight();
         int maxHeight = world.getMaxHeight();
         Location rayLocation = eyeLocation.clone();
         int solidWallLayers = 0;
         Location targetLocation = null;
         int placedDistance = -1;

         for (int i = 1; i <= maxDistance; i++) {
            rayLocation.add(stepDirection);
            int blockY = rayLocation.getBlockY();
            if (blockY < minHeight || blockY >= maxHeight) {
               break;
            }

            Block rayBlock = rayLocation.getBlock();
            if (rayBlock.getType().isOccluding()) {
               solidWallLayers++;
            } else {
               if (i >= minDistance && solidWallLayers >= requiredWallLayers) {
                  targetLocation = new Location(world, rayLocation.getBlockX(), rayLocation.getBlockY(), rayLocation.getBlockZ());
                  placedDistance = i;
                  break;
               }

               solidWallLayers = 0;
            }
         }

         if (targetLocation == null) {
            this.plugin.logDebug("诱饵投放: 方向 " + stepDirection + " 未找到有效落点");
            return null;
         } else {
            Block targetBlock = targetLocation.getBlock();
            if (!targetBlock.getType().isAir()) {
               this.plugin.logDebug("诱饵投放: 落点非空气 " + this.formatLocation(targetLocation));
               return null;
            } else if (!this.fakeOreTypes.containsKey(targetLocation) && !this.playerPlacedOres.contains(targetLocation)) {
               targetBlock.setType(effectiveMaterial, false);
               this.fakeOreTypes.put(targetLocation, effectiveMaterial);
               this.decoyOreOwners.put(targetLocation, playerId);
               List<Location> playerOres = this.oreMap.computeIfAbsent(playerId, key -> new ArrayList<>());
               playerOres.add(targetLocation);
               this.setupSingleOreCleanupTask(playerId, Collections.singletonList(targetLocation));
               String formattedLocation = this.formatLocation(targetLocation);
               this.plugin.logDebug("诱饵投放成功: " + formattedLocation + " 距离=" + placedDistance + " 穿透层数=" + solidWallLayers);
               return new XrayDetector.DecoyPlacementResult(targetLocation, directionLabel, placedDistance);
            } else {
               return null;
            }
         }
      } catch (Exception ex) {
         this.plugin.logDebug("诱饵投放异常: " + ex);
         return null;
      }
   }

   public void queueDecoyOrePlacement(Player player, Material material, int maxDistance) {
      this.pendingDecoyRequests.put(player.getUniqueId(), new XrayDetector.PendingDecoyRequest(material, maxDistance));
      player.sendMessage("§e诱饵投放已就绪: 请挖掘一个方块后自动投放。");
   }

   public void processPendingDecoyPlacement(Player player) {
      XrayDetector.PendingDecoyRequest pendingDecoyRequest = this.pendingDecoyRequests.remove(player.getUniqueId());
      if (pendingDecoyRequest != null) {
         Vector[] candidateDirections = new Vector[]{
            player.getEyeLocation().getDirection().clone().normalize(),
            player.getEyeLocation().getDirection().clone().normalize().multiply(-1),
            rotateVectorAroundY(player.getEyeLocation().getDirection(), 90.0),
            rotateVectorAroundY(player.getEyeLocation().getDirection(), -90.0),
            new Vector(0, 1, 0),
            new Vector(0, -1, 0)
         };
         String[] directionLabels = new String[]{"前方", "后方", "左方", "右方", "上方", "下方"};

         for (int i = 0; i < candidateDirections.length; i++) {
            Vector direction = candidateDirections[i];
            Material material = pendingDecoyRequest.oreMaterial;
            int requestedDistance = pendingDecoyRequest.distance;
            String label = directionLabels[i];
            XrayDetector.DecoyPlacementResult placementResult = this.placeDecoyOre(player, direction, requestedDistance, material, label);
            if (placementResult != null) {
               Location placedLocation = placementResult.location;
               String placedDirection = placementResult.directionLabel;
               int blockX = placedLocation.getBlockX();
               int blockY = placedLocation.getBlockY();
               int blockZ = placedLocation.getBlockZ();
               int placedDistance = placementResult.distance;
               player.sendMessage("§a诱饵矿石已投放! §7方位: §f" + placedDirection + " §7坐标: §f" + blockX + ", " + blockY + ", " + blockZ + " §7距离: §f" + placedDistance + "格");
               String playerName = player.getName();
               String formattedLocation = this.formatLocation(placedLocation);
               this.plugin.logDebug("诱饵投放(decoy指令): " + playerName + " -> " + placedDirection + " " + formattedLocation);
               return;
            }
         }

         player.sendMessage("§c四周均不符合投放条件(需要连续 " + this.plugin.getConfigManager().getFakeOreDecoyThrowBehindWallLayers() + " 层实心方块后的空气位置)。");
      }
   }

   private static Vector rotateVectorAroundY(Vector vector, double degrees) {
      double radians = Math.toRadians(degrees);
      double cos = Math.cos(radians);
      double sin = Math.sin(radians);
      double rotatedX = vector.getX() * cos + vector.getZ() * sin;
      double rotatedZ = -vector.getX() * sin + vector.getZ() * cos;
      return new Vector(rotatedX, vector.getY(), rotatedZ);
   }

   private void generateSingleFakeOre(Player player, Block block, Material material) {
      ConfigManager config = this.plugin.getConfigManager();
      int spawnCount = config.getCheckXraySpawnOre();
      List<CompletableFuture<Location>> locationFutures = new ArrayList<>();
      String debugHeader = String.format(
         "\n=== 开始生成单个假矿石 ===\n玩家: %s\n原始位置: %s\n计划生成数量: %d\n矿石类型: %s", player.getName(), this.formatLocation(block.getLocation()), spawnCount, material
      );
      this.plugin.logDebug(debugHeader);

      for (int i = 0; i < spawnCount; i++) {
         CompletableFuture<Location> locationFuture = new CompletableFuture<>();
         locationFutures.add(locationFuture);
         this.foliaLib.getImpl().runAtLocation(block.getLocation(), wrappedTask -> this.findValidFakeOreLocation(player, block.getLocation(), config, locationFuture, material));
      }

      CompletableFuture.allOf(locationFutures.toArray(new CompletableFuture[0]))
         .thenAccept(ignored -> this.onSingleFakeOreLocationsReady(locationFutures, block, player, material));
   }

   private void findValidFakeOreLocation(Player player, Location originLocation, ConfigManager config, CompletableFuture locationFuture, Material material) {
      int maxAttempts = 50;
      AtomicInteger attemptCounter = new AtomicInteger(0);
      this.tryGenerateLocation(player, originLocation, config, locationFuture, attemptCounter, maxAttempts, material);
   }

   private void tryGenerateLocation(Player player, Location originLocation, ConfigManager config, CompletableFuture locationFuture, AtomicInteger attemptCounter, int maxAttempts, Material material) {
      if (attemptCounter.get() >= maxAttempts) {
         this.plugin.logDebug("达到最大尝试次数，放弃生成");
         locationFuture.complete(null);
      } else {
         int checkXraySpawnXChangeMin = config.getCheckXraySpawnXChangeMin();
         int checkXraySpawnXChangeMax = config.getCheckXraySpawnXChangeMax();
         int checkXraySpawnZChangeMin = config.getCheckXraySpawnZChangeMin();
         int checkXraySpawnZChangeMax = config.getCheckXraySpawnZChangeMax();
         int checkXraySpawnYChangeMin = config.getCheckXraySpawnYChangeMin();
         int checkXraySpawnYChangeMax = config.getCheckXraySpawnYChangeMax();
         Location candidateLocation = FakeOreProtectionListener.generateRandomOffsetLocation(originLocation, checkXraySpawnXChangeMin, checkXraySpawnXChangeMax, checkXraySpawnZChangeMin, checkXraySpawnZChangeMax, checkXraySpawnYChangeMin, checkXraySpawnYChangeMax);
         this.isValidFakeOreLocation(candidateLocation, player, material)
            .thenAccept(isValid -> this.onCandidateLocationChecked(candidateLocation, locationFuture, attemptCounter, player, originLocation, config, maxAttempts, material, (Boolean)isValid));
      }
   }

   private void finalizeSingleFakeOreGeneration(Player player, List locations, Material material) {
      this.plugin.logDebug("\n=== 开始最终生成阶段 ===");
      this.plugin.logDebug(String.format("玩家: %s", player.getName()));
      this.plugin.logDebug(String.format("矿石类型: %s", material));
      this.plugin.logDebug(String.format("有效位置数量: %d", locations.size()));
      List<Location> playerOres = this.oreMap.computeIfAbsent(player.getUniqueId(), playerId -> new ArrayList<>());

      for (Object entry : locations) {
         Location oreLocation = normalizeBlockLocation((Location)entry);
         Block block = oreLocation.getBlock();
         block.setType(material, false);
         this.fakeOreTypes.put(oreLocation, material);
         playerOres.add(oreLocation);
         this.plugin.logDebug(String.format("生成假矿石: %s -> %s", this.formatLocation(oreLocation), material));
      }

      if (!locations.isEmpty()) {
         this.setupSingleOreCleanupTask(player.getUniqueId(), locations);
         this.plugin.logDebug("已设置清理任务");
      }
   }

   private void setupSingleOreCleanupTask(UUID playerId, List locations) {
      if (!locations.isEmpty()) {
         Location anchorLocation = (Location)locations.get(0);
         long delayTicks = this.plugin.getConfigManager().getCheckXraySpawnTime() * 20L;
         this.foliaLib.getImpl().runAtLocationLater(anchorLocation, wrappedTask -> {
            for (Object entry : locations) {
               Location oreLocation = (Location)entry;
               Block block = oreLocation.getBlock();
               if (block.getType() != Material.AIR) {
                  Material replacementMaterial = oreLocation.getWorld().getEnvironment() == Environment.NETHER ? Material.NETHERRACK : Material.STONE;
                  block.setType(replacementMaterial, false);
                  this.plugin.logDebug(String.format("清理假矿石 - 位置: %s -> %s", this.formatLocation(oreLocation), replacementMaterial));
               } else {
                  this.plugin.logDebug(String.format("跳过已被挖掘的假矿石位置: %s", this.formatLocation(oreLocation)));
               }

               this.fakeOreTypes.remove(oreLocation);
               this.decoyOreOwners.remove(oreLocation);
            }

            List<Location> playerOres = this.oreMap.get(playerId);
            if (playerOres != null) {
               playerOres.removeAll(locations);
               if (playerOres.isEmpty()) {
                  this.oreMap.remove(playerId);
               }
            }

            this.plugin.logDebug("清理任务完成");
         }, delayTicks);
      }
   }

   private boolean isInFakeOreGenerationCooldown(UUID playerId, LocalDateTime currentTime) {
      LocalDateTime previousGenerationTime = this.lastFakeOreGenerationTime.getOrDefault(playerId, LocalDateTime.MIN);
      long elapsedSeconds = ChronoUnit.SECONDS.between(previousGenerationTime, currentTime);
      if (elapsedSeconds < this.fakeOreGenerationCooldown) {
         this.plugin.logDebug("玩家的假矿石生成仍在冷却中，跳过生成。还需等待 " + (this.fakeOreGenerationCooldown - elapsedSeconds) + " 秒。");
         return true;
      } else {
         return false;
      }
   }

   private CompletableFuture generateOreCluster(Location centerLocation, Material material, int minSize, int maxSize, Player player) {
      CompletableFuture<XrayDetector.OreCluster> clusterFuture = new CompletableFuture<>();
      this.foliaLib.getImpl().runAtLocation(centerLocation, wrappedTask -> {
         try {
            List<Location> clusterLocations = new ArrayList<>();
            int targetSize = ThreadLocalRandom.current().nextInt(minSize, maxSize + 1);
            LinkedList<Location> bfsQueue = new LinkedList<>();
            HashSet<Location> visitedLocations = new HashSet<>();
            this.plugin.logDebug(String.format("\n=== 开始生成矿石群集 ===\n中心位置: %s\n目标大小: %d\n矿石类型: %s", this.formatLocation(centerLocation), targetSize, material));
            bfsQueue.add(centerLocation);

            while (!bfsQueue.isEmpty() && clusterLocations.size() < targetSize) {
               Location currentLocation = bfsQueue.poll();
               if (!visitedLocations.contains(currentLocation)) {
                  visitedLocations.add(currentLocation);
                  if (this.isValidFakeOreLocationSync(currentLocation, player, material)) {
                     clusterLocations.add(currentLocation);
                     this.plugin.logDebug(String.format("添加位置到群集: %s (当前大小: %d/%d)", this.formatLocation(currentLocation), clusterLocations.size(), targetSize));

                     for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                           for (int dz = -1; dz <= 1; dz++) {
                              if (dx != 0 || dy != 0 || dz != 0) {
                                 Location neighborLocation = currentLocation.clone().add(dx, dy, dz);
                                 if (!visitedLocations.contains(neighborLocation)) {
                                    bfsQueue.add(neighborLocation);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            this.plugin.logDebug(String.format("=== 群集生成完成 ===\n最终大小: %d/%d\n检查的位置数: %d", clusterLocations.size(), targetSize, visitedLocations.size()));
            clusterFuture.complete(new XrayDetector.OreCluster(clusterLocations, material));
         } catch (Exception ex) {
            this.plugin.getLogger().log(Level.SEVERE, "生成矿石群集时发生错误: " + ex.getMessage(), ex);
            clusterFuture.complete(new XrayDetector.OreCluster(new ArrayList<>(), material));
         }
      });
      return clusterFuture;
   }

   private void finalizeFakeOreGeneration(Player player, List clusters) {
      if (clusters.isEmpty()) {
         this.plugin.logDebug("没有有效的群集可以生成");
      } else {
         this.plugin.logDebug("\n=== 开始最终生成阶段 ===");
         this.plugin.logDebug(String.format("玩家: %s", player.getName()));
         this.plugin.logDebug(String.format("群集数量: %d", clusters.size()));
         int totalGenerated = 0;
         List<Location> playerOres = this.oreMap.computeIfAbsent(player.getUniqueId(), playerId -> new ArrayList<>());
         Map<Material, Integer> materialCounts = new HashMap<>();

         for (Object entry : clusters) {
            XrayDetector.OreCluster oreCluster = (XrayDetector.OreCluster)entry;
            Material oreMaterial = oreCluster.type;

            for (Location location : oreCluster.locations) {
               Location normalizedLocation = normalizeBlockLocation(location);
               Block block = normalizedLocation.getBlock();
               block.setType(oreMaterial, false);
               this.fakeOreTypes.put(normalizedLocation, oreMaterial);
               playerOres.add(normalizedLocation);
               totalGenerated++;
               materialCounts.merge(oreMaterial, 1, Integer::sum);
               this.plugin.logDebug(String.format("生成假矿石: %s -> %s", this.formatLocation(normalizedLocation), oreMaterial));
            }
         }

         this.plugin.logDebug("本次生成矿石类型分布: " + materialCounts);
         String statsMessage = String.format("\n=== 生成统计 ===\n总生成数量: %d\n当前假矿石总数: %d\n玩家假矿石数量: %d", totalGenerated, this.fakeOreTypes.size(), playerOres.size());
         this.plugin.logDebug(statsMessage);
         if (totalGenerated > 0) {
            this.setupCleanupTask(player.getUniqueId(), clusters);
            this.plugin.logDebug("已设置清理任务");
         }
      }
   }

   private void setupCleanupTask(UUID playerId, List clusters) {
      if (!clusters.isEmpty() && !((XrayDetector.OreCluster)clusters.get(0)).locations.isEmpty()) {
         Location anchorLocation = ((XrayDetector.OreCluster)clusters.get(0)).locations.get(0);
         long delayTicks = this.plugin.getConfigManager().getCheckXraySpawnTime() * 20L;
         String scheduleLog = String.format(
            "\n=== 设置清理任务 ===\n玩家: %s\n延迟: %d ticks\n群集数量: %d\n第一个位置: %s",
            Bukkit.getOfflinePlayer(playerId).getName(),
            delayTicks,
            clusters.size(),
            this.formatLocation(anchorLocation)
         );
         this.plugin.logDebug(scheduleLog);
         this.foliaLib
            .getImpl()
            .runAtLocationLater(
               anchorLocation,
               wrappedTask -> {
                  this.plugin.logDebug("\n=== 开始执行清理任务 ===");
                  int processedCount = 0;
                  Map<Material, Integer> replacementCounts = new HashMap<>();

                  for (Object entry : clusters) {
                     XrayDetector.OreCluster oreCluster = (XrayDetector.OreCluster)entry;
                     for (Location oreLocation : oreCluster.locations) {
                        try {
                           Block block = oreLocation.getBlock();
                           Material currentType = block.getType();
                           if (currentType != Material.AIR) {
                              Material replacementMaterial = oreLocation.getWorld().getEnvironment() == Environment.NETHER ? Material.NETHERRACK : Material.STONE;
                              block.setType(replacementMaterial, false);
                              replacementCounts.merge(replacementMaterial, 1, Integer::sum);
                              this.plugin.logDebug(String.format("清理假矿石 - 位置: %s, %s -> %s", this.formatLocation(oreLocation), currentType, replacementMaterial));
                           } else {
                              this.plugin.logDebug(String.format("跳过被挖掘的方块位置: %s", this.formatLocation(oreLocation)));
                           }

                           this.fakeOreTypes.remove(oreLocation);
                           processedCount++;
                        } catch (Exception ex) {
                           this.plugin.getLogger().warning(String.format("清理假矿石时发生错误 - 位置: %s, 错误: %s", this.formatLocation(oreLocation), ex.getMessage()));
                        }
                     }
                  }

                  List<Location> playerOres = this.oreMap.get(playerId);
                  int removedFromPlayerCount = 0;
                  if (playerOres != null) {
                     int sizeBeforeRemoval = playerOres.size();
                     playerOres.removeAll(
                        (Collection<?>)clusters.stream().flatMap(cluster -> streamClusterLocationsForRemoval((XrayDetector.OreCluster)cluster)).collect(Collectors.toList())
                     );
                     removedFromPlayerCount = sizeBeforeRemoval - playerOres.size();
                     if (playerOres.isEmpty()) {
                        this.oreMap.remove(playerId);
                     }
                  }

                  this.plugin.logDebug(String.format("\n=== %d 个集群清理统计 ===", clusters.size()));
                  String totalAttemptedLog = String.format(
                     "总清理位置数 (尝试): %d",
                     ((List<?>)clusters.stream().flatMap(cluster -> streamClusterLocationsForStats((XrayDetector.OreCluster)cluster)).collect(Collectors.toList())).size()
                  );
                  this.plugin.logDebug(totalAttemptedLog);
                  this.plugin.logDebug(String.format("实际替换方块数量: %d", processedCount));
                  this.plugin.logDebug(String.format("从玩家 %s 数据中移除: %d", Bukkit.getOfflinePlayer(playerId).getName(), removedFromPlayerCount));
                  this.plugin.logDebug("替换方块统计:");
                  replacementCounts.forEach((material, count) -> this.logReplacedCleanupMaterialCount(material, count));
                  this.plugin.logDebug(String.format("剩余fakeOreTypes大小: %d", this.fakeOreTypes.size()));
                  this.plugin.logDebug("=== 清理任务完成 ===\n");
               },
               delayTicks
            );
      } else {
         this.plugin.logDebug("没有假矿石需要清理");
      }
   }

   private CompletableFuture isValidFakeOreLocation(Location candidateLocation, Player player, Material material) {
      CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
      this.foliaLib.getImpl().runAtLocation(candidateLocation, wrappedTask -> {
         try {
            boolean validFakeOreLocationSync = this.isValidFakeOreLocationSync(candidateLocation, player, material);
            resultFuture.complete(validFakeOreLocationSync);
         } catch (Exception ex) {
            this.plugin.getLogger().warning("检查位置有效性时发生错误 (isValidFakeOreLocation): " + ex.getMessage());
            resultFuture.complete(false);
         }
      });
      return resultFuture;
   }

   private boolean isValidFakeOreLocationSync(Location candidateLocation, Player player, Material material) {
      Block block = candidateLocation.getBlock();
      Material currentMaterial = block.getType();
      World world = candidateLocation.getWorld();
      if (world == null) {
         this.plugin.logDebug("isValidFakeOreLocationSync: World is null for location: " + this.formatLocation(candidateLocation));
         return false;
      }

      boolean isNether = world.getEnvironment() == Environment.NETHER;
      int blockY = candidateLocation.getBlockY();
      int minHeight = world.getMinHeight();
      int maxHeight = world.getMaxHeight();
      if (isNether) {
         if (blockY <= 4 || blockY >= 123) {
            this.plugin.logDebug("isValidFakeOreLocationSync: Invalid Y coordinate for Nether: " + blockY + " at " + this.formatLocation(candidateLocation));
            return false;
         }
      } else {
         if (blockY <= minHeight + 4) {
            String formattedLocation = this.formatLocation(candidateLocation);
            String logMessage = "isValidFakeOreLocationSync: Y coordinate too close to bedrock layer: " + blockY + " (minHeight: " + minHeight + ") at " + formattedLocation;
            this.plugin.logDebug(logMessage);
            return false;
         }

         if (blockY >= maxHeight - 5) {
            String formattedLocation = this.formatLocation(candidateLocation);
            String logMessage = "isValidFakeOreLocationSync: Y coordinate too close to world maximum: " + blockY + " (maxHeight: " + maxHeight + ") at " + formattedLocation;
            this.plugin.logDebug(logMessage);
            return false;
         }
      }

      boolean isValidHostMaterial;
      if (isNether) {
         isValidHostMaterial = currentMaterial == Material.NETHERRACK || currentMaterial == Material.BASALT || currentMaterial == Material.BLACKSTONE;
      } else {
         isValidHostMaterial = currentMaterial == Material.STONE
            || currentMaterial == Material.DEEPSLATE
            || currentMaterial == Material.ANDESITE
            || currentMaterial == Material.DIORITE
            || currentMaterial == Material.GRANITE
            || currentMaterial == Material.TUFF;
      }

      boolean isNotExposed = !OreMaterialMatcher.isExposedToAirOrLiquid(block, 0);
      double distanceToPlayer = player.getLocation().distance(candidateLocation);
      double minPlayerDistance = this.plugin.getConfigManager().getCheckXraySpawnDistanceLimitPlayer();
      boolean isFarEnoughFromPlayer = distanceToPlayer >= minPlayerDistance;
      boolean hasNoDisallowedAdjacentOre;
      if (material == Material.ANCIENT_DEBRIS) {
         hasNoDisallowedAdjacentOre = !this.hasDisallowedAdjacentOre(candidateLocation, EnumSet.of(Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE));
      } else {
         hasNoDisallowedAdjacentOre = !this.hasDisallowedAdjacentOre(candidateLocation, EnumSet.noneOf(Material.class));
      }

      boolean isValidVerticalDist = this.verticalDistributionCheck.isValidVerticalDistribution(candidateLocation, player);
      double minOreDistance = this.plugin.getConfigManager().getCheckXraySpawnDistanceLimitOre();
      double minOreDistanceSquared = minOreDistance * minOreDistance;
      boolean isTooCloseToOtherFakeOre = false;

      for (Location existingLocation : new HashSet<>(this.fakeOreTypes.keySet())) {
         if (existingLocation != null && existingLocation.getWorld() != null && existingLocation.getWorld().equals(candidateLocation.getWorld()) && existingLocation.distanceSquared(candidateLocation) < minOreDistanceSquared) {
            isTooCloseToOtherFakeOre = true;
            break;
         }
      }

      boolean isValid = isValidHostMaterial && isNotExposed && isFarEnoughFromPlayer && isValidVerticalDist && hasNoDisallowedAdjacentOre && !isTooCloseToOtherFakeOre;
      if (!isValid) {
         String failureReason = String.format(
            "位置检查无效 - %s, 原因: %s%s%s%s%s%s",
            this.formatLocation(candidateLocation),
            !isValidHostMaterial ? "材料无效(" + currentMaterial + ") " : "",
            !isNotExposed ? "暴露空气 " : "",
            !isFarEnoughFromPlayer ? String.format("距离玩家(%.1f<%.1f) ", distanceToPlayer, minPlayerDistance) : "",
            !hasNoDisallowedAdjacentOre ? "邻近不允许矿石 " : "",
            !isValidVerticalDist ? "垂直分布无效 " : "",
            isTooCloseToOtherFakeOre ? String.format("过近其他假矿(<%.1f) ", minOreDistance) : ""
         );
         this.plugin.logDebug(failureReason);
      }

      return isValid;
   }

   private boolean hasDisallowedAdjacentOre(Location location, EnumSet allowedOres) {
      Block block = location.getBlock();
      BlockFace[] faces = new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

      for (BlockFace face : faces) {
         Block relative = block.getRelative(face);
         Material relativeType = relative.getType();
         if (OreMaterialMatcher.isOreMaterial(relativeType) && !allowedOres.contains(relativeType)) {
            return true;
         }
      }

      return false;
   }

   public void cleanupInactiveMiningData() {
      LocalDateTime now = LocalDateTime.now();
      Iterator<Entry<UUID, Integer>> iterator = this.playerMiningCount.entrySet().iterator();

      while (iterator.hasNext()) {
         Entry<UUID, Integer> entry = iterator.next();
         UUID playerId = entry.getKey();
         LocalDateTime lastMinedAt = this.lastMiningTime.getOrDefault(playerId, LocalDateTime.MIN);
         if (ChronoUnit.SECONDS.between(lastMinedAt, now) > 600L) {
            iterator.remove();
            this.lastMiningTime.remove(playerId);
         }
      }

      NekoAntiXray.getInstance().logDebug("已清理不活跃玩家的挖矿数据");
   }

   public void cleanupFakeOres() {
      Map<World, List<Location>> locationsByWorld = new HashMap<>();

      for (Location oreLocation : new ArrayList<>(this.fakeOreTypes.keySet())) {
         locationsByWorld.computeIfAbsent(oreLocation.getWorld(), world -> createLocationList()).add(oreLocation);
      }

      for (Entry<World, List<Location>> entry : locationsByWorld.entrySet()) {
         List<Location> worldLocations = entry.getValue();

         for (int startIndex = 0; startIndex < worldLocations.size(); startIndex += 16) {
            int endIndex = Math.min(startIndex + 16, worldLocations.size());
            List<Location> batch = worldLocations.subList(startIndex, endIndex);
            if (!batch.isEmpty()) {
               Location anchorLocation = batch.get(0);
               if (!this.plugin.isEnabled()) {
                  this.restoreFakeOreBatch(batch);
               } else {
                  this.foliaLib.getImpl().runAtLocation(anchorLocation, wrappedTask -> this.restoreFakeOreBatch(batch));
               }
            }
         }
      }

      this.fakeOreTypes.clear();
      this.oreMap.clear();
      this.plugin.logDebug("所有假矿石已清理完成");
   }

   private void restoreFakeOreBatch(List locations) {
      for (Object entry : locations) {
         Location oreLocation = (Location)entry;
         try {
            Block block = oreLocation.getBlock();
            if (block.getType() != Material.AIR) {
               Material replacementMaterial = oreLocation.getWorld().getEnvironment() == Environment.NETHER ? Material.NETHERRACK : Material.STONE;
               block.setType(replacementMaterial, false);
               this.plugin.logDebug(String.format("清理假矿石 - 位置: %s, %s -> %s", this.formatLocation(oreLocation), block.getType().name(), replacementMaterial));
            } else {
               this.plugin.logDebug(String.format("跳过已被挖掘的方块位置: %s", this.formatLocation(oreLocation)));
            }

            this.fakeOreTypes.remove(oreLocation);
         } catch (Exception ex) {
            this.plugin.getLogger().warning("清理假矿石时发生错误: " + ex.getMessage());
         }
      }
   }

   public void unregisterFakeOre(Location location) {
      this.fakeOreTypes.remove(location);

      for (List<Location> playerOres : this.oreMap.values()) {
         playerOres.remove(location);
      }
   }

   public void processFakeOreBreak(Player player, Block block) {
      if (block != null && player != null) {
         Location normalizedLocation = normalizeBlockLocation(block.getLocation());
         this.plugin.logDebug(String.format("检查方块破坏事件 - 玩家: %s, 位置: %s, 方块类型: %s", player.getName(), this.formatLocation(normalizedLocation), block.getType().name()));
         if (!this.isFakeOre(block)) {
            this.plugin.logDebug("方块不是假矿石，跳过处理");
         } else {
            this.foliaLib
               .getImpl()
               .runAtLocation(
                  normalizedLocation,
                  wrappedTask -> {
                     try {
                        this.handleFakeOreBreakOnRegion(player, block);
                     } catch (Exception ex) {
                        this.plugin
                           .getLogger()
                           .log(
                              Level.SEVERE,
                              String.format("处理假矿石破坏时发生错误 - 玩家: %s, 位置: %s, 错误: %s", player.getName(), this.formatLocation(normalizedLocation), ex.getMessage()),
                              ex
                           );
                     }
                  }
               );
         }
      } else {
         this.plugin.logDebug("无效的方块或玩家参数");
      }
   }

   private void handleFakeOreBreakOnRegion(Player player, Block block) {
      Location normalizedLocation = normalizeBlockLocation(block.getLocation());
      UUID playerId = player.getUniqueId();
      Material oreMaterial = this.fakeOreTypes.get(normalizedLocation);
      this.plugin.logDebug("\n=== 开始处理假矿石破坏 ===");
      this.plugin.logDebug(String.format("玩家: %s (UUID: %s)", player.getName(), playerId));
      this.plugin.logDebug(String.format("位置: %s", this.formatLocation(normalizedLocation)));
      this.plugin.logDebug(String.format("矿石类型: %s", oreMaterial));
      if (this.hasDetectionBypass(player)) {
         this.plugin.logDebug("玩家具有绕过权限，不进行处理");
         this.removeFakeOreWithLogging(normalizedLocation);
      } else {
         LocalDateTime now = LocalDateTime.now();
         if (this.isInViolationCooldown(playerId, now)) {
            this.plugin.logDebug(String.format("玩家在冷却时间内 - 上次违规时间: %s, 当前时间: %s", this.lastViolationTime.get(playerId), now));
            this.removeFakeOreWithLogging(normalizedLocation);
         } else {
            boolean ownFakeOre = this.isOwnFakeOre(playerId, normalizedLocation);
            this.plugin.logDebug("是否为自己放置的假矿石: " + ownFakeOre);
            double violationIncrement = this.calculateViolationIncrement(playerId, normalizedLocation);
            this.plugin.logDebug(String.format("计算得到的违规值: %.2f", violationIncrement));
            this.recordAndNotifyViolation(player, violationIncrement, normalizedLocation);
            this.updateViolationData(playerId, now);
            this.plugin.logDebug("违规数据已更新");
            this.removeFakeOreWithLogging(normalizedLocation);
            this.plugin.logDebug("=== 假矿石处理完成 ===\n");
         }
      }
   }

   private void removeFakeOreWithLogging(Location location) {
      this.plugin.logDebug(String.format("正在移除假矿石数据 - 位置: %s", this.formatLocation(location)));
      Material originalMaterial = this.fakeOreTypes.get(location);
      this.plugin.logDebug("原始矿石类型: " + originalMaterial);
      this.foliaLib.getImpl().runAtLocation(location, wrappedTask -> {
         try {
            Block block = location.getBlock();
            if (block.getType() != Material.AIR) {
               Material replacementMaterial = location.getWorld().getEnvironment() == Environment.NETHER ? Material.NETHERRACK : Material.STONE;
               block.setType(replacementMaterial, false);
               this.plugin.logDebug(String.format("方块已替换为 %s - 位置: %s", replacementMaterial, this.formatLocation(location)));
            } else {
               this.plugin.logDebug("方块已被挖掘成空气，不进行替换 - 位置: " + this.formatLocation(location));
            }

            this.fakeOreTypes.remove(location);
            this.playerPlacedOres.remove(location);
            int removedPlayerCount = 0;

            for (Entry<UUID, List<Location>> entry : this.oreMap.entrySet()) {
               if (entry.getValue().remove(location)) {
                  removedPlayerCount++;
                  this.plugin.logDebug(String.format("从玩家 %s 的假矿石列表中移除位置", Bukkit.getOfflinePlayer(entry.getKey()).getName()));
               }
            }

            this.plugin.logDebug("总共从 " + removedPlayerCount + " 个玩家的列表中移除了该位置");
         } catch (Exception ex) {
            this.plugin.getLogger().log(Level.SEVERE, String.format("移除假矿石时发生错误 - 位置: %s, 错误: %s", this.formatLocation(location), ex.getMessage()), ex);
         }
      });
   }

   public void decayPlayerViolations() {
      LocalDateTime now = LocalDateTime.now();
      long decreaseIntervalSeconds = NekoAntiXray.getInstance().getConfigManager().getViolationDecreaseInterval();
      double decreaseAmount = NekoAntiXray.getInstance().getConfigManager().getViolationDecreaseAmount();
      Iterator<Entry<UUID, Double>> iterator = this.playerViolations.entrySet().iterator();

      while (iterator.hasNext()) {
         Entry<UUID, Double> entry = iterator.next();
         UUID playerId = entry.getKey();
         double currentViolationLevel = entry.getValue();
         if (ChronoUnit.SECONDS.between(this.lastViolationTime.getOrDefault(playerId, LocalDateTime.MIN), now) > decreaseIntervalSeconds) {
            double updatedViolationLevel = Math.max(0.0, currentViolationLevel - decreaseAmount);
            if (updatedViolationLevel > 0.0) {
               entry.setValue(updatedViolationLevel);
               NekoAntiXray.getInstance().logDebug("玩家 " + Bukkit.getOfflinePlayer(playerId).getName() + " 的违规值已降低。新违规值: " + updatedViolationLevel);
            } else {
               iterator.remove();
               this.lastViolationTime.remove(playerId);
               NekoAntiXray.getInstance().logDebug("玩家 " + Bukkit.getOfflinePlayer(playerId).getName() + " 的违规值已清零并移除记录。");
            }
         }
      }
   }

   public void clearAllData() {
      this.oreMap.clear();
      this.playerPlacedOres.clear();
      this.blockBreak.clear();
      this.playerViolations.clear();
      this.fakeOreTypes.clear();
      this.playerMiningCount.clear();
      this.lastMiningTime.clear();
      this.lastViolationTime.clear();
      this.recentlyMinedBlocks.clear();
      this.internalBypassManager.clearAllBypasses();
   }

   public void shutdownExecutor() {
      if (this.executorService != null && !this.executorService.isShutdown()) {
         this.executorService.shutdown();

         try {
            if (!this.executorService.awaitTermination(5L, TimeUnit.SECONDS)) {
               this.executorService.shutdownNow();
            }
         } catch (InterruptedException ex) {
            this.executorService.shutdownNow();
            Thread.currentThread().interrupt();
         }
      }
   }

   public void clearPlayerData(UUID playerId) {
      String playerName = Bukkit.getOfflinePlayer(playerId).getName();
      if (playerName == null) {
         playerName = "未知玩家";
      }

      this.plugin.logDebug("clearPlayerData: === 开始清除玩家 " + playerName + " (UUID: " + playerId + ") 的数据 ===");

      try {
         boolean removedViolations = this.playerViolations.remove(playerId) != null;
         this.plugin.logDebug("clearPlayerData: playerViolations.remove(" + playerId + ") 执行结果 (是否移除了元素): " + removedViolations);
         boolean removedLastViolationTime = this.lastViolationTime.remove(playerId) != null;
         this.plugin.logDebug("clearPlayerData: lastViolationTime.remove(" + playerId + ") 执行结果: " + removedLastViolationTime);
         this.violationTracker.clearViolations(playerId);
         this.plugin.logDebug("clearPlayerData: violationTracker.clearViolations(" + playerId + ") 已调用");
         this.blockBreak.remove(playerId);
         this.plugin.logDebug("clearPlayerData: blockBreak removed for " + playerId);
         this.playerMiningCount.remove(playerId);
         this.plugin.logDebug("clearPlayerData: playerMiningCount removed for " + playerId);
         this.lastMiningTime.remove(playerId);
         this.plugin.logDebug("clearPlayerData: lastMiningTime removed for " + playerId);
         this.lastFakeOreGenerationTime.remove(playerId);
         this.plugin.logDebug("clearPlayerData: lastFakeOreGenerationTime removed for " + playerId);
         this.fakeOreBreakCount.remove(playerId);
         this.plugin.logDebug("clearPlayerData: fakeOreBreakCount removed for " + playerId);
         this.lastFakeOreBreakTime.remove(playerId);
         this.plugin.logDebug("clearPlayerData: lastFakeOreBreakTime removed for " + playerId);
         this.recentlyMinedBlocks.remove(playerId);
         this.plugin.logDebug("clearPlayerData: recentlyMinedBlocks removed for " + playerId);
         this.plugin.logDebug("clearPlayerData: 其他相关数据已调用 remove");
         List<Location> playerOres = this.oreMap.get(playerId);
         if (playerOres != null && !playerOres.isEmpty()) {
            this.plugin.logDebug("clearPlayerData: 准备清理玩家 " + playerId + " 相关的 " + playerOres.size() + " 个假矿石位置");
            List<Location> oresCopy = new ArrayList<>(playerOres);
            int removedFakeOreCount = 0;

            for (Location oreLocation : oresCopy) {
               if (this.fakeOreTypes.remove(oreLocation) != null) {
                  removedFakeOreCount++;
               }
            }

            this.plugin.logDebug("clearPlayerData: 从 fakeOreTypes 中移除了 " + removedFakeOreCount + " 个条目");
            boolean removedOreMapEntry = this.oreMap.remove(playerId) != null;
            this.plugin.logDebug("clearPlayerData: oreMap.remove(" + playerId + ") 执行结果 (是否移除了元素): " + removedOreMapEntry);
         } else {
            this.plugin.logDebug("clearPlayerData: 玩家 " + playerId + " 没有关联的假矿石位置需要清理 (oreMap value is null or empty)");
            this.oreMap.remove(playerId);
         }

         this.internalBypassManager.clearPlayerData(playerId);
         this.plugin.logDebug("clearPlayerData: internalBypassManager 清理完成");
         this.plugin.logDebug("clearPlayerData: === 清除玩家 " + playerName + " (UUID: " + playerId + ") 的数据完成 ===");
      } catch (Exception ex) {
         this.plugin.getLogger().log(Level.SEVERE, "clearPlayerData: 清除玩家 " + playerId + " 数据时发生异常", ex);
      }
   }

   private boolean isBlacklistedMaterial(Material material) {
      return this.plugin.getConfigManager().getBlacklistedOres().contains(material);
   }

   private boolean isBlacklistedWorld(String worldName) {
      return this.plugin.getConfigManager().getBlacklistWorlds().contains(worldName);
   }

   private boolean hasDetectionBypass(Player player) {
      boolean hasPermissionBypass = player.hasPermission("nekoantixray.bypass");
      boolean hasInternalBypass = this.internalBypassManager.hasBypass(player);
      boolean bypassed = hasPermissionBypass || hasInternalBypass;
      if (bypassed && this.configManager.isDebug()) {
         String bypassReason = hasPermissionBypass ? (hasInternalBypass ? "权限豁免+内部豁免" : "权限豁免") : "内部豁免";
         this.plugin.logDebug("玩家 " + player.getName() + " 拥有检测豁免: " + bypassReason);
      }

      return bypassed;
   }

   public boolean hasReachedContinuousViolationThreshold(List violationTimestamps) {
      return violationTimestamps.size() >= NekoAntiXray.getInstance().getConfigManager().getCheckXrayVlContinuousIncreaseCount();
   }

   public void notifyAdminsContinuousViolation(Player player) {
      UUID playerId = player.getUniqueId();
      double violationLevel = this.playerViolations.getOrDefault(playerId, 0.0);
      Location playerLocation = player.getLocation();
      String formattedCoords = String.format("X: %.2f, Y: %.2f, Z: %.2f", playerLocation.getX(), playerLocation.getY(), playerLocation.getZ());
      String worldName = playerLocation.getWorld().getName();
      String playerName = player.getName();
      CrossServerNotificationManager.sendCrossServerAlert(playerName, formattedCoords, worldName, violationLevel);
      NekoAntiXray.getInstance().logDebug("已通知管理员关于玩家 " + player.getName() + " 的持续违规行为");
   }

   private String formatLocation(Location location) {
      return String.format("X: %.2f, Y: %.2f, Z: %.2f", location.getX(), location.getY(), location.getZ());
   }

   public void cleanupAdjacentFakeOres(Block block) {
      Block[] adjacentBlocks = new Block[]{
         block.getRelative(BlockFace.NORTH),
         block.getRelative(BlockFace.SOUTH),
         block.getRelative(BlockFace.EAST),
         block.getRelative(BlockFace.WEST),
         block.getRelative(BlockFace.UP),
         block.getRelative(BlockFace.DOWN)
      };

      for (Block adjacentBlock : adjacentBlocks) {
         if (this.isFakeOre(adjacentBlock)) {
            if (adjacentBlock.getType() != Material.AIR) {
               Material replacementMaterial = this.getNaturalReplacementMaterial(adjacentBlock.getLocation());
               adjacentBlock.setType(replacementMaterial, false);
               this.plugin.logDebug(String.format("由于相邻方块被破坏，替换假矿石: %s -> %s", adjacentBlock.getType().name(), replacementMaterial));
            } else {
               this.plugin.logDebug("由于相邻方块被破坏，跳过已被挖掘的假矿石: " + adjacentBlock.getLocation());
            }

            this.unregisterFakeOre(adjacentBlock.getLocation());
            this.plugin.logDebug("由于相邻方块被破坏，移除假矿石数据: " + adjacentBlock.getLocation());
         }
      }
   }

   private Material getNaturalReplacementMaterial(Location location) {
      World world = location.getWorld();
      Environment environment = world.getEnvironment();
      int blockY = location.getBlockY();
      switch (environment) {
         case NETHER:
            if (blockY <= 30) {
               return Material.BASALT;
            } else {
               if (blockY >= 100) {
                  return Material.BLACKSTONE;
               }

               return Material.NETHERRACK;
            }
         case THE_END:
            return Material.END_STONE;
         case NORMAL:
         default:
            if (blockY <= 0) {
               return Material.DEEPSLATE;
            } else if (blockY <= 16) {
               return ThreadLocalRandom.current().nextDouble() < 0.7 ? Material.DEEPSLATE : Material.STONE;
            } else {
               double roll = ThreadLocalRandom.current().nextDouble();
               if (roll < 0.15) {
                  return Material.ANDESITE;
               } else if (roll < 0.3) {
                  return Material.DIORITE;
               } else if (roll < 0.45) {
                  return Material.GRANITE;
               } else {
                  return roll < 0.55 && blockY <= 64 ? Material.TUFF : Material.STONE;
               }
            }
      }
   }

   public double getPlayerViolationLevel(Player player) {
      return this.playerViolations.getOrDefault(player.getUniqueId(), 0.0);
   }

   public void addPlayerViolation(Player player, double violationIncrement) {
      if (player != null && !(violationIncrement <= 0.0)) {
         this.addViolation(player, violationIncrement);
      }
   }

   private void generateMultipleAncientDebrisClustersAsync(Player player, Block block, Material material) {
      ConfigManager config = this.plugin.getConfigManager();
      int clusterAttempts = config.getCheckXraySpawnOre();
      if (clusterAttempts <= 0) {
         this.plugin.logDebug("远古残骸集群尝试次数配置为0或负数，跳过生成");
      } else {
         String debugHeader = String.format(
            "\n=== 开始异步生成多个远古残骸集群 ===\n玩家: %s\n原始位置: %s\n尝试生成集群数量: %d\n矿石类型: %s", player.getName(), this.formatLocation(block.getLocation()), clusterAttempts, material
         );
         this.plugin.logDebug(debugHeader);
         List<CompletableFuture<?>> clusterFutures = new ArrayList<>();
         Location originLocation = block.getLocation();

         for (int i = 0; i < clusterAttempts; i++) {
            CompletableFuture clusterFuture = this.tryGenerateOneAncientDebrisClusterAsync(player, originLocation, material, i + 1, clusterAttempts);
            clusterFutures.add(clusterFuture);
         }

         CompletableFuture.allOf(clusterFutures.toArray(new CompletableFuture[0]))
            .thenApplyAsync(ignored -> this.collectValidAncientDebrisClusters(clusterFutures), this.executorService)
            .thenAcceptAsync(validClusters -> {
               this.plugin.logDebug(String.format("远古残骸多集群生成尝试完成 - 成功生成 %d / %d 个集群", validClusters.size(), clusterAttempts));
               if (!validClusters.isEmpty()) {
                  this.foliaLib.getImpl().runAtLocation(originLocation, wrappedTask -> this.finalizeAndCleanupMultipleClusters(player, validClusters, material));
               } else {
                  this.plugin.logDebug("未能生成任何有效的远古残骸假矿集群");
               }
            }, this.executorService);
      }
   }

   private CompletableFuture tryGenerateOneAncientDebrisClusterAsync(Player player, Location baseLocation, Material material, int attemptIndex, int totalAttempts) {
      ConfigManager config = this.plugin.getConfigManager();
      String debugHeader = String.format("\n--- 尝试生成远古残骸集群 [%d/%d] ---\n玩家: %s\n基础位置: %s\n矿石类型: %s", attemptIndex, totalAttempts, player.getName(), this.formatLocation(baseLocation), material);
      this.plugin.logDebug(debugHeader);
      CompletableFuture locationFuture = new CompletableFuture();
      this.foliaLib.getImpl().runAtLocation(baseLocation, wrappedTask -> this.findValidFakeOreLocation(player, baseLocation, config, locationFuture, material));
      return locationFuture.thenComposeAsync(startLocation -> this.buildAncientDebrisClusterFromStartLocation(attemptIndex, totalAttempts, player, material, (Location)startLocation), this.executorService);
   }

   private List buildAncientDebrisClusterSync(Player player, Location startLocation, Material material) {
      List<Location> clusterLocations = new ArrayList<>();
      Set<Location> visitedLocations = new HashSet<>();
      int targetSize = ThreadLocalRandom.current().nextInt(1, 4);
      this.plugin.logDebug(String.format("开始同步构建远古残骸集群 - 起始: %s, 目标大小: %d", this.formatLocation(startLocation), targetSize));
      if (!this.isValidFakeOreLocationSync(startLocation, player, material)) {
         this.plugin.logDebug("警告：起始位置无效: " + this.formatLocation(startLocation));
         return Collections.emptyList();
      }

      clusterLocations.add(startLocation);
      visitedLocations.add(startLocation);
      this.plugin.logDebug(String.format("添加起始位置: %s (当前大小: %d/%d)", this.formatLocation(startLocation), clusterLocations.size(), targetSize));
      if (targetSize == 1) {
         this.plugin.logDebug("远古残骸集群同步构建完成 - 最终大小: 1/1");
         return clusterLocations;
      }

      Location secondLocation = null;
      BlockFace secondBlockFace = null;
      List<BlockFace> shuffledFaces = new ArrayList<>(Arrays.asList(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST));
      Collections.shuffle(shuffledFaces);

      for (BlockFace candidateFace : shuffledFaces) {
         Location adjacentLocation = startLocation.getBlock().getRelative(candidateFace).getLocation();
         if (!visitedLocations.contains(adjacentLocation) && this.isValidFakeOreLocationSync(adjacentLocation, player, material)) {
            secondLocation = adjacentLocation;
            secondBlockFace = candidateFace;
            clusterLocations.add(secondLocation);
            visitedLocations.add(secondLocation);
            this.plugin.logDebug(String.format("添加第二个位置: %s (Face: %s, 当前大小: %d/%d)", this.formatLocation(secondLocation), secondBlockFace, clusterLocations.size(), targetSize));
            break;
         }
      }

      if (secondLocation == null) {
         this.plugin.logDebug("未能为集群找到第二个有效位置，最终大小: 1");
      } else if (targetSize == 2) {
         this.plugin.logDebug("远古残骸集群同步构建完成 - 最终大小: 2/2");
      }

      if (targetSize == 3 && secondLocation != null && secondBlockFace != null) {
         Location thirdLocation = null;
         List<Location> thirdBlockCandidates = new ArrayList<>();
         Location extendedLocation = secondLocation.getBlock().getRelative(secondBlockFace).getLocation();
         if (!visitedLocations.contains(extendedLocation)) {
            thirdBlockCandidates.add(extendedLocation);
         }

         BlockFace oppositeFace = secondBlockFace.getOppositeFace();

         for (BlockFace sideFace : shuffledFaces) {
            if (sideFace != secondBlockFace && sideFace != oppositeFace) {
               Location sideLocation = startLocation.getBlock().getRelative(sideFace).getLocation();
               if (!visitedLocations.contains(sideLocation)) {
                  thirdBlockCandidates.add(sideLocation);
               }
            }
         }

         Collections.shuffle(thirdBlockCandidates);

         for (Location candidateLocation : thirdBlockCandidates) {
            if (this.isValidFakeOreLocationSync(candidateLocation, player, material)) {
               thirdLocation = candidateLocation;
               this.plugin.logDebug(String.format("通过 '间隔'/'侧向' 逻辑找到第三个位置: %s", this.formatLocation(thirdLocation)));
               break;
            }
         }

         if (thirdLocation == null) {
            this.plugin.logDebug("未找到合适的'间隔'位置，尝试寻找与第二个位置相邻的第三个位置");

            for (BlockFace adjacentFace : shuffledFaces) {
               Location neighborLocation = secondLocation.getBlock().getRelative(adjacentFace).getLocation();
               if (!visitedLocations.contains(neighborLocation) && this.isValidFakeOreLocationSync(neighborLocation, player, material)) {
                  thirdLocation = neighborLocation;
                  this.plugin.logDebug(String.format("通过 '邻近第二个' 逻辑找到第三个位置: %s", this.formatLocation(thirdLocation)));
                  break;
               }
            }
         }

         if (thirdLocation != null) {
            clusterLocations.add(thirdLocation);
            visitedLocations.add(thirdLocation);
            this.plugin.logDebug(String.format("添加第三个位置: %s (当前大小: %d/%d)", this.formatLocation(thirdLocation), clusterLocations.size(), targetSize));
         } else {
            this.plugin.logDebug("未能为集群找到第三个有效位置，最终大小: 2");
         }

         this.plugin.logDebug(String.format("远古残骸集群同步构建完成 - 最终大小: %d/%d", clusterLocations.size(), targetSize));
      }

      if (clusterLocations.size() > 1) {
         boolean isPurelyVertical = true;
         Location referenceLocation = clusterLocations.get(0);

         for (int i = 1; i < clusterLocations.size(); i++) {
            Location currentLocation = clusterLocations.get(i);
            if (currentLocation.getBlockX() != referenceLocation.getBlockX() || currentLocation.getBlockZ() != referenceLocation.getBlockZ()) {
               isPurelyVertical = false;
               break;
            }
         }

         if (isPurelyVertical) {
            this.plugin.logDebug(String.format("拒绝纯垂直集群 - 起始: %s, 大小: %d", this.formatLocation(startLocation), clusterLocations.size()));
            return Collections.emptyList();
         }
      }

      return clusterLocations;
   }

   private void finalizeAndCleanupMultipleClusters(Player player, List clusters, Material material) {
      if (clusters != null && !clusters.isEmpty()) {
         UUID playerId = player.getUniqueId();
         List<Location> playerOres = this.oreMap.computeIfAbsent(playerId, key -> new ArrayList<>());
         List<Location> allLocations = ((List<?>)clusters)
            .stream()
            .flatMap(cluster -> ((Collection<?>)cluster).stream().map(Location.class::cast))
            .collect(Collectors.toList());
         if (allLocations.isEmpty()) {
            this.plugin.logDebug("所有集群的位置列表为空，无需最终化或清理");
         } else {
            String finalizeLog = String.format("\n=== 开始最终化 %d 个集群 (%d 个总方块) 的生成 ===\n玩家: %s\n矿石类型: %s", clusters.size(), allLocations.size(), player.getName(), material);
            this.plugin.logDebug(finalizeLog);

            for (Location location : allLocations) {
               Location normalizedLocation = normalizeBlockLocation(location);
               Block block = normalizedLocation.getBlock();
               block.setType(material, false);
               this.fakeOreTypes.put(normalizedLocation, material);
               playerOres.add(normalizedLocation);
               this.plugin.logDebug(String.format("生成假矿石: %s -> %s", this.formatLocation(normalizedLocation), material));
            }

            this.plugin.logDebug("方块放置完成，数据已记录");
            long delayTicks = this.plugin.getConfigManager().getCheckXraySpawnTime() * 20L;
            Location anchorLocation = allLocations.get(0);
            String scheduleLog = String.format(
               "\n=== 设置 %d 个集群 (%d 个总方块) 的清理任务 ===\n玩家: %s\n延迟: %d ticks\n集群数量: %d\n总方块数: %d\n调度位置: %s",
               clusters.size(),
               allLocations.size(),
               player.getName(),
               delayTicks,
               clusters.size(),
               allLocations.size(),
               this.formatLocation(anchorLocation)
            );
            this.plugin.logDebug(scheduleLog);
            this.foliaLib.getImpl().runAtLocationLater(anchorLocation, wrappedTask -> {
               this.plugin.logDebug(String.format("\n=== 开始执行 %d 个集群 (%d 个总方块) 的清理任务 ===", clusters.size(), allLocations.size()));
               int replacedBlockCount = 0;
               Map<Material, Integer> replacementCounts = new HashMap<>();

               for (Location oreLocation : allLocations) {
                  try {
                     Block targetBlock = oreLocation.getBlock();
                     Material currentType = targetBlock.getType();
                     if (currentType != Material.AIR) {
                        Material replacementMaterial = this.getNaturalReplacementMaterial(oreLocation);
                        targetBlock.setType(replacementMaterial, false);
                        replacementCounts.merge(replacementMaterial, 1, Integer::sum);
                        this.plugin.logDebug(String.format("清理假矿石: %s, %s -> %s", this.formatLocation(oreLocation), currentType, replacementMaterial));
                        replacedBlockCount++;
                     } else {
                        this.plugin.logDebug(String.format("跳过已被挖掘的假矿石位置: %s", this.formatLocation(oreLocation)));
                     }

                     this.fakeOreTypes.remove(oreLocation);
                  } catch (Exception ex) {
                     this.plugin.getLogger().warning(String.format("清理假矿石时发生错误 - 位置: %s, 错误: %s", this.formatLocation(oreLocation), ex.getMessage()));
                  }
               }

               List<Location> trackedPlayerOres = this.oreMap.get(playerId);
               int removedFromPlayerCount = 0;
               if (trackedPlayerOres != null) {
                  int sizeBeforeRemoval = trackedPlayerOres.size();
                  trackedPlayerOres.removeAll(allLocations);
                  removedFromPlayerCount = sizeBeforeRemoval - trackedPlayerOres.size();
                  if (trackedPlayerOres.isEmpty()) {
                     this.oreMap.remove(playerId);
                  }
               }

               this.plugin.logDebug(String.format("\n=== %d 个集群清理统计 ===", clusters.size()));
               this.plugin.logDebug(String.format("总清理位置数 (尝试): %d", allLocations.size()));
               this.plugin.logDebug(String.format("实际替换方块数量: %d", replacedBlockCount));
               this.plugin.logDebug(String.format("从玩家 %s 数据中移除: %d", Bukkit.getOfflinePlayer(playerId).getName(), removedFromPlayerCount));
               this.plugin.logDebug("替换方块统计:");
               replacementCounts.forEach((replacedMaterial, count) -> this.logReplacedClusterMaterialCount(replacedMaterial, count));
               this.plugin.logDebug(String.format("剩余fakeOreTypes大小: %d", this.fakeOreTypes.size()));
               this.plugin.logDebug("=== 清理任务完成 ===\n");
            }, delayTicks);
         }
      } else {
         this.plugin.logDebug("没有有效的集群可以最终化或清理");
      }
   }

   private void removeFakeOreSilently(Location location) {
      Location normalizedLocation = normalizeBlockLocation(location);
      this.fakeOreTypes.remove(normalizedLocation);
      this.decoyOreOwners.remove(normalizedLocation);
      this.playerPlacedOres.remove(normalizedLocation);

      for (List<Location> playerOres : this.oreMap.values()) {
         playerOres.remove(normalizedLocation);
      }
   }

   public static final class DecoyPlacementResult {
      final Location location;
      final String directionLabel;
      final int distance;

      DecoyPlacementResult(Location location, String directionLabel, int distance) {
         this.location = location;
         this.directionLabel = directionLabel;
         this.distance = distance;
      }
   }

   public static final class EnvironmentSwitchMap {
      static final int[] ENVIRONMENT_ORDINAL_MAP = new int[Environment.values().length];

      static {
         try {
            ENVIRONMENT_ORDINAL_MAP[Environment.NETHER.ordinal()] = 1;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            ENVIRONMENT_ORDINAL_MAP[Environment.THE_END.ordinal()] = 2;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            ENVIRONMENT_ORDINAL_MAP[Environment.NORMAL.ordinal()] = 3;
         } catch (NoSuchFieldError ignored) {
         }
      }
   }

   private static final class OreCluster {
      final List<Location> locations;
      final Material type;

      OreCluster(List<Location> locations, Material type) {
         this.locations = locations;
         this.type = type;
      }
   }

   private static final class PendingDecoyRequest {
      final Material oreMaterial;
      final int distance;

      PendingDecoyRequest(Material oreMaterial, int distance) {
         this.oreMaterial = oreMaterial;
         this.distance = distance;
      }
   }

   public class ViolationTracker {
      private static int violationCounter = 0;
      private Map<UUID, List<LocalDateTime>> playerViolationTimestamps = new ConcurrentHashMap<>();

      private static boolean isViolationExpired(LocalDateTime cutoffTime, LocalDateTime timestamp) {
         return timestamp.isBefore(cutoffTime);
      }

      public void addViolation(Player player, double violationIncrement) {
         UUID playerId = player.getUniqueId();
         List<LocalDateTime> timestamps = this.playerViolationTimestamps.computeIfAbsent(playerId, key -> new ArrayList<>());
         timestamps.add(LocalDateTime.now());
         String playerName = player.getName();
         int continuousCount = timestamps.size();
         XrayDetector.this.plugin.logDebug("玩家 " + playerName + " 添加违规记录，当前连续违规次数: " + continuousCount + "，违规值增加: " + violationIncrement);
         String detectionReason = "连续违规次数: " + timestamps.size();
         XrayDetector.this.plugin.logDetectionEvent(player, "Xray", violationIncrement, detectionReason);
         if (XrayDetector.this.hasReachedContinuousViolationThreshold(timestamps)) {
            XrayDetector.this.plugin.logDebug("玩家 " + player.getName() + " 达到连续违规阈值，通知管理员");
            XrayDetector.this.notifyAdminsContinuousViolation(player);
         }

         LocalDateTime cutoffTime = LocalDateTime.now().minusSeconds(XrayDetector.this.plugin.getConfigManager().getCheckXrayVlContinuousIncreaseInterval());
         timestamps.removeIf(timestamp -> isViolationExpired(cutoffTime, timestamp));
      }

      public void clearViolations(UUID playerId) {
         this.playerViolationTimestamps.remove(playerId);
         XrayDetector.this.plugin.logDebug("玩家 " + Bukkit.getOfflinePlayer(playerId).getName() + " 的违规记录已被清除");
      }
   }
}
