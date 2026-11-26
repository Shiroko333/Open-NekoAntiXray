package sakuyaayane.nekoantixray.world;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PlayerBlockStateCache {
   private final ConcurrentMap<UUID, PlayerBlockStateCache.PlayerShadowStore> playerStores = new ConcurrentHashMap<>();
   private final long entryTtlMs;
   private final int maxPositionsPerPlayer;
   private static final long MILLIS_PER_SECOND = 1000L;

   public void clearAll() {
      this.playerStores.clear();
   }

   public void putBlockState(UUID playerId, String worldName, int blockX, int blockZ, WrappedBlockState blockState) {
      if (playerId != null && blockState != null) {
         this.put(playerId, worldName, blockX, blockZ, blockState.clone(), false);
      }
   }

   public void markBlockEntityHidden(UUID playerId, String worldName, int blockX, int blockZ) {
      if (playerId != null) {
         this.put(playerId, worldName, blockX, blockZ, null, true);
      }
   }

   public PlayerBlockStateCache.ShadowBlockSnapshot getSnapshot(UUID playerId, String worldName, Vector3i position) {
      if (position == null) {
         return null;
      }

      return this.getSnapshot(playerId, worldName, position.getX(), position.getZ());
   }

   public PlayerBlockStateCache.ShadowBlockSnapshot getSnapshot(UUID playerId, String worldName, int blockX, int blockZ) {
      if (playerId == null) {
         return null;
      }

      PlayerBlockStateCache.PlayerShadowStore playerShadowStore = this.playerStores.get(playerId);
      if (playerShadowStore == null) {
         return null;
      }

      long positionKey = packPositionKey(worldName, blockX, blockZ);
      long nowMs = System.currentTimeMillis();
      return playerShadowStore.getSnapshot(positionKey, nowMs);
   }

   public boolean hasBlockState(UUID playerId, String worldName, Vector3i position) {
      PlayerBlockStateCache.ShadowBlockSnapshot snapshot = this.getSnapshot(playerId, worldName, position);
      return snapshot != null && snapshot.getBlockState() != null;
   }

   public boolean isBlockEntityHidden(UUID playerId, String worldName, Vector3i position) {
      PlayerBlockStateCache.ShadowBlockSnapshot snapshot = this.getSnapshot(playerId, worldName, position);
      return snapshot != null && snapshot.isBlockEntityHidden();
   }

   public void clearPlayer(UUID playerId) {
      if (playerId != null) {
         this.playerStores.remove(playerId);
      }
   }

   private void put(UUID playerId, String worldName, int blockX, int blockZ, WrappedBlockState blockState, boolean blockEntityHidden) {
      long positionKey = packPositionKey(worldName, blockX, blockZ);
      long nowMs = System.currentTimeMillis();
      PlayerBlockStateCache.PlayerShadowStore playerShadowStore = this.playerStores
         .computeIfAbsent(playerId, ignored -> new PlayerBlockStateCache.PlayerShadowStore(this.maxPositionsPerPlayer));
      long expiresAtMs = nowMs + this.entryTtlMs;
      playerShadowStore.upsertEntry(positionKey, blockState, blockEntityHidden, expiresAtMs, nowMs);
   }

   private static long packPositionKey(String worldName, int blockX, int blockZ) {
      int worldHash = worldName != null && !worldName.isBlank() ? worldName.trim().toLowerCase(Locale.ROOT).hashCode() : 0;
      return (long)worldHash << 32 | (blockX & 65535) << 16 | blockZ & 65535;
   }

   public PlayerBlockStateCache(long entryTtlSeconds, int maxPositionsPerPlayer) {
      this.entryTtlMs = Math.max(1L, entryTtlSeconds) * 1000L;
      this.maxPositionsPerPlayer = Math.max(1024, maxPositionsPerPlayer);
   }

   private static final class PlayerShadowStore {
      private final int maxEntries;
      private final LinkedHashMap<Long, PlayerBlockStateCache.ShadowBlockEntry> entries = new LinkedHashMap<>(1024, 0.75F, true);
      private long lastCleanupTimeMs;
      private static final long CLEANUP_INTERVAL_MS = 30000L;

      public synchronized PlayerBlockStateCache.ShadowBlockSnapshot getSnapshot(long positionKey, long nowMs) {
         PlayerBlockStateCache.ShadowBlockEntry shadowBlockEntry = this.entries.get(positionKey);
         if (shadowBlockEntry == null) {
            return null;
         } else if (shadowBlockEntry.expiresAtMs <= nowMs) {
            this.entries.remove(positionKey);
            return null;
         } else {
            WrappedBlockState clonedBlockState = shadowBlockEntry.blockState != null ? shadowBlockEntry.blockState.clone() : null;
            return new PlayerBlockStateCache.ShadowBlockSnapshot(clonedBlockState, shadowBlockEntry.blockEntityHidden);
         }
      }

      private void purgeExpiredEntries(long nowMs) {
         Iterator<Entry<Long, PlayerBlockStateCache.ShadowBlockEntry>> iterator = this.entries.entrySet().iterator();

         while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAtMs <= nowMs) {
               iterator.remove();
            }
         }
      }

      private void evictExcessEntries() {
         Iterator<Long> iterator = this.entries.keySet().iterator();

         while (this.entries.size() > this.maxEntries && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
         }
      }

      private PlayerShadowStore(int maxEntries) {
         this.maxEntries = maxEntries;
      }

      public synchronized void upsertEntry(long positionKey, WrappedBlockState blockState, boolean blockEntityHidden, long expiresAtMs, long nowMs) {
         if (nowMs - this.lastCleanupTimeMs > 30000L) {
            this.purgeExpiredEntries(nowMs);
            this.lastCleanupTimeMs = nowMs;
         }

         PlayerBlockStateCache.ShadowBlockEntry shadowBlockEntry = this.entries.get(positionKey);
         if (shadowBlockEntry != null) {
            if (blockState != null) {
               shadowBlockEntry.blockState = blockState;
            }

            shadowBlockEntry.blockEntityHidden |= blockEntityHidden;
            shadowBlockEntry.expiresAtMs = Math.max(shadowBlockEntry.expiresAtMs, expiresAtMs);
         } else {
            this.entries.put(positionKey, new PlayerBlockStateCache.ShadowBlockEntry(blockState, blockEntityHidden, expiresAtMs));
         }

         this.evictExcessEntries();
      }
   }

   private static final class ShadowBlockEntry {
      private WrappedBlockState blockState;
      private boolean blockEntityHidden;
      private long expiresAtMs;

      private ShadowBlockEntry(WrappedBlockState blockState, boolean blockEntityHidden, long expiresAtMs) {
         this.blockState = blockState;
         this.blockEntityHidden = blockEntityHidden;
         this.expiresAtMs = expiresAtMs;
      }
   }

   public record ShadowBlockSnapshot(WrappedBlockState blockState, boolean blockEntityHidden) {
      public WrappedBlockState getBlockState() {
         return this.blockState;
      }

      public boolean isBlockEntityHidden() {
         return this.blockEntityHidden;
      }
   }
}
