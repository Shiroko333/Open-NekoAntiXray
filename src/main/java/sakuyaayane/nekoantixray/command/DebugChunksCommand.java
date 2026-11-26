package sakuyaayane.nekoantixray.command;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import sakuyaayane.nekoantixray.NekoAntiXray;

public class DebugChunksCommand implements CommandExecutor {
   private static final int PARTICLE_REFRESH_INTERVAL_TICKS = 10;
   private final NekoAntiXray plugin;

   public DebugChunksCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender instanceof Player player) {
         if (!sender.hasPermission("nekoantixray.admin")) {
            sender.sendMessage("§c你没有权限执行此命令。");
            return true;
         }

         int radius = 4;
         int durationSeconds = 5;
         if (args.length >= 1) {
            try {
               radius = Math.max(1, Math.min(Integer.parseInt(args[0]), 32));
            } catch (NumberFormatException ex) {
               sender.sendMessage("§c无效的半径: " + args[0] + " (用法: /nax debugchunks [半径] [持续秒数])");
               return true;
            }
         }

         if (args.length >= 2) {
            try {
               durationSeconds = Math.max(1, Math.min(Integer.parseInt(args[1]), 60));
            } catch (NumberFormatException ex) {
               sender.sendMessage("§c无效的持续秒数: " + args[1] + " (用法: /nax debugchunks [半径] [持续秒数])");
               return true;
            }
         }

         int finalRadius = radius;
         int finalDurationSeconds = durationSeconds;
         ConcurrentHashMap chunkObfuscationStates = this.plugin.getChunkObfuscationStates();
         if (chunkObfuscationStates.isEmpty()) {
            sender.sendMessage("§e暂无已记录的区块混淆状态：请先移动几步(触发新区块加载/混淆)再执行本命令。");
         }

         Location location = player.getLocation();
         World world = location.getWorld();
         int centerChunkX = location.getBlockX() >> 4;
         int centerChunkZ = location.getBlockZ() >> 4;
         int particleY = location.getBlockY() + 2;
         int[] totalChunkCount = new int[]{0};
         int[] obfuscatedChunkCount = new int[]{0};
         Runnable task = () -> this.renderDebugChunksInRadius(
            totalChunkCount, obfuscatedChunkCount, finalRadius, centerChunkX, centerChunkZ, chunkObfuscationStates, world, player, particleY
         );
         task.run();
         long endTimeMillis = System.currentTimeMillis() + finalDurationSeconds * 1000L;
         this.plugin.getFoliaScheduler().getImpl().runTimer(wrappedTask -> {
            task.run();
            if (System.currentTimeMillis() >= endTimeMillis) {
               wrappedTask.cancel();
            }
         }, 10L, 10L);
         int totalChunks = totalChunkCount[0];
         int obfuscatedChunks = obfuscatedChunkCount[0];
         int unobfuscatedChunks = totalChunks - obfuscatedChunks;
         sender.sendMessage(
            "§a已框选 " + totalChunks + " 个区块 (火焰=已混淆 " + obfuscatedChunks + " 个, 音爆=未混淆 " + unobfuscatedChunks + " 个), 持续 " + finalDurationSeconds + " 秒, 半径=" + finalRadius
         );
         sender.sendMessage("§7提示: 粒子可见范围约2-3个区块; 持续期间移动可看到新区块被实时框选。");
         return true;
      } else {
         sender.sendMessage("§c该命令只能由玩家执行。");
         return true;
      }
   }

   private void renderDebugChunksInRadius(
      int[] totalChunkCount,
      int[] obfuscatedChunkCount,
      int radius,
      int centerChunkX,
      int centerChunkZ,
      Map chunkObfuscationStates,
      World world,
      Player player,
      int particleY
   ) {
      totalChunkCount[0] = 0;
      obfuscatedChunkCount[0] = 0;

      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            int chunkX = centerChunkX + dx;
            int chunkZ = centerChunkZ + dz;
            Boolean isObfuscated = (Boolean)chunkObfuscationStates.get(this.computeWorldChunkKey(world, chunkX, chunkZ));
            if (isObfuscated != null) {
               this.spawnChunkBorderParticles(player, chunkX, chunkZ, particleY, isObfuscated);
               totalChunkCount[0]++;
               if (isObfuscated) {
                  obfuscatedChunkCount[0]++;
               }
            }
         }
      }
   }

   private void spawnChunkBorderParticles(Player player, int chunkX, int chunkZ, int particleY, boolean obfuscated) {
      Particle particle = obfuscated ? Particle.FLAME : Particle.SONIC_BOOM;
      int particleCount = obfuscated ? 6 : 4;
      int baseBlockX = chunkX << 4;
      int baseBlockZ = chunkZ << 4;

      for (int i = 0; i < 4; i++) {
         double edgeX = baseBlockX + i * 5 + 0.5;
         double edgeZ = baseBlockZ + i * 5 + 0.5;
         player.spawnParticle(particle, edgeX, particleY, baseBlockZ + 0.5, particleCount, 0.3, 0.6, 0.3, 0.0);
         player.spawnParticle(particle, edgeX, particleY + 1, baseBlockZ + 0.5, particleCount, 0.3, 0.6, 0.3, 0.0);
         player.spawnParticle(particle, edgeX, particleY, baseBlockZ + 15.5, particleCount, 0.3, 0.6, 0.3, 0.0);
         player.spawnParticle(particle, edgeX, particleY + 1, baseBlockZ + 15.5, particleCount, 0.3, 0.6, 0.3, 0.0);
         player.spawnParticle(particle, baseBlockX + 0.5, particleY, edgeZ, particleCount, 0.3, 0.6, 0.3, 0.0);
         player.spawnParticle(particle, baseBlockX + 0.5, particleY + 1, edgeZ, particleCount, 0.3, 0.6, 0.3, 0.0);
         player.spawnParticle(particle, baseBlockX + 15.5, particleY, edgeZ, particleCount, 0.3, 0.6, 0.3, 0.0);
         player.spawnParticle(particle, baseBlockX + 15.5, particleY + 1, edgeZ, particleCount, 0.3, 0.6, 0.3, 0.0);
      }
   }

   private long computeWorldChunkKey(World world, int chunkX, int chunkZ) {
      return world.getUID().getMostSignificantBits() << 32 ^ world.getUID().getLeastSignificantBits() & 4294967295L ^ (long)chunkX << 32 ^ chunkZ & 4294967295L;
   }
}
