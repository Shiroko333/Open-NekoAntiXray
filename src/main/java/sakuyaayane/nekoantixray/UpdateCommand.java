package sakuyaayane.nekoantixray;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class UpdateCommand implements CommandExecutor {
   private final NekoAntiXray plugin;

   public UpdateCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("nekoantixray.update")) {
         sender.sendMessage("§c你没有权限使用此命令。");
         return true;
      } else {
         sender.sendMessage("§a正在检查更新...");
         sender.sendMessage("§e自动更新维护中 请在群文件下载。");
         return true;
      }
   }

   private String buildProgressBar(int percentage) {
      byte barLength = 20;
      int filledLength = (int)(percentage / 100.0 * barLength);
      StringBuilder progressBar = new StringBuilder("[");

      for (int i = 0; i < barLength; i++) {
         if (i < filledLength) {
            progressBar.append("=");
         } else {
            progressBar.append(" ");
         }
      }

      progressBar.append("]");
      return progressBar.toString();
   }
}
