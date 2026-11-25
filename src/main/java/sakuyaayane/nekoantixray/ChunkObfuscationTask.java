package sakuyaayane.nekoantixray;

import io.papermc.paper.antixray.ChunkPacketInfo;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public final class ChunkObfuscationTask implements Runnable {
   private final ChunkPacketInfo<BlockState> chunkPacketInfo;
   private final ClientboundLevelChunkWithLightPacket chunkPacket;
   private final EnhancedChunkObfuscationController enhancedChunkObfuscationController;
   private LevelChunk[] nearbyChunks;
   private static final String OBFUSCATION_FAILURE_PREFIX = "区块混淆失败: ";

   public ChunkObfuscationTask(
      ChunkPacketInfo<BlockState> chunkPacketInfo,
      ClientboundLevelChunkWithLightPacket chunkPacket,
      EnhancedChunkObfuscationController enhancedChunkObfuscationController
   ) {
      this.chunkPacketInfo = chunkPacketInfo;
      this.chunkPacket = chunkPacket;
      this.enhancedChunkObfuscationController = enhancedChunkObfuscationController;
   }

   public ChunkPacketInfo getChunkPacketInfo() {
      return this.chunkPacketInfo;
   }

   public ClientboundLevelChunkWithLightPacket getChunkPacket() {
      return this.chunkPacket;
   }

   public LevelChunk[] getNearbyChunks() {
      return this.nearbyChunks;
   }

   @Override
   public void run() {
      try {
         this.enhancedChunkObfuscationController.obfuscate(this.chunkPacketInfo, this.nearbyChunks);
      } catch (Throwable ex) {
         this.enhancedChunkObfuscationController.getPlugin().getObfuscationStatistics().recordFailure();
         this.enhancedChunkObfuscationController.getPlugin().getLogger().warning("区块混淆失败: " + ex);

         try {
            this.chunkPacket.setReady(true);
         } catch (Throwable ignored) {
         }
      }
   }

   public void setNearbyChunks(LevelChunk[] nearbyChunks) {
      this.nearbyChunks = nearbyChunks;
   }
}
