package sakuyaayane.nekoantixray;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SecureSeedRandom {
   private static int hashCacheSize = 4096;
   private static final ConcurrentHashMap<Long, long[]> HASH_CACHE = new ConcurrentHashMap<>(hashCacheSize);
   private static volatile int cacheWriteCounter = 0;
   private static int cacheCleanupThreshold = hashCacheSize * 2;
   private static int maxPoolSize = 64;
   private static final ConcurrentLinkedQueue<SecureSeedRandom> INSTANCE_POOL = new ConcurrentLinkedQueue<>();
   private static volatile boolean configInitialized = false;
   private static final long[] EMPTY_SEED_CHAIN_VALUE = Blake2bSeedHasher.hashWorldSeed(new long[16]);
   private static final ThreadLocal<long[]> CACHED_WORLD_SEED = ThreadLocal.withInitial(() -> new long[16]);
   private static final ThreadLocal<long[]> CACHED_CHAIN_VALUE = ThreadLocal.withInitial(() -> (long[])EMPTY_SEED_CHAIN_VALUE.clone());
   private static volatile Boolean cachedDebugEnabled = null;
   private static volatile long lastDebugCheckTimeMs = 0L;
   private static final long DEBUG_CHECK_INTERVAL_MS = 5000L;
   private final long[] worldSeed;
   private final long[] randomBitsBuffer;
   private int bitPosition;
   private static final int BUFFER_BIT_CAPACITY = 512;
   private long blockCounter;
   private final long[] message;
   private final long[] hashInternalState;
   private boolean fromPool = false;

   public SecureSeedRandom(int chunkX, int chunkZ, WorldSeedResolver.SeedSalt seedSalt, long extraSalt) {
      this.worldSeed = new long[16];
      this.randomBitsBuffer = new long[8];
      this.message = new long[16];
      this.hashInternalState = new long[16];
      this.fromPool = false;
      if (seedSalt != null) {
         this.reseed(chunkX, chunkZ, seedSalt, extraSalt);
      }
   }

   private SecureSeedRandom() {
      this.worldSeed = new long[16];
      this.randomBitsBuffer = new long[8];
      this.message = new long[16];
      this.hashInternalState = new long[16];
   }

   public SecureSeedRandom(long chunkX, long chunkZ, WorldSeedResolver.SeedSalt seedSalt, long extraSalt) {
      this((int)chunkX, (int)chunkZ, seedSalt, extraSalt);
   }

   public static void clearCaches() {
      HASH_CACHE.clear();
      INSTANCE_POOL.clear();
      cacheWriteCounter = 0;
      configInitialized = false;
   }

   public static SecureSeedRandom acquire(int chunkX, int chunkZ, WorldSeedResolver.SeedSalt seedSalt, long extraSalt) {
      SecureSeedRandom secureSeedRandom = INSTANCE_POOL.poll();
      if (secureSeedRandom == null) {
         secureSeedRandom = new SecureSeedRandom();
      }

      secureSeedRandom.fromPool = true;
      secureSeedRandom.reseed(chunkX, chunkZ, seedSalt, extraSalt);
      return secureSeedRandom;
   }

   public void release() {
      if (this.fromPool && INSTANCE_POOL.size() < maxPoolSize) {
         this.blockCounter = 0L;
         this.bitPosition = 512;
         INSTANCE_POOL.offer(this);
      }

      this.fromPool = false;
   }

   private static boolean isDebugEnabled() {
      long nowMs = System.currentTimeMillis();
      if (cachedDebugEnabled == null || nowMs - lastDebugCheckTimeMs > 5000L) {
         NekoAntiXray plugin = NekoAntiXray.getInstance();
         cachedDebugEnabled = plugin != null && plugin.getConfigManager() != null && plugin.getConfigManager().isDebug();
         lastDebugCheckTimeMs = nowMs;
      }

      return cachedDebugEnabled;
   }

   public void reseed(int chunkX, int chunkZ, WorldSeedResolver.SeedSalt seedSalt, long extraSalt) {
      WorldSeedResolver.copyWorldSeed(this.worldSeed);
      this.message[0] = (long)chunkX << 32 | chunkZ & 4294967295L;
      this.message[1] = (long)WorldSeedResolver.getCurrentDimension() << 32 | extraSalt & 4294967295L;
      this.message[2] = seedSalt.getValue();
      this.message[3] = this.blockCounter = 0L;

      for (int i = 4; i < 16; i++) {
         this.message[i] = 0L;
      }

      this.bitPosition = 512;
   }

   private long[] getPrehashedChainValue() {
      long[] cachedSeed = CACHED_WORLD_SEED.get();
      if (!Arrays.equals(this.worldSeed, cachedSeed)) {
         CACHED_CHAIN_VALUE.set(Blake2bSeedHasher.hashWorldSeed(this.worldSeed));
         System.arraycopy(this.worldSeed, 0, cachedSeed, 0, 16);
      }

      return CACHED_CHAIN_VALUE.get();
   }

   private long packCacheKey(int chunkX, int chunkZ, int saltId) {
      return (long)chunkX << 40 | (long)chunkZ << 16 | saltId & 65535;
   }

   private void refillRandomBuffer() {
      this.message[3] = this.blockCounter++;
      int chunkX = (int)(this.message[0] >> 32);
      int chunkZ = (int)this.message[0];
      int saltId = (int)this.message[2];
      long baseCacheKey = this.packCacheKey(chunkX, chunkZ, saltId);
      long cacheKey = baseCacheKey ^ this.blockCounter;
      long[] cachedBlock = HASH_CACHE.get(cacheKey);
      if (cachedBlock != null) {
         System.arraycopy(cachedBlock, 0, this.randomBitsBuffer, 0, 8);
      } else {
         System.arraycopy(this.getPrehashedChainValue(), 0, this.randomBitsBuffer, 0, 8);
         Blake2bSeedHasher.compressBlock(this.message, this.randomBitsBuffer, this.hashInternalState, 64L, true);
         this.cacheHashResult(cacheKey, this.randomBitsBuffer);
      }
   }

   private void cacheHashResult(long cacheKey, long[] hashBlock) {
      if (++cacheWriteCounter > cacheCleanupThreshold) {
         synchronized (HASH_CACHE) {
            if (cacheWriteCounter > cacheCleanupThreshold) {
               if (HASH_CACHE.size() > hashCacheSize) {
                  int entriesToEvict = HASH_CACHE.size() / 2;
                  Iterator<Long> iterator = HASH_CACHE.keySet().iterator();

                  while (entriesToEvict-- > 0 && iterator.hasNext()) {
                     iterator.next();
                     iterator.remove();
                  }
               }

               cacheWriteCounter = 0;
            }
         }
      }

      if (HASH_CACHE.size() < hashCacheSize) {
         HASH_CACHE.put(cacheKey, (long[])hashBlock.clone());
      }
   }

   private long nextBits(int bitCount) {
      if (bitCount > 64) {
         throw new IllegalArgumentException("Bit count must be between 1 and 64");
      }

      if (this.bitPosition >= 512) {
         this.refillRandomBuffer();
         this.bitPosition = 0;
      }

      int bitOffset = this.bitPosition & 63;
      int wordIndex = this.bitPosition >>> 6;
      if (this.bitPosition >>> 6 == this.bitPosition + bitCount - 1 >>> 6) {
         long extractedBits = this.randomBitsBuffer[wordIndex] >>> bitOffset & (1L << bitCount) - 1L;
         this.bitPosition += bitCount;
         return extractedBits;
      }

      int lowBitCount = 64 - bitOffset;
      long resultBits = this.randomBitsBuffer[wordIndex] >>> bitOffset & (1L << lowBitCount) - 1L;
      this.bitPosition += bitCount;
      if (this.bitPosition >= 512) {
         this.refillRandomBuffer();
         this.bitPosition -= 512;
      }

      int remainingBits = bitCount - lowBitCount;
      if (remainingBits > 0) {
         wordIndex = this.bitPosition >>> 6;
         long highBits = this.randomBitsBuffer[wordIndex] & (1L << remainingBits) - 1L;
         resultBits |= highBits << lowBitCount;
      }

      return resultBits;
   }

   public int nextIntBits(int bitCount) {
      if (bitCount > 0 && bitCount <= 32) {
         return (int)this.nextBits(bitCount);
      } else {
         throw new IllegalArgumentException("Bits must be between 1 and 32");
      }
   }

   public int nextInt(int bound) {
      int bitCount = 32 - Integer.numberOfLeadingZeros(bound - 1);

      int candidate;
      do {
         candidate = this.nextIntBits(bitCount);
      } while (candidate >= bound);

      return candidate;
   }

   public long nextLong() {
      return this.nextBits(64);
   }

   public double nextDouble() {
      return (float)this.nextBits(53) * 1.110223E-16F;
   }

   public float nextFloat() {
      return (float)this.nextBits(24) * 5.9604645E-8F;
   }

   public boolean nextBoolean() {
      return this.nextBits(1) != 0L;
   }

   public static SecureSeedRandom forSlimeChunk(int chunkX, int chunkZ) {
      return acquire(chunkX, chunkZ, WorldSeedResolver.SeedSalt.SLIME_CHUNK, 0L);
   }

   public static SecureSeedRandom forFeature(int chunkX, int chunkZ, WorldSeedResolver.SeedSalt seedSalt, long extraSalt) {
      return acquire(chunkX, chunkZ, seedSalt, extraSalt);
   }

   public static void initializeConfig() {
      if (!configInitialized) {
         try {
            NekoAntiXray plugin = NekoAntiXray.getInstance();
            if (plugin != null && plugin.getConfig() != null) {
               hashCacheSize = plugin.getConfig().getInt("secure-seed.hash-cache-size", 4096);
               cacheCleanupThreshold = hashCacheSize * 2;
               maxPoolSize = plugin.getConfig().getInt("secure-seed.random-pool-size", 64);
               configInitialized = true;
            }
         } catch (Exception ignored) {
         }
      }
   }

   public static String getPoolAndCacheStats() {
      return String.format("HashCache: %d/%d, Pool: %d/%d", HASH_CACHE.size(), hashCacheSize, INSTANCE_POOL.size(), maxPoolSize);
   }
}
