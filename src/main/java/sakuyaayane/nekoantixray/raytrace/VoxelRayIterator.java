package sakuyaayane.nekoantixray.raytrace;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class VoxelRayIterator implements Iterator<int[]> {
   private int blockX;
   private int blockY;
   private int blockZ;
   private int stepX;
   private int stepY;
   private int stepZ;
   private double maxDistance;
   private double tMaxX;
   private double tMaxY;
   private double tMaxZ;
   private double tDeltaX;
   private double tDeltaY;
   private double tDeltaZ;
   private int[] ref = new int[3];
   private int[] swapBuffer = new int[3];
   private int[] next;

   public VoxelRayIterator(double startX, double startY, double startZ, double endX, double endY, double endZ) {
      this.initializeSegment(startX, startY, startZ, endX, endY, endZ);
   }

   public VoxelRayIterator(
      int startBlockX, int startBlockY, int startBlockZ, double startX, double startY, double startZ, double endX, double endY, double endZ
   ) {
      this.initializeSegmentFromBlock(startBlockX, startBlockY, startBlockZ, startX, startY, startZ, endX, endY, endZ);
   }

   public VoxelRayIterator(double startX, double startY, double startZ, double dirX, double dirY, double dirZ, double distance) {
      this.initializeRay(startX, startY, startZ, dirX, dirY, dirZ, distance);
   }

   public VoxelRayIterator(
      int startBlockX, int startBlockY, int startBlockZ, double startX, double startY, double startZ, double dirX, double dirY, double dirZ, double distance
   ) {
      this.initializeWithDirection(startBlockX, startBlockY, startBlockZ, startX, startY, startZ, dirX, dirY, dirZ, distance);
   }

   public VoxelRayIterator(double startX, double startY, double startZ, double dirX, double dirY, double dirZ, double distance, boolean normalized) {
      if (normalized) {
         this.initializeNormalizedRay(startX, startY, startZ, dirX, dirY, dirZ, distance);
      } else {
         this.initializeRay(startX, startY, startZ, dirX, dirY, dirZ, distance);
      }
   }

   public VoxelRayIterator(
      int startBlockX,
      int startBlockY,
      int startBlockZ,
      double startX,
      double startY,
      double startZ,
      double dirX,
      double dirY,
      double dirZ,
      double distance,
      boolean normalized
   ) {
      if (normalized) {
         this.initializeNormalized(startBlockX, startBlockY, startBlockZ, startX, startY, startZ, dirX, dirY, dirZ, distance);
      } else {
         this.initializeWithDirection(startBlockX, startBlockY, startBlockZ, startX, startY, startZ, dirX, dirY, dirZ, distance);
      }
   }

   public VoxelRayIterator initializeNormalized(
      int startBlockX, int startBlockY, int startBlockZ, double startX, double startY, double startZ, double dirX, double dirY, double dirZ, double maxDistance
   ) {
      this.blockX = startBlockX;
      this.blockY = startBlockY;
      this.blockZ = startBlockZ;
      this.maxDistance = maxDistance;
      this.stepX = dirX < 0.0 ? -1 : 1;
      this.stepY = dirY < 0.0 ? -1 : 1;
      this.stepZ = dirZ < 0.0 ? -1 : 1;
      this.tMaxX = dirX == 0.0 ? Double.POSITIVE_INFINITY : (startBlockX + (this.stepX + 1) / 2 - startX) / dirX;
      this.tMaxY = dirY == 0.0 ? Double.POSITIVE_INFINITY : (startBlockY + (this.stepY + 1) / 2 - startY) / dirY;
      this.tMaxZ = dirZ == 0.0 ? Double.POSITIVE_INFINITY : (startBlockZ + (this.stepZ + 1) / 2 - startZ) / dirZ;
      this.tDeltaX = 1.0 / Math.abs(dirX);
      this.tDeltaY = 1.0 / Math.abs(dirY);
      this.tDeltaZ = 1.0 / Math.abs(dirZ);
      this.next = this.ref;
      this.ref[0] = startBlockX;
      this.ref[1] = startBlockY;
      this.ref[2] = startBlockZ;
      return this;
   }

   @Override
   public boolean hasNext() {
      return this.next != null;
   }

   public VoxelRayIterator initializeSegment(double startX, double startY, double startZ, double endX, double endY, double endZ) {
      return this.initializeSegmentFromBlock(fastFloor(startX), fastFloor(startY), fastFloor(startZ), startX, startY, startZ, endX, endY, endZ);
   }

   public VoxelRayIterator initializeSegmentFromBlock(
      int startBlockX, int startBlockY, int startBlockZ, double startX, double startY, double startZ, double endX, double endY, double endZ
   ) {
      double deltaX = endX - startX;
      double deltaY = endY - startY;
      double deltaZ = endZ - startZ;
      double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
      double divisor = distance == 0.0 ? Double.NaN : distance;
      deltaX /= divisor;
      deltaY /= divisor;
      deltaZ /= divisor;
      return this.initializeNormalized(startBlockX, startBlockY, startBlockZ, startX, startY, startZ, deltaX, deltaY, deltaZ, distance);
   }

   public VoxelRayIterator initializeRay(double startX, double startY, double startZ, double dirX, double dirY, double dirZ, double distance) {
      return this.initializeWithDirection(fastFloor(startX), fastFloor(startY), fastFloor(startZ), startX, startY, startZ, dirX, dirY, dirZ, distance);
   }

   public VoxelRayIterator initializeWithDirection(
      int startBlockX, int startBlockY, int startBlockZ, double startX, double startY, double startZ, double dirX, double dirY, double dirZ, double distance
   ) {
      double directionSign = Math.signum(distance);
      double signedDirX = dirX * directionSign;
      double signedDirY = dirY * directionSign;
      double signedDirZ = dirZ * directionSign;
      double length = Math.sqrt(signedDirX * signedDirX + signedDirY * signedDirY + signedDirZ * signedDirZ);
      if (length == 0.0) {
         length = Double.NaN;
      }

      dirX = signedDirX / length;
      dirY = signedDirY / length;
      dirZ = signedDirZ / length;
      return this.initializeNormalized(startBlockX, startBlockY, startBlockZ, startX, startY, startZ, dirX, dirY, dirZ, Math.abs(distance));
   }

   public VoxelRayIterator initializeNormalizedRay(double startX, double startY, double startZ, double dirX, double dirY, double dirZ, double distance) {
      return this.initializeNormalized(fastFloor(startX), fastFloor(startY), fastFloor(startZ), startX, startY, startZ, dirX, dirY, dirZ, Math.abs(distance));
   }

   public int[] advanceVoxel() {
      if (this.tMaxX < this.tMaxY) {
         if (this.tMaxZ < this.tMaxX) {
            if (this.tMaxZ <= this.maxDistance) {
               this.blockZ = this.blockZ + this.stepZ;
               this.ref[0] = this.blockX;
               this.ref[1] = this.blockY;
               this.ref[2] = this.blockZ;
               this.tMaxZ = this.tMaxZ + this.tDeltaZ;
            } else {
               this.next = null;
            }
         } else if (this.tMaxX <= this.maxDistance) {
            if (this.tMaxZ == this.tMaxX) {
               this.blockZ = this.blockZ + this.stepZ;
               this.tMaxZ = this.tMaxZ + this.tDeltaZ;
            }

            this.blockX = this.blockX + this.stepX;
            this.ref[0] = this.blockX;
            this.ref[1] = this.blockY;
            this.ref[2] = this.blockZ;
            this.tMaxX = this.tMaxX + this.tDeltaX;
         } else {
            this.next = null;
         }
      } else if (this.tMaxY < this.tMaxZ) {
         if (this.tMaxY <= this.maxDistance) {
            if (this.tMaxX == this.tMaxY) {
               this.blockX = this.blockX + this.stepX;
               this.tMaxX = this.tMaxX + this.tDeltaX;
            }

            this.blockY = this.blockY + this.stepY;
            this.ref[0] = this.blockX;
            this.ref[1] = this.blockY;
            this.ref[2] = this.blockZ;
            this.tMaxY = this.tMaxY + this.tDeltaY;
         } else {
            this.next = null;
         }
      } else if (this.tMaxZ <= this.maxDistance) {
         if (this.tMaxX == this.tMaxZ) {
            this.blockX = this.blockX + this.stepX;
            this.tMaxX = this.tMaxX + this.tDeltaX;
         }

         if (this.tMaxY == this.tMaxZ) {
            this.blockY = this.blockY + this.stepY;
            this.tMaxY = this.tMaxY + this.tDeltaY;
         }

         this.blockZ = this.blockZ + this.stepZ;
         this.ref[0] = this.blockX;
         this.ref[1] = this.blockY;
         this.ref[2] = this.blockZ;
         this.tMaxZ = this.tMaxZ + this.tDeltaZ;
      } else {
         this.next = null;
      }

      return this.next;
   }

   private static int fastFloor(double coord) {
      int floor = (int)coord;
      return coord < floor ? floor - 1 : floor;
   }

   public int[] nextVoxel() {
      int[] currentVoxel = this.next;
      if (currentVoxel == null) {
         throw new NoSuchElementException();
      }

      this.ref = this.swapBuffer;
      this.swapBuffer = this.ref;
      this.next = this.ref;
      this.advanceVoxel();
      return currentVoxel;
   }

   public int[] next() {
      return this.nextVoxel();
   }
}