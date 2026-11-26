package sakuyaayane.nekoantixray.chunk;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.World;
import sakuyaayane.nekoantixray.compat.ServerLevelAccess;
import sakuyaayane.nekoantixray.NekoAntiXray;
import sakuyaayane.nekoantixray.raytrace.PlayerRayTraceState;
import sakuyaayane.nekoantixray.world.ChunkCoordinateCodec;
import sakuyaayane.nekoantixray.world.ChunkKey;
import sakuyaayane.nekoantixray.world.TrackedChunkVisibility;

public final class ChunkRefreshService {
   private final NekoAntiXray plugin;

   public ChunkRefreshService(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   public int resendChunks(Player player, int radius) {
      if (player != null && player.isOnline() && radius >= 1) {
         Location location = player.getLocation();
         World world = location.getWorld();
         if (world == null) {
            return 0;
         }

         int centerChunkX = location.getBlockX() >> 4;
         int centerChunkZ = location.getBlockZ() >> 4;
         int maxViewDistance = Math.max(2, world.getViewDistance());
         int effectiveRadius = Math.min(radius, maxViewDistance);
         int scheduledCount = 0;

         for (int dx = -effectiveRadius; dx <= effectiveRadius; dx++) {
            for (int dz = -effectiveRadius; dz <= effectiveRadius; dz++) {
               int chunkX = centerChunkX + dx;
               int chunkZ = centerChunkZ + dz;
               if (world.isChunkLoaded(chunkX, chunkZ)) {
                  scheduledCount++;
                  Location chunkCenter = new Location(world, (chunkX << 4) + 8.0, world.getMinHeight() + 1.0, (chunkZ << 4) + 8.0);
                  this.plugin.getFoliaScheduler().getImpl().runAtLocation(chunkCenter, wrappedTask -> this.resendSingleChunk(player, world, chunkX, chunkZ));
               }
            }
         }

         return scheduledCount;
      } else {
         return 0;
      }
   }

   private void resendSingleChunk(Player player, World world, int chunkX, int chunkZ) {
      if (player != null && player.isOnline() && player.getWorld().equals(world)) {
         ServerPlayer serverPlayer = ((CraftPlayer)player).getHandle();
         ServerLevel serverLevel = ServerLevelAccess.getServerLevel(serverPlayer);
         LevelChunk levelChunk = serverLevel.getChunkIfLoaded(chunkX, chunkZ);
         if (levelChunk != null) {
            PlayerChunkSender.sendChunk(serverPlayer.connection, serverLevel, levelChunk);
            PlayerRayTraceState playerRayTraceState = (PlayerRayTraceState)this.plugin.getPlayerRayTraceStates().get(player.getUniqueId());
            if (playerRayTraceState != null) {
               playerRayTraceState.getChunks().remove(new ChunkKey(ChunkCoordinateCodec.encode(levelChunk.getPos())));
            }
         }
      }
   }

   public int refreshBlocks(Player player, int radius, boolean reveal) {
      if (player != null && player.isOnline() && radius >= 1) {
         Location location = player.getLocation();
         World world = location.getWorld();
         if (world == null) {
            return 0;
         } else {
            int centerChunkX = location.getBlockX() >> 4;
            int centerChunkZ = location.getBlockZ() >> 4;
            if (this.plugin.isFoliaServer()) {
               this.plugin.getFoliaScheduler().getImpl().runAtEntity(player, wrappedTask -> this.applyBlockRefresh(player, world, centerChunkX, centerChunkZ, radius, reveal));
               return 0;
            } else {
               return this.applyBlockRefresh(player, world, centerChunkX, centerChunkZ, radius, reveal);
            }
         }
      } else {
         return 0;
      }
   }

   private int applyBlockRefresh(Player player, World world, int centerChunkX, int centerChunkZ, int radius, boolean reveal) {
      if (player.isOnline() && player.getWorld().equals(world)) {
         PlayerRayTraceState playerRayTraceState = (PlayerRayTraceState)this.plugin.getPlayerRayTraceStates().get(player.getUniqueId());
         if (playerRayTraceState == null) {
            return 0;
         }

         ServerPlayer serverPlayer = ((CraftPlayer)player).getHandle();
         ServerLevel serverLevel = ServerLevelAccess.getServerLevel(serverPlayer);
         Environment environment = world.getEnvironment();
         int updatedCount = 0;

         Collection<TrackedChunkVisibility> trackedChunks = playerRayTraceState.getChunks().values();
         for (TrackedChunkVisibility trackedChunkVisibility : trackedChunks) {
            LevelChunk chunk = trackedChunkVisibility.getChunk();
            if (chunk != null) {
               int chunkX = ChunkCoordinateCodec.getChunkX(chunk.getPos());
               int chunkZ = ChunkCoordinateCodec.getChunkZ(chunk.getPos());
               if (Math.abs(chunkX - centerChunkX) <= radius && Math.abs(chunkZ - centerChunkZ) <= radius) {
                  Map<BlockPos, Boolean> trackedBlocks = trackedChunkVisibility.getBlocks();
                  for (Entry<BlockPos, Boolean> entry : trackedBlocks.entrySet()) {
                     boolean currentlyHidden = Boolean.TRUE.equals(entry.getValue());
                     if (currentlyHidden != !reveal) {
                        BlockPos blockPos = entry.getKey();
                        BlockState blockState = reveal ? serverLevel.getBlockState(blockPos) : getFallbackMaskBlockState(environment, blockPos.getY());
                        serverPlayer.connection.send(new ClientboundBlockUpdatePacket(blockPos, blockState));
                        entry.setValue(!reveal);
                        updatedCount++;
                     }
                  }
               }
            }
         }

         this.plugin.logDebug("[Resend] 方块纠正完成: 玩家=" + player.getName() + ", reveal=" + reveal + ", 数量=" + updatedCount);
         return updatedCount;
      } else {
         return 0;
      }
   }

   public int hideObfuscationTargets(Player player, int radius) {
      if (player != null && player.isOnline() && radius >= 1) {
         Location location = player.getLocation();
         World world = location.getWorld();
         if (world == null) {
            return 0;
         } else {
            int centerChunkX = location.getBlockX() >> 4;
            int centerChunkZ = location.getBlockZ() >> 4;
            if (this.plugin.isFoliaServer()) {
               this.plugin.getFoliaScheduler().getImpl().runAtEntity(player, wrappedTask -> this.applyWorldHide(player, world, centerChunkX, centerChunkZ, radius));
               return 0;
            } else {
               return this.applyWorldHide(player, world, centerChunkX, centerChunkZ, radius);
            }
         }
      } else {
         return 0;
      }
   }

   private int applyWorldHide(Player player, World world, int centerChunkX, int centerChunkZ, int radius) {
      if (player.isOnline() && player.getWorld().equals(world)) {
         ServerPlayer serverPlayer = ((CraftPlayer)player).getHandle();
         ServerLevel serverLevel = ServerLevelAccess.getServerLevel(serverPlayer);
         if (!(serverLevel.chunkPacketBlockController instanceof EnhancedChunkObfuscationController enhancedChunkObfuscationController)) {
            this.plugin.logDebug("[Resend] 世界未安装增强反X光控制器(heavy-mode 未开启?), 跳过反向隐藏");
            return 0;
         } else {
            Environment environment = world.getEnvironment();
            PlayerRayTraceState playerRayTraceState = (PlayerRayTraceState)this.plugin.getPlayerRayTraceStates().get(player.getUniqueId());
            int hiddenCount = 0;
            int gridDiameter = radius * 2 + 1;
            int[][] chunkOffsets = new int[gridDiameter * gridDiameter][2];
            int offsetIndex = 0;

            for (int dx = -radius; dx <= radius; dx++) {
               for (int dz = -radius; dz <= radius; dz++) {
                  chunkOffsets[offsetIndex][0] = dx;
                  chunkOffsets[offsetIndex][1] = dz;
                  offsetIndex++;
               }
            }

            Arrays.sort(chunkOffsets, Comparator.comparingInt(offset -> Math.max(Math.abs(offset[0]), Math.abs(offset[1]))));

            for (int[] offset : chunkOffsets) {
               if (hiddenCount >= 4096) {
                  break;
               }

               int offsetX = offset[0];
               int offsetZ = offset[1];
               LevelChunk levelChunk = serverLevel.getChunkIfLoaded(centerChunkX + offsetX, centerChunkZ + offsetZ);
               if (levelChunk != null) {
                  int chunkX = ChunkCoordinateCodec.getChunkX(levelChunk.getPos());
                  int chunkZ = ChunkCoordinateCodec.getChunkZ(levelChunk.getPos());
                  TrackedChunkVisibility trackedChunkVisibility = playerRayTraceState == null
                     ? null
                     : (TrackedChunkVisibility)playerRayTraceState.getChunks().get(new ChunkKey(ChunkCoordinateCodec.encode(levelChunk.getPos())));
                  LevelChunkSection[] sections = levelChunk.getSections();
                  int minSectionY = levelChunk.getMinSectionY();

                  for (int sectionIndex = 0; sectionIndex < sections.length && hiddenCount < 4096; sectionIndex++) {
                     LevelChunkSection levelChunkSection = sections[sectionIndex];
                     if (levelChunkSection != null && !levelChunkSection.hasOnlyAir() && this.sectionHasObfuscationTarget(levelChunkSection, enhancedChunkObfuscationController)) {
                        int sectionBaseY = minSectionY + sectionIndex << 4;

                        for (int localY = 0; localY < 16 && hiddenCount < 4096; localY++) {
                           for (int localZ = 0; localZ < 16 && hiddenCount < 4096; localZ++) {
                              for (int localX = 0; localX < 16 && hiddenCount < 4096; localX++) {
                                 BlockState blockState = levelChunkSection.getBlockState(localX, localY, localZ);
                                 if (enhancedChunkObfuscationController.isObfuscatedBlockState(blockState)) {
                                    BlockPos blockPos = new BlockPos((chunkX << 4) + localX, sectionBaseY + localY, (chunkZ << 4) + localZ);
                                    serverPlayer.connection.send(new ClientboundBlockUpdatePacket(blockPos, getFallbackMaskBlockState(environment, blockPos.getY())));
                                    if (trackedChunkVisibility != null) {
                                       trackedChunkVisibility.getBlocks().put(blockPos, Boolean.TRUE);
                                    }

                                    hiddenCount++;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            this.plugin.logDebug("[Resend] 反向隐藏完成: 玩家=" + player.getName() + ", 数量=" + hiddenCount);
            return hiddenCount;
         }
      } else {
         return 0;
      }
   }

   private boolean sectionHasObfuscationTarget(LevelChunkSection levelChunkSection, EnhancedChunkObfuscationController enhancedChunkObfuscationController) {
      boolean[] foundTarget = new boolean[]{false};
      levelChunkSection.getStates().getAll(blockState -> {
         if (!foundTarget[0] && enhancedChunkObfuscationController.isObfuscatedBlockState(blockState)) {
            foundTarget[0] = true;
         }
      });
      return foundTarget[0];
   }

   public String getTrackingStatusSummary(Player player, int radius) {
      if (player != null && player.isOnline()) {
         PlayerRayTraceState playerRayTraceState = (PlayerRayTraceState)this.plugin.getPlayerRayTraceStates().get(player.getUniqueId());
         if (playerRayTraceState == null) {
            return "无射线追踪数据(heavy-mode 未开启或该世界未启用增强反矿透 → resend/方块纠正都无内容)";
         }

         Location location = player.getLocation();
         World world = location.getWorld();
         if (world == null) {
            return "世界为空";
         }

         int centerChunkX = location.getBlockX() >> 4;
         int centerChunkZ = location.getBlockZ() >> 4;
         int trackedChunkCount = 0;
         int fakeBlockCount = 0;
         int realBlockCount = 0;

         Collection<TrackedChunkVisibility> trackedChunks = playerRayTraceState.getChunks().values();
         for (TrackedChunkVisibility trackedChunkVisibility : trackedChunks) {
            LevelChunk chunk = trackedChunkVisibility.getChunk();
            if (chunk != null) {
               int chunkX = ChunkCoordinateCodec.getChunkX(chunk.getPos());
               int chunkZ = ChunkCoordinateCodec.getChunkZ(chunk.getPos());
               if (Math.abs(chunkX - centerChunkX) <= radius && Math.abs(chunkZ - centerChunkZ) <= radius) {
                  trackedChunkCount++;

                  Collection<Boolean> blockStates = trackedChunkVisibility.getBlocks().values();
                  for (Boolean isFakeBlock : blockStates) {
                     if (Boolean.TRUE.equals(isFakeBlock)) {
                        fakeBlockCount++;
                     } else {
                        realBlockCount++;
                     }
                  }
               }
            }
         }

         return "追踪区块=" + trackedChunkCount + " §7| §7客户端当前: 假方块=" + fakeBlockCount + ", 真实方块=" + realBlockCount;
      } else {
         return "玩家不在线";
      }
   }

   private static BlockState getFallbackMaskBlockState(Environment environment, int blockY) {
      if (environment == Environment.NETHER) {
         return Blocks.NETHERRACK.defaultBlockState();
      } else if (environment == Environment.THE_END) {
         return Blocks.END_STONE.defaultBlockState();
      } else {
         return blockY < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
      }
   }
}
