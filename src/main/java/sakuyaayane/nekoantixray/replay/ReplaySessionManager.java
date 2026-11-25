package sakuyaayane.nekoantixray.replay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import sakuyaayane.nekoantixray.NekoAntiXray;
import sakuyaayane.nekoantixray.lib.folialib.FoliaLib;
import sakuyaayane.nekoantixray.lib.folialib.enums.EntityTaskResult;

public class ReplaySessionManager implements Listener {
   private static final String REPLAY_ARMOR_STAND_TAG = "nekoantixray_replay_entity";
   private final Map<UUID, ReplaySession> activeSessions = new ConcurrentHashMap<>();
   private final Map<UUID, ArmorStand> trackedReplayEntities = new ConcurrentHashMap<>();
   private final NekoAntiXray plugin;
   private final FoliaLib foliaLib;

   public ReplaySessionManager(NekoAntiXray plugin) {
      this.plugin = plugin;
      this.foliaLib = plugin.getFoliaScheduler();
   }

   public void addSession(UUID uuid, ReplaySession replaySession) {
      if (uuid != null && replaySession != null) {
         this.trackEntityReference(replaySession.getPlayerEntity());
         this.trackEntityReference(replaySession.getNameEntity());
         this.activeSessions.put(uuid, replaySession);
         this.plugin.logDebug("添加回放会话，玩家UUID: " + uuid + "，当前活跃会话数: " + this.activeSessions.size());
      } else {
         this.plugin.logDebug("忽略空的回放会话注册请求");
      }
   }

   public ReplaySession getSession(UUID uuid) {
      return this.activeSessions.get(uuid);
   }

   public ReplaySession removeSession(UUID uuid) {
      ReplaySession replaySession = this.activeSessions.remove(uuid);
      this.plugin.logDebug("移除回放会话，玩家UUID: " + uuid + "，当前活跃会话数: " + this.activeSessions.size());
      return replaySession;
   }

   boolean removeSession(UUID uuid, ReplaySession replaySession) {
      if (uuid != null && replaySession != null) {
         boolean removed = this.activeSessions.remove(uuid, replaySession);
         if (removed) {
            this.plugin.logDebug("移除指定回放会话，玩家UUID: " + uuid + "，当前活跃会话数: " + this.activeSessions.size());
         }

         return removed;
      } else {
         return false;
      }
   }

   public boolean hasActiveSession(UUID uuid) {
      return this.activeSessions.containsKey(uuid);
   }

   public int getActiveSessionCount() {
      return this.activeSessions.size();
   }

   void registerReplayEntity(ArmorStand armorStand) {
      if (armorStand != null) {
         this.trackEntityReference(armorStand);
         armorStand.setPersistent(false);
         armorStand.addScoreboardTag("nekoantixray_replay_entity");
      }
   }

   public void cleanupAllSessions() {
      this.cleanupAllSessions(false);
   }

   public void cleanupAllSessions(boolean immediate) {
      if (immediate) {
         this.cleanupSessions();
      } else {
         try {
            this.foliaLib.getImpl().runNextTick(wrappedTask -> this.cleanupSessions()).exceptionally(asyncError -> {
               this.plugin.logDebug("无法在全局调度器清理回放会话，改为立即清理引用: " + this.getErrorMessage(asyncError));
               this.cleanupSessions();
               return null;
            });
         } catch (Exception scheduleEx) {
            this.plugin.logDebug("无法调度回放会话清理，改为立即清理引用: " + scheduleEx.getMessage());
            this.cleanupSessions();
         }
      }
   }

   public CompletableFuture<Void> cleanupBeforeDisable() {
      return this.cleanupSessions();
   }

   private CompletableFuture<Void> cleanupSessions() {
      Set<ArmorStand> armorStandsToRemove = new HashSet<>(this.trackedReplayEntities.values());

      for (ReplaySession replaySession : this.activeSessions.values()) {
         if (replaySession != null) {
            try {
               if (replaySession.getTask() != null) {
                  replaySession.getTask().cancel();
                  replaySession.setTask(null);
               }
            } catch (Exception ex) {
               this.plugin.logDebug("取消回放任务失败，已跳过: " + ex.getMessage());
            }

            this.addIfNotNull(armorStandsToRemove, replaySession.getPlayerEntity());
            this.addIfNotNull(armorStandsToRemove, replaySession.getNameEntity());
         }
      }

      this.activeSessions.clear();
      List<CompletableFuture<Void>> removalFutures = new ArrayList<>();

      for (ArmorStand armorStand : armorStandsToRemove) {
         removalFutures.add(this.removeArmorStandSafely(armorStand, "回放实体"));
      }

      CompletableFuture<Void> allRemovedFuture = CompletableFuture.allOf(removalFutures.toArray(new CompletableFuture[0]));
      allRemovedFuture.thenRun(() -> this.plugin.logDebug("所有回放会话引用和可调度实体已清理。"));
      return allRemovedFuture;
   }

   CompletableFuture<Void> cleanupSessionEntities(ReplaySession replaySession) {
      return replaySession == null
         ? CompletableFuture.completedFuture(null)
         : CompletableFuture.allOf(
            this.removeArmorStandSafely(replaySession.getPlayerEntity(), "回放玩家实体"),
            this.removeArmorStandSafely(replaySession.getNameEntity(), "回放名称实体")
         );
   }

   CompletableFuture<Void> removeArmorStandSafely(ArmorStand armorStand, String entityLabel) {
      if (armorStand == null) {
         this.plugin.logDebug(entityLabel + "为null，跳过清理");
         return CompletableFuture.completedFuture(null);
      }

      UUID entityId = this.getEntityId(armorStand);
      Runnable task = () -> this.removeArmorStandOnOwnedThread(armorStand, entityId, entityLabel);

      try {
         if (this.foliaLib.getImpl().isOwnedByCurrentRegion(armorStand)) {
            task.run();
            return CompletableFuture.completedFuture(null);
         } else if (!this.plugin.isEnabled()) {
            this.plugin.logDebug("插件已禁用且当前线程不拥有" + entityLabel + "，跳过跨区域移除");
            return CompletableFuture.completedFuture(null);
         } else {
            return this.foliaLib
               .getImpl()
               .runAtEntityWithFallback(armorStand, wrappedTask -> task.run(), () -> this.retireTrackedEntity(entityId, armorStand, entityLabel))
               .handle((entityTaskResult, taskError) -> {
                  if (taskError != null) {
                     this.untrackEntity(entityId, armorStand);
                     this.plugin.logDebug("移除" + entityLabel + "的实体任务失败: " + this.getErrorMessage(taskError));
                     return null;
                  }

                  if (entityTaskResult != EntityTaskResult.SUCCESS) {
                     this.untrackEntity(entityId, armorStand);
                     this.plugin.logDebug(entityLabel + "的实体调度器已退休，跳过移除: " + entityTaskResult);
                  }

                  return null;
               });
         }
      } catch (Exception scheduleEx) {
         this.untrackEntity(entityId, armorStand);
         this.plugin.logDebug("无法调度" + entityLabel + "移除任务，已跳过: " + scheduleEx.getMessage());
         return CompletableFuture.completedFuture(null);
      }
   }

   private void removeArmorStandOnOwnedThread(ArmorStand armorStand, UUID uuid, String entityLabel) {
      try {
         if (!armorStand.isValid()) {
            this.untrackEntity(uuid, armorStand);
            this.plugin.logDebug(entityLabel + "已无效，跳过清理");
            return;
         }

         armorStand.remove();
         this.untrackEntity(uuid, armorStand);
         this.plugin.logDebug("已移除" + entityLabel + ": " + uuid);
      } catch (Exception ex) {
         this.plugin.logDebug("移除" + entityLabel + "时发生错误，已跳过: " + ex.getMessage());
      }
   }

   private void retireTrackedEntity(UUID uuid, ArmorStand armorStand, String entityLabel) {
      this.untrackEntity(uuid, armorStand);
      this.plugin.logDebug(entityLabel + "已退休或卸载，无需继续移除");
   }

   @EventHandler
   public void onEntitiesLoad(EntitiesLoadEvent event) {
      int cleanedCount = this.cleanupTaggedArmorStands(event.getEntities());
      if (cleanedCount > 0) {
         this.plugin.logDebug("区块加载时清理了 " + cleanedCount + " 个遗留回放实体");
      }
   }

   public CompletableFuture<Integer> cleanupLoadedReplayArmorStands(String worldName) {
      CompletableFuture<Integer> future = new CompletableFuture<>();

      try {
         this.foliaLib.getImpl().runNextTick(wrappedTask -> this.scheduleLoadedChunkCleanup(worldName, future)).whenComplete((voidValue, taskError) -> {
            if (taskError != null) {
               future.completeExceptionally(taskError);
            }
         });
      } catch (Exception scheduleEx) {
         future.completeExceptionally(scheduleEx);
      }

      return future;
   }

   private void scheduleLoadedChunkCleanup(String worldName, CompletableFuture<Integer> future) {
      try {
         List<CompletableFuture<Integer>> chunkFutures = new ArrayList<>();

         for (World world : Bukkit.getWorlds()) {
            if (worldName == null || world.getName().equals(worldName)) {
               for (Chunk chunk : world.getLoadedChunks()) {
                  chunkFutures.add(this.cleanupChunkOnOwnedRegion(chunk));
               }
            }
         }

         if (chunkFutures.isEmpty()) {
            future.complete(0);
            return;
         }

         CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0]))
            .whenComplete((voidValue, completionError) -> this.completeTotalCleanupCount(chunkFutures, future));
      } catch (Exception scheduleEx) {
         future.completeExceptionally(scheduleEx);
      }
   }

   private CompletableFuture<Integer> cleanupChunkOnOwnedRegion(Chunk chunk) {
      CompletableFuture<Integer> future = new CompletableFuture<>();
      World world = chunk.getWorld();
      int chunkX = chunk.getX();
      int chunkZ = chunk.getZ();
      Location location = new Location(world, (chunkX << 4) + 8, 0.0, (chunkZ << 4) + 8);

      try {
         this.foliaLib.getImpl().runAtLocation(location, wrappedTask -> {
            try {
               if (!chunk.isLoaded()) {
                  future.complete(0);
                  return;
               }

               future.complete(this.cleanupTaggedArmorStands(Arrays.asList(chunk.getEntities())));
            } catch (Exception cleanupEx) {
               this.plugin.logDebug("清理区块 " + chunkX + "," + chunkZ + " 的遗留回放实体失败: " + cleanupEx.getMessage());
               future.complete(0);
            }
         }).whenComplete((voidValue, dispatchError) -> {
            if (dispatchError != null) {
               this.plugin.logDebug("无法在区块 " + chunkX + "," + chunkZ + " 调度回放实体清理: " + this.getErrorMessage(dispatchError));
               future.complete(0);
            }
         });
      } catch (Exception scheduleEx) {
         this.plugin.logDebug("无法调度区块 " + chunkX + "," + chunkZ + " 的回放实体清理: " + scheduleEx.getMessage());
         future.complete(0);
      }

      return future;
   }

   private int cleanupTaggedArmorStands(Iterable<? extends Entity> entities) {
      int removedCount = 0;

      for (Entity entity : entities) {
         if (entity instanceof ArmorStand armorStand) {
            try {
               UUID uuid = armorStand.getUniqueId();
               if (!this.trackedReplayEntities.containsKey(uuid) && armorStand.getScoreboardTags().contains("nekoantixray_replay_entity")) {
                  armorStand.remove();
                  removedCount++;
               }
            } catch (Exception ex) {
               this.plugin.logDebug("清理遗留回放实体时发生错误，已跳过: " + ex.getMessage());
            }
         }
      }

      return removedCount;
   }

   public void updateArmorStandVisibility(Player player) {
      if (player != null) {
         for (ReplaySession replaySession : this.activeSessions.values()) {
            if (replaySession != null) {
               boolean isViewer = player == replaySession.getViewer();
               this.updateEntityVisibility(player, replaySession.getPlayerEntity(), isViewer);
               this.updateEntityVisibility(player, replaySession.getNameEntity(), isViewer);
            }
         }
      }
   }

   private void updateEntityVisibility(Player player, ArmorStand armorStand, boolean visible) {
      if (armorStand != null) {
         try {
            if (visible) {
               player.showEntity(this.plugin, armorStand);
            } else {
               player.hideEntity(this.plugin, armorStand);
            }
         } catch (Exception ex) {
            this.plugin.logDebug("更新回放实体可见性失败，已跳过: " + ex.getMessage());
         }
      }
   }

   private void trackEntityReference(ArmorStand armorStand) {
      if (armorStand != null) {
         this.trackedReplayEntities.put(armorStand.getUniqueId(), armorStand);
      }
   }

   private UUID getEntityId(ArmorStand armorStand) {
      try {
         return armorStand.getUniqueId();
      } catch (Exception ex) {
         this.plugin.logDebug("无法读取回放实体UUID: " + ex.getMessage());
         return null;
      }
   }

   private void addIfNotNull(Set<ArmorStand> set, ArmorStand armorStand) {
      if (armorStand != null) {
         set.add(armorStand);
      }
   }

   private void untrackEntity(UUID uuid, ArmorStand armorStand) {
      if (uuid != null) {
         this.trackedReplayEntities.remove(uuid, armorStand);
      }
   }

   private int getCompletedCleanupCount(CompletableFuture<Integer> future) {
      try {
         return future.getNow(0);
      } catch (Exception ex) {
         return 0;
      }
   }

   private String getErrorMessage(Throwable error) {
      Throwable rootCause = error;

      while (rootCause.getCause() != null) {
         rootCause = rootCause.getCause();
      }

      return rootCause.getMessage() != null ? rootCause.getMessage() : rootCause.getClass().getSimpleName();
   }

   private void completeTotalCleanupCount(List chunkFutures, CompletableFuture totalFuture) {
      int totalCleaned = chunkFutures.stream().mapToInt(chunkFuture -> this.getCompletedCleanupCount((CompletableFuture<Integer>)chunkFuture)).sum();
      totalFuture.complete(totalCleaned);
   }
}
