package sakuyaayane.nekoantixray.listener;

import java.util.HashSet;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.Location;
import org.bukkit.World;
import sakuyaayane.nekoantixray.compat.ServerLevelAccess;
import sakuyaayane.nekoantixray.NekoAntiXray;
import sakuyaayane.nekoantixray.raytrace.PlayerRayTraceState;
import sakuyaayane.nekoantixray.world.ChunkCoordinateCodec;
import sakuyaayane.nekoantixray.world.ChunkKey;

public final class ChunkResendListener implements Listener {
   private final NekoAntiXray plugin;

   public ChunkResendListener(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPortalCreate(PortalCreateEvent event) {
      World world = event.getWorld();
      if (this.plugin.getConfigManager().isHeavyModeEnabled() || this.plugin.isWorldSettingsDefaultEnabled(world)) {
         List<BlockState> blocks = event.getBlocks();
         if (blocks != null && !blocks.isEmpty()) {
            HashSet<Long> chunkKeys = new HashSet<>();

            for (BlockState blockState : blocks) {
               Block block = blockState.getBlock();
               int chunkX = block.getX() >> 4;
               int chunkZ = block.getZ() >> 4;
               chunkKeys.add(ChunkCoordinateCodec.encode(chunkX, chunkZ));
            }

            for (long chunkKey : chunkKeys) {
               int chunkX = ChunkCoordinateCodec.decodeChunkX(chunkKey);
               int chunkZ = ChunkCoordinateCodec.decodeChunkZ(chunkKey);
               this.resendChunkToNearbyPlayers(world, chunkX, chunkZ);
            }
         }
      }
   }

   private void triggerChunkResend(World world, int chunkX, int chunkZ) {
      this.resendChunkToNearbyPlayers(world, chunkX, chunkZ);
   }

   private void resendChunkToNearbyPlayers(World world, int chunkX, int chunkZ) {
      long chunkKey = ChunkCoordinateCodec.encode(chunkX, chunkZ);

      for (Player player : world.getPlayers()) {
         if (player.isOnline()) {
            int viewDistance = world.getViewDistance();
            int playerChunkX = player.getLocation().getBlockX() >> 4;
            int playerChunkZ = player.getLocation().getBlockZ() >> 4;
            if (Math.abs(playerChunkX - chunkX) <= viewDistance && Math.abs(playerChunkZ - chunkZ) <= viewDistance) {
               this.plugin
                  .getFoliaScheduler()
                  .getImpl()
                  .runAtLocation(new Location(world, (chunkX << 4) + 8.0, world.getMinHeight() + 1.0, (chunkZ << 4) + 8.0), wrappedTask -> {
                     if (player.isOnline() && player.getWorld().equals(world)) {
                        ServerPlayer handle = ((CraftPlayer)player).getHandle();
                        ServerLevel serverLevel = ServerLevelAccess.getServerLevel(handle);
                        LevelChunk chunkIfLoaded = serverLevel.getChunkIfLoaded(chunkX, chunkZ);
                        PlayerRayTraceState playerRayTraceState = (PlayerRayTraceState)this.plugin.getPlayerRayTraceStates().get(player.getUniqueId());
                        if (playerRayTraceState != null) {
                           playerRayTraceState.getChunks().remove(new ChunkKey(chunkKey));
                        }

                        if (chunkIfLoaded != null) {
                           PlayerChunkSender.sendChunk(handle.connection, serverLevel, chunkIfLoaded);
                        }
                     }
                  });
            }
         }
      }
   }
}
