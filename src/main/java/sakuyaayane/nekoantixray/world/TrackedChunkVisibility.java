package sakuyaayane.nekoantixray.world;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;

public final class TrackedChunkVisibility {
   private final Reference<LevelChunk> chunk;
   private final ChunkKey key;
   private final Map<BlockPos, Boolean> blocks;

   public TrackedChunkVisibility(LevelChunk levelChunk, Map map) {
      this.chunk = new WeakReference<>(levelChunk);
      this.key = new ChunkKey(ChunkCoordinateCodec.encode(levelChunk.getPos()));
      this.blocks = map;
   }

   public LevelChunk getChunk() {
      return this.chunk.get();
   }

   public ChunkKey getKey() {
      return this.key;
   }

   public Map getBlocks() {
      return this.blocks;
   }
}
