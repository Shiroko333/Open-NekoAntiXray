package sakuyaayane.nekoantixray;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUnloadChunk;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class ChunkPacketListener extends PacketListenerAbstract {
   private final NekoAntiXray plugin;
   private static final long FAWE_REFRESH_GRACE_MS = 5000L;

   public ChunkPacketListener(NekoAntiXray plugin) {
      super(PacketListenerPriority.NORMAL);
      this.plugin = plugin;
   }

   @Override
   public void onPacketSend(PacketSendEvent event) {
      long nowNanos = System.nanoTime();

      try {
         PacketTypeCommon packetType = event.getPacketType();
         if (packetType == Server.CHUNK_DATA) {
            this.handleChunkDataPacket(event);
         } else if (packetType == Server.UNLOAD_CHUNK) {
            this.handleUnloadChunkPacket(event);
         } else if (packetType == Server.RESPAWN) {
            this.handleRespawnPacket(event);
         }
      } finally {
         ModuleTimingStatistics.record("Netty-区块数据包", System.nanoTime() - nowNanos);
      }
   }

   private World getBukkitWorld(Level level) {
      try {
         Method method = level.getClass().getMethod("getWorld");
         return (World)method.invoke(level);
      } catch (Exception ex) {
         return null;
      }
   }

   private void handleChunkDataPacket(PacketSendEvent event) {
      Player player = event.getPlayer();
      WrapperPlayServerChunkData wrapperPlayServerChunkData = new WrapperPlayServerChunkData(event);
      int chunkX = wrapperPlayServerChunkData.getColumn().getX();
      int chunkZ = wrapperPlayServerChunkData.getColumn().getZ();
      long encodedChunkKey = ChunkCoordinateCodec.encode(chunkX, chunkZ);
      ChunkKey chunkKey = new ChunkKey(encodedChunkKey);
      PlayerChunkKey playerChunkKey = new PlayerChunkKey(player.getWorld().getUID(), encodedChunkKey);
      TrackedChunkVisibility trackedChunkVisibility = (TrackedChunkVisibility)this.plugin.getTrackedChunkCache().get(playerChunkKey);
      if (trackedChunkVisibility == null) {
         this.handleUntrackedChunkData(event, chunkX, chunkZ, chunkKey);
      } else {
         LevelChunk chunk = trackedChunkVisibility.getChunk();
         if (chunk != null) {
            World bukkitWorld = this.getBukkitWorld(chunk.getLevel());
            if (bukkitWorld != null) {
               ConcurrentMap playerRayTraceStates = this.plugin.getPlayerRayTraceStates();
               UUID uuid = player.getUniqueId();
               PlayerRayTraceState rayTraceState = this.getOrCreateRayTraceState(playerRayTraceStates, uuid, player, bukkitWorld);
               if (rayTraceState != null) {
                  if (!bukkitWorld.equals(rayTraceState.getViewSnapshots()[0].getWorld())) {
                     Location eyeLocation = player.getEyeLocation();
                     if (!bukkitWorld.equals(eyeLocation.getWorld())) {
                        return;
                     }

                     rayTraceState = this.createRayTraceState(eyeLocation);
                     playerRayTraceStates.put(uuid, rayTraceState);
                  }

                  trackedChunkVisibility = new TrackedChunkVisibility(chunk, new HashMap(trackedChunkVisibility.getBlocks()));
                  rayTraceState.getChunks().put(trackedChunkVisibility.getKey(), trackedChunkVisibility);
               }
            }
         }
      }
   }

   private void handleUntrackedChunkData(PacketSendEvent event, int chunkX, int chunkZ, ChunkKey chunkKey) {
      FaweChunkRefresher faweChunkRefresher = this.plugin.getFaweChunkRefresher();
      Player player = event.getPlayer();
      if (faweChunkRefresher != null) {
         long elapsedSinceEditMs = System.currentTimeMillis() - faweChunkRefresher.getLastPlayerEditTime(player.getUniqueId());
         if (elapsedSinceEditMs > 5000L) {
            PlayerRayTraceState playerRayTraceState = (PlayerRayTraceState)this.plugin.getPlayerRayTraceStates().get(player.getUniqueId());
            if (playerRayTraceState != null && playerRayTraceState.getChunks().containsKey(chunkKey)) {
               faweChunkRefresher.scheduleRefresh(player, chunkX, chunkZ);
            }
         }
      }

      Location eyeLocation = player.getEyeLocation();
      if (eyeLocation.getWorld() != null) {
         ConcurrentMap playerRayTraceStates = this.plugin.getPlayerRayTraceStates();
         UUID uuid = player.getUniqueId();
         PlayerRayTraceState rayTraceState = this.getOrCreateRayTraceState(playerRayTraceStates, uuid, player, eyeLocation.getWorld());
         if (rayTraceState != null && !eyeLocation.getWorld().equals(rayTraceState.getViewSnapshots()[0].getWorld())) {
            rayTraceState = this.createRayTraceState(eyeLocation);
            playerRayTraceStates.put(uuid, rayTraceState);
         }
      }
   }

   private void handleUnloadChunkPacket(PacketSendEvent event) {
      Player player = event.getPlayer();
      PlayerRayTraceState playerRayTraceState = (PlayerRayTraceState)this.plugin.getPlayerRayTraceStates().get(player.getUniqueId());
      if (this.plugin.hasRayTraceState(player, playerRayTraceState)) {
         WrapperPlayServerUnloadChunk wrapperPlayServerUnloadChunk = new WrapperPlayServerUnloadChunk(event);
         ConcurrentMap chunks = playerRayTraceState.getChunks();
         int chunkX = wrapperPlayServerUnloadChunk.getChunkX();
         int chunkZ = wrapperPlayServerUnloadChunk.getChunkZ();
         chunks.remove(new ChunkKey(ChunkCoordinateCodec.encode(chunkX, chunkZ)));
         this.plugin.logDebug("[ChunkDebug] Forget → " + player.getName() + " @ " + chunkX + "," + chunkZ);
      }
   }

   private void handleRespawnPacket(PacketSendEvent event) {
      Player player = event.getPlayer();
      PlayerRayTraceState playerRayTraceState = (PlayerRayTraceState)this.plugin.getPlayerRayTraceStates().get(player.getUniqueId());
      if (this.plugin.hasRayTraceState(player, playerRayTraceState)) {
         playerRayTraceState.getChunks().clear();
      }
   }

   private PlayerRayTraceState getOrCreateRayTraceState(ConcurrentMap playerRayTraceStates, UUID playerId, Player player, World world) {
      PlayerRayTraceState existingState = (PlayerRayTraceState)playerRayTraceStates.get(playerId);
      if (existingState != null) {
         return existingState;
      } else if (!this.plugin.isRealPlayer(player)) {
         return null;
      } else {
         Location eyeLocation = player.getEyeLocation();
         if (eyeLocation.getWorld() != null && eyeLocation.getWorld().equals(world)) {
            PlayerRayTraceState newState = this.createRayTraceState(eyeLocation);
            PlayerRayTraceState previousState = (PlayerRayTraceState)playerRayTraceStates.putIfAbsent(playerId, newState);
            return previousState != null ? previousState : newState;
         } else {
            return null;
         }
      }
   }

   private PlayerRayTraceState createRayTraceState(Location location) {
      PlayerRayTraceState playerRayTraceState = new PlayerRayTraceState(NekoAntiXray.createViewSnapshots(new PlayerViewSnapshot(location)));
      playerRayTraceState.setCallable(new RayTraceWorker(this.plugin, playerRayTraceState));
      return playerRayTraceState;
   }
}
