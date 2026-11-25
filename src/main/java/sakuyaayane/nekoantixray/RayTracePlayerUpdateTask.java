package sakuyaayane.nekoantixray;

import io.netty.channel.Channel;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public final class RayTracePlayerUpdateTask extends BukkitRunnable implements Consumer<ScheduledTask> {
   private final NekoAntiXray plugin;
   private final Player player;
   private static final String TASK_ACTION_NAME = "update";

   public RayTracePlayerUpdateTask(NekoAntiXray plugin) {
      this(plugin, null);
   }

   public RayTracePlayerUpdateTask(NekoAntiXray plugin, Player player) {
      this.plugin = plugin;
      this.player = player;
   }

   @Override
   public void run() {
      if (this.player == null) {
         this.plugin.getServer().getOnlinePlayers().forEach(onlinePlayer -> this.update(onlinePlayer));
      } else {
         this.update(this.player);
      }
   }

   public void update(Player targetPlayer) {
      PlayerRayTraceState playerRayTraceState = (PlayerRayTraceState)this.plugin.getPlayerRayTraceStates().get(targetPlayer.getUniqueId());
      if (this.plugin.hasRayTraceState(targetPlayer, playerRayTraceState)) {
         World world = playerRayTraceState.getViewSnapshots()[0].getWorld();
         if (targetPlayer.getWorld().equals(world)) {
            this.syncPlayerEyeView(targetPlayer, playerRayTraceState, world);
            ConcurrentMap chunks = playerRayTraceState.getChunks();
            ServerLevel serverLevel = ((CraftWorld)world).getHandle();
            Environment environment = world.getEnvironment();

            BlockVisibilityUpdate blockVisibilityUpdate;
            while ((blockVisibilityUpdate = playerRayTraceState.pollUpdate()) != null) {
               TrackedChunkVisibility trackedChunkVisibility = blockVisibilityUpdate.getTrackedChunkVisibility();
               if (trackedChunkVisibility.getChunk() != null && chunks.get(trackedChunkVisibility.getKey()) == trackedChunkVisibility) {
                  BlockPos blockPos = blockVisibilityUpdate.getBlock();
                  if (world.isChunkLoaded(blockPos.getX() >> 4, blockPos.getZ() >> 4)) {
                     BlockEntity blockEntity = null;
                     BlockState blockState;
                     if (blockVisibilityUpdate.isVisible()) {
                        blockState = serverLevel.getBlockState(blockPos);
                        if (blockState.hasBlockEntity()) {
                           blockEntity = serverLevel.getBlockEntity(blockPos);
                        }
                     } else if (environment == Environment.NETHER) {
                        blockState = Blocks.NETHERRACK.defaultBlockState();
                     } else if (environment == Environment.THE_END) {
                        blockState = Blocks.END_STONE.defaultBlockState();
                     } else if (blockPos.getY() < 0) {
                        blockState = Blocks.DEEPSLATE.defaultBlockState();
                     } else {
                        blockState = Blocks.STONE.defaultBlockState();
                     }

                     ClientboundBlockUpdatePacket blockUpdatePacket = new ClientboundBlockUpdatePacket(blockPos, blockState);
                     sendPacketDirect(targetPlayer, blockUpdatePacket);
                     if (blockEntity != null) {
                        Packet updatePacket = blockEntity.getUpdatePacket();
                        if (updatePacket != null) {
                           sendPacketDirect(targetPlayer, updatePacket);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void syncPlayerEyeView(Player targetPlayer, PlayerRayTraceState playerRayTraceState, World world) {
      Location eyeLocation = targetPlayer.getEyeLocation();
      if (eyeLocation.getWorld() != null && eyeLocation.getWorld().equals(world)) {
         playerRayTraceState.setViewSnapshots(NekoAntiXray.createViewSnapshots(new PlayerViewSnapshot(eyeLocation)));
      }
   }

   private static boolean sendPacketDirect(Player targetPlayer, Packet packet) {
      ServerGamePacketListenerImpl packetListener = ((CraftPlayer)targetPlayer).getHandle().connection;
      if (packetListener != null && !packetListener.isDisconnected()) {
         Connection connection = packetListener.connection;
         Channel channel = connection.channel;
         if (channel != null && channel.isOpen()) {
            channel.writeAndFlush(packet);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public void accept(ScheduledTask scheduledTask) {
      this.run();
   }
}
