package sakuyaayane.nekoantixray.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sakuyaayane.nekoantixray.NekoAntiXray;
import sakuyaayane.nekoantixray.notification.OfflineDetectionReporter;

public class DetectionHistoryCommand implements CommandExecutor {
   private final NekoAntiXray plugin;

   public DetectionHistoryCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender instanceof Player player) {
         if (args.length == 0) {
            if (!this.hasHistoryPermission(player)) {
               player.sendMessage("§c你没有权限查看检测记录。");
               return true;
            } else {
               this.plugin.getOfflineDetectionReporter().showPlayerHistory(player, player.getName());
               return true;
            }
         } else {
            long durationMillis = OfflineDetectionReporter.parseDurationMillis(args[0]);
            if (durationMillis == -1L) {
               if (!player.hasPermission("nekoantixray.admin")) {
                  player.sendMessage("§c你没有权限查看其他玩家的检测记录。");
                  return true;
               }

               if (args.length >= 2) {
                  long targetDurationMillis = OfflineDetectionReporter.parseDurationMillis(args[1]);
                  if (targetDurationMillis == -1L) {
                     player.sendMessage("§c无效的时间参数: " + args[1] + " (用法: 30m / 6h / 3d / all)");
                     return true;
                  }
               }

               this.plugin.getOfflineDetectionReporter().showPlayerHistory(player, args[0]);
               return true;
            } else if (!this.hasHistoryPermission(player)) {
               player.sendMessage("§c你没有权限查看检测记录。");
               return true;
            } else {
               this.plugin.getOfflineDetectionReporter().showPlayerHistory(player, player.getName());
               return true;
            }
         }
      } else {
         sender.sendMessage("§c只有玩家可以使用此命令(控制台可直接查看 logs/offline-report-events.log)。");
         return true;
      }
   }

   private boolean hasHistoryPermission(Player player) {
      return player.hasPermission("nekoantixray.notify") || player.hasPermission("nekoantixray.alerts") || player.hasPermission("nekoantixray.admin");
   }
}
