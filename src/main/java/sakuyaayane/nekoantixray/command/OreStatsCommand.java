package sakuyaayane.nekoantixray.command;

import java.util.List;
import java.util.Map.Entry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import sakuyaayane.nekoantixray.config.ConfigManager;
import sakuyaayane.nekoantixray.NekoAntiXray;
import sakuyaayane.nekoantixray.stats.OreStatsStorage;

public class OreStatsCommand implements CommandExecutor {
   private final NekoAntiXray plugin;

   public OreStatsCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("nekoantixray.orestats") && !sender.hasPermission("nekoantixray.admin")) {
         sender.sendMessage("§c你没有权限使用此命令。");
         return true;
      }

      ConfigManager configManager = this.plugin.getConfigManager();
      if (!configManager.isOreStatsEnabled()) {
         sender.sendMessage("§c矿石统计功能未启用。");
         return true;
      }

      OreStatsStorage oreStatsStorage = this.plugin.getOreStatsStorage();
      List<Entry<String, Integer>> topPlayers = oreStatsStorage.getTopPlayers(10);
      if (topPlayers.isEmpty()) {
         sender.sendMessage("§c暂无挖矿统计数据。");
         return true;
      }

      sender.sendMessage("§6=== §e挖矿排行榜 (按总矿石数) §6===");
      int rank = 1;

      for (Entry<String, Integer> entry : topPlayers) {
         String playerName = entry.getKey();
         int oreCount = entry.getValue();
         String rankColor;
         if (rank == 1) {
            rankColor = "§6";
         } else if (rank == 2) {
            rankColor = "§7";
         } else if (rank == 3) {
            rankColor = "§c";
         } else {
            rankColor = "§e";
         }

         sender.sendMessage(rankColor + rank + ". §a" + playerName + " §7- §b" + oreCount + " §7个矿石");
         rank++;
      }

      return true;
   }
}
