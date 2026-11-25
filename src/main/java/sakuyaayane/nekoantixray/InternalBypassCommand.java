package sakuyaayane.nekoantixray;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InternalBypassCommand implements CommandExecutor {
   private final NekoAntiXray plugin;
   private final InternalBypassManager internalBypassManager;

   public InternalBypassCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
      this.internalBypassManager = plugin.getXrayDetector().getInternalBypassManager();
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("nekoantixray.admin")) {
         sender.sendMessage(ChatColor.RED + "你没有权限使用此命令。");
         return true;
      }

      if (args.length == 0) {
         this.sendUsageHelp(sender);
         return true;
      }

      String subCommand = args[0].toLowerCase();
      switch (subCommand) {
         case "grant":
            this.handleGrantCommand(sender, args);
            break;
         case "remove":
            this.handleRemoveCommand(sender, args);
            break;
         case "list":
            this.handleListCommand(sender);
            break;
         case "clear":
            this.handleClearCommand(sender);
            break;
         case "info":
            this.handleInfoCommand(sender, args);
            break;
         case "extend":
            this.handleExtendCommand(sender, args);
            break;
         default:
            this.sendUsageHelp(sender);
      }

      return true;
   }

   private void handleGrantCommand(CommandSender sender, String[] args) {
      if (args.length < 3) {
         sender.sendMessage(ChatColor.RED + "用法: /internalbypass grant <玩家> <时间(秒)> [原因]");
      } else {
         Player targetPlayer = Bukkit.getPlayer(args[1]);
         if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "玩家 " + args[1] + " 不在线或不存在。");
         } else {
            long durationSeconds;
            try {
               durationSeconds = Long.parseLong(args[2]);
               if (durationSeconds <= 0L) {
                  sender.sendMessage(ChatColor.RED + "时间必须是正数。");
                  return;
               }
            } catch (NumberFormatException ex) {
               sender.sendMessage(ChatColor.RED + "无效的时间格式: " + args[2]);
               return;
            }

            String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "管理员手动授予";
            this.internalBypassManager.grantBypass(targetPlayer, durationSeconds, reason);
            sender.sendMessage(ChatColor.GREEN + "已给予玩家 " + targetPlayer.getName() + " " + durationSeconds + "秒的内部检测豁免。");
            sender.sendMessage(ChatColor.GRAY + "原因: " + reason);
            targetPlayer.sendMessage(ChatColor.YELLOW + "你获得了 " + durationSeconds + "秒的检测豁免。");
            targetPlayer.sendMessage(ChatColor.GRAY + "原因: " + reason);
         }
      }
   }

   private void handleRemoveCommand(CommandSender sender, String[] args) {
      if (args.length < 2) {
         sender.sendMessage(ChatColor.RED + "用法: /internalbypass remove <玩家>");
      } else {
         Player targetPlayer = Bukkit.getPlayer(args[1]);
         if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "玩家 " + args[1] + " 不在线或不存在。");
         } else if (!this.internalBypassManager.hasBypass(targetPlayer)) {
            sender.sendMessage(ChatColor.YELLOW + "玩家 " + targetPlayer.getName() + " 当前没有内部检测豁免。");
         } else {
            this.internalBypassManager.removeBypass(targetPlayer);
            sender.sendMessage(ChatColor.GREEN + "已移除玩家 " + targetPlayer.getName() + " 的内部检测豁免。");
            targetPlayer.sendMessage(ChatColor.YELLOW + "你的检测豁免已被管理员移除。");
         }
      }
   }

   private void handleListCommand(CommandSender sender) {
      Map<String, String> activeBypassSummaries = this.internalBypassManager.getActiveBypassSummaries();
      if (activeBypassSummaries.isEmpty()) {
         sender.sendMessage(ChatColor.YELLOW + "当前没有玩家拥有内部检测豁免。");
      } else {
         sender.sendMessage(ChatColor.GREEN + "=== 当前内部检测豁免列表 ===");

         for (Entry<String, String> entry : activeBypassSummaries.entrySet()) {
            sender.sendMessage(ChatColor.WHITE + entry.getKey() + ": " + ChatColor.GRAY + entry.getValue());
         }

         sender.sendMessage(ChatColor.GREEN + "总计: " + activeBypassSummaries.size() + " 个玩家");
      }
   }

   private void handleClearCommand(CommandSender sender) {
      int activeBypassCount = this.internalBypassManager.getActiveBypassCount();
      this.internalBypassManager.clearAllBypasses();
      sender.sendMessage(ChatColor.GREEN + "已清理所有内部检测豁免，共影响 " + activeBypassCount + " 个玩家。");
   }

   private void handleInfoCommand(CommandSender sender, String[] args) {
      if (args.length < 2) {
         sender.sendMessage(ChatColor.RED + "用法: /internalbypass info <玩家>");
      } else {
         Player targetPlayer = Bukkit.getPlayer(args[1]);
         if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "玩家 " + args[1] + " 不在线或不存在。");
         } else if (!this.internalBypassManager.hasBypass(targetPlayer)) {
            sender.sendMessage(ChatColor.YELLOW + "玩家 " + targetPlayer.getName() + " 当前没有内部检测豁免。");
         } else {
            long remainingBypassSeconds = this.internalBypassManager.getRemainingBypassSeconds(targetPlayer);
            String bypassReason = this.internalBypassManager.getBypassReason(targetPlayer);
            sender.sendMessage(ChatColor.GREEN + "=== 玩家 " + targetPlayer.getName() + " 的豁免信息 ===");
            sender.sendMessage(ChatColor.WHITE + "剩余时间: " + ChatColor.YELLOW + remainingBypassSeconds + " 秒");
            sender.sendMessage(ChatColor.WHITE + "豁免原因: " + ChatColor.YELLOW + bypassReason);
         }
      }
   }

   private void handleExtendCommand(CommandSender sender, String[] args) {
      if (args.length < 3) {
         sender.sendMessage(ChatColor.RED + "用法: /internalbypass extend <玩家> <额外时间(秒)>");
      } else {
         Player targetPlayer = Bukkit.getPlayer(args[1]);
         if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "玩家 " + args[1] + " 不在线或不存在。");
         } else if (!this.internalBypassManager.hasBypass(targetPlayer)) {
            sender.sendMessage(ChatColor.YELLOW + "玩家 " + targetPlayer.getName() + " 当前没有内部检测豁免。");
         } else {
            long extraSeconds;
            try {
               extraSeconds = Long.parseLong(args[2]);
               if (extraSeconds <= 0L) {
                  sender.sendMessage(ChatColor.RED + "时间必须是正数。");
                  return;
               }
            } catch (NumberFormatException ex) {
               sender.sendMessage(ChatColor.RED + "无效的时间格式: " + args[2]);
               return;
            }

            boolean extended = this.internalBypassManager.extendBypass(targetPlayer, extraSeconds);
            if (extended) {
               sender.sendMessage(ChatColor.GREEN + "已延长玩家 " + targetPlayer.getName() + " 的豁免时间 " + extraSeconds + " 秒。");
               targetPlayer.sendMessage(ChatColor.YELLOW + "你的检测豁免时间已被延长 " + extraSeconds + " 秒。");
            } else {
               sender.sendMessage(ChatColor.RED + "延长豁免时间失败。");
            }
         }
      }
   }

   private void sendUsageHelp(CommandSender sender) {
      sender.sendMessage(ChatColor.GOLD + "=== 内部豁免管理命令 ===");
      sender.sendMessage(ChatColor.WHITE + "/internalbypass grant <玩家> <时间(秒)> [原因] " + ChatColor.GRAY + "- 给予玩家内部检测豁免");
      sender.sendMessage(ChatColor.WHITE + "/internalbypass remove <玩家> " + ChatColor.GRAY + "- 移除玩家的内部检测豁免");
      sender.sendMessage(ChatColor.WHITE + "/internalbypass list " + ChatColor.GRAY + "- 列出所有拥有豁免的玩家");
      sender.sendMessage(ChatColor.WHITE + "/internalbypass clear " + ChatColor.GRAY + "- 清理所有内部检测豁免");
      sender.sendMessage(ChatColor.WHITE + "/internalbypass info <玩家> " + ChatColor.GRAY + "- 查看玩家的豁免信息");
      sender.sendMessage(ChatColor.WHITE + "/internalbypass extend <玩家> <时间(秒)> " + ChatColor.GRAY + "- 延长玩家的豁免时间");
   }
}
