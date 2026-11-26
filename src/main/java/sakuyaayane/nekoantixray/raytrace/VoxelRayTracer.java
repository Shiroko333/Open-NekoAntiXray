package sakuyaayane.nekoantixray.raytrace;

import java.util.function.Consumer;

public final class VoxelRayTracer {
   private static final VoxelRayTracer.VoxelStep STEP_POS_X = coords -> coords[0]++;
   private static final VoxelRayTracer.VoxelStep STEP_NEG_X = coords -> coords[0]--;
   private static final VoxelRayTracer.VoxelStep STEP_POS_Y = coords -> coords[1]++;
   private static final VoxelRayTracer.VoxelStep STEP_NEG_Y = coords -> coords[1]--;
   private static final VoxelRayTracer.VoxelStep STEP_POS_Z = coords -> coords[2]++;
   private static final VoxelRayTracer.VoxelStep STEP_NEG_Z = coords -> coords[2]--;
   private static final VoxelRayTracer.VoxelStep[] STEPS_POS_Y_POS_Z = new VoxelRayTracer.VoxelStep[]{STEP_POS_Y, STEP_POS_Z, STEP_NEG_Y};
   private static final VoxelRayTracer.VoxelStep[] STEPS_POS_Y_NEG_Z = new VoxelRayTracer.VoxelStep[]{STEP_POS_Y, STEP_NEG_Z, STEP_NEG_Y};
   private static final VoxelRayTracer.VoxelStep[] STEPS_NEG_Y_POS_Z = new VoxelRayTracer.VoxelStep[]{STEP_NEG_Y, STEP_POS_Z, STEP_POS_Y};
   private static final VoxelRayTracer.VoxelStep[] STEPS_NEG_Y_NEG_Z = new VoxelRayTracer.VoxelStep[]{STEP_NEG_Y, STEP_NEG_Z, STEP_POS_Y};
   private static final VoxelRayTracer.VoxelStep[] STEPS_POS_Z_POS_X = new VoxelRayTracer.VoxelStep[]{STEP_POS_Z, STEP_POS_X, STEP_NEG_Z};
   private static final VoxelRayTracer.VoxelStep[] STEPS_POS_Z_NEG_X = new VoxelRayTracer.VoxelStep[]{STEP_POS_Z, STEP_NEG_X, STEP_NEG_Z};
   private static final VoxelRayTracer.VoxelStep[] STEPS_NEG_Z_POS_X = new VoxelRayTracer.VoxelStep[]{STEP_NEG_Z, STEP_POS_X, STEP_POS_Z};
   private static final VoxelRayTracer.VoxelStep[] STEPS_NEG_Z_NEG_X = new VoxelRayTracer.VoxelStep[]{STEP_NEG_Z, STEP_NEG_X, STEP_POS_Z};
   private static final VoxelRayTracer.VoxelStep[] STEPS_POS_Y_POS_X = new VoxelRayTracer.VoxelStep[]{STEP_POS_Y, STEP_POS_X, STEP_NEG_Y};
   private static final VoxelRayTracer.VoxelStep[] STEPS_NEG_Y_POS_X = new VoxelRayTracer.VoxelStep[]{STEP_NEG_Y, STEP_POS_X, STEP_POS_Y};
   private static final VoxelRayTracer.VoxelStep[] STEPS_POS_Y_NEG_X = new VoxelRayTracer.VoxelStep[]{STEP_POS_Y, STEP_NEG_X, STEP_NEG_Y};
   private static final VoxelRayTracer.VoxelStep[] STEPS_NEG_Y_NEG_X = new VoxelRayTracer.VoxelStep[]{STEP_NEG_Y, STEP_NEG_X, STEP_POS_Y};
   private final VoxelRayTracer.RayIteratorSupplier rayIteratorSupplier;
   private final VoxelRayTracer.OcclusionChecker occlusionChecker;
   private final boolean cullBehindView;

   public VoxelRayTracer(VoxelRayTracer.RayIteratorSupplier rayIteratorSupplier, VoxelRayTracer.OcclusionChecker occlusionChecker, boolean cullBehindView) {
      this.rayIteratorSupplier = rayIteratorSupplier;
      this.occlusionChecker = occlusionChecker;
      this.cullBehindView = cullBehindView;
   }

   public boolean isBlockVisible(int targetBlockX, int targetBlockY, int targetBlockZ, double viewerX, double viewerY, double viewerZ, double viewDirX, double viewDirY, double viewDirZ) {
      double blockCenterX = targetBlockX + 0.5;
      double blockCenterY = targetBlockY + 0.5;
      double blockCenterZ = targetBlockZ + 0.5;
      double deltaX = viewerX - blockCenterX;
      double deltaY = viewerY - blockCenterY;
      double deltaZ = viewerZ - blockCenterZ;
      return this.traceVisibility(
         targetBlockX,
         targetBlockY,
         targetBlockZ,
         blockCenterX,
         blockCenterY,
         blockCenterZ,
         deltaX,
         deltaY,
         deltaZ,
         deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ,
         viewDirX,
         viewDirY,
         viewDirZ
      );
   }

   public boolean traceVisibility(
      int targetBlockX,
      int targetBlockY,
      int targetBlockZ,
      double startX,
      double startY,
      double startZ,
      double deltaX,
      double deltaY,
      double deltaZ,
      double distanceSquared,
      double viewDirX,
      double viewDirY,
      double viewDirZ
   ) {
      if (this.cullBehindView && (deltaX - viewDirX) * viewDirX + (deltaY - viewDirY) * viewDirY + (deltaZ - viewDirZ) * viewDirZ > 0.0) {
         return false;
      }

      double distance = Math.sqrt(distanceSquared);
      double safeDistance = distance == 0.0 ? Double.NaN : distance;
      VoxelRayIterator voxelRayIterator = this.rayIteratorSupplier.createIterator(
         targetBlockX, targetBlockY, targetBlockZ, startX, startY, startZ, deltaX / safeDistance, deltaY / safeDistance, deltaZ / safeDistance, distance
      );

      int[] voxelCoords;
      while ((voxelCoords = voxelRayIterator.advanceVoxel()) != null) {
         int voxelX = voxelCoords[0];
         int voxelY = voxelCoords[1];
         int voxelZ = voxelCoords[2];
         if (this.occlusionChecker.isRayBlockOccluding(voxelX, voxelY, voxelZ)) {
            if (this.isFullyOccludedAtVoxel(targetBlockX, targetBlockY, targetBlockZ, voxelCoords, voxelX, voxelY, voxelZ, deltaX, deltaY, deltaZ)) {
               return false;
            }
         }
      }

      return true;
   }

   private boolean isFullyOccludedAtVoxel(
      int targetBlockX, int targetBlockY, int targetBlockZ, int[] voxelCoords, int voxelX, int voxelY, int voxelZ, double deltaX, double deltaY, double deltaZ
   ) {
      double absDeltaX = Math.abs(deltaX);
      double absDeltaY = Math.abs(deltaY);
      double absDeltaZ = Math.abs(deltaZ);
      double offsetX = voxelX - targetBlockX;
      double offsetY = voxelY - targetBlockY;
      double offsetZ = voxelZ - targetBlockZ;
      VoxelRayTracer.VoxelStep[] neighborSteps;
      VoxelRayTracer.VoxelStep stepTowardTarget;
      VoxelRayTracer.VoxelStep stepAwayFromTarget;
      if (absDeltaX > absDeltaY) {
         if (absDeltaZ > absDeltaX) {
            double scaleZ = safeDivide(deltaZ, offsetZ);
            offsetX = safeMultiply(scaleZ, offsetX) - deltaX;
            offsetY = safeMultiply(scaleZ, offsetY) - deltaY;
            if (offsetX > 0.0) {
               if (offsetY > 0.0) {
                  neighborSteps = STEPS_NEG_Y_NEG_X;
               } else {
                  neighborSteps = STEPS_POS_Y_NEG_X;
               }
            } else if (offsetY > 0.0) {
               neighborSteps = STEPS_NEG_Y_POS_X;
            } else {
               neighborSteps = STEPS_POS_Y_POS_X;
            }

            if (deltaZ > 0.0) {
               stepTowardTarget = STEP_NEG_Z;
               stepAwayFromTarget = STEP_POS_Z;
            } else {
               stepTowardTarget = STEP_POS_Z;
               stepAwayFromTarget = STEP_NEG_Z;
            }
         } else {
            double scaleX = safeDivide(deltaX, offsetX);
            offsetY = safeMultiply(scaleX, offsetY) - deltaY;
            offsetZ = safeMultiply(scaleX, offsetZ) - deltaZ;
            if (offsetY > 0.0) {
               if (offsetZ > 0.0) {
                  neighborSteps = STEPS_NEG_Y_NEG_Z;
               } else {
                  neighborSteps = STEPS_NEG_Y_POS_Z;
               }
            } else if (offsetZ > 0.0) {
               neighborSteps = STEPS_POS_Y_NEG_Z;
            } else {
               neighborSteps = STEPS_POS_Y_POS_Z;
            }

            if (deltaX > 0.0) {
               stepTowardTarget = STEP_NEG_X;
               stepAwayFromTarget = STEP_POS_X;
            } else {
               stepTowardTarget = STEP_POS_X;
               stepAwayFromTarget = STEP_NEG_X;
            }
         }
      } else if (absDeltaY > absDeltaZ) {
         double scaleY = safeDivide(deltaY, offsetY);
         offsetZ = safeMultiply(scaleY, offsetZ) - deltaZ;
         offsetX = safeMultiply(scaleY, offsetX) - deltaX;
         if (offsetZ > 0.0) {
            if (offsetX > 0.0) {
               neighborSteps = STEPS_NEG_Z_NEG_X;
            } else {
               neighborSteps = STEPS_NEG_Z_POS_X;
            }
         } else if (offsetX > 0.0) {
            neighborSteps = STEPS_POS_Z_NEG_X;
         } else {
            neighborSteps = STEPS_POS_Z_POS_X;
         }

         if (deltaY > 0.0) {
            stepTowardTarget = STEP_NEG_Y;
            stepAwayFromTarget = STEP_POS_Y;
         } else {
            stepTowardTarget = STEP_POS_Y;
            stepAwayFromTarget = STEP_NEG_Y;
         }
      } else {
         double scaleZ = safeDivide(deltaZ, offsetZ);
         offsetX = safeMultiply(scaleZ, offsetX) - deltaX;
         offsetY = safeMultiply(scaleZ, offsetY) - deltaY;
         if (offsetX > 0.0) {
            if (offsetY > 0.0) {
               neighborSteps = STEPS_NEG_Y_NEG_X;
            } else {
               neighborSteps = STEPS_POS_Y_NEG_X;
            }
         } else if (offsetY > 0.0) {
            neighborSteps = STEPS_NEG_Y_POS_X;
         } else {
            neighborSteps = STEPS_POS_Y_POS_X;
         }

         if (deltaZ > 0.0) {
            stepTowardTarget = STEP_NEG_Z;
            stepAwayFromTarget = STEP_POS_Z;
         } else {
            stepTowardTarget = STEP_POS_Z;
            stepAwayFromTarget = STEP_NEG_Z;
         }
      }

      int stepIndex = 0;

      while (true) {
         if (stepIndex >= neighborSteps.length) {
            return true;
         }

         neighborSteps[stepIndex].accept(voxelCoords);
         int neighborX = voxelCoords[0];
         int neighborY = voxelCoords[1];
         int neighborZ = voxelCoords[2];
         if (!this.occlusionChecker.isAdjacentBlockOccluding(neighborX, neighborY, neighborZ)) {
            stepTowardTarget.accept(voxelCoords);
            voxelX = voxelCoords[0];
            voxelY = voxelCoords[1];
            voxelZ = voxelCoords[2];
            if (voxelX == targetBlockX && voxelY == targetBlockY && voxelZ == targetBlockZ) {
               break;
            }

            if (!this.occlusionChecker.isAdjacentBlockOccluding(voxelX, voxelY, voxelZ)) {
               break;
            }

            stepAwayFromTarget.accept(voxelCoords);
         }

         stepIndex++;
      }

      return false;
   }

   private static double safeDivide(double numerator, double denominator) {
      return (denominator == 0.0 && !Double.isNaN(numerator) ? Math.copySign(1.0, numerator) : numerator) / denominator;
   }

   private static double safeMultiply(double factor, double scale) {
      return (scale == 0.0 ? Math.signum(factor) : factor) * scale;
   }

   @FunctionalInterface
   public interface OcclusionChecker {
      boolean isOccluding(int blockX, int blockY, int blockZ);

      default boolean isRayBlockOccluding(int blockX, int blockY, int blockZ) {
         return this.isOccluding(blockX, blockY, blockZ);
      }

      default boolean isAdjacentBlockOccluding(int blockX, int blockY, int blockZ) {
         return this.isOccluding(blockX, blockY, blockZ);
      }
   }

   @FunctionalInterface
   public interface RayIteratorSupplier {
      VoxelRayIterator createIterator(
         int startBlockX, int startBlockY, int startBlockZ, double startX, double startY, double startZ, double dirX, double dirY, double dirZ, double distance
      );
   }

   @FunctionalInterface
   private interface VoxelStep extends Consumer<int[]> {
   }
}
