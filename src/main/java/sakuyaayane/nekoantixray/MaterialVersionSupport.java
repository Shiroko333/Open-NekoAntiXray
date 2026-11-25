package sakuyaayane.nekoantixray;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;

public class MaterialVersionSupport {
   private static final boolean IS_LEGACY_1_12 = Bukkit.getBukkitVersion().startsWith("1.12");

   private static void addMaterialIfPresent(List materials, String materialName) {
      try {
         Material material = Material.valueOf(materialName);
         materials.add(material);
      } catch (IllegalArgumentException ignored) {
      }
   }

   private static boolean isVersionAtLeast(String currentVersion, String minimumVersion) {
      return currentVersion.contains(minimumVersion) || currentVersion.compareTo(minimumVersion) >= 0;
   }

   public static List getSupportedOreMaterials() {
      ArrayList materials = new ArrayList();
      String bukkitVersion = Bukkit.getBukkitVersion();
      addMaterialIfPresent(materials, "DIAMOND_ORE");
      addMaterialIfPresent(materials, "GOLD_ORE");
      addMaterialIfPresent(materials, "IRON_ORE");
      addMaterialIfPresent(materials, "EMERALD_ORE");
      addMaterialIfPresent(materials, "COAL_ORE");
      addMaterialIfPresent(materials, "REDSTONE_ORE");
      addMaterialIfPresent(materials, "LAPIS_ORE");
      if (IS_LEGACY_1_12) {
         addMaterialIfPresent(materials, "QUARTZ_ORE");
      } else {
         addMaterialIfPresent(materials, "NETHER_QUARTZ_ORE");
      }

      if (isVersionAtLeast(bukkitVersion, "1.16")) {
         addMaterialIfPresent(materials, "NETHER_GOLD_ORE");
         addMaterialIfPresent(materials, "ANCIENT_DEBRIS");
         addMaterialIfPresent(materials, "GILDED_BLACKSTONE");
      }

      if (isVersionAtLeast(bukkitVersion, "1.17")) {
         addMaterialIfPresent(materials, "COPPER_ORE");
         addMaterialIfPresent(materials, "DEEPSLATE_DIAMOND_ORE");
         addMaterialIfPresent(materials, "DEEPSLATE_GOLD_ORE");
         addMaterialIfPresent(materials, "DEEPSLATE_IRON_ORE");
         addMaterialIfPresent(materials, "DEEPSLATE_EMERALD_ORE");
         addMaterialIfPresent(materials, "DEEPSLATE_COAL_ORE");
         addMaterialIfPresent(materials, "DEEPSLATE_REDSTONE_ORE");
         addMaterialIfPresent(materials, "DEEPSLATE_LAPIS_ORE");
         addMaterialIfPresent(materials, "DEEPSLATE_COPPER_ORE");
      }

      return materials;
   }
}
