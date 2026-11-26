package sakuyaayane.nekoantixray.chunk;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World;
import sakuyaayane.nekoantixray.compat.ServerLevelAccess;
import sakuyaayane.nekoantixray.NekoAntiXray;
import sakuyaayane.nekoantixray.raytrace.PlayerRayTraceState;
import sakuyaayane.nekoantixray.world.ChunkCoordinateCodec;
import sakuyaayane.nekoantixray.world.ChunkKey;

public class FaweChunkRefresher {
   private final NekoAntiXray plugin;
   private final int refreshDelayTicks;
   private final long refreshCooldownMs;
   private final int maxChunksPerCycle;
   private final ConcurrentHashMap<UUID, ConcurrentHashMap<Long, Long>> pendingChunkRefreshes = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<UUID, ConcurrentHashMap<Long, Long>> chunkRefreshCooldowns = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<UUID, Long> playerEditTimestamps = new ConcurrentHashMap<>();

   public FaweChunkRefresher(NekoAntiXray plugin) {
      this.plugin = plugin;
      this.refreshDelayTicks = plugin.getConfigManager().getFaweCompatibilityRefreshDelayTicks();
      this.refreshCooldownMs = plugin.getConfigManager().getFaweCompatibilityRefreshCooldownMs();
      this.maxChunksPerCycle = plugin.getConfigManager().getFaweCompatibilityMaxChunksPerCycle();
   }

   private void processRefreshQueue() {
      long nowMs = System.currentTimeMillis();
      long refreshDelayMs = this.refreshDelayTicks * 50L;
      int processedCount = 0;
      ArrayList<FaweChunkRefresher.PendingChunkRefresh> dueRefreshes = new ArrayList<>();

      for (Entry<UUID, ConcurrentHashMap<Long, Long>> playerEntry : this.pendingChunkRefreshes.entrySet()) {
         if (processedCount >= this.maxChunksPerCycle) {
            break;
         }

         UUID playerUuid = playerEntry.getKey();
         ConcurrentHashMap<Long, Long> playerPendingChunks = playerEntry.getValue();
         ConcurrentHashMap<Long, Long> playerCooldowns = this.chunkRefreshCooldowns.computeIfAbsent(playerUuid, ignoredUuid -> new ConcurrentHashMap<>());
         playerCooldowns.entrySet().removeIf(cooldownEntry -> this.isCooldownExpired(nowMs, cooldownEntry));
         Iterator<Entry<Long, Long>> chunkIterator = playerPendingChunks.entrySet().iterator();

         while (true) {
            if (chunkIterator.hasNext()) {
               Entry<Long, Long> chunkEntry = chunkIterator.next();
               if (processedCount < this.maxChunksPerCycle) {
                  long packedChunkKey = chunkEntry.getKey();
                  long queuedAtMs = chunkEntry.getValue();
                  if (nowMs - queuedAtMs < refreshDelayMs || !playerPendingChunks.remove(packedChunkKey, queuedAtMs)) {
                     continue;
                  }

                  Long lastRefreshMs = playerCooldowns.get(packedChunkKey);
                  if (lastRefreshMs == null || nowMs - lastRefreshMs >= this.refreshCooldownMs) {
                     playerCooldowns.put(packedChunkKey, nowMs);
                     dueRefreshes.add(new FaweChunkRefresher.PendingChunkRefresh(playerUuid, packedChunkKey));
                     processedCount++;
                  }
                  continue;
               }
            }

            if (playerPendingChunks.isEmpty()) {
               this.pendingChunkRefreshes.remove(playerUuid, playerPendingChunks);
            }

            if (playerCooldowns.isEmpty()) {
               this.chunkRefreshCooldowns.remove(playerUuid, playerCooldowns);
            }
            break;
         }
      }

      for (FaweChunkRefresher.PendingChunkRefresh pendingChunkRefresh : dueRefreshes) {
         Player player = Bukkit.getPlayer(pendingChunkRefresh.playerUuid);
         if (player != null && player.isOnline()) {
            int chunkX = unpackChunkX(pendingChunkRefresh.chunkKey);
            int chunkZ = unpackChunkZ(pendingChunkRefresh.chunkKey);
            long encodedChunkKey = ChunkCoordinateCodec.encode(chunkX, chunkZ);
            World world = player.getWorld();
            this.plugin
               .getFoliaScheduler()
               .getImpl()
               .runAtLocation(new Location(world, (chunkX << 4) + 8.0, world.getMinHeight() + 1.0, (chunkZ << 4) + 8.0), wrappedTask -> {
                  if (player.isOnline() && player.getWorld().equals(world)) {
                     ServerPlayer serverPlayer = ((CraftPlayer)player).getHandle();
                     ServerLevel serverLevel = ServerLevelAccess.getServerLevel(serverPlayer);
                     LevelChunk chunkIfLoaded = serverLevel.getChunkIfLoaded(chunkX, chunkZ);
                     PlayerRayTraceState playerRayTraceState = (PlayerRayTraceState)this.plugin.getPlayerRayTraceStates().get(player.getUniqueId());
                     if (playerRayTraceState != null) {
                        playerRayTraceState.getChunks().remove(new ChunkKey(encodedChunkKey));
                     }

                     if (chunkIfLoaded != null) {
                        PlayerChunkSender.sendChunk(serverPlayer.connection, serverLevel, chunkIfLoaded);
                     }
                  }
               });
         }
      }

      if (processedCount > 0) {
         this.plugin.logDebug("FAWE chunk refresher processed " + processedCount + " chunks this cycle");
      }
   }

   public void shutdown() {
      this.pendingChunkRefreshes.clear();
      this.chunkRefreshCooldowns.clear();
      this.playerEditTimestamps.clear();
   }

   private boolean isCooldownExpired(long nowMs, Entry entry) {
      return nowMs - (Long)entry.getValue() > this.refreshCooldownMs;
   }

   public void start() {
      this.plugin.getFoliaScheduler().getImpl().runTimer(() -> this.processRefreshQueue(), 10L, 10L);
      this.plugin.logDebug("FAWE chunk refresher started (delay=" + this.refreshDelayTicks + " ticks, cooldown=" + this.refreshCooldownMs + "ms, max=" + this.maxChunksPerCycle + ")");
   }

   public void queueChunkRefresh(Player player, int chunkX, int chunkZ) {
      this.scheduleRefresh(player, chunkX, chunkZ);
   }

   public void recordPlayerEdit(UUID playerUuid) {
      this.playerEditTimestamps.put(playerUuid, System.currentTimeMillis());
   }

   public void clearPlayerData(UUID playerUuid) {
      this.playerEditTimestamps.remove(playerUuid);
      this.pendingChunkRefreshes.remove(playerUuid);
      this.chunkRefreshCooldowns.remove(playerUuid);
   }

   public long getLastPlayerEditTime(UUID playerUuid) {
      return this.playerEditTimestamps.getOrDefault(playerUuid, 0L);
   }

   private static long packChunkCoords(int chunkX, int chunkZ) {
      return (long)chunkX << 32 | chunkZ & 4294967295L;
   }

   private static int unpackChunkX(long packedChunkKey) {
      return (int)(packedChunkKey >> 32);
   }

   private static int unpackChunkZ(long packedChunkKey) {
      return (int)packedChunkKey;
   }

   public void scheduleRefresh(Player player, int chunkX, int chunkZ) {
      if (player != null) {
         UUID playerUuid = player.getUniqueId();
         long packedChunkKey = packChunkCoords(chunkX, chunkZ);
         this.pendingChunkRefreshes.computeIfAbsent(playerUuid, ignoredUuid -> new ConcurrentHashMap<>()).putIfAbsent(packedChunkKey, System.currentTimeMillis());
      }
   }

   private static final class PendingChunkRefresh {
      private final UUID playerUuid;
      private final long chunkKey;

      private PendingChunkRefresh(UUID playerUuid, long chunkKey) {
         this.playerUuid = playerUuid;
         this.chunkKey = chunkKey;
      }
   }
}