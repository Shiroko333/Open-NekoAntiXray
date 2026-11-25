package sakuyaayane.nekoantixray;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

public final class RayTracePlayerListener implements Listener {
   private final NekoAntiXray plugin;
   private static final double MIN_MOVE_DISTANCE_SQUARED = 1.0E-4;
   private static final float MIN_ROTATION_DELTA_DEGREES = 0.5F;

   public RayTracePlayerListener(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      if (this.plugin.isRealPlayer(player)) {
         this.resetPlayerState(player, player.getLocation(), "join");
         if (this.plugin.isFoliaServer()) {
            player.getScheduler().runAtFixedRate(this.plugin, new RayTracePlayerUpdateTask(this.plugin, player), null, 1L, this.plugin.getRayTraceUpdateTicks());
         }

         this.plugin.logDebug("Enhanced ray trace player data created for: " + player.getName());
      }
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      this.plugin.getPlayerRayTraceStates().remove(event.getPlayer().getUniqueId());
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPlayerMove(PlayerMoveEvent event) {
      long nowNanos = System.nanoTime();

      try {
         Player player = event.getPlayer();
         PlayerRayTraceState playerRayTraceState = (PlayerRayTraceState)this.plugin.getPlayerRayTraceStates().get(player.getUniqueId());
         if (!this.plugin.hasRayTraceState(player, playerRayTraceState)) {
            return;
         }

         Location from = event.getFrom();
         Location to = event.getTo();
         if (to == null || to.getWorld() == null) {
            return;
         }

         double dx = to.getX() - from.getX();
         double dy = to.getY() - from.getY();
         double dz = to.getZ() - from.getZ();
         double distanceSquared = dx * dx + dy * dy + dz * dz;
         float yawDelta = this.calculateYawDelta(from.getYaw(), to.getYaw());
         float pitchDelta = Math.abs(from.getPitch() - to.getPitch());
         if (!(distanceSquared < 1.0E-4) || !(yawDelta < 0.5F) || !(pitchDelta < 0.5F)) {
            if (to.getWorld().equals(playerRayTraceState.getViewSnapshots()[0].getWorld())) {
               this.updatePlayerView(player, playerRayTraceState, to);
            } else {
               this.resetPlayerState(player, to, "move-world-change");
            }

            return;
         }
      } finally {
         ModuleTimingStatistics.record("主线程-移动检测", System.nanoTime() - nowNanos);
      }
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPlayerTeleport(PlayerTeleportEvent event) {
      Location to = event.getTo();
      if (to != null && to.getWorld() != null) {
         Player player = event.getPlayer();
         PlayerRayTraceState playerRayTraceState = (PlayerRayTraceState)this.plugin.getPlayerRayTraceStates().get(player.getUniqueId());
         if (playerRayTraceState != null && to.getWorld().equals(playerRayTraceState.getViewSnapshots()[0].getWorld())) {
            this.updatePlayerView(player, playerRayTraceState, to);
         } else {
            this.resetPlayerState(player, to, "teleport-" + event.getCause());
         }
      }
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
      Player player = event.getPlayer();
      this.resetPlayerState(player, player.getLocation(), "changed-world");
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onPlayerRespawn(PlayerRespawnEvent event) {
      this.resetPlayerState(event.getPlayer(), event.getRespawnLocation(), "respawn");
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPlayerChunkLoad(PlayerChunkLoadEvent event) {
      long nowNanos = System.nanoTime();

      try {
         Player player = event.getPlayer();
         if (!this.plugin.isRealPlayer(player)) {
            return;
         }

         Chunk bukkitChunk = event.getChunk();
         int chunkX = bukkitChunk.getX();
         int chunkZ = bukkitChunk.getZ();
         long packedCoords = ChunkCoordinateCodec.encode(chunkX, chunkZ);
         PlayerChunkKey playerChunkKey = new PlayerChunkKey(bukkitChunk.getWorld().getUID(), packedCoords);
         TrackedChunkVisibility trackedChunkVisibility = (TrackedChunkVisibility)this.plugin.getTrackedChunkCache().get(playerChunkKey);
         if (trackedChunkVisibility == null) {
            return;
         }

         LevelChunk levelChunk = trackedChunkVisibility.getChunk();
         if (levelChunk == null) {
            return;
         }

         PlayerRayTraceState playerRayTraceState = (PlayerRayTraceState)this.plugin.getPlayerRayTraceStates().get(player.getUniqueId());
         if (playerRayTraceState == null || !bukkitChunk.getWorld().equals(playerRayTraceState.getViewSnapshots()[0].getWorld())) {
            this.resetPlayerState(player, player.getLocation(), "player-chunk-load");
            playerRayTraceState = (PlayerRayTraceState)this.plugin.getPlayerRayTraceStates().get(player.getUniqueId());
         }

         if (playerRayTraceState != null) {
            trackedChunkVisibility = new TrackedChunkVisibility(levelChunk, new HashMap(trackedChunkVisibility.getBlocks()));
            playerRayTraceState.getChunks().put(trackedChunkVisibility.getKey(), trackedChunkVisibility);
            return;
         }
      } finally {
         ModuleTimingStatistics.record("主线程-区块加载", System.nanoTime() - nowNanos);
      }
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPlayerChunkUnload(PlayerChunkUnloadEvent event) {
      PlayerRayTraceState playerRayTraceState = (PlayerRayTraceState)this.plugin.getPlayerRayTraceStates().get(event.getPlayer().getUniqueId());
      if (playerRayTraceState != null) {
         ConcurrentMap chunks = playerRayTraceState.getChunks();
         int chunkX = event.getChunk().getX();
         int chunkZ = event.getChunk().getZ();
         chunks.remove(new ChunkKey(ChunkCoordinateCodec.encode(chunkX, chunkZ)));
      }
   }

   private void resetPlayerState(Player player, Location location, String reason) {
      if (this.plugin.isRealPlayer(player) && location != null && location.getWorld() != null) {
         PlayerRayTraceState previousState = (PlayerRayTraceState)this.plugin.getPlayerRayTraceStates().get(player.getUniqueId());
         PlayerRayTraceState newState = new PlayerRayTraceState(NekoAntiXray.createViewSnapshots(this.createEyeViewSnapshot(player, location)));
         this.copyTrackedChunksForWorld(previousState, newState, location.getWorld());
         newState.setCallable(new RayTraceWorker(this.plugin, newState));
         this.plugin.getPlayerRayTraceStates().put(player.getUniqueId(), newState);
         String playerName = player.getName();
         String worldName = location.getWorld().getName();
         this.plugin.logDebug("Enhanced ray trace player data reset for " + playerName + " (" + reason + ", world=" + worldName + ")");
      }
   }

   private void copyTrackedChunksForWorld(PlayerRayTraceState sourceState, PlayerRayTraceState targetState, World world) {
      if (sourceState != null && world != null) {
         ConcurrentMap<ChunkKey, TrackedChunkVisibility> sourceChunks = sourceState.getChunks();
         for (TrackedChunkVisibility trackedChunk : sourceChunks.values()) {
            LevelChunk chunk = trackedChunk.getChunk();
            if (chunk != null && world.equals(chunk.getLevel().getWorld())) {
               TrackedChunkVisibility copiedChunk = new TrackedChunkVisibility(chunk, new HashMap(trackedChunk.getBlocks()));
               targetState.getChunks().put(copiedChunk.getKey(), copiedChunk);
            }
         }
      }
   }

   private void updatePlayerView(Player player, PlayerRayTraceState playerRayTraceState, Location location) {
      playerRayTraceState.setViewSnapshots(NekoAntiXray.createViewSnapshots(this.createEyeViewSnapshot(player, location)));
   }

   private PlayerViewSnapshot createEyeViewSnapshot(Player player, Location location) {
      PlayerViewSnapshot playerViewSnapshot = new PlayerViewSnapshot(location);
      Vector vector = playerViewSnapshot.getVector();
      vector.setY(vector.getY() + player.getEyeHeight());
      return playerViewSnapshot;
   }

   private float calculateYawDelta(float fromYaw, float toYaw) {
      float delta = Math.abs(fromYaw - toYaw) % 360.0F;
      return delta > 180.0F ? 360.0F - delta : delta;
   }
}
