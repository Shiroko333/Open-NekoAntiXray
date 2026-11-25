package sakuyaayane.nekoantixray;

import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public final class ProbeGuardChecker {
   private static final BlockState AIR = Blocks.AIR.defaultBlockState();
   private static final double DEG_TO_RAD = Math.PI / 180.0;
   private static final double RAY_PARALLEL_EPSILON = 1.0E-7;
   private final NekoAntiXray plugin;
   private final ConcurrentMap<UUID, ProbeGuardChecker.PlayerProbeState> states = new ConcurrentHashMap<>();

   public ProbeGuardChecker(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   public void onMovement(Player player, boolean hasPosition, double posX, double posY, double posZ, boolean hasRotation, float yaw, float pitch, String worldName) {
      if (player != null && worldName != null) {
         ProbeGuardChecker.PlayerProbeState playerProbeState = this.states.computeIfAbsent(player.getUniqueId(), uuid -> new ProbeGuardChecker.PlayerProbeState());
         synchronized (playerProbeState) {
            if (!worldName.equals(playerProbeState.world)) {
               playerProbeState.resetForWorld(worldName);
            }

            if (hasPosition) {
               playerProbeState.posX = posX;
               playerProbeState.posY = posY;
               playerProbeState.posZ = posZ;
               playerProbeState.eyeY = posY + this.getEyeHeightSafe(player);
               playerProbeState.positionSynced = true;
            }

            if (hasRotation) {
               playerProbeState.yaw = yaw;
               playerProbeState.pitch = pitch;
               playerProbeState.rotationSynced = true;
            }
         }
      }
   }

   public void removePlayerState(UUID playerId) {
      if (playerId != null) {
         this.states.remove(playerId);
      }
   }

   public ProbeGuardChecker.CheckResult check(Player player, DiggingAction diggingAction, int blockX, int blockY, int blockZ, String worldName) {
      if (player == null || diggingAction == null || worldName == null) {
         return ProbeGuardChecker.CheckResult.ALLOWED;
      }

      if (diggingAction != DiggingAction.START_DIGGING && diggingAction != DiggingAction.CANCELLED_DIGGING && diggingAction != DiggingAction.FINISHED_DIGGING) {
         return ProbeGuardChecker.CheckResult.ALLOWED;
      }

      if (player.hasPermission("nekoantixray.probeguard.bypass")) {
         return ProbeGuardChecker.CheckResult.ALLOWED;
      }

      ConfigManager configManager = this.plugin.getConfigManager();
      ProbeGuardChecker.PlayerProbeState playerProbeState = this.states.computeIfAbsent(player.getUniqueId(), uuid -> new ProbeGuardChecker.PlayerProbeState());
      long nowMs = System.currentTimeMillis();
      synchronized (playerProbeState) {
         if (!worldName.equals(playerProbeState.world)) {
            playerProbeState.resetForWorld(worldName);
         }

         boolean isStateSynced = playerProbeState.positionSynced && playerProbeState.rotationSynced;
         if (isStateSynced) {
            if (diggingAction == DiggingAction.START_DIGGING) {
               double effectiveMaxReach = this.getEffectiveMaxReach(player, configManager);
               boolean checkCrosshair = configManager.isCheckXrayProbeGuardCheckCrosshair();
               boolean crosshairAiming = true;
               if (checkCrosshair) {
                  double maxRayDistance = effectiveMaxReach + 1.0;
                  double crosshairTolerance = configManager.getCheckXrayProbeGuardCrosshairTolerance();
                  if (!this.isCrosshairAimingAtBlock(playerProbeState, blockX, blockY, blockZ, maxRayDistance, crosshairTolerance)) {
                     crosshairAiming = false;
                  }
               }

               if (crosshairAiming && configManager.isCheckXrayProbeGuardCheckLineOfSight()) {
                  boolean onlyObfuscated = configManager.isCheckXrayProbeGuardOnlyObfuscatedPlayers();
                  if (!onlyObfuscated || this.isPlayerObfuscated(player, configManager)) {
                     this.hasLineOfSight(player, playerProbeState, blockX, blockY, blockZ);
                  }
               }
            } else if (playerProbeState.hasActiveDigTarget
               && playerProbeState.digTargetX == blockX
               && playerProbeState.digTargetY == blockY
               && playerProbeState.digTargetZ == blockZ) {
               long abortGraceMs = configManager.getCheckXrayProbeGuardAbortGraceMs();
               if (abortGraceMs > 0L && nowMs - playerProbeState.digStartTimeMs < configManager.getCheckXrayProbeGuardAbortGraceMs()) {
               }
            }
         }

         updateDiggingState(playerProbeState, diggingAction, blockX, blockY, blockZ, nowMs);
         return ProbeGuardChecker.CheckResult.ALLOWED;
      }
   }

   private static void updateDiggingState(ProbeGuardChecker.PlayerProbeState playerProbeState, DiggingAction diggingAction, int blockX, int blockY, int blockZ, long nowMs) {
      if (diggingAction == DiggingAction.START_DIGGING) {
         playerProbeState.digTargetX = blockX;
         playerProbeState.digTargetY = blockY;
         playerProbeState.digTargetZ = blockZ;
         playerProbeState.hasActiveDigTarget = true;
         playerProbeState.digStartTimeMs = nowMs;
      } else {
         playerProbeState.hasActiveDigTarget = false;
      }
   }

   private boolean isPlayerObfuscated(Player player, ConfigManager configManager) {
      if (!configManager.isHeavyModeEnabled()) {
         return false;
      }

      if (!configManager.isHeavyModeDynamic()) {
         return true;
      }

      double playerViolationLevel = this.plugin.getXrayDetector().getPlayerViolationLevel(player);
      double cancelBaselineVl = this.plugin.isObfuscationCancelled(player) ? this.plugin.getObfuscationCancelBaselineVl(player) : 0.0;
      return playerViolationLevel >= cancelBaselineVl + configManager.getCheckXrayHeavyModeTriggerVl();
   }

   public String formatPlayerDebugStatus(Player player) {
      ProbeGuardChecker.PlayerProbeState playerProbeState = this.states.get(player.getUniqueId());
      if (playerProbeState == null) {
         return "无移动数据(尚未收到移动包)";
      }

      ConfigManager configManager = this.plugin.getConfigManager();
      synchronized (playerProbeState) {
         return String.format(
            "world=%s pos=(%.2f, %.2f, %.2f) eyeY=%.2f yaw=%.1f pitch=%.1f 已同步=%s/%s 目标=%s 手长=%.2f 混淆=%s VL=%.1f",
            playerProbeState.world,
            playerProbeState.posX,
            playerProbeState.posY,
            playerProbeState.posZ,
            playerProbeState.eyeY,
            playerProbeState.yaw,
            playerProbeState.pitch,
            playerProbeState.positionSynced,
            playerProbeState.rotationSynced,
            playerProbeState.hasActiveDigTarget ? playerProbeState.digTargetX + "," + playerProbeState.digTargetY + "," + playerProbeState.digTargetZ : "无",
            this.getEffectiveMaxReach(player, configManager),
            this.isPlayerObfuscated(player, configManager),
            this.plugin.getXrayDetector().getPlayerViolationLevel(player)
         );
      }
   }

   private boolean recordInvalidAndCheckThreshold(ProbeGuardChecker.PlayerProbeState playerProbeState, long nowMs, ConfigManager configManager) {
      long invalidWindowMs = configManager.getCheckXrayProbeGuardInvalidWindowMs();
      if (playerProbeState.invalidWindowStartMs == 0L || nowMs - playerProbeState.invalidWindowStartMs > invalidWindowMs) {
         playerProbeState.invalidWindowStartMs = nowMs;
         playerProbeState.invalidCountInWindow = 0;
      }

      playerProbeState.invalidCountInWindow++;
      return playerProbeState.invalidCountInWindow >= configManager.getCheckXrayProbeGuardMinInvalidPerWindow();
   }

   private double getEffectiveMaxReach(Player player, ConfigManager configManager) {
      double baseReach;
      if (configManager.isCheckXrayProbeGuardUseGamemodeReach()) {
         double attributeRange = this.getAttributeInteractionRange(player);
         baseReach = attributeRange > 0.0 ? attributeRange : this.getGamemodeDefaultReach(player);
      } else {
         baseReach = configManager.getCheckXrayProbeGuardMaxReach();
      }

      return baseReach + configManager.getCheckXrayProbeGuardReachTolerance();
   }

   private double getAttributeInteractionRange(Player player) {
      try {
         if (player instanceof CraftPlayer craftPlayer) {
            return craftPlayer.getHandle().blockInteractionRange();
         }
      } catch (Throwable ex) {
      }

      return -1.0;
   }

   private double getGamemodeDefaultReach(Player player) {
      try {
         return player.getGameMode() == GameMode.CREATIVE ? 5.0 : 4.5;
      } catch (Throwable ex) {
         return 4.5;
      }
   }

   private boolean hasLineOfSight(Player player, ProbeGuardChecker.PlayerProbeState playerProbeState, int blockX, int blockY, int blockZ) {
      PlayerRayTraceState playerRayTraceState = (PlayerRayTraceState)this.plugin.getPlayerRayTraceStates().get(player.getUniqueId());
      if (playerRayTraceState == null) {
         return true;
      }

      boolean[] occludingStates = this.getOccludingStates(player);
      if (occludingStates == null) {
         return true;
      }

      ProbeGuardChecker.ChunkOcclusionProbe chunkOcclusionProbe = new ProbeGuardChecker.ChunkOcclusionProbe(playerRayTraceState.getChunks(), occludingStates);
      VoxelRayIterator rayIterator = new VoxelRayIterator(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
      VoxelRayTracer voxelRayTracer = new VoxelRayTracer(rayIterator::initializeNormalized, chunkOcclusionProbe, true);
      double[] lookDirection = computeLookDirection(playerProbeState.yaw, playerProbeState.pitch);
      boolean blockVisible = voxelRayTracer.isBlockVisible(
         blockX, blockY, blockZ, playerProbeState.posX, playerProbeState.eyeY, playerProbeState.posZ, lookDirection[0], lookDirection[1], lookDirection[2]
      );
      return chunkOcclusionProbe.missingChunkData ? true : blockVisible;
   }

   private boolean[] getOccludingStates(Player player) {
      try {
         ServerLevel handle = ((CraftWorld)player.getWorld()).getHandle();
         if (handle.chunkPacketBlockController instanceof EnhancedChunkObfuscationController enhancedChunkObfuscationController) {
            return enhancedChunkObfuscationController.solidGlobal;
         }
      } catch (Throwable ex) {
      }

      return null;
   }

   private boolean isCrosshairAimingAtBlock(
      ProbeGuardChecker.PlayerProbeState playerProbeState, int blockX, int blockY, int blockZ, double maxRayDistance, double tolerance
   ) {
      double[] lookDirection = computeLookDirection(playerProbeState.yaw, playerProbeState.pitch);
      double originX = playerProbeState.posX;
      double originY = playerProbeState.eyeY;
      double originZ = playerProbeState.posZ;
      double dirX = lookDirection[0];
      double dirY = lookDirection[1];
      double dirZ = lookDirection[2];
      double minX = blockX - tolerance;
      double minY = blockY - tolerance;
      double minZ = blockZ - tolerance;
      double maxX = blockX + 1.0 + tolerance;
      double maxY = blockY + 1.0 + tolerance;
      double maxZ = blockZ + 1.0 + tolerance;
      return intersectsRayAabb(originX, originY, originZ, dirX, dirY, dirZ, minX, minY, minZ, maxX, maxY, maxZ, maxRayDistance);
   }

   private static double distanceSquaredToBlock(double pointX, double pointY, double pointZ, int blockX, int blockY, int blockZ) {
      double dx = pointX - clamp(pointX, blockX, blockX + 1.0);
      double dy = pointY - clamp(pointY, blockY, blockY + 1.0);
      double dz = pointZ - clamp(pointZ, blockZ, blockZ + 1.0);
      return dx * dx + dy * dy + dz * dz;
   }

   private static double clamp(double coord, double minBound, double maxBound) {
      return coord < minBound ? minBound : (coord > maxBound ? maxBound : coord);
   }

   private static double[] computeLookDirection(float yaw, float pitch) {
      double yawRad = yaw * (Math.PI / 180.0);
      double pitchRad = pitch * (Math.PI / 180.0);
      double cosPitch = Math.cos(pitchRad);
      return new double[]{-Math.sin(yawRad) * cosPitch, -Math.sin(pitchRad), Math.cos(yawRad) * cosPitch};
   }

   private static double[] intersectRayAxisSlab(double origin, double direction, double minBound, double maxBound, double tMin, double tMax) {
      if (!(Math.abs(direction) < 1.0E-7)) {
         double invDirection = 1.0 / direction;
         double tNear = (minBound - origin) * invDirection;
         double tFar = (maxBound - origin) * invDirection;
         if (tNear > tFar) {
            double swap = tNear;
            tNear = tFar;
            tFar = swap;
         }

         double clampedMin = Math.max(tMin, tNear);
         double clampedMax = Math.min(tMax, tFar);
         return clampedMin > clampedMax ? null : new double[]{clampedMin, clampedMax};
      } else {
         return !(origin < minBound) && !(origin > maxBound) ? new double[]{tMin, tMax} : null;
      }
   }

   private double getEyeHeightSafe(Player player) {
      try {
         return player.getEyeHeight();
      } catch (Throwable ex) {
         return 1.62;
      }
   }

   private static boolean intersectsRayAabb(
      double originX,
      double originY,
      double originZ,
      double dirX,
      double dirY,
      double dirZ,
      double minX,
      double minY,
      double minZ,
      double maxX,
      double maxY,
      double maxZ,
      double maxDistance
   ) {
      double[] xSlab = intersectRayAxisSlab(originX, dirX, minX, maxX, 0.0, maxDistance);
      if (xSlab == null) {
         return false;
      }

      double[] ySlab = intersectRayAxisSlab(originY, dirY, minY, maxY, xSlab[0], xSlab[1]);
      if (ySlab == null) {
         return false;
      }

      double[] zSlab = intersectRayAxisSlab(originZ, dirZ, minZ, maxZ, ySlab[0], ySlab[1]);
      return zSlab != null;
   }

   public static final class CheckResult {
      public static final ProbeGuardChecker.CheckResult ALLOWED;
      private final boolean cancel;
      private final String reason;

      private CheckResult(boolean cancel, String reason) {
         this.cancel = cancel;
         this.reason = reason;
      }

      public boolean isCancel() {
         return this.cancel;
      }

      public String getReason() {
         return this.reason;
      }

      public static ProbeGuardChecker.CheckResult cancel(String reason) {
         return new ProbeGuardChecker.CheckResult(true, reason);
      }

      public static ProbeGuardChecker.CheckResult allow(String reason) {
         return new ProbeGuardChecker.CheckResult(false, reason);
      }

      static {
         ALLOWED = new ProbeGuardChecker.CheckResult(false, "ok");
      }
   }

   private static final class ChunkOcclusionProbe implements VoxelRayTracer.OcclusionChecker {
      private final ConcurrentMap<ChunkKey, TrackedChunkVisibility> chunks;
      private final boolean[] occludingBlockStates;
      private final MutableChunkKey key = new MutableChunkKey(0L);
      private boolean missingChunkData;

      private ChunkOcclusionProbe(ConcurrentMap<ChunkKey, TrackedChunkVisibility> chunks, boolean[] occludingBlockStates) {
         this.chunks = chunks;
         this.occludingBlockStates = occludingBlockStates;
      }

      @Override
      public boolean isOccluding(int blockX, int blockY, int blockZ) {
         return this.checkBlockOccluding(blockX, blockY, blockZ);
      }

      private static BlockState getBlockStateSafe(LevelChunkSection levelChunkSection, int blockX, int blockY, int blockZ) {
         try {
            return levelChunkSection.getBlockState(blockX & 15, blockY & 15, blockZ & 15);
         } catch (MissingPaletteEntryException ex) {
            return ProbeGuardChecker.AIR;
         }
      }

      public boolean checkBlockOccluding(int blockX, int blockY, int blockZ) {
         int chunkX = blockX >> 4;
         int chunkZ = blockZ >> 4;
         this.key.setValue(ChunkCoordinateCodec.encode(chunkX, chunkZ));
         TrackedChunkVisibility trackedChunkVisibility = this.chunks.get(this.key);
         if (trackedChunkVisibility == null) {
            this.missingChunkData = true;
            return true;
         } else {
            LevelChunk chunk = trackedChunkVisibility.getChunk();
            if (chunk == null) {
               this.missingChunkData = true;
               return true;
            } else {
               int sectionY = blockY >> 4;
               int minSectionY = chunk.getMinSectionY();
               if (sectionY >= minSectionY && sectionY < chunk.getMaxSectionY()) {
                  LevelChunkSection levelChunkSection = chunk.getSections()[sectionY - minSectionY];
                  return levelChunkSection != null && !levelChunkSection.hasOnlyAir()
                     ? this.occludingBlockStates[BlockStateIdMapper.getGlobalBlockStateId(getBlockStateSafe(levelChunkSection, blockX, blockY, blockZ))]
                     : false;
               } else {
                  return false;
               }
            }
         }
      }
   }

   private static final class PlayerProbeState {
      private String world;
      private double posX;
      private double posY;
      private double posZ;
      private double eyeY;
      private float yaw;
      private float pitch;
      private boolean positionSynced;
      private boolean rotationSynced;
      private boolean hasActiveDigTarget;
      private int digTargetX;
      private int digTargetY;
      private int digTargetZ;
      private long digStartTimeMs;
      private long invalidWindowStartMs;
      private int invalidCountInWindow;

      public void resetForWorld(String worldName) {
         this.world = worldName;
         this.positionSynced = false;
         this.rotationSynced = false;
         this.hasActiveDigTarget = false;
      }
   }
}
