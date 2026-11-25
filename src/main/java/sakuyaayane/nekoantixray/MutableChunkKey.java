package sakuyaayane.nekoantixray;

public final class MutableChunkKey extends ChunkKey {
   public MutableChunkKey(long chunkKey) {
      super(chunkKey);
   }

   public void setValue(long chunkKey) {
      this.value = chunkKey;
   }
}
