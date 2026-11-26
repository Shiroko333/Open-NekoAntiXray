package sakuyaayane.nekoantixray.stats;

import java.util.concurrent.atomic.AtomicLong;

public class ObfuscationStatistics {
   private final AtomicLong attemptCount = new AtomicLong();
   private final AtomicLong successCount = new AtomicLong();
   private final AtomicLong skippedCount = new AtomicLong();
   private final AtomicLong failureCount = new AtomicLong();
   private final AtomicLong replacedBlockCount = new AtomicLong();
   private final AtomicLong totalDurationNanos = new AtomicLong();
   private final AtomicLong lastDurationNanos = new AtomicLong();
   private static final long NANOS_PER_MICROSECOND = 1000L;

   public void recordAttempt() {
      this.attemptCount.incrementAndGet();
   }

   public void recordSkipped() {
      this.skippedCount.incrementAndGet();
   }

   public void recordFailure() {
      this.failureCount.incrementAndGet();
   }

   public long getAttemptCount() {
      return this.attemptCount.get();
   }

   public long getSuccessCount() {
      return this.successCount.get();
   }

   public long getSkippedCount() {
      return this.skippedCount.get();
   }

   public long getFailureCount() {
      return this.failureCount.get();
   }

   public long getReplacedBlockCount() {
      return this.replacedBlockCount.get();
   }

   public long getTotalDurationNanos() {
      return this.totalDurationNanos.get();
   }

   public long getLastDurationNanos() {
      return this.lastDurationNanos.get();
   }

   public void reset() {
      this.attemptCount.set(0L);
      this.successCount.set(0L);
      this.skippedCount.set(0L);
      this.failureCount.set(0L);
      this.replacedBlockCount.set(0L);
      this.totalDurationNanos.set(0L);
      this.lastDurationNanos.set(0L);
   }

   @Override
   public String toString() {
      long attempts = this.attemptCount.get();
      long successes = this.successCount.get();
      long skipped = this.skippedCount.get();
      long failures = this.failureCount.get();
      long replacedBlocks = this.replacedBlockCount.get();
      long averageDurationMicros = this.getAverageDurationMicros();
      return "尝试=" + attempts + " 成功=" + successes + " 跳过=" + skipped + " 失败=" + failures + " 替换方块=" + replacedBlocks + " 平均耗时=" + averageDurationMicros + "µs";
   }

   public void recordSuccess(long replacedBlocks, long durationNanos) {
      this.successCount.incrementAndGet();
      this.replacedBlockCount.addAndGet(replacedBlocks);
      this.totalDurationNanos.addAndGet(durationNanos);
      this.lastDurationNanos.set(durationNanos);
   }

   public long getAverageDurationMicros() {
      long successes = this.successCount.get();
      return successes == 0L ? 0L : this.totalDurationNanos.get() / successes / 1000L;
   }
}
