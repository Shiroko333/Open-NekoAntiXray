package sakuyaayane.nekoantixray.stats;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustTransition;
import org.bukkit.entity.Player;

public class OreParticleRenderer {
   private static final Map<Material, Color> ORE_COLORS = new HashMap<>();

   public static void spawnTrailParticle(Player player, Location location) {
      DustTransition dustTransition = new DustTransition(Color.WHITE, Color.WHITE, 0.5F);
      player.spawnParticle(Particle.DUST_COLOR_TRANSITION, location.clone().add(0.3, 0.1, 0.3), 5, 0.2, 0.2, 0.2, 0.01, dustTransition);
   }

   public static void renderOreOutline(Player player, Location location, Material material) {
      Color oreColor = ORE_COLORS.getOrDefault(material, Color.WHITE);
      DustTransition dustTransition = new DustTransition(oreColor, oreColor, 1.0F);
      Location baseLocation = location.clone();
      double cornerOffset = 0.2;
      spawnCornerParticle(player, baseLocation.clone().add(cornerOffset, cornerOffset, cornerOffset), dustTransition);
      spawnCornerParticle(player, baseLocation.clone().add(0.8, cornerOffset, cornerOffset), dustTransition);
      spawnCornerParticle(player, baseLocation.clone().add(cornerOffset, cornerOffset, 0.8), dustTransition);
      spawnCornerParticle(player, baseLocation.clone().add(0.8, cornerOffset, 0.8), dustTransition);
      spawnCornerParticle(player, baseLocation.clone().add(cornerOffset, 0.8, cornerOffset), dustTransition);
      spawnCornerParticle(player, baseLocation.clone().add(0.8, 0.8, cornerOffset), dustTransition);
      spawnCornerParticle(player, baseLocation.clone().add(cornerOffset, 0.8, 0.8), dustTransition);
      spawnCornerParticle(player, baseLocation.clone().add(0.8, 0.8, 0.8), dustTransition);
   }

   private static void spawnCornerParticle(Player player, Location location, DustTransition dustTransition) {
      player.spawnParticle(Particle.DUST_COLOR_TRANSITION, location, 1, 0.0, 0.0, 0.0, 0.0, dustTransition);
   }

   public static boolean isSupportedOre(Material material) {
      return ORE_COLORS.containsKey(material);
   }

   static {
      ORE_COLORS.put(Material.DIAMOND_ORE, Color.fromRGB(51, 235, 203));
      ORE_COLORS.put(Material.DEEPSLATE_DIAMOND_ORE, Color.fromRGB(51, 235, 203));
      ORE_COLORS.put(Material.GOLD_ORE, Color.fromRGB(255, 215, 0));
      ORE_COLORS.put(Material.DEEPSLATE_GOLD_ORE, Color.fromRGB(255, 215, 0));
      ORE_COLORS.put(Material.IRON_ORE, Color.fromRGB(213, 213, 213));
      ORE_COLORS.put(Material.DEEPSLATE_IRON_ORE, Color.fromRGB(213, 213, 213));
      ORE_COLORS.put(Material.EMERALD_ORE, Color.fromRGB(0, 217, 58));
      ORE_COLORS.put(Material.DEEPSLATE_EMERALD_ORE, Color.fromRGB(0, 217, 58));
      ORE_COLORS.put(Material.REDSTONE_ORE, Color.fromRGB(255, 0, 0));
      ORE_COLORS.put(Material.DEEPSLATE_REDSTONE_ORE, Color.fromRGB(255, 0, 0));
      ORE_COLORS.put(Material.LAPIS_ORE, Color.fromRGB(0, 0, 255));
      ORE_COLORS.put(Material.DEEPSLATE_LAPIS_ORE, Color.fromRGB(0, 0, 255));
      ORE_COLORS.put(Material.COPPER_ORE, Color.fromRGB(180, 113, 89));
      ORE_COLORS.put(Material.DEEPSLATE_COPPER_ORE, Color.fromRGB(180, 113, 89));
      ORE_COLORS.put(Material.COAL_ORE, Color.fromRGB(47, 44, 54));
      ORE_COLORS.put(Material.DEEPSLATE_COAL_ORE, Color.fromRGB(47, 44, 54));
      ORE_COLORS.put(Material.ANCIENT_DEBRIS, Color.fromRGB(92, 49, 49));
      ORE_COLORS.put(Material.NETHER_GOLD_ORE, Color.fromRGB(255, 215, 0));
      ORE_COLORS.put(Material.NETHER_QUARTZ_ORE, Color.fromRGB(255, 255, 255));
   }
}
