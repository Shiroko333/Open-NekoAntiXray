package sakuyaayane.nekoantixray;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ObfuscationStatsCommand implements CommandExecutor {
   private final NekoAntiXray plugin;

   public ObfuscationStatsCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("nekoantixray.admin")) {
         sender.sendMessage("§c你没有权限执行此命令。");
         return true;
      } else if (args.length >= 1 && args[0].equalsIgnoreCase("reset")) {
         this.plugin.getObfuscationStatistics().reset();
         ModuleTimingStatistics.reset();
         sender.sendMessage("§a混淆统计与模块耗时采样已重置。");
         return true;
      } else {
         sender.sendMessage("§6===== NekoAntiXray 混淆统计 =====");
         sender.sendMessage("§e" + this.plugin.getObfuscationStatistics().toString());
         sender.sendMessage("§6==================================");
         sender.sendMessage("§7使用 /nax stats reset 重置计数。");
         return true;
      }
   }
}
