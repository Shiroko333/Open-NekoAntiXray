package sakuyaayane.nekoantixray;

import java.io.Serializable;
import java.lang.reflect.Field;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class DiagnosticsCommand implements CommandExecutor {
   private final NekoAntiXray plugin;

   public DiagnosticsCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("nekoantixray.admin")) {
         sender.sendMessage("§c你没有权限执行此命令。");
         return true;
      }

      if (args.length >= 1 && args[0].equalsIgnoreCase("reset")) {
         ModuleTimingStatistics.reset();
         this.plugin.getObfuscationStatistics().reset();
         sender.sendMessage("§a模块耗时与混淆统计已重置。");
         return true;
      }

      sender.sendMessage("§6========== NekoAntiXray 诊断 ==========");
      sender.sendMessage("§e插件版本: §f" + this.plugin.getDescription().getVersion());
      sender.sendMessage("§e服务器: §f" + Bukkit.getName() + " " + Bukkit.getVersion());
      sender.sendMessage("§eBukkit版本: §f" + Bukkit.getBukkitVersion());

      try {
         double[] tps = Bukkit.getTPS();
         if (tps != null && tps.length >= 3) {
            String tpsFormatted = String.format("%.1f / %.1f / %.1f", tps[0], tps[1], tps[2]);
            String tpsStatus = tps[0] < 18.0 ? " §c⚠ 服务器性能不足!" : " §a正常";
            sender.sendMessage("§eTPS(1m/5m/15m): §f" + tpsFormatted + tpsStatus);
         } else {
            sender.sendMessage("§eTPS: §fN/A (非Paper核心)");
         }
      } catch (Throwable ex) {
         sender.sendMessage("§eTPS: §fN/A (" + ex.getClass().getSimpleName() + ")");
      }

      sender.sendMessage("§6---------- 模块耗时采样(累计/平均) ----------");
      sender.sendMessage(ModuleTimingStatistics.formatAll());
      sender.sendMessage("§7使用 /nax diagnose reset 重置采样。");
      Serializable asyncChunkSendEnabled = LeafCompatibilitySupport.isAsyncChunkSendEnabled();
      sender.sendMessage("§easync-chunk-send: §f" + (asyncChunkSendEnabled == null ? "非Leaf/未知" : asyncChunkSendEnabled));
      sender.sendMessage("§6---------- 反矿透API兼容 ----------");
      sender.sendMessage("§e" + ChunkPacketInfoAdapter.describeDetectedApi());
      sender.sendMessage("§6---------- 世界控制器 ----------");

      for (World world : Bukkit.getWorlds()) {
         Object controller = this.getChunkPacketBlockController(world);
         String controllerStatus;
         if (controller == null) {
            controllerStatus = "§c无法获取(字段未找到或核心不支持)";
         } else {
            String controllerClassName = controller.getClass().getName();
            if (controllerClassName.equals("sakuyaayane.nekoantixray.EnhancedChunkObfuscationController")) {
               controllerStatus = "§a插件增强控制器 ✓";
            } else if (controllerClassName.startsWith("io.papermc.paper.antixray.")) {
               controllerStatus = "§ePaper默认控制器: " + controller.getClass().getSimpleName();
            } else {
               controllerStatus = "§c未知类型: " + controllerClassName;
            }
         }

         sender.sendMessage("§e" + world.getName() + ": §f" + controllerStatus);
      }

      sender.sendMessage("§6---------- shouldModify 判定 ----------");
      if (sender instanceof Player player) {
         this.reportPlayerShouldModify(sender, player);
      } else {
         for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            this.reportPlayerShouldModify(sender, onlinePlayer);
         }
      }

      sender.sendMessage("§6---------- 混淆统计 ----------");
      ObfuscationStatistics obfuscationStatistics = this.plugin.getObfuscationStatistics();
      sender.sendMessage("§e" + obfuscationStatistics.toString());
      long failureCount = obfuscationStatistics.getFailureCount();
      long successCount = obfuscationStatistics.getSuccessCount();
      if (failureCount > 0L) {
         sender.sendMessage("§c检测到 " + failureCount + " 个区块混淆失败！请查看服务器日志中的 \"反X光区块混淆失败\"。");
      } else if (obfuscationStatistics.getAttemptCount() > 0L && successCount == 0L) {
         sender.sendMessage("§c检测到混淆链路异常：有尝试但从未成功。若日志无报错，可能是 Leaf 异步构造器(ClientboundLevelChunkPacketData)静默抛异常，请升级核心或检查 leaf.yml。");
      }

      sender.sendMessage("§6====================================");
      return true;
   }

   private void reportPlayerShouldModify(CommandSender sender, Player player) {
      try {
         ServerPlayer nmsPlayer = ((CraftPlayer)player).getHandle();
         ServerLevel serverLevel = ((CraftWorld)player.getWorld()).getHandle();
         int chunkX = player.getLocation().getBlockX() >> 4;
         int chunkZ = player.getLocation().getBlockZ() >> 4;
         LevelChunk loadedChunk = serverLevel.getChunkIfLoaded(chunkX, chunkZ);
         String modifyStatus;
         if (loadedChunk == null) {
            modifyStatus = "§e所在区块未加载，无法判定";
         } else {
            boolean shouldModify = serverLevel.chunkPacketBlockController.shouldModify(nmsPlayer, loadedChunk);
            modifyStatus = shouldModify ? "§a应混淆 ✓" : "§c已豁免(OP/use-permission)";
         }

         sender.sendMessage("§e" + player.getName() + ": §f" + modifyStatus);
      } catch (Throwable ex) {
         sender.sendMessage("§e" + player.getName() + ": §c判定失败: " + ex);
      }
   }

   private Object getChunkPacketBlockController(World world) {
      try {
         Class<?> worldClass = world.getClass();
         Object worldHandle = worldClass.getMethod("getHandle").invoke(world);

         for (Class<?> currentClass = worldHandle.getClass(); currentClass != null; currentClass = currentClass.getSuperclass()) {
            for (Field field : currentClass.getDeclaredFields()) {
               String fieldTypeName = field.getType().getName();
               if (fieldTypeName.startsWith("io.papermc.paper.antixray.") || field.getName().equals("chunkPacketBlockController")) {
                  field.setAccessible(true);
                  return field.get(worldHandle);
               }
            }
         }
      } catch (Throwable ex) {
      }

      return null;
   }
}
