package sakuyaayane.nekoantixray;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.util.logging.Logger;

public class CpuInformation {
   private static final Logger logger = Logger.getLogger("NekoAntiXray");

   public static int getAvailableProcessors() {
      return Runtime.getRuntime().availableProcessors();
   }

   public static String getCpuArchitectureInfo() {
      try {
         OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
         String arch = operatingSystemMXBean.getArch();
         String name = operatingSystemMXBean.getName();
         String property = System.getProperty("os.arch", "Unknown");
         return String.format("%s (%s on %s)", property, arch, name);
      } catch (Exception ex) {
         return System.getProperty("os.arch", "Unknown");
      }
   }

   public static int getRecommendedThreadCount() {
      int availableProcessors = getAvailableProcessors();
      int recommendedThreads;
      if (availableProcessors <= 2) {
         recommendedThreads = 2;
      } else if (availableProcessors <= 4) {
         recommendedThreads = (int)Math.ceil(availableProcessors * 1.5);
      } else if (availableProcessors <= 8) {
         recommendedThreads = (int)Math.ceil(availableProcessors * 1.5);
      } else {
         recommendedThreads = (int)Math.ceil(availableProcessors * 1.25);
      }

      return Math.max(2, Math.min(32, recommendedThreads));
   }

   public static String getCpuLoadSummary() {
      try {
         OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
         double systemCpuLoadPercent = operatingSystemMXBean.getSystemCpuLoad() * 100.0;
         double processCpuLoadPercent = operatingSystemMXBean.getProcessCpuLoad() * 100.0;
         return String.format("System: %.1f%%, Process: %.1f%%", systemCpuLoadPercent, processCpuLoadPercent);
      } catch (Exception ex) {
         return "N/A";
      }
   }

   private static String formatThreadFormula(int processorCount) {
      if (processorCount <= 2) {
         return "固定2线程（低端CPU保护）";
      } else {
         return processorCount <= 8 ? processorCount + " 核心 × 1.5 = " + getRecommendedThreadCount() + " 线程" : processorCount + " 核心 × 1.25 = " + getRecommendedThreadCount() + " 线程";
      }
   }

   public static String validateThreadCount(int threadCount) {
      int availableProcessors = getAvailableProcessors();
      int recommendedThreadCount = getRecommendedThreadCount();
      if (threadCount < 1) {
         return "错误: 线程数不能小于1，已自动调整为1";
      } else if (threadCount > availableProcessors * 3) {
         return String.format("警告: 线程数(%d)过高，可能导致上下文切换开销。推荐值: %d", threadCount, recommendedThreadCount);
      } else {
         return threadCount < availableProcessors / 2 ? String.format("提示: 线程数(%d)较低，可能无法充分利用CPU。推荐值: %d", threadCount, recommendedThreadCount) : String.format("配置合理（推荐值: %d）", recommendedThreadCount);
      }
   }

   public static void logCpuInfo() {
      int availableProcessors = getAvailableProcessors();
      String cpuArchitectureInfo = getCpuArchitectureInfo();
      int recommendedThreadCount = getRecommendedThreadCount();
      logger.info("========================================");
      logger.info("CPU检测信息:");
      logger.info("  型号: " + cpuArchitectureInfo);
      logger.info("  核心数: " + availableProcessors + " 核心");
      logger.info("  推荐线程数: " + recommendedThreadCount + " 线程");
      logger.info("  计算公式: " + formatThreadFormula(availableProcessors));
      logger.info("========================================");
   }
}
