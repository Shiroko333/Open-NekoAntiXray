package sakuyaayane.nekoantixray;

import lombok.Generated;
import net.minecraft.core.BlockPos;

public final class BlockVisibilityUpdate {
   private final TrackedChunkVisibility trackedChunkVisibility;
   private final BlockPos block;
   private final boolean visible;

   @Generated
   public TrackedChunkVisibility getTrackedChunkVisibility() {
      return this.trackedChunkVisibility;
   }

   @Generated
   public BlockPos getBlock() {
      return this.block;
   }

   @Generated
   public boolean isVisible() {
      return this.visible;
   }

   @Generated
   public BlockVisibilityUpdate(TrackedChunkVisibility trackedChunkVisibility, BlockPos blockPos, boolean visible) {
      this.trackedChunkVisibility = trackedChunkVisibility;
      this.block = blockPos;
      this.visible = visible;
   }
}
