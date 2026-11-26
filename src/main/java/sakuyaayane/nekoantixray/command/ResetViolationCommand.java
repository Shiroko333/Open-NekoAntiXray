package sakuyaayane.nekoantixray.command;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sakuyaayane.nekoantixray.NekoAntiXray;

public class ResetViolationCommand implements CommandExecutor {
   private final NekoAntiXray plugin;

   public ResetViolationCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length < 1) {
         sender.sendMessage("§c用法: /nax resetviolation <玩家名>");
         return true;
      }

      String targetPlayerName = args[0];
      Player onlinePlayer = Bukkit.getPlayer(targetPlayerName);
      UUID playerUuid;
      if (onlinePlayer != null) {
         playerUuid = onlinePlayer.getUniqueId();
      } else {
         playerUuid = Bukkit.getOfflinePlayer(targetPlayerName).getUniqueId();
      }

      if (playerUuid == null) {
         sender.sendMessage("§c找不到玩家: " + targetPlayerName);
         return true;
      } else {
         this.plugin.getXrayDetector().clearPlayerData(playerUuid);
         sender.sendMessage("§a已重置玩家 " + targetPlayerName + " 的违规记录");
         return true;
      }
   }
}
