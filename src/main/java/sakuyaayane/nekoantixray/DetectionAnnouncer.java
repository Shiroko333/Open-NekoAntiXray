package sakuyaayane.nekoantixray;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class DetectionAnnouncer {
   public static void AnnounceDetection(Player player, String reason, double violationLevel) {
      NekoAntiXray plugin = NekoAntiXray.getInstance();
      ConfigManager configManager = plugin.getConfigManager();
      if (plugin.getDetectionNotificationManager() != null) {
         double effectiveViolationLevel = configManager.getCheckXrayVlAddBreakSelf() >= 0.0 ? violationLevel : violationLevel;
         String locationText = formatPlayerLocation(player);
         DetectionNotificationManager detectionNotificationManager = plugin.getDetectionNotificationManager();
         detectionNotificationManager.notifyDetection(player, "xray", reason, effectiveViolationLevel, locationText);
      } else {
         String alertMessage = String.format("§c%s §7被检测到可能使用 §f%s §7(VL +%.1f)", player.getName(), reason, violationLevel);
         Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> onlinePlayer.hasPermission("nekoantixray.announce")).forEach(onlinePlayer -> onlinePlayer.sendMessage(alertMessage));
         Bukkit.getConsoleSender().sendMessage(alertMessage);
      }

      if (configManager.isNotificationSystemBanMessagesShowLocationInConsole()) {
         Bukkit.getConsoleSender().sendMessage(formatPlayerLocation(player));
      }
   }

   private static String formatPlayerLocation(Player player) {
      return player != null && player.getWorld() != null
         ? String.format(
            "世界=%s, X=%.1f, Y=%.1f, Z=%.1f", player.getWorld().getName(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ()
         )
         : "";
   }

   public static void announceBan(Player player, String reason) {
      NekoAntiXray plugin = NekoAntiXray.getInstance();
      if (plugin.getDetectionNotificationManager() != null) {
         plugin.getDetectionNotificationManager().notifyBan(player, "xray", reason);
      } else {
         String banMessage = plugin.getConfigManager().getNotificationSystemXrayBanMessage().replace("%player%", player.getName());
         String banDetail = plugin.getConfigManager().getNotificationSystemXrayBanDetail().replace("%reason%", reason);
         banMessage = ChatColor.translateAlternateColorCodes('&', banMessage);
         banDetail = ChatColor.translateAlternateColorCodes('&', banDetail);
         Bukkit.broadcastMessage(banMessage);
         Bukkit.broadcastMessage(banDetail);
         Bukkit.getConsoleSender().sendMessage(banMessage);
         Bukkit.getConsoleSender().sendMessage(banDetail);
      }
   }
}
