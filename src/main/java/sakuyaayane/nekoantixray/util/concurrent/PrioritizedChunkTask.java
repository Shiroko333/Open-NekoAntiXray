package sakuyaayane.nekoantixray.util.concurrent;

import net.minecraft.world.level.ChunkPos;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import sakuyaayane.nekoantixray.world.ChunkCoordinateCodec;

public class PrioritizedChunkTask implements Runnable, Comparable<PrioritizedChunkTask> {
   private final Runnable task;
   private final int priority;
   private final long createdTimeNanos;
   private final ChunkPos chunkPos;
   private static final String TO_STRING_FORMAT = "PriorityChunkTask{priority=%d, chunk=%s, waitTime=%dms}";
   private static final long NANOS_PER_MILLI = 1000000L;

   public PrioritizedChunkTask(Runnable task, int priority) {
      this.task = task;
      this.chunkPos = null;
      this.createdTimeNanos = System.nanoTime();
      this.priority = priority;
   }

   @Override
   public void run() {
      this.task.run();
   }

   public int getPriority() {
      return this.priority;
   }

   public ChunkPos getChunkPos() {
      return this.chunkPos;
   }

   public long getCreatedTimeNanos() {
      return this.createdTimeNanos;
   }

   @Override
   public String toString() {
      return String.format("PriorityChunkTask{priority=%d, chunk=%s, waitTime=%dms}", this.priority, this.chunkPos, this.getWaitTimeMs());
   }

   private int calculateDistancePriority(Player player, ChunkPos targetChunkPos) {
      if (player != null && targetChunkPos != null) {
         Location location = player.getLocation();
         int chunkCenterX = (ChunkCoordinateCodec.getChunkX(targetChunkPos) << 4) + 8;
         int chunkCenterZ = (ChunkCoordinateCodec.getChunkZ(targetChunkPos) << 4) + 8;
         int deltaX = Math.abs(location.getBlockX() - chunkCenterX);
         int deltaZ = Math.abs(location.getBlockZ() - chunkCenterZ);
         return deltaX + deltaZ;
      } else {
         return Integer.MAX_VALUE;
      }
   }

   public int comparePriority(PrioritizedChunkTask other) {
      int priorityComparison = Integer.compare(this.priority, other.priority);
      return priorityComparison != 0 ? priorityComparison : Long.compare(this.createdTimeNanos, other.createdTimeNanos);
   }

   public long getWaitTimeMs() {
      return (System.nanoTime() - this.createdTimeNanos) / 1000000L;
   }

   public PrioritizedChunkTask(Runnable task, Player player, ChunkPos chunkPos) {
      this.task = task;
      this.chunkPos = chunkPos;
      this.createdTimeNanos = System.nanoTime();
      this.priority = this.calculateDistancePriority(player, chunkPos);
   }

   public int compareTo(PrioritizedChunkTask other) {
      return this.comparePriority(other);
   }
}
