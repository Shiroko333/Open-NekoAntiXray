package sakuyaayane.nekoantixray;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OreTrackCommand implements CommandExecutor {
   private final NekoAntiXray plugin;

   public OreTrackCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender instanceof Player player) {
         if (!player.hasPermission("nekoantixray.oretrack") && !player.hasPermission("nekoantixray.admin")) {
            player.sendMessage("§c你没有权限使用此命令。");
            return true;
         }

         ConfigManager configManager = this.plugin.getConfigManager();
         if (!configManager.isOreStatsEnabled()) {
            player.sendMessage("§c矿石统计功能未启用。");
            return true;
         }

         if (args.length < 1) {
            player.sendMessage("§c用法: /nax oretrack <玩家名>");
            return true;
         }

         String targetPlayerName = args[0];
         if (targetPlayerName.equalsIgnoreCase(player.getName())) {
            player.sendMessage("§c你不能追踪自己。");
            return true;
         }

         OreTrackingManager oreTrackingManager = this.plugin.getOreTrackingManager();
         boolean isTracking = oreTrackingManager.toggleTracking(player, targetPlayerName);
         if (isTracking) {
            player.sendMessage("§a你现在正在追踪 §e" + targetPlayerName + " §a的挖矿行为。");
            player.sendMessage("§7当该玩家挖掘矿石时，你会收到通知。");
         } else {
            player.sendMessage("§c你已停止追踪 §e" + targetPlayerName + "§c。");
         }

         return true;
      } else {
         sender.sendMessage("§c此命令只能由玩家使用。");
         return true;
      }
   }
}
