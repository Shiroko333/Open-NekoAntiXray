package sakuyaayane.nekoantixray.replay;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.UUID;
import lombok.Generated;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.block.data.BlockData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.World;
import sakuyaayane.nekoantixray.lib.folialib.enums.EntityTaskResult;
import sakuyaayane.nekoantixray.lib.folialib.FoliaLib;
import sakuyaayane.nekoantixray.lib.folialib.wrapper.task.WrappedTask;
import sakuyaayane.nekoantixray.NekoAntiXray;
import sakuyaayane.nekoantixray.stats.OreParticleRenderer;

public class ReplayCommand implements CommandExecutor {
   private final NekoAntiXray plugin;
   private final FoliaLib foliaLib;
   private final ReplaySessionManager sessionManager;
   private final ExecutorService workerPool;
   private final ScheduledExecutorService scheduledPool;
   private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
   private final Object replayLifecycleLock = new Object();
   private final Map<ReplayCommand.ChunkCoordinate, Map<ReplayCommand.BlockPosition, Material>> brokenBlocksCache = new ConcurrentHashMap<>();
   private final AtomicLong cacheLastCleanup = new AtomicLong(System.currentTimeMillis());
   private static final int MAX_CACHE_SIZE = 10000;
   private static final long CACHE_CLEANUP_INTERVAL = 300000L;
   private static final int OPTIMIZED_RADIUS = 12;
   private static final int MAX_BLOCKS_PER_BATCH = 50;
   private static final long BLOCK_RESTORE_INTERVAL = 5L;
   private static final long PARTICLE_INTERVAL = 2L;
   private static final int ITEMS_PER_PAGE = 45;
   private final Map<UUID, Integer> playerPages = new ConcurrentHashMap<>();
   private static boolean optimizedModeEnabled = true;

   public ReplayCommand(NekoAntiXray plugin, ReplaySessionManager sessionManager) {
      this.plugin = plugin;
      this.foliaLib = plugin.getFoliaScheduler();
      this.sessionManager = sessionManager;
      int availableProcessors = Runtime.getRuntime().availableProcessors();
      this.workerPool = Executors.newFixedThreadPool(Math.max(2, availableProcessors / 2), runnable -> {
         Thread workerThread = new Thread(runnable, "ReplayWorker-" + Thread.currentThread().getId());
         workerThread.setDaemon(true);
         workerThread.setPriority(4);
         return workerThread;
      });
      this.scheduledPool = Executors.newScheduledThreadPool(2, runnable -> {
         Thread schedulerThread = new Thread(runnable, "ReplayScheduler-" + Thread.currentThread().getId());
         schedulerThread.setDaemon(true);
         schedulerThread.setPriority(4);
         return schedulerThread;
      });
      this.setupCacheCleanupTask();
      plugin.logDebug("ReplayCommand 优化版本已初始化 - CPU核心数: " + availableProcessors + ", 工作线程数: " + Math.max(2, availableProcessors / 2));
   }

   private void setupCacheCleanupTask() {
      this.scheduledPool.scheduleAtFixedRate(() -> {
         try {
            this.cleanupExpiredCache();
         } catch (Exception ex) {
            this.plugin.getLogger().warning("缓存清理任务异常: " + ex.getMessage());
         }
      }, 300000L, 300000L, TimeUnit.MILLISECONDS);
   }

   private void cleanupExpiredCache() {
      if (this.brokenBlocksCache.size() > 10000) {
         int targetCacheSize = 5000;
         this.brokenBlocksCache.entrySet().removeIf(entry -> this.exceedsCacheTargetSize(targetCacheSize));
         this.plugin.logDebug("缓存清理完成，当前大小: " + this.brokenBlocksCache.size());
      }

      this.cacheLastCleanup.set(System.currentTimeMillis());
   }

   public void shutdown() {
      synchronized (this.replayLifecycleLock) {
      }

      if (!this.shuttingDown.compareAndSet(false, true)) {
         return;
      }

      this.workerPool.shutdownNow();
      this.scheduledPool.shutdownNow();
      this.brokenBlocksCache.clear();
      this.playerPages.clear();
      this.plugin.logDebug("ReplayCommand 资源清理完成");
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender instanceof Player player) {
         if (!player.hasPermission("nekoantixray.replay")) {
            player.sendMessage("§c你没有权限使用此命令。");
            return true;
         }

         if (args.length == 0) {
            this.openReplayGUI(player);
            return true;
         }

         switch (args[0].toLowerCase()) {
            case "tp":
               this.teleportToArmorStand(player);
               break;
            case "stop":
               this.stopReplay(player);
               break;
            case "pause":
               this.pauseReplay(player);
               break;
            case "resume":
               this.resumeReplay(player);
               break;
            case "forward":
               this.forwardReplay(player);
               break;
            case "backward":
               this.backwardReplay(player);
               break;
            case "delete":
               if (args.length < 2) {
                  player.sendMessage("§c请指定要删除的回放文件名。");
                  return true;
               }

               this.deleteReplay(player, args[1]);
               break;
            case "play":
               if (args.length < 2) {
                  player.sendMessage("§c请指定要播放的回放文件名。");
                  return true;
               }

               this.startReplay(player, args[1]);
               break;
            default:
               player.sendMessage("§c未知的子命令。使用 /nax replay 打开回放管理界面。");
         }

         return true;
      } else {
         sender.sendMessage("§c此命令只能由玩家执行。");
         return true;
      }
   }

   private void teleportToArmorStand(Player player) {
      ReplaySession session = this.sessionManager.getSession(player.getUniqueId());
      if (session == null) {
         player.sendMessage("§c你当前没有正在进行的回放。");
      } else {
         ArmorStand playerEntity = session.getPlayerEntity();
         if (playerEntity == null) {
            player.sendMessage("§c无法找到有效的回放位置。");
         } else {
            Location viewerLocation = player.getLocation();

            try {
               this.foliaLib
                  .getImpl()
                  .runAtEntityWithFallback(
                     playerEntity,
                     wrappedTask -> {
                        if (!playerEntity.isValid()) {
                           this.sendViewerMessage(player, "§c无法找到有效的回放位置。");
                        } else {
                           Location targetLocation = playerEntity.getLocation().clone();
                           targetLocation.setYaw(viewerLocation.getYaw());
                           targetLocation.setPitch(viewerLocation.getPitch());
                           this.foliaLib
                              .getImpl()
                              .teleportAsync(player, targetLocation)
                              .whenComplete(
                                 (teleported, error) -> this.sendViewerMessage(player, error == null && Boolean.TRUE.equals(teleported) ? "§a已传送到回放位置。" : "§c传送失败。")
                              );
                        }
                     },
                     () -> this.sendViewerMessage(player, "§c无法找到有效的回放位置。")
                  );
            } catch (Exception ex) {
               player.sendMessage("§c无法找到有效的回放位置。");
            }
         }
      }
   }

   public void openReplayGUI(Player player, int page) {
      Inventory inventory = Bukkit.createInventory(null, 54, "回放管理 - 第 " + page + " 页");
      File replaysDir = new File(this.plugin.getDataFolder(), "replays");
      File[] replayFiles = replaysDir.listFiles((dir, fileName) -> fileName.endsWith(".replay"));
      if (replayFiles != null) {
         Arrays.sort(replayFiles, Comparator.comparingLong(File::lastModified).reversed());
         int startIndex = (page - 1) * 45;
         int endIndex = Math.min(startIndex + 45, replayFiles.length);

         for (int i = startIndex; i < endIndex; i++) {
            File replayFile = replayFiles[i];
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta itemMeta = item.getItemMeta();
            itemMeta.setDisplayName("§e" + replayFile.getName());
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedDate = simpleDateFormat.format(new Date(replayFile.lastModified()));
            itemMeta.setLore(Arrays.asList("§7最后修改时间: " + formattedDate, "§a左键点击播放", "§c右键点击删除"));
            item.setItemMeta(itemMeta);
            inventory.setItem(i - startIndex, item);
         }
      }

      inventory.setItem(45, this.createGuiItem(Material.BARRIER, "§c关闭"));
      inventory.setItem(46, this.createGuiItem(Material.PAPER, "§e上一页"));
      inventory.setItem(52, this.createGuiItem(Material.PAPER, "§e下一页"));
      inventory.setItem(53, this.createGuiItem(Material.HOPPER, "§a刷新"));
      player.openInventory(inventory);
      this.playerPages.put(player.getUniqueId(), page);
   }

   public void openReplayGUI(Player player) {
      this.openReplayGUI(player, 1);
   }

   private ItemStack createGuiItem(Material material, String displayName) {
      ItemStack item = new ItemStack(material);
      ItemMeta itemMeta = item.getItemMeta();
      itemMeta.setDisplayName(displayName);
      item.setItemMeta(itemMeta);
      return item;
   }

   public void deleteReplay(Player player, String fileName) {
      File replayFile = new File(this.plugin.getDataFolder(), "replays/" + fileName);
      if (replayFile.exists()) {
         if (replayFile.delete()) {
            player.sendMessage("§a成功删除回放文件: " + fileName);
         } else {
            player.sendMessage("§c删除回放文件失败: " + fileName);
         }
      } else {
         player.sendMessage("§c找不到指定的回放文件: " + fileName);
      }
   }

   private void pauseReplay(Player player) {
      this.plugin.logDebug("尝试暂停回放，玩家: " + player.getName());
      ReplaySession session = this.sessionManager.getSession(player.getUniqueId());
      if (session != null) {
         this.plugin.logDebug("找到玩家的活跃回放会话: " + player.getName());
         if (session.isPaused()) {
            session.resume();
            player.sendMessage("§a回放已继续。");
         } else {
            session.pause();
            player.sendMessage("§a回放已暂停。");
         }

         this.plugin.logDebug("回放暂停状态: " + session.isPaused());
      } else {
         this.plugin.logDebug("未找到玩家的活跃回放会话: " + player.getName());
         player.sendMessage("§c你当前没有正在进行的回放。");
      }
   }

   private void resumeReplay(Player player) {
      ReplaySession session = this.sessionManager.getSession(player.getUniqueId());
      if (session != null) {
         session.resume();
         player.sendMessage("§a回放已继续。");
      } else {
         player.sendMessage("§c你当前没有正在进行的回放。");
      }
   }

   private void forwardReplay(Player player) {
      ReplaySession session = this.sessionManager.getSession(player.getUniqueId());
      if (session != null) {
         session.forward();
         player.sendMessage("§a回放已快进。");
      } else {
         player.sendMessage("§c你当前没有正在进行的回放。");
      }
   }

   private void backwardReplay(Player player) {
      ReplaySession session = this.sessionManager.getSession(player.getUniqueId());
      if (session != null) {
         session.backward();
         player.sendMessage("§a回放已倒退。");
      } else {
         player.sendMessage("§c你当前没有正在进行的回放。");
      }
   }

   private void stopReplay(Player player) {
      this.plugin.logDebug("开始停止回放，玩家: " + player.getName());
      ReplaySession replaySession = this.sessionManager.removeSession(player.getUniqueId());
      if (replaySession == null) {
         this.plugin.logDebug("未找到玩家的活跃回放会话: " + player.getName());
         player.sendMessage("§c你当前没有正在进行的回放。");
      } else {
         this.plugin.logDebug("找到并移除了玩家的回放会话: " + player.getName());
         if (this.cancelSessionTask(replaySession)) {
            this.plugin.logDebug("取消回放任务");
         } else {
            this.plugin.logDebug("没有找到活跃的回放任务引用");
         }

         Location originalLocation = replaySession.getOriginalLocation();
         GameMode originalGameMode = replaySession.getOriginalGameMode();
         this.plugin.logDebug("获取玩家原始状态 - 位置: " + originalLocation + ", 游戏模式: " + originalGameMode);
         this.plugin.logDebug("开始清理回放实体");
         this.sessionManager.cleanupSessionEntities(replaySession);
         this.plugin.logDebug("实体清理任务已调度");
         this.plugin.logDebug("准备传送玩家回原始位置: " + originalLocation);
         player.sendMessage("§a回放已停止。");
         if (originalLocation == null) {
            this.restoreViewerState(player, originalGameMode);
         } else {
            this.foliaLib.getImpl().teleportAsync(player, originalLocation).whenComplete((teleported, error) -> {
               if (error == null && Boolean.TRUE.equals(teleported)) {
                  this.restoreViewerState(player, originalGameMode);
               } else {
                  this.plugin.logDebug("玩家传送失败: " + this.getErrorMessage(error != null ? error : new IllegalStateException("teleport returned false")));
               }
            });
         }

         this.plugin.logDebug("回放停止处理完成，玩家: " + player.getName());
      }
   }

   private CompletableFuture<ReplaySession> playReplayAsync(Player player, File replayFile) {
      this.plugin.logDebug("开始创建回放会话，文件: " + replayFile.getName() + ", 玩家: " + player.getName());
      CompletableFuture<ReplaySession> future = new CompletableFuture<>();
      if (!this.isReplayStartAllowed()) {
         future.complete(null);
         return future;
      }

      try {
         this.workerPool.submit(() -> this.loadReplayFrames(player, replayFile, future));
      } catch (RejectedExecutionException ex) {
         future.complete(null);
      }

      return future;
   }

   private void loadReplayFrames(Player player, File replayFile, CompletableFuture<ReplaySession> future) {
      try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(replayFile))) {
         List<ReplayFrame> frames = (List<ReplayFrame>)objectInputStream.readObject();
         if (frames != null && !frames.isEmpty()) {
            this.plugin.logDebug("成功读取回放文件，帧数: " + frames.size());
            if (this.isReplayStartAllowed()) {
               this.scheduleReplayPreparation(player, replayFile, frames, future);
            } else {
               future.complete(null);
            }
         } else {
            this.plugin.logDebug("回放文件为空");
            this.sendViewerMessage(player, "§c回放文件是空的。");
            future.complete(null);
         }
      } catch (IOException | ClassNotFoundException | ClassCastException ex) {
         this.plugin.logDebug("读取回放文件时出错: " + ex.getMessage());
         this.sendViewerMessage(player, "§c读取回放文件时出错: " + ex.getMessage());
         future.completeExceptionally(ex);
      }
   }

   private void scheduleReplayPreparation(Player player, File replayFile, List<ReplayFrame> frames, CompletableFuture<ReplaySession> future) {
      try {
         this.foliaLib.getImpl().runAtEntityWithFallback(player, wrappedTask -> {
            if (!this.isReplayStartAllowed()) {
               future.complete(null);
            } else {
               ReplayFrame firstFrame = frames.get(0);
               World world = Bukkit.getWorld(firstFrame.worldName);
               if (world == null) {
                  this.plugin.logDebug("目标世界不存在: " + firstFrame.worldName);
                  player.sendMessage("§c回放所在的世界不存在：" + firstFrame.worldName);
                  future.complete(null);
               } else {
                  Location originalLocation = player.getLocation().clone();
                  GameMode originalGameMode = player.getGameMode();
                  Location initialLocation = firstFrame.getLocation(world);
                  String targetPlayerName = replayFile.getName().split("_")[0];
                  this.plugin.logDebug("提取的玩家名称: " + targetPlayerName + "，初始位置: " + initialLocation);
                  this.spawnReplayEntities(player, world, initialLocation, originalLocation, originalGameMode, targetPlayerName, frames, future);
               }
            }
         }, () -> future.complete(null)).whenComplete((entityTaskResult, error) -> this.completeIfEntityTaskRejected(future, entityTaskResult, error));
      } catch (Exception ex) {
         future.completeExceptionally(ex);
      }
   }

   private void spawnReplayEntities(
      Player player,
      World world,
      Location initialLocation,
      Location originalLocation,
      GameMode originalGameMode,
      String targetPlayerName,
      List<ReplayFrame> frames,
      CompletableFuture<ReplaySession> future
   ) {
      try {
         this.foliaLib.getImpl().runAtLocation(initialLocation, wrappedTask -> {
            if (!this.isReplayStartAllowed()) {
               future.complete(null);
            } else {
               ArmorStand playerEntity = null;
               ArmorStand nameEntity = null;

               try {
                  synchronized (this.replayLifecycleLock) {
                     if (!this.isReplayStartAllowed()) {
                        future.complete(null);
                        return;
                     }

                     playerEntity = world.spawn(initialLocation, ArmorStand.class);
                     this.configurePlayerReplayEntity(playerEntity, targetPlayerName);
                     this.sessionManager.registerReplayEntity(playerEntity);
                     nameEntity = world.spawn(initialLocation, ArmorStand.class);
                     this.configureNameReplayEntity(nameEntity, targetPlayerName);
                     this.sessionManager.registerReplayEntity(nameEntity);
                  }

                  this.hideReplayEntitiesFromOnlinePlayers(playerEntity, nameEntity);
                  this.plugin.logDebug("成功创建回放实体: " + playerEntity.getUniqueId() + " / " + nameEntity.getUniqueId());
                  this.teleportViewerAndCreateSession(player, initialLocation, originalLocation, originalGameMode, frames, playerEntity, nameEntity, future);
               } catch (Exception ex) {
                  this.rollbackReplayStart(player, null, playerEntity, nameEntity, originalLocation, originalGameMode, false);
                  future.completeExceptionally(ex);
               }
            }
         }).whenComplete((ignored, error) -> {
            if (error != null) {
               future.completeExceptionally(error);
            }
         });
      } catch (Exception ex) {
         future.completeExceptionally(ex);
      }
   }

   private void configurePlayerReplayEntity(ArmorStand playerEntity, String targetPlayerName) {
      playerEntity.setVisible(false);
      playerEntity.setGravity(false);
      playerEntity.setMarker(true);
      playerEntity.setSmall(false);
      playerEntity.setArms(true);
      playerEntity.setBasePlate(false);
      playerEntity.setGlowing(true);
      ItemStack item = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta itemMeta = (SkullMeta)item.getItemMeta();
      if (itemMeta != null) {
         itemMeta.setOwner(targetPlayerName);
         item.setItemMeta(itemMeta);
      }

      playerEntity.getEquipment().setHelmet(item);
      playerEntity.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
      playerEntity.getEquipment().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
      playerEntity.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
   }

   private void configureNameReplayEntity(ArmorStand nameEntity, String targetPlayerName) {
      nameEntity.setVisible(false);
      nameEntity.setGravity(false);
      nameEntity.setCustomNameVisible(true);
      nameEntity.setCustomName(targetPlayerName);
      nameEntity.setMarker(true);
   }

   private void hideReplayEntitiesFromOnlinePlayers(ArmorStand playerEntity, ArmorStand nameEntity) {
      for (Player player : Bukkit.getOnlinePlayers()) {
         try {
            this.foliaLib.getImpl().runAtEntity(player, wrappedTask -> {
               player.hideEntity(this.plugin, playerEntity);
               player.hideEntity(this.plugin, nameEntity);
            }).exceptionally(error -> {
               this.plugin.logDebug("隐藏回放实体失败: " + this.getErrorMessage(error));
               return null;
            });
         } catch (Exception ex) {
            this.plugin.logDebug("无法调度回放实体可见性更新: " + ex.getMessage());
         }
      }
   }

   private void teleportViewerAndCreateSession(
      Player player,
      Location initialLocation,
      Location originalLocation,
      GameMode originalGameMode,
      List<ReplayFrame> frames,
      ArmorStand playerEntity,
      ArmorStand nameEntity,
      CompletableFuture<ReplaySession> future
   ) {
      this.foliaLib.getImpl().teleportAsync(player, initialLocation).whenComplete((teleported, error) -> {
         if (error == null && Boolean.TRUE.equals(teleported)) {
            this.initializeReplaySession(player, originalLocation, originalGameMode, frames, playerEntity, nameEntity, future);
         } else {
            this.rollbackReplayStart(player, null, playerEntity, nameEntity, originalLocation, originalGameMode, true);
            future.completeExceptionally(error != null ? error : new IllegalStateException("回放观察者传送失败"));
         }
      });
   }

   private void initializeReplaySession(
      Player player,
      Location originalLocation,
      GameMode originalGameMode,
      List<ReplayFrame> frames,
      ArmorStand playerEntity,
      ArmorStand nameEntity,
      CompletableFuture<ReplaySession> future
   ) {
      try {
         this.foliaLib.getImpl().runAtEntityWithFallback(player, wrappedTask -> {
            ReplaySession replaySession = null;

            try {
               synchronized (this.replayLifecycleLock) {
                  if (!this.isReplayStartAllowed()) {
                     this.rollbackReplayStart(player, null, playerEntity, nameEntity, originalLocation, originalGameMode, true);
                     future.complete(null);
                     return;
                  }

                  player.setGameMode(GameMode.SPECTATOR);
                  player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
                  player.showEntity(this.plugin, playerEntity);
                  player.showEntity(this.plugin, nameEntity);
                  replaySession = new ReplaySession(null, playerEntity, nameEntity, originalLocation, originalGameMode, frames, player);
                  this.sessionManager.addSession(player.getUniqueId(), replaySession);
                  replaySession.initializeBlocks();
                  this.sendReplayInstructions(player);
                  this.startOptimizedReplayTask(player, replaySession);
               }

               future.complete(replaySession);
               this.plugin.logDebug("回放会话创建完全成功");
            } catch (Exception ex) {
               this.rollbackReplayStart(player, replaySession, playerEntity, nameEntity, originalLocation, originalGameMode, true);
               future.completeExceptionally(ex);
            }
         }, () -> {
            this.rollbackReplayStart(player, null, playerEntity, nameEntity, originalLocation, originalGameMode, false);
            future.complete(null);
         }).whenComplete((entityTaskResult, error) -> this.completeIfEntityTaskRejected(future, entityTaskResult, error));
      } catch (Exception ex) {
         this.rollbackReplayStart(player, null, playerEntity, nameEntity, originalLocation, originalGameMode, false);
         future.completeExceptionally(ex);
      }
   }

   private void rollbackReplayStart(
      Player player,
      ReplaySession replaySession,
      ArmorStand playerEntity,
      ArmorStand nameEntity,
      Location originalLocation,
      GameMode originalGameMode,
      boolean restoreViewer
   ) {
      if (replaySession != null) {
         this.sessionManager.removeSession(player.getUniqueId(), replaySession);
      }

      this.sessionManager.removeArmorStandSafely(playerEntity, "未完成的回放玩家实体");
      this.sessionManager.removeArmorStandSafely(nameEntity, "未完成的回放名称实体");
      if (restoreViewer && this.plugin.isEnabled()) {
         try {
            this.restoreViewerState(player, originalGameMode);
            if (originalLocation != null) {
               this.foliaLib.getImpl().teleportAsync(player, originalLocation);
            }
         } catch (Exception ex) {
            this.plugin.logDebug("恢复失败回放的观察者状态时出错: " + ex.getMessage());
         }
      }
   }

   private void completeIfEntityTaskRejected(CompletableFuture<ReplaySession> future, EntityTaskResult entityTaskResult, Throwable error) {
      if (error != null) {
         future.completeExceptionally(error);
      } else if (entityTaskResult != EntityTaskResult.SUCCESS) {
         future.complete(null);
      }
   }

   private void sendViewerMessage(Player player, String message) {
      if (this.plugin.isEnabled()) {
         try {
            if (this.foliaLib.getImpl().isOwnedByCurrentRegion(player)) {
               player.sendMessage(message);
               return;
            }

            this.foliaLib.getImpl().runAtEntity(player, wrappedTask -> player.sendMessage(message));
         } catch (Exception ex) {
            this.plugin.logDebug("无法向回放观察者发送消息: " + ex.getMessage());
         }
      }
   }

   private boolean isReplayStartAllowed() {
      return !this.shuttingDown.get() && this.plugin.isEnabled();
   }

   private void restoreViewerState(Player player, GameMode originalGameMode) {
      if (player != null && originalGameMode != null && this.plugin.isEnabled()) {
         try {
            if (this.foliaLib.getImpl().isOwnedByCurrentRegion(player)) {
               player.setGameMode(originalGameMode);
               player.removePotionEffect(PotionEffectType.NIGHT_VISION);
               return;
            }

            this.foliaLib.getImpl().runAtEntity(player, wrappedTask -> {
               player.setGameMode(originalGameMode);
               player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            });
         } catch (Exception ex) {
            this.plugin.logDebug("恢复回放观察者状态失败: " + ex.getMessage());
         }
      }
   }

   private String getErrorMessage(Throwable error) {
      Throwable rootCause = error;

      while (rootCause.getCause() != null) {
         rootCause = rootCause.getCause();
      }

      return rootCause.getMessage() != null ? rootCause.getMessage() : rootCause.getClass().getSimpleName();
   }

   private void sendReplayInstructions(Player player) {
      player.sendMessage("§e=== 回放控制指令 ===");
      player.sendMessage("§6/nax replay stop §7- 停止回放");
      player.sendMessage("§6/nax replay pause §7- 暂停/恢复回放");
      player.sendMessage("§6/nax replay forward §7- 向前跳转");
      player.sendMessage("§6/nax replay backward §7- 向后跳转");
      player.sendMessage("§6/nax replay tp §7- 传送到当前回放位置");
      player.sendMessage("§e==================");
   }

   private void displayOreSummary(Player player, Map<Material, Integer> oreCounts) {
      if (!oreCounts.isEmpty()) {
         player.sendMessage("§a回放结束，矿石挖掘统计：");
         oreCounts.entrySet()
            .stream()
            .sorted((firstEntry, secondEntry) -> secondEntry.getValue().compareTo(firstEntry.getValue()))
            .forEach(entry -> player.sendMessage(String.format("§e%s: §6%d", this.formatOreName(entry.getKey().name()), entry.getValue())));
      }
   }

   private void startOptimizedReplayTask(Player player, ReplaySession replaySession) {
      this.plugin.logDebug("开始启动优化的回放任务，玩家: " + player.getName());
      UUID viewerId = player.getUniqueId();
      AtomicInteger frameIndex = new AtomicInteger(0);
      AtomicLong tickCounter = new AtomicLong(0L);
      AtomicBoolean replayFinished = new AtomicBoolean(false);
      this.plugin.logDebug("开始预处理被破坏的方块");
      this.preprocessBrokenBlocks(replaySession.getFrames(), player.getWorld());
      this.plugin.logDebug("预处理被破坏的方块完成");
      WrappedTask wrappedTask = this.foliaLib.getImpl().runAtEntityTimer(player, () -> {
         if (!this.sessionManager.hasActiveSession(viewerId)) {
            this.plugin.logDebug("检测到会话已被移除，停止回放任务");
            this.cancelSessionTask(replaySession);
         } else if (replayFinished.get()) {
            this.plugin.logDebug("检测到回放已完成标志，跳过执行");
         } else if (replaySession.isPaused()) {
            this.displayProgressBar(player, replaySession);
         } else {
            tickCounter.incrementAndGet();
            int currentFrameIndex = frameIndex.get();
            if (currentFrameIndex >= replaySession.getFrames().size()) {
               if (replayFinished.compareAndSet(false, true)) {
                  this.plugin.logDebug("回放正常结束，帧索引: " + currentFrameIndex + "/" + replaySession.getFrames().size());
                  player.sendMessage("§a回放结束。");
                  this.displayOreSummary(player, replaySession.getOreCount());
                  this.cancelSessionTask(replaySession);
                  this.plugin.logDebug("开始执行回放停止逻辑");
                  this.stopReplay(player);
               } else {
                  this.plugin.logDebug("回放结束逻辑已被其他线程执行，跳过");
               }
            } else {
               ReplayFrame replayFrame = replaySession.getFrames().get(currentFrameIndex);
               Location correctLocation = replayFrame.getCorrectLocation();
               if (correctLocation == null) {
                  this.plugin.logDebug("回放世界不存在: " + replayFrame.worldName + ", 跳过此帧");
               } else if (!player.getWorld().equals(correctLocation.getWorld())) {
                  this.plugin.logDebug("检测到跨世界传送需求: " + player.getWorld().getName() + " -> " + correctLocation.getWorld().getName());
                  this.foliaLib.getImpl().teleportAsync(player, correctLocation).thenRun(() -> this.plugin.logDebug("跨世界传送完成: " + player.getWorld().getName()));
               } else {
                  switch (replayFrame.action) {
                     case "MOVE":
                        this.handlePlayerMove(replaySession, correctLocation);
                        replaySession.addPathLocation(correctLocation);
                        break;
                     case "BREAK":
                        if (replayFrame.blockType != null) {
                           Material material = Material.valueOf(replayFrame.blockType);
                           if (OreParticleRenderer.isSupportedOre(material)) {
                              replaySession.addBrokenOre(correctLocation, material);
                              replaySession.incrementOreCount(material);
                              this.plugin.logDebug("玩家挖掘矿石: " + material.name() + " 在位置: " + correctLocation);
                              player.sendMessage(String.format("§e玩家挖掘了 §6%s§e！", this.formatOreName(material.name())));
                           }
                        }
                  }

                  if (tickCounter.get() % 5L == 0L) {
                     this.restoreOptimizedBlocksAroundLocation(player, correctLocation, replaySession);
                  }

                  if (tickCounter.get() % 2L == 0L) {
                     replaySession.displayParticles(tickCounter.get());
                  }

                  replaySession.setCurrentIndex(frameIndex.incrementAndGet());
                  this.displayProgressBar(player, replaySession);
               }
            }
         }
      }, () -> {
         if (this.sessionManager.removeSession(viewerId, replaySession)) {
            this.sessionManager.cleanupSessionEntities(replaySession);
         }
      }, 1L, 1L);
      replaySession.setTask(wrappedTask);
      this.plugin.logDebug("回放任务启动完成，任务引用已设置");
   }

   private void preprocessBrokenBlocks(List<ReplayFrame> frames, World world) {
      for (ReplayFrame replayFrame : frames) {
         if ("BREAK".equals(replayFrame.action) && replayFrame.blockType != null) {
            Location frameLocation = replayFrame.getLocation(world);
            ReplayCommand.ChunkCoordinate chunkCoordinate = new ReplayCommand.ChunkCoordinate(frameLocation);
            ReplayCommand.BlockPosition blockPosition = new ReplayCommand.BlockPosition(frameLocation);
            this.brokenBlocksCache.computeIfAbsent(chunkCoordinate, coord -> new ConcurrentHashMap<>()).put(blockPosition, Material.valueOf(replayFrame.blockType));
         }
      }

      this.plugin.logDebug("预处理完成，缓存了 " + this.brokenBlocksCache.size() + " 个区块的方块数据");
   }

   private boolean cancelSessionTask(ReplaySession replaySession) {
      if (replaySession == null) {
         return false;
      } else {
         WrappedTask task = replaySession.getTask();
         if (task != null && !task.isCancelled()) {
            task.cancel();
            replaySession.setTask(null);
            return true;
         } else {
            return false;
         }
      }
   }

   private void restoreOptimizedBlocksAroundLocation(Player player, Location centerLocation, ReplaySession replaySession) {
      this.workerPool
         .submit(
            () -> {
               List<ReplayCommand.BlockChange> blockChanges = new ArrayList<>();
               AtomicInteger checkedBlockCount = new AtomicInteger(0);
               AtomicInteger restoredBlockCount = new AtomicInteger(0);
               ReplayCommand.ChunkCoordinate centerChunk = new ReplayCommand.ChunkCoordinate(centerLocation);

               for (int dx = -1; dx <= 1; dx++) {
                  for (int dz = -1; dz <= 1; dz++) {
                     ReplayCommand.ChunkCoordinate neighborChunk = new ReplayCommand.ChunkCoordinate(
                        new Location(centerLocation.getWorld(), (centerChunk.chunkX + dx) * 16, centerLocation.getY(), (centerChunk.chunkZ + dz) * 16)
                     );
                     Map<ReplayCommand.BlockPosition, Material> chunkBlocks = this.brokenBlocksCache.get(neighborChunk);
                     if (chunkBlocks != null) {
                        chunkBlocks.entrySet()
                           .stream()
                           .filter(entry -> isBlockWithinRestoreRadius(centerLocation, entry))
                           .limit(50L)
                           .forEach(entry -> collectBlockChangeForRestoration(centerLocation, checkedBlockCount, replaySession, blockChanges, restoredBlockCount, entry));
                     }
                  }
               }

               if (!blockChanges.isEmpty()) {
                  this.foliaLib.getImpl().runAtEntity(player, wrappedTask -> this.applyRestoredBlockChanges(blockChanges, player, restoredBlockCount, checkedBlockCount));
               }
            }
         );
   }

   private void handlePlayerMove(ReplaySession replaySession, Location targetLocation) {
      Location standLocation = targetLocation.clone().add(0.0, -0.2, 0.0);
      ArmorStand playerEntity = replaySession.getPlayerEntity();
      ArmorStand nameEntity = replaySession.getNameEntity();
      if (playerEntity != null) {
         this.foliaLib.getImpl().teleportAsync(playerEntity, standLocation);
      }

      if (nameEntity != null) {
         this.foliaLib.getImpl().teleportAsync(nameEntity, standLocation.clone().add(0.0, 1.7, 0.0));
      }

      Player viewer = replaySession.getViewer();
      this.checkAndBreakBlock(replaySession, viewer, targetLocation.getBlock().getLocation());
      this.checkAndBreakBlock(replaySession, viewer, targetLocation.clone().add(1.0, 0.0, 0.0).getBlock().getLocation());
      this.checkAndBreakBlock(replaySession, viewer, targetLocation.clone().add(-1.0, 0.0, 0.0).getBlock().getLocation());
      this.checkAndBreakBlock(replaySession, viewer, targetLocation.clone().add(0.0, 1.0, 0.0).getBlock().getLocation());
      this.checkAndBreakBlock(replaySession, viewer, targetLocation.clone().add(0.0, -1.0, 0.0).getBlock().getLocation());
      this.checkAndBreakBlock(replaySession, viewer, targetLocation.clone().add(0.0, 0.0, 1.0).getBlock().getLocation());
      this.checkAndBreakBlock(replaySession, viewer, targetLocation.clone().add(0.0, 0.0, -1.0).getBlock().getLocation());
   }

   private void checkAndBreakBlock(ReplaySession replaySession, Player player, Location blockLocation) {
      if (replaySession.isBlockBroken(blockLocation)) {
         player.sendBlockChange(blockLocation, Material.AIR.createBlockData());
         replaySession.markBlockBrokenInReplay(blockLocation);
         this.plugin.logDebug("模拟破坏方块: " + blockLocation);
      }
   }

   private void displayProgressBar(Player player, ReplaySession replaySession) {
      if (!replaySession.getFrames().isEmpty()) {
         double progress = (double)replaySession.getCurrentIndex() / replaySession.getFrames().size();
         int barLength = 20;
         int filledBars = (int)Math.round(progress * 20.0);
         filledBars = Math.min(Math.max(filledBars, 0), barLength);
         StringBuilder progressBar = new StringBuilder();
         progressBar.append("§e回放进度: §r");
         progressBar.append("§a[");

         for (int i = 0; i < barLength; i++) {
            if (i < filledBars) {
               progressBar.append("§2|");
            } else {
               progressBar.append("§7|");
            }
         }

         progressBar.append("§a] ");
         progressBar.append(String.format("§e%.1f%%", progress * 100.0));
         progressBar.append(String.format(" §7(%d/%d)", replaySession.getCurrentIndex(), replaySession.getFrames().size()));
         player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(progressBar.toString()));
      }
   }

   public void startReplay(Player player, String fileName) {
      this.plugin.logDebug("收到启动回放请求，玩家: " + player.getName() + ", 文件: " + fileName);
      if (this.sessionManager.hasActiveSession(player.getUniqueId())) {
         this.plugin.logDebug("玩家已有活跃回放会话，正在停止当前回放: " + player.getName());
         player.sendMessage("§c你已经在观看一个回放。正在停止当前回放...");
         this.stopReplay(player);
      }

      File replayFile = new File(this.plugin.getDataFolder(), "replays/" + fileName);
      this.plugin.logDebug("回放文件路径: " + replayFile.getAbsolutePath());
      if (replayFile.exists()) {
         this.plugin.logDebug("回放文件存在，文件大小: " + replayFile.length() + " 字节");
         this.playReplayAsync(player, replayFile).thenAccept(replaySession -> {
            if (replaySession != null) {
               this.plugin.logDebug("回放会话创建成功，无需重复添加到会话管理器");
               this.plugin.logDebug("回放启动完成，玩家: " + player.getName());
            } else {
               this.plugin.logDebug("回放会话创建失败，玩家: " + player.getName());
            }
         }).exceptionally(error -> {
            this.plugin.logDebug("回放创建过程中发生异常: " + error.getMessage());
            error.printStackTrace();
            player.sendMessage("§c创建回放会话时出错: " + error.getMessage());
            return null;
         });
      } else {
         this.plugin.logDebug("找不到回放文件: " + replayFile.getAbsolutePath());
         player.sendMessage("§c找不到指定的回放文件。");
      }
   }

   private String formatOreName(String materialName) {
      return Arrays.stream(materialName.split("_"))
         .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
         .collect(Collectors.joining(" "))
         .replace("Ore", "矿石");
   }

   private void applyRestoredBlockChanges(List blockChanges, Player player, AtomicInteger restoredCount, AtomicInteger checkedCount) {
      for (Object item : blockChanges) {
         ReplayCommand.BlockChange blockChange = (ReplayCommand.BlockChange)item;
         player.sendBlockChange(blockChange.location, blockChange.data);
      }

      this.plugin.logDebug("优化版恢复了 " + restoredCount.get() + "/" + checkedCount.get() + " 个方块");
   }

   private static void collectBlockChangeForRestoration(
      Location centerLocation, AtomicInteger checkedCount, ReplaySession replaySession, List blockChanges, AtomicInteger restoredCount, Entry entry
   ) {
      ReplayCommand.BlockPosition blockPosition = (ReplayCommand.BlockPosition)entry.getKey();
      Material material = (Material)entry.getValue();
      Location blockLocation = new Location(centerLocation.getWorld(), blockPosition.blockX, blockPosition.blockY, blockPosition.blockZ);
      checkedCount.incrementAndGet();
      if (!replaySession.isBlockBrokenInReplay(blockLocation) && replaySession.isBlockBroken(blockLocation)) {
         blockChanges.add(new ReplayCommand.BlockChange(blockLocation, material.createBlockData()));
         restoredCount.incrementAndGet();
      }
   }

   private static boolean isBlockWithinRestoreRadius(Location centerLocation, Entry entry) {
      ReplayCommand.BlockPosition blockPosition = (ReplayCommand.BlockPosition)entry.getKey();
      double distance = Math.sqrt(
         Math.pow(blockPosition.blockX - centerLocation.getX(), 2.0)
            + Math.pow(blockPosition.blockY - centerLocation.getY(), 2.0)
            + Math.pow(blockPosition.blockZ - centerLocation.getZ(), 2.0)
      );
      return distance <= 12.0;
   }

   private boolean exceedsCacheTargetSize(int targetSize) {
      return this.brokenBlocksCache.size() > targetSize;
   }

   public static void setOptimizedModeEnabled(boolean enabled) {
      optimizedModeEnabled = enabled;
   }

   private static class BlockChange {
      final Location location;
      final BlockData data;

      BlockChange(Location location, BlockData data) {
         this.location = location;
         this.data = data;
      }
   }

   public static class BlockPosition {
      public final int blockX;
      public final int blockY;
      public final int blockZ;

      public BlockPosition(Location location) {
         this.blockX = location.getBlockX();
         this.blockY = location.getBlockY();
         this.blockZ = location.getBlockZ();
      }

      @Generated
      public int getX() {
         return this.blockX;
      }

      @Generated
      public int getY() {
         return this.blockY;
      }

      @Generated
      public int getZ() {
         return this.blockZ;
      }

      @Generated
      @Override
      public boolean equals(Object other) {
         if (other == this) {
            return true;
         }

         if (other instanceof ReplayCommand.BlockPosition blockPosition) {
            if (!blockPosition.canEqual(this)) {
               return false;
            } else if (this.getX() != blockPosition.getX()) {
               return false;
            } else {
               return this.getY() != blockPosition.getY() ? false : this.getZ() == blockPosition.getZ();
            }
         } else {
            return false;
         }
      }

      @Generated
      protected boolean canEqual(Object other) {
         return other instanceof ReplayCommand.BlockPosition;
      }

      @Generated
      @Override
      public int hashCode() {
         int hash = 59 + this.getX();
         hash = hash * 59 + this.getY();
         return hash * 59 + this.getZ();
      }
   }

   private static class ChunkCoordinate {
      final int chunkX;
      final int chunkZ;

      ChunkCoordinate(Location location) {
         this.chunkX = location.getBlockX() >> 4;
         this.chunkZ = location.getBlockZ() >> 4;
      }

      @Override
      public boolean equals(Object other) {
         if (this == other) {
            return true;
         } else if (other != null && this.getClass() == other.getClass()) {
            ReplayCommand.ChunkCoordinate chunkCoordinate = (ReplayCommand.ChunkCoordinate)other;
            return this.chunkX == chunkCoordinate.chunkX && this.chunkZ == chunkCoordinate.chunkZ;
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.chunkX, this.chunkZ);
      }
   }
}
