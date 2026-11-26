package sakuyaayane.nekoantixray.command;

import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import sakuyaayane.nekoantixray.NekoAntiXray;

public class CheckStatusCommand implements CommandExecutor {
   private final NekoAntiXray plugin;

   public CheckStatusCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("nekoantixray.admin")) {
         sender.sendMessage(ChatColor.RED + "你没有权限执行此命令!");
         return true;
      }

      if (args.length > 0 && args[0].equalsIgnoreCase("status")) {
         this.sendStatusMessage(sender);
         return true;
      }

      FileConfiguration config = this.plugin.getConfig();
      boolean massExcavationEnabled = config.getBoolean("check.mass-excavation.enabled", true);
      boolean xrayCheckEnabled = config.getBoolean("check.xray.enable", true);
      if (!massExcavationEnabled && !xrayCheckEnabled) {
         sender.sendMessage(ChatColor.YELLOW + "[NekoAntiXray] " + ChatColor.WHITE + "XRay检测当前已禁用，大规模挖掘检测功能无法单独启用。");
         sender.sendMessage(ChatColor.YELLOW + "[NekoAntiXray] " + ChatColor.WHITE + "请先启用XRay检测功能。");
         return true;
      }

      config.set("check.mass-excavation.enabled", !massExcavationEnabled);

      try {
         this.plugin.saveConfig();
         this.plugin.reloadPluginConfig();
         String statusText = !massExcavationEnabled ? ChatColor.GREEN + "已启用" : ChatColor.RED + "已禁用";
         sender.sendMessage(ChatColor.GOLD + "[NekoAntiXray] " + ChatColor.WHITE + "大规模挖掘检测功能 " + statusText);
         if (!xrayCheckEnabled && !massExcavationEnabled) {
            sender.sendMessage(ChatColor.YELLOW + "[NekoAntiXray] " + ChatColor.WHITE + "注意：由于XRay检测当前已禁用，大规模挖掘检测功能也将不会生效。");
         }

         return true;
      } catch (Exception ex) {
         sender.sendMessage(ChatColor.RED + "保存配置时发生错误: " + ex.getMessage());
         this.plugin.getLogger().log(Level.SEVERE, "切换大规模挖掘检测状态时发生错误: " + ex.getMessage(), ex);
         return false;
      }
   }

   private void sendStatusMessage(CommandSender sender) {
      FileConfiguration config = this.plugin.getConfig();
      boolean massExcavationEnabled = config.getBoolean("check.mass-excavation.enabled", true);
      boolean xrayCheckEnabled = config.getBoolean("check.xray.enable", true);
      sender.sendMessage(ChatColor.GOLD + "===== 大规模挖掘检测状态 =====");
      sender.sendMessage(ChatColor.WHITE + "功能状态: " + (massExcavationEnabled ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
      sender.sendMessage(ChatColor.WHITE + "XRay检测状态: " + (xrayCheckEnabled ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
      sender.sendMessage(
         ChatColor.WHITE + "实际运行状态: " + (massExcavationEnabled && xrayCheckEnabled ? ChatColor.GREEN + "正常工作中" : ChatColor.RED + "未工作")
      );
      int detectionTimeWindow = config.getInt("check.mass-excavation.detection-time-window", 60);
      int blockCountThreshold = config.getInt("check.mass-excavation.block-count-threshold", 40);
      int diversityThreshold = config.getInt("check.mass-excavation.diversity-threshold", 5);
      int bypassDuration = config.getInt("check.mass-excavation.bypass-duration", 180);
      sender.sendMessage(ChatColor.WHITE + "检测时间窗口: " + ChatColor.YELLOW + detectionTimeWindow + "秒");
      sender.sendMessage(ChatColor.WHITE + "方块数量阈值: " + ChatColor.YELLOW + blockCountThreshold + "个");
      sender.sendMessage(ChatColor.WHITE + "多样性阈值: " + ChatColor.YELLOW + diversityThreshold + "种");
      sender.sendMessage(ChatColor.WHITE + "权限持续时间: " + ChatColor.YELLOW + bypassDuration + "秒");
      sender.sendMessage(ChatColor.GRAY + "提示: 启用调试模式可查看更多详细信息");
      sender.sendMessage(ChatColor.GRAY + "在config.yml中设置 debug: true 并重载配置");
      sender.sendMessage(ChatColor.GOLD + "=============================");
   }
}
