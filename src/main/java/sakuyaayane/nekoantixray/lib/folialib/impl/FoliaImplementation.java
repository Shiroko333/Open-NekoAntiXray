package sakuyaayane.nekoantixray.lib.folialib.impl;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.folialib.FoliaLib;
import sakuyaayane.nekoantixray.lib.folialib.enums.EntityTaskResult;
import sakuyaayane.nekoantixray.lib.folialib.util.InvalidTickDelayNotifier;
import sakuyaayane.nekoantixray.lib.folialib.util.TimeConverter;
import sakuyaayane.nekoantixray.lib.folialib.wrapper.task.WrappedFoliaTask;
import sakuyaayane.nekoantixray.lib.folialib.wrapper.task.WrappedTask;

public class FoliaImplementation implements PlatformScheduler {
   private final Plugin plugin;
   private final GlobalRegionScheduler globalRegionScheduler;
   private final RegionScheduler regionScheduler;
   private final AsyncScheduler asyncScheduler;

   public FoliaImplementation(FoliaLib var1) {
      this.plugin = var1.getPlugin();
      this.globalRegionScheduler = this.plugin.getServer().getGlobalRegionScheduler();
      this.regionScheduler = this.plugin.getServer().getRegionScheduler();
      this.asyncScheduler = this.plugin.getServer().getAsyncScheduler();
   }

   @Override
   public boolean isOwnedByCurrentRegion(@NotNull Location var1) {
      return this.plugin.getServer().isOwnedByCurrentRegion(var1);
   }

   @Override
   public boolean isOwnedByCurrentRegion(@NotNull Location var1, int var2) {
      return this.plugin.getServer().isOwnedByCurrentRegion(var1, var2);
   }

   @Override
   public boolean isOwnedByCurrentRegion(@NotNull Block var1) {
      return this.plugin.getServer().isOwnedByCurrentRegion(var1);
   }

   @Override
   public boolean isOwnedByCurrentRegion(@NotNull World var1, int var2, int var3) {
      return this.plugin.getServer().isOwnedByCurrentRegion(var1, var2, var3);
   }

   @Override
   public boolean isOwnedByCurrentRegion(@NotNull World var1, int var2, int var3, int var4) {
      return this.plugin.getServer().isOwnedByCurrentRegion(var1, var2, var3, var4);
   }

   @Override
   public boolean isOwnedByCurrentRegion(@NotNull Entity var1) {
      return this.plugin.getServer().isOwnedByCurrentRegion(var1);
   }

   @Override
   public boolean isGlobalTickThread() {
      return this.plugin.getServer().isGlobalTickThread();
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runNextTick(@NotNull Consumer<WrappedTask> var1) {
      CompletableFuture var2 = new CompletableFuture();
      this.globalRegionScheduler.run(this.plugin, var3 -> this.restoredLambda$runNextTick$0(var1, var2, var3));
      return var2;
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runAsync(@NotNull Consumer<WrappedTask> var1) {
      CompletableFuture var2 = new CompletableFuture();
      this.asyncScheduler.runNow(this.plugin, var3 -> this.restoredLambda$runAsync$1(var1, var2, var3));
      return var2;
   }

   @Override
   public WrappedTask runLater(@NotNull Runnable var1, long var2) {
      if (var2 <= 0L) {
         InvalidTickDelayNotifier.notifyOnce(this.plugin.getLogger(), var2);
         var2 = 1L;
      }

      return this.wrapTask(this.globalRegionScheduler.runDelayed(this.plugin, var1x -> restoredLambda$runLater$2(var1, var1x), var2));
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runLater(@NotNull Consumer<WrappedTask> var1, long var2) {
      CompletableFuture var4 = new CompletableFuture();
      if (var2 <= 0L) {
         InvalidTickDelayNotifier.notifyOnce(this.plugin.getLogger(), var2);
         var2 = 1L;
      }

      this.globalRegionScheduler.runDelayed(this.plugin, var3 -> this.restoredLambda$runLater$3(var1, var4, var3), var2);
      return var4;
   }

   @Override
   public WrappedTask runLater(@NotNull Runnable var1, long var2, TimeUnit var4) {
      return this.runLater(var1, TimeConverter.toTicks(var2, var4));
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runLater(@NotNull Consumer<WrappedTask> var1, long var2, TimeUnit var4) {
      return this.runLater(var1, TimeConverter.toTicks(var2, var4));
   }

   @Override
   public WrappedTask runLaterAsync(@NotNull Runnable var1, long var2) {
      return this.runLaterAsync(var1, TimeConverter.toMillis(var2), TimeUnit.MILLISECONDS);
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runLaterAsync(@NotNull Consumer<WrappedTask> var1, long var2) {
      return this.runLaterAsync(var1, TimeConverter.toMillis(var2), TimeUnit.MILLISECONDS);
   }

   @Override
   public WrappedTask runLaterAsync(@NotNull Runnable var1, long var2, TimeUnit var4) {
      return this.wrapTask(this.asyncScheduler.runDelayed(this.plugin, var1x -> restoredLambda$runLaterAsync$4(var1, var1x), var2, var4));
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runLaterAsync(@NotNull Consumer<WrappedTask> var1, long var2, TimeUnit var4) {
      CompletableFuture var5 = new CompletableFuture();
      this.asyncScheduler.runDelayed(this.plugin, var3 -> this.restoredLambda$runLaterAsync$5(var1, var5, var3), var2, var4);
      return var5;
   }

   @Override
   public WrappedTask runTimer(@NotNull Runnable var1, long var2, long var4) {
      if (var2 <= 0L) {
         InvalidTickDelayNotifier.notifyOnce(this.plugin.getLogger(), var2);
         var2 = 1L;
      }

      if (var4 <= 0L) {
         InvalidTickDelayNotifier.notifyOnce(this.plugin.getLogger(), var4);
         var4 = 1L;
      }

      return this.wrapTask(this.globalRegionScheduler.runAtFixedRate(this.plugin, var1x -> restoredLambda$runTimer$6(var1, var1x), var2, var4));
   }

   @Override
   public void runTimer(@NotNull Consumer<WrappedTask> var1, long var2, long var4) {
      if (var2 <= 0L) {
         InvalidTickDelayNotifier.notifyOnce(this.plugin.getLogger(), var2);
         var2 = 1L;
      }

      if (var4 <= 0L) {
         InvalidTickDelayNotifier.notifyOnce(this.plugin.getLogger(), var4);
         var4 = 1L;
      }

      this.globalRegionScheduler.runAtFixedRate(this.plugin, var2x -> this.restoredLambda$runTimer$7(var1, var2x), var2, var4);
   }

   @Override
   public WrappedTask runTimer(@NotNull Runnable var1, long var2, long var4, TimeUnit var6) {
      return this.runTimer(var1, TimeConverter.toTicks(var2, var6), TimeConverter.toTicks(var4, var6));
   }

   @Override
   public void runTimer(@NotNull Consumer<WrappedTask> var1, long var2, long var4, TimeUnit var6) {
      this.runTimer(var1, TimeConverter.toTicks(var2, var6), TimeConverter.toTicks(var4, var6));
   }

   @Override
   public WrappedTask runTimerAsync(@NotNull Runnable var1, long var2, long var4) {
      return this.runTimerAsync(var1, TimeConverter.toMillis(var2), TimeConverter.toMillis(var4), TimeUnit.MILLISECONDS);
   }

   @Override
   public void runTimerAsync(@NotNull Consumer<WrappedTask> var1, long var2, long var4) {
      this.runTimerAsync(var1, TimeConverter.toMillis(var2), TimeConverter.toMillis(var4), TimeUnit.MILLISECONDS);
   }

   @Override
   public WrappedTask runTimerAsync(@NotNull Runnable var1, long var2, long var4, TimeUnit var6) {
      return this.wrapTask(this.asyncScheduler.runAtFixedRate(this.plugin, var1x -> restoredLambda$runTimerAsync$8(var1, var1x), var2, var4, var6));
   }

   @Override
   public void runTimerAsync(@NotNull Consumer<WrappedTask> var1, long var2, long var4, TimeUnit var6) {
      this.asyncScheduler.runAtFixedRate(this.plugin, var2x -> this.restoredLambda$runTimerAsync$9(var1, var2x), var2, var4, var6);
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runAtLocation(Location var1, @NotNull Consumer<WrappedTask> var2) {
      CompletableFuture var3 = new CompletableFuture();
      this.regionScheduler.run(this.plugin, var1, var3x -> this.restoredLambda$runAtLocation$10(var2, var3, var3x));
      return var3;
   }

   @Override
   public WrappedTask runAtLocationLater(Location var1, @NotNull Runnable var2, long var3) {
      if (var3 <= 0L) {
         InvalidTickDelayNotifier.notifyOnce(this.plugin.getLogger(), var3);
         var3 = 1L;
      }

      return this.wrapTask(this.regionScheduler.runDelayed(this.plugin, var1, var1x -> restoredLambda$runAtLocationLater$11(var2, var1x), var3));
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runAtLocationLater(Location var1, @NotNull Consumer<WrappedTask> var2, long var3) {
      CompletableFuture var5 = new CompletableFuture();
      if (var3 <= 0L) {
         InvalidTickDelayNotifier.notifyOnce(this.plugin.getLogger(), var3);
         var3 = 1L;
      }

      this.regionScheduler.runDelayed(this.plugin, var1, var3x -> this.restoredLambda$runAtLocationLater$12(var2, var5, var3x), var3);
      return var5;
   }

   @Override
   public WrappedTask runAtLocationLater(Location var1, @NotNull Runnable var2, long var3, TimeUnit var5) {
      return this.runAtLocationLater(var1, var2, TimeConverter.toTicks(var3, var5));
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runAtLocationLater(Location var1, @NotNull Consumer<WrappedTask> var2, long var3, TimeUnit var5) {
      return this.runAtLocationLater(var1, var2, TimeConverter.toTicks(var3, var5));
   }

   @Override
   public WrappedTask runAtLocationTimer(Location var1, @NotNull Runnable var2, long var3, long var5) {
      if (var3 <= 0L) {
         InvalidTickDelayNotifier.notifyOnce(this.plugin.getLogger(), var3);
         var3 = 1L;
      }

      if (var5 <= 0L) {
         InvalidTickDelayNotifier.notifyOnce(this.plugin.getLogger(), var5);
         var5 = 1L;
      }

      return this.wrapTask(this.regionScheduler.runAtFixedRate(this.plugin, var1, var1x -> restoredLambda$runAtLocationTimer$13(var2, var1x), var3, var5));
   }

   @Override
   public void runAtLocationTimer(Location var1, @NotNull Consumer<WrappedTask> var2, long var3, long var5) {
      if (var3 <= 0L) {
         InvalidTickDelayNotifier.notifyOnce(this.plugin.getLogger(), var3);
         var3 = 1L;
      }

      if (var5 <= 0L) {
         InvalidTickDelayNotifier.notifyOnce(this.plugin.getLogger(), var5);
         var5 = 1L;
      }

      this.regionScheduler.runAtFixedRate(this.plugin, var1, var2x -> this.restoredLambda$runAtLocationTimer$14(var2, var2x), var3, var5);
   }

   @Override
   public WrappedTask runAtLocationTimer(Location var1, @NotNull Runnable var2, long var3, long var5, TimeUnit var7) {
      return this.runAtLocationTimer(var1, var2, TimeConverter.toTicks(var3, var7), TimeConverter.toTicks(var5, var7));
   }

   @Override
   public void runAtLocationTimer(Location var1, @NotNull Consumer<WrappedTask> var2, long var3, long var5, TimeUnit var7) {
      this.runAtLocationTimer(var1, var2, TimeConverter.toTicks(var3, var7), TimeConverter.toTicks(var5, var7));
   }

   @NotNull
   @Override
   public CompletableFuture<EntityTaskResult> runAtEntity(Entity var1, @NotNull Consumer<WrappedTask> var2) {
      CompletableFuture var3 = new CompletableFuture();
      ScheduledTask var4 = var1.getScheduler().run(this.plugin, var3x -> this.restoredLambda$runAtEntity$15(var2, var3, var3x), null);
      if (var4 == null) {
         var3.complete(EntityTaskResult.SCHEDULER_RETIRED);
      }

      return var3;
   }

   @NotNull
   @Override
   public CompletableFuture<EntityTaskResult> runAtEntityWithFallback(Entity var1, @NotNull Consumer<WrappedTask> var2, Runnable var3) {
      CompletableFuture var4 = new CompletableFuture();
      ScheduledTask var5 = var1.getScheduler()
         .run(this.plugin, var3x -> this.restoredLambda$runAtEntityWithFallback$16(var2, var4, var3x), () -> restoredLambda$runAtEntityWithFallback$17(var3, var4));
      if (var5 == null) {
         var4.complete(EntityTaskResult.SCHEDULER_RETIRED);
      }

      return var4;
   }

   @Override
   public WrappedTask runAtEntityLater(Entity var1, @NotNull Runnable var2, long var3) {
      return this.runAtEntityLater(var1, var2, null, var3);
   }

   @Override
   public WrappedTask runAtEntityLater(Entity var1, @NotNull Runnable var2, Runnable var3, long var4) {
      if (var4 <= 0L) {
         InvalidTickDelayNotifier.notifyOnce(this.plugin.getLogger(), var4);
         var4 = 1L;
      }

      return this.wrapTask(var1.getScheduler().runDelayed(this.plugin, var1x -> restoredLambda$runAtEntityLater$18(var2, var1x), var3, var4));
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runAtEntityLater(Entity var1, @NotNull Consumer<WrappedTask> var2, long var3) {
      return this.runAtEntityLater(var1, var2, null, var3);
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runAtEntityLater(Entity var1, @NotNull Consumer<WrappedTask> var2, Runnable var3, long var4) {
      CompletableFuture var6 = new CompletableFuture();
      if (var3 != null) {
         Runnable var7 = var3;
         var3 = () -> restoredLambda$runAtEntityLater$19(var7, var6);
      }

      if (var4 <= 0L) {
         InvalidTickDelayNotifier.notifyOnce(this.plugin.getLogger(), var4);
         var4 = 1L;
      }

      var1.getScheduler().runDelayed(this.plugin, var3x -> this.restoredLambda$runAtEntityLater$20(var2, var6, var3x), var3, var4);
      return var6;
   }

   @Override
   public WrappedTask runAtEntityLater(Entity var1, @NotNull Runnable var2, long var3, TimeUnit var5) {
      return this.runAtEntityLater(var1, var2, TimeConverter.toTicks(var3, var5));
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runAtEntityLater(Entity var1, @NotNull Consumer<WrappedTask> var2, long var3, TimeUnit var5) {
      return this.runAtEntityLater(var1, var2, TimeConverter.toTicks(var3, var5));
   }

   @Override
   public WrappedTask runAtEntityTimer(Entity var1, @NotNull Runnable var2, long var3, long var5) {
      return this.runAtEntityTimer(var1, var2, null, var3, var5);
   }

   @Override
   public WrappedTask runAtEntityTimer(Entity var1, @NotNull Runnable var2, Runnable var3, long var4, long var6) {
      if (var4 <= 0L) {
         InvalidTickDelayNotifier.notifyOnce(this.plugin.getLogger(), var4);
         var4 = 1L;
      }

      if (var6 <= 0L) {
         InvalidTickDelayNotifier.notifyOnce(this.plugin.getLogger(), var6);
         var6 = 1L;
      }

      return this.wrapTask(var1.getScheduler().runAtFixedRate(this.plugin, var1x -> restoredLambda$runAtEntityTimer$21(var2, var1x), var3, var4, var6));
   }

   @Override
   public void runAtEntityTimer(Entity var1, @NotNull Consumer<WrappedTask> var2, long var3, long var5) {
      this.runAtEntityTimer(var1, var2, null, var3, var5);
   }

   @Override
   public void runAtEntityTimer(Entity var1, @NotNull Consumer<WrappedTask> var2, Runnable var3, long var4, long var6) {
      if (var4 <= 0L) {
         InvalidTickDelayNotifier.notifyOnce(this.plugin.getLogger(), var4);
         var4 = 1L;
      }

      if (var6 <= 0L) {
         InvalidTickDelayNotifier.notifyOnce(this.plugin.getLogger(), var6);
         var6 = 1L;
      }

      var1.getScheduler().runAtFixedRate(this.plugin, var2x -> this.restoredLambda$runAtEntityTimer$22(var2, var2x), var3, var4, var6);
   }

   @Override
   public WrappedTask runAtEntityTimer(Entity var1, @NotNull Runnable var2, long var3, long var5, TimeUnit var7) {
      return this.runAtEntityTimer(var1, var2, TimeConverter.toTicks(var3, var7), TimeConverter.toTicks(var5, var7));
   }

   @Override
   public void runAtEntityTimer(Entity var1, @NotNull Consumer<WrappedTask> var2, long var3, long var5, TimeUnit var7) {
      this.runAtEntityTimer(var1, var2, TimeConverter.toTicks(var3, var7), TimeConverter.toTicks(var5, var7));
   }

   @Override
   public void cancelTask(WrappedTask var1) {
      var1.cancel();
   }

   @Override
   public void cancelAllTasks() {
      this.globalRegionScheduler.cancelTasks(this.plugin);
      this.asyncScheduler.cancelTasks(this.plugin);
   }

   @Override
   public List<WrappedTask> getAllTasks() {
      try {
         return this.getAllScheduledTasks()
            .stream()
            .filter(var1 -> this.restoredLambda$getAllTasks$23(var1))
            .map(var1 -> this.wrapTask(var1))
            .collect(Collectors.toList());
      } catch (Exception var2) {
         var2.printStackTrace();
         return null;
      }
   }

   @Override
   public List<WrappedTask> getAllServerTasks() {
      try {
         return this.getAllScheduledTasks().stream().map(var1 -> this.wrapTask(var1)).collect(Collectors.toList());
      } catch (Exception var2) {
         var2.printStackTrace();
         return null;
      }
   }

   @NotNull
   private List<ScheduledTask> getAllScheduledTasks() throws NoSuchFieldException, IllegalAccessException {
      Class var1 = this.globalRegionScheduler.getClass();
      Field var2 = var1.getDeclaredField("tasksByDeadline");
      boolean var3 = var2.isAccessible();
      var2.setAccessible(true);
      Long2ObjectOpenHashMap var4 = (Long2ObjectOpenHashMap)var2.get(this.globalRegionScheduler);
      var2.setAccessible(var3);
      Class var5 = this.asyncScheduler.getClass();
      Field var6 = var5.getDeclaredField("tasks");
      var3 = var6.isAccessible();
      var6.setAccessible(true);
      Set var7 = (Set)var6.get(this.asyncScheduler);
      var6.setAccessible(var3);
      ArrayList var8 = new ArrayList();

      for (List var10 : (java.lang.Iterable<java.util.List>)(java.lang.Iterable<?>)(var4.values())) {
         var8.addAll(var10);
      }

      ArrayList var12 = new ArrayList(var8.size() + var7.size());
      var12.addAll(var8);
      var12.addAll(var7);
      return var12;
   }

   @Override
   public Player getPlayer(String var1) {
      return this.plugin.getServer().getPlayer(var1);
   }

   @Override
   public Player getPlayerExact(String var1) {
      return this.plugin.getServer().getPlayerExact(var1);
   }

   @Override
   public Player getPlayer(UUID var1) {
      return this.plugin.getServer().getPlayer(var1);
   }

   @Override
   public CompletableFuture<Boolean> teleportAsync(Entity var1, Location var2) {
      return var1.teleportAsync(var2);
   }

   @Override
   public CompletableFuture<Boolean> teleportAsync(Entity var1, Location var2, TeleportCause var3) {
      return var1.teleportAsync(var2, var3);
   }

   @Override
   public WrappedTask wrapTask(@NotNull Object var1) {
      Objects.requireNonNull(var1, "nativeTask");
      if (!(var1 instanceof ScheduledTask)) {
         throw new IllegalArgumentException("The nativeTask provided must be a ScheduledTask. Got: " + var1.getClass().getName() + " instead.");
      } else {
         return new WrappedFoliaTask((ScheduledTask)var1);
      }
   }

   // $VF: synthetic method
   private boolean restoredLambda$getAllTasks$23(ScheduledTask var1) {
      return var1.getOwningPlugin().equals(this.plugin);
   }

   // $VF: synthetic method
   private void restoredLambda$runAtEntityTimer$22(Consumer var1, ScheduledTask var2) {
      var1.accept(this.wrapTask(var2));
   }

   // $VF: synthetic method
   private static void restoredLambda$runAtEntityTimer$21(Runnable var0, ScheduledTask var1) {
      var0.run();
   }

   // $VF: synthetic method
   private void restoredLambda$runAtEntityLater$20(Consumer var1, CompletableFuture var2, ScheduledTask var3) {
      var1.accept(this.wrapTask(var3));
      var2.complete(null);
   }

   // $VF: synthetic method
   private static void restoredLambda$runAtEntityLater$19(Runnable var0, CompletableFuture var1) {
      var0.run();
      var1.complete(null);
   }

   // $VF: synthetic method
   private static void restoredLambda$runAtEntityLater$18(Runnable var0, ScheduledTask var1) {
      var0.run();
   }

   // $VF: synthetic method
   private static void restoredLambda$runAtEntityWithFallback$17(Runnable var0, CompletableFuture var1) {
      var0.run();
      var1.complete(EntityTaskResult.ENTITY_RETIRED);
   }

   // $VF: synthetic method
   private void restoredLambda$runAtEntityWithFallback$16(Consumer var1, CompletableFuture var2, ScheduledTask var3) {
      var1.accept(this.wrapTask(var3));
      var2.complete(EntityTaskResult.SUCCESS);
   }

   // $VF: synthetic method
   private void restoredLambda$runAtEntity$15(Consumer var1, CompletableFuture var2, ScheduledTask var3) {
      var1.accept(this.wrapTask(var3));
      var2.complete(EntityTaskResult.SUCCESS);
   }

   // $VF: synthetic method
   private void restoredLambda$runAtLocationTimer$14(Consumer var1, ScheduledTask var2) {
      var1.accept(this.wrapTask(var2));
   }

   // $VF: synthetic method
   private static void restoredLambda$runAtLocationTimer$13(Runnable var0, ScheduledTask var1) {
      var0.run();
   }

   // $VF: synthetic method
   private void restoredLambda$runAtLocationLater$12(Consumer var1, CompletableFuture var2, ScheduledTask var3) {
      var1.accept(this.wrapTask(var3));
      var2.complete(null);
   }

   // $VF: synthetic method
   private static void restoredLambda$runAtLocationLater$11(Runnable var0, ScheduledTask var1) {
      var0.run();
   }

   // $VF: synthetic method
   private void restoredLambda$runAtLocation$10(Consumer var1, CompletableFuture var2, ScheduledTask var3) {
      var1.accept(this.wrapTask(var3));
      var2.complete(null);
   }

   // $VF: synthetic method
   private void restoredLambda$runTimerAsync$9(Consumer var1, ScheduledTask var2) {
      var1.accept(this.wrapTask(var2));
   }

   // $VF: synthetic method
   private static void restoredLambda$runTimerAsync$8(Runnable var0, ScheduledTask var1) {
      var0.run();
   }

   // $VF: synthetic method
   private void restoredLambda$runTimer$7(Consumer var1, ScheduledTask var2) {
      var1.accept(this.wrapTask(var2));
   }

   // $VF: synthetic method
   private static void restoredLambda$runTimer$6(Runnable var0, ScheduledTask var1) {
      var0.run();
   }

   // $VF: synthetic method
   private void restoredLambda$runLaterAsync$5(Consumer var1, CompletableFuture var2, ScheduledTask var3) {
      var1.accept(this.wrapTask(var3));
      var2.complete(null);
   }

   // $VF: synthetic method
   private static void restoredLambda$runLaterAsync$4(Runnable var0, ScheduledTask var1) {
      var0.run();
   }

   // $VF: synthetic method
   private void restoredLambda$runLater$3(Consumer var1, CompletableFuture var2, ScheduledTask var3) {
      var1.accept(this.wrapTask(var3));
      var2.complete(null);
   }

   // $VF: synthetic method
   private static void restoredLambda$runLater$2(Runnable var0, ScheduledTask var1) {
      var0.run();
   }

   // $VF: synthetic method
   private void restoredLambda$runAsync$1(Consumer var1, CompletableFuture var2, ScheduledTask var3) {
      var1.accept(this.wrapTask(var3));
      var2.complete(null);
   }

   // $VF: synthetic method
   private void restoredLambda$runNextTick$0(Consumer var1, CompletableFuture var2, ScheduledTask var3) {
      var1.accept(this.wrapTask(var3));
      var2.complete(null);
   }
}
