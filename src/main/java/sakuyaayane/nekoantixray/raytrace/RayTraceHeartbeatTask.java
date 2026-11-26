package sakuyaayane.nekoantixray.raytrace;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.TimerTask;
import sakuyaayane.nekoantixray.NekoAntiXray;

public final class RayTraceHeartbeatTask extends TimerTask {
   private final NekoAntiXray plugin;
   private static final long HEARTBEAT_LOG_INTERVAL_MS = 30000L;
   private long lastHeartbeatLogTimeMs;

   public RayTraceHeartbeatTask(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public void run() {
      boolean rayTraceTimingDebug = this.plugin.isRayTraceTimingDebug();
      long tickStartMs = rayTraceTimingDebug ? System.currentTimeMillis() : 0L;

      try {
         if (this.plugin.isFoliaServer()) {
            ConcurrentMap<?, PlayerRayTraceState> foliaStates = this.plugin.getPlayerRayTraceStates();
            for (PlayerRayTraceState playerRayTraceState : foliaStates.values()) {
               if (playerRayTraceState.tryAcquire()) {
                  try {
                     this.plugin.getRayTraceExecutor().submit(() -> {
                        try {
                           playerRayTraceState.call();
                        } catch (Exception ex) {
                           throw new RuntimeException(ex);
                        } finally {
                           playerRayTraceState.release();
                        }
                     });
                  } catch (RejectedExecutionException rejectedEx) {
                     playerRayTraceState.release();
                  }
               }
            }
         } else {
            this.plugin.getRayTraceExecutor().invokeAll(this.plugin.getPlayerRayTraceStates().values());
         }
      } catch (InterruptedException interruptedEx) {
         Thread.currentThread().interrupt();
      } catch (RejectedExecutionException rejectedEx) {
      }

      long nowMs = System.currentTimeMillis();
      if (nowMs - this.lastHeartbeatLogTimeMs >= 30000L) {
         this.lastHeartbeatLogTimeMs = nowMs;
         ConcurrentMap<?, PlayerRayTraceState> playerRayTraceStates = this.plugin.getPlayerRayTraceStates();
         int playerCount = playerRayTraceStates.size();
         int chunkCacheSize = this.plugin.getTrackedChunkCache().size();
         int trackedChunkCount = 0;
         int pendingResultCount = 0;

         for (PlayerRayTraceState rayTraceState : playerRayTraceStates.values()) {
            trackedChunkCount += rayTraceState.getChunks().size();
            pendingResultCount += rayTraceState.getPendingUpdateCount();
         }

         String heartbeatMessage = "[RayTrace] heartbeat players=" + playerCount + " trackedChunks=" + trackedChunkCount + " chunkCache=" + chunkCacheSize + " pendingResults=" + pendingResultCount;
         this.plugin.logDebug(heartbeatMessage);
      }

      if (rayTraceTimingDebug) {
         long tickEndMs = System.currentTimeMillis();
         this.plugin.getLogger().info(tickEndMs - tickStartMs + "ms per ray trace tick.");
      }
   }
}
