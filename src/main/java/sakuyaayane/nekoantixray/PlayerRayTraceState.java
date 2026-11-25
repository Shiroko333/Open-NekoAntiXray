package sakuyaayane.nekoantixray;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Generated;

public final class PlayerRayTraceState implements Callable<Object> {
   private volatile PlayerViewSnapshot[] viewSnapshots;
   private final ConcurrentMap<ChunkKey, TrackedChunkVisibility> chunks = new ConcurrentHashMap<>();
   private final Queue<BlockVisibilityUpdate> pendingUpdates = new ConcurrentLinkedQueue<>();
   private final AtomicInteger pendingUpdateCount = new AtomicInteger(0);
   private final AtomicBoolean running = new AtomicBoolean(false);
   private Callable<?> callable;

   public PlayerRayTraceState(PlayerViewSnapshot[] playerViewSnapshots) {
      this.viewSnapshots = playerViewSnapshots;
   }

   @Override
   public Object call() throws Exception {
      return this.callable != null ? this.callable.call() : null;
   }

   public boolean tryAcquire() {
      return this.running.compareAndSet(false, true);
   }

   public void release() {
      this.running.set(false);
   }

   public int getPendingUpdateCount() {
      return this.pendingUpdateCount.get();
   }

   @Generated
   public PlayerViewSnapshot[] getViewSnapshots() {
      return this.viewSnapshots;
   }

   @Generated
   public ConcurrentMap getChunks() {
      return this.chunks;
   }

   @Generated
   public Queue getPendingUpdates() {
      return this.pendingUpdates;
   }

   @Generated
   public Callable getCallable() {
      return this.callable;
   }

   public void enqueueUpdate(BlockVisibilityUpdate blockVisibilityUpdate) {
      this.pendingUpdates.add(blockVisibilityUpdate);
      this.pendingUpdateCount.incrementAndGet();
   }

   public BlockVisibilityUpdate pollUpdate() {
      BlockVisibilityUpdate blockVisibilityUpdate = this.pendingUpdates.poll();
      if (blockVisibilityUpdate != null) {
         this.pendingUpdateCount.decrementAndGet();
      }

      return blockVisibilityUpdate;
   }

   public void setViewSnapshots(PlayerViewSnapshot[] playerViewSnapshots) {
      this.viewSnapshots = playerViewSnapshots;
   }

   public void setCallable(Callable callable) {
      this.callable = callable;
   }
}
