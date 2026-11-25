package sakuyaayane.nekoantixray;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class OreStatisticsListener implements Listener {
   private final NekoAntiXray plugin;
   private final OreStatsStorage oreStatsStorage;
   private final OreTrackingManager oreTrackingManager;

   public OreStatisticsListener(NekoAntiXray plugin, OreStatsStorage oreStatsStorage, OreTrackingManager oreTrackingManager) {
      this.plugin = plugin;
      this.oreStatsStorage = oreStatsStorage;
      this.oreTrackingManager = oreTrackingManager;
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onBlockBreak(BlockBreakEvent event) {
      ConfigManager configManager = this.plugin.getConfigManager();
      if (configManager.isOreStatsEnabled()) {
         Player player = event.getPlayer();
         Material blockType = event.getBlock().getType();
         if (this.isAnyOre(blockType)) {
            boolean trackAllOres = this.plugin.getConfig().getBoolean("ore-stats.track-all-ores", true);
            if (trackAllOres || this.isTrackedValuableOre(blockType)) {
               this.oreStatsStorage.incrementOreCount(player, blockType);
               this.oreTrackingManager.notifyTrackers(player, blockType, event.getBlock().getLocation());
            }
         }
      }
   }

   private boolean isAnyOre(Material material) {
      if (material == null) {
         return false;
      }

      String materialName = material.name();
      return materialName.endsWith("_ORE") || material == Material.ANCIENT_DEBRIS;
   }

   private boolean isTrackedValuableOre(Material material) {
      switch (material) {
         case DIAMOND_ORE:
         case DEEPSLATE_DIAMOND_ORE:
         case EMERALD_ORE:
         case DEEPSLATE_EMERALD_ORE:
         case GOLD_ORE:
         case DEEPSLATE_GOLD_ORE:
         case NETHER_GOLD_ORE:
         case IRON_ORE:
         case DEEPSLATE_IRON_ORE:
         case LAPIS_ORE:
         case DEEPSLATE_LAPIS_ORE:
         case COPPER_ORE:
         case DEEPSLATE_COPPER_ORE:
         case REDSTONE_ORE:
         case DEEPSLATE_REDSTONE_ORE:
         case ANCIENT_DEBRIS:
         case NETHER_QUARTZ_ORE:
            return true;
         default:
            return false;
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
            MATERIAL_ORDINAL_MAP[Material.GOLD_ORE.ordinal()] = 5;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINAL_MAP[Material.DEEPSLATE_GOLD_ORE.ordinal()] = 6;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINAL_MAP[Material.NETHER_GOLD_ORE.ordinal()] = 7;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINAL_MAP[Material.IRON_ORE.ordinal()] = 8;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINAL_MAP[Material.DEEPSLATE_IRON_ORE.ordinal()] = 9;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINAL_MAP[Material.LAPIS_ORE.ordinal()] = 10;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINAL_MAP[Material.DEEPSLATE_LAPIS_ORE.ordinal()] = 11;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINAL_MAP[Material.COPPER_ORE.ordinal()] = 12;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINAL_MAP[Material.DEEPSLATE_COPPER_ORE.ordinal()] = 13;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINAL_MAP[Material.REDSTONE_ORE.ordinal()] = 14;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINAL_MAP[Material.DEEPSLATE_REDSTONE_ORE.ordinal()] = 15;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINAL_MAP[Material.ANCIENT_DEBRIS.ordinal()] = 16;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            MATERIAL_ORDINAL_MAP[Material.NETHER_QUARTZ_ORE.ordinal()] = 17;
         } catch (NoSuchFieldError ignored) {
         }
      }
   }
}
