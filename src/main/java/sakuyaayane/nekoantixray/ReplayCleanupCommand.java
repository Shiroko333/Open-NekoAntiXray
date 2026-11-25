package sakuyaayane.nekoantixray;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReplayCleanupCommand implements CommandExecutor {
   private final NekoAntiXray plugin;

   public ReplayCleanupCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("nekoantixray.cleanup")) {
         sender.sendMessage("§c你没有权限执行此命令。");
         return true;
      } else if (this.plugin.getSessionManager() == null) {
         sender.sendMessage("§c回放会话管理器尚未初始化。");
         return true;
      } else {
         String worldName = sender instanceof Player ? ((Player)sender).getWorld().getName() : null;
         sender.sendMessage("§e正在按区块清理遗留回放实体...");
         this.plugin.getSessionManager().cleanupLoadedReplayArmorStands(worldName).whenComplete((removedCount, error) -> {
            if (error != null) {
               this.sendResult(sender, "§c清理盔甲架时发生错误: " + this.getRootCauseMessage(error));
               this.plugin.getLogger().warning("清理盔甲架命令执行失败: " + this.getRootCauseMessage(error));
            } else if (removedCount > 0) {
               this.sendResult(sender, "§a总共清理了 " + removedCount + " 个遗留回放盔甲架。");
               this.plugin.logDebug("手动清理了 " + removedCount + " 个遗留回放盔甲架，执行者: " + sender.getName());
            } else {
               this.sendResult(sender, "§e没有找到需要清理的遗留回放盔甲架。");
            }
         });
         return true;
      }
   }

   private void sendResult(CommandSender sender, String message) {
      try {
         if (sender instanceof Player player) {
            this.plugin.getFoliaScheduler().getImpl().runAtEntity(player, task -> player.sendMessage(message));
         } else {
            this.plugin.getFoliaScheduler().getImpl().runNextTick(task -> sender.sendMessage(message));
         }
      } catch (Exception ex) {
         this.plugin.logDebug("无法发送盔甲架清理结果: " + ex.getMessage());
      }
   }

   private String getRootCauseMessage(Throwable throwable) {
      Throwable rootCause = throwable;

      while (rootCause.getCause() != null) {
         rootCause = rootCause.getCause();
      }

      return rootCause.getMessage() != null ? rootCause.getMessage() : rootCause.getClass().getSimpleName();
   }
}
