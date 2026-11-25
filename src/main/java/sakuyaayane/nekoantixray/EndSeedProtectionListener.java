package sakuyaayane.nekoantixray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.DragonBattle;
import org.bukkit.boss.DragonBattle.RespawnPhase;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import sakuyaayane.nekoantixray.lib.folialib.FoliaLib;
import sakuyaayane.nekoantixray.lib.folialib.wrapper.task.WrappedTask;

public class EndSeedProtectionListener implements Listener {
   private final NekoAntiXray plugin;
   private final AntiSeedCrackerManager antiSeedCrackerManager;
   private final Random random = new Random();
   private final NamespacedKey endCityProcessedKey;
   private final NamespacedKey endSpikesModifiedKey;
   private static final List<Integer> VANILLA_SPIKE_HEIGHTS = List.of(76, 79, 82, 85, 88, 91, 94, 97, 100, 103);
   private boolean spikeModificationScheduled = false;
   private EntityType endCrystalEntityType;
   private FoliaLib foliaScheduler;
   private WrappedTask spikeModificationTask = null;

   @EventHandler
   public void onChunkPopulate(ChunkPopulateEvent event) {
      if (this.antiSeedCrackerManager.isModifyEndCitiesEnabled() && this.antiSeedCrackerManager.isModifyEndStructuresEnabled()) {
         World world = event.getWorld();
         if (world.getEnvironment() == Environment.THE_END) {
            List endStructureWorlds = this.antiSeedCrackerManager.getEndStructureWorlds();
            String dimensionKey = WorldSeedResolver.getDimensionKey(world);
            if (endStructureWorlds != null && endStructureWorlds.contains(dimensionKey)) {
               Chunk chunk = event.getChunk();
               PersistentDataContainer persistentDataContainer = chunk.getPersistentDataContainer();
               if (!persistentDataContainer.getOrDefault(this.endCityProcessedKey, PersistentDataType.BOOLEAN, false)) {
                  boolean modifiedAnyGlass = false;

                  for (int x = 0; x < 16; x++) {
                     for (int z = 0; z < 16; z++) {
                        for (int y = 62; y <= 65; y++) {
                           Block block = chunk.getBlock(x, y, z);
                           if (block.getType() == Material.MAGENTA_STAINED_GLASS) {
                              block.setType(Material.MAGENTA_STAINED_GLASS_PANE);
                              modifiedAnyGlass = true;
                           }
                        }
                     }
                  }

                  if (modifiedAnyGlass) {
                     this.plugin.logDebug("[AntiSeedCracker] 已修改末地城区块 " + chunk.getX() + "," + chunk.getZ() + " 中的洋红色染色玻璃");
                  }

                  persistentDataContainer.set(this.endCityProcessedKey, PersistentDataType.BOOLEAN, true);
               }
            }
         }
      }
   }

   @EventHandler
   public void onEndCentralChunkLoad(ChunkLoadEvent event) {
      if (this.antiSeedCrackerManager.isModifyEndStructuresEnabled() && this.antiSeedCrackerManager.isModifyEndSpikesEnabled()) {
         World world = event.getWorld();
         if (world.getEnvironment() == Environment.THE_END) {
            List endStructureWorlds = this.antiSeedCrackerManager.getEndStructureWorlds();
            String dimensionKey = WorldSeedResolver.getDimensionKey(world);
            if (endStructureWorlds != null && endStructureWorlds.contains(dimensionKey)) {
               int chunkX = event.getChunk().getX();
               int chunkZ = event.getChunk().getZ();
               if (Math.abs(chunkX) <= 4
                  && Math.abs(chunkZ) <= 4
                  && !world.getPersistentDataContainer().getOrDefault(this.endSpikesModifiedKey, PersistentDataType.BOOLEAN, false)) {
                  this.plugin.logDebug("[AntiSeedCracker] 检测到末地中心区块加载，尝试提前修改末地尖刺: " + world.getName());
                  this.foliaScheduler.getImpl().runAtLocation(new Location(world, 0.0, 0.0, 0.0), wrappedTask -> this.modifyEndSpikes(world));
               }
            }
         }
      }
   }

   @EventHandler
   public void onEnterEnd(PlayerChangedWorldEvent event) {
      if (this.antiSeedCrackerManager.isModifyEndStructuresEnabled() && this.antiSeedCrackerManager.isModifyEndSpikesEnabled()) {
         World world = event.getPlayer().getWorld();
         if (world.getEnvironment() == Environment.THE_END) {
            List endStructureWorlds = this.antiSeedCrackerManager.getEndStructureWorlds();
            String dimensionKey = WorldSeedResolver.getDimensionKey(world);
            if (endStructureWorlds != null
               && endStructureWorlds.contains(dimensionKey)
               && !world.getPersistentDataContainer().getOrDefault(this.endSpikesModifiedKey, PersistentDataType.BOOLEAN, false)) {
               this.plugin.logDebug("[AntiSeedCracker] 玩家进入末地，尝试提前修改末地尖刺: " + world.getName());
               this.foliaScheduler.getImpl().runAtLocation(new Location(world, 0.0, 0.0, 0.0), wrappedTask -> this.modifyEndSpikes(world));
            }
         }
      }
   }

   @EventHandler
   public void onEndPortalTeleport(PlayerTeleportEvent event) {
      if (event.getTo() != null
         && event.getCause() == TeleportCause.END_PORTAL
         && this.antiSeedCrackerManager.isModifyEndStructuresEnabled()
         && this.antiSeedCrackerManager.isModifyEndSpikesEnabled()) {
         World world = event.getTo().getWorld();
         if (world != null && world.getEnvironment() == Environment.THE_END) {
            List endStructureWorlds = this.antiSeedCrackerManager.getEndStructureWorlds();
            String dimensionKey = WorldSeedResolver.getDimensionKey(world);
            if (endStructureWorlds != null
               && endStructureWorlds.contains(dimensionKey)
               && !world.getPersistentDataContainer().getOrDefault(this.endSpikesModifiedKey, PersistentDataType.BOOLEAN, false)) {
               this.plugin.logDebug("[AntiSeedCracker] 通过末地传送门进入，尝试提前修改末地尖刺: " + world.getName());
               this.foliaScheduler.getImpl().runAtLocation(new Location(world, 0.0, 0.0, 0.0), wrappedTask -> this.modifyEndSpikes(world));
            }
         }
      }
   }

   @EventHandler
   public void onPlayerPlaceRespawnCrystal(EntityPlaceEvent event) {
      if (event.getEntityType() == this.endCrystalEntityType) {
         World world = event.getEntity().getWorld();
         if (world.getEnvironment() == Environment.THE_END && event.getBlock() != null && event.getBlock().getType() == Material.BEDROCK) {
            Location location = event.getBlock().getLocation();
            if (!(Math.abs(location.getX()) > 3.0) && !(Math.abs(location.getZ()) > 3.0) && this.getAmountOfEnderCrystalsOnPortal(location) == 3) {
               List endStructureWorlds = this.antiSeedCrackerManager.getEndStructureWorlds();
               String dimensionKey = WorldSeedResolver.getDimensionKey(world);
               if (endStructureWorlds != null
                  && endStructureWorlds.contains(dimensionKey)
                  && (!this.spikeModificationScheduled || this.spikeModificationTask == null || this.spikeModificationTask.isCancelled())
                  && this.antiSeedCrackerManager.isModifyEndSpikesEnabled()
                  && this.antiSeedCrackerManager.isModifyEndStructuresEnabled()) {
                  world.getPersistentDataContainer().set(this.endSpikesModifiedKey, PersistentDataType.BOOLEAN, false);
                  this.plugin.logDebug("[AntiSeedCracker] 第四个末影水晶已放置。正在为世界调度尖刺修改任务: " + world.getName());
                  this.spikeModificationScheduled = true;
                  this.spikeModificationTask = this.foliaScheduler.getImpl().runTimer(() -> {
                     if (!this.spikeModificationScheduled) {
                        if (this.spikeModificationTask != null) {
                           this.spikeModificationTask.cancel();
                           this.spikeModificationTask = null;
                        }
                     } else {
                        DragonBattle enderDragonBattle = world.getEnderDragonBattle();
                        if (enderDragonBattle == null) {
                           this.plugin.logDebug("[AntiSeedCracker] 未找到 DragonBattle。作为后备方案修改尖刺 (世界: " + world.getName() + ")。");
                           this.foliaScheduler.getImpl().runAtLocation(new Location(world, 0.0, 0.0, 0.0), wrappedTask -> this.modifyEndSpikes(world));
                           this.spikeModificationScheduled = false;
                           if (this.spikeModificationTask != null) {
                              this.spikeModificationTask.cancel();
                              this.spikeModificationTask = null;
                           }
                        } else {
                           RespawnPhase respawnPhase = enderDragonBattle.getRespawnPhase();
                           if (respawnPhase != RespawnPhase.START && respawnPhase != RespawnPhase.PREPARING_TO_SUMMON_PILLARS && respawnPhase != RespawnPhase.SUMMONING_PILLARS) {
                              this.foliaScheduler.getImpl().runAtLocation(new Location(world, 0.0, 0.0, 0.0), wrappedTask -> this.modifyEndSpikes(world));
                              this.spikeModificationScheduled = false;
                              if (this.spikeModificationTask != null) {
                                 this.spikeModificationTask.cancel();
                                 this.spikeModificationTask = null;
                              }
                           }
                        }
                     }
                  }, 300L, 20L);
               }
            }
         }
      }
   }

   private int getAmountOfEnderCrystalsOnPortal(Location location) {
      World world = location.getWorld();
      if (world == null) {
         return 0;
      }

      Location searchCenter = location.clone();
      return world.getNearbyEntities(
            searchCenter,
            6.0,
            6.0,
            6.0,
            entity -> entity.getType() == this.endCrystalEntityType && entity.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.BEDROCK
         )
         .size();
   }

   public void modifyEndSpikes(World world) {
      if (world.getEnvironment() == Environment.THE_END
         && !world.getPersistentDataContainer().getOrDefault(this.endSpikesModifiedKey, PersistentDataType.BOOLEAN, false)) {
         this.plugin.logDebug("[AntiSeedCracker] 正在修改世界 " + world.getName() + " 中的末地尖刺");
         Map spikeBedrockMap = this.findEndSpikeBedrockBlocks(world);
         if (spikeBedrockMap.size() < 2 && "swap".equalsIgnoreCase(this.antiSeedCrackerManager.getEndSpikesMode())) {
            this.plugin.logDebug("[AntiSeedCracker] 世界 " + world.getName() + " 中的基岩尖刺数量不足 (<2)，无法执行 'swap' 模式");
            world.getPersistentDataContainer().set(this.endSpikesModifiedKey, PersistentDataType.BOOLEAN, true);
         } else if (spikeBedrockMap.isEmpty() && "move".equalsIgnoreCase(this.antiSeedCrackerManager.getEndSpikesMode())) {
            this.plugin.logDebug("[AntiSeedCracker] 世界 " + world.getName() + " 中未找到基岩尖刺，无法执行 'move' 模式");
            world.getPersistentDataContainer().set(this.endSpikesModifiedKey, PersistentDataType.BOOLEAN, true);
         } else {
            String endSpikesMode = this.antiSeedCrackerManager.getEndSpikesMode();
            boolean modified = false;
            if ("swap".equalsIgnoreCase(endSpikesMode)) {
               this.swapEndSpikeHeights(world, spikeBedrockMap);
               modified = true;
            } else if ("move".equalsIgnoreCase(endSpikesMode)) {
               this.shiftEndSpikeBedrockDown(world, spikeBedrockMap);
               modified = true;
            } else {
               this.plugin.logDebug("[AntiSeedCracker] 未知的末地尖刺模式: " + endSpikesMode);
            }

            if (modified) {
               this.plugin.logDebug("[AntiSeedCracker] 末地尖刺已在世界 " + world.getName() + " 中修改 (模式: " + endSpikesMode + ").");
            }

            world.getPersistentDataContainer().set(this.endSpikesModifiedKey, PersistentDataType.BOOLEAN, true);
         }
      }
   }

   private Map findEndSpikeBedrockBlocks(World world) {
      HashMap spikeBedrockMap = new HashMap(10);

      for (int i = 0; i < 10; i++) {
         double angle = 2.0 * (-Math.PI + (Math.PI / 10) * i);
         double spikeX = 42.0 * Math.cos(angle);
         double spikeZ = 42.0 * Math.sin(angle);
         Location highestLocation = new Location(world, spikeX, world.getHighestBlockYAt((int)spikeX, (int)spikeZ), spikeZ);
         Block highestBlock = highestLocation.getBlock();

         for (int y = highestBlock.getY(); y > world.getMinHeight(); y--) {
            Block blockAt = world.getBlockAt((int)spikeX, y, (int)spikeZ);
            if (blockAt.getType() == Material.BEDROCK) {
               if (VANILLA_SPIKE_HEIGHTS.contains(y)) {
                  spikeBedrockMap.put(y, blockAt);
               }
               break;
            }
         }
      }

      return spikeBedrockMap;
   }

   private void swapEndSpikeHeights(World world, Map spikeBedrockMap) {
      if (spikeBedrockMap.size() >= 2) {
         ArrayList spikeBlocks = new ArrayList(spikeBedrockMap.values());
         int firstIndex = ThreadLocalRandom.current().nextInt(spikeBlocks.size());

         int secondIndex;
         do {
            secondIndex = ThreadLocalRandom.current().nextInt(spikeBlocks.size());
         } while (firstIndex == secondIndex);

         Block firstBlock = (Block)spikeBlocks.get(firstIndex);
         Block secondBlock = (Block)spikeBlocks.get(secondIndex);
         int firstY = firstBlock.getY();
         int secondY = secondBlock.getY();
         firstBlock.setType(Material.OBSIDIAN, false);
         secondBlock.setType(Material.OBSIDIAN, false);
         new Location(world, firstBlock.getX(), secondY, firstBlock.getZ()).getBlock().setType(Material.BEDROCK, false);
         new Location(world, secondBlock.getX(), firstY, secondBlock.getZ()).getBlock().setType(Material.BEDROCK, false);
         int firstX = firstBlock.getX();
         int firstZ = firstBlock.getZ();
         int secondX = secondBlock.getX();
         int secondZ = secondBlock.getZ();
         this.plugin.logDebug("[AntiSeedCracker] 已交换末地尖刺基岩 Y 坐标，位于 XZ:(" + firstX + "," + firstZ + ") 和 XZ:(" + secondX + "," + secondZ + ") 之间");
      }
   }

   private void shiftEndSpikeBedrockDown(World world, Map spikeBedrockMap) {
      if (!spikeBedrockMap.isEmpty()) {
         ArrayList spikeBlocks = new ArrayList(spikeBedrockMap.values());
         Block spikeBlock = (Block)spikeBlocks.get(ThreadLocalRandom.current().nextInt(spikeBlocks.size()));
         spikeBlock.setType(Material.OBSIDIAN, false);
         Block belowBlock = spikeBlock.getRelative(BlockFace.DOWN);
         if (belowBlock.getY() > world.getMinHeight() && belowBlock.getType() != Material.BEDROCK && belowBlock.getType() != Material.END_PORTAL_FRAME) {
            belowBlock.setType(Material.BEDROCK, false);
            this.plugin.logDebug("[AntiSeedCracker] 已将位于 XZ:(" + spikeBlock.getX() + "," + spikeBlock.getZ() + ") 的末地尖刺基岩向下移动一个方块。");
         } else {
            this.plugin.logDebug("[AntiSeedCracker] 无法安全地将位于 XZ:(" + spikeBlock.getX() + "," + spikeBlock.getZ() + ") 的末地尖刺基岩向下移动。仅更改为黑曜石。");
         }
      }
   }

   public EndSeedProtectionListener(NekoAntiXray plugin, AntiSeedCrackerManager antiSeedCrackerManager) {
      this.plugin = plugin;
      this.antiSeedCrackerManager = antiSeedCrackerManager;
      this.foliaScheduler = plugin.getFoliaScheduler();
      this.endCityProcessedKey = new NamespacedKey(plugin, "neko_asc_chunk_city_processed");
      this.endSpikesModifiedKey = new NamespacedKey(plugin, "neko_asc_world_spikes_modified");

      try {
         this.endCrystalEntityType = EntityType.END_CRYSTAL;
      } catch (NoSuchFieldError ignored) {
         this.endCrystalEntityType = EntityType.valueOf("ENDER_CRYSTAL");
      }
   }
}
