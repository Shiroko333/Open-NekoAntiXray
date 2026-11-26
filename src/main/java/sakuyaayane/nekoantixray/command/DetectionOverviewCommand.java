package sakuyaayane.nekoantixray.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sakuyaayane.nekoantixray.NekoAntiXray;
import sakuyaayane.nekoantixray.notification.OfflineDetectionReporter;

public class DetectionOverviewCommand implements CommandExecutor {
   private final NekoAntiXray plugin;
   private static final long INVALID_DURATION = -1L;

   public DetectionOverviewCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage("§c只有玩家可以使用此命令(控制台可直接查看 logs/offline-report-events.log)。");
         return true;
      } else {
         boolean hasPermission = player.hasPermission("nekoantixray.notify")
            || player.hasPermission("nekoantixray.alerts")
            || player.hasPermission("nekoantixray.admin");
         if (!hasPermission) {
            player.sendMessage("§c你没有权限查看检测概况。");
            return true;
         }

         if (args.length >= 1) {
            long durationMillis = OfflineDetectionReporter.parseDurationMillis(args[0]);
            if (durationMillis == -1L) {
               player.sendMessage("§c无效的时间参数: " + args[0] + " (用法: 30m / 6h / 3d / all)");
               return true;
            } else {
               OfflineDetectionReporter offlineDetectionReporter = this.plugin.getOfflineDetectionReporter();
               offlineDetectionReporter.showRecentReport(player, durationMillis);
               return true;
            }
         } else {
            this.plugin.getOfflineDetectionReporter().sendOfflineReportOnJoin(player);
            return true;
         }
      }
   }
}
