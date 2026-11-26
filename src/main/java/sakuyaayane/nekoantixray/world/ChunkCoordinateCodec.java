package sakuyaayane.nekoantixray.world;

import net.minecraft.world.level.ChunkPos;

public final class ChunkCoordinateCodec {
   private ChunkCoordinateCodec() {
   }

   public static long encode(int chunkX, int chunkZ) {
      return chunkX & 4294967295L | (chunkZ & 4294967295L) << 32;
   }

   public static int getChunkX(ChunkPos chunkPos) {
      return chunkPos.getMinBlockX() >> 4;
   }

   public static int getChunkZ(ChunkPos chunkPos) {
      return chunkPos.getMinBlockZ() >> 4;
   }

   public static long encode(ChunkPos chunkPos) {
      int chunkX = getChunkX(chunkPos);
      int chunkZ = getChunkZ(chunkPos);
      return encode(chunkX, chunkZ);
   }

   public static int decodeChunkX(long packedCoords) {
      return (int)(packedCoords & 4294967295L);
   }

   public static int decodeChunkZ(long packedCoords) {
      return (int)(packedCoords >>> 32);
   }
}
