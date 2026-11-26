package sakuyaayane.nekoantixray.compat;

public interface ChunkAccessWrapper {
   boolean saveSecureSlimeChunkResult(boolean isSlimeChunk);

   int getChunkX();

   int getChunkZ();

   boolean isValid();

   Object getChunk();

   static ChunkAccessWrapper create(Object chunk, int chunkX, int chunkZ) {
      return ChunkAccessWrapperFactory.createWrapper(chunk, chunkX, chunkZ);
   }

   Boolean getSecureSlimeChunkResult();
}
