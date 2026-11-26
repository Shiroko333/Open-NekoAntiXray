package sakuyaayane.nekoantixray.seed;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.World;
import sakuyaayane.nekoantixray.compat.ChunkAccessTransformer;
import sakuyaayane.nekoantixray.NekoAntiXray;
import sakuyaayane.nekoantixray.world.SlimeChunkCache;

public class AsyncChunkSeedProtection implements Listener {
   private static final Map<String, AsyncChunkSeedProtection.WorldProtectionConfig> WORLD_CONFIGS = new ConcurrentHashMap<>();
   private static final Map<String, Boolean> INITIALIZED_WORLDS = new ConcurrentHashMap<>();
   private static final Queue<AsyncChunkSeedProtection.ChunkProtectionTask> PENDING_TASKS = new ConcurrentLinkedQueue<>();
   private static final Set<String> QUEUED_CHUNK_KEYS = ConcurrentHashMap.newKeySet();
   private static final AtomicBoolean PROCESSING_ACTIVE = new AtomicBoolean(false);
   private static int batchSize = 16;
   private static int maxQueueSize = 1024;
   private static volatile boolean configLoaded = false;
   private static final Map<World, String> WORLD_DIMENSION_CACHE = new ConcurrentHashMap<>();
   private static volatile Method getHandleMethod = null;
   private static volatile Method getSeedMethod = null;
   private static volatile Method dimensionMethod = null;
   private static volatile Method locationMethod = null;

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onWorldInit(WorldInitEvent event) {
      NekoAntiXray plugin = NekoAntiXray.getInstance();
      loadConfigIfNeeded(plugin);

      try {
         ChunkAccessTransformer.initialize();
         if (!ChunkAccessTransformer.isCompatible()) {
            return;
         }

         Object serverLevelHandle = getServerLevelHandle(event.getWorld());
         if (serverLevelHandle == null) {
            return;
         }

         String dimensionKey = resolveDimensionKey(serverLevelHandle, event.getWorld());
         if (INITIALIZED_WORLDS.putIfAbsent(dimensionKey, true) != null) {
            return;
         }

         WORLD_DIMENSION_CACHE.put(event.getWorld(), dimensionKey);
         WorldSeedResolver.setupWorldSeed(serverLevelHandle);
         AsyncChunkSeedProtection.WorldProtectionConfig worldProtectionConfig = new AsyncChunkSeedProtection.WorldProtectionConfig(
            dimensionKey, serverLevelHandle
         );
         WORLD_CONFIGS.put(dimensionKey, worldProtectionConfig);
         this.startAsyncProcessing(plugin);
      } catch (Exception ex) {
      }
   }

   @EventHandler(priority = EventPriority.HIGH)
   public void onChunkLoad(ChunkLoadEvent event) {
      if (event.isNewChunk()) {
         try {
            World world = event.getWorld();
            String dimensionKey = WORLD_DIMENSION_CACHE.get(world);
            if (dimensionKey == null) {
               Object serverLevelHandle = getServerLevelHandle(world);
               if (serverLevelHandle == null) {
                  return;
               }

               dimensionKey = resolveDimensionKey(serverLevelHandle, world);
               WORLD_DIMENSION_CACHE.put(world, dimensionKey);
            }

            AsyncChunkSeedProtection.WorldProtectionConfig worldProtectionConfig = WORLD_CONFIGS.get(dimensionKey);
            if (worldProtectionConfig == null) {
               return;
            }

            int chunkX = event.getChunk().getX();
            int chunkZ = event.getChunk().getZ();
            NekoAntiXray plugin = NekoAntiXray.getInstance();
            boolean asyncEnabled = plugin != null && plugin.getConfig().getBoolean("secure-seed.async-chunk-protection", true);
            if (asyncEnabled && PENDING_TASKS.size() < maxQueueSize) {
               String chunkKey = dimensionKey + ":" + chunkX + ":" + chunkZ;
               if (!QUEUED_CHUNK_KEYS.add(chunkKey)) {
                  return;
               }

               PENDING_TASKS.offer(new AsyncChunkSeedProtection.ChunkProtectionTask(dimensionKey, chunkX, chunkZ, worldProtectionConfig, null, world));
               this.startAsyncProcessing(plugin);
            } else {
               this.processChunkSync(chunkX, chunkZ, worldProtectionConfig);
            }
         } catch (Exception ex) {
         }
      }
   }

   private void processChunkSync(int chunkX, int chunkZ, AsyncChunkSeedProtection.WorldProtectionConfig worldProtectionConfig) {
      this.computeAndCacheSlimeChunk(chunkX, chunkZ, worldProtectionConfig);
   }

   private void computeAndCacheSlimeChunk(int chunkX, int chunkZ, AsyncChunkSeedProtection.WorldProtectionConfig worldProtectionConfig) {
      try {
         WorldSeedResolver.StructureOffset structureOffset = worldProtectionConfig.getStructureOffset();
         int shiftedChunkX = chunkX + (structureOffset.getOffsetX() >> 4);
         int shiftedChunkZ = chunkZ + (structureOffset.getOffsetZ() >> 4);
         SecureSeedRandom seedRandom = SecureSeedRandom.acquire(
            shiftedChunkX, shiftedChunkZ, WorldSeedResolver.SeedSalt.GENERATE_FEATURE, structureOffset.getSalt()
         );
         boolean isSlime = seedRandom.nextInt(10) == 0;
         SlimeChunkCache.put(worldProtectionConfig.getWorldName(), chunkX, chunkZ, isSlime);
         seedRandom.release();
      } catch (Exception ex) {
      }
   }

   public static void cleanup() {
      WORLD_CONFIGS.clear();
      INITIALIZED_WORLDS.clear();
      WORLD_DIMENSION_CACHE.clear();
      PENDING_TASKS.clear();
      QUEUED_CHUNK_KEYS.clear();
      SecureSeedRandom.clearCaches();
      SlimeChunkCache.clearAll();
   }

   public static Set getProtectedWorlds() {
      return WORLD_CONFIGS.keySet();
   }

   public static int getPendingTaskCount() {
      return PENDING_TASKS.size();
   }

   private void startAsyncProcessing(NekoAntiXray plugin) {
      if (plugin != null && !PROCESSING_ACTIVE.get() && PROCESSING_ACTIVE.compareAndSet(false, true)) {
         plugin.getFoliaScheduler().getImpl().runAsync(wrappedTask -> {
            processNextBatch();
            PROCESSING_ACTIVE.set(false);
            if (!PENDING_TASKS.isEmpty()) {
               this.startAsyncProcessing(plugin);
            }
         });
      }
   }

   private static void processNextBatch() {
      int processedCount = 0;

      AsyncChunkSeedProtection.ChunkProtectionTask chunkProtectionTask;
      while (processedCount < batchSize && (chunkProtectionTask = PENDING_TASKS.poll()) != null) {
         try {
            processChunkTask(chunkProtectionTask);
            processedCount++;
         } catch (Exception ex) {
         }
      }
   }

   private static void processChunkTask(AsyncChunkSeedProtection.ChunkProtectionTask chunkProtectionTask) {
      try {
         WorldSeedResolver.StructureOffset structureOffset = chunkProtectionTask.config.getStructureOffset();
         int shiftedChunkX = chunkProtectionTask.chunkX + (structureOffset.getOffsetX() >> 4);
         int shiftedChunkZ = chunkProtectionTask.chunkZ + (structureOffset.getOffsetZ() >> 4);
         SecureSeedRandom seedRandom = SecureSeedRandom.acquire(
            shiftedChunkX, shiftedChunkZ, WorldSeedResolver.SeedSalt.GENERATE_FEATURE, structureOffset.getSalt()
         );
         boolean isSlime = seedRandom.nextInt(10) == 0;
         seedRandom.release();
         SlimeChunkCache.put(chunkProtectionTask.worldName, chunkProtectionTask.chunkX, chunkProtectionTask.chunkZ, isSlime);
      } catch (Exception ex) {
      } finally {
         QUEUED_CHUNK_KEYS.remove(chunkProtectionTask.worldName + ":" + chunkProtectionTask.chunkX + ":" + chunkProtectionTask.chunkZ);
      }
   }

   public static int[] applyStructureOffset(String worldName, int chunkX, int chunkZ) {
      AsyncChunkSeedProtection.WorldProtectionConfig worldProtectionConfig = WORLD_CONFIGS.get(worldName);
      if (worldProtectionConfig != null) {
         WorldSeedResolver.StructureOffset structureOffset = worldProtectionConfig.getStructureOffset();
         return new int[]{chunkX + structureOffset.getOffsetX(), chunkZ + structureOffset.getOffsetZ()};
      } else {
         return new int[]{chunkX, chunkZ};
      }
   }

   public static boolean isSlimeChunk(String worldName, int chunkX, int chunkZ) {
      Boolean cachedResult = SlimeChunkCache.get(worldName, chunkX, chunkZ);
      if (cachedResult != null) {
         return cachedResult;
      }

      SecureSeedRandom seedRandom = SecureSeedRandom.forSlimeChunk(chunkX, chunkZ);
      boolean isSlime = seedRandom.nextInt(10) == 0;
      seedRandom.release();
      SlimeChunkCache.put(worldName, chunkX, chunkZ, isSlime);
      return isSlime;
   }

   public static SecureSeedRandom createChunkRandom(String worldName, int chunkX, int chunkZ, WorldSeedResolver.SeedSalt seedSalt) {
      AsyncChunkSeedProtection.WorldProtectionConfig worldProtectionConfig = WORLD_CONFIGS.get(worldName);
      if (worldProtectionConfig != null) {
         int[] offsetCoords = applyStructureOffset(worldName, chunkX, chunkZ);
         return SecureSeedRandom.forFeature(offsetCoords[0], offsetCoords[1], seedSalt, worldProtectionConfig.getStructureOffset().getSalt());
      } else {
         return SecureSeedRandom.forFeature(chunkX, chunkZ, seedSalt, 0L);
      }
   }

   public static AsyncChunkSeedProtection.WorldProtectionConfig getWorldConfig(String worldName) {
      return WORLD_CONFIGS.get(worldName);
   }

   private static Object getServerLevelHandle(World world) {
      try {
         if (getHandleMethod == null) {
            getHandleMethod = world.getClass().getMethod("getHandle");
         }

         return getHandleMethod.invoke(world);
      } catch (Exception ex) {
         try {
            getHandleMethod = world.getClass().getMethod("getHandle");
            return getHandleMethod.invoke(world);
         } catch (Exception retryEx) {
            return null;
         }
      }
   }

   private static long getServerLevelSeed(Object serverLevelHandle) {
      try {
         if (getSeedMethod == null) {
            getSeedMethod = serverLevelHandle.getClass().getMethod("getSeed");
         }

         return (Long)getSeedMethod.invoke(serverLevelHandle);
      } catch (Exception ex) {
         try {
            getSeedMethod = serverLevelHandle.getClass().getMethod("getSeed");
            return (Long)getSeedMethod.invoke(serverLevelHandle);
         } catch (Exception retryEx) {
            return System.currentTimeMillis();
         }
      }
   }

   private static String resolveDimensionKey(Object serverLevelHandle, World world) {
      try {
         if (dimensionMethod == null) {
            dimensionMethod = serverLevelHandle.getClass().getMethod("dimension");
         }

         Object dimensionResourceKey = dimensionMethod.invoke(serverLevelHandle);
         if (locationMethod == null) {
            locationMethod = dimensionResourceKey.getClass().getMethod("location");
         }

         Object dimensionLocation = locationMethod.invoke(dimensionResourceKey);
         return dimensionLocation.toString();
      } catch (Exception ex) {
         String worldName = world.getName();
         if (worldName.equals("world")) {
            return "minecraft:overworld";
         } else if (worldName.equals("world_nether")) {
            return "minecraft:the_nether";
         } else {
            return worldName.equals("world_the_end") ? "minecraft:the_end" : "minecraft:" + worldName;
         }
      }
   }

   private static void loadConfigIfNeeded(NekoAntiXray plugin) {
      if (!configLoaded && plugin != null) {
         try {
            batchSize = plugin.getConfig().getInt("secure-seed.batch-size", 16);
            maxQueueSize = plugin.getConfig().getInt("secure-seed.max-queue-size", 1024);
            SecureSeedRandom.initializeConfig();
            configLoaded = true;
         } catch (Exception ex) {
         }
      }
   }

   private static class ChunkProtectionTask {
      final String worldName;
      final int chunkX;
      final int chunkZ;
      final AsyncChunkSeedProtection.WorldProtectionConfig config;
      final Object nmsChunk;
      final World bukkitWorld;

      ChunkProtectionTask(
         String worldName, int chunkX, int chunkZ, AsyncChunkSeedProtection.WorldProtectionConfig config, Object nmsChunk, World bukkitWorld
      ) {
         this.worldName = worldName;
         this.chunkX = chunkX;
         this.chunkZ = chunkZ;
         this.config = config;
         this.nmsChunk = nmsChunk;
         this.bukkitWorld = bukkitWorld;
      }
   }

   public static class WorldProtectionConfig {
      private final String worldName;
      private final long[] worldSeed;
      private final WorldSeedResolver.StructureOffset structureOffset;
      private final SecureSeedRandom secureSeedRandom;
      private static final String STRUCTURE_OFFSET_SUFFIX = ":structure_offset";

      public WorldProtectionConfig(String worldName, Object serverLevelHandle) {
         this.worldName = worldName;
         this.worldSeed = WorldSeedResolver.generateRandomSeed();
         long serverLevelSeed = AsyncChunkSeedProtection.getServerLevelSeed(serverLevelHandle);
         long offsetSeed = Blake2bSeedHasher.hashBytes((worldName + ":structure_offset").getBytes())[0] ^ serverLevelSeed;
         SecureSeedRandom offsetRandom = SecureSeedRandom.acquire(
            worldName.hashCode(), (int)serverLevelSeed, WorldSeedResolver.SeedSalt.STRUCTURE_OFFSET, offsetSeed
         );
         int offsetX = offsetRandom.nextInt(2001) - 1000;
         int offsetZ = offsetRandom.nextInt(2001) - 1000;
         long salt = offsetRandom.nextLong();
         offsetRandom.release();
         this.structureOffset = new WorldSeedResolver.StructureOffset(offsetX, offsetZ, salt);
         this.secureSeedRandom = new SecureSeedRandom(0, 0, WorldSeedResolver.SeedSalt.WORLD_GENERATION, 0L);
      }

      public String getWorldName() {
         return this.worldName;
      }

      public long[] getWorldSeed() {
         return (long[])this.worldSeed.clone();
      }

      public WorldSeedResolver.StructureOffset getStructureOffset() {
         return this.structureOffset;
      }

      public SecureSeedRandom getSecureSeedRandom() {
         return this.secureSeedRandom;
      }
   }
}
