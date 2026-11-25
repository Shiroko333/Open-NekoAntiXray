package sakuyaayane.nekoantixray;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class HelpCommand implements CommandExecutor {
   private static final String ADMIN_PERMISSION = "nekoantixray.admin";
   private static final int COMMANDS_PER_PAGE = 10;
   private static final Map<String, String[]> COMMAND_DESCRIPTIONS = new LinkedHashMap<>();
   private final NaxCommandDispatcher naxCommandDispatcher;

   public HelpCommand(NaxCommandDispatcher naxCommandDispatcher) {
      this.naxCommandDispatcher = naxCommandDispatcher;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("nekoantixray.admin")) {
         sender.sendMessage("§c你没有权限查看指令帮助(需要 nekoantixray.admin)。");
         return true;
      }

      ArrayList commandNames = new ArrayList(this.naxCommandDispatcher.getSubCommandNames());
      if (args.length >= 1) {
         String firstArg = args[0].trim();
         if (firstArg.matches("\\d+")) {
            int pageNumber = Integer.parseInt(firstArg);
            this.sendHelpPage(sender, commandNames, pageNumber);
            return true;
         }

         String subCommandName = firstArg.toLowerCase();
         if (!commandNames.contains(subCommandName)) {
            sender.sendMessage("§c未知的子命令: " + firstArg + " §7(输入 §f/nax help§7 查看全部指令)");
            return true;
         }

         String[] descriptionEntry = COMMAND_DESCRIPTIONS.getOrDefault(subCommandName, new String[]{"暂无说明", null});
         sender.sendMessage("§6§l/nax " + subCommandName + " §7- §f" + descriptionEntry[0]);
         if (descriptionEntry[1] != null) {
            sender.sendMessage("§7用法: §f" + descriptionEntry[1]);
         }

         return true;
      } else {
         this.sendHelpPage(sender, commandNames, 1);
         return true;
      }
   }

   private void sendHelpPage(CommandSender sender, List commandNames, int requestedPage) {
      int totalCommands = commandNames.size();
      int totalPages = Math.max(1, (totalCommands + 10 - 1) / 10);
      int currentPage = Math.max(1, Math.min(requestedPage, totalPages));
      sender.sendMessage("§6§lNekoAntiXray §7指令列表 §8(第 " + currentPage + "/" + totalPages + " 页 · 共 " + totalCommands + " 条)");
      int startIndex = (currentPage - 1) * 10;
      int endIndex = Math.min(startIndex + 10, totalCommands);

      for (int i = startIndex; i < endIndex; i++) {
         String commandName = (String)commandNames.get(i);
         String[] descriptionEntry = COMMAND_DESCRIPTIONS.getOrDefault(commandName, new String[]{"暂无说明", null});
         sender.sendMessage("§e/nax " + commandName + " §7- §f" + descriptionEntry[0]);
      }

      if (totalPages > 1) {
         sender.sendMessage("§7翻页: §f/nax help <页码> §7| 单条说明: §f/nax help <指令>");
      }
   }

   static {
      COMMAND_DESCRIPTIONS.put("reload", new String[]{"重新加载配置文件(部分配置即时生效)", null});
      COMMAND_DESCRIPTIONS.put("gui", new String[]{"打开游戏内图形化配置界面(点击修改并保存)", null});
      COMMAND_DESCRIPTIONS.put("ban", new String[]{"立即封禁指定玩家; 配置了外部指令时交由第三方封禁插件处理", "/nax ban <玩家> [原因]"});
      COMMAND_DESCRIPTIONS.put("resetviolation", new String[]{"清零指定玩家的违规值(VL)", "/nax resetviolation <玩家>"});
      COMMAND_DESCRIPTIONS.put("vl", new String[]{"查看/管理玩家违规值", "/nax vl [玩家]"});
      COMMAND_DESCRIPTIONS.put("summary", new String[]{"查看离线期间或指定时间的精简检测概况(检测次数/最后VL/是否封禁)", "/nax summary [时间]  (30m / 6h / 3d / all)"});
      COMMAND_DESCRIPTIONS.put("report", new String[]{"查看指定玩家的详细检测与封禁记录", "/nax report [玩家] [时间]"});
      COMMAND_DESCRIPTIONS.put("probeguard", new String[]{"鬼手探测防护(反 TrueSight)状态、拦截与放行计数", "/nax probeguard [玩家]"});
      COMMAND_DESCRIPTIONS.put("resend", new String[]{"强制重发区块, 或取消某玩家的区块混淆(仅管理员可控)", "/nax resend [玩家] [半径|cancel]"});
      COMMAND_DESCRIPTIONS.put("decoy", new String[]{"向指定方向抛投诱饵矿石(测试用)", "/nax decoy [玩家] [矿石] [距离]"});
      COMMAND_DESCRIPTIONS.put("diagnose", new String[]{"反矿透诊断: 控制器状态、跨版本兼容层、shouldModify 判定", null});
      COMMAND_DESCRIPTIONS.put("stats", new String[]{"区块混淆运行统计(尝试/成功/跳过/失败)", null});
      COMMAND_DESCRIPTIONS.put("debugchunks", new String[]{"查看指定区块最近的混淆状态", "/nax debugchunks [半径]"});
      COMMAND_DESCRIPTIONS.put("togglemassexcavation", new String[]{"切换大规模挖掘检测的启停", null});
      COMMAND_DESCRIPTIONS.put("internalbypass", new String[]{"内部豁免管理: 授予/移除/查看跳过检测的玩家", "/nax internalbypass <grant|remove|list|info|extend|clear> ..."});
      COMMAND_DESCRIPTIONS.put("record", new String[]{"深度学习检测数据录制管理(开始/停止/状态)", null});
      COMMAND_DESCRIPTIONS.put("replay", new String[]{"回放系统: 录制并回看玩家行为", "/nax replay <stop|pause|forward|backward|tp>"});
      COMMAND_DESCRIPTIONS.put("cleanup", new String[]{"清理插件残留的盔甲架等实体", null});
      COMMAND_DESCRIPTIONS.put("announcement", new String[]{"公告系统: 手动发送/开关定期公告", null});
      COMMAND_DESCRIPTIONS.put("realseed", new String[]{"查看当前世界的真实种子(安全种子功能)", "/nax realseed [世界]"});
      COMMAND_DESCRIPTIONS.put("secureseed", new String[]{"安全种子工具: 真实种子校验与相关处理", null});
      COMMAND_DESCRIPTIONS.put("orestats", new String[]{"矿石统计数据", "/nax orestats [玩家]"});
      COMMAND_DESCRIPTIONS.put("oretop", new String[]{"挖矿排行(按玩家/矿石统计)", null});
      COMMAND_DESCRIPTIONS.put("oretrack", new String[]{"矿石追踪: 开关追踪与查询记录", "/nax oretrack [玩家]"});
      COMMAND_DESCRIPTIONS.put("help", new String[]{"显示本帮助(指令列表/单条说明)", "/nax help [页码|子命令]"});
   }
}
