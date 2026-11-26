package sakuyaayane.nekoantixray.chunk;

import io.papermc.paper.antixray.BitStorageReader;
import io.papermc.paper.antixray.BitStorageWriter;
import io.papermc.paper.antixray.ChunkPacketBlockController;
import io.papermc.paper.antixray.ChunkPacketInfo;
import io.papermc.paper.configuration.type.EngineMode;
import io.papermc.paper.configuration.WorldConfiguration.Anticheat.AntiXray;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntSupplier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block.UpdateFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.GlobalPalette;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import sakuyaayane.nekoantixray.NekoAntiXray;
import sakuyaayane.nekoantixray.util.LongHashSet;
import sakuyaayane.nekoantixray.world.BlockStateIdMapper;
import sakuyaayane.nekoantixray.world.ChunkCoordinateCodec;
import sakuyaayane.nekoantixray.world.PlayerChunkKey;
import sakuyaayane.nekoantixray.world.TrackedChunkVisibility;

public final class EnhancedChunkObfuscationController extends ChunkPacketBlockController {
   public static final Palette<BlockState> GLOBAL_PALETTE = new GlobalPalette<>(Block.BLOCK_STATE_REGISTRY);
   private final NekoAntiXray plugin;
   private final String worldName;
   private final Executor executor;
   private final EngineMode engineMode;
   private final int maxBlockHeight;
   private final int updateRadius;
   private final boolean usePermission;
   public final boolean rayTraceThirdPerson;
   public final double rayTraceDistance;
   public final boolean rehideBlocks;
   public final double rehideDistance;
   private final int maxRayTraceBlockCountPerChunk;
   private final BlockState[] presetBlockStates;
   private final BlockState[] presetBlockStatesFull;
   private final BlockState[] presetBlockStatesStone;
   private final BlockState[] presetBlockStatesDeepslate;
   private final BlockState[] presetBlockStatesNetherrack;
   private final BlockState[] presetBlockStatesEndStone;
   private final int[] presetBlockStateGlobalIds;
   private final int[] presetStoneGlobalIds;
   private final int[] presetDeepslateGlobalIds;
   private final int[] presetNetherrackGlobalIds;
   private final int[] presetEndStoneGlobalIds;
   public final boolean[] solidGlobal = new boolean[Block.BLOCK_STATE_REGISTRY.size()];
   private final boolean[] obfuscateGlobal = new boolean[Block.BLOCK_STATE_REGISTRY.size()];
   private final boolean[] rayTraceGlobal;
   private final boolean[] blockEntityGlobal = new boolean[Block.BLOCK_STATE_REGISTRY.size()];
   private final LevelChunkSection[] emptyNearbyChunkSections = new LevelChunkSection[]{null, null, null, null};
   private final int maxBlockHeightUpdatePosition;
   private final LongHashSet fakeOreBlockPositions;
   private final LongHashSet fakeOreChunkKeys;
   private final ThreadLocal<EnhancedChunkObfuscationController.SectionSolidCache> sectionSolidCache = ThreadLocal.withInitial(
      () -> new EnhancedChunkObfuscationController.SectionSolidCache()
   );
   private static final ThreadLocal<BitStorageReader> BIT_STORAGE_READER = ThreadLocal.withInitial(() -> new BitStorageReader());
   private static final ThreadLocal<BitStorageWriter> BIT_STORAGE_WRITER = ThreadLocal.withInitial(() -> new BitStorageWriter());
   private static final ThreadLocal<LevelChunkSection[]> NEARBY_CHUNK_SECTIONS = ThreadLocal.withInitial(() -> new LevelChunkSection[4]);
   private final ThreadLocal<int[]> presetBlockStateBits = ThreadLocal.withInitial(() -> new int[this.getPresetBlockStatesFullLength()]);
   private static final ThreadLocal<boolean[]> SOLID_PALETTE_FLAGS = ThreadLocal.withInitial(() -> new boolean[Block.BLOCK_STATE_REGISTRY.size()]);
   private static final ThreadLocal<boolean[]> OBFUSCATE_PALETTE_FLAGS = ThreadLocal.withInitial(() -> new boolean[Block.BLOCK_STATE_REGISTRY.size()]);
   private static final ThreadLocal<boolean[]> RAY_TRACE_PALETTE_FLAGS = ThreadLocal.withInitial(() -> new boolean[Block.BLOCK_STATE_REGISTRY.size()]);
   private static final ThreadLocal<boolean[]> BLOCK_ENTITY_PALETTE_FLAGS = ThreadLocal.withInitial(() -> new boolean[Block.BLOCK_STATE_REGISTRY.size()]);
   private static final ThreadLocal<boolean[][]> CURRENT_LAYER_TRANSPARENT = ThreadLocal.withInitial(() -> new boolean[16][16]);
   private static final ThreadLocal<boolean[][]> NEXT_LAYER_TRANSPARENT = ThreadLocal.withInitial(() -> new boolean[16][16]);
   private static final ThreadLocal<boolean[][]> NEXT_NEXT_LAYER_TRANSPARENT = ThreadLocal.withInitial(() -> new boolean[16][16]);
   private static final ThreadLocal<boolean[][]> LAYER_RAY_TRACE_MASK = ThreadLocal.withInitial(() -> new boolean[16][16]);
   private static final ThreadLocal<boolean[][]> LAYER_BLOCK_ENTITY_MASK = ThreadLocal.withInitial(() -> new boolean[16][16]);
   private static final Field BLOCK_ENTITIES_DATA_FIELD;
   private static final Field BLOCK_ENTITY_PACKED_XZ_FIELD;
   private static final Field BLOCK_ENTITY_Y_FIELD;

   public NekoAntiXray getPlugin() {
      return this.plugin;
   }

   public EnhancedChunkObfuscationController(
      NekoAntiXray plugin,
      boolean rayTraceThirdPerson,
      double rayTraceDistance,
      boolean rehideBlocks,
      double rehideDistance,
      int maxRayTraceBlockCountPerChunk,
      Iterable rayTraceBlockNames,
      Level level,
      Map fakeOreMap,
      Executor executor
   ) {
      this.plugin = plugin;
      this.worldName = level.getWorld().getName();
      this.executor = executor;
      AntiXray antiXray = level.paperConfig().anticheat.antiXray;
      this.engineMode = antiXray.engineMode;
      int defaultMaxBlockHeight = antiXray.maxBlockHeight >> 4 << 4;
      this.maxBlockHeight = plugin.getConfig()
            .getInt(
               "world-settings." + this.worldName + ".anti-xray.max-block-height",
               plugin.getConfig().getInt("world-settings.default.anti-xray.max-block-height", defaultMaxBlockHeight)
            )
         >> 4
         << 4;
      this.updateRadius = antiXray.updateRadius;
      this.usePermission = antiXray.usePermission;
      this.rayTraceThirdPerson = rayTraceThirdPerson;
      this.rayTraceDistance = rayTraceDistance;
      this.rehideBlocks = rehideBlocks;
      this.rehideDistance = rehideDistance;
      this.maxRayTraceBlockCountPerChunk = maxRayTraceBlockCountPerChunk;
      if (fakeOreMap != null && !fakeOreMap.isEmpty()) {
         this.fakeOreBlockPositions = new LongHashSet(fakeOreMap.size());
         this.fakeOreChunkKeys = new LongHashSet(Math.min(fakeOreMap.size(), 256));

         for (Object rawLocation : fakeOreMap.keySet()) {
            Location location = (Location)rawLocation;
            this.fakeOreBlockPositions.add(BlockPos.asLong(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
            int chunkX = location.getBlockX() >> 4;
            int chunkZ = location.getBlockZ() >> 4;
            this.fakeOreChunkKeys.add(ChunkCoordinateCodec.encode(chunkX, chunkZ));
         }
      } else {
         this.fakeOreBlockPositions = null;
         this.fakeOreChunkKeys = null;
      }

      List paperHiddenBlocks = antiXray.hiddenBlocks;
      List<Block> resolvedHiddenBlocks = this.resolveHiddenBlocks(rayTraceBlockNames, paperHiddenBlocks);
      if (this.engineMode == EngineMode.HIDE) {
         this.presetBlockStates = null;
         this.presetBlockStatesFull = null;
         this.presetBlockStatesStone = new BlockState[]{Blocks.STONE.defaultBlockState()};
         this.presetBlockStatesDeepslate = new BlockState[]{Blocks.DEEPSLATE.defaultBlockState()};
         this.presetBlockStatesNetherrack = new BlockState[]{Blocks.NETHERRACK.defaultBlockState()};
         this.presetBlockStatesEndStone = new BlockState[]{Blocks.END_STONE.defaultBlockState()};
         this.presetBlockStateGlobalIds = null;
         this.presetStoneGlobalIds = new int[]{BlockStateIdMapper.getGlobalBlockStateId(Blocks.STONE.defaultBlockState())};
         this.presetDeepslateGlobalIds = new int[]{BlockStateIdMapper.getGlobalBlockStateId(Blocks.DEEPSLATE.defaultBlockState())};
         this.presetNetherrackGlobalIds = new int[]{BlockStateIdMapper.getGlobalBlockStateId(Blocks.NETHERRACK.defaultBlockState())};
         this.presetEndStoneGlobalIds = new int[]{BlockStateIdMapper.getGlobalBlockStateId(Blocks.END_STONE.defaultBlockState())};
      } else {
         LinkedList<BlockState> presetStatesList = new LinkedList<>();

         for (Block block : resolvedHiddenBlocks) {
            if (!(block instanceof EntityBlock)) {
               presetStatesList.add(block.defaultBlockState());
            }
         }

         LinkedHashSet<BlockState> uniquePresetStates = new LinkedHashSet<>();
         uniquePresetStates.addAll(presetStatesList);
         this.presetBlockStates = uniquePresetStates.isEmpty()
            ? new BlockState[]{Blocks.DIAMOND_ORE.defaultBlockState()}
            : uniquePresetStates.toArray(new BlockState[0]);
         this.presetBlockStatesFull = uniquePresetStates.isEmpty()
            ? new BlockState[]{Blocks.DIAMOND_ORE.defaultBlockState()}
            : presetStatesList.toArray(new BlockState[0]);
         this.presetBlockStatesStone = null;
         this.presetBlockStatesDeepslate = null;
         this.presetBlockStatesNetherrack = null;
         this.presetBlockStatesEndStone = null;
         this.presetBlockStateGlobalIds = new int[this.presetBlockStatesFull.length];

         for (int i = 0; i < this.presetBlockStatesFull.length; i++) {
            this.presetBlockStateGlobalIds[i] = BlockStateIdMapper.getGlobalBlockStateId(this.presetBlockStatesFull[i]);
         }

         this.presetStoneGlobalIds = null;
         this.presetDeepslateGlobalIds = null;
         this.presetNetherrackGlobalIds = null;
         this.presetEndStoneGlobalIds = null;
      }

      for (Block hiddenBlock : resolvedHiddenBlocks) {
         if (hiddenBlock != null && !hiddenBlock.defaultBlockState().isAir()) {
            for (BlockState blockState : hiddenBlock.getStateDefinition().getPossibleStates()) {
               this.obfuscateGlobal[BlockStateIdMapper.getGlobalBlockStateId(blockState)] = true;
            }
         }
      }

      if (rayTraceBlockNames == null) {
         this.rayTraceGlobal = this.obfuscateGlobal;
      } else {
         this.rayTraceGlobal = new boolean[Block.BLOCK_STATE_REGISTRY.size()];

         for (Object rawBlockName : rayTraceBlockNames) {
            Block rayTraceBlock = this.resolveBlockByName((String)rawBlockName);
            if (rayTraceBlock != null && !rayTraceBlock.defaultBlockState().isAir()) {
               for (BlockState rayTraceState : rayTraceBlock.getStateDefinition().getPossibleStates()) {
                  int globalBlockStateId = BlockStateIdMapper.getGlobalBlockStateId(rayTraceState);
                  this.rayTraceGlobal[globalBlockStateId] = true;
                  this.obfuscateGlobal[globalBlockStateId] = true;
               }
            }
         }
      }

      EmptyLevelChunk emptyLevelChunk = new EmptyLevelChunk(
         level, new ChunkPos(0, 0), MinecraftServer.getServer().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)
      );
      BlockPos originPos = new BlockPos(0, 0, 0);

      for (int stateId = 0; stateId < this.solidGlobal.length; stateId++) {
         BlockState blockState = GLOBAL_PALETTE.valueFor(stateId);
         if (blockState != null) {
            this.blockEntityGlobal[stateId] = blockState.hasBlockEntity();
            this.solidGlobal[stateId] = blockState.isRedstoneConductor(emptyLevelChunk, originPos)
                  && blockState.getBlock() != Blocks.SPAWNER
                  && blockState.getBlock() != Blocks.BARRIER
                  && blockState.getBlock() != Blocks.SHULKER_BOX
                  && blockState.getBlock() != Blocks.SLIME_BLOCK
                  && blockState.getBlock() != Blocks.MANGROVE_ROOTS
               || antiXray.lavaObscures && blockState == Blocks.LAVA.defaultBlockState();
         }
      }

      this.maxBlockHeightUpdatePosition = this.maxBlockHeight + this.updateRadius - 1;
   }

   @Override
   public BlockState[] getPresetBlockStates(Level level, ChunkPos chunkPos, int chunkSectionY) {
      int bottomBlockY = chunkSectionY << 4;
      if (bottomBlockY < this.maxBlockHeight) {
         if (this.engineMode == EngineMode.HIDE) {
            switch (level.getWorld().getEnvironment()) {
               case NETHER:
                  return this.presetBlockStatesNetherrack;
               case THE_END:
                  return this.presetBlockStatesEndStone;
               default:
                  return bottomBlockY < 0 ? this.presetBlockStatesDeepslate : this.presetBlockStatesStone;
            }
         } else {
            return this.presetBlockStates;
         }
      } else {
         return null;
      }
   }

   @Override
   public boolean shouldModify(ServerPlayer serverPlayer, LevelChunk levelChunk) {
      if (!this.plugin.isEnabled()) {
         return false;
      }

      boolean worldEnabled = this.plugin.getConfigManager().isHeavyModeEnabled()
         || this.plugin
            .getConfig()
            .getBoolean("world-settings." + this.worldName + ".enabled", this.plugin.getConfig().getBoolean("world-settings.default.enabled", false));
      if (!worldEnabled) {
         return false;
      }

      double playerVl = 0.0;
      if (this.plugin.getConfigManager().isHeavyModeDynamic()) {
         try {
            playerVl = this.plugin.getXrayDetector().getPlayerViolationLevel(serverPlayer.getBukkitEntity());
         } catch (Throwable ignored) {
         }
      }

      if (this.plugin.isObfuscationCancelled(serverPlayer.getBukkitEntity())) {
         if (!this.plugin.getConfigManager().isHeavyModeDynamic()
            || playerVl < this.plugin.getObfuscationCancelBaselineVl(serverPlayer.getBukkitEntity()) + this.plugin.getConfigManager().getCheckXrayHeavyModeTriggerVl()) {
            return false;
         }

         this.plugin.clearObfuscationCancelState(serverPlayer.getBukkitEntity());
      }

      boolean triggeredDynamicHeavyMode = false;
      if (this.plugin.getConfigManager().isHeavyModeDynamic()) {
         if (playerVl < this.plugin.getConfigManager().getCheckXrayHeavyModeTriggerVl() && !this.plugin.isForcedObfuscation(serverPlayer.getBukkitEntity())) {
            return false;
         }

         triggeredDynamicHeavyMode = true;
      }

      boolean allowed = !this.usePermission || !serverPlayer.getBukkitEntity().hasPermission("paper.antixray.bypass");
      if (allowed && triggeredDynamicHeavyMode) {
         this.plugin.triggerHeavyModeResend(serverPlayer.getBukkitEntity());
      }

      return allowed;
   }

   public ChunkPacketInfo<BlockState> getChunkPacketInfo(ClientboundLevelChunkWithLightPacket clientboundLevelChunkWithLightPacket, LevelChunk levelChunk) {
      return ChunkPacketInfoAdapter.createChunkPacketInfo(clientboundLevelChunkWithLightPacket, levelChunk);
   }

   @Override
   public void modifyBlocks(ClientboundLevelChunkWithLightPacket clientboundLevelChunkWithLightPacket, ChunkPacketInfo<BlockState> chunkPacketInfo) {
      if (chunkPacketInfo == null) {
         clientboundLevelChunkWithLightPacket.setReady(true);
      } else if (!Bukkit.isPrimaryThread()) {
         MinecraftServer server = MinecraftServer.getServer();

         try {
            server.scheduleOnMain(() -> this.modifyBlocks(clientboundLevelChunkWithLightPacket, chunkPacketInfo));
         } catch (NoSuchMethodError ignored) {
            server.execute(() -> this.modifyBlocks(clientboundLevelChunkWithLightPacket, chunkPacketInfo));
         }
      } else {
         LevelChunk chunk = chunkPacketInfo.getChunk();
         int chunkX = ChunkCoordinateCodec.getChunkX(chunk.getPos());
         int chunkZ = ChunkCoordinateCodec.getChunkZ(chunk.getPos());
         Level level = chunk.getLevel();
         ChunkObfuscationTask chunkObfuscationTask = new ChunkObfuscationTask(chunkPacketInfo, clientboundLevelChunkWithLightPacket, this);
         chunkObfuscationTask.setNearbyChunks(
            new LevelChunk[]{
               level.getChunkIfLoaded(chunkX - 1, chunkZ),
               level.getChunkIfLoaded(chunkX + 1, chunkZ),
               level.getChunkIfLoaded(chunkX, chunkZ - 1),
               level.getChunkIfLoaded(chunkX, chunkZ + 1)
            }
         );

         try {
            this.executor.execute(chunkObfuscationTask);
         } catch (RejectedExecutionException rejected) {
            chunkObfuscationTask.run();
         }
      }
   }

   @Override
   public void onBlockChange(Level level, BlockPos blockPos, BlockState newBlockState, BlockState oldBlockState, @UpdateFlags int updateFlags, int maxUpdateDepth) {
      if (oldBlockState != null
         && this.solidGlobal[BlockStateIdMapper.getGlobalBlockStateId(oldBlockState)]
         && !this.solidGlobal[BlockStateIdMapper.getGlobalBlockStateId(newBlockState)]
         && blockPos.getY() <= this.maxBlockHeightUpdatePosition) {
         this.updateNearbyBlocks(level, blockPos);
      }
   }

   public void updateNearbyBlocksIfBelowMaxHeight(Level level, BlockPos blockPos) {
      if (blockPos.getY() <= this.maxBlockHeightUpdatePosition) {
         this.updateNearbyBlocks(level, blockPos);
      }
   }

   public boolean isObfuscatedBlockState(BlockState blockState) {
      return this.obfuscateGlobal[BlockStateIdMapper.getGlobalBlockStateId(blockState)];
   }

   private Block resolveBlockByName(String blockName) {
      if (blockName != null && !blockName.isBlank()) {
         String normalizedName = blockName.trim().toUpperCase(Locale.ROOT).replace(':', '_');
         Material material = Material.matchMaterial(normalizedName);
         return material == null ? null : CraftMagicNumbers.getBlock(material);
      } else {
         return null;
      }
   }

   private int getPresetBlockStatesFullLength() {
      return this.engineMode == EngineMode.HIDE ? 1 : this.presetBlockStatesFull.length;
   }

   private LongHashSet collectFakeOrePositionsInChunk(Level level, LevelChunk levelChunk) {
      CraftWorld world = level.getWorld();
      int chunkX = ChunkCoordinateCodec.getChunkX(levelChunk.getPos());
      int chunkZ = ChunkCoordinateCodec.getChunkZ(levelChunk.getPos());
      LongHashSet fakeOrePositions = new LongHashSet();
      if (this.plugin.getXrayDetector() == null) {
         return fakeOrePositions;
      }

      Map<Location, ?> fakeOreTypes = this.plugin.getXrayDetector().getFakeOreTypes();
      for (Entry<Location, ?> entry : fakeOreTypes.entrySet()) {
         Location location = entry.getKey();
         if (location != null
            && location.getWorld() != null
            && location.getWorld().equals(world)
            && location.getBlockX() >> 4 == chunkX
            && location.getBlockZ() >> 4 == chunkZ) {
            fakeOrePositions.add(BlockPos.asLong(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
         }
      }

      return fakeOrePositions;
   }

   public void obfuscate(ChunkPacketInfo chunkPacketInfo, LevelChunk[] nearbyChunks) {
      long startNanos = System.nanoTime();
      this.plugin.getObfuscationStatistics().recordAttempt();
      int[] presetBlockStateBitsBuffer = this.presetBlockStateBits.get();
      boolean[] solidPaletteFlags = SOLID_PALETTE_FLAGS.get();
      boolean[] obfuscatePaletteFlags = OBFUSCATE_PALETTE_FLAGS.get();
      boolean[] rayTracePaletteFlags = this.rayTraceGlobal == this.obfuscateGlobal ? obfuscatePaletteFlags : RAY_TRACE_PALETTE_FLAGS.get();
      boolean[] blockEntityPaletteFlags = BLOCK_ENTITY_PALETTE_FLAGS.get();
      boolean[][] currentLayerTransparent = CURRENT_LAYER_TRANSPARENT.get();
      boolean[][] nextLayerTransparent = NEXT_LAYER_TRANSPARENT.get();
      boolean[][] nextNextLayerTransparent = NEXT_NEXT_LAYER_TRANSPARENT.get();
      boolean[][] layerRayTraceMask = LAYER_RAY_TRACE_MASK.get();
      boolean[][] layerBlockEntityMask = LAYER_BLOCK_ENTITY_MASK.get();
      BitStorageReader bitStorageReader = BIT_STORAGE_READER.get();
      BitStorageWriter bitStorageWriter = BIT_STORAGE_WRITER.get();
      LevelChunkSection[] nearbyChunkSections = NEARBY_CHUNK_SECTIONS.get();
      LevelChunk chunk = chunkPacketInfo.getChunk();
      Level level = chunk.getLevel();
      int maxChunkSectionIndex = Math.min((this.maxBlockHeight >> 4) - chunk.getMinSectionY(), chunk.getSectionsCount()) - 1;
      boolean[] solid = null;
      boolean[] obfuscate = null;
      boolean[] rayTrace = null;
      boolean[] blockEntity = null;
      bitStorageReader.setBuffer(chunkPacketInfo.getBuffer());
      bitStorageWriter.setBuffer(chunkPacketInfo.getBuffer());
      final int presetCount = presetBlockStateBitsBuffer.length;
      EnhancedChunkObfuscationController.PresetIndexGenerator presetIndexGenerator = presetCount == 1
         ? () -> 0
         : (this.engineMode == EngineMode.OBFUSCATE_LAYER ? new EnhancedChunkObfuscationController.PresetIndexGenerator() {
            private int state;
            private int next;

            {
               while ((this.state = ThreadLocalRandom.current().nextInt()) == 0) {
               }
            }

            @Override
            public int getAsInt() {
               return this.next;
            }

            @Override
            public void nextLayer() {
               this.state = this.state ^ this.state << 13;
               this.state = this.state ^ this.state >>> 17;
               this.state = this.state ^ this.state << 5;
               this.next = (int)(Integer.toUnsignedLong(this.state) * presetCount >>> 32);
            }
         } : new EnhancedChunkObfuscationController.PresetIndexGenerator() {
            private int state;

            {
               while ((this.state = ThreadLocalRandom.current().nextInt()) == 0) {
               }
            }

            @Override
            public int getAsInt() {
               this.state = this.state ^ this.state << 13;
               this.state = this.state ^ this.state >>> 17;
               this.state = this.state ^ this.state << 5;
               return (int)(Integer.toUnsignedLong(this.state) * presetCount >>> 32);
            }
         });
      HashMap<BlockPos, Boolean> rayTraceBlocks = new HashMap<>();
      HashSet<BlockPos> hiddenBlockEntities = new HashSet<>();
      boolean skipFakeOres = !this.plugin.getConfigManager().isCheckXrayHeavyModeObfuscateFakeOres();
      LongHashSet chunkFakeOrePositions = null;
      boolean hasFakeOresInChunk = false;
      if (skipFakeOres) {
         long chunkKey = ChunkCoordinateCodec.encode(chunk.getPos());
         hasFakeOresInChunk = this.fakeOreChunkKeys != null && this.fakeOreChunkKeys.contains(chunkKey);
         if (!hasFakeOresInChunk) {
            chunkFakeOrePositions = this.collectFakeOrePositionsInChunk(level, chunk);
            hasFakeOresInChunk = chunkFakeOrePositions != null && !chunkFakeOrePositions.isEmpty();
         }
      }

      for (int chunkSectionIndex = 0; chunkSectionIndex <= maxChunkSectionIndex; chunkSectionIndex++) {
         if (chunkPacketInfo.isWritten(chunkSectionIndex) && chunkPacketInfo.getPresetValues(chunkSectionIndex) != null) {
            int[] sectionPresetBits;
            if (chunkPacketInfo.getPalette(chunkSectionIndex) instanceof GlobalPalette) {
               if (this.engineMode == EngineMode.HIDE) {
                  switch (level.getWorld().getEnvironment()) {
                     case NETHER:
                        sectionPresetBits = this.presetNetherrackGlobalIds;
                        break;
                     case THE_END:
                        sectionPresetBits = this.presetEndStoneGlobalIds;
                        break;
                     default:
                        sectionPresetBits = chunkSectionIndex + chunk.getMinSectionY() < 0
                           ? this.presetDeepslateGlobalIds
                           : this.presetStoneGlobalIds;
                  }
               } else {
                  sectionPresetBits = this.presetBlockStateGlobalIds;
               }
            } else {
               BlockState[] presetValues = chunkPacketInfo.getPresetValues(chunkSectionIndex) == this.presetBlockStates
                  ? this.presetBlockStatesFull
                  : (BlockState[])chunkPacketInfo.getPresetValues(chunkSectionIndex);
               sectionPresetBits = presetBlockStateBitsBuffer;

               for (int presetIndex = 0; presetIndex < sectionPresetBits.length; presetIndex++) {
                  sectionPresetBits[presetIndex] = BlockStateIdMapper.getPaletteId(chunkPacketInfo.getPalette(chunkSectionIndex), presetValues[presetIndex]);
               }
            }

            bitStorageWriter.setIndex(chunkPacketInfo.getIndex(chunkSectionIndex));
            if (chunkSectionIndex == 0
               || !chunkPacketInfo.isWritten(chunkSectionIndex - 1)
               || chunkPacketInfo.getPresetValues(chunkSectionIndex - 1) == null) {
               bitStorageReader.setBits(chunkPacketInfo.getBits(chunkSectionIndex));
               bitStorageReader.setIndex(chunkPacketInfo.getIndex(chunkSectionIndex));
               Palette sectionPalette = chunkPacketInfo.getPalette(chunkSectionIndex);
               solid = this.mapGlobalFlagsToPalette(sectionPalette, solidPaletteFlags, this.solidGlobal);
               obfuscate = this.mapGlobalFlagsToPalette(sectionPalette, obfuscatePaletteFlags, this.obfuscateGlobal);
               rayTrace = rayTracePaletteFlags == obfuscatePaletteFlags
                  ? obfuscate
                  : this.mapGlobalFlagsToPalette(sectionPalette, rayTracePaletteFlags, this.rayTraceGlobal);
               blockEntity = this.mapGlobalFlagsToPalette(sectionPalette, blockEntityPaletteFlags, this.blockEntityGlobal);
               LevelChunkSection belowSection = null;
               boolean belowSectionMissing = chunkSectionIndex == 0 || (belowSection = chunk.getSections()[chunkSectionIndex - 1]) == null;

               for (int z = 0; z < 16; z++) {
                  for (int x = 0; x < 16; x++) {
                     currentLayerTransparent[z][x] = true;
                     nextLayerTransparent[z][x] = belowSectionMissing || this.isTransparentInSection(belowSection, x, 15, z);
                     layerRayTraceMask[z][x] = false;
                  }
               }

               bitStorageWriter.setBits(0);
               this.obfuscateLayer(
                  chunk.getPos(),
                  chunk.getMinSectionY(),
                  chunkSectionIndex,
                  -1,
                  bitStorageReader,
                  bitStorageWriter,
                  solid,
                  obfuscate,
                  rayTrace,
                  blockEntity,
                  sectionPresetBits,
                  currentLayerTransparent,
                  nextLayerTransparent,
                  nextNextLayerTransparent,
                  layerRayTraceMask,
                  layerBlockEntityMask,
                  this.emptyNearbyChunkSections,
                  presetIndexGenerator,
                  rayTraceBlocks,
                  hiddenBlockEntities,
                  chunkFakeOrePositions,
                  hasFakeOresInChunk
               );
            }

            bitStorageWriter.setBits(chunkPacketInfo.getBits(chunkSectionIndex));
            nearbyChunkSections[0] = nearbyChunks != null && nearbyChunks[0] != null ? nearbyChunks[0].getSections()[chunkSectionIndex] : null;
            nearbyChunkSections[1] = nearbyChunks != null && nearbyChunks[1] != null ? nearbyChunks[1].getSections()[chunkSectionIndex] : null;
            nearbyChunkSections[2] = nearbyChunks != null && nearbyChunks[2] != null ? nearbyChunks[2].getSections()[chunkSectionIndex] : null;
            nearbyChunkSections[3] = nearbyChunks != null && nearbyChunks[3] != null ? nearbyChunks[3].getSections()[chunkSectionIndex] : null;

            for (int y = 0; y < 15; y++) {
               boolean[][] previousLayer = currentLayerTransparent;
               currentLayerTransparent = nextLayerTransparent;
               nextLayerTransparent = nextNextLayerTransparent;
               nextNextLayerTransparent = previousLayer;
               presetIndexGenerator.nextLayer();
               this.obfuscateLayer(
                  chunk.getPos(),
                  chunk.getMinSectionY(),
                  chunkSectionIndex,
                  y,
                  bitStorageReader,
                  bitStorageWriter,
                  solid,
                  obfuscate,
                  rayTrace,
                  blockEntity,
                  sectionPresetBits,
                  currentLayerTransparent,
                  nextLayerTransparent,
                  nextNextLayerTransparent,
                  layerRayTraceMask,
                  layerBlockEntityMask,
                  nearbyChunkSections,
                  presetIndexGenerator,
                  rayTraceBlocks,
                  hiddenBlockEntities,
                  chunkFakeOrePositions,
                  hasFakeOresInChunk
               );
            }

            if (chunkSectionIndex != maxChunkSectionIndex
               && chunkPacketInfo.isWritten(chunkSectionIndex + 1)
               && chunkPacketInfo.getPresetValues(chunkSectionIndex + 1) != null) {
               bitStorageReader.setBits(chunkPacketInfo.getBits(chunkSectionIndex + 1));
               bitStorageReader.setIndex(chunkPacketInfo.getIndex(chunkSectionIndex + 1));
               Palette nextSectionPalette = chunkPacketInfo.getPalette(chunkSectionIndex + 1);
               solid = this.mapGlobalFlagsToPalette(nextSectionPalette, solidPaletteFlags, this.solidGlobal);
               obfuscate = this.mapGlobalFlagsToPalette(nextSectionPalette, obfuscatePaletteFlags, this.obfuscateGlobal);
               rayTrace = rayTracePaletteFlags == obfuscatePaletteFlags
                  ? obfuscate
                  : this.mapGlobalFlagsToPalette(nextSectionPalette, rayTracePaletteFlags, this.rayTraceGlobal);
               blockEntity = this.mapGlobalFlagsToPalette(nextSectionPalette, blockEntityPaletteFlags, this.blockEntityGlobal);
               boolean[][] previousLayer = currentLayerTransparent;
               currentLayerTransparent = nextLayerTransparent;
               nextLayerTransparent = nextNextLayerTransparent;
               nextNextLayerTransparent = previousLayer;
               presetIndexGenerator.nextLayer();
               this.obfuscateLayer(
                  chunk.getPos(),
                  chunk.getMinSectionY(),
                  chunkSectionIndex,
                  15,
                  bitStorageReader,
                  bitStorageWriter,
                  solid,
                  obfuscate,
                  rayTrace,
                  blockEntity,
                  sectionPresetBits,
                  currentLayerTransparent,
                  nextLayerTransparent,
                  nextNextLayerTransparent,
                  layerRayTraceMask,
                  layerBlockEntityMask,
                  nearbyChunkSections,
                  presetIndexGenerator,
                  rayTraceBlocks,
                  hiddenBlockEntities,
                  chunkFakeOrePositions,
                  hasFakeOresInChunk
               );
            } else {
               LevelChunkSection aboveSection = chunkSectionIndex == chunk.getSectionsCount() - 1 ? null : chunk.getSections()[chunkSectionIndex + 1];
               boolean aboveSectionMissing = aboveSection == null;
               boolean[][] previousLayer = currentLayerTransparent;
               currentLayerTransparent = nextLayerTransparent;
               nextLayerTransparent = nextNextLayerTransparent;
               nextNextLayerTransparent = previousLayer;

               for (int z = 0; z < 16; z++) {
                  for (int x = 0; x < 16; x++) {
                     if (aboveSectionMissing || this.isTransparentInSection(aboveSection, x, 0, z)) {
                        currentLayerTransparent[z][x] = true;
                     }
                  }
               }

               bitStorageReader.setBits(0);
               solidPaletteFlags[0] = true;
               presetIndexGenerator.nextLayer();
               this.obfuscateLayer(
                  chunk.getPos(),
                  chunk.getMinSectionY(),
                  chunkSectionIndex,
                  15,
                  bitStorageReader,
                  bitStorageWriter,
                  solidPaletteFlags,
                  obfuscate,
                  rayTrace,
                  blockEntity,
                  sectionPresetBits,
                  currentLayerTransparent,
                  nextLayerTransparent,
                  nextNextLayerTransparent,
                  layerRayTraceMask,
                  layerBlockEntityMask,
                  nearbyChunkSections,
                  presetIndexGenerator,
                  rayTraceBlocks,
                  hiddenBlockEntities,
                  chunkFakeOrePositions,
                  hasFakeOresInChunk
               );
            }

            bitStorageWriter.flush();
         }
      }

      long elapsedNanos = System.nanoTime() - startNanos;
      boolean obfuscated = !rayTraceBlocks.isEmpty();
      if (obfuscated) {
         this.plugin.getObfuscationStatistics().recordSuccess(rayTraceBlocks.size(), elapsedNanos);
      } else {
         this.plugin.getObfuscationStatistics().recordSkipped();
      }

      CraftWorld world = chunk.getLevel().getWorld();
      int chunkX = ChunkCoordinateCodec.getChunkX(chunk.getPos());
      int chunkZ = ChunkCoordinateCodec.getChunkZ(chunk.getPos());
      this.plugin.recordChunkObfuscationState(world, chunkX, chunkZ, obfuscated);
      PlayerChunkKey playerChunkKey = new PlayerChunkKey(chunk.getLevel().getWorld().getUID(), ChunkCoordinateCodec.encode(chunk.getPos()));
      if (this.plugin.isEnabled()) {
         if (rayTraceBlocks.isEmpty()) {
            this.plugin.getTrackedChunkCache().remove(playerChunkKey);
         } else {
            this.plugin.getTrackedChunkCache().put(playerChunkKey, new TrackedChunkVisibility(chunkPacketInfo.getChunk(), rayTraceBlocks));
         }
      }

      if (!hiddenBlockEntities.isEmpty()) {
         try {
            List blockEntitiesData = (List)BLOCK_ENTITIES_DATA_FIELD.get(ChunkPacketInfoAdapter.getChunkPacketData(chunkPacketInfo.getChunkPacket()));
            ChunkPos chunkPos = chunk.getPos();
            int minBlockX = chunkPos.getMinBlockX();
            int minBlockZ = chunkPos.getMinBlockZ();
            MutableBlockPos mutableBlockPos = new MutableBlockPos();
            blockEntitiesData.removeIf(blockEntityInfo -> {
               try {
                  int packedXZ = BLOCK_ENTITY_PACKED_XZ_FIELD.getInt(blockEntityInfo);
                  return hiddenBlockEntities.contains(
                     mutableBlockPos.set(minBlockX + (packedXZ >>> 4), BLOCK_ENTITY_Y_FIELD.getInt(blockEntityInfo), minBlockZ + (packedXZ & 15))
                  );
               } catch (IllegalAccessException fieldAccessEx) {
                  throw new RuntimeException(fieldAccessEx);
               }
            });
         } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
         }
      }

      chunkPacketInfo.getChunkPacket().setReady(true);
   }

   private void obfuscateLayer(
      ChunkPos chunkPos,
      int minSectionY,
      int sectionIndex,
      int y,
      BitStorageReader bitStorageReader,
      BitStorageWriter bitStorageWriter,
      boolean[] solid,
      boolean[] obfuscate,
      boolean[] rayTrace,
      boolean[] blockEntity,
      int[] sectionPresetBits,
      boolean[][] currentLayer,
      boolean[][] nextLayer,
      boolean[][] nextNextLayer,
      boolean[][] rayTraceLayer,
      boolean[][] blockEntityLayer,
      LevelChunkSection[] nearbyChunkSections,
      IntSupplier presetIndexSupplier,
      Map rayTraceBlocks,
      Set hiddenBlockEntities,
      LongHashSet chunkFakeOrePositions,
      boolean checkFakeOres
   ) {
      int minBlockX = chunkPos.getMinBlockX();
      int minBlockZ = chunkPos.getMinBlockZ();
      int blockY = (sectionIndex + minSectionY << 4) + y;
      int paletteIndex = bitStorageReader.read();
      if (nextNextLayer[0][0] = !solid[paletteIndex]) {
         if (rayTraceLayer[0][0] && rayTraceBlocks.size() < this.maxRayTraceBlockCountPerChunk) {
            bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
            BlockPos blockPos = BlockPos.of(BlockPos.asLong(minBlockX + 0, blockY, minBlockZ + 0));
            rayTraceBlocks.put(blockPos, true);
            if (blockEntityLayer[0][0]) {
               hiddenBlockEntities.add(blockPos);
            }
         } else {
            bitStorageWriter.skip();
         }

         nextLayer[0][1] = true;
         nextLayer[1][0] = true;
      } else if (!currentLayer[0][0]
         && !this.isTransparentInSection(nearbyChunkSections[2], 0, y, 15)
         && !this.isTransparentInSection(nearbyChunkSections[0], 15, y, 0)) {
         bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
         if (blockEntityLayer[0][0]) {
            hiddenBlockEntities.add(BlockPos.of(BlockPos.asLong(minBlockX + 0, blockY, minBlockZ + 0)));
         }
      } else if (rayTraceLayer[0][0] && rayTraceBlocks.size() < this.maxRayTraceBlockCountPerChunk) {
         bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
         BlockPos blockPos = BlockPos.of(BlockPos.asLong(minBlockX + 0, blockY, minBlockZ + 0));
         rayTraceBlocks.put(blockPos, true);
         if (blockEntityLayer[0][0]) {
            hiddenBlockEntities.add(blockPos);
         }
      } else {
         bitStorageWriter.skip();
      }

      if (rayTrace[paletteIndex]) {
         rayTraceLayer[0][0] = true;
         blockEntityLayer[0][0] = blockEntity[paletteIndex];
      } else {
         rayTraceLayer[0][0] = false;
         if (!obfuscate[paletteIndex]) {
            nextLayer[0][0] = true;
         } else {
            blockEntityLayer[0][0] = blockEntity[paletteIndex];
         }
      }

      for (int x = 1; x < 15; x++) {
         paletteIndex = bitStorageReader.read();
         if (nextNextLayer[0][x] = !solid[paletteIndex]) {
            if (rayTraceLayer[0][x] && rayTraceBlocks.size() < this.maxRayTraceBlockCountPerChunk) {
               bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
               BlockPos blockPos = BlockPos.of(BlockPos.asLong(minBlockX + x, blockY, minBlockZ + 0));
               rayTraceBlocks.put(blockPos, true);
               if (blockEntityLayer[0][x]) {
                  hiddenBlockEntities.add(blockPos);
               }
            } else {
               bitStorageWriter.skip();
            }

            nextLayer[0][x - 1] = true;
            nextLayer[0][x + 1] = true;
            nextLayer[1][x] = true;
         } else if (!currentLayer[0][x] && !this.isTransparentInSection(nearbyChunkSections[2], x, y, 15)) {
            bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
            if (blockEntityLayer[0][x]) {
               hiddenBlockEntities.add(BlockPos.of(BlockPos.asLong(minBlockX + x, blockY, minBlockZ + 0)));
            }
         } else if (rayTraceLayer[0][x] && rayTraceBlocks.size() < this.maxRayTraceBlockCountPerChunk) {
            bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
            BlockPos blockPos = BlockPos.of(BlockPos.asLong(minBlockX + x, blockY, minBlockZ + 0));
            rayTraceBlocks.put(blockPos, true);
            if (blockEntityLayer[0][x]) {
               hiddenBlockEntities.add(blockPos);
            }
         } else {
            bitStorageWriter.skip();
         }

         if (rayTrace[paletteIndex]) {
            rayTraceLayer[0][x] = true;
            blockEntityLayer[0][x] = blockEntity[paletteIndex];
         } else {
            rayTraceLayer[0][x] = false;
            if (!obfuscate[paletteIndex]) {
               nextLayer[0][x] = true;
            } else {
               blockEntityLayer[0][x] = blockEntity[paletteIndex];
            }
         }
      }

      paletteIndex = bitStorageReader.read();
      if (nextNextLayer[0][15] = !solid[paletteIndex]) {
         if (rayTraceLayer[0][15] && rayTraceBlocks.size() < this.maxRayTraceBlockCountPerChunk) {
            bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
            BlockPos blockPos = BlockPos.of(BlockPos.asLong(minBlockX + 15, blockY, minBlockZ + 0));
            rayTraceBlocks.put(blockPos, true);
            if (blockEntityLayer[0][15]) {
               hiddenBlockEntities.add(blockPos);
            }
         } else {
            bitStorageWriter.skip();
         }

         nextLayer[0][14] = true;
         nextLayer[1][15] = true;
      } else if (!currentLayer[0][15]
         && !this.isTransparentInSection(nearbyChunkSections[2], 15, y, 15)
         && !this.isTransparentInSection(nearbyChunkSections[1], 0, y, 0)) {
         bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
         if (blockEntityLayer[0][15]) {
            hiddenBlockEntities.add(BlockPos.of(BlockPos.asLong(minBlockX + 15, blockY, minBlockZ + 0)));
         }
      } else if (rayTraceLayer[0][15] && rayTraceBlocks.size() < this.maxRayTraceBlockCountPerChunk) {
         bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
         BlockPos blockPos = BlockPos.of(BlockPos.asLong(minBlockX + 15, blockY, minBlockZ + 0));
         rayTraceBlocks.put(blockPos, true);
         if (blockEntityLayer[0][15]) {
            hiddenBlockEntities.add(blockPos);
         }
      } else {
         bitStorageWriter.skip();
      }

      if (rayTrace[paletteIndex]) {
         rayTraceLayer[0][15] = true;
         blockEntityLayer[0][15] = blockEntity[paletteIndex];
      } else {
         rayTraceLayer[0][15] = false;
         if (!obfuscate[paletteIndex]) {
            nextLayer[0][15] = true;
         } else {
            blockEntityLayer[0][15] = blockEntity[paletteIndex];
         }
      }

      for (int z = 1; z < 15; z++) {
         paletteIndex = bitStorageReader.read();
         if (nextNextLayer[z][0] = !solid[paletteIndex]) {
            if (rayTraceLayer[z][0] && rayTraceBlocks.size() < this.maxRayTraceBlockCountPerChunk) {
               bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
               BlockPos blockPos = BlockPos.of(BlockPos.asLong(minBlockX + 0, blockY, minBlockZ + z));
               rayTraceBlocks.put(blockPos, true);
               if (blockEntityLayer[z][0]) {
                  hiddenBlockEntities.add(blockPos);
               }
            } else {
               bitStorageWriter.skip();
            }

            nextLayer[z][1] = true;
            nextLayer[z - 1][0] = true;
            nextLayer[z + 1][0] = true;
         } else if (!currentLayer[z][0] && !this.isTransparentInSection(nearbyChunkSections[0], 15, y, z)) {
            bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
            if (blockEntityLayer[z][0]) {
               hiddenBlockEntities.add(BlockPos.of(BlockPos.asLong(minBlockX + 0, blockY, minBlockZ + z)));
            }
         } else if (rayTraceLayer[z][0] && rayTraceBlocks.size() < this.maxRayTraceBlockCountPerChunk) {
            bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
            BlockPos blockPos = BlockPos.of(BlockPos.asLong(minBlockX + 0, blockY, minBlockZ + z));
            rayTraceBlocks.put(blockPos, true);
            if (blockEntityLayer[z][0]) {
               hiddenBlockEntities.add(blockPos);
            }
         } else {
            bitStorageWriter.skip();
         }

         if (rayTrace[paletteIndex]) {
            rayTraceLayer[z][0] = true;
            blockEntityLayer[z][0] = blockEntity[paletteIndex];
         } else {
            rayTraceLayer[z][0] = false;
            if (!obfuscate[paletteIndex]) {
               nextLayer[z][0] = true;
            } else {
               blockEntityLayer[z][0] = blockEntity[paletteIndex];
            }
         }

         for (int x = 1; x < 15; x++) {
            paletteIndex = bitStorageReader.read();
            if (checkFakeOres) {
               long packedPos = BlockPos.asLong(minBlockX + x, blockY, minBlockZ + z);
               if (this.fakeOreBlockPositions != null && this.fakeOreBlockPositions.contains(packedPos)
                  || chunkFakeOrePositions != null && chunkFakeOrePositions.contains(packedPos)) {
                  bitStorageWriter.skip();
                  continue;
               }
            }

            if (!(nextNextLayer[z][x] = !solid[paletteIndex])) {
               if (currentLayer[z][x]) {
                  if (rayTraceLayer[z][x] && rayTraceBlocks.size() < this.maxRayTraceBlockCountPerChunk) {
                     bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
                     BlockPos blockPos = BlockPos.of(BlockPos.asLong(minBlockX + x, blockY, minBlockZ + z));
                     rayTraceBlocks.put(blockPos, true);
                     if (blockEntityLayer[z][x]) {
                        hiddenBlockEntities.add(blockPos);
                     }
                  } else {
                     bitStorageWriter.skip();
                  }
               } else {
                  bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
                  if (blockEntityLayer[z][x]) {
                     hiddenBlockEntities.add(BlockPos.of(BlockPos.asLong(minBlockX + x, blockY, minBlockZ + z)));
                  }
               }
            } else {
               if (rayTraceLayer[z][x] && rayTraceBlocks.size() < this.maxRayTraceBlockCountPerChunk) {
                  bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
                  BlockPos blockPos = BlockPos.of(BlockPos.asLong(minBlockX + x, blockY, minBlockZ + z));
                  rayTraceBlocks.put(blockPos, true);
                  if (blockEntityLayer[z][x]) {
                     hiddenBlockEntities.add(blockPos);
                  }
               } else {
                  bitStorageWriter.skip();
               }

               nextLayer[z][x - 1] = true;
               nextLayer[z][x + 1] = true;
               nextLayer[z - 1][x] = true;
               nextLayer[z + 1][x] = true;
            }

            if (rayTrace[paletteIndex]) {
               rayTraceLayer[z][x] = true;
               blockEntityLayer[z][x] = blockEntity[paletteIndex];
            } else {
               rayTraceLayer[z][x] = false;
               if (!obfuscate[paletteIndex]) {
                  nextLayer[z][x] = true;
               } else {
                  blockEntityLayer[z][x] = blockEntity[paletteIndex];
               }
            }
         }

         paletteIndex = bitStorageReader.read();
         if (nextNextLayer[z][15] = !solid[paletteIndex]) {
            if (rayTraceLayer[z][15] && rayTraceBlocks.size() < this.maxRayTraceBlockCountPerChunk) {
               bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
               BlockPos blockPos = BlockPos.of(BlockPos.asLong(minBlockX + 15, blockY, minBlockZ + z));
               rayTraceBlocks.put(blockPos, true);
               if (blockEntityLayer[z][15]) {
                  hiddenBlockEntities.add(blockPos);
               }
            } else {
               bitStorageWriter.skip();
            }

            nextLayer[z][14] = true;
            nextLayer[z - 1][15] = true;
            nextLayer[z + 1][15] = true;
         } else if (!currentLayer[z][15] && !this.isTransparentInSection(nearbyChunkSections[1], 0, y, z)) {
            bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
            if (blockEntityLayer[z][15]) {
               hiddenBlockEntities.add(BlockPos.of(BlockPos.asLong(minBlockX + 15, blockY, minBlockZ + z)));
            }
         } else if (rayTraceLayer[z][15] && rayTraceBlocks.size() < this.maxRayTraceBlockCountPerChunk) {
            bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
            BlockPos blockPos = BlockPos.of(BlockPos.asLong(minBlockX + 15, blockY, minBlockZ + z));
            rayTraceBlocks.put(blockPos, true);
            if (blockEntityLayer[z][15]) {
               hiddenBlockEntities.add(blockPos);
            }
         } else {
            bitStorageWriter.skip();
         }

         if (rayTrace[paletteIndex]) {
            rayTraceLayer[z][15] = true;
            blockEntityLayer[z][15] = blockEntity[paletteIndex];
         } else {
            rayTraceLayer[z][15] = false;
            if (!obfuscate[paletteIndex]) {
               nextLayer[z][15] = true;
            } else {
               blockEntityLayer[z][15] = blockEntity[paletteIndex];
            }
         }
      }

      paletteIndex = bitStorageReader.read();
      if (nextNextLayer[15][0] = !solid[paletteIndex]) {
         if (rayTraceLayer[15][0] && rayTraceBlocks.size() < this.maxRayTraceBlockCountPerChunk) {
            bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
            BlockPos blockPos = BlockPos.of(BlockPos.asLong(minBlockX + 0, blockY, minBlockZ + 15));
            rayTraceBlocks.put(blockPos, true);
            if (blockEntityLayer[15][0]) {
               hiddenBlockEntities.add(blockPos);
            }
         } else {
            bitStorageWriter.skip();
         }

         nextLayer[15][1] = true;
         nextLayer[14][0] = true;
      } else if (!currentLayer[15][0]
         && !this.isTransparentInSection(nearbyChunkSections[3], 0, y, 0)
         && !this.isTransparentInSection(nearbyChunkSections[0], 15, y, 15)) {
         bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
         if (blockEntityLayer[15][0]) {
            hiddenBlockEntities.add(BlockPos.of(BlockPos.asLong(minBlockX + 0, blockY, minBlockZ + 15)));
         }
      } else if (rayTraceLayer[15][0] && rayTraceBlocks.size() < this.maxRayTraceBlockCountPerChunk) {
         bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
         BlockPos blockPos = BlockPos.of(BlockPos.asLong(minBlockX + 0, blockY, minBlockZ + 15));
         rayTraceBlocks.put(blockPos, true);
         if (blockEntityLayer[15][0]) {
            hiddenBlockEntities.add(blockPos);
         }
      } else {
         bitStorageWriter.skip();
      }

      if (rayTrace[paletteIndex]) {
         rayTraceLayer[15][0] = true;
         blockEntityLayer[15][0] = blockEntity[paletteIndex];
      } else {
         rayTraceLayer[15][0] = false;
         if (!obfuscate[paletteIndex]) {
            nextLayer[15][0] = true;
         } else {
            blockEntityLayer[15][0] = blockEntity[paletteIndex];
         }
      }

      for (int x = 1; x < 15; x++) {
         paletteIndex = bitStorageReader.read();
         if (nextNextLayer[15][x] = !solid[paletteIndex]) {
            if (rayTraceLayer[15][x] && rayTraceBlocks.size() < this.maxRayTraceBlockCountPerChunk) {
               bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
               BlockPos blockPos = BlockPos.of(BlockPos.asLong(minBlockX + x, blockY, minBlockZ + 15));
               rayTraceBlocks.put(blockPos, true);
               if (blockEntityLayer[15][x]) {
                  hiddenBlockEntities.add(blockPos);
               }
            } else {
               bitStorageWriter.skip();
            }

            nextLayer[15][x - 1] = true;
            nextLayer[15][x + 1] = true;
            nextLayer[14][x] = true;
         } else if (!currentLayer[15][x] && !this.isTransparentInSection(nearbyChunkSections[3], x, y, 0)) {
            bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
            if (blockEntityLayer[15][x]) {
               hiddenBlockEntities.add(BlockPos.of(BlockPos.asLong(minBlockX + x, blockY, minBlockZ + 15)));
            }
         } else if (rayTraceLayer[15][x] && rayTraceBlocks.size() < this.maxRayTraceBlockCountPerChunk) {
            bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
            BlockPos blockPos = BlockPos.of(BlockPos.asLong(minBlockX + x, blockY, minBlockZ + 15));
            rayTraceBlocks.put(blockPos, true);
            if (blockEntityLayer[15][x]) {
               hiddenBlockEntities.add(blockPos);
            }
         } else {
            bitStorageWriter.skip();
         }

         if (rayTrace[paletteIndex]) {
            rayTraceLayer[15][x] = true;
            blockEntityLayer[15][x] = blockEntity[paletteIndex];
         } else {
            rayTraceLayer[15][x] = false;
            if (!obfuscate[paletteIndex]) {
               nextLayer[15][x] = true;
            } else {
               blockEntityLayer[15][x] = blockEntity[paletteIndex];
            }
         }
      }

      paletteIndex = bitStorageReader.read();
      if (nextNextLayer[15][15] = !solid[paletteIndex]) {
         if (rayTraceLayer[15][15] && rayTraceBlocks.size() < this.maxRayTraceBlockCountPerChunk) {
            bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
            BlockPos blockPos = BlockPos.of(BlockPos.asLong(minBlockX + 15, blockY, minBlockZ + 15));
            rayTraceBlocks.put(blockPos, true);
            if (blockEntityLayer[15][15]) {
               hiddenBlockEntities.add(blockPos);
            }
         } else {
            bitStorageWriter.skip();
         }

         nextLayer[15][14] = true;
         nextLayer[14][15] = true;
      } else if (!currentLayer[15][15]
         && !this.isTransparentInSection(nearbyChunkSections[3], 15, y, 0)
         && !this.isTransparentInSection(nearbyChunkSections[1], 0, y, 15)) {
         bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
         if (blockEntityLayer[15][15]) {
            hiddenBlockEntities.add(BlockPos.of(BlockPos.asLong(minBlockX + 15, blockY, minBlockZ + 15)));
         }
      } else if (rayTraceLayer[15][15] && rayTraceBlocks.size() < this.maxRayTraceBlockCountPerChunk) {
         bitStorageWriter.write(sectionPresetBits[presetIndexSupplier.getAsInt()]);
         BlockPos blockPos = BlockPos.of(BlockPos.asLong(minBlockX + 15, blockY, minBlockZ + 15));
         rayTraceBlocks.put(blockPos, true);
         if (blockEntityLayer[15][15]) {
            hiddenBlockEntities.add(blockPos);
         }
      } else {
         bitStorageWriter.skip();
      }

      if (rayTrace[paletteIndex]) {
         rayTraceLayer[15][15] = true;
         blockEntityLayer[15][15] = blockEntity[paletteIndex];
      } else {
         rayTraceLayer[15][15] = false;
         if (!obfuscate[paletteIndex]) {
            nextLayer[15][15] = true;
         } else {
            blockEntityLayer[15][15] = blockEntity[paletteIndex];
         }
      }
   }

   private boolean isTransparentInSection(LevelChunkSection levelChunkSection, int x, int y, int z) {
      if (levelChunkSection == null) {
         return true;
      }

      try {
         EnhancedChunkObfuscationController.SectionSolidCache solidCache = this.sectionSolidCache.get();
         boolean[] solidBlockFlags = solidCache.getSolidBlockFlags(levelChunkSection);
         return !solidBlockFlags[y << 8 | z << 4 | x];
      } catch (MissingPaletteEntryException ex) {
         return true;
      }
   }

   private boolean[] mapGlobalFlagsToPalette(Palette palette, boolean[] paletteFlags, boolean[] globalFlags) {
      if (palette instanceof GlobalPalette) {
         return globalFlags;
      }

      try {
         for (int i = 0; i < palette.getSize(); i++) {
            paletteFlags[i] = globalFlags[BlockStateIdMapper.getGlobalBlockStateId((BlockState)palette.valueFor(i))];
         }
      } catch (MissingPaletteEntryException ignored) {
      }

      return paletteFlags;
   }

   private void updateNearbyBlocks(Level level, BlockPos blockPos) {
      if (this.updateRadius >= 2) {
         BlockPos neighbor = blockPos.west();
         this.notifyBlockChangedIfObfuscated(level, neighbor);
         this.notifyBlockChangedIfObfuscated(level, neighbor.west());
         this.notifyBlockChangedIfObfuscated(level, neighbor.below());
         this.notifyBlockChangedIfObfuscated(level, neighbor.above());
         this.notifyBlockChangedIfObfuscated(level, neighbor.north());
         this.notifyBlockChangedIfObfuscated(level, neighbor.south());

         neighbor = blockPos.east();
         this.notifyBlockChangedIfObfuscated(level, neighbor);
         this.notifyBlockChangedIfObfuscated(level, neighbor.east());
         this.notifyBlockChangedIfObfuscated(level, neighbor.below());
         this.notifyBlockChangedIfObfuscated(level, neighbor.above());
         this.notifyBlockChangedIfObfuscated(level, neighbor.north());
         this.notifyBlockChangedIfObfuscated(level, neighbor.south());

         neighbor = blockPos.below();
         this.notifyBlockChangedIfObfuscated(level, neighbor);
         this.notifyBlockChangedIfObfuscated(level, neighbor.below());
         this.notifyBlockChangedIfObfuscated(level, neighbor.north());
         this.notifyBlockChangedIfObfuscated(level, neighbor.south());

         neighbor = blockPos.above();
         this.notifyBlockChangedIfObfuscated(level, neighbor);
         this.notifyBlockChangedIfObfuscated(level, neighbor.above());
         this.notifyBlockChangedIfObfuscated(level, neighbor.north());
         this.notifyBlockChangedIfObfuscated(level, neighbor.south());

         neighbor = blockPos.north();
         this.notifyBlockChangedIfObfuscated(level, neighbor);
         this.notifyBlockChangedIfObfuscated(level, neighbor.north());

         neighbor = blockPos.south();
         this.notifyBlockChangedIfObfuscated(level, neighbor);
         this.notifyBlockChangedIfObfuscated(level, neighbor.south());
      } else if (this.updateRadius == 1) {
         this.notifyBlockChangedIfObfuscated(level, blockPos.west());
         this.notifyBlockChangedIfObfuscated(level, blockPos.east());
         this.notifyBlockChangedIfObfuscated(level, blockPos.below());
         this.notifyBlockChangedIfObfuscated(level, blockPos.above());
         this.notifyBlockChangedIfObfuscated(level, blockPos.north());
         this.notifyBlockChangedIfObfuscated(level, blockPos.south());
      }
   }

   private void notifyBlockChangedIfObfuscated(Level level, BlockPos blockPos) {
      BlockState blockStateIfLoaded = level.getBlockStateIfLoaded(blockPos);
      if (blockStateIfLoaded != null && this.obfuscateGlobal[BlockStateIdMapper.getGlobalBlockStateId(blockStateIfLoaded)]) {
         ((ServerLevel)level).getChunkSource().blockChanged(blockPos);
      }
   }

   private List resolveHiddenBlocks(Iterable blockNames, List defaultBlocks) {
      ArrayList<Block> resolvedBlocks = new ArrayList<>();
      if (blockNames != null) {
         for (Object rawBlockName : blockNames) {
            Block block = this.resolveBlockByName((String)rawBlockName);
            if (block != null && !resolvedBlocks.contains(block)) {
               resolvedBlocks.add(block);
            }
         }
      }

      return resolvedBlocks.isEmpty() ? defaultBlocks : resolvedBlocks;
   }

   public ChunkPacketInfo createChunkPacketInfo(LevelChunk levelChunk) {
      return ChunkPacketInfoAdapter.createChunkPacketInfo(null, levelChunk);
   }

   static {
      try {
         BLOCK_ENTITIES_DATA_FIELD = ClientboundLevelChunkPacketData.class.getDeclaredField("blockEntitiesData");
         BLOCK_ENTITIES_DATA_FIELD.setAccessible(true);
         Class<?> blockEntityInfoClass = Class.forName("net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData$BlockEntityInfo");
         BLOCK_ENTITY_PACKED_XZ_FIELD = blockEntityInfoClass.getDeclaredField("packedXZ");
         BLOCK_ENTITY_PACKED_XZ_FIELD.setAccessible(true);
         BLOCK_ENTITY_Y_FIELD = blockEntityInfoClass.getDeclaredField("y");
         BLOCK_ENTITY_Y_FIELD.setAccessible(true);
      } catch (NoSuchFieldException | ClassNotFoundException ex) {
         throw new RuntimeException(ex);
      }
   }

   public static final class EnvironmentSwitchMap {
      static final int[] ENVIRONMENT_ORDINALS = new int[Environment.values().length];

      static {
         try {
            ENVIRONMENT_ORDINALS[Environment.NETHER.ordinal()] = 1;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            ENVIRONMENT_ORDINALS[Environment.THE_END.ordinal()] = 2;
         } catch (NoSuchFieldError ignored) {
         }
      }
   }

   @FunctionalInterface
   private interface PresetIndexGenerator extends IntSupplier {
      default void nextLayer() {
      }
   }

   private final class SectionSolidCache {
      private LevelChunkSection owner;
      private final boolean[] solidBlockStates = new boolean[4096];

      public boolean[] getSolidBlockFlags(LevelChunkSection levelChunkSection) {
         if (this.owner != levelChunkSection) {
            this.owner = levelChunkSection;
            int index = 0;

            for (int y = 0; y < 16; y++) {
               for (int z = 0; z < 16; z++) {
                  for (int x = 0; x < 16; x++) {
                     this.solidBlockStates[index++] = EnhancedChunkObfuscationController.this.solidGlobal[BlockStateIdMapper.getGlobalBlockStateId(
                        levelChunkSection.getBlockState(x, y, z)
                     )];
                  }
               }
            }
         }

         return this.solidBlockStates;
      }
   }
}
