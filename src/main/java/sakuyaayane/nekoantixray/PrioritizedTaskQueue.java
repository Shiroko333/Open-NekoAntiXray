package sakuyaayane.nekoantixray;

import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class PrioritizedTaskQueue<E> extends PriorityBlockingQueue<E> {
   private final Semaphore capacitySemaphore;

   public PrioritizedTaskQueue(int initialCapacity, Comparator<? super E> comparator) {
      super(initialCapacity, comparator);
      this.capacitySemaphore = new Semaphore(initialCapacity);
   }

   @Override
   public boolean offer(E element) {
      if (!this.capacitySemaphore.tryAcquire()) {
         return false;
      }

      try {
         return super.offer(element);
      } finally {
         this.capacitySemaphore.release();
      }
   }

   @Override
   public boolean offer(E element, long timeout, TimeUnit timeUnit) {
      boolean acquired;
      try {
         acquired = this.capacitySemaphore.tryAcquire(timeout, timeUnit);
      } catch (InterruptedException ex) {
         Thread.currentThread().interrupt();
         return false;
      }

      if (!acquired) {
         return false;
      }

      try {
         return super.offer(element);
      } finally {
         this.capacitySemaphore.release();
      }
   }

   @Override
   public void put(E element) {
      this.capacitySemaphore.acquireUninterruptibly();

      try {
         super.put(element);
      } finally {
         this.capacitySemaphore.release();
      }
   }

   @Override
   public E poll() {
      E element = super.poll();
      if (element != null) {
         this.capacitySemaphore.release();
      }

      return element;
   }

   @Override
   public E poll(long timeout, TimeUnit timeUnit) throws InterruptedException {
      E element = super.poll(timeout, timeUnit);
      if (element != null) {
         this.capacitySemaphore.release();
      }

      return element;
   }

   @Override
   public E take() throws InterruptedException {
      E element = super.take();
      this.capacitySemaphore.release();
      return element;
   }

   @Override
   public boolean remove(Object target) {
      boolean removed = super.remove(target);
      if (removed) {
         this.capacitySemaphore.release();
      }

      return removed;
   }

   @Override
   public int drainTo(Collection<? super E> targetCollection) {
      int drainedCount = super.drainTo(targetCollection);
      if (drainedCount > 0) {
         this.capacitySemaphore.release(drainedCount);
      }

      return drainedCount;
   }

   @Override
   public int drainTo(Collection<? super E> targetCollection, int maxElements) {
      int drainedCount = super.drainTo(targetCollection, maxElements);
      if (drainedCount > 0) {
         this.capacitySemaphore.release(drainedCount);
      }

      return drainedCount;
   }

   @Override
   public int remainingCapacity() {
      return this.capacitySemaphore.availablePermits();
   }

   @Override
   public void clear() {
      while (this.poll() != null) {
      }
   }
}