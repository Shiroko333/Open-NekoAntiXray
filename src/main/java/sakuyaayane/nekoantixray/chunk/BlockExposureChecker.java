package sakuyaayane.nekoantixray.chunk;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class BlockExposureChecker {
   private static final Set<Material> TRANSPARENT_MATERIALS = EnumSet.of(
      Material.AIR,
      Material.CAVE_AIR,
      Material.VOID_AIR,
      Material.WATER,
      Material.LAVA,
      Material.SHORT_GRASS,
      Material.TALL_GRASS,
      Material.FERN,
      Material.LARGE_FERN,
      Material.POPPY,
      Material.DANDELION,
      Material.VINE,
      Material.COBWEB,
      Material.SUGAR_CANE,
      Material.TORCH,
      Material.WALL_TORCH,
      Material.REDSTONE_TORCH,
      Material.REDSTONE_WALL_TORCH,
      Material.SOUL_TORCH,
      Material.SOUL_WALL_TORCH,
      Material.LANTERN,
      Material.SOUL_LANTERN,
      Material.CHAIN,
      Material.OAK_SAPLING,
      Material.SPRUCE_SAPLING,
      Material.BIRCH_SAPLING,
      Material.JUNGLE_SAPLING,
      Material.ACACIA_SAPLING,
      Material.DARK_OAK_SAPLING,
      Material.WHEAT,
      Material.CARROTS,
      Material.POTATOES,
      Material.BEETROOTS,
      Material.NETHER_WART,
      Material.RAIL,
      Material.ACTIVATOR_RAIL,
      Material.DETECTOR_RAIL,
      Material.POWERED_RAIL,
      Material.LEVER,
      Material.STONE_BUTTON,
      Material.OAK_BUTTON,
      Material.TRIPWIRE_HOOK,
      Material.REDSTONE_WIRE,
      Material.TRIPWIRE
   );
   private static final BlockFace[] ADJACENT_FACES = new BlockFace[]{
      BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
   };

   public static boolean isCompletelyBuried(Block block) {
      for (BlockFace face : ADJACENT_FACES) {
         Block relative = block.getRelative(face);
         boolean isTransparent = TRANSPARENT_MATERIALS.contains(relative.getType());
         if (isTransparent || !relative.getType().isOccluding()) {
            return false;
         }
      }

      return true;
   }

   public static int countExposedFaces(Block block) {
      int exposedCount = 0;

      for (BlockFace face : ADJACENT_FACES) {
         Block relative = block.getRelative(face);
         boolean isTransparent = TRANSPARENT_MATERIALS.contains(relative.getType());
         if (isTransparent || !relative.getType().isOccluding()) {
            exposedCount++;
         }
      }

      return exposedCount;
   }
}
