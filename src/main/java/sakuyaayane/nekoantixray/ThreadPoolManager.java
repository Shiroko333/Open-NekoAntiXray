package sakuyaayane.nekoantixray;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.logging.Logger;

public class ThreadPoolManager {
   private static final Logger logger = Logger.getLogger("NekoAntiXray");

   public static ThreadPoolExecutor createDynamicPool(int corePoolSize, int maxPoolSize, int queueCapacity, String threadNameFormat, boolean usePriorityQueue) {
      int clampedCoreSize = Math.max(1, Math.min(32, corePoolSize));
      int clampedMaxSize = Math.max(clampedCoreSize, Math.min(64, maxPoolSize));
      int clampedQueueCapacity = Math.max(0, Math.min(2048, queueCapacity));
      ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(threadNameFormat).setDaemon(true).setPriority(4).setUncaughtExceptionHandler((thread, uncaughtEx) -> {
         logger.severe("线程 " + thread.getName() + " 发生未捕获异常: " + uncaughtEx.getMessage());
         uncaughtEx.printStackTrace();
      }).build();
      BlockingQueue<Runnable> blockingQueue;
      if (clampedQueueCapacity == 0) {
         blockingQueue = new SynchronousQueue<>();
      } else if (usePriorityQueue) {
         blockingQueue = new PrioritizedTaskQueue<>(
            clampedQueueCapacity,
            (firstTask, secondTask) -> firstTask instanceof PrioritizedChunkTask && secondTask instanceof PrioritizedChunkTask
               ? ((PrioritizedChunkTask)firstTask).comparePriority((PrioritizedChunkTask)secondTask)
               : 0
         );
      } else {
         blockingQueue = new LinkedBlockingQueue<>(clampedQueueCapacity);
      }

      CallerRunsPolicy callerRunsPolicy = new CallerRunsPolicy() {
         private static final String REJECTED_TASK_WARNING_FORMAT = "线程池队列已满，任务被拒绝: %s (队列大小: %d, 活跃线程: %d)";

         @Override
         public void rejectedExecution(Runnable task, ThreadPoolExecutor threadPoolExecutor) {
            if (!threadPoolExecutor.isShutdown()) {
               if (task instanceof PrioritizedChunkTask prioritizedChunkTask) {
                  ThreadPoolManager.logger
                     .warning(String.format("线程池队列已满，任务被拒绝: %s (队列大小: %d, 活跃线程: %d)", prioritizedChunkTask, threadPoolExecutor.getQueue().size(), threadPoolExecutor.getActiveCount()));
               }

               super.rejectedExecution(task, threadPoolExecutor);
            }
         }
      };
      ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(clampedCoreSize, clampedMaxSize, 60L, TimeUnit.SECONDS, blockingQueue, threadFactory, callerRunsPolicy);
      threadPoolExecutor.allowCoreThreadTimeOut(true);
      logger.info(String.format("创建动态线程池: 核心=%d, 最大=%d, 队列=%d, 优先级=%s", clampedCoreSize, clampedMaxSize, clampedQueueCapacity, usePriorityQueue));
      return threadPoolExecutor;
   }

   public static String formatPoolStatus(ThreadPoolExecutor threadPoolExecutor) {
      return threadPoolExecutor == null
         ? "线程池未初始化"
         : String.format(
            "线程池状态: 活跃=%d/%d, 队列=%d/%d, 已完成=%d, 拒绝=%d",
            threadPoolExecutor.getActiveCount(),
            threadPoolExecutor.getPoolSize(),
            threadPoolExecutor.getQueue().size(),
            threadPoolExecutor.getQueue().remainingCapacity() + threadPoolExecutor.getQueue().size(),
            threadPoolExecutor.getCompletedTaskCount(),
            threadPoolExecutor.getTaskCount() - threadPoolExecutor.getCompletedTaskCount() - threadPoolExecutor.getQueue().size()
         );
   }

   public static void shutdownPool(ThreadPoolExecutor threadPoolExecutor, long timeoutSeconds) {
      if (threadPoolExecutor != null && !threadPoolExecutor.isShutdown()) {
         logger.info("开始关闭线程池...");
         threadPoolExecutor.shutdown();

         try {
            if (!threadPoolExecutor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
               logger.warning("线程池未能在 " + timeoutSeconds + " 秒内关闭，强制关闭");
               threadPoolExecutor.shutdownNow();
               if (!threadPoolExecutor.awaitTermination(5L, TimeUnit.SECONDS)) {
                  logger.severe("线程池强制关闭失败");
               }
            } else {
               logger.info("线程池已成功关闭");
            }
         } catch (InterruptedException interruptedEx) {
            logger.warning("线程池关闭被中断");
            threadPoolExecutor.shutdownNow();
            Thread.currentThread().interrupt();
         }
      }
   }

   public static ThreadPoolExecutor createDefaultDynamicPool(boolean usePriorityQueue) {
      int availableProcessors = CpuInformation.getAvailableProcessors();
      int maxPoolSize = availableProcessors * 2;
      short defaultQueueCapacity = 512;
      return createDynamicPool(availableProcessors, maxPoolSize, defaultQueueCapacity, "NekoAntiXray-Dynamic-%d", usePriorityQueue);
   }
}
