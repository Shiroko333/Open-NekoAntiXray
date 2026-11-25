package sakuyaayane.nekoantixray;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class OreMaterialMatcher {
   private static final Set<Material> ORE_MATERIALS = EnumSet.noneOf(Material.class);
   private static final Set<Material> EXPOSING_MATERIALS = EnumSet.noneOf(Material.class);
   private static final BlockFace[] CARDINAL_FACES = new BlockFace[]{
      BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
   };

   private static void initializeExposingMaterials() {
      EXPOSING_MATERIALS.add(Material.AIR);
      EXPOSING_MATERIALS.add(Material.CAVE_AIR);
      EXPOSING_MATERIALS.add(Material.VOID_AIR);
      EXPOSING_MATERIALS.add(Material.WATER);
      EXPOSING_MATERIALS.add(Material.LAVA);
   }

   private static void initializeOreMaterials() {
      for (Material material : Material.values()) {
         String materialName = material.name();
         if (materialName.endsWith("_ORE")
            || materialName.equals("ANCIENT_DEBRIS")
            || materialName.equals("NETHER_QUARTZ_ORE")
            || materialName.equals("NETHER_GOLD_ORE")
            || materialName.equals("GILDED_BLACKSTONE")
            || materialName.equals("AMETHYST_CLUSTER")) {
            ORE_MATERIALS.add(material);
         }
      }
   }

   public static boolean isOreMaterial(Material material) {
      return ORE_MATERIALS.contains(material);
   }

   public static boolean isOreBlock(Block block) {
      return isOreMaterial(block.getType());
   }

   public static boolean isExposedToAirOrLiquid(Block block, int maxExposedFaces) {
      int exposedFaces = 0;

      for (BlockFace face : CARDINAL_FACES) {
         if (EXPOSING_MATERIALS.contains(block.getRelative(face).getType())) {
            if (++exposedFaces > maxExposedFaces) {
               return true;
            }
         }
      }

      return false;
   }

   static {
      initializeOreMaterials();
      initializeExposingMaterials();
   }
}
