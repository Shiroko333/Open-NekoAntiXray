package sakuyaayane.nekoantixray;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.World;

public class WorldSeedResolver {
   public static final int WORLD_SEED_LONGS = 16;
   public static final int WORLD_SEED_BITS = 1024;
   private static final long[] worldSeed = new long[16];
   private static final ThreadLocal<Integer> currentDimension = ThreadLocal.withInitial(() -> 0);
   private static final ConcurrentHashMap<String, WorldSeedResolver.StructureOffset> structureOffsets = new ConcurrentHashMap<>();
   private static final SecureRandom secureRandom = new SecureRandom();
   private static volatile MethodHandle getSeedHandle = null;
   private static volatile MethodHandle dimensionHandle = null;
   private static volatile MethodHandle dimensionLocationHandle = null;
   private static volatile MethodHandle getWorldHandle = null;
   private static volatile MethodHandle getWorldNameHandle = null;
   private static final Lookup methodLookup = MethodHandles.lookup();
   private static final long UNSIGNED_INT_MASK = 4294967295L;

   public static long[] getWorldSeed() {
      return (long[])worldSeed.clone();
   }

   public static int getCurrentDimension() {
      return currentDimension.get();
   }

   public static void clearStructureOffsets() {
      structureOffsets.clear();
   }

   public static String normalizeDimensionKey(String worldName) {
      if (worldName == null || worldName.isEmpty()) {
         return "unknown_world";
      } else if (worldName.equals("world")) {
         return "minecraft:overworld";
      } else if (worldName.equals("world_nether")) {
         return "minecraft:the_nether";
      } else {
         return worldName.equals("world_the_end") ? "minecraft:the_end" : "minecraft:" + worldName;
      }
   }

   public static SecureRandom getSecureRandom() {
      return secureRandom;
   }

   public static void setupWorldSeed(Object level) {
      String dimensionKey = resolveDimensionKey(level);

      try {
         generateRandomSeed();
         int dimensionId = resolveDimensionId(level);
         currentDimension.set(dimensionId);
         computeStructureOffsetIfAbsent(level, dimensionKey);
      } catch (Exception ignored) {
         generateRandomSeed();
      }
   }

   public static long[] generateRandomSeed() {
      long[] seed = new long[16];

      for (int i = 0; i < 16; i++) {
         seed[i] = secureRandom.nextLong();
      }

      System.arraycopy(seed, 0, worldSeed, 0, 16);
      return (long[])seed.clone();
   }

   public static Optional parseSeedString(String seedStr) {
      if (seedStr != null && !seedStr.isEmpty()) {
         try {
            long[] seed = new long[16];
            BigInteger seedValue = new BigInteger(seedStr);
            if (seedValue.signum() < 0) {
               seedValue = seedValue.and(BigInteger.ONE.shiftLeft(1024).subtract(BigInteger.ONE));
            }

            for (int i = 0; i < 16; i++) {
               BigInteger[] divRem = seedValue.divideAndRemainder(BigInteger.ONE.shiftLeft(64));
               seed[i] = divRem[1].longValue();
               seedValue = divRem[0];
            }

            return Optional.of(seed);
         } catch (NumberFormatException ex) {
            return Optional.empty();
         }
      } else {
         return Optional.empty();
      }
   }

   public static String formatSeedToString(long[] seed) {
      if (seed.length != 16) {
         throw new IllegalArgumentException("Invalid seed array length");
      }

      BigInteger combined = BigInteger.ZERO;

      for (int i = 15; i >= 0; i--) {
         BigInteger unsignedPart = BigInteger.valueOf(seed[i]);
         if (unsignedPart.signum() < 0) {
            unsignedPart = unsignedPart.add(BigInteger.ONE.shiftLeft(64));
         }

         combined = combined.shiftLeft(64).add(unsignedPart);
      }

      return combined.toString();
   }

   public static void copyWorldSeed(long[] destination) {
      if (destination.length != 16) {
         throw new IllegalArgumentException("Invalid seed array length");
      }

      System.arraycopy(worldSeed, 0, destination, 0, 16);
   }

   public static void setCurrentDimension(int dimensionId) {
      currentDimension.set(dimensionId);
   }

   private static void computeStructureOffsetIfAbsent(Object level, String dimensionKey) {
      if (!structureOffsets.containsKey(dimensionKey)) {
         long baseSeed = extractWorldSeed(level);
         SecureSeedRandom random = SecureSeedRandom.acquire(
            dimensionKey.hashCode(), (int)(baseSeed & 4294967295L), WorldSeedResolver.SeedSalt.STRUCTURE_OFFSET, 0L
         );
         int offsetX = random.nextInt(2001) - 1000;
         int offsetZ = random.nextInt(2001) - 1000;
         long offsetSalt = random.nextLong();
         random.release();
         structureOffsets.put(dimensionKey, new WorldSeedResolver.StructureOffset(offsetX, offsetZ, offsetSalt));
      }
   }

   public static WorldSeedResolver.StructureOffset getStructureOffset(String dimensionKey) {
      return structureOffsets.get(dimensionKey);
   }

   public static int[] applyStructureOffset(String dimensionKey, int chunkX, int chunkZ) {
      WorldSeedResolver.StructureOffset structureOffset = structureOffsets.get(dimensionKey);
      return structureOffset != null ? new int[]{chunkX + structureOffset.getOffsetX(), chunkZ + structureOffset.getOffsetZ()} : new int[]{chunkX, chunkZ};
   }

   private static int resolveDimensionId(Object level) {
      try {
         String dimensionKey = resolveDimensionKey(level);
         return Math.abs(dimensionKey.hashCode()) % 1000;
      } catch (Exception ex) {
         String fallbackKey = "unknown_" + (level != null ? level.getClass().getSimpleName() : "null");
         return Math.abs(fallbackKey.hashCode()) % 1000;
      }
   }

   private static long extractWorldSeed(Object level) {
      try {
         if (getSeedHandle == null) {
            Method getSeedMethod = level.getClass().getMethod("getSeed");
            getSeedMethod.setAccessible(true);
            getSeedHandle = methodLookup.unreflect(getSeedMethod);
         }

         return (Long)getSeedHandle.invoke((Object)level);
      } catch (Throwable ex) {
         String dimensionKey = resolveDimensionKey(level);
         return Blake2bSeedHasher.hashBytes(dimensionKey.getBytes())[0];
      }
   }

   private static String resolveDimensionKey(Object level) {
      try {
         if (dimensionHandle == null) {
            Method dimensionMethod = level.getClass().getMethod("dimension");
            dimensionMethod.setAccessible(true);
            dimensionHandle = methodLookup.unreflect(dimensionMethod);
         }

         Object dimension = (Object)dimensionHandle.invoke((Object)level);
         if (dimensionLocationHandle == null) {
            Method locationMethod = dimension.getClass().getMethod("location");
            locationMethod.setAccessible(true);
            dimensionLocationHandle = methodLookup.unreflect(locationMethod);
         }

         Object dimensionLocation = (Object)dimensionLocationHandle.invoke((Object)dimension);
         return dimensionLocation.toString();
      } catch (Throwable dimensionError) {
         try {
            if (getWorldHandle == null) {
               Method getWorldMethod = level.getClass().getMethod("getWorld");
               getWorldMethod.setAccessible(true);
               getWorldHandle = methodLookup.unreflect(getWorldMethod);
            }

            Object bukkitWorld = (Object)getWorldHandle.invoke((Object)level);
            if (getWorldNameHandle == null) {
               Method getNameMethod = bukkitWorld.getClass().getMethod("getName");
               getNameMethod.setAccessible(true);
               getWorldNameHandle = methodLookup.unreflect(getNameMethod);
            }

            String worldName = (String)getWorldNameHandle.invoke((Object)bukkitWorld);
            return normalizeDimensionKey(worldName);
         } catch (Throwable fallbackError) {
            return "unknown_" + level.getClass().getSimpleName();
         }
      }
   }

   public static String getDimensionKey(World world) {
      return world == null ? "unknown_world" : normalizeDimensionKey(world.getName());
   }

   public enum SeedSalt {
      UNDEFINED(0),
      BASTION_FEATURE(1),
      WOODLAND_MANSION_FEATURE(2),
      MINESHAFT_FEATURE(3),
      BURIED_TREASURE_FEATURE(4),
      NETHER_FORTRESS_FEATURE(5),
      PILLAGER_OUTPOST_FEATURE(6),
      GEODE_FEATURE(7),
      NETHER_FOSSIL_FEATURE(8),
      OCEAN_MONUMENT_FEATURE(9),
      RUINED_PORTAL_FEATURE(10),
      POTENTIAL_FEATURE(11),
      GENERATE_FEATURE(12),
      JIGSAW_PLACEMENT(13),
      STRONGHOLDS(14),
      POPULATION(15),
      DECORATION(16),
      SLIME_CHUNK(17),
      STRUCTURE_OFFSET(18),
      WORLD_GENERATION(19),
      DESERT_PYRAMID(20),
      JUNGLE_TEMPLE(21),
      SWAMP_HUT(22),
      IGLOO(23),
      SHIPWRECK(24),
      END_CITY(25),
      DUNGEON(26),
      ORE_GENERATION(27),
      LAVA_POOL(28),
      CLAY_PATCH(29),
      BIOME_TEMPERATURE(30),
      BIOME_HUMIDITY(31),
      BIOME_NOISE(32),
      FEATURE_PLACEMENT(33),
      STRUCTURE_ROTATION(34),
      STRUCTURE_INTEGRITY(35);

      private final int value;

      SeedSalt(int saltId) {
         this.value = saltId;
      }

      public int getValue() {
         return this.value;
      }

      private static WorldSeedResolver.SeedSalt[] createValues() {
         return new WorldSeedResolver.SeedSalt[]{
            UNDEFINED,
            BASTION_FEATURE,
            WOODLAND_MANSION_FEATURE,
            MINESHAFT_FEATURE,
            BURIED_TREASURE_FEATURE,
            NETHER_FORTRESS_FEATURE,
            PILLAGER_OUTPOST_FEATURE,
            GEODE_FEATURE,
            NETHER_FOSSIL_FEATURE,
            OCEAN_MONUMENT_FEATURE,
            RUINED_PORTAL_FEATURE,
            POTENTIAL_FEATURE,
            GENERATE_FEATURE,
            JIGSAW_PLACEMENT,
            STRONGHOLDS,
            POPULATION,
            DECORATION,
            SLIME_CHUNK,
            STRUCTURE_OFFSET,
            WORLD_GENERATION,
            DESERT_PYRAMID,
            JUNGLE_TEMPLE,
            SWAMP_HUT,
            IGLOO,
            SHIPWRECK,
            END_CITY,
            DUNGEON,
            ORE_GENERATION,
            LAVA_POOL,
            CLAY_PATCH,
            BIOME_TEMPERATURE,
            BIOME_HUMIDITY,
            BIOME_NOISE,
            FEATURE_PLACEMENT,
            STRUCTURE_ROTATION,
            STRUCTURE_INTEGRITY
         };
      }
   }

   public static class StructureOffset {
      private final int offsetX;
      private final int offsetZ;
      private final long salt;

      public StructureOffset(int offsetX, int offsetZ, long salt) {
         this.offsetX = offsetX;
         this.offsetZ = offsetZ;
         this.salt = salt;
      }

      public int getOffsetX() {
         return this.offsetX;
      }

      public int getOffsetZ() {
         return this.offsetZ;
      }

      public long getSalt() {
         return this.salt;
      }
   }
}
