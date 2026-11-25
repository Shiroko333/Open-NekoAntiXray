package sakuyaayane.nekoantixray;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class DetectionNotificationManager {
   private final NekoAntiXray plugin;
   private final Map<String, Long> lastNotificationTimestamps = new ConcurrentHashMap<>();
   private static final long DEFAULT_THROTTLE_MS = 5000L;

   public DetectionNotificationManager(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   public boolean isNotificationSystemEnabled() {
      return this.plugin.getConfig().getBoolean("notification-system.enabled", true);
   }

   public long getNotificationSystemThrottleMs() {
      return Math.max(0L, this.plugin.getConfig().getLong("notification-system.throttle-ms", 5000L));
   }

   public void notifyDetection(Player player, String category, String checkName, double violationLevel, String details) {
      if (player != null && this.isNotificationSystemEnabled()) {
         DetectionNotificationManager.NotificationLevel notificationLevel = this.resolveNotificationLevel(category, violationLevel);
         if (notificationLevel != null) {
            String throttleKey = player.getUniqueId() + ":" + category + ":" + notificationLevel.name();
            long nowMs = System.currentTimeMillis();
            Long lastNotificationTime = this.lastNotificationTimestamps.get(throttleKey);
            if (lastNotificationTime == null || nowMs - lastNotificationTime >= this.getNotificationSystemThrottleMs()) {
               this.lastNotificationTimestamps.put(throttleKey, nowMs);
               if (this.lastNotificationTimestamps.size() > 10000) {
                  this.lastNotificationTimestamps.clear();
               }

               this.dispatchNotification(player, category, checkName, violationLevel, details, notificationLevel);
               OfflineDetectionReporter offlineDetectionReporter = this.plugin.getOfflineDetectionReporter();
               offlineDetectionReporter.recordDetection(player, checkName, violationLevel);
            }
         }
      }
   }

   public void notifyBan(Player player, String category, String reason) {
      if (player != null && this.isNotificationSystemEnabled()) {
         if (category == null || category.isEmpty()) {
            category = "xray";
         }

         this.plugin.getOfflineDetectionReporter().recordBan(player, category, reason);
         String configBasePath = "notification-system." + category + ".ban";
         boolean banNotificationEnabled = this.plugin.getConfig().getBoolean(configBasePath + ".enabled", true);
         boolean broadcastEnabled = this.plugin.getConfig().getBoolean(configBasePath + ".broadcast", true);
         boolean notifyAdminsEnabled = this.plugin.getConfig().getBoolean("notification-system." + category + ".ban.notify-admins", false);
         boolean consoleEnabled = this.plugin.getConfig().getBoolean(configBasePath + ".console", true);
         if (!banNotificationEnabled) {
            if (consoleEnabled) {
               Bukkit.getConsoleSender().sendMessage("玩家 " + player.getName() + " 已被封禁。原因: " + reason);
            }
         } else {
            String rawBanMessage = this.plugin
               .getConfig()
               .getString(configBasePath + ".message", "§c玩家 %player% 因使用作弊被封禁！")
               .replace("%player%", player.getName());
            String rawDetailMessage = this.plugin.getConfig().getString(configBasePath + ".detail", "§7原因: §f%reason%").replace("%reason%", reason);
            String formattedBanMessage = ChatColor.translateAlternateColorCodes('&', rawBanMessage);
            String formattedDetailMessage = ChatColor.translateAlternateColorCodes('&', rawDetailMessage);
            if (broadcastEnabled) {
               Bukkit.broadcastMessage(formattedBanMessage);
               Bukkit.broadcastMessage(formattedDetailMessage);
            }

            if (notifyAdminsEnabled) {
               for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                  if (onlinePlayer.hasPermission("nekoantixray.notify") || onlinePlayer.hasPermission("nekoantixray.alerts")) {
                     onlinePlayer.sendMessage(formattedBanMessage);
                     onlinePlayer.sendMessage(formattedDetailMessage);
                  }
               }
            }

            if (consoleEnabled) {
               Bukkit.getConsoleSender().sendMessage(formattedBanMessage);
               Bukkit.getConsoleSender().sendMessage(formattedDetailMessage);
            }
         }
      }
   }

   private boolean isLevelEnabled(String category, String levelKey) {
      return this.plugin.getConfig().getBoolean("notification-system." + category + "." + levelKey + ".enabled", true);
   }

   private double getLevelThreshold(String category, String levelKey) {
      return this.plugin
         .getConfig()
         .getDouble("notification-system." + category + "." + levelKey + ".threshold", levelKey.equals("notify") ? 1.0 : (levelKey.equals("warn") ? 5.0 : 10.0));
   }

   private String formatNotificationMessage(String template, Player player, String checkName, double violationLevel, String details) {
      String formattedMessage = template.replace("%player%", player.getName()).replace("%check%", checkName).replace("%vl%", String.format("%.1f", violationLevel));
      if (details != null) {
         formattedMessage = formattedMessage.replace("%details%", details);
      } else {
         formattedMessage = formattedMessage.replace("%details%", "");
      }

      return ChatColor.translateAlternateColorCodes('&', formattedMessage);
   }

   private String getDefaultMessageTemplate(DetectionNotificationManager.NotificationLevel notificationLevel) {
      switch (notificationLevel) {
         case WARN:
            return "&e[NAX] &f%player% &7触发 &e%check% &7(VL: &e%vl%&7) &8%details%";
         case ALERT:
            return "&c[NAX] &f%player% &7疑似使用 &c%check% &7(VL: &e%vl%&7) &8%details%";
         default:
            return "&7[NAX] &f%player% &7检测到 &f%check% &7(VL: &e%vl%&7) &8%details%";
      }
   }

   private DetectionNotificationManager.NotificationLevel resolveNotificationLevel(String category, double violationLevel) {
      double alertThreshold = this.getLevelThreshold(category, "alert");
      double warnThreshold = this.getLevelThreshold(category, "warn");
      double notifyThreshold = this.getLevelThreshold(category, "notify");
      boolean alertEnabled = this.isLevelEnabled(category, "alert");
      boolean warnEnabled = this.isLevelEnabled(category, "warn");
      boolean notifyEnabled = this.isLevelEnabled(category, "notify");
      if (alertEnabled && violationLevel >= alertThreshold) {
         return DetectionNotificationManager.NotificationLevel.ALERT;
      } else if (warnEnabled && violationLevel >= warnThreshold) {
         return DetectionNotificationManager.NotificationLevel.WARN;
      } else {
         return notifyEnabled && violationLevel >= notifyThreshold ? DetectionNotificationManager.NotificationLevel.NOTIFY : null;
      }
   }

   private void dispatchNotification(
      Player player,
      String category,
      String checkName,
      double violationLevel,
      String details,
      DetectionNotificationManager.NotificationLevel notificationLevel
   ) {
      String configBasePath = "notification-system." + category + "." + notificationLevel.name().toLowerCase();
      boolean notifyAdmins = this.plugin.getConfig().getBoolean(configBasePath + ".notify-admins", true);
      boolean notifyPlayer = this.plugin.getConfig().getBoolean(configBasePath + ".notify-player", false);
      boolean broadcast = this.plugin.getConfig().getBoolean(configBasePath + ".broadcast", false);
      boolean logToConsole = this.plugin.getConfig().getBoolean(configBasePath + ".console", true);
      String messageTemplate = this.plugin.getConfig().getString(configBasePath + ".message", this.getDefaultMessageTemplate(notificationLevel));
      String formattedMessage = this.formatNotificationMessage(messageTemplate, player, checkName, violationLevel, details);
      if (notifyAdmins) {
         for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("nekoantixray.notify") || onlinePlayer.hasPermission("nekoantixray.alerts")) {
               onlinePlayer.sendMessage(formattedMessage);
            }
         }
      }

      if (notifyPlayer) {
         player.sendMessage(formattedMessage);
      }

      if (broadcast) {
         Bukkit.broadcastMessage(formattedMessage);
      }

      if (logToConsole) {
         Bukkit.getConsoleSender().sendMessage(formattedMessage);
      }
   }

   public enum NotificationLevel {
      NOTIFY,
      WARN,
      ALERT;

      private static DetectionNotificationManager.NotificationLevel[] createNotificationLevels() {
         return new DetectionNotificationManager.NotificationLevel[]{NOTIFY, WARN, ALERT};
      }
   }
}
