package sakuyaayane.nekoantixray;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResendCommand implements CommandExecutor {
   private final NekoAntiXray plugin;

   public ResendCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      int radius = 4;
      Player targetPlayer;
      if (args.length >= 1) {
         if (!sender.hasPermission("nekoantixray.admin")) {
            sender.sendMessage("§c你没有权限执行此命令。");
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
         sender.sendMessage("§c控制台请使用: /nax resend <玩家名> [半径|cancel]");
         return true;
      }

      if (args.length >= 2 && args[1].equalsIgnoreCase("cancel")) {
         this.plugin.cancelPlayerObfuscation(targetPlayer);
         ChunkRefreshService chunkRefreshService = this.plugin.getChunkRefreshService();
         int revealedCount = chunkRefreshService.refreshBlocks(targetPlayer, radius, true);
         sender.sendMessage("§a已取消 " + targetPlayer.getName() + " 的区块混淆, 并揭示 §f" + revealedCount + " §a个方块。");
         sender.sendMessage("§8[追踪数据] §7" + this.plugin.getChunkRefreshService().getTrackingStatusSummary(targetPlayer, radius));
         if (revealedCount == 0) {
            sender.sendMessage("§e说明: 没有任何方块被揭示 —— 该玩家要么本就没有被混淆的方块(heavy-mode 未开/该世界未启用),要么已经全部是真实方块。");
         }

         sender.sendMessage("§7" + targetPlayer.getName() + " 将在 VL 再度触发(dynamic 模式)或管理员再次 /nax resend 时才恢复混淆。");
         this.sendDiagnosticInfo(sender, targetPlayer);
         return true;
      } else {
         if (args.length >= 2) {
            try {
               radius = Integer.parseInt(args[1]);
               if (radius < 1 || radius > 32) {
                  sender.sendMessage("§c半径需在 1~32 之间。");
                  return true;
               }
            } catch (NumberFormatException ex) {
               sender.sendMessage("§c无效的参数: " + args[1] + " (用法: /nax resend <玩家> [半径|cancel])");
               return true;
            }
         }

         if (!this.plugin.hasPaperAntiXrayApi()) {
            sender.sendMessage("§c当前核心不支持数据包混淆, 无法重发混淆区块。");
            return true;
         }

         this.plugin.forcePlayerObfuscation(targetPlayer);
         ChunkRefreshService chunkRefreshService = this.plugin.getChunkRefreshService();
         int rehiddenTrackedCount = chunkRefreshService.refreshBlocks(targetPlayer, radius, false);
         int rehiddenScannedCount = this.plugin.getChunkRefreshService().hideObfuscationTargets(targetPlayer, radius);
         int resentChunkCount = this.plugin.getChunkRefreshService().resendChunks(targetPlayer, radius);
         sender.sendMessage("§a已重发 " + targetPlayer.getName() + " 周围 " + resentChunkCount + " 个区块(半径 " + radius + "), 已恢复该玩家的区块混淆。");
         sender.sendMessage("§7方块级纠正: 追踪表 §f" + rehiddenTrackedCount + " §7个 + 真实世界扫描 §f" + rehiddenScannedCount + " §7个方块已重新隐藏(实时生效, 不依赖客户端重载区块)。");
         sender.sendMessage("§8[追踪数据] §7" + this.plugin.getChunkRefreshService().getTrackingStatusSummary(targetPlayer, radius));
         if (rehiddenTrackedCount == 0) {
            sender.sendMessage("§7说明: 当前没有【已显示为真实】的受保护方块, 因此无需重新隐藏 —— 这通常表示该玩家一直是混淆状态(正常)。若要查看真实方块, 请用 /nax resend " + targetPlayer.getName() + " cancel。");
         }

         this.sendDiagnosticInfo(sender, targetPlayer);
         return true;
      }
   }

   private void sendDiagnosticInfo(CommandSender sender, Player player) {
      boolean heavyModeEnabled = this.plugin.getConfigManager().isHeavyModeEnabled();
      String heavyModeSetting = this.plugin.getConfigManager().getHeavyModeSetting();
      double playerViolationLevel = this.plugin.getXrayDetector().getPlayerViolationLevel(player);
      boolean obfuscationCancelled = this.plugin.isObfuscationCancelled(player);
      boolean obfuscationActive = heavyModeEnabled
         && (!this.plugin.getConfigManager().isHeavyModeDynamic() || playerViolationLevel >= this.plugin.getConfigManager().getCheckXrayHeavyModeTriggerVl());
      String playerName = player.getName();
      String formattedVl = String.format("%.1f", playerViolationLevel);
      String obfuscationStatus = obfuscationActive ? "§a是" : "§c否";
      String cancelledSuffix = obfuscationCancelled ? " §8| §7已取消混淆" : "";
      sender.sendMessage("§8[诊断] §7heavy-mode=§f" + heavyModeSetting + " §8| §7" + playerName + " VL=§f" + formattedVl + " §8| §7当前混淆: " + obfuscationStatus + cancelledSuffix);
      if (!heavyModeEnabled) {
         sender.sendMessage("§e提示: heavy-mode 未开启 —— 插件不做数据包混淆, 重发不会改变矿物显示(玩家看到的仍是 Paper 反矿透的假矿)。要测试混淆/取消混淆请先开启 heavy-mode。");
      } else if (this.plugin.getConfigManager().isHeavyModeDynamic() && !obfuscationActive) {
         sender.sendMessage(
            "§e提示: dynamic 模式下该玩家 VL 未达 heavy-mode-trigger-vl(" + this.plugin.getConfigManager().getCheckXrayHeavyModeTriggerVl() + "), 其区块本就未混淆, 重发无可见变化。"
         );
      } else if (obfuscationActive) {
         sender.sendMessage("§7该玩家当前处于混淆状态(重发后仍为假矿); 如需查看真实方块请用 /nax resend " + player.getName() + " cancel。");
      }
   }
}
