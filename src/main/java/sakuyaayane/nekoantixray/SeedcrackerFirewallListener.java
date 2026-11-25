package sakuyaayane.nekoantixray;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange.EncodedBlock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.entity.Player;

public final class SeedcrackerFirewallListener extends PacketListenerAbstract {
   private static final long ISOLATION_WINDOW_MS = 10000L;
   private static final long PROBE_POSITION_TTL_MS = 30000L;
   private static final int ISOLATION_SCORE_THRESHOLD = 8;
   private static final int REPEAT_PROBE_SCORE = 3;
   private static final int CANCEL_DIG_PROBE_SCORE = 3;
   private static final int SINGLE_PROBE_SCORE = 1;
   private static final int BLOCK_ENTITY_UPDATE_SCORE = 2;
   private static final int UPDATE_BURST_BONUS_SCORE = 2;
   private static final int UPDATE_BURST_THRESHOLD = 16;
   private final NekoAntiXray plugin;
   private final PlayerBlockStateCache shadowState;
   private final boolean rewriteBlockUpdates;
   private final boolean rewriteBlockEntityUpdates;
   private final boolean interceptPlayerActions;
   private final boolean interceptStartStopDigging;
   private final boolean debug;
   private final ConcurrentMap<UUID, SeedcrackerFirewallListener.PlayerProbeTracker> playerProbeTrackers = new ConcurrentHashMap<>();

   @Override
   public void onPacketSend(PacketSendEvent event) {
      if (!event.isCancelled()) {
         try {
            if (this.rewriteBlockUpdates && event.getPacketType() == Server.BLOCK_CHANGE) {
               this.handleBlockChangePacket(event);
               return;
            }

            if (this.rewriteBlockUpdates && event.getPacketType() == Server.MULTI_BLOCK_CHANGE) {
               this.handleMultiBlockChangePacket(event);
               return;
            }

            if (this.rewriteBlockEntityUpdates && event.getPacketType() == Server.BLOCK_ENTITY_DATA) {
               this.handleBlockEntityDataPacket(event);
            }
         } catch (Throwable ex) {
            if (this.debug) {
               this.plugin
                  .logDebug("[AntiSeedCracker] SeedcrackerX firewall send hook failed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
         }
      }
   }

   @Override
   public void onPacketReceive(PacketReceiveEvent event) {
      if (this.interceptPlayerActions && !event.isCancelled() && event.getPacketType() == Client.PLAYER_DIGGING) {
         try {
            Player player = event.getPlayer();
            if (player == null) {
               return;
            }

            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
            if (!this.shouldInterceptDiggingAction(packet.getAction())) {
               return;
            }

            Vector3i blockPosition = packet.getBlockPosition();
            PlayerBlockStateCache.ShadowBlockSnapshot snapshot = this.shadowState.getSnapshot(player.getUniqueId(), player.getWorld().getName(), blockPosition);
            if (snapshot == null || snapshot.getBlockState() == null && !snapshot.isBlockEntityHidden()) {
               return;
            }

            boolean shouldIsolate = this.recordProtectedProbe(player.getUniqueId(), player.getWorld().getName(), blockPosition, packet.getAction());
            if (!shouldIsolate) {
               if (this.debug) {
                  this.plugin
                     .logDebug(
                        "[AntiSeedCracker] Observed protected block probe from "
                           + player.getName()
                           + " action="
                           + String.valueOf(packet.getAction())
                           + " pos="
                           + String.valueOf(blockPosition)
                           + " (below isolation threshold)"
                     );
               }

               return;
            }

            event.setCancelled(true);
            if (this.debug) {
               this.plugin
                  .logDebug(
                     "[AntiSeedCracker] Cancelled SeedcrackerX-style block update probe from "
                        + player.getName()
                        + " action="
                        + String.valueOf(packet.getAction())
                        + " pos="
                        + String.valueOf(blockPosition)
                  );
            }
         } catch (Throwable ex) {
            if (this.debug) {
               this.plugin
                  .logDebug("[AntiSeedCracker] SeedcrackerX firewall receive hook failed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
         }
      }
   }

   private void handleBlockChangePacket(PacketSendEvent event) {
      Player player = event.getPlayer();
      if (player != null) {
         WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(event);
         PlayerBlockStateCache.ShadowBlockSnapshot snapshot = this.shadowState.getSnapshot(player.getUniqueId(), player.getWorld().getName(), packet.getBlockPosition());
         if (snapshot != null && snapshot.getBlockState() != null) {
            this.recordProtectedUpdate(player.getUniqueId(), false, 1);
            packet.setBlockState(snapshot.getBlockState());
            event.markForReEncode(true);
            if (this.debug) {
               this.plugin.logDebug("[AntiSeedCracker] Rewrote protected block update for " + player.getName() + " pos=" + packet.getBlockPosition());
            }
         }
      }
   }

   private void handleMultiBlockChangePacket(PacketSendEvent event) {
      Player player = event.getPlayer();
      if (player != null) {
         UUID playerId = player.getUniqueId();
         String worldName = player.getWorld().getName();
         WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange(event);
         Vector3i chunkPosition = packet.getChunkPosition();
         EncodedBlock[] blocks = packet.getBlocks();
         if (chunkPosition != null && blocks != null && blocks.length != 0) {
            int baseX = chunkPosition.getX() << 4;
            int baseY = chunkPosition.getY() << 4;
            int baseZ = chunkPosition.getZ() << 4;
            int rewrittenCount = 0;

            for (EncodedBlock encodedBlock : blocks) {
               if (encodedBlock != null) {
                  int blockX = baseX + encodedBlock.getX();
                  int blockY = baseY + encodedBlock.getY();
                  int blockZ = baseZ + encodedBlock.getZ();
                  PlayerBlockStateCache.ShadowBlockSnapshot snapshot = this.shadowState.getSnapshot(playerId, worldName, blockX, blockZ);
                  WrappedBlockState shadowBlockState = snapshot != null ? snapshot.getBlockState() : null;
                  if (shadowBlockState != null) {
                     encodedBlock.setBlockState(shadowBlockState);
                     rewrittenCount++;
                  }
               }
            }

            if (rewrittenCount != 0) {
               this.recordProtectedUpdate(playerId, false, rewrittenCount);
               packet.setBlocks(blocks);
               event.markForReEncode(true);
               if (this.debug) {
                  this.plugin.logDebug("[AntiSeedCracker] Rewrote " + rewrittenCount + " protected multi-block updates for " + player.getName());
               }
            }
         }
      }
   }

   private void handleBlockEntityDataPacket(PacketSendEvent event) {
      Player player = event.getPlayer();
      if (player != null) {
         WrapperPlayServerBlockEntityData packet = new WrapperPlayServerBlockEntityData(event);
         if (this.shadowState.isBlockEntityHidden(player.getUniqueId(), player.getWorld().getName(), packet.getPosition())) {
            this.recordProtectedUpdate(player.getUniqueId(), true, 1);
            event.setCancelled(true);
            if (this.debug) {
               this.plugin.logDebug("[AntiSeedCracker] Cancelled protected block entity update for " + player.getName() + " pos=" + packet.getPosition());
            }
         }
      }
   }

   private boolean shouldInterceptDiggingAction(DiggingAction diggingAction) {
      return diggingAction == DiggingAction.CANCELLED_DIGGING
         ? true
         : this.interceptStartStopDigging && (diggingAction == DiggingAction.START_DIGGING || diggingAction == DiggingAction.FINISHED_DIGGING);
   }

   private boolean recordProtectedProbe(UUID playerId, String worldName, Vector3i blockPos, DiggingAction diggingAction) {
      if (playerId != null && blockPos != null) {
         SeedcrackerFirewallListener.PlayerProbeTracker tracker = this.playerProbeTrackers
            .computeIfAbsent(playerId, ignored -> new SeedcrackerFirewallListener.PlayerProbeTracker());
         String positionKey = formatBlockPositionKey(worldName, blockPos);
         boolean isCancelDig = diggingAction == DiggingAction.CANCELLED_DIGGING;
         long nowMs = System.currentTimeMillis();
         return tracker.recordProbe(positionKey, isCancelDig, nowMs);
      } else {
         return false;
      }
   }

   private void recordProtectedUpdate(UUID playerId, boolean isBlockEntityUpdate, int updateCount) {
      if (playerId != null) {
         SeedcrackerFirewallListener.PlayerProbeTracker tracker = this.playerProbeTrackers
            .computeIfAbsent(playerId, ignored -> new SeedcrackerFirewallListener.PlayerProbeTracker());
         long nowMs = System.currentTimeMillis();
         tracker.recordProtectedUpdate(isBlockEntityUpdate, updateCount, nowMs);
      }
   }

   private static String formatBlockPositionKey(String worldName, Vector3i blockPos) {
      return worldName + ":" + blockPos.getX() + ":" + blockPos.getY() + ":" + blockPos.getZ();
   }

   public SeedcrackerFirewallListener(NekoAntiXray plugin, PlayerBlockStateCache shadowState, ConfigManager configManager) {
      super(PacketListenerPriority.HIGHEST);
      this.plugin = plugin;
      this.shadowState = shadowState;
      this.rewriteBlockUpdates = configManager.isAntiSeedCrackerSeedcrackerXFirewallRewriteBlockUpdates();
      this.rewriteBlockEntityUpdates = configManager.isAntiSeedCrackerSeedcrackerXFirewallRewriteBlockEntityUpdates();
      this.interceptPlayerActions = configManager.isAntiSeedCrackerSeedcrackerXFirewallInterceptPlayerActions();
      this.interceptStartStopDigging = configManager.isAntiSeedCrackerSeedcrackerXFirewallInterceptStartStopDigging();
      this.debug = plugin.getConfig().getBoolean("debug", false);
   }

   private static final class PlayerProbeTracker {
      private final Map<String, Long> recentProbeTimestamps = new ConcurrentHashMap<>();
      private long windowStartTimeMs;
      private long isolatedUntilMs;
      private int score;
      private int protectedUpdateCount;

      public synchronized boolean recordProbe(String positionKey, boolean isCancelDig, long nowMs) {
         this.refreshWindowIfExpired(nowMs);
         Long lastProbeTimeMs = this.recentProbeTimestamps.put(positionKey, nowMs);
         if (lastProbeTimeMs != null && nowMs - lastProbeTimeMs <= 30000L) {
            this.score += 3;
         }

         if (isCancelDig) {
            this.score += 3;
         } else {
            this.score++;
         }

         this.cleanupOldProbePositions(nowMs);
         return this.evaluateIsolation(nowMs);
      }

      public synchronized void recordProtectedUpdate(boolean isBlockEntityUpdate, int updateCount, long nowMs) {
         this.refreshWindowIfExpired(nowMs);
         this.protectedUpdateCount += updateCount;
         this.score += isBlockEntityUpdate ? 2 : 1;
         if (this.protectedUpdateCount >= 16) {
            this.score += 2;
            this.protectedUpdateCount = 0;
         }

         this.evaluateIsolation(nowMs);
      }

      private boolean evaluateIsolation(long nowMs) {
         if (this.score >= 8) {
            this.isolatedUntilMs = nowMs + 10000L;
            return true;
         } else {
            return this.isolatedUntilMs > nowMs;
         }
      }

      private void refreshWindowIfExpired(long nowMs) {
         if (this.windowStartTimeMs == 0L || nowMs - this.windowStartTimeMs > 10000L) {
            this.windowStartTimeMs = nowMs;
            this.score = this.isolatedUntilMs > nowMs ? Math.max(1, this.score / 2) : 0;
            this.protectedUpdateCount = 0;
         }
      }

      private void cleanupOldProbePositions(long nowMs) {
         this.recentProbeTimestamps.entrySet().removeIf(entry -> nowMs - entry.getValue() > 30000L);
      }
   }
}
