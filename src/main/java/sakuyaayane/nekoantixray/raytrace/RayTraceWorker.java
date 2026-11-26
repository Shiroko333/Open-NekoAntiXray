package sakuyaayane.nekoantixray.raytrace;

import io.papermc.paper.antixray.ChunkPacketBlockController;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.Map.Entry;
import java.util.Queue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import net.minecraft.world.level.ChunkPos;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.util.Vector;
import sakuyaayane.nekoantixray.chunk.EnhancedChunkObfuscationController;
import sakuyaayane.nekoantixray.NekoAntiXray;
import sakuyaayane.nekoantixray.world.BlockStateIdMapper;
import sakuyaayane.nekoantixray.world.ChunkCoordinateCodec;
import sakuyaayane.nekoantixray.world.MutableChunkKey;
import sakuyaayane.nekoantixray.world.TrackedChunkVisibility;

public final class RayTraceWorker implements Callable<Void> {
   private static final BlockState AIR = Blocks.AIR.defaultBlockState();
   private final NekoAntiXray plugin;
   private final PlayerRayTraceState playerRayTraceState;
   private final RayTraceWorker.CachedOcclusionChecker occlusionChecker;
   private final VoxelRayTracer voxelRayTracer;
   private final Collection<TrackedChunkVisibility> chunks;
   private final double maxRayTraceDistance;
   private final double maxRayTraceDistanceSquared;
   private final boolean reHideBlocks;
   private final double visibilityDistanceSquared;
   private static final String TICK_ERROR_MESSAGE = "An error occured on the RayTraceAntiXray tick thread";

   public RayTraceWorker(NekoAntiXray plugin, PlayerRayTraceState playerRayTraceState) {
      this.plugin = plugin;
      ChunkPacketBlockController chunkPacketBlockController = ((CraftWorld)playerRayTraceState.getViewSnapshots()[0].getWorld()).getHandle().chunkPacketBlockController;
      if (!(chunkPacketBlockController instanceof EnhancedChunkObfuscationController)) {
         this.playerRayTraceState = null;
         this.occlusionChecker = null;
         this.voxelRayTracer = null;
         this.chunks = null;
         this.maxRayTraceDistance = 0.0;
         this.maxRayTraceDistanceSquared = 0.0;
         this.reHideBlocks = false;
         this.visibilityDistanceSquared = 0.0;
      } else {
         this.playerRayTraceState = playerRayTraceState;
         final MutableChunkKey mutableChunkKey = new MutableChunkKey(0L);
         final ConcurrentMap trackedChunks = playerRayTraceState.getChunks();
         EnhancedChunkObfuscationController obfuscationController = (EnhancedChunkObfuscationController)chunkPacketBlockController;
         final boolean[] solidGlobal = obfuscationController.solidGlobal;
         this.occlusionChecker = new RayTraceWorker.CachedOcclusionChecker() {
            private static final boolean TREAT_UNLOADED_AS_OCCLUDING = true;
            private LevelChunk chunk;
            private LevelChunkSection cachedSection;
            private int chunkX;
            private int sectionY;
            private int chunkZ;

            @Override
            public boolean isOccluding(int blockX, int blockY, int blockZ) {
               return this.checkBlockOccludingUncached(blockX, blockY, blockZ);
            }

            @Override
            public void clearCache() {
               this.chunk = null;
               this.cachedSection = null;
            }

            public boolean checkBlockOccludingUncached(int blockX, int blockY, int blockZ) {
               int targetChunkX = blockX >> 4;
               int targetChunkZ = blockZ >> 4;
               if (this.chunkX == targetChunkX && this.chunkZ == targetChunkZ) {
                  int targetSectionY = blockY >> 4;
                  if (this.sectionY != targetSectionY) {
                     if (this.chunk == null) {
                        return true;
                     }

                     int minSectionY = this.chunk.getMinSectionY();
                     if (targetSectionY >= minSectionY && targetSectionY < this.chunk.getMaxSectionY()) {
                        LevelChunkSection section = this.chunk.getSections()[targetSectionY - minSectionY];
                        if (section != null && !section.hasOnlyAir()) {
                           if (solidGlobal[BlockStateIdMapper.getGlobalBlockStateId(RayTraceWorker.getBlockStateSafe(section, blockX, blockY, blockZ))]) {
                              return true;
                           }
                        }

                        return false;
                     } else {
                        return false;
                     }
                  } else {
                     if (this.cachedSection == null) {
                        return this.chunk == null;
                     }

                     return solidGlobal[BlockStateIdMapper.getGlobalBlockStateId(RayTraceWorker.getBlockStateSafe(this.cachedSection, blockX, blockY, blockZ))];
                  }
               } else {
                  mutableChunkKey.setValue(ChunkCoordinateCodec.encode(targetChunkX, targetChunkZ));
                  TrackedChunkVisibility trackedChunkVisibility = (TrackedChunkVisibility)trackedChunks.get(mutableChunkKey);
                  if (trackedChunkVisibility == null) {
                     return true;
                  }

                  LevelChunk targetChunk = trackedChunkVisibility.getChunk();
                  if (targetChunk == null) {
                     return true;
                  }

                  int targetSectionY = blockY >> 4;
                  int minSectionY = targetChunk.getMinSectionY();
                  if (targetSectionY >= minSectionY && targetSectionY < targetChunk.getMaxSectionY()) {
                     LevelChunkSection section = targetChunk.getSections()[targetSectionY - minSectionY];
                     if (section != null && !section.hasOnlyAir()) {
                        if (solidGlobal[BlockStateIdMapper.getGlobalBlockStateId(RayTraceWorker.getBlockStateSafe(section, blockX, blockY, blockZ))]) {
                           return true;
                        }
                     }

                     return false;
                  } else {
                     return false;
                  }
               }
            }

            @Override
            public boolean isRayBlockOccluding(int blockX, int blockY, int blockZ) {
               int targetChunkX = blockX >> 4;
               int targetSectionY = blockY >> 4;
               int targetChunkZ = blockZ >> 4;
               if (this.chunkX != targetChunkX || this.chunkZ != targetChunkZ) {
                  this.chunkX = targetChunkX;
                  this.sectionY = targetSectionY;
                  this.chunkZ = targetChunkZ;
                  mutableChunkKey.setValue(ChunkCoordinateCodec.encode(targetChunkX, targetChunkZ));
                  TrackedChunkVisibility trackedChunkVisibility = (TrackedChunkVisibility)trackedChunks.get(mutableChunkKey);
                  if (trackedChunkVisibility == null) {
                     this.chunk = null;
                     this.cachedSection = null;
                     return true;
                  }

                  this.chunk = trackedChunkVisibility.getChunk();
                  if (this.chunk == null) {
                     this.cachedSection = null;
                     return true;
                  }

                  int minSectionY = this.chunk.getMinSectionY();
                  if (targetSectionY >= minSectionY && targetSectionY < this.chunk.getMaxSectionY()) {
                     this.cachedSection = this.chunk.getSections()[targetSectionY - minSectionY];
                     if (this.cachedSection == null) {
                        return false;
                     } else if (this.cachedSection.hasOnlyAir()) {
                        this.cachedSection = null;
                        return false;
                     } else {
                        return solidGlobal[BlockStateIdMapper.getGlobalBlockStateId(RayTraceWorker.getBlockStateSafe(this.cachedSection, blockX, blockY, blockZ))];
                     }
                  } else {
                     this.cachedSection = null;
                     return false;
                  }
               } else if (this.sectionY != targetSectionY) {
                  this.sectionY = targetSectionY;
                  if (this.chunk == null) {
                     return true;
                  }

                  int minSectionY = this.chunk.getMinSectionY();
                  if (targetSectionY >= minSectionY && targetSectionY < this.chunk.getMaxSectionY()) {
                     this.cachedSection = this.chunk.getSections()[targetSectionY - minSectionY];
                     if (this.cachedSection == null) {
                        return false;
                     } else if (this.cachedSection.hasOnlyAir()) {
                        this.cachedSection = null;
                        return false;
                     } else {
                        return solidGlobal[BlockStateIdMapper.getGlobalBlockStateId(RayTraceWorker.getBlockStateSafe(this.cachedSection, blockX, blockY, blockZ))];
                     }
                  } else {
                     this.cachedSection = null;
                     return false;
                  }
               } else {
                  if (this.cachedSection == null) {
                     return this.chunk == null;
                  }

                  return solidGlobal[BlockStateIdMapper.getGlobalBlockStateId(RayTraceWorker.getBlockStateSafe(this.cachedSection, blockX, blockY, blockZ))];
               }
            }

            @Override
            public void initSectionCache(LevelChunk levelChunk, int chunkX, int sectionY, int chunkZ) {
               this.chunk = levelChunk;
               this.cachedSection = levelChunk.getSections()[sectionY - levelChunk.getMinSectionY()];
               this.chunkX = chunkX;
               this.sectionY = sectionY;
               this.chunkZ = chunkZ;
            }
         };
         VoxelRayIterator rayIterator = new VoxelRayIterator(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
         this.voxelRayTracer = new VoxelRayTracer(rayIterator::initializeNormalized, this.occlusionChecker, true);
         this.chunks = trackedChunks.values();
         this.maxRayTraceDistance = obfuscationController.rayTraceDistance;
         this.maxRayTraceDistanceSquared = this.maxRayTraceDistance * this.maxRayTraceDistance;
         this.reHideBlocks = obfuscationController.rehideBlocks;
         double rehideDistance = obfuscationController.rehideDistance;
         this.visibilityDistanceSquared = rehideDistance * rehideDistance;
      }
   }

   public Void executeRayTrace() {
      try {
         this.traceVisibleBlocks();
         return null;
      } catch (Throwable ex) {
         this.plugin.getLogger().log(Level.SEVERE, "An error occured on the RayTraceAntiXray tick thread", ex);
         throw ex;
      }
   }

   private void traceVisibleBlocks() {
      if (this.voxelRayTracer != null) {
         ConcurrentMap trackedChunks = this.playerRayTraceState.getChunks();
         PlayerViewSnapshot[] viewSnapshots = this.playerRayTraceState.getViewSnapshots();
         Vector primaryEyePos = viewSnapshots[0].getVector();
         double eyeX = primaryEyePos.getX();
         double eyeY = primaryEyePos.getY();
         double eyeZ = primaryEyePos.getZ();
         primaryEyePos.setX(eyeX - this.maxRayTraceDistance);
         primaryEyePos.setZ(eyeZ - this.maxRayTraceDistance);
         int minChunkX = primaryEyePos.getBlockX() >> 4;
         int minChunkZ = primaryEyePos.getBlockZ() >> 4;
         primaryEyePos.setX(eyeX + this.maxRayTraceDistance);
         primaryEyePos.setZ(eyeZ + this.maxRayTraceDistance);
         int maxChunkX = primaryEyePos.getBlockX() >> 4;
         int maxChunkZ = primaryEyePos.getBlockZ() >> 4;
         primaryEyePos.setX(eyeX);
         primaryEyePos.setZ(eyeZ);
         Queue pendingUpdates = this.playerRayTraceState.getPendingUpdates();

         for (TrackedChunkVisibility trackedChunkVisibility : this.chunks) {
            LevelChunk levelChunk = trackedChunkVisibility.getChunk();
            if (levelChunk == null) {
               trackedChunks.remove(trackedChunkVisibility.getKey(), trackedChunkVisibility);
            } else {
               ChunkPos chunkPos = levelChunk.getPos();
               int chunkX = ChunkCoordinateCodec.getChunkX(chunkPos);
               if (chunkX >= minChunkX && chunkX <= maxChunkX) {
                  int chunkZ = ChunkCoordinateCodec.getChunkZ(chunkPos);
                  if (chunkZ >= minChunkZ && chunkZ <= maxChunkZ) {
                     Iterator iterator = trackedChunkVisibility.getBlocks().entrySet().iterator();

                     while (iterator.hasNext()) {
                        Entry entry = (Entry)iterator.next();
                        BlockPos blockPos = (BlockPos)entry.getKey();
                        int blockX = blockPos.getX();
                        int blockY = blockPos.getY();
                        int blockZ = blockPos.getZ();
                        double blockCenterX = blockX + 0.5;
                        double blockCenterY = blockY + 0.5;
                        double blockCenterZ = blockZ + 0.5;
                        double deltaX = eyeX - blockCenterX;
                        double deltaY = eyeY - blockCenterY;
                        double deltaZ = eyeZ - blockCenterZ;
                        double distanceSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
                        if (distanceSquared <= this.maxRayTraceDistanceSquared) {
                           boolean visible = false;
                           if (distanceSquared < this.visibilityDistanceSquared) {
                              int sectionY = blockY >> 4;

                              for (int i = 0; i < viewSnapshots.length; i++) {
                                 PlayerViewSnapshot playerViewSnapshot = viewSnapshots[i];
                                 Vector direction = playerViewSnapshot.getDirection();
                                 double dirX = direction.getX();
                                 double dirY = direction.getY();
                                 double dirZ = direction.getZ();
                                 this.occlusionChecker.initSectionCache(levelChunk, chunkX, sectionY, chunkZ);
                                 if (i == 0) {
                                    if (this.voxelRayTracer.traceVisibility(blockX, blockY, blockZ, blockCenterX, blockCenterY, blockCenterZ, deltaX, deltaY, deltaZ, distanceSquared, dirX, dirY, dirZ)) {
                                       visible = true;
                                       break;
                                    }
                                 } else {
                                    Vector altEyePos = playerViewSnapshot.getVector();
                                    double altDeltaX = altEyePos.getX() - blockCenterX;
                                    double altDeltaY = altEyePos.getY() - blockCenterY;
                                    double altDeltaZ = altEyePos.getZ() - blockCenterZ;
                                    double altDistanceSquared = altDeltaX * altDeltaX + altDeltaY * altDeltaY + altDeltaZ * altDeltaZ;
                                    if (this.voxelRayTracer.traceVisibility(blockX, blockY, blockZ, blockCenterX, blockCenterY, blockCenterZ, altDeltaX, altDeltaY, altDeltaZ, altDistanceSquared, dirX, dirY, dirZ)) {
                                       visible = true;
                                       break;
                                    }
                                 }
                              }
                           }

                           boolean currentlyHidden = (Boolean)entry.getValue();
                           if (visible) {
                              if (currentlyHidden) {
                                 pendingUpdates.add(new BlockVisibilityUpdate(trackedChunkVisibility, blockPos, true));
                                 if (this.reHideBlocks) {
                                    entry.setValue(false);
                                 } else {
                                    iterator.remove();
                                 }
                              }
                           } else if (!currentlyHidden) {
                              pendingUpdates.add(new BlockVisibilityUpdate(trackedChunkVisibility, blockPos, false));
                              entry.setValue(true);
                           }
                        }
                     }
                  }
               }
            }
         }

         this.occlusionChecker.clearCache();
      }
   }

   public static BlockState getBlockStateSafe(LevelChunkSection levelChunkSection, int blockX, int blockY, int blockZ) {
      try {
         return levelChunkSection.getBlockState(blockX & 15, blockY & 15, blockZ & 15);
      } catch (MissingPaletteEntryException ex) {
         return AIR;
      }
   }

   public Void call() throws Exception {
      return this.executeRayTrace();
   }

   private interface CachedOcclusionChecker extends VoxelRayTracer.OcclusionChecker {
      void clearCache();

      void initSectionCache(LevelChunk levelChunk, int chunkX, int sectionY, int chunkZ);
   }
}