package sakuyaayane.nekoantixray;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataType;

public class SecureSeedStatusFormatter {
   private static final NamespacedKey SECURE_SLIME_CHUNK_KEY = new NamespacedKey("nekoantixray", "secure_slime_chunk");

   public static SecureSeedRandom createStrongholdRandom() {
      return new SecureSeedRandom(0, 0, WorldSeedResolver.SeedSalt.STRONGHOLDS, 0L);
   }

   public static SecureSeedRandom createGeodeRandom(int chunkX, int chunkZ) {
      return new SecureSeedRandom(chunkX, chunkZ, WorldSeedResolver.SeedSalt.GEODE_FEATURE, 0L);
   }

   public static long[] getGlobalWorldSeed() {
      return WorldSeedResolver.getWorldSeed();
   }

   public static long[] hashChunkFeatureSeed(int chunkX, int chunkZ, WorldSeedResolver.SeedSalt seedSalt, long extraSalt) {
      int currentDimension = WorldSeedResolver.getCurrentDimension();
      return Blake2bSeedHasher.hashFeatureSeed(chunkX, chunkZ, currentDimension, seedSalt, extraSalt);
   }

   public static boolean isSlimeChunk(World world, int chunkX, int chunkZ) {
      String dimensionKey = WorldSeedResolver.getDimensionKey(world);
      return AntiSeedCrackerManager.isSlimeChunk(dimensionKey, chunkX, chunkZ);
   }

   public static boolean isChunkSlimeChunk(Chunk chunk) {
      if (chunk.getPersistentDataContainer().has(SECURE_SLIME_CHUNK_KEY, PersistentDataType.BYTE)) {
         byte cachedFlag = chunk.getPersistentDataContainer().get(SECURE_SLIME_CHUNK_KEY, PersistentDataType.BYTE);
         return cachedFlag == 1;
      } else {
         World world = chunk.getWorld();
         int chunkX = chunk.getX();
         int chunkZ = chunk.getZ();
         boolean slimeChunk = isSlimeChunk(world, chunkX, chunkZ);
         chunk.getPersistentDataContainer().set(SECURE_SLIME_CHUNK_KEY, PersistentDataType.BYTE, (byte)(slimeChunk ? 1 : 0));
         return slimeChunk;
      }
   }

   public static int[] getOffsetChunkCoords(World world, int chunkX, int chunkZ) {
      String dimensionKey = WorldSeedResolver.getDimensionKey(world);
      return AntiSeedCrackerManager.applyStructureOffset(dimensionKey, chunkX, chunkZ);
   }

   public static SecureSeedRandom createPopulationRandom(World world, int chunkX, int chunkZ) {
      world.getName();
      return new SecureSeedRandom(chunkX, chunkZ, WorldSeedResolver.SeedSalt.POPULATION, 0L);
   }

   public static SecureSeedRandom createFeatureRandom(World world, int chunkX, int chunkZ, WorldSeedResolver.SeedSalt seedSalt) {
      String dimensionKey = WorldSeedResolver.getDimensionKey(world);
      return AntiSeedCrackerManager.createChunkRandom(dimensionKey, chunkX, chunkZ, seedSalt);
   }

   public static SecureSeedRandom createMineshaftRandom(World world, int chunkX, int chunkZ) {
      String dimensionKey = WorldSeedResolver.getDimensionKey(world);
      return AntiSeedCrackerManager.createChunkRandom(dimensionKey, chunkX, chunkZ, WorldSeedResolver.SeedSalt.MINESHAFT_FEATURE);
   }

   public static SecureSeedRandom createBuriedTreasureRandom(World world, int chunkX, int chunkZ) {
      String dimensionKey = WorldSeedResolver.getDimensionKey(world);
      return AntiSeedCrackerManager.createChunkRandom(dimensionKey, chunkX, chunkZ, WorldSeedResolver.SeedSalt.BURIED_TREASURE_FEATURE);
   }

   public static SecureSeedRandom createPillagerOutpostRandom(World world, int chunkX, int chunkZ) {
      String dimensionKey = WorldSeedResolver.getDimensionKey(world);
      return AntiSeedCrackerManager.createChunkRandom(dimensionKey, chunkX, chunkZ, WorldSeedResolver.SeedSalt.PILLAGER_OUTPOST_FEATURE);
   }

   public static SecureSeedRandom createOceanMonumentRandom(World world, int chunkX, int chunkZ) {
      String dimensionKey = WorldSeedResolver.getDimensionKey(world);
      return AntiSeedCrackerManager.createChunkRandom(dimensionKey, chunkX, chunkZ, WorldSeedResolver.SeedSalt.OCEAN_MONUMENT_FEATURE);
   }

   public static SecureSeedRandom createWoodlandMansionRandom(World world, int chunkX, int chunkZ) {
      String dimensionKey = WorldSeedResolver.getDimensionKey(world);
      return AntiSeedCrackerManager.createChunkRandom(dimensionKey, chunkX, chunkZ, WorldSeedResolver.SeedSalt.WOODLAND_MANSION_FEATURE);
   }

   public static SecureSeedRandom createBastionRandom(World world, int chunkX, int chunkZ) {
      String dimensionKey = WorldSeedResolver.getDimensionKey(world);
      return AntiSeedCrackerManager.createChunkRandom(dimensionKey, chunkX, chunkZ, WorldSeedResolver.SeedSalt.BASTION_FEATURE);
   }

   public static SecureSeedRandom createNetherFortressRandom(World world, int chunkX, int chunkZ) {
      String dimensionKey = WorldSeedResolver.getDimensionKey(world);
      return AntiSeedCrackerManager.createChunkRandom(dimensionKey, chunkX, chunkZ, WorldSeedResolver.SeedSalt.NETHER_FORTRESS_FEATURE);
   }

   public static SecureSeedRandom createRuinedPortalRandom(World world, int chunkX, int chunkZ) {
      String dimensionKey = WorldSeedResolver.getDimensionKey(world);
      return AntiSeedCrackerManager.createChunkRandom(dimensionKey, chunkX, chunkZ, WorldSeedResolver.SeedSalt.RUINED_PORTAL_FEATURE);
   }

   public static String getGlobalSeedString() {
      return WorldSeedResolver.formatSeedToString(WorldSeedResolver.getWorldSeed());
   }

   public static WorldSeedResolver.StructureOffset getStructureOffset(World world) {
      String dimensionKey = WorldSeedResolver.getDimensionKey(world);
      return WorldSeedResolver.getStructureOffset(dimensionKey);
   }

   public static boolean isSecureSystemActive() {
      try {
         long[] worldSeed = WorldSeedResolver.getWorldSeed();
         if (worldSeed != null && worldSeed.length == 16) {
            for (long seedWord : worldSeed) {
               if (seedWord != 0L) {
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      } catch (Exception ex) {
         return false;
      }
   }

   public static String formatSecurityStatus() {
      StringBuilder status = new StringBuilder();
      status.append("§a[SecureAntiSeedCracker Security Status]§r\n");
      status.append("§7- Secure System Active: §").append(isSecureSystemActive() ? "a✓" : "c✗").append("\n");
      status.append("§7- Seed Bits: §b").append(1024).append(" bits\n");
      status.append("§7- Hash Algorithm: §bBLAKE2b\n");
      status.append("§7- RNG Type: §bCryptographically Secure\n");
      status.append("§7- Structure Offsets: §a✓\n");
      if (isSecureSystemActive()) {
         status.append("§7- Current Dimension: §b").append(WorldSeedResolver.getCurrentDimension()).append("\n");
         status.append("§7- Seed Hash: §7").append(getGlobalSeedString().substring(0, Math.min(16, getGlobalSeedString().length()))).append("...");
      }

      return status.toString();
   }

   public static long[] hashBytes(byte[] input) {
      return Blake2bSeedHasher.hashBytes(input);
   }
}