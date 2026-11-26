package sakuyaayane.nekoantixray.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import sakuyaayane.nekoantixray.NekoAntiXray;

public class DecoyOreCommand implements CommandExecutor {
   private final NekoAntiXray plugin;

   public DecoyOreCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage("§c只有玩家可以使用诱饵投放。");
         return true;
      }

      if (!sender.hasPermission("nekoantixray.admin")) {
         sender.sendMessage("§c你没有权限执行此命令。");
         return true;
      }

      Player targetPlayer = (Player)sender;
      Material decoyMaterial = Material.DIAMOND_ORE;
      int throwDistance = -1;
      int argOffset = 0;
      if (args.length >= 1 && !args[0].isEmpty()) {
         Player specifiedPlayer = Bukkit.getPlayerExact(args[0]);
         if (specifiedPlayer != null) {
            targetPlayer = specifiedPlayer;
            argOffset = 1;
         }
      }

      if (args.length >= argOffset + 1) {
         Material parsedMaterial = Material.matchMaterial(args[argOffset].toUpperCase());
         if (parsedMaterial == null) {
            sender.sendMessage("§c无效的矿石类型: " + args[argOffset] + " (例如: DIAMOND_ORE, EMERALD_ORE, ANCIENT_DEBRIS)");
            return true;
         }

         decoyMaterial = parsedMaterial;
      }

      if (args.length >= argOffset + 2) {
         try {
            throwDistance = Integer.parseInt(args[argOffset + 1]);
            if (throwDistance < 1 || throwDistance > 128) {
               sender.sendMessage("§c投放距离需在 1~128 之间。");
               return true;
            }
         } catch (NumberFormatException ex) {
            sender.sendMessage("§c无效的距离: " + args[argOffset + 1]);
            return true;
         }
      }

      if (!this.plugin.getConfigManager().isFakeOreDecoyThrowEnabled()) {
         sender.sendMessage("§c诱饵投放功能未启用 (config.yml: fake-ore-decoy-throw.enabled: true)。");
         return true;
      }

      if (targetPlayer == sender) {
         this.plugin.getXrayDetector().queueDecoyOrePlacement(targetPlayer, decoyMaterial, throwDistance);
      } else {
         this.plugin.getXrayDetector().queueDecoyOrePlacement(targetPlayer, decoyMaterial, throwDistance);
         sender.sendMessage("§a已为 " + targetPlayer.getName() + " 登记诱饵投放, 等待其挖掘方块后自动投放。");
      }

      return true;
   }
}
