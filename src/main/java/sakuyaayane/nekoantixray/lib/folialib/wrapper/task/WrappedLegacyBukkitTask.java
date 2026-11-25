package sakuyaayane.nekoantixray.lib.folialib.wrapper.task;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class WrappedLegacyBukkitTask implements WrappedTask {
   private final BukkitTask task;

   public WrappedLegacyBukkitTask(BukkitTask var1) {
      this.task = var1;
   }

   @Override
   public void cancel() {
      this.task.cancel();
   }

   @Override
   public boolean isCancelled() {
      int var1 = this.task.getTaskId();
      BukkitScheduler var2 = Bukkit.getScheduler();
      return !var2.isCurrentlyRunning(var1) && !var2.isQueued(var1);
   }

   @Override
   public Plugin getOwningPlugin() {
      return this.task.getOwner();
   }

   @Override
   public boolean isAsync() {
      return !this.task.isSync();
   }
}
