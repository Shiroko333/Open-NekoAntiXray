package sakuyaayane.nekoantixray;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ModuleTimingStatistics {
   private static final Map<String, AtomicLong[]> DATA = new ConcurrentHashMap<>();

   private ModuleTimingStatistics() {
   }

   public static void reset() {
      DATA.clear();
   }

   public static void record(String moduleName, long elapsedNanos) {
      AtomicLong[] counters = DATA.computeIfAbsent(moduleName, ignored -> new AtomicLong[]{new AtomicLong(), new AtomicLong()});
      counters[0].incrementAndGet();
      counters[1].addAndGet(elapsedNanos);
   }

   public static long getSampleCount(String moduleName) {
      AtomicLong[] counters = DATA.get(moduleName);
      return counters == null ? 0L : counters[0].get();
   }

   public static long getTotalNanos(String moduleName) {
      AtomicLong[] counters = DATA.get(moduleName);
      return counters == null ? 0L : counters[1].get();
   }

   public static String formatAll() {
      StringBuilder output = new StringBuilder();
      DATA.entrySet()
         .stream()
         .sorted((first, second) -> Long.compare(second.getValue()[1].get(), first.getValue()[1].get()))
         .forEach(
            entry -> {
               long sampleCount = entry.getValue()[0].get();
               long totalNanos = entry.getValue()[1].get();
               if (sampleCount != 0L) {
                  double totalMillis = totalNanos / 1000000.0;
                  double avgMillis = totalMillis / sampleCount;
                  output.append("§e")
                     .append(entry.getKey())
                     .append(": §f")
                     .append(sampleCount)
                     .append(" 次, 累计 ")
                     .append(String.format("%.1f", totalMillis))
                     .append("ms, 平均 ")
                     .append(String.format("%.3f", avgMillis))
                     .append("ms/次\n");
               }
            }
         );
      return output.length() == 0 ? "§7暂无采样数据（需要先产生区块/追踪流量）" : output.toString();
   }
}
