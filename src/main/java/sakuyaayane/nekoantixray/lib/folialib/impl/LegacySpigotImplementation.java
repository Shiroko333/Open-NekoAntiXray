package sakuyaayane.nekoantixray.lib.folialib.impl;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.folialib.FoliaLib;
import sakuyaayane.nekoantixray.lib.folialib.enums.EntityTaskResult;
import sakuyaayane.nekoantixray.lib.folialib.util.ImplementationTestsUtil;
import sakuyaayane.nekoantixray.lib.folialib.util.TimeConverter;
import sakuyaayane.nekoantixray.lib.folialib.wrapper.task.WrappedBukkitTask;
import sakuyaayane.nekoantixray.lib.folialib.wrapper.task.WrappedLegacyBukkitTask;
import sakuyaayane.nekoantixray.lib.folialib.wrapper.task.WrappedTask;

public class LegacySpigotImplementation implements PlatformScheduler {
   private final Plugin plugin;
   @NotNull
   private final BukkitScheduler scheduler;

   public LegacySpigotImplementation(FoliaLib var1) {
      this.plugin = var1.getPlugin();
      this.scheduler = this.plugin.getServer().getScheduler();
   }

   @Override
   public boolean isOwnedByCurrentRegion(@NotNull Location var1) {
      return this.plugin.getServer().isPrimaryThread();
   }

   @Override
   public boolean isOwnedByCurrentRegion(@NotNull Location var1, int var2) {
      return this.plugin.getServer().isPrimaryThread();
   }

   @Override
   public boolean isOwnedByCurrentRegion(@NotNull Block var1) {
      return this.plugin.getServer().isPrimaryThread();
   }

   @Override
   public boolean isOwnedByCurrentRegion(@NotNull World var1, int var2, int var3) {
      return this.plugin.getServer().isPrimaryThread();
   }

   @Override
   public boolean isOwnedByCurrentRegion(@NotNull World var1, int var2, int var3, int var4) {
      return this.plugin.getServer().isPrimaryThread();
   }

   @Override
   public boolean isOwnedByCurrentRegion(@NotNull Entity var1) {
      return this.plugin.getServer().isPrimaryThread();
   }

   @Override
   public boolean isGlobalTickThread() {
      return this.plugin.getServer().isPrimaryThread();
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runNextTick(@NotNull Consumer<WrappedTask> var1) {
      CompletableFuture var2 = new CompletableFuture();
      WrappedTask[] var3 = new WrappedTask[1];
      var3[0] = this.wrapTask(this.scheduler.runTask(this.plugin, () -> restoredLambda$runNextTick$0(var1, var3, var2)));
      return var2;
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runAsync(@NotNull Consumer<WrappedTask> var1) {
      CompletableFuture var2 = new CompletableFuture();
      WrappedTask[] var3 = new WrappedTask[1];
      var3[0] = this.wrapTask(this.scheduler.runTaskAsynchronously(this.plugin, () -> restoredLambda$runAsync$1(var1, var3, var2)));
      return var2;
   }

   @Override
   public WrappedTask runLater(@NotNull Runnable var1, long var2) {
      return this.wrapTask(this.scheduler.runTaskLater(this.plugin, var1, var2));
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runLater(@NotNull Consumer<WrappedTask> var1, long var2) {
      CompletableFuture var4 = new CompletableFuture();
      WrappedTask[] var5 = new WrappedTask[1];
      var5[0] = this.wrapTask(this.scheduler.runTaskLater(this.plugin, () -> restoredLambda$runLater$2(var1, var5, var4), var2));
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
      return this.wrapTask(this.scheduler.runTaskLaterAsynchronously(this.plugin, var1, var2));
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runLaterAsync(@NotNull Consumer<WrappedTask> var1, long var2) {
      CompletableFuture var4 = new CompletableFuture();
      WrappedTask[] var5 = new WrappedTask[1];
      var5[0] = this.wrapTask(this.scheduler.runTaskLaterAsynchronously(this.plugin, () -> restoredLambda$runLaterAsync$3(var1, var5, var4), var2));
      return var4;
   }

   @Override
   public WrappedTask runLaterAsync(@NotNull Runnable var1, long var2, TimeUnit var4) {
      return this.runLaterAsync(var1, TimeConverter.toTicks(var2, var4));
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runLaterAsync(@NotNull Consumer<WrappedTask> var1, long var2, TimeUnit var4) {
      return this.runLaterAsync(var1, TimeConverter.toTicks(var2, var4));
   }

   @Override
   public WrappedTask runTimer(@NotNull Runnable var1, long var2, long var4) {
      return this.wrapTask(this.scheduler.runTaskTimer(this.plugin, var1, var2, var4));
   }

   @Override
   public void runTimer(@NotNull Consumer<WrappedTask> var1, long var2, long var4) {
      WrappedTask[] var6 = new WrappedTask[1];
      var6[0] = this.wrapTask(this.scheduler.runTaskTimer(this.plugin, () -> restoredLambda$runTimer$4(var1, var6), var2, var4));
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
      return this.wrapTask(this.scheduler.runTaskTimerAsynchronously(this.plugin, var1, var2, var4));
   }

   @Override
   public void runTimerAsync(@NotNull Consumer<WrappedTask> var1, long var2, long var4) {
      WrappedTask[] var6 = new WrappedTask[1];
      var6[0] = this.wrapTask(this.scheduler.runTaskTimerAsynchronously(this.plugin, () -> restoredLambda$runTimerAsync$5(var1, var6), var2, var4));
   }

   @Override
   public WrappedTask runTimerAsync(@NotNull Runnable var1, long var2, long var4, TimeUnit var6) {
      return this.runTimerAsync(var1, TimeConverter.toTicks(var2, var6), TimeConverter.toTicks(var4, var6));
   }

   @Override
   public void runTimerAsync(@NotNull Consumer<WrappedTask> var1, long var2, long var4, TimeUnit var6) {
      this.runTimerAsync(var1, TimeConverter.toTicks(var2, var6), TimeConverter.toTicks(var4, var6));
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runAtLocation(Location var1, @NotNull Consumer<WrappedTask> var2) {
      return this.runNextTick(var2);
   }

   @Override
   public WrappedTask runAtLocationLater(Location var1, @NotNull Runnable var2, long var3) {
      return this.wrapTask(this.scheduler.runTaskLater(this.plugin, var2, var3));
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runAtLocationLater(Location var1, @NotNull Consumer<WrappedTask> var2, long var3) {
      CompletableFuture var5 = new CompletableFuture();
      WrappedTask[] var6 = new WrappedTask[1];
      var6[0] = this.wrapTask(this.scheduler.runTaskLater(this.plugin, () -> restoredLambda$runAtLocationLater$6(var2, var6, var5), var3));
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
      return this.wrapTask(this.scheduler.runTaskTimer(this.plugin, var2, var3, var5));
   }

   @Override
   public void runAtLocationTimer(Location var1, @NotNull Consumer<WrappedTask> var2, long var3, long var5) {
      WrappedTask[] var7 = new WrappedTask[1];
      var7[0] = this.wrapTask(this.scheduler.runTaskTimer(this.plugin, () -> restoredLambda$runAtLocationTimer$7(var2, var7), var3, var5));
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
      WrappedTask[] var4 = new WrappedTask[1];
      var4[0] = this.wrapTask(this.scheduler.runTask(this.plugin, () -> restoredLambda$runAtEntity$8(var2, var4, var3)));
      return var3;
   }

   @NotNull
   @Override
   public CompletableFuture<EntityTaskResult> runAtEntityWithFallback(Entity var1, @NotNull Consumer<WrappedTask> var2, Runnable var3) {
      CompletableFuture var4 = new CompletableFuture();
      WrappedTask[] var5 = new WrappedTask[1];
      var5[0] = this.wrapTask(this.scheduler.runTask(this.plugin, () -> this.restoredLambda$runAtEntityWithFallback$9(var1, var2, var5, var4, var3)));
      return var4;
   }

   @Override
   public WrappedTask runAtEntityLater(Entity var1, @NotNull Runnable var2, long var3) {
      return this.runAtEntityLater(var1, var2, null, var3);
   }

   @Override
   public WrappedTask runAtEntityLater(Entity var1, @NotNull Runnable var2, @Nullable Runnable var3, long var4) {
      if (!this.isValid(var1)) {
         if (var3 != null) {
            var3.run();
         }

         return null;
      } else {
         return this.wrapTask(this.scheduler.runTaskLater(this.plugin, var2, var4));
      }
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
      if (!this.isValid(var1) && var3 != null) {
         var3.run();
         var6.complete(null);
      }

      WrappedTask[] var7 = new WrappedTask[1];
      var7[0] = this.wrapTask(this.scheduler.runTaskLater(this.plugin, () -> restoredLambda$runAtEntityLater$10(var2, var7, var6), var4));
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
      if (!this.isValid(var1)) {
         if (var3 != null) {
            var3.run();
         }

         return null;
      } else {
         return this.wrapTask(this.scheduler.runTaskTimer(this.plugin, var2, var4, var6));
      }
   }

   @Override
   public void runAtEntityTimer(Entity var1, @NotNull Consumer<WrappedTask> var2, long var3, long var5) {
      this.runAtEntityTimer(var1, var2, null, var3, var5);
   }

   @Override
   public void runAtEntityTimer(Entity var1, @NotNull Consumer<WrappedTask> var2, Runnable var3, long var4, long var6) {
      if (!this.isValid(var1) && var3 != null) {
         var3.run();
      }

      WrappedTask[] var8 = new WrappedTask[1];
      var8[0] = this.wrapTask(this.scheduler.runTaskTimer(this.plugin, () -> restoredLambda$runAtEntityTimer$11(var2, var8), var4, var6));
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
      this.scheduler.cancelTasks(this.plugin);
   }

   @Override
   public List<WrappedTask> getAllTasks() {
      return this.scheduler
         .getPendingTasks()
         .stream()
         .filter(var1 -> this.restoredLambda$getAllTasks$12(var1))
         .map(var1 -> this.wrapTask(var1))
         .collect(Collectors.toList());
   }

   @Override
   public List<WrappedTask> getAllServerTasks() {
      return this.scheduler.getPendingTasks().stream().map(var1 -> this.wrapTask(var1)).collect(Collectors.toList());
   }

   @Override
   public Player getPlayer(String var1) {
      return this.getPlayerFromMainThread(() -> this.restoredLambda$getPlayer$13(var1));
   }

   @Override
   public Player getPlayerExact(String var1) {
      return this.getPlayerFromMainThread(() -> this.restoredLambda$getPlayerExact$14(var1));
   }

   @Override
   public Player getPlayer(UUID var1) {
      return this.getPlayerFromMainThread(() -> this.restoredLambda$getPlayer$15(var1));
   }

   @Override
   public CompletableFuture<Boolean> teleportAsync(Entity var1, Location var2) {
      return this.teleportAsync(var1, var2, TeleportCause.PLUGIN);
   }

   @Override
   public CompletableFuture<Boolean> teleportAsync(Entity var1, Location var2, TeleportCause var3) {
      CompletableFuture var4 = new CompletableFuture();
      this.runAtEntity(var1, var4x -> this.restoredLambda$teleportAsync$16(var1, var2, var4, var4x));
      return var4;
   }

   @Override
   public WrappedTask wrapTask(@NotNull Object var1) {
      Objects.requireNonNull(var1, "nativeTask cannot be null");
      if (!(var1 instanceof BukkitTask)) {
         throw new IllegalArgumentException("The nativeTask provided must be a BukkitTask. Got: " + var1.getClass().getName() + " instead.");
      } else {
         return ImplementationTestsUtil.isCancelledSupported() ? new WrappedBukkitTask((BukkitTask)var1) : new WrappedLegacyBukkitTask((BukkitTask)var1);
      }
   }

   private Player getPlayerFromMainThread(Supplier<Player> var1) {
      if (this.plugin.getServer().isPrimaryThread()) {
         return (Player)var1.get();
      }

      try {
         return this.scheduler.<Player>callSyncMethod(this.plugin, () -> (Player)var1.get()).get();
      } catch (InterruptedException | ExecutionException var3) {
         var3.printStackTrace();
         return null;
      }
   }

   private boolean isValid(Entity var1) {
      return var1.isValid() ? !(var1 instanceof Player) || ((Player)var1).isOnline() : var1 instanceof Projectile && !var1.isDead();
   }

   // $VF: synthetic method
   private void restoredLambda$teleportAsync$16(Entity var1, Location var2, CompletableFuture var3, WrappedTask var4) {
      if (this.isValid(var1)) {
         var1.teleport(var2);
         var3.complete(true);
      } else {
         var3.complete(false);
      }
   }

   // $VF: synthetic method
   private Player restoredLambda$getPlayer$15(UUID var1) {
      return this.plugin.getServer().getPlayer(var1);
   }

   // $VF: synthetic method
   private Player restoredLambda$getPlayerExact$14(String var1) {
      return this.plugin.getServer().getPlayerExact(var1);
   }

   // $VF: synthetic method
   private Player restoredLambda$getPlayer$13(String var1) {
      return this.plugin.getServer().getPlayer(var1);
   }

   // $VF: synthetic method
   private boolean restoredLambda$getAllTasks$12(BukkitTask var1) {
      return var1.getOwner().equals(this.plugin);
   }

   // $VF: synthetic method
   private static void restoredLambda$runAtEntityTimer$11(Consumer var0, WrappedTask[] var1) {
      var0.accept(var1[0]);
   }

   // $VF: synthetic method
   private static void restoredLambda$runAtEntityLater$10(Consumer var0, WrappedTask[] var1, CompletableFuture var2) {
      var0.accept(var1[0]);
      var2.complete(null);
   }

   // $VF: synthetic method
   private void restoredLambda$runAtEntityWithFallback$9(Entity var1, Consumer var2, WrappedTask[] var3, CompletableFuture var4, Runnable var5) {
      if (this.isValid(var1)) {
         var2.accept(var3[0]);
         var4.complete(EntityTaskResult.SUCCESS);
      } else {
         var5.run();
         var4.complete(EntityTaskResult.ENTITY_RETIRED);
      }
   }

   // $VF: synthetic method
   private static void restoredLambda$runAtEntity$8(Consumer var0, WrappedTask[] var1, CompletableFuture var2) {
      var0.accept(var1[0]);
      var2.complete(EntityTaskResult.SUCCESS);
   }

   // $VF: synthetic method
   private static void restoredLambda$runAtLocationTimer$7(Consumer var0, WrappedTask[] var1) {
      var0.accept(var1[0]);
   }

   // $VF: synthetic method
   private static void restoredLambda$runAtLocationLater$6(Consumer var0, WrappedTask[] var1, CompletableFuture var2) {
      var0.accept(var1[0]);
      var2.complete(null);
   }

   // $VF: synthetic method
   private static void restoredLambda$runTimerAsync$5(Consumer var0, WrappedTask[] var1) {
      var0.accept(var1[0]);
   }

   // $VF: synthetic method
   private static void restoredLambda$runTimer$4(Consumer var0, WrappedTask[] var1) {
      var0.accept(var1[0]);
   }

   // $VF: synthetic method
   private static void restoredLambda$runLaterAsync$3(Consumer var0, WrappedTask[] var1, CompletableFuture var2) {
      var0.accept(var1[0]);
      var2.complete(null);
   }

   // $VF: synthetic method
   private static void restoredLambda$runLater$2(Consumer var0, WrappedTask[] var1, CompletableFuture var2) {
      var0.accept(var1[0]);
      var2.complete(null);
   }

   // $VF: synthetic method
   private static void restoredLambda$runAsync$1(Consumer var0, WrappedTask[] var1, CompletableFuture var2) {
      var0.accept(var1[0]);
      var2.complete(null);
   }

   // $VF: synthetic method
   private static void restoredLambda$runNextTick$0(Consumer var0, WrappedTask[] var1, CompletableFuture var2) {
      var0.accept(var1[0]);
      var2.complete(null);
   }
}
