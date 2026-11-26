package sakuyaayane.nekoantixray.command;

import java.util.Arrays;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sakuyaayane.nekoantixray.NekoAntiXray;
import sakuyaayane.nekoantixray.notification.PeriodicAnnouncementManager;
import sakuyaayane.nekoantixray.stats.BanStatisticsManager;

public class AnnouncementCommand implements CommandExecutor {
   private final NekoAntiXray plugin;

   public AnnouncementCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("nekoantixray.admin")) {
         sender.sendMessage("§c你没有权限使用此命令！");
         return true;
      }

      if (args.length < 1) {
         this.sendHelpMessage(sender);
         return true;
      }

      BanStatisticsManager banStatisticsManager = this.plugin.getBanStatisticsManager();
      PeriodicAnnouncementManager periodicAnnouncementManager = this.plugin.getPeriodicAnnouncementManager();
      switch (args[0].toLowerCase()) {
         case "stats":
            this.handleStatsCommand(sender, args, banStatisticsManager);
            break;
         case "announce":
            this.handleAnnounceCommand(sender, args, periodicAnnouncementManager);
            break;
         case "test":
            this.handleTestCommand(sender, args, periodicAnnouncementManager);
            break;
         case "reload":
            this.handleReloadCommand(sender, periodicAnnouncementManager);
            break;
         default:
            this.sendHelpMessage(sender);
      }

      return true;
   }

   private static void sendHistoryLine(CommandSender sender, String historyEntry) {
      sender.sendMessage("§f" + historyEntry);
   }

   private void handleStatsCommand(CommandSender sender, String[] args, BanStatisticsManager banStatisticsManager) {
      if (args.length < 2) {
         sender.sendMessage("§6=== 封禁统计信息 ===");
         sender.sendMessage("§e总封禁次数: §f" + banStatisticsManager.getTotalBans());
         sender.sendMessage("§7使用 /nax announcement stats history [数量] 查看历史记录");
         sender.sendMessage("§7使用 /nax announcement stats reset 重置统计");
      } else {
         switch (args[1].toLowerCase()) {
            case "history":
               int limit = args.length > 2 ? this.parseIntOrDefault(args[2], 10) : 10;
               List<String> banHistory = banStatisticsManager.getBanHistory(limit);
               sender.sendMessage("§6=== 最近 " + limit + " 次封禁记录 ===");
               if (banHistory.isEmpty()) {
                  sender.sendMessage("§7暂无封禁记录");
               } else {
                  banHistory.forEach(historyEntry -> sendHistoryLine(sender, historyEntry));
               }
               break;
            case "reset":
               banStatisticsManager.resetStatistics();
               sender.sendMessage("§a封禁统计已重置！");
               break;
            default:
               sender.sendMessage("§c未知的统计子命令: " + args[1]);
         }
      }
   }

   private void handleAnnounceCommand(CommandSender sender, String[] args, PeriodicAnnouncementManager periodicAnnouncementManager) {
      if (args.length < 2) {
         periodicAnnouncementManager.broadcastNow();
         sender.sendMessage("§a已手动发送公告！");
      } else if (args[1].equalsIgnoreCase("custom")) {
         if (args.length < 3) {
            sender.sendMessage("§c请提供自定义消息内容！");
            return;
         }

         String customMessage = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
         periodicAnnouncementManager.broadcastCustomMessage(customMessage);
         sender.sendMessage("§a已发送自定义公告: §f" + customMessage);
      } else {
         sender.sendMessage("§c未知的公告子命令: " + args[1]);
      }
   }

   private void handleTestCommand(CommandSender sender, String[] args, PeriodicAnnouncementManager periodicAnnouncementManager) {
      if (args.length < 2) {
         sender.sendMessage("§c请提供要测试的MiniMessage格式！");
         sender.sendMessage("§7示例: /nax announcement test <red>这是红色文本</red>");
      } else {
         String miniMessageFormat = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
         if (sender instanceof Player) {
            try {
               sender.sendMessage(periodicAnnouncementManager.formatMiniMessage(miniMessageFormat));
               sender.sendMessage("§a格式测试成功！消息已发送给你。");
            } catch (Exception ex) {
               sender.sendMessage("§c格式错误: " + ex.getMessage());
            }
         } else {
            sender.sendMessage("§c此命令只能由玩家执行！");
         }
      }
   }

   private void handleReloadCommand(CommandSender sender, PeriodicAnnouncementManager periodicAnnouncementManager) {
      periodicAnnouncementManager.reload();
      sender.sendMessage("§a公告系统配置已重新加载！");
   }

   private void sendHelpMessage(CommandSender sender) {
      sender.sendMessage("§6=== NekoAntiXray 公告与统计系统 ===");
      sender.sendMessage("§e/nax announcement stats §7- 查看封禁统计");
      sender.sendMessage("§e/nax announcement stats history [数量] §7- 查看封禁历史");
      sender.sendMessage("§e/nax announcement stats reset §7- 重置封禁统计");
      sender.sendMessage("§e/nax announcement announce §7- 手动发送公告");
      sender.sendMessage("§e/nax announcement announce custom <消息> §7- 发送自定义公告");
      sender.sendMessage("§e/nax announcement test <MiniMessage格式> §7- 测试消息格式");
      sender.sendMessage("§e/nax announcement reload §7- 重新加载公告系统");
   }

   private int parseIntOrDefault(String rawValue, int defaultValue) {
      try {
         return Integer.parseInt(rawValue);
      } catch (NumberFormatException ex) {
         return defaultValue;
      }
   }
}