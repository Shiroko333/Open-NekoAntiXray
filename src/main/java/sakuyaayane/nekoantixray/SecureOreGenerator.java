package sakuyaayane.nekoantixray;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class SecureOreGenerator {
   private static final Set<Material> PROTECTED_ORE_MATERIALS = Set.of(
      Material.DIAMOND_ORE,
      Material.DEEPSLATE_DIAMOND_ORE,
      Material.EMERALD_ORE,
      Material.DEEPSLATE_EMERALD_ORE,
      Material.ANCIENT_DEBRIS,
      Material.GOLD_ORE,
      Material.DEEPSLATE_GOLD_ORE,
      Material.LAPIS_ORE,
      Material.DEEPSLATE_LAPIS_ORE
   );
   private static final Map<String, SecureSeedRandom> WORLD_RANDOMIZERS = new ConcurrentHashMap<>();

   public static void initializeWorld(String worldName, long worldSeed) {
      NekoAntiXray plugin = NekoAntiXray.getInstance();
      if (plugin != null) {
         plugin.logDebug("[SecureOreGeneration] Initializing secure ore generation for world: " + worldName);
      }

      long derivedSeed = Blake2bSeedHasher.hashBytes((worldName + ":ore_generation").getBytes())[0] ^ worldSeed;
      SecureSeedRandom randomizer = new SecureSeedRandom(worldName.hashCode(), (int)worldSeed, WorldSeedResolver.SeedSalt.ORE_GENERATION, derivedSeed);
      WORLD_RANDOMIZERS.put(worldName, randomizer);
      if (plugin != null) {
         plugin.logDebug("[SecureOreGeneration] Ore generation randomizer created for world: " + worldName);
      }
   }

   public static void generateChunkOres(String worldName, int chunkX, int chunkZ, World world) {
      SecureSeedRandom randomizer = WORLD_RANDOMIZERS.get(worldName);
      if (randomizer != null) {
         NekoAntiXray plugin = NekoAntiXray.getInstance();
         long chunkSeed = Blake2bSeedHasher.hashBytes((worldName + ":ore:" + chunkX + "," + chunkZ).getBytes())[0];
         randomizer.reseed(chunkX, chunkZ, WorldSeedResolver.SeedSalt.ORE_GENERATION, chunkSeed);
         if (plugin != null) {
            plugin.logDebug("[SecureOreGeneration] Generating secure ores for chunk (" + chunkX + "," + chunkZ + ") in " + worldName);
         }

         for (Material material : PROTECTED_ORE_MATERIALS) {
            generateOreVeins(world, chunkX, chunkZ, material, randomizer);
         }
      }
   }

   private static void generateOreVeins(World world, int chunkX, int chunkZ, Material material, SecureSeedRandom randomizer) {
      NekoAntiXray plugin = NekoAntiXray.getInstance();
      SecureOreGenerator.OreGenerationConfig oreConfig = getOreConfig(material);
      int veinRange = oreConfig.maxVeinsPerChunk - oreConfig.minVeinsPerChunk + 1;
      int veinCount = oreConfig.minVeinsPerChunk + randomizer.nextInt(veinRange);
      if (plugin != null) {
         plugin.logDebug("[SecureOreGeneration] Generating " + veinCount + " veins of " + String.valueOf(material) + " in chunk (" + chunkX + "," + chunkZ + ")");
      }

      for (int i = 0; i < veinCount; i++) {
         int blockX = (chunkX << 4) + randomizer.nextInt(16);
         int blockZ = (chunkZ << 4) + randomizer.nextInt(16);
         int yRange = oreConfig.maxY - oreConfig.minY + 1;
         int blockY = oreConfig.minY + randomizer.nextInt(yRange);
         int sizeRange = oreConfig.maxVeinSize - oreConfig.minVeinSize + 1;
         int veinSize = oreConfig.minVeinSize + randomizer.nextInt(sizeRange);
         placeOreVein(world, blockX, blockY, blockZ, material, veinSize, randomizer);
      }
   }

   private static void placeOreVein(World world, int originX, int originY, int originZ, Material material, int veinSize, SecureSeedRandom randomizer) {
      NekoAntiXray plugin = NekoAntiXray.getInstance();
      int placedCount = 0;
      int attempts = 0;

      for (int maxAttempts = veinSize * 3; placedCount < veinSize && attempts < maxAttempts; attempts++) {
         int offsetX = randomizer.nextInt(9) - 4;
         int offsetY = randomizer.nextInt(7) - 3;
         int offsetZ = randomizer.nextInt(9) - 4;
         int targetX = originX + offsetX;
         int targetY = originY + offsetY;
         int targetZ = originZ + offsetZ;
         if (targetY >= world.getMinHeight() && targetY <= world.getMaxHeight()) {
            Block blockAt = world.getBlockAt(targetX, targetY, targetZ);
            if (isReplaceableHostBlock(blockAt.getType()) && randomizer.nextFloat() > 0.3F && plugin != null) {
               if (++placedCount == 1) {
                  plugin.logDebug("[SecureOreGeneration] Placed " + String.valueOf(material) + " vein at (" + targetX + "," + targetY + "," + targetZ + ")");
               }
            }
         }
      }
   }

   private static boolean isReplaceableHostBlock(Material material) {
      return material == Material.STONE
         || material == Material.DEEPSLATE
         || material == Material.GRANITE
         || material == Material.DIORITE
         || material == Material.ANDESITE
         || material == Material.TUFF
         || material == Material.NETHERRACK;
   }

   private static SecureOreGenerator.OreGenerationConfig getOreConfig(Material material) {
      return switch (material) {
         case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> new SecureOreGenerator.OreGenerationConfig(1, 3, -64, 16, 1, 8);
         case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> new SecureOreGenerator.OreGenerationConfig(0, 2, -16, 320, 1, 3);
         case ANCIENT_DEBRIS -> new SecureOreGenerator.OreGenerationConfig(1, 2, 8, 119, 1, 3);
         case GOLD_ORE, DEEPSLATE_GOLD_ORE -> new SecureOreGenerator.OreGenerationConfig(2, 6, -64, 32, 1, 9);
         case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> new SecureOreGenerator.OreGenerationConfig(1, 4, -64, 64, 1, 7);
         default -> new SecureOreGenerator.OreGenerationConfig(1, 3, -64, 64, 1, 6);
      };
   }

   public static boolean shouldRandomizeOre(String worldName, int blockX, int blockY, int blockZ, Material material) {
      if (!PROTECTED_ORE_MATERIALS.contains(material)) {
         return false;
      }

      SecureSeedRandom randomizer = WORLD_RANDOMIZERS.get(worldName);
      if (randomizer == null) {
         return false;
      }

      randomizer.reseed(blockX, blockZ, WorldSeedResolver.SeedSalt.ORE_GENERATION, blockY);
      return randomizer.nextFloat() < 0.2F;
   }

   public static Map getStatistics() {
      Map<String, Object> statistics = new ConcurrentHashMap<>();
      statistics.put("protectedWorlds", WORLD_RANDOMIZERS.size());
      statistics.put("protectedOreTypes", PROTECTED_ORE_MATERIALS.size());
      statistics.put("oreTypes", PROTECTED_ORE_MATERIALS.stream().map(Material::name).toArray());
      return statistics;
   }

   public static void clearWorld(String worldName) {
      WORLD_RANDOMIZERS.remove(worldName);
      NekoAntiXray plugin = NekoAntiXray.getInstance();
      if (plugin != null) {
         plugin.logDebug("[SecureOreGeneration] Cleared ore protection for world: " + worldName);
      }
   }

   public static void clearAll() {
      WORLD_RANDOMIZERS.clear();
      NekoAntiXray plugin = NekoAntiXray.getInstance();
      if (plugin != null) {
         plugin.logDebug("[SecureOreGeneration] Cleared all ore protection data");
      }
   }

   public static final class MaterialSwitchMap {
      static final int[] MATERIAL_ORDINAL_MAP = new int[Material.values().length];

      static {
         try {
            MATERIAL_ORDINAL_MAP[Material.DIAMOND_ORE.ordinal()] = 1;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINAL_MAP[Material.DEEPSLATE_DIAMOND_ORE.ordinal()] = 2;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINAL_MAP[Material.EMERALD_ORE.ordinal()] = 3;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINAL_MAP[Material.DEEPSLATE_EMERALD_ORE.ordinal()] = 4;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINAL_MAP[Material.ANCIENT_DEBRIS.ordinal()] = 5;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINAL_MAP[Material.GOLD_ORE.ordinal()] = 6;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINAL_MAP[Material.DEEPSLATE_GOLD_ORE.ordinal()] = 7;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINAL_MAP[Material.LAPIS_ORE.ordinal()] = 8;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINAL_MAP[Material.DEEPSLATE_LAPIS_ORE.ordinal()] = 9;
         } catch (NoSuchFieldError ignored) {
         }
      }
   }

   private static class OreGenerationConfig {
      final int minVeinsPerChunk;
      final int maxVeinsPerChunk;
      final int minY;
      final int maxY;
      final int minVeinSize;
      final int maxVeinSize;

      OreGenerationConfig(int minVeinsPerChunk, int maxVeinsPerChunk, int minY, int maxY, int minVeinSize, int maxVeinSize) {
         this.minVeinsPerChunk = minVeinsPerChunk;
         this.maxVeinsPerChunk = maxVeinsPerChunk;
         this.minY = minY;
         this.maxY = maxY;
         this.minVeinSize = minVeinSize;
         this.maxVeinSize = maxVeinSize;
      }
   }
}
