package sakuyaayane.nekoantixray;

import java.util.UUID;
import lombok.Generated;

public final class PlayerChunkKey {
   private final UUID worldId;
   private final long chunkPos;

   @Generated
   public UUID getWorldId() {
      return this.worldId;
   }

   @Generated
   public long getChunkPos() {
      return this.chunkPos;
   }

   @Generated
   public PlayerChunkKey(UUID worldId, long chunkPos) {
      this.worldId = worldId;
      this.chunkPos = chunkPos;
   }

   @Generated
   @Override
   public boolean equals(Object other) {
      if (other == this) {
         return true;
      }

      if (other instanceof PlayerChunkKey otherKey) {
         if (this.getChunkPos() != otherKey.getChunkPos()) {
            return false;
         }

         UUID currentWorldId = this.getWorldId();
         UUID otherWorldId = otherKey.getWorldId();
         return currentWorldId == null ? otherWorldId == null : currentWorldId.equals(otherWorldId);
      } else {
         return false;
      }
   }

   @Generated
   @Override
   public int hashCode() {
      long currentChunkPos = this.getChunkPos();
      int result = 59 + (int)(currentChunkPos >>> 32 ^ currentChunkPos);
      UUID currentWorldId = this.getWorldId();
      return result * 59 + (currentWorldId == null ? 43 : currentWorldId.hashCode());
   }
}
