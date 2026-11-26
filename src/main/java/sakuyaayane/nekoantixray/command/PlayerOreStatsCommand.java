package sakuyaayane.nekoantixray.command;

import java.util.Map.Entry;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import sakuyaayane.nekoantixray.config.ConfigManager;
import sakuyaayane.nekoantixray.NekoAntiXray;

public class PlayerOreStatsCommand implements CommandExecutor {
   private final NekoAntiXray plugin;

   public PlayerOreStatsCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("nekoantixray.orestats") && !sender.hasPermission("nekoantixray.admin")) {
         sender.sendMessage("§c你没有权限使用此命令。");
         return true;
      } else {
         ConfigManager configManager = this.plugin.getConfigManager();
         if (!configManager.isOreStatsEnabled()) {
            sender.sendMessage("§c矿石统计功能未启用。");
            return true;
         } else if (args.length < 1) {
            sender.sendMessage("§c用法: /nax orestats <玩家名>");
            return true;
         } else {
            String targetPlayerName = args[0];
            Map<String, Integer> playerOreCounts = this.plugin.getOreStatsStorage().getPlayerOreCounts(targetPlayerName);
            if (playerOreCounts.isEmpty()) {
               sender.sendMessage("§c未找到玩家 " + targetPlayerName + " 的挖矿统计数据。");
               return true;
            } else {
               sender.sendMessage("§6=== §e" + targetPlayerName + " §6的挖矿统计 ===");
               playerOreCounts.entrySet().stream().sorted(Entry.comparingByKey()).forEach(entry -> this.sendOreCountEntry(sender, entry));
               int totalOreCount = playerOreCounts.values().stream().mapToInt(Integer::intValue).sum();
               sender.sendMessage("§6总计: §e" + totalOreCount + " §6个矿石");
               return true;
            }
         }
      }
   }

   private void sendOreCountEntry(CommandSender sender, Entry entry) {
      String localizedOreName = this.getLocalizedOreName((String)entry.getKey());
      sender.sendMessage("§7- §b" + localizedOreName + "§7: §a" + entry.getValue());
   }

   private String getLocalizedOreName(String materialName) {
      String baseOreName = materialName.replace("_ORE", "");
      switch (baseOreName) {
         case "DIAMOND":
            return "钻石矿";
         case "DEEPSLATE_DIAMOND":
            return "深层钻石矿";
         case "EMERALD":
            return "绿宝石矿";
         case "DEEPSLATE_EMERALD":
            return "深层绿宝石矿";
         case "GOLD":
            return "金矿";
         case "DEEPSLATE_GOLD":
            return "深层金矿";
         case "NETHER_GOLD":
            return "下界金矿";
         case "IRON":
            return "铁矿";
         case "DEEPSLATE_IRON":
            return "深层铁矿";
         case "COAL":
            return "煤矿";
         case "DEEPSLATE_COAL":
            return "深层煤矿";
         case "COPPER":
            return "铜矿";
         case "DEEPSLATE_COPPER":
            return "深层铜矿";
         case "LAPIS":
            return "青金石矿";
         case "DEEPSLATE_LAPIS":
            return "深层青金石矿";
         case "REDSTONE":
            return "红石矿";
         case "DEEPSLATE_REDSTONE":
            return "深层红石矿";
         case "NETHER_QUARTZ":
            return "下界石英矿";
         case "ANCIENT_DEBRIS":
            return "远古残骸";
         default:
            return baseOreName.replace("_", " ").toLowerCase();
      }
   }
}