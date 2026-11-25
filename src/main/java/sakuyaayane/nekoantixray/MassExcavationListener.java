package sakuyaayane.nekoantixray;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;
import lombok.Generated;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import sakuyaayane.nekoantixray.lib.folialib.FoliaLib;

public class MassExcavationListener implements Listener {
   private final NekoAntiXray plugin;
   private final FoliaLib foliaLib;
   private final ConfigManager configManager;
   private final InternalBypassManager internalBypassManager;
   private final List<Material> targetMaterials;
   private final Map<UUID, List<MassExcavationListener.ExcavatedBlockRecord>> playerBlockRecords = new ConcurrentHashMap<>();
   private final Map<UUID, LocalDateTime> bypassGrantedTimes = new ConcurrentHashMap<>();
   private final Map<UUID, Boolean> playerConfirmationResponses = new ConcurrentHashMap<>();
   private final Map<UUID, LocalDateTime> lastPromptTimes = new ConcurrentHashMap<>();
   private final Map<UUID, LocalDateTime> denialCooldownUntil = new ConcurrentHashMap<>();
   private final int detectionTimeWindowSeconds;
   private final int blockCountThreshold;
   private final int minVolumeThreshold;
   private final int bypassDurationSeconds;
   private final int confirmationCooldownSeconds;
   private final int denialCooldownSeconds;
   private final double minDensityThreshold;
   private final int minHorizontalAreaThreshold;

   public MassExcavationListener(NekoAntiXray plugin, FoliaLib foliaLib, InternalBypassManager internalBypassManager) {
      this.plugin = plugin;
      this.foliaLib = foliaLib;
      this.configManager = plugin.getConfigManager();
      this.internalBypassManager = internalBypassManager;
      this.detectionTimeWindowSeconds = this.configManager.getCheckXrayMassExcavationDetectionTimeWindow();
      this.blockCountThreshold = this.configManager.getCheckXrayMassExcavationBlockCountThreshold();
      this.minVolumeThreshold = this.configManager.getCheckXrayMassExcavationMinVolumeThreshold();
      this.bypassDurationSeconds = this.configManager.getCheckXrayMassExcavationBypassDuration();
      this.confirmationCooldownSeconds = this.configManager.getCheckXrayMassExcavationConfirmationCooldown();
      this.denialCooldownSeconds = this.configManager.getCheckXrayMassExcavationDenialCooldownDuration();
      this.minDensityThreshold = this.configManager.getCheckXrayMassExcavationMinDensityThreshold();
      this.minHorizontalAreaThreshold = this.configManager.getCheckXrayMassExcavationMinHorizontalAreaThreshold();
      this.targetMaterials = this.configManager.getMassExcavationTargetBlocks();
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
      this.setupCleanupTask();
   }

   @EventHandler
   public void onBlockBreak(BlockBreakEvent event) {
      if (this.configManager.isCheckXrayMassExcavationEnabled() && this.configManager.isCheckXrayEnable()) {
         Player player = event.getPlayer();
         if (!player.hasPermission("nekoantixray.bypass")) {
            UUID playerId = player.getUniqueId();
            this.recordBlockBreak(playerId, event.getBlock());
            this.checkMassExcavation(player);
         }
      }
   }

   @EventHandler
   public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
      String commandMessage = event.getMessage().toLowerCase();
      Player player = event.getPlayer();
      UUID playerId = player.getUniqueId();
      if (commandMessage.startsWith("/nax_confirm_excavation")) {
         event.setCancelled(true);
         LocalDateTime lastPromptTime = this.lastPromptTimes.getOrDefault(playerId, LocalDateTime.MIN);
         if (LocalDateTime.now().isAfter(lastPromptTime.plusSeconds(this.confirmationCooldownSeconds * 2))) {
            player.sendMessage(this.configManager.getCheckXrayMassExcavationMessagePrefix() + "确认请求已过期或无效。");
            this.plugin.logDebug("[大规模挖掘] 玩家 " + player.getName() + " 的确认请求已过期 (命令)");
         } else if (Boolean.TRUE.equals(this.playerConfirmationResponses.get(playerId))) {
            LocalDateTime bypassGrantedTime = this.bypassGrantedTimes.getOrDefault(playerId, LocalDateTime.MIN);
            if (LocalDateTime.now().isBefore(bypassGrantedTime.plusSeconds(this.bypassDurationSeconds))) {
               player.sendMessage(this.configManager.getCheckXrayMassExcavationMessagePrefix() + "您已确认且临时权限仍然有效。");
               this.plugin.logDebug("[大规模挖掘] 玩家 " + player.getName() + " 尝试重复确认，已有有效权限");
            } else {
               this.plugin.logDebug("[大规模挖掘] 玩家 " + player.getName() + " 重新确认 (豁免已过期)");
               this.playerConfirmationResponses.put(playerId, Boolean.TRUE);
               this.grantExcavationBypass(player);
            }
         } else {
            this.plugin.logDebug("[大规模挖掘] 玩家 " + player.getName() + " 已确认大规模挖掘 (命令)");
            this.playerConfirmationResponses.put(playerId, Boolean.TRUE);
            this.grantExcavationBypass(player);
         }
      } else if (commandMessage.startsWith("/nax_deny_excavation")) {
         event.setCancelled(true);
         LocalDateTime lastPromptTime = this.lastPromptTimes.getOrDefault(playerId, LocalDateTime.MIN);
         if (LocalDateTime.now().isAfter(lastPromptTime.plusSeconds(this.confirmationCooldownSeconds * 2))) {
            player.sendMessage(this.configManager.getCheckXrayMassExcavationMessagePrefix() + "确认请求已过期或无效。");
            this.plugin.logDebug("[大规模挖掘] 玩家 " + player.getName() + " 的否认请求已过期 (命令)");
         } else if (this.playerConfirmationResponses.containsKey(playerId)) {
            player.sendMessage(this.configManager.getCheckXrayMassExcavationMessagePrefix() + "您已经回应过此请求。");
            this.plugin.logDebug("[大规模挖掘] 玩家 " + player.getName() + " 尝试重复回应 (否认)");
         } else {
            this.plugin.logDebug("[大规模挖掘] 玩家 " + player.getName() + " 否认了大规模挖掘 (命令)");
            this.playerConfirmationResponses.put(playerId, Boolean.FALSE);
            LocalDateTime cooldownUntil = LocalDateTime.now().plusSeconds(this.denialCooldownSeconds);
            this.denialCooldownUntil.put(playerId, cooldownUntil);
            this.plugin.logDebug("[大规模挖掘] 玩家 " + player.getName() + " 进入否认冷却期，直至: " + cooldownUntil);
            player.sendMessage(this.configManager.getCheckXrayMassExcavationMessagePrefix() + "已记录您的回应。");
         }
      }
   }

   private static boolean isBlockRecordExpired(LocalDateTime cutoffTime, MassExcavationListener.ExcavatedBlockRecord excavatedBlockRecord) {
      return excavatedBlockRecord.getTimestamp().isBefore(cutoffTime);
   }

   private void recordBlockBreak(UUID playerId, Block block) {
      if (this.targetMaterials == null || this.targetMaterials.isEmpty() || this.targetMaterials.contains(block.getType())) {
         List<MassExcavationListener.ExcavatedBlockRecord> records = this.playerBlockRecords.computeIfAbsent(playerId, id -> new ArrayList<>());
         records.add(new MassExcavationListener.ExcavatedBlockRecord(block.getType(), block.getLocation()));
         int maxRecordCount = this.blockCountThreshold * 3;

         while (records.size() > maxRecordCount) {
            records.remove(0);
         }

         Player player = Bukkit.getPlayer(playerId);
         if (player != null && this.configManager.isDebug()) {
            this.plugin
               .logDebug(
                  "[大规模挖掘] 玩家 "
                     + player.getName()
                     + " 破坏方块: "
                     + block.getType()
                     + " 在坐标: "
                     + block.getX()
                     + ", "
                     + block.getY()
                     + ", "
                     + block.getZ()
                     + ". 当前记录数: "
                     + records.size()
               );
         }
      } else if (this.configManager.isDebug()) {
         String playerName = Bukkit.getPlayer(playerId).getName();
         String targetBlockNames = this.targetMaterials.stream().map(Material::name).collect(Collectors.joining(", "));
         this.plugin.logDebug("[大规模挖掘] 玩家 " + playerName + " 破坏的方块 " + block.getType() + " 不在目标检测列表 " + targetBlockNames + " 中，已忽略。");
      }
   }

   private void checkMassExcavation(Player player) {
      UUID playerId = player.getUniqueId();
      List<MassExcavationListener.ExcavatedBlockRecord> allRecords = this.playerBlockRecords.get(playerId);
      if (allRecords != null && allRecords.size() >= this.blockCountThreshold) {
         LocalDateTime lastPromptTime = this.lastPromptTimes.getOrDefault(playerId, LocalDateTime.MIN);
         if (LocalDateTime.now().isBefore(lastPromptTime.plusSeconds(this.confirmationCooldownSeconds))) {
            this.plugin.logDebug("[大规模挖掘] 玩家 " + player.getName() + " 在确认冷却期内，跳过检测");
         } else {
            LocalDateTime bypassGrantedTime = this.bypassGrantedTimes.getOrDefault(playerId, LocalDateTime.MIN);
            if (LocalDateTime.now().isBefore(bypassGrantedTime.plusSeconds(this.bypassDurationSeconds))) {
               this.plugin.logDebug("[大规模挖掘] 玩家 " + player.getName() + " 在临时Bypass权限有效期内，跳过检测");
            } else {
               LocalDateTime windowStart = LocalDateTime.now().minusSeconds(this.detectionTimeWindowSeconds);
               List<MassExcavationListener.ExcavatedBlockRecord> recentRecords = new ArrayList<>();

               for (int i = allRecords.size() - 1; i >= 0; i--) {
                  MassExcavationListener.ExcavatedBlockRecord record = allRecords.get(i);
                  if (!record.getTimestamp().isAfter(windowStart)) {
                     break;
                  }

                  recentRecords.add(record);
               }

               if (recentRecords.size() < this.blockCountThreshold) {
                  if (this.configManager.isDebug()) {
                     this.plugin.logDebug("[大规模挖掘] 玩家 " + player.getName() + " 时间窗口内方块数量不足 (" + recentRecords.size() + "/" + this.blockCountThreshold + ")");
                  }
               } else {
                  this.plugin.logDebug("[大规模挖掘] 玩家 " + player.getName() + " 时间窗口内破坏方块数达到阈值 (" + recentRecords.size() + "/" + this.blockCountThreshold + ")，开始体积检测");
                  this.foliaLib
                     .getImpl()
                     .runAtEntity(
                        player,
                        wrappedTask -> {
                           try {
                              List<MassExcavationListener.ExcavatedBlockRecord> latestRecords = this.playerBlockRecords.get(playerId);
                              if (latestRecords == null) {
                                 return;
                              }

                              LocalDateTime asyncWindowStart = LocalDateTime.now().minusSeconds(this.detectionTimeWindowSeconds);
                              List<MassExcavationListener.ExcavatedBlockRecord> windowRecords = new ArrayList<>();

                              for (int i = latestRecords.size() - 1; i >= 0; i--) {
                                 MassExcavationListener.ExcavatedBlockRecord blockRecord = latestRecords.get(i);
                                 if (!blockRecord.getTimestamp().isAfter(asyncWindowStart)) {
                                    break;
                                 }

                                 windowRecords.add(blockRecord);
                              }

                              if (windowRecords.size() < this.blockCountThreshold) {
                                 this.plugin
                                    .logDebug(
                                       "[大规模挖掘] 玩家 " + player.getName() + " (异步) 时间窗口内方块数量不足 (" + windowRecords.size() + "/" + this.blockCountThreshold + ")"
                                    );
                                 return;
                              }

                              int minX = Integer.MAX_VALUE;
                              int minY = Integer.MAX_VALUE;
                              int minZ = Integer.MAX_VALUE;
                              int maxX = Integer.MIN_VALUE;
                              int maxY = Integer.MIN_VALUE;
                              int maxZ = Integer.MIN_VALUE;

                              for (MassExcavationListener.ExcavatedBlockRecord windowRecord : windowRecords) {
                                 Location recordLocation = windowRecord.getLocation();
                                 int blockX = recordLocation.getBlockX();
                                 int blockY = recordLocation.getBlockY();
                                 int blockZ = recordLocation.getBlockZ();
                                 minX = Math.min(minX, blockX);
                                 maxX = Math.max(maxX, blockX);
                                 minY = Math.min(minY, blockY);
                                 maxY = Math.max(maxY, blockY);
                                 minZ = Math.min(minZ, blockZ);
                                 maxZ = Math.max(maxZ, blockZ);
                              }

                              int boundingVolume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
                              int horizontalArea = (maxX - minX + 1) * (maxZ - minZ + 1);
                              double density = 0.0;
                              if (boundingVolume > 0) {
                                 density = (double)windowRecords.size() / boundingVolume;
                              }

                              String formattedDensity = String.format("%.3f", density);
                              String formattedMinDensity = String.format("%.3f", this.minDensityThreshold);
                              this.plugin
                                 .logDebug(
                                    "[大规模挖掘] 玩家 "
                                       + player.getName()
                                       + " 挖掘区域体积: "
                                       + boundingVolume
                                       + " (阈值: "
                                       + this.minVolumeThreshold
                                       + "), 水平面积: "
                                       + horizontalArea
                                       + " (阈值: "
                                       + this.minHorizontalAreaThreshold
                                       + "), 密度: "
                                       + formattedDensity
                                       + " (阈值: "
                                       + formattedMinDensity
                                       + ")"
                                 );
                              if (boundingVolume >= this.minVolumeThreshold && density >= this.minDensityThreshold && horizontalArea >= this.minHorizontalAreaThreshold) {
                                 LocalDateTime denialCooldown = this.denialCooldownUntil.get(playerId);
                                 if (denialCooldown != null && LocalDateTime.now().isBefore(denialCooldown)) {
                                    this.plugin.logDebug("[大规模挖掘] 玩家 " + player.getName() + " 处于否认冷却期，跳过确认请求。冷却至: " + denialCooldown);
                                    return;
                                 }

                                 Boolean confirmationResponse = this.playerConfirmationResponses.get(playerId);
                                 if (Boolean.FALSE.equals(confirmationResponse)) {
                                    this.plugin.logDebug("[大规模挖掘] 玩家 " + player.getName() + " 此前已否认，本次不再发送确认请求。");
                                    return;
                                 }

                                 LocalDateTime lastBypassTime = this.bypassGrantedTimes.getOrDefault(playerId, LocalDateTime.MIN);
                                 if (Boolean.TRUE.equals(confirmationResponse) && LocalDateTime.now().isBefore(lastBypassTime.plusSeconds(this.bypassDurationSeconds))) {
                                    this.plugin.logDebug("[大规模挖掘] 玩家 " + player.getName() + " 已确认且Bypass权限有效，不再发送确认请求。");
                                    return;
                                 }

                                 this.plugin.logDebug("[大规模挖掘] 玩家 " + player.getName() + " 检测到大规模挖掘 (体积满足条件)");
                                 this.sendConfirmationPrompt(player);
                                 this.lastPromptTimes.put(playerId, LocalDateTime.now());
                              }
                           } catch (Exception ex) {
                              this.plugin.getLogger().log(Level.SEVERE, "检测大规模挖掘时发生错误: " + ex.getMessage(), ex);
                           }
                        }
                     );
               }
            }
         }
      }
   }

   private void grantExcavationBypass(Player player) {
      UUID playerId = player.getUniqueId();
      this.plugin.logDebug("[大规模挖掘] 为玩家 " + player.getName() + " 授予内部检测豁免，持续时间: " + this.bypassDurationSeconds + "秒");
      this.internalBypassManager.grantBypass(player, this.bypassDurationSeconds, "大规模挖掘确认");
      this.bypassGrantedTimes.put(playerId, LocalDateTime.now());
      this.plugin.logDebug("[大规模挖掘] 内部豁免授予完成，玩家: " + player.getName());
      if (this.configManager.isCheckXrayMassExcavationNotificationEnabled()) {
         player.sendMessage(this.configManager.getCheckXrayMassExcavationNotificationMessage());
         this.plugin.logDebug("[大规模挖掘] 已发送豁免通知给玩家 " + player.getName());
      }
   }

   private void sendConfirmationPrompt(Player player) {
      String messagePrefix = this.configManager.getCheckXrayMassExcavationMessagePrefix();
      String confirmationMessage = this.configManager.getCheckXrayMassExcavationConfirmationMessage();
      String confirmButtonText = this.configManager.getCheckXrayMassExcavationConfirmButton();
      String denyButtonText = this.configManager.getCheckXrayMassExcavationDenyButton();
      TextComponent promptComponent = new TextComponent(messagePrefix + confirmationMessage + " ");
      TextComponent confirmComponent = new TextComponent(confirmButtonText);
      confirmComponent.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/nax_confirm_excavation"));
      confirmComponent.setHoverEvent(
         new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("点击确认你在进行大规模挖掘").color(ChatColor.GREEN).create())
      );
      TextComponent denyComponent = new TextComponent(" " + denyButtonText);
      denyComponent.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/nax_deny_excavation"));
      denyComponent.setHoverEvent(
         new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("点击否认你在进行大规模挖掘").color(ChatColor.RED).create())
      );
      promptComponent.addExtra(confirmComponent);
      promptComponent.addExtra(denyComponent);
      player.spigot().sendMessage(promptComponent);
   }

   private void setupCleanupTask() {
      long cleanupIntervalTicks = 6000L;
      this.foliaLib.getImpl().runTimer(wrappedTask -> this.cleanupExpiredData(), cleanupIntervalTicks, cleanupIntervalTicks);
   }

   private void cleanupExpiredData() {
      LocalDateTime now = LocalDateTime.now();
      LocalDateTime recordCutoff = now.minusSeconds(this.detectionTimeWindowSeconds * 5L);
      LocalDateTime bypassCutoff = now.minusSeconds(this.bypassDurationSeconds * 2L);
      this.plugin.logDebug("[大规模挖掘] 开始清理过期数据...");
      this.playerBlockRecords.entrySet().removeIf(entry -> {
         if (entry.getValue().isEmpty()) {
            return true;
         }

         entry.getValue().removeIf(excavatedBlockRecord -> isBlockRecordExpired(recordCutoff, excavatedBlockRecord));
         return entry.getValue().isEmpty();
      });
      this.bypassGrantedTimes.entrySet().removeIf(entry -> entry.getValue().isBefore(bypassCutoff));
      this.playerConfirmationResponses.entrySet().removeIf(entry -> {
         LocalDateTime grantedTime = this.bypassGrantedTimes.get(entry.getKey());
         if (grantedTime != null) {
            return grantedTime.isBefore(bypassCutoff);
         }

         LocalDateTime promptTime = this.lastPromptTimes.get(entry.getKey());
         return promptTime == null || promptTime.isBefore(now.minusSeconds(this.confirmationCooldownSeconds * 10L));
      });
      this.lastPromptTimes.entrySet().removeIf(entry -> entry.getValue().isBefore(bypassCutoff));
      this.denialCooldownUntil.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
      this.plugin.logDebug("[大规模挖掘] 过期数据清理完成。");
   }

   public void clearPlayerData(UUID playerId) {
      this.playerBlockRecords.remove(playerId);
      this.bypassGrantedTimes.remove(playerId);
      this.playerConfirmationResponses.remove(playerId);
      this.lastPromptTimes.remove(playerId);
      this.denialCooldownUntil.remove(playerId);
      this.plugin.logDebug("[大规模挖掘] 已清理玩家 " + playerId + " 的数据");
   }

   public void clearAllData() {
      this.playerBlockRecords.clear();
      this.bypassGrantedTimes.clear();
      this.playerConfirmationResponses.clear();
      this.lastPromptTimes.clear();
      this.denialCooldownUntil.clear();
      this.plugin.logDebug("[大规模挖掘] 已清理所有玩家的数据");
   }

   private static class ExcavatedBlockRecord {
      private final Material material;
      private final Location location;
      private final LocalDateTime timestamp;

      public ExcavatedBlockRecord(Material material, Location location) {
         this.material = material;
         this.location = location.clone();
         this.timestamp = LocalDateTime.now();
      }

      @Generated
      public Material getMaterial() {
         return this.material;
      }

      @Generated
      public Location getLocation() {
         return this.location;
      }

      @Generated
      public LocalDateTime getTimestamp() {
         return this.timestamp;
      }
   }
}
