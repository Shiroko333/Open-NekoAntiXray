package sakuyaayane.nekoantixray.world;

import lombok.Generated;

public class ChunkKey {
   protected long value;

   @Generated
   public long getValue() {
      return this.value;
   }

   @Generated
   public ChunkKey(long packedKey) {
      this.value = packedKey;
   }

   @Generated
   @Override
   public boolean equals(Object other) {
      if (other == this) {
         return true;
      } else if (other instanceof ChunkKey chunkKey) {
         return !chunkKey.canEqual(this) ? false : this.getValue() == chunkKey.getValue();
      } else {
         return false;
      }
   }

   @Generated
   @Override
   public int hashCode() {
      long packedKey = this.getValue();
      return 59 + (int)(packedKey >>> 32 ^ packedKey);
   }

   protected boolean canEqual(Object other) {
      return other instanceof ChunkKey;
   }
}
