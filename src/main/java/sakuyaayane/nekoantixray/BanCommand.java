package sakuyaayane.nekoantixray;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sakuyaayane.nekoantixray.lib.folialib.FoliaLib;

public class BanCommand implements CommandExecutor {
   private final NekoAntiXray plugin;
   private final XrayDetector xrayDetector;
   private final FoliaLib foliaLib;
   private static final long CLEANUP_DELAY_TICKS = 20L;

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length < 1) {
         sender.sendMessage("§c用法: /nax ban <玩家名> [原因]");
         return true;
      }

      String targetName = args[0];
      Player targetPlayer = Bukkit.getPlayer(targetName);
      if (targetPlayer == null) {
         sender.sendMessage("§c找不到玩家: " + targetName);
         return true;
      }

      double banThreshold = this.plugin.getConfigManager().getNotificationSystemXrayBanThreshold();
      String banReason = args.length > 1
         ? String.join(" ", args).substring(args[0].length() + 1)
         : this.plugin.getConfigManager().getNotificationSystemBanMessagesBanReason().replace("%vl%", String.format("%.1f", banThreshold));
      this.plugin.banPlayerOrCommand(targetPlayer, "xray", banReason, banThreshold);
      this.plugin
         .logDebug(
            "banPlayerOrCommand 调用完成: 玩家="
               + targetPlayer.getName()
               + ", UUID="
               + String.valueOf(targetPlayer.getUniqueId())
               + ", 在线="
               + targetPlayer.isOnline()
               + ", 原因="
               + banReason
         );
      this.foliaLib.getImpl().runLater(wrappedTask -> this.xrayDetector.clearPlayerData(targetPlayer.getUniqueId()), 20L);
      sender.sendMessage("§a已成功封禁玩家 " + targetName + " 原因: " + banReason);
      sender.sendMessage("§a玩家的所有相关数据已被清除。");
      if (this.plugin.getConfigManager().isDebug()) {
         this.plugin.getLogger().info("管理员 " + sender.getName() + " 封禁了玩家 " + targetName + "，原因: " + banReason);
         this.plugin.getLogger().info("玩家 " + targetName + " 的所有相关数据已被清除");
      }

      return true;
   }

   public BanCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
      this.xrayDetector = plugin.getXrayDetector();
      this.foliaLib = plugin.getFoliaScheduler();
   }
}
