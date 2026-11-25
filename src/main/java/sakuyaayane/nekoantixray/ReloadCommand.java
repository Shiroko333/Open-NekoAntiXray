package sakuyaayane.nekoantixray;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReloadCommand implements CommandExecutor {
   private static final String RELOAD_PERMISSION = "nekoantixray.reload";
   private static final String NO_PERMISSION_MESSAGE = "§c你没有权限执行此命令。";
   private static final String RELOAD_SUCCESS_MESSAGE = "§a配置文件已成功重新加载。";
   private final NekoAntiXray plugin;

   public ReloadCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("nekoantixray.reload")) {
         sender.sendMessage("§c你没有权限执行此命令。");
         return true;
      }

      this.plugin.reloadPluginConfig();
      sender.sendMessage("§a配置文件已成功重新加载。");
      if (sender instanceof Player) {
         this.plugin.getLogger().info("玩家 " + sender.getName() + " 重新加载了配置文件。");
      } else {
         this.plugin.getLogger().info("控制台重新加载了配置文件。");
      }

      return true;
   }
}
