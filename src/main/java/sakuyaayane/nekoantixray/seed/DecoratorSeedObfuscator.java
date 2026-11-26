package sakuyaayane.nekoantixray.seed;

import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.Palette;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import sakuyaayane.nekoantixray.config.ConfigManager;
import sakuyaayane.nekoantixray.stats.ModuleTimingStatistics;
import sakuyaayane.nekoantixray.world.PlayerBlockStateCache;

public final class DecoratorSeedObfuscator {
   private static final StateType AIR = resolveStateType("air");
   private static final StateType CAVE_AIR = resolveStateType("cave_air");
   private static final StateType STONE = resolveStateType("stone");
   private static final StateType DEEPSLATE = resolveStateType("deepslate");
   private static final StateType NETHERRACK = resolveStateType("netherrack");
   private static final StateType SPAWNER = resolveStateType("spawner");
   private static final StateType END_GATEWAY = resolveStateType("end_gateway");
   private static final StateType EMERALD_ORE = resolveStateType("emerald_ore");
   private static final StateType DEEPSLATE_EMERALD_ORE = resolveStateType("deepslate_emerald_ore");
   private static final StateType COBBLESTONE = resolveStateType("cobblestone");
   private static final StateType MOSSY_COBBLESTONE = resolveStateType("mossy_cobblestone");
   private static final StateType WATER = resolveStateType("water");
   private static final StateType SANDSTONE = resolveStateType("sandstone");
   private static final StateType CUT_SANDSTONE = resolveStateType("cut_sandstone");
   private static final StateType CHISELED_SANDSTONE = resolveStateType("chiseled_sandstone");
   private static final StateType SANDSTONE_SLAB = resolveStateType("sandstone_slab");
   private static final StateType STONE_PRESSURE_PLATE = resolveStateType("stone_pressure_plate");
   private static final StateType TNT = resolveStateType("tnt");
   private static final StateType TERRACOTTA = resolveStateType("terracotta");
   private static final StateType ORANGE_TERRACOTTA = resolveStateType("orange_terracotta");
   private static final StateType BLUE_TERRACOTTA = resolveStateType("blue_terracotta");
   private static final StateType REDSTONE_WIRE = resolveStateType("redstone_wire");
   private static final StateType TRIPWIRE_HOOK = resolveStateType("tripwire_hook");
   private static final StateType TRIPWIRE = resolveStateType("tripwire");
   private static final StateType STICKY_PISTON = resolveStateType("sticky_piston");
   private static final StateType REPEATER = resolveStateType("repeater");
   private static final StateType CHISELED_STONE_BRICKS = resolveStateType("chiseled_stone_bricks");
   private static final StateType CAULDRON = resolveStateType("cauldron");
   private static final StateType WATER_CAULDRON = resolveStateType("water_cauldron");
   private static final StateType LAVA_CAULDRON = resolveStateType("lava_cauldron");
   private static final StateType POWDER_SNOW_CAULDRON = resolveStateType("powder_snow_cauldron");
   private static final StateType CRAFTING_TABLE = resolveStateType("crafting_table");
   private static final StateType POTTED_RED_MUSHROOM = resolveStateType("potted_red_mushroom");
   private static final StateType SPRUCE_PLANKS = resolveStateType("spruce_planks");
   private static final StateType OAK_PLANKS = resolveStateType("oak_planks");
   private static final StateType SPRUCE_LOG = resolveStateType("spruce_log");
   private static final StateType OAK_LOG = resolveStateType("oak_log");
   private static final StateType SPRUCE_STAIRS = resolveStateType("spruce_stairs");
   private static final StateType OAK_STAIRS = resolveStateType("oak_stairs");
   private static final StateType SPRUCE_FENCE = resolveStateType("spruce_fence");
   private static final StateType OAK_FENCE = resolveStateType("oak_fence");
   private static final StateType ICE = resolveStateType("ice");
   private static final StateType SNOW_BLOCK = resolveStateType("snow_block");
   private static final StateType BIRCH_PLANKS = resolveStateType("birch_planks");
   private static final StateType WARPED_STEM = resolveStateType("warped_stem");
   private static final StateType WARPED_WART_BLOCK = resolveStateType("warped_wart_block");
   private static final StateType SHROOMLIGHT = resolveStateType("shroomlight");
   private static final StateType CHEST = resolveStateType("chest");
   private static final StateType DIRT = resolveStateType("dirt");
   private static final StateType ANDESITE = resolveStateType("andesite");
   private static final StateType GRANITE = resolveStateType("granite");
   private static final StateType DIORITE = resolveStateType("diorite");
   private static final StateType COAL_ORE = resolveStateType("coal_ore");
   private static final StateType IRON_ORE = resolveStateType("iron_ore");
   private static final StateType GOLD_ORE = resolveStateType("gold_ore");
   private static final StateType GRAVEL = resolveStateType("gravel");
   private static final StateType PRISMARINE = resolveStateType("prismarine");
   private static final StateType PRISMARINE_BRICKS = resolveStateType("prismarine_bricks");
   private static final StateType DARK_PRISMARINE = resolveStateType("dark_prismarine");
   private static final StateType SEA_LANTERN = resolveStateType("sea_lantern");
   private static final WrappedBlockState AIR_STATE = createDefaultBlockState(AIR, CAVE_AIR);
   private static final WrappedBlockState STONE_STATE = createDefaultBlockState(STONE, AIR);
   private static final WrappedBlockState DEEPSLATE_STATE = createDefaultBlockState(DEEPSLATE, STONE);
   private static final WrappedBlockState NETHERRACK_STATE = createDefaultBlockState(NETHERRACK, STONE);
   private static final WrappedBlockState DIRT_STATE = createDefaultBlockState(DIRT, STONE);
   private static final WrappedBlockState SANDSTONE_STATE = createDefaultBlockState(SANDSTONE, STONE);
   private static final WrappedBlockState COBBLESTONE_STATE = createDefaultBlockState(COBBLESTONE, STONE);
   private static final WrappedBlockState OAK_PLANKS_STATE = createDefaultBlockState(OAK_PLANKS, STONE);
   private static final WrappedBlockState SNOW_BLOCK_STATE = createDefaultBlockState(SNOW_BLOCK, AIR);
   private static final WrappedBlockState DARK_PRISMARINE_STATE = createDefaultBlockState(DARK_PRISMARINE, STONE);
   private static final WrappedBlockState WATER_STATE = createDefaultBlockState(WATER, AIR);
   private static final Set<StateType> DUNGEON_FLOOR_BLOCKS = createBlockTypeSet(new StateType[]{COBBLESTONE, MOSSY_COBBLESTONE});
   private static final Set<StateType> DESERT_WELL_SANDSTONE_BLOCKS = createBlockTypeSet(
      new StateType[]{SANDSTONE, CUT_SANDSTONE, CHISELED_SANDSTONE, SANDSTONE_SLAB}
   );
   private static final Set<StateType> BURIED_TREASURE_FLOOR_BLOCKS = createBlockTypeSet(
      new StateType[]{SANDSTONE, STONE, ANDESITE, GRANITE, DIORITE, COAL_ORE, IRON_ORE, GOLD_ORE, GRAVEL}
   );
   private static final Set<StateType> MONUMENT_TARGET_BLOCKS = createBlockTypeSet(new StateType[]{PRISMARINE, PRISMARINE_BRICKS, SEA_LANTERN});
   private static final Set<StateType> DESERT_PYRAMID_BLOCKS = createBlockTypeSet(
      new StateType[]{SANDSTONE, CUT_SANDSTONE, CHISELED_SANDSTONE, SANDSTONE_SLAB, TERRACOTTA, ORANGE_TERRACOTTA, BLUE_TERRACOTTA}
   );
   private static final Set<StateType> JUNGLE_TEMPLE_TRAP_BLOCKS = createBlockTypeSet(
      new StateType[]{TRIPWIRE_HOOK, TRIPWIRE, REDSTONE_WIRE, STICKY_PISTON, REPEATER}
   );
   private static final Set<StateType> JUNGLE_TEMPLE_STONE_BLOCKS = createBlockTypeSet(new StateType[]{COBBLESTONE, MOSSY_COBBLESTONE, CHISELED_STONE_BRICKS});
   private static final Set<StateType> CAULDRON_BLOCKS = createBlockTypeSet(new StateType[]{CAULDRON, WATER_CAULDRON, LAVA_CAULDRON, POWDER_SNOW_CAULDRON});
   private static final Set<StateType> SWAMP_HUT_WOOD_BLOCKS = createBlockTypeSet(
      new StateType[]{SPRUCE_PLANKS, OAK_PLANKS, SPRUCE_LOG, OAK_LOG, SPRUCE_STAIRS, OAK_STAIRS, SPRUCE_FENCE, OAK_FENCE}
   );
   private static final Set<StateType> WOODEN_STAIRS_BLOCKS;
   private static final Set<StateType> WOODEN_TRAPDOOR_BLOCKS;
   private static final Set<StateType> WOODEN_LOG_BLOCKS;
   private final boolean strictMode;
   private final boolean decoratorObfuscationEnabled;
   private final boolean dungeonsEnabled;
   private final ThreadLocal<DecoratorSeedObfuscator.ChunkSectionStateCache> sectionStateCache = ThreadLocal.withInitial(
      () -> new DecoratorSeedObfuscator.ChunkSectionStateCache()
   );
   private final Set<StateType> activeTriggerBlockTypes = this.buildActiveTriggerBlockTypes();
   private final boolean stripSpawnerBlockEntity;
   private final boolean replaceSpawnerBlockInStrictMode;
   private final boolean perturbFloorInStrictMode;
   private final boolean breakFloorCount;
   private final int floorTargetCount;
   private final boolean buriedTreasureEnabled;
   private final boolean decoratorShipwreckEnabled;
   private final boolean monumentEnabled;
   private final boolean endGatewayEnabled;
   private final boolean emeraldOreEnabled;
   private final boolean desertWellEnabled;
   private final boolean warpedFungusEnabled;
   private final boolean structureSeedProtectionEnabled;
   private final String structureProtectionStrategy;
   private final boolean aggressiveStructureProtectionEnabled;
   private final boolean desertPyramidProtectionEnabled;
   private final boolean jungleTempleProtectionEnabled;
   private final boolean swampHutProtectionEnabled;
   private final boolean iglooProtectionEnabled;
   private final boolean pillagerOutpostProtectionEnabled;
   private final boolean structureShipwreckProtectionEnabled;
   private final PlayerBlockStateCache shadowState;

   public DecoratorSeedObfuscator(ConfigManager configManager, PlayerBlockStateCache shadowState) {
      this.strictMode = "strict".equalsIgnoreCase(configManager.getAntiSeedCrackerDecoratorPacketObfuscationMode());
      this.decoratorObfuscationEnabled = configManager.isAntiSeedCrackerDecoratorPacketObfuscationEnabled();
      this.dungeonsEnabled = this.decoratorObfuscationEnabled && configManager.isAntiSeedCrackerDecoratorPacketObfuscationDungeonsEnabled();
      this.stripSpawnerBlockEntity = this.decoratorObfuscationEnabled && configManager.isAntiSeedCrackerDecoratorPacketObfuscationDungeonsStripSpawnerBlockEntity();
      this.replaceSpawnerBlockInStrictMode = this.decoratorObfuscationEnabled
         && configManager.isAntiSeedCrackerDecoratorPacketObfuscationDungeonsReplaceSpawnerBlockInStrictMode();
      this.perturbFloorInStrictMode = this.decoratorObfuscationEnabled && configManager.isAntiSeedCrackerDecoratorPacketObfuscationDungeonsPerturbFloorInStrictMode();
      this.breakFloorCount = this.decoratorObfuscationEnabled && configManager.isAntiSeedCrackerDecoratorPacketObfuscationDungeonsBreakFloorCount();
      this.floorTargetCount = configManager.getAntiSeedCrackerDecoratorPacketObfuscationDungeonsFloorTargetCount();
      this.buriedTreasureEnabled = this.decoratorObfuscationEnabled && configManager.isAntiSeedCrackerDecoratorPacketObfuscationStructuresBuriedTreasure();
      this.decoratorShipwreckEnabled = this.decoratorObfuscationEnabled && configManager.isAntiSeedCrackerDecoratorPacketObfuscationStructuresShipwreck();
      this.monumentEnabled = this.decoratorObfuscationEnabled && configManager.isAntiSeedCrackerDecoratorPacketObfuscationStructuresMonument();
      this.endGatewayEnabled = this.decoratorObfuscationEnabled && configManager.isAntiSeedCrackerDecoratorPacketObfuscationEndGatewayEnabled();
      this.emeraldOreEnabled = this.decoratorObfuscationEnabled && configManager.isAntiSeedCrackerDecoratorPacketObfuscationEmeraldOreEnabled();
      this.desertWellEnabled = this.decoratorObfuscationEnabled && configManager.isAntiSeedCrackerDecoratorPacketObfuscationDesertWellEnabled();
      this.warpedFungusEnabled = this.decoratorObfuscationEnabled && configManager.isAntiSeedCrackerDecoratorPacketObfuscationWarpedFungusEnabled();
      this.structureProtectionStrategy = normalizeStructureStrategy(configManager.getAntiSeedCrackerStructureSeedProtectionStrategy());
      this.structureSeedProtectionEnabled = configManager.isAntiSeedCrackerStructureSeedProtectionEnabled();
      this.aggressiveStructureProtectionEnabled = this.structureSeedProtectionEnabled && isAggressiveStrategy(this.structureProtectionStrategy);
      this.desertPyramidProtectionEnabled = this.aggressiveStructureProtectionEnabled && configManager.isAntiSeedCrackerStructureSeedProtectionStructuresDesertPyramid();
      this.jungleTempleProtectionEnabled = this.aggressiveStructureProtectionEnabled && configManager.isAntiSeedCrackerStructureSeedProtectionStructuresJungleTemple();
      this.swampHutProtectionEnabled = this.aggressiveStructureProtectionEnabled && configManager.isAntiSeedCrackerStructureSeedProtectionStructuresSwampHut();
      this.iglooProtectionEnabled = this.aggressiveStructureProtectionEnabled && configManager.isAntiSeedCrackerStructureSeedProtectionStructuresIgloo();
      this.pillagerOutpostProtectionEnabled = this.aggressiveStructureProtectionEnabled
         && configManager.isAntiSeedCrackerStructureSeedProtectionStructuresPillagerOutpost();
      this.structureShipwreckProtectionEnabled = this.structureSeedProtectionEnabled
         && this.aggressiveStructureProtectionEnabled
         && configManager.isAntiSeedCrackerStructureSeedProtectionStructuresShipwreck();
      this.shadowState = shadowState;
   }

   public DecoratorSeedObfuscator.PacketObfuscationResult obfuscateChunkPacket(
      WrapperPlayServerChunkData chunkDataPacket, User user, UUID playerId, String worldName
   ) {
      long startNanos = System.nanoTime();

      try {
         Column column = chunkDataPacket.getColumn();
         if (column != null && column.getChunks() != null) {
            int minWorldHeight = user != null ? user.getMinWorldHeight() : 0;
            TileEntity[] originalTileEntities = column.getTileEntities() != null ? column.getTileEntities() : new TileEntity[0];
            TileEntity[] filteredTileEntities = this.stripProtectedTileEntities(column, minWorldHeight, originalTileEntities, playerId, worldName);
            int strippedCount = originalTileEntities.length - filteredTileEntities.length;
            int modifiedCount = this.obfuscateChunkBlocks(column, minWorldHeight, playerId, worldName);
            if (strippedCount > 0) {
               chunkDataPacket.setColumn(this.copyColumnWithTileEntities(column, filteredTileEntities));
            }

            return modifiedCount == 0 && strippedCount == 0
               ? DecoratorSeedObfuscator.PacketObfuscationResult.NONE
               : new DecoratorSeedObfuscator.PacketObfuscationResult(modifiedCount, strippedCount);
         } else {
            return DecoratorSeedObfuscator.PacketObfuscationResult.NONE;
         }
      } finally {
         ModuleTimingStatistics.record("AntiSeedCracker-装饰特征", System.nanoTime() - startNanos);
      }
   }

   private TileEntity[] stripProtectedTileEntities(Column column, int minWorldHeight, TileEntity[] tileEntities, UUID playerId, String worldName) {
      if (tileEntities != null && tileEntities.length != 0) {
         List<TileEntity> keptTileEntities = new ArrayList<>(tileEntities.length);

         for (TileEntity tileEntity : tileEntities) {
            if (this.shouldStripTileEntity(column, minWorldHeight, tileEntity)) {
               this.recordStrippedTileEntity(column, playerId, worldName, tileEntity);
            } else {
               keptTileEntities.add(tileEntity);
            }
         }

         return keptTileEntities.size() == tileEntities.length ? tileEntities : keptTileEntities.toArray(new TileEntity[0]);
      } else {
         return new TileEntity[0];
      }
   }

   private boolean shouldStripTileEntity(Column column, int minWorldHeight, TileEntity tileEntity) {
      if (tileEntity == null) {
         return false;
      }

      int localX = this.toLocalChunkCoord(tileEntity.getX(), column.getX());
      int localZ = this.toLocalChunkCoord(tileEntity.getZ(), column.getZ());
      WrappedBlockState blockState = this.getBlockState(column.getChunks(), minWorldHeight, localX, tileEntity.getY(), localZ);
      if (this.dungeonsEnabled && (this.stripSpawnerBlockEntity || this.strictMode && this.replaceSpawnerBlockInStrictMode)) {
         if (isBlockStateOfType(blockState, SPAWNER)) {
            return true;
         }

         String[] spawnerIds = new String[]{"minecraft:mob_spawner", "mob_spawner", "minecraft:spawner", "spawner"};
         if (this.matchesTileEntityId(tileEntity, spawnerIds)) {
            return true;
         }
      }

      if (this.strictMode && this.endGatewayEnabled) {
         if (isBlockStateOfType(blockState, END_GATEWAY)) {
            return true;
         }

         String[] endGatewayIds = new String[]{"minecraft:end_gateway", "end_gateway"};
         if (this.matchesTileEntityId(tileEntity, endGatewayIds)) {
            return true;
         }
      }

      return false;
   }

   private int obfuscateChunkBlocks(Column column, int minWorldHeight, UUID playerId, String worldName) {
      Set<StateType> triggerBlockTypes = this.activeTriggerBlockTypes;
      if (triggerBlockTypes.isEmpty()) {
         return 0;
      }

      int modifiedBlocks = 0;
      int desertWellChanges = 0;
      int desertPyramidChanges = 0;
      int jungleTempleChanges = 0;
      int swampHutChanges = 0;
      int iglooChanges = 0;
      int pillagerOutpostChanges = 0;
      BaseChunk[] sections = column.getChunks();

      for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
         BaseChunk section = sections[sectionIndex];
         if (section != null && !section.isEmpty() && this.sectionPaletteContainsAny(section, triggerBlockTypes)) {
            int sectionBaseY = minWorldHeight + (sectionIndex << 4);

            for (int localY = 0; localY < 16; localY++) {
               int worldY = sectionBaseY + localY;

               for (int localZ = 0; localZ < 16; localZ++) {
                  for (int localX = 0; localX < 16; localX++) {
                     WrappedBlockState blockState = this.getBlockState(sections, minWorldHeight, localX, worldY, localZ);
                     StateType stateType = blockState != null ? blockState.getType() : null;
                     if (this.desertPyramidProtectionEnabled && desertPyramidChanges < 2 && this.isDesertPyramidTrigger(stateType)) {
                        int changed = this.obfuscateDesertPyramid(column, minWorldHeight, localX, worldY, localZ, stateType, playerId, worldName);
                        if (changed > 0) {
                           modifiedBlocks += changed;
                           desertPyramidChanges += changed;
                           continue;
                        }
                     }

                     if (this.jungleTempleProtectionEnabled && jungleTempleChanges < 1 && this.isJungleTempleTrigger(stateType)) {
                        int changed = this.obfuscateJungleTemple(column, minWorldHeight, localX, worldY, localZ, stateType, playerId, worldName);
                        if (changed > 0) {
                           modifiedBlocks += changed;
                           jungleTempleChanges += changed;
                           continue;
                        }
                     }

                     if (this.swampHutProtectionEnabled && swampHutChanges < 1 && this.isSwampHutTrigger(stateType)) {
                        int changed = this.obfuscateSwampHut(column, minWorldHeight, localX, worldY, localZ, stateType, playerId, worldName);
                        if (changed > 0) {
                           modifiedBlocks += changed;
                           swampHutChanges += changed;
                           continue;
                        }
                     }

                     if (this.iglooProtectionEnabled && iglooChanges < 1 && isSameBlockType(stateType, CRAFTING_TABLE)) {
                        int changed = this.obfuscateIgloo(column, minWorldHeight, localX, worldY, localZ, playerId, worldName);
                        if (changed > 0) {
                           modifiedBlocks += changed;
                           iglooChanges += changed;
                           continue;
                        }
                     }

                     if (this.dungeonsEnabled && isSameBlockType(stateType, SPAWNER)) {
                        boolean spawnerReplaced = false;
                        if (this.strictMode && this.replaceSpawnerBlockInStrictMode && this.setChunkBlockState(section, localX, localY, localZ, AIR_STATE)) {
                           this.recordShadowBlockState(column, playerId, worldName, localX, localZ, AIR_STATE);
                           modifiedBlocks++;
                           spawnerReplaced = true;
                        }

                        if (this.breakFloorCount || this.strictMode && this.perturbFloorInStrictMode) {
                           modifiedBlocks += this.obfuscateDungeonFloor(column, minWorldHeight, localX, worldY, localZ, playerId, worldName);
                        }

                        if (spawnerReplaced || this.breakFloorCount) {
                           continue;
                        }
                     }

                     if (this.buriedTreasureEnabled && localX == 9 && localZ == 9 && isSameBlockType(stateType, CHEST)) {
                        modifiedBlocks += this.obfuscateBuriedTreasure(column, minWorldHeight, localX, worldY, localZ, playerId, worldName);
                     }

                     if (this.isShipwreckObfuscationEnabled() && isSameBlockType(stateType, CHEST)) {
                        int changed = this.obfuscateShipwreck(column, minWorldHeight, localX, worldY, localZ, playerId, worldName);
                        if (changed > 0) {
                           modifiedBlocks += changed;
                           continue;
                        }
                     }

                     if (this.pillagerOutpostProtectionEnabled && pillagerOutpostChanges < 1 && isSameBlockType(stateType, CHEST)) {
                        int changed = this.obfuscatePillagerOutpost(column, minWorldHeight, localX, worldY, localZ, playerId, worldName);
                        if (changed > 0) {
                           modifiedBlocks += changed;
                           pillagerOutpostChanges += changed;
                           continue;
                        }
                     }

                     if (this.strictMode
                        && this.monumentEnabled
                        && isStateInSet(blockState, MONUMENT_TARGET_BLOCKS)
                        && this.shouldPerturbMonumentBlock(column, localX, worldY, localZ)) {
                        if (this.setChunkBlockState(section, localX, localY, localZ, DARK_PRISMARINE_STATE)) {
                           this.recordShadowBlockState(column, playerId, worldName, localX, localZ, DARK_PRISMARINE_STATE);
                           modifiedBlocks++;
                        }
                     } else if (this.strictMode && this.endGatewayEnabled && isSameBlockType(stateType, END_GATEWAY)) {
                        if (this.setChunkBlockState(section, localX, localY, localZ, AIR_STATE)) {
                           this.recordShadowBlockState(column, playerId, worldName, localX, localZ, AIR_STATE);
                           modifiedBlocks++;
                        }
                     } else {
                        if (this.strictMode && this.emeraldOreEnabled) {
                           if (isSameBlockType(stateType, EMERALD_ORE) && this.setChunkBlockState(section, localX, localY, localZ, STONE_STATE)) {
                              this.recordShadowBlockState(column, playerId, worldName, localX, localZ, STONE_STATE);
                              modifiedBlocks++;
                              continue;
                           }

                           if (isSameBlockType(stateType, DEEPSLATE_EMERALD_ORE) && this.setChunkBlockState(section, localX, localY, localZ, DEEPSLATE_STATE)) {
                              this.recordShadowBlockState(column, playerId, worldName, localX, localZ, DEEPSLATE_STATE);
                              modifiedBlocks++;
                              continue;
                           }
                        }

                        if (this.strictMode && this.warpedFungusEnabled && isWarpedFungusBlock(stateType)) {
                           if (this.setChunkBlockState(section, localX, localY, localZ, NETHERRACK_STATE)) {
                              this.recordShadowBlockState(column, playerId, worldName, localX, localZ, NETHERRACK_STATE);
                              modifiedBlocks++;
                           }
                        } else if (this.desertWellEnabled
                           && desertWellChanges < 2
                           && isSameBlockType(stateType, WATER)
                           && worldY >= 48
                           && worldY <= 128
                           && this.matchesDesertWellContext(column, minWorldHeight, localX, worldY, localZ)
                           && this.setChunkBlockState(section, localX, localY, localZ, AIR_STATE)) {
                           this.recordShadowBlockState(column, playerId, worldName, localX, localZ, AIR_STATE);
                           modifiedBlocks++;
                           desertWellChanges++;
                        }
                     }
                  }
               }
            }
         }
      }

      return modifiedBlocks;
   }

   private Set buildActiveTriggerBlockTypes() {
      Set<StateType> triggerTypes = new LinkedHashSet<>();
      if (this.dungeonsEnabled && (this.breakFloorCount || this.strictMode && (this.replaceSpawnerBlockInStrictMode || this.perturbFloorInStrictMode))) {
         addIfNonNull(triggerTypes, SPAWNER);
      }

      if (this.buriedTreasureEnabled || this.isShipwreckObfuscationEnabled() || this.pillagerOutpostProtectionEnabled) {
         addIfNonNull(triggerTypes, CHEST);
      }

      if (this.desertPyramidProtectionEnabled) {
         addIfNonNull(triggerTypes, STONE_PRESSURE_PLATE);
         addIfNonNull(triggerTypes, TNT);
      }

      if (this.jungleTempleProtectionEnabled) {
         addIfNonNull(triggerTypes, REDSTONE_WIRE);
         addIfNonNull(triggerTypes, TRIPWIRE_HOOK);
      }

      if (this.swampHutProtectionEnabled) {
         addIfNonNull(triggerTypes, CAULDRON);
         addIfNonNull(triggerTypes, WATER_CAULDRON);
         addIfNonNull(triggerTypes, LAVA_CAULDRON);
         addIfNonNull(triggerTypes, POWDER_SNOW_CAULDRON);
         addIfNonNull(triggerTypes, CRAFTING_TABLE);
         addIfNonNull(triggerTypes, POTTED_RED_MUSHROOM);
      }

      if (this.iglooProtectionEnabled) {
         addIfNonNull(triggerTypes, CRAFTING_TABLE);
      }

      if (this.strictMode && this.monumentEnabled) {
         addIfNonNull(triggerTypes, PRISMARINE);
         addIfNonNull(triggerTypes, PRISMARINE_BRICKS);
         addIfNonNull(triggerTypes, SEA_LANTERN);
      }

      if (this.strictMode && this.endGatewayEnabled) {
         addIfNonNull(triggerTypes, END_GATEWAY);
      }

      if (this.strictMode && this.emeraldOreEnabled) {
         addIfNonNull(triggerTypes, EMERALD_ORE);
         addIfNonNull(triggerTypes, DEEPSLATE_EMERALD_ORE);
      }

      if (this.strictMode && this.warpedFungusEnabled) {
         addIfNonNull(triggerTypes, WARPED_STEM);
         addIfNonNull(triggerTypes, WARPED_WART_BLOCK);
         addIfNonNull(triggerTypes, SHROOMLIGHT);
      }

      if (this.desertWellEnabled) {
         addIfNonNull(triggerTypes, WATER);
      }

      return triggerTypes;
   }

   private int obfuscateDungeonFloor(Column column, int minWorldHeight, int spawnerX, int spawnerY, int spawnerZ, UUID playerId, String worldName) {
      int floorY = spawnerY - 1;
      List<int[]> floorPositions = new ArrayList<>(81);

      for (int dz = -4; dz <= 4; dz++) {
         int blockZ = spawnerZ + dz;
         if (blockZ >= 0 && blockZ <= 15) {
            for (int dx = -4; dx <= 4; dx++) {
               int blockX = spawnerX + dx;
               if (blockX >= 0 && blockX <= 15) {
                  WrappedBlockState blockState = this.getBlockState(column.getChunks(), minWorldHeight, blockX, floorY, blockZ);
                  if (isStateInSet(blockState, DUNGEON_FLOOR_BLOCKS)) {
                     floorPositions.add(new int[]{blockX, floorY, blockZ});
                  }
               }
            }
         }
      }

      boolean nearChunkBorder = spawnerX < 4 || spawnerX > 11 || spawnerZ < 4 || spawnerZ > 11;
      if (floorPositions.isEmpty()) {
         return 0;
      }

      int targetRemainingCount = Math.max(0, this.floorTargetCount);
      int blocksToReplace = nearChunkBorder ? floorPositions.size() : floorPositions.size() - targetRemainingCount;
      if (blocksToReplace <= 0) {
         return 0;
      }

      int modifiedCount = 0;

      for (int[] pos : floorPositions) {
         if (modifiedCount >= blocksToReplace) {
            break;
         }

         if (this.setWorldBlockState(column.getChunks(), minWorldHeight, pos[0], pos[1], pos[2], STONE_STATE)) {
            this.recordShadowBlockState(column, playerId, worldName, pos[0], pos[2], STONE_STATE);
            modifiedCount++;
         }
      }

      return modifiedCount;
   }

   private int obfuscateDesertPyramid(
      Column column, int minWorldHeight, int localX, int worldY, int localZ, StateType triggerType, UUID playerId, String worldName
   ) {
      if (!this.matchesDesertPyramidContext(column, minWorldHeight, localX, worldY, localZ)) {
         return 0;
      }

      WrappedBlockState replacementState = isSameBlockType(triggerType, TNT) ? AIR_STATE : SANDSTONE_STATE;
      return this.replaceBlockAndRecord(column, minWorldHeight, localX, worldY, localZ, replacementState, playerId, worldName) ? 1 : 0;
   }

   private int obfuscateJungleTemple(
      Column column, int minWorldHeight, int localX, int worldY, int localZ, StateType triggerType, UUID playerId, String worldName
   ) {
      if (!this.matchesJungleTempleContext(column, minWorldHeight, localX, worldY, localZ)) {
         return 0;
      }

      if (isSameBlockType(triggerType, REDSTONE_WIRE)) {
         if (this.replaceBlockAndRecord(column, minWorldHeight, localX, worldY, localZ, COBBLESTONE_STATE, playerId, worldName)) {
            return 1;
         }
      }

      int[] redstonePos = this.findNearbyBlock(column, minWorldHeight, localX, worldY, localZ, 6, -3, 3, REDSTONE_WIRE);
      if (redstonePos != null) {
         if (this.replaceBlockAndRecord(column, minWorldHeight, redstonePos[0], redstonePos[1], redstonePos[2], COBBLESTONE_STATE, playerId, worldName)) {
            return 1;
         }
      }

      int[] mossyPos = this.findNearbyBlock(column, minWorldHeight, localX, worldY, localZ, 6, -3, 3, MOSSY_COBBLESTONE);
      if (mossyPos != null) {
         if (this.replaceBlockAndRecord(column, minWorldHeight, mossyPos[0], mossyPos[1], mossyPos[2], COBBLESTONE_STATE, playerId, worldName)) {
            return 1;
         }
      }

      return 0;
   }

   private int obfuscateSwampHut(
      Column column, int minWorldHeight, int localX, int worldY, int localZ, StateType triggerType, UUID playerId, String worldName
   ) {
      if (!this.matchesSwampHutContext(column, minWorldHeight, localX, worldY, localZ)) {
         return 0;
      }

      int[] planksPos = this.findNearbyBlock(column, minWorldHeight, localX, worldY, localZ, 6, -3, 3, SPRUCE_PLANKS);
      if (planksPos != null) {
         if (this.replaceBlockAndRecord(column, minWorldHeight, planksPos[0], planksPos[1], planksPos[2], OAK_PLANKS_STATE, playerId, worldName)) {
            return 1;
         }
      }

      if (isTypeInSet(triggerType, CAULDRON_BLOCKS)) {
         if (this.replaceBlockAndRecord(column, minWorldHeight, localX, worldY, localZ, AIR_STATE, playerId, worldName)) {
            return 1;
         }
      }

      int[] cauldronPos = this.findNearbyBlockInSet(column, minWorldHeight, localX, worldY, localZ, 6, -3, 3, CAULDRON_BLOCKS);
      if (cauldronPos != null) {
         if (this.replaceBlockAndRecord(column, minWorldHeight, cauldronPos[0], cauldronPos[1], cauldronPos[2], AIR_STATE, playerId, worldName)) {
            return 1;
         }
      }

      return 0;
   }

   private int obfuscateIgloo(Column column, int minWorldHeight, int localX, int worldY, int localZ, UUID playerId, String worldName) {
      if (!this.matchesIglooContext(column, minWorldHeight, localX, worldY, localZ)) {
         return 0;
      }

      int[] icePos = this.findNearbyBlock(column, minWorldHeight, localX, worldY, localZ, 6, -3, 3, ICE);
      if (icePos != null) {
         if (this.replaceBlockAndRecord(column, minWorldHeight, icePos[0], icePos[1], icePos[2], SNOW_BLOCK_STATE, playerId, worldName)) {
            return 1;
         }
      }

      return 0;
   }

   private int obfuscatePillagerOutpost(Column column, int minWorldHeight, int localX, int worldY, int localZ, UUID playerId, String worldName) {
      if (!this.matchesPillagerOutpostContext(column, minWorldHeight, localX, worldY, localZ)) {
         return 0;
      }

      int[] birchPos = this.findNearbyBlock(column, minWorldHeight, localX, worldY, localZ, 8, -16, -8, BIRCH_PLANKS);
      if (birchPos != null) {
         if (this.replaceBlockAndRecord(column, minWorldHeight, birchPos[0], birchPos[1], birchPos[2], OAK_PLANKS_STATE, playerId, worldName)) {
            return 1;
         }
      }

      return 0;
   }

   private int obfuscateBuriedTreasure(Column column, int minWorldHeight, int localX, int worldY, int localZ, UUID playerId, String worldName) {
      WrappedBlockState blockState = this.getBlockState(column.getChunks(), minWorldHeight, localX, worldY - 1, localZ);
      if (!isStateInSet(blockState, BURIED_TREASURE_FLOOR_BLOCKS)) {
         return 0;
      } else if (this.setWorldBlockState(column.getChunks(), minWorldHeight, localX, worldY - 1, localZ, DIRT_STATE)) {
         this.recordShadowBlockState(column, playerId, worldName, localX, localZ, DIRT_STATE);
         return 1;
      } else {
         return 0;
      }
   }

   private int obfuscateShipwreck(Column column, int minWorldHeight, int localX, int worldY, int localZ, UUID playerId, String worldName) {
      int stairsCount = 0;
      List<int[]> trapdoorPositions = new ArrayList<>(8);

      for (int dy = -1; dy <= 2; dy++) {
         for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
               int checkX = localX + dx;
               int checkZ = localZ + dz;
               if (checkX >= 0 && checkX <= 15 && checkZ >= 0 && checkZ <= 15) {
                  WrappedBlockState blockState = this.getBlockState(column.getChunks(), minWorldHeight, checkX, worldY + dy, checkZ);
                  StateType stateType = blockState != null ? blockState.getType() : null;
                  if (isWoodenStairs(stateType)) {
                     stairsCount++;
                  } else if (isWoodenTrapdoor(stateType)) {
                     trapdoorPositions.add(new int[]{checkX, worldY + dy, checkZ});
                  }
               }
            }
         }
      }

      if (stairsCount >= 2 && trapdoorPositions.size() >= 2) {
         int logCount = 0;

         for (int dy = -3; dy <= 3; dy++) {
            for (int dz = -3; dz <= 3; dz++) {
               for (int dx = -3; dx <= 3; dx++) {
                  int checkX = localX + dx;
                  int checkZ = localZ + dz;
                  if (checkX >= 0 && checkX <= 15 && checkZ >= 0 && checkZ <= 15) {
                     WrappedBlockState logState = this.getBlockState(column.getChunks(), minWorldHeight, checkX, worldY + dy, checkZ);
                     if (logState != null && isWoodenLog(logState.getType())) {
                        if (++logCount >= 5) {
                           break;
                        }
                     }
                  }
               }

               if (logCount >= 5) {
                  break;
               }
            }

            if (logCount >= 5) {
               break;
            }
         }

         if (logCount < 5) {
            return 0;
         }

         int modifiedCount = 0;

         for (int[] pos : trapdoorPositions) {
            if (this.setWorldBlockState(column.getChunks(), minWorldHeight, pos[0], pos[1], pos[2], WATER_STATE)) {
               this.recordShadowBlockState(column, playerId, worldName, pos[0], pos[2], WATER_STATE);
               modifiedCount++;
            }
         }

         return modifiedCount;
      } else {
         return 0;
      }
   }

   private boolean shouldPerturbMonumentBlock(Column column, int localX, int worldY, int localZ) {
      int worldX = (column.getX() << 4) + localX;
      int worldZ = (column.getZ() << 4) + localZ;
      int hash = worldX * 31 + worldY * 131 + worldZ * 17;
      return (hash & 7) == 0;
   }

   private boolean isShipwreckObfuscationEnabled() {
      return this.strictMode && this.decoratorShipwreckEnabled || this.structureShipwreckProtectionEnabled;
   }

   private boolean matchesDesertPyramidContext(Column column, int minWorldHeight, int localX, int worldY, int localZ) {
      int tntCount = this.countNearbyBlocks(column, minWorldHeight, localX, worldY, localZ, 3, -3, 3, TNT);
      int pyramidBlockCount = this.countNearbyBlocksInSet(column, minWorldHeight, localX, worldY, localZ, 6, -4, 8, DESERT_PYRAMID_BLOCKS);
      boolean hasPressurePlate = this.hasNearbyBlock(column, minWorldHeight, localX, worldY, localZ, 3, -3, 3, STONE_PRESSURE_PLATE);
      return tntCount >= 3 && hasPressurePlate && pyramidBlockCount >= 18;
   }

   private boolean matchesJungleTempleContext(Column column, int minWorldHeight, int localX, int worldY, int localZ) {
      int trapBlockCount = this.countNearbyBlocksInSet(column, minWorldHeight, localX, worldY, localZ, 7, -3, 3, JUNGLE_TEMPLE_TRAP_BLOCKS);
      int stoneBlockCount = this.countNearbyBlocksInSet(column, minWorldHeight, localX, worldY, localZ, 8, -4, 4, JUNGLE_TEMPLE_STONE_BLOCKS);
      boolean hasTripwireHook = this.hasNearbyBlock(column, minWorldHeight, localX, worldY, localZ, 7, -3, 3, TRIPWIRE_HOOK);
      boolean hasTrapMechanism = this.hasNearbyBlock(column, minWorldHeight, localX, worldY, localZ, 7, -3, 3, STICKY_PISTON)
         || this.hasNearbyBlock(column, minWorldHeight, localX, worldY, localZ, 7, -3, 3, REPEATER)
         || this.hasNearbyBlock(column, minWorldHeight, localX, worldY, localZ, 7, -3, 3, TRIPWIRE);
      return trapBlockCount >= 5 && stoneBlockCount >= 6 && hasTripwireHook && hasTrapMechanism;
   }

   private boolean matchesSwampHutContext(Column column, int minWorldHeight, int localX, int worldY, int localZ) {
      boolean hasCauldron = this.findNearbyBlockInSet(column, minWorldHeight, localX, worldY, localZ, 6, -3, 3, CAULDRON_BLOCKS) != null;
      boolean hasCraftingTable = this.hasNearbyBlock(column, minWorldHeight, localX, worldY, localZ, 6, -3, 3, CRAFTING_TABLE);
      boolean hasPottedMushroom = this.hasNearbyBlock(column, minWorldHeight, localX, worldY, localZ, 6, -3, 3, POTTED_RED_MUSHROOM);
      int woodBlockCount = this.countNearbyBlocksInSet(column, minWorldHeight, localX, worldY, localZ, 7, -4, 5, SWAMP_HUT_WOOD_BLOCKS);
      return hasCauldron && hasCraftingTable && hasPottedMushroom && woodBlockCount >= 10;
   }

   private boolean matchesIglooContext(Column column, int minWorldHeight, int localX, int worldY, int localZ) {
      boolean hasIce = this.hasNearbyBlock(column, minWorldHeight, localX, worldY, localZ, 7, -3, 3, ICE);
      int snowBlockCount = this.countNearbyBlocks(column, minWorldHeight, localX, worldY, localZ, 8, -3, 4, SNOW_BLOCK);
      return hasIce && snowBlockCount >= 10;
   }

   private boolean matchesPillagerOutpostContext(Column column, int minWorldHeight, int localX, int worldY, int localZ) {
      int birchPlanksCount = this.countNearbyBlocks(column, minWorldHeight, localX, worldY, localZ, 8, -16, -8, BIRCH_PLANKS);
      return birchPlanksCount >= 10;
   }

   private boolean replaceBlockAndRecord(
      Column column, int minWorldHeight, int localX, int worldY, int localZ, WrappedBlockState replacementState, UUID playerId, String worldName
   ) {
      if (this.setWorldBlockState(column.getChunks(), minWorldHeight, localX, worldY, localZ, replacementState)) {
         this.recordShadowBlockState(column, playerId, worldName, localX, localZ, replacementState);
         return true;
      } else {
         return false;
      }
   }

   private int[] findNearbyBlock(
      Column column, int minWorldHeight, int centerX, int centerY, int centerZ, int horizontalRadius, int minDy, int maxDy, StateType targetType
   ) {
      for (int dy = minDy; dy <= maxDy; dy++) {
         int checkY = centerY + dy;

         for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
            int checkZ = centerZ + dz;
            if (checkZ >= 0 && checkZ <= 15) {
               for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
                  int checkX = centerX + dx;
                  if (checkX >= 0 && checkX <= 15) {
                     WrappedBlockState blockState = this.getBlockState(column.getChunks(), minWorldHeight, checkX, checkY, checkZ);
                     if (isBlockStateOfType(blockState, targetType)) {
                        return new int[]{checkX, checkY, checkZ};
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   private int[] findNearbyBlockInSet(
      Column column, int minWorldHeight, int centerX, int centerY, int centerZ, int horizontalRadius, int minDy, int maxDy, Set targetTypes
   ) {
      for (int dy = minDy; dy <= maxDy; dy++) {
         int checkY = centerY + dy;

         for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
            int checkZ = centerZ + dz;
            if (checkZ >= 0 && checkZ <= 15) {
               for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
                  int checkX = centerX + dx;
                  if (checkX >= 0 && checkX <= 15) {
                     WrappedBlockState blockState = this.getBlockState(column.getChunks(), minWorldHeight, checkX, checkY, checkZ);
                     if (isStateInSet(blockState, targetTypes)) {
                        return new int[]{checkX, checkY, checkZ};
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   private boolean hasNearbyBlock(
      Column column, int minWorldHeight, int centerX, int centerY, int centerZ, int horizontalRadius, int minDy, int maxDy, StateType targetType
   ) {
      return this.findNearbyBlock(column, minWorldHeight, centerX, centerY, centerZ, horizontalRadius, minDy, maxDy, targetType) != null;
   }

   private int countNearbyBlocks(
      Column column, int minWorldHeight, int centerX, int centerY, int centerZ, int horizontalRadius, int minDy, int maxDy, StateType targetType
   ) {
      int matchCount = 0;

      for (int dy = minDy; dy <= maxDy; dy++) {
         int checkY = centerY + dy;

         for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
            int checkZ = centerZ + dz;
            if (checkZ >= 0 && checkZ <= 15) {
               for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
                  int checkX = centerX + dx;
                  if (checkX >= 0 && checkX <= 15) {
                     WrappedBlockState blockState = this.getBlockState(column.getChunks(), minWorldHeight, checkX, checkY, checkZ);
                     if (isBlockStateOfType(blockState, targetType)) {
                        matchCount++;
                     }
                  }
               }
            }
         }
      }

      return matchCount;
   }

   private int countNearbyBlocksInSet(
      Column column, int minWorldHeight, int centerX, int centerY, int centerZ, int horizontalRadius, int minDy, int maxDy, Set targetTypes
   ) {
      int matchCount = 0;

      for (int dy = minDy; dy <= maxDy; dy++) {
         int checkY = centerY + dy;

         for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
            int checkZ = centerZ + dz;
            if (checkZ >= 0 && checkZ <= 15) {
               for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
                  int checkX = centerX + dx;
                  if (checkX >= 0 && checkX <= 15 && isStateInSet(this.getBlockState(column.getChunks(), minWorldHeight, checkX, checkY, checkZ), targetTypes)) {
                     matchCount++;
                  }
               }
            }
         }
      }

      return matchCount;
   }

   private void recordShadowBlockState(Column column, UUID playerId, String worldName, int localX, int localZ, WrappedBlockState replacementState) {
      if (this.shadowState != null && replacementState != null) {
         int worldX = (column.getX() << 4) + localX;
         int worldZ = (column.getZ() << 4) + localZ;
         this.shadowState.putBlockState(playerId, worldName, worldX, worldZ, replacementState);
      }
   }

   private void recordStrippedTileEntity(Column column, UUID playerId, String worldName, TileEntity tileEntity) {
      if (this.shadowState != null && tileEntity != null) {
         int worldX = this.toWorldCoord(tileEntity.getX(), column.getX());
         int worldZ = this.toWorldCoord(tileEntity.getZ(), column.getZ());
         this.shadowState.markBlockEntityHidden(playerId, worldName, worldX, worldZ);
      }
   }

   private int toLocalChunkCoord(int coord, int chunkCoord) {
      return coord >= 0 && coord <= 15 ? coord : coord - (chunkCoord << 4);
   }

   private int toWorldCoord(int coord, int chunkCoord) {
      return coord >= 0 && coord <= 15 ? (chunkCoord << 4) + coord : coord;
   }

   private boolean matchesDesertWellContext(Column column, int minWorldHeight, int localX, int worldY, int localZ) {
      if (localX >= 2 && localX <= 13 && localZ >= 2 && localZ <= 13) {
         int sandstoneCount = 0;
         int waterCount = 0;

         for (int dy = -1; dy <= 3; dy++) {
            for (int dz = -2; dz <= 2; dz++) {
               for (int dx = -2; dx <= 2; dx++) {
                  WrappedBlockState blockState = this.getBlockState(column.getChunks(), minWorldHeight, localX + dx, worldY + dy, localZ + dz);
                  if (isStateInSet(blockState, DESERT_WELL_SANDSTONE_BLOCKS)) {
                     sandstoneCount++;
                  } else if (isBlockStateOfType(blockState, WATER)) {
                     waterCount++;
                  }
               }
            }
         }

         return waterCount >= 1 && waterCount <= 6 && sandstoneCount >= 18;
      } else {
         return false;
      }
   }

   private Column copyColumnWithTileEntities(Column column, TileEntity[] tileEntities) {
      if (column.hasHeightMaps()) {
         if (column.hasBiomeData()) {
            return column.getBiomeDataBytes() != null
               ? new Column(column.getX(), column.getZ(), column.isFullChunk(), column.getChunks(), tileEntities, column.getHeightMaps(), column.getBiomeDataBytes())
               : new Column(column.getX(), column.getZ(), column.isFullChunk(), column.getChunks(), tileEntities, column.getHeightMaps(), column.getBiomeDataInts());
         } else {
            return new Column(column.getX(), column.getZ(), column.isFullChunk(), column.getChunks(), tileEntities, column.getHeightMaps());
         }
      } else if (column.hasBiomeData()) {
         return column.getBiomeDataBytes() != null
            ? new Column(column.getX(), column.getZ(), column.isFullChunk(), column.getChunks(), tileEntities, column.getBiomeDataBytes())
            : new Column(column.getX(), column.getZ(), column.isFullChunk(), column.getChunks(), tileEntities, column.getBiomeDataInts());
      } else {
         return new Column(column.getX(), column.getZ(), column.isFullChunk(), column.getChunks(), tileEntities);
      }
   }

   private WrappedBlockState getBlockState(BaseChunk[] sections, int minWorldHeight, int localX, int worldY, int localZ) {
      int sectionIndex = worldY - minWorldHeight >> 4;
      if (localX >= 0 && localX <= 15 && localZ >= 0 && localZ <= 15 && sectionIndex >= 0 && sectionIndex < sections.length) {
         BaseChunk section = sections[sectionIndex];
         if (section != null && !section.isEmpty()) {
            DecoratorSeedObfuscator.ChunkSectionStateCache stateCache = this.sectionStateCache.get();
            int localY = worldY & 15;
            return stateCache.getBlockState(sections, section, sectionIndex, localX, localY, localZ);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private boolean matchesTileEntityId(TileEntity tileEntity, String[] expectedIds) {
      NBTCompound nbt = tileEntity.getNBT();
      if (nbt == null) {
         return false;
      }

      String tileEntityId = nbt.getStringTagValueOrDefault("id", "").toLowerCase(Locale.ROOT);

      for (String expectedId : expectedIds) {
         if (tileEntityId.equals(expectedId)) {
            return true;
         }
      }

      return false;
   }

   private static boolean isWoodenStairs(StateType stateType) {
      return stateType != null && WOODEN_STAIRS_BLOCKS.contains(stateType);
   }

   private static boolean isWoodenTrapdoor(StateType stateType) {
      return stateType != null && WOODEN_TRAPDOOR_BLOCKS.contains(stateType);
   }

   private static boolean isWoodenLog(StateType stateType) {
      return stateType != null && WOODEN_LOG_BLOCKS.contains(stateType);
   }

   private static String normalizeStructureStrategy(String strategy) {
      if (strategy != null && !strategy.isBlank()) {
         String normalized = strategy.trim().toLowerCase(Locale.ROOT);
         return !"aggressive".equals(normalized)
               && !"legacy-signature".equals(normalized)
               && !"aggressive-signature".equals(normalized)
               && !"broad-signature".equals(normalized)
            ? "minimal"
            : "aggressive";
      } else {
         return "minimal";
      }
   }

   private static boolean isAggressiveStrategy(String strategy) {
      return "aggressive".equals(strategy);
   }

   private static boolean isTypeInSet(StateType stateType, Set targetTypes) {
      return stateType != null && targetTypes.contains(stateType);
   }

   private static boolean isStateInSet(WrappedBlockState blockState, Set targetTypes) {
      return blockState != null && blockState.getType() != null && targetTypes.contains(blockState.getType());
   }

   private static boolean isSameBlockType(StateType first, StateType second) {
      return first != null && second != null && first.equals(second);
   }

   private static void addIfNonNull(Set targetSet, StateType stateType) {
      if (stateType != null) {
         targetSet.add(stateType);
      }
   }

   private static Set createBlockTypeSet(StateType[] stateTypes) {
      Set<StateType> blockTypes = new LinkedHashSet<>();

      for (StateType stateType : stateTypes) {
         addIfNonNull(blockTypes, stateType);
      }

      return Set.copyOf(blockTypes);
   }

   private static StateType resolveStateType(String blockName) {
      StateType resolved = StateTypes.getByName(blockName);
      if (resolved != null) {
         return resolved;
      }

      String namespaced = blockName.contains(":") ? blockName : "minecraft:" + blockName;
      resolved = StateTypes.getByName(namespaced);
      if (resolved != null) {
         return resolved;
      }

      int colonIndex = namespaced.indexOf(58);
      return colonIndex >= 0 ? StateTypes.getByName(namespaced.substring(colonIndex + 1)) : null;
   }

   private static WrappedBlockState createDefaultBlockState(StateType primaryType, StateType fallbackType) {
      if (primaryType != null) {
         return WrappedBlockState.getDefaultState(primaryType);
      } else {
         return fallbackType != null ? WrappedBlockState.getDefaultState(fallbackType) : null;
      }
   }

   private boolean isDesertPyramidTrigger(StateType stateType) {
      return isSameBlockType(stateType, STONE_PRESSURE_PLATE) || isSameBlockType(stateType, TNT);
   }

   private boolean isJungleTempleTrigger(StateType stateType) {
      return isSameBlockType(stateType, REDSTONE_WIRE) || isSameBlockType(stateType, TRIPWIRE_HOOK);
   }

   private boolean isSwampHutTrigger(StateType stateType) {
      return isTypeInSet(stateType, CAULDRON_BLOCKS) || isSameBlockType(stateType, CRAFTING_TABLE) || isSameBlockType(stateType, POTTED_RED_MUSHROOM);
   }

   private boolean sectionPaletteContainsAny(BaseChunk section, Set targetTypes) {
      if (section instanceof Chunk_v1_18 modernSection) {
         Palette palette = modernSection.getChunkData() != null ? modernSection.getChunkData().palette : null;
         if (palette != null && palette.size() <= 4096) {
            for (int i = 0; i < palette.size(); i++) {
               WrappedBlockState blockState = WrappedBlockState.getByGlobalId(palette.idToState(i));
               if (isStateInSet(blockState, targetTypes)) {
                  return true;
               }
            }

            return false;
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private boolean setSectionBlockState(BaseChunk[] sections, BaseChunk section, int localX, int localY, int localZ, WrappedBlockState targetState) {
      if (targetState == null) {
         return false;
      }

      WrappedBlockState currentState = section.get(localX, localY, localZ);
      if (currentState != null && isSameBlockType(currentState.getType(), targetState.getType())) {
         return false;
      }

      section.set(localX, localY, localZ, targetState.clone());
      this.sectionStateCache.get();
      return true;
   }

   private static boolean isWarpedFungusBlock(StateType stateType) {
      return isSameBlockType(stateType, WARPED_STEM) || isSameBlockType(stateType, WARPED_WART_BLOCK) || isSameBlockType(stateType, SHROOMLIGHT);
   }

   private static boolean isBlockStateOfType(WrappedBlockState blockState, StateType stateType) {
      return blockState != null && isSameBlockType(blockState.getType(), stateType);
   }

   private boolean setWorldBlockState(BaseChunk[] sections, int minWorldHeight, int localX, int worldY, int localZ, WrappedBlockState targetState) {
      int sectionIndex = worldY - minWorldHeight >> 4;
      if (localX >= 0 && localX <= 15 && localZ >= 0 && localZ <= 15 && sectionIndex >= 0 && sectionIndex < sections.length) {
         BaseChunk section = sections[sectionIndex];
         return section == null ? false : this.setSectionBlockState(sections, section, localX, worldY & 15, localZ, targetState);
      } else {
         return false;
      }
   }

   private boolean setChunkBlockState(BaseChunk section, int localX, int localY, int localZ, WrappedBlockState targetState) {
      return this.setSectionBlockState(null, section, localX, localY, localZ, targetState);
   }

   static {
      StateType[] woodenStairs = new StateType[]{
         resolveStateType("oak_stairs"),
         resolveStateType("spruce_stairs"),
         resolveStateType("birch_stairs"),
         resolveStateType("jungle_stairs"),
         resolveStateType("acacia_stairs"),
         resolveStateType("dark_oak_stairs"),
         resolveStateType("mangrove_stairs"),
         resolveStateType("cherry_stairs"),
         resolveStateType("bamboo_stairs")
      };
      WOODEN_STAIRS_BLOCKS = createBlockTypeSet(woodenStairs);
      StateType[] woodenTrapdoors = new StateType[]{
         resolveStateType("oak_trapdoor"),
         resolveStateType("spruce_trapdoor"),
         resolveStateType("birch_trapdoor"),
         resolveStateType("jungle_trapdoor"),
         resolveStateType("acacia_trapdoor"),
         resolveStateType("dark_oak_trapdoor"),
         resolveStateType("mangrove_trapdoor"),
         resolveStateType("cherry_trapdoor"),
         resolveStateType("bamboo_trapdoor")
      };
      WOODEN_TRAPDOOR_BLOCKS = createBlockTypeSet(woodenTrapdoors);
      StateType[] woodenLogs = new StateType[]{
         resolveStateType("oak_log"),
         resolveStateType("spruce_log"),
         resolveStateType("birch_log"),
         resolveStateType("jungle_log"),
         resolveStateType("acacia_log"),
         resolveStateType("dark_oak_log"),
         resolveStateType("mangrove_log"),
         resolveStateType("cherry_log"),
         resolveStateType("oak_wood"),
         resolveStateType("spruce_wood"),
         resolveStateType("dark_oak_wood"),
         resolveStateType("stripped_oak_log"),
         resolveStateType("stripped_spruce_log"),
         resolveStateType("stripped_dark_oak_log")
      };
      WOODEN_LOG_BLOCKS = createBlockTypeSet(woodenLogs);
   }

   private static final class ChunkSectionStateCache {
      private BaseChunk[] owner;
      private WrappedBlockState[][] states = new WrappedBlockState[0][];

      public WrappedBlockState getBlockState(BaseChunk[] sections, BaseChunk section, int sectionIndex, int localX, int localY, int localZ) {
         if (this.owner != sections) {
            this.owner = sections;
            this.states = new WrappedBlockState[sections.length][];
         }

         WrappedBlockState[] sectionStates = this.states[sectionIndex];
         if (sectionStates == null) {
            sectionStates = new WrappedBlockState[4096];
            this.states[sectionIndex] = sectionStates;
            int flatIndex = 0;

            for (int y = 0; y < 16; y++) {
               for (int z = 0; z < 16; z++) {
                  for (int x = 0; x < 16; x++) {
                     sectionStates[flatIndex++] = section.get(x, y, z);
                  }
               }
            }
         }

         return sectionStates[localY << 8 | localZ << 4 | localX];
      }
   }

   public static final class PacketObfuscationResult {
      private static final DecoratorSeedObfuscator.PacketObfuscationResult NONE = new DecoratorSeedObfuscator.PacketObfuscationResult(0, 0);
      private final int modifiedBlocks;
      private final int strippedBlockEntities;

      private PacketObfuscationResult(int modifiedBlocks, int strippedBlockEntities) {
         this.modifiedBlocks = modifiedBlocks;
         this.strippedBlockEntities = strippedBlockEntities;
      }

      public int getModifiedBlocks() {
         return this.modifiedBlocks;
      }

      public int getStrippedBlockEntities() {
         return this.strippedBlockEntities;
      }

      public boolean hasChanges() {
         return this.modifiedBlocks > 0 || this.strippedBlockEntities > 0;
      }
   }
}
