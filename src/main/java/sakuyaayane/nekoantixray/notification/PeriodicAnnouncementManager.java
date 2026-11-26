package sakuyaayane.nekoantixray.notification;

import java.util.concurrent.ScheduledFuture;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.MiniMessage;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.serializer.legacy.LegacyComponentSerializer;
import sakuyaayane.nekoantixray.lib.folialib.FoliaLib;
import sakuyaayane.nekoantixray.NekoAntiXray;
import sakuyaayane.nekoantixray.stats.BanStatisticsManager;

public class PeriodicAnnouncementManager {
   private final NekoAntiXray plugin;
   private final FoliaLib foliaLib;
   private final BanStatisticsManager banStatisticsManager;
   private final MiniMessage miniMessage;
   private ScheduledFuture<?> scheduledTask;

   public PeriodicAnnouncementManager(NekoAntiXray plugin, BanStatisticsManager banStatisticsManager) {
      this.plugin = plugin;
      this.foliaLib = plugin.getFoliaScheduler();
      this.banStatisticsManager = banStatisticsManager;
      this.miniMessage = MiniMessage.miniMessage();
   }

   private String serializeToLegacy(Component component) {
      return LegacyComponentSerializer.legacySection().serialize(component);
   }

   public void start() {
      if (this.plugin.getConfig().getBoolean("announcement-system.enabled", true)) {
         int intervalSeconds = this.plugin.getConfig().getInt("announcement-system.interval", 300);
         this.foliaLib.getImpl().runTimer(wrappedTask -> this.broadcastAnnouncement(), intervalSeconds * 20L, intervalSeconds * 20L);
         this.plugin.getLogger().info("公告系统已启动，间隔: " + intervalSeconds + " 秒");
      }
   }

   public void stop() {
      if (this.scheduledTask != null && !this.scheduledTask.isCancelled()) {
         this.scheduledTask.cancel(false);
         this.plugin.getLogger().info("公告系统已停止");
      }
   }

   private void broadcastAnnouncement() {
      if (!Bukkit.getOnlinePlayers().isEmpty()) {
         String template = this.plugin
            .getConfig()
            .getString(
               "announcement-system.message",
               "<red>⚠</red> <yellow>服务器反矿透系统</yellow> <gray>|</gray> <white>本服务器已封禁</white> <gold>%total_bans%</gold> <white>名作弊玩家！</white> <red>请勿使用任何作弊软件！</red>"
            );
         String rawMessage = template.replace("%total_bans%", String.valueOf(this.banStatisticsManager.getTotalBans()));
         if (this.plugin.getConfig().getBoolean("announcement-system.gradient-enabled", true)) {
            rawMessage = this.applyGradientTags(rawMessage);
         }

         Component component = this.miniMessage.deserialize(rawMessage);
         String legacyMessage = this.serializeToLegacy(component);
         boolean showInChat = this.plugin.getConfig().getBoolean("announcement-system.show-in-chat", true);
         boolean showAsActionBar = this.plugin.getConfig().getBoolean("announcement-system.show-as-actionbar", false);
         boolean showAsTitle = this.plugin.getConfig().getBoolean("announcement-system.show-as-title", false);

         for (Player player : Bukkit.getOnlinePlayers()) {
            if (showInChat) {
               player.sendMessage(legacyMessage);
            }

            if (showAsActionBar) {
               player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(legacyMessage));
            }

            if (showAsTitle) {
               this.sendTitleAnnouncement(player, component);
            }
         }

         this.plugin.logDebug("已广播公告消息给 " + Bukkit.getOnlinePlayers().size() + " 名玩家");
      }
   }

   private String applyGradientTags(String message) {
      String gradientStart = this.plugin.getConfig().getString("announcement-system.gradient-start", "#ff6b6b");
      String gradientEnd = this.plugin.getConfig().getString("announcement-system.gradient-end", "#feca57");
      if (!message.contains("<gradient:")) {
         return "<gradient:" + gradientStart + ":" + gradientEnd + ">" + message + "</gradient>";
      } else {
         return message;
      }
   }

   private void sendTitleAnnouncement(Player player, Component component) {
      int fadeInTicks = this.plugin.getConfig().getInt("announcement-system.title.fade-in", 10);
      int stayTicks = this.plugin.getConfig().getInt("announcement-system.title.stay", 40);
      int fadeOutTicks = this.plugin.getConfig().getInt("announcement-system.title.fade-out", 10);
      player.sendTitle(this.serializeToLegacy(component), "", fadeInTicks, stayTicks, fadeOutTicks);
   }

   public void broadcastNow() {
      this.broadcastAnnouncement();
   }

   public void broadcastCustomMessage(String message) {
      if (!Bukkit.getOnlinePlayers().isEmpty()) {
         String formattedRaw = message.replace("%total_bans%", String.valueOf(this.banStatisticsManager.getTotalBans()));
         Component component = this.miniMessage.deserialize(formattedRaw);
         String legacyMessage = this.serializeToLegacy(component);

         for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(legacyMessage);
         }

         this.plugin.logDebug("已发送自定义公告消息");
      }
   }

   public String formatMiniMessage(String message) {
      try {
         return this.serializeToLegacy(this.miniMessage.deserialize(message));
      } catch (Exception ex) {
         this.plugin.getLogger().warning("MiniMessage格式错误: " + ex.getMessage());
         return message;
      }
   }

   public void reload() {
      this.stop();
      this.start();
      this.plugin.getLogger().info("公告系统配置已重新加载");
   }
}
