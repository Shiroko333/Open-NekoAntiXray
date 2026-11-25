package sakuyaayane.nekoantixray;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class SlimeChunkCache {
   private static final ConcurrentHashMap<String, ConcurrentHashMap<Long, Boolean>> WORLD_SLIME_CACHE = new ConcurrentHashMap<>();
   private static int maxEntriesPerWorld = 100000;
   private static final long INT_UNSIGNED_MASK = 4294967295L;

   public static void clearAll() {
      WORLD_SLIME_CACHE.clear();
   }

   private static long packChunkCoords(int chunkX, int chunkZ) {
      return (long)chunkX << 32 | chunkZ & 4294967295L;
   }

   public static void cacheSlimeChunk(String worldName, int chunkX, int chunkZ, boolean isSlimeChunk) {
      put(worldName, chunkX, chunkZ, isSlimeChunk);
   }

   public static void clearWorld(String worldName) {
      WORLD_SLIME_CACHE.remove(worldName);
   }

   public static String getStatsSummary() {
      StringBuilder summary = new StringBuilder("SlimeChunkCache Stats:\n");
      int totalEntries = 0;

      for (Entry<String, ConcurrentHashMap<Long, Boolean>> entry : WORLD_SLIME_CACHE.entrySet()) {
         int worldEntries = entry.getValue().size();
         totalEntries += worldEntries;
         summary.append("  ").append(entry.getKey()).append(": ").append(worldEntries).append(" entries\n");
      }

      summary.append("  Total: ").append(totalEntries).append(" entries across ").append(WORLD_SLIME_CACHE.size()).append(" worlds");
      return summary.toString();
   }

   public static void setMaxEntriesPerWorld(int maxEntries) {
      maxEntriesPerWorld = maxEntries;
   }

   public static int getWorldCacheSize(String worldName) {
      ConcurrentHashMap<Long, Boolean> worldCache = WORLD_SLIME_CACHE.get(worldName);
      return worldCache != null ? worldCache.size() : 0;
   }

   public static void put(String worldName, int chunkX, int chunkZ, boolean isSlimeChunk) {
      ConcurrentHashMap<Long, Boolean> worldCache = WORLD_SLIME_CACHE.computeIfAbsent(worldName, ignored -> new ConcurrentHashMap<>());
      if (worldCache.size() >= maxEntriesPerWorld) {
         int toEvict = maxEntriesPerWorld / 4;
         Iterator<Long> iterator = worldCache.keySet().iterator();

         while (toEvict-- > 0 && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
         }
      }

      worldCache.put(packChunkCoords(chunkX, chunkZ), isSlimeChunk);
   }

   public static Boolean get(String worldName, int chunkX, int chunkZ) {
      ConcurrentHashMap<Long, Boolean> worldCache = WORLD_SLIME_CACHE.get(worldName);
      return worldCache == null ? null : worldCache.get(packChunkCoords(chunkX, chunkZ));
   }

   public static boolean contains(String worldName, int chunkX, int chunkZ) {
      ConcurrentHashMap<Long, Boolean> worldCache = WORLD_SLIME_CACHE.get(worldName);
      return worldCache != null && worldCache.containsKey(packChunkCoords(chunkX, chunkZ));
   }
}
