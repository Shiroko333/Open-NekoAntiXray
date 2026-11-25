package sakuyaayane.nekoantixray;

import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ViolationLevelCommand implements CommandExecutor {
   private final NekoAntiXray plugin;

   public ViolationLevelCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      Player targetPlayer;
      if (args.length >= 1) {
         if (!sender.hasPermission("nekoantixray.admin")) {
            sender.sendMessage("§c你没有权限查询其他玩家的违规值。");
            return true;
         }

         targetPlayer = Bukkit.getPlayerExact(args[0]);
         if (targetPlayer == null) {
            sender.sendMessage("§c找不到玩家: " + args[0]);
            return true;
         }
      } else if (sender instanceof Player) {
         targetPlayer = (Player)sender;
      } else {
         sender.sendMessage("§c控制台请使用: /nax vl <玩家名>");
         return true;
      }

      double playerViolationLevel = this.plugin.getXrayDetector().getPlayerViolationLevel(targetPlayer);
      Double behaviorViolationLevel = this.getBehaviorViolationLevel(targetPlayer);
      sender.sendMessage("§6===== " + targetPlayer.getName() + " 违规值 =====");
      sender.sendMessage("§eXRay模式: §f" + String.format("%.1f", playerViolationLevel));
      if (behaviorViolationLevel != null) {
         sender.sendMessage("§e行为分析: §f" + String.format("%.1f", behaviorViolationLevel));
      } else {
         sender.sendMessage("§e行为分析: §7未启用或不可用");
      }

      double banThreshold = this.plugin.getConfigManager().getNotificationSystemXrayBanThreshold();
      String formattedThreshold = String.format("%.1f", banThreshold);
      String thresholdWarning = playerViolationLevel >= banThreshold ? " §c(已达阈值!)" : "";
      sender.sendMessage("§7封禁阈值: §f" + formattedThreshold + thresholdWarning);
      sender.sendMessage("§6========================");
      return true;
   }

   private Double getBehaviorViolationLevel(Player player) {
      try {
         MiningBehaviorDetector miningBehaviorDetector = this.plugin.getMiningBehaviorDetector();
         if (miningBehaviorDetector == null) {
            return null;
         }

         Method getterMethod = miningBehaviorDetector.getClass().getMethod("L", UUID.class);
         Object rawResult = getterMethod.invoke(miningBehaviorDetector, player.getUniqueId());
         return rawResult instanceof Number ? ((Number)rawResult).doubleValue() : null;
      } catch (ReflectiveOperationException ex) {
         return null;
      }
   }
}