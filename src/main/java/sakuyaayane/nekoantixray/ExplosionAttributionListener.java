package sakuyaayane.nekoantixray;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import sakuyaayane.nekoantixray.lib.folialib.FoliaLib;

public class ExplosionAttributionListener implements Listener {
   private final NekoAntiXray plugin;
   private XrayDetector xrayDetector;
   private final ExplosionTracker explosionTracker;
   private final FoliaLib foliaLib;
   private static final EntityType TNT_ENTITY_TYPE;
   private static final long BED_EXPLOSION_BYPASS_SECONDS = 180L;

   public ExplosionAttributionListener(NekoAntiXray plugin) {
      this.plugin = plugin;
      this.explosionTracker = new ExplosionTracker();
      this.foliaLib = new FoliaLib(plugin);
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onTntSpawn(EntitySpawnEvent event) {
      Entity entity = event.getEntity();
      if (TNT_ENTITY_TYPE != null && entity.getType() == TNT_ENTITY_TYPE) {
         Location location = entity.getLocation();
         World world = location.getWorld();
         if (world != null) {
            Player nearestPlayer = null;
            double nearestDistSq = 100.0;

            for (Player candidate : world.getPlayers()) {
               if (candidate != null && candidate.isOnline()) {
                  try {
                     double distSq = candidate.getLocation().distanceSquared(location);
                     if (distSq < nearestDistSq) {
                        nearestDistSq = distSq;
                        nearestPlayer = candidate;
                     }
                  } catch (Exception ignored) {
                  }
               }
            }

            if (nearestPlayer != null) {
               entity.setMetadata("PLACED_BY_PLAYER", new FixedMetadataValue(this.plugin, nearestPlayer.getUniqueId().toString()));
               String debugMessage = "TNT实体生成，标记责任人: "
                  + nearestPlayer.getName()
                  + " 位置: "
                  + String.format("%.1f, %.1f, %.1f", location.getX(), location.getY(), location.getZ());
               this.plugin.logDebug(debugMessage);
            } else {
               this.plugin.logDebug("TNT实体生成，但附近没有玩家，位置: " + String.format("%.1f, %.1f, %.1f", location.getX(), location.getY(), location.getZ()));
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPlayerInteract(PlayerInteractEvent event) {
      if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
         Block clickedBlock = event.getClickedBlock();
         if (clickedBlock != null) {
            Player player = event.getPlayer();
            World world = player.getWorld();
            Location location = clickedBlock.getLocation();
            if (clickedBlock.getType().toString().contains("BED")) {
               Environment environment = world.getEnvironment();
               if (environment == Environment.NETHER || environment == Environment.THE_END) {
                  this.foliaLib.getImpl().runAtLocation(location, wrappedTask -> {
                     try {
                        this.explosionTracker.recordExplosion(player, location);
                        this.notifyAffectedBlocksInRadius(location, 8);
                        if (this.xrayDetector != null) {
                           InternalBypassManager internalBypassManager = this.xrayDetector.getInternalBypassManager();
                           internalBypassManager.grantBypass(player, 180L, "床爆炸触发");
                        }
                     } catch (Exception ex) {
                        this.plugin.getLogger().severe("处理床爆炸事件时发生错误: " + ex.getMessage());
                     }
                  });
               }
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onEntityExplode(EntityExplodeEvent event) {
      if (this.plugin.getConfig().getBoolean("AntiXray.Enabled", true)
         && this.plugin.getConfig().getBoolean("AntiXray.World.Enabled", true)
         && (
            !this.plugin.getConfig().getBoolean("AntiXray.World.WorldWhitelist.Enabled", false)
               || this.plugin.getConfig().getStringList("AntiXray.World.WorldWhitelist.Worlds").contains(event.getLocation().getWorld().getName())
         )) {
         Entity entity = event.getEntity();
         if (!(entity instanceof Player)
            && TNT_ENTITY_TYPE != null
            && event.getEntityType() == TNT_ENTITY_TYPE
            && this.plugin.getConfig().getBoolean("AntiXray.ProximityHider.Explosives.TrackTNT", true)) {
            this.foliaLib.getImpl().runAsync(wrappedTask -> {
               if (entity.hasMetadata("PLACED_BY_PLAYER")) {
                  List<MetadataValue> metadataValues = entity.getMetadata("PLACED_BY_PLAYER");
                  if (!metadataValues.isEmpty()) {
                     String placerUuidText = metadataValues.get(0).asString();
                     UUID placerUuid = UUID.fromString(placerUuidText);
                     Player placer = Bukkit.getPlayer(placerUuid);
                     if (placer != null) {
                        this.explosionTracker.recordExplosion(placer, event.getLocation());
                     }
                  }
               }
            });
         }
      }
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onBlockExplode(BlockExplodeEvent event) {
      if (event.getBlock() != null) {
         Location location = event.getBlock().getLocation();
         this.foliaLib.getImpl().runAtLocation(location, wrappedTask -> {
            try {
               World world = location.getWorld();
               Player nearestPlayer = this.findNearestPlayer(world, location);
               if (nearestPlayer != null) {
                  this.explosionTracker.recordExplosion(nearestPlayer, location);
               }

               this.notifyAffectedBlocksInRadius(location, 6);
            } catch (Exception ex) {
               this.plugin.getLogger().severe("处理方块爆炸事件时发生错误: " + ex.getMessage());
            }
         });
      }
   }

   public ExplosionTracker getExplosionTracker() {
      return this.explosionTracker;
   }

   public void setXrayDetector(XrayDetector xrayDetector) {
      this.xrayDetector = xrayDetector;
   }

   private void notifyAffectedBlocksInRadius(Location center, int radius) {
      if (this.xrayDetector != null) {
         for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
               for (int dz = -radius; dz <= radius; dz++) {
                  Location targetLocation = center.clone().add(dx, dy, dz);
                  if (targetLocation.distanceSquared(center) <= radius * radius) {
                     Block affectedBlock = targetLocation.getBlock();

                     try {
                        this.xrayDetector.cleanupAdjacentFakeOres(affectedBlock);
                     } catch (Exception ex) {
                        this.plugin.getLogger().warning("处理爆炸影响的方块时发生错误: " + ex.getMessage());
                     }
                  }
               }
            }
         }
      }
   }

   private Player findNearestPlayer(World world, Location location) {
      if (world != null && location != null) {
         Player nearestPlayer = null;
         double nearestDistSq = 10000.0;

         for (Player candidate : world.getPlayers()) {
            if (candidate != null && candidate.isOnline()) {
               try {
                  double distSq = candidate.getLocation().distanceSquared(location);
                  if (distSq < nearestDistSq) {
                     nearestDistSq = distSq;
                     nearestPlayer = candidate;
                  }
               } catch (Exception ex) {
                  this.plugin.getLogger().warning("计算玩家距离时发生错误: " + ex.getMessage());
               }
            }
         }

         return nearestPlayer;
      } else {
         return null;
      }
   }

   static {
      EntityType resolvedTntType = null;

      try {
         Field primedTntField = EntityType.class.getField("PRIMED_TNT");
         resolvedTntType = (EntityType)primedTntField.get(null);
      } catch (Exception primedTntFieldError) {
         try {
            Field tntField = EntityType.class.getField("TNT");
            resolvedTntType = (EntityType)tntField.get(null);
         } catch (Exception tntFieldError) {
            try {
               resolvedTntType = EntityType.valueOf("PRIMED_TNT");
            } catch (Exception primedTntEnumError) {
               try {
                  resolvedTntType = EntityType.valueOf("TNT");
               } catch (Exception tntEnumError) {
               }
            }
         }
      }

      TNT_ENTITY_TYPE = resolvedTntType;
      if (TNT_ENTITY_TYPE == null) {
         if (NekoAntiXray.getInstance() != null && NekoAntiXray.getInstance().getLogger() != null) {
            NekoAntiXray.getInstance()
               .getLogger()
               .severe("[NekoAntiXray] CRITICAL: Could not resolve any TNT EntityType. TNT-related features will be disabled or malfunction.");
         } else {
            Bukkit.getLogger().severe("[NekoAntiXray] CRITICAL: Could not resolve any TNT EntityType. (Fallback logger)");
         }
      }
   }
}
