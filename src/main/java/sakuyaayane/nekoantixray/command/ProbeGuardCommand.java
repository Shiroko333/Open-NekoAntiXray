package sakuyaayane.nekoantixray.command;

import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sakuyaayane.nekoantixray.config.ConfigManager;
import sakuyaayane.nekoantixray.NekoAntiXray;
import sakuyaayane.nekoantixray.packet.ProbeGuardPacketListener;

public class ProbeGuardCommand implements CommandExecutor {
   private final NekoAntiXray plugin;
   private static final long MILLIS_PER_SECOND = 1000L;

   public ProbeGuardCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("nekoantixray.admin")) {
         sender.sendMessage("§c你没有权限使用此命令。");
         return true;
      }

      ConfigManager configManager = this.plugin.getConfigManager();
      ProbeGuardPacketListener probeGuardPacketListener = this.plugin.getProbeGuardPacketListener();
      sender.sendMessage("§8§m--------§r §c鬼手探测防护 §7(Probe / 反 TrueSight) §8§m--------");
      String configToggleState = this.formatToggleState(configManager.isCheckXrayProbeGuardEnabled());
      String listenerStatus = probeGuardPacketListener != null ? "§a已注册" : "§c未注册";
      String activeStatus = probeGuardPacketListener != null && probeGuardPacketListener.isActive() ? "§a是" : "§c否";
      sender.sendMessage("§7配置开关: " + configToggleState + " §8| §7监听器: " + listenerStatus + " §8| §7当前生效: " + activeStatus);
      String heavyModeSetting = configManager.getHeavyModeSetting();
      String requireHeavyModeState = this.formatToggleState(configManager.isCheckXrayProbeGuardRequireHeavyMode());
      String onlyObfuscatedState = this.formatToggleState(configManager.isCheckXrayProbeGuardOnlyObfuscatedPlayers());
      sender.sendMessage("§7heavy-mode: §f" + heavyModeSetting + " §8| §7require-heavy-mode: " + requireHeavyModeState + " §8| §7only-obfuscated-players: " + onlyObfuscatedState);
      sender.sendMessage(
         "§7手长: §f"
            + (configManager.isCheckXrayProbeGuardUseGamemodeReach() ? "按游戏模式/属性(生存4.5/创造5.0)" : String.format("%.2f", configManager.getCheckXrayProbeGuardMaxReach()))
            + " §7+容差 §f"
            + String.format("%.2f", configManager.getCheckXrayProbeGuardReachTolerance())
      );
      String crosshairCheckState = this.formatToggleState(configManager.isCheckXrayProbeGuardCheckCrosshair());
      String lineOfSightCheckState = this.formatToggleState(configManager.isCheckXrayProbeGuardCheckLineOfSight());
      String abortGraceText = configManager.getCheckXrayProbeGuardAbortGraceMs() <= 0L ? "不限制" : configManager.getCheckXrayProbeGuardAbortGraceMs() + "ms";
      String vlPerCancelText = String.format("%.1f", configManager.getCheckXrayProbeGuardVlPerCancel());
      sender.sendMessage("§7校验: 准星=" + crosshairCheckState + " §8| §7遮挡=" + lineOfSightCheckState + " §8| §7ABORT宽限=§f" + abortGraceText + " §8| §7每次VL=§f" + vlPerCancelText);
      if (probeGuardPacketListener == null) {
         sender.sendMessage("§e监听器未注册: 请确认 PacketEvents 已安装, 且 check.xray.probe-guard.enabled 为 true。");
         return true;
      }

      long checkedPacketCount = probeGuardPacketListener.getCheckedPacketCount();
      long cancelledPacketCount = probeGuardPacketListener.getCancelledPacketCount();
      String lastCancelText = probeGuardPacketListener.getLastCancelTimeMs() == 0L ? "从未" : (System.currentTimeMillis() - probeGuardPacketListener.getLastCancelTimeMs()) / 1000L + "秒前";
      sender.sendMessage("§7已检查挖掘包: §f" + checkedPacketCount + " §8| §7已拦截: §f" + cancelledPacketCount + " §8| §7最近拦截: §f" + lastCancelText);
      Map cancelReasons = probeGuardPacketListener.getCancelReasons();
      if (!cancelReasons.isEmpty()) {
         sender.sendMessage("§7拦截原因: §f" + cancelReasons);
      }

      Map gatedReasons = probeGuardPacketListener.getGatedReasons();
      if (!gatedReasons.isEmpty()) {
         sender.sendMessage("§7误伤保护放行(未达阈值, 不拦截不加VL): §f" + gatedReasons);
         sender.sendMessage("§8  ↑ 若这些计数很高, 说明对应判定在误伤正常挖掘, 可关闭该判定(check-crosshair / check-line-of-sight)或调大 min-invalid-per-window。");
      }

      if (args.length >= 1) {
         Player targetPlayer = Bukkit.getPlayerExact(args[0]);
         if (targetPlayer == null) {
            sender.sendMessage("§c找不到在线玩家: " + args[0]);
            return true;
         } else {
            sender.sendMessage("§7[" + targetPlayer.getName() + "] " + probeGuardPacketListener.getPlayerDebugStatus(targetPlayer));
            return true;
         }
      } else {
         for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            sender.sendMessage("§7[" + onlinePlayer.getName() + "] " + probeGuardPacketListener.getPlayerDebugStatus(onlinePlayer));
         }

         return true;
      }
   }

   private String formatToggleState(boolean enabled) {
      return enabled ? "§a开" : "§c关";
   }
}