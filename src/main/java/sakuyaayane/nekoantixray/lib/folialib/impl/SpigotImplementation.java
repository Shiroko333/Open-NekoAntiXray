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
import sakuyaayane.nekoantixray.lib.folialib.type.Ref;
import sakuyaayane.nekoantixray.lib.folialib.util.TimeConverter;
import sakuyaayane.nekoantixray.lib.folialib.wrapper.task.WrappedBukkitTask;
import sakuyaayane.nekoantixray.lib.folialib.wrapper.task.WrappedTask;

public class SpigotImplementation implements PlatformScheduler {
   private final Plugin plugin;
   @NotNull
   private final BukkitScheduler scheduler;

   public SpigotImplementation(FoliaLib var1) {
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
      this.scheduler.runTask(this.plugin, var3 -> this.restoredLambda$runNextTick$0(var1, var2, var3));
      return var2;
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runAsync(@NotNull Consumer<WrappedTask> var1) {
      CompletableFuture var2 = new CompletableFuture();
      this.scheduler.runTaskAsynchronously(this.plugin, var3 -> this.restoredLambda$runAsync$1(var1, var2, var3));
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
      this.scheduler.runTaskLater(this.plugin, var3 -> this.restoredLambda$runLater$2(var1, var4, var3), var2);
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
      this.scheduler.runTaskLaterAsynchronously(this.plugin, var3 -> this.restoredLambda$runLaterAsync$3(var1, var4, var3), var2);
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
      this.scheduler.runTaskTimer(this.plugin, var2x -> this.restoredLambda$runTimer$4(var1, var2x), var2, var4);
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
      this.scheduler.runTaskTimerAsynchronously(this.plugin, var2x -> this.restoredLambda$runTimerAsync$5(var1, var2x), var2, var4);
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
      return this.runLater(var2, var3);
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runAtLocationLater(Location var1, @NotNull Consumer<WrappedTask> var2, long var3) {
      return this.runLater(var2, var3);
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
      return this.runTimer(var2, var3, var5);
   }

   @Override
   public void runAtLocationTimer(Location var1, @NotNull Consumer<WrappedTask> var2, long var3, long var5) {
      this.runTimer(var2, var3, var5);
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
      this.runNextTick(var2x -> restoredLambda$runAtEntity$6(var2, var3, var2x));
      return var3;
   }

   @NotNull
   @Override
   public CompletableFuture<EntityTaskResult> runAtEntityWithFallback(Entity var1, @NotNull Consumer<WrappedTask> var2, Runnable var3) {
      CompletableFuture var4 = new CompletableFuture();
      this.runNextTick(var5 -> this.restoredLambda$runAtEntityWithFallback$7(var1, var2, var4, var3, var5));
      return var4;
   }

   @Override
   public WrappedTask runAtEntityLater(Entity var1, @NotNull Runnable var2, long var3) {
      return this.runAtEntityLater(var1, var2, null, var3);
   }

   @Override
   public WrappedTask runAtEntityLater(Entity var1, @NotNull Runnable var2, Runnable var3, long var4) {
      if (!this.isValid(var1)) {
         if (var3 != null) {
            var3.run();
         }

         return null;
      } else {
         return this.runLater(var2, var4);
      }
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runAtEntityLater(Entity var1, @NotNull Consumer<WrappedTask> var2, long var3) {
      return this.runAtEntityLater(var1, var2, null, var3);
   }

   @NotNull
   @Override
   public CompletableFuture<Void> runAtEntityLater(Entity var1, @NotNull Consumer<WrappedTask> var2, @Nullable Runnable var3, long var4) {
      CompletableFuture var6 = new CompletableFuture();
      if (!this.isValid(var1)) {
         var6.complete(null);
         if (var3 != null) {
            var3.run();
         }

         return var6;
      } else {
         this.runLater(var5 -> this.restoredLambda$runAtEntityLater$8(var1, var3, var6, var2, var5), var4);
         return var6;
      }
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
   public WrappedTask runAtEntityTimer(Entity var1, @NotNull Runnable var2, @Nullable Runnable var3, long var4, long var6) {
      if (!this.isValid(var1)) {
         if (var3 != null) {
            var3.run();
         }

         return null;
      } else {
         Ref var8 = new Ref();
         WrappedTask var9 = this.runTimer(() -> this.restoredLambda$runAtEntityTimer$9(var8, var1, var3, var2), var4, var6);
         var8.set(var9);
         return var9;
      }
   }

   @Override
   public void runAtEntityTimer(Entity var1, @NotNull Consumer<WrappedTask> var2, long var3, long var5) {
      this.runAtEntityTimer(var1, var2, null, var3, var5);
   }

   @Override
   public void runAtEntityTimer(Entity var1, @NotNull Consumer<WrappedTask> var2, Runnable var3, long var4, long var6) {
      if (!this.isValid(var1)) {
         if (var3 != null) {
            var3.run();
         }
      } else {
         this.runTimer(var4x -> this.restoredLambda$runAtEntityTimer$10(var1, var3, var2, var4x), var4, var6);
      }
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
         .filter(var1 -> this.restoredLambda$getAllTasks$11(var1))
         .map(var1 -> this.wrapTask(var1))
         .collect(Collectors.toList());
   }

   @Override
   public List<WrappedTask> getAllServerTasks() {
      return this.scheduler.getPendingTasks().stream().map(var1 -> this.wrapTask(var1)).collect(Collectors.toList());
   }

   @Override
   public Player getPlayer(String var1) {
      return this.getPlayerFromMainThread(() -> this.restoredLambda$getPlayer$12(var1));
   }

   @Override
   public Player getPlayerExact(String var1) {
      return this.getPlayerFromMainThread(() -> this.restoredLambda$getPlayerExact$13(var1));
   }

   @Override
   public Player getPlayer(UUID var1) {
      return this.getPlayerFromMainThread(() -> this.restoredLambda$getPlayer$14(var1));
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

   @Override
   public CompletableFuture<Boolean> teleportAsync(Entity var1, Location var2) {
      return this.teleportAsync(var1, var2, TeleportCause.PLUGIN);
   }

   @Override
   public CompletableFuture<Boolean> teleportAsync(Entity var1, Location var2, TeleportCause var3) {
      CompletableFuture var4 = new CompletableFuture();
      this.runAtEntity(var1, var4x -> this.restoredLambda$teleportAsync$15(var1, var2, var4, var4x));
      return var4;
   }

   @Override
   public WrappedTask wrapTask(@NotNull Object var1) {
      Objects.requireNonNull(var1, "nativeTask cannot be null");
      if (!(var1 instanceof BukkitTask)) {
         throw new IllegalArgumentException("The nativeTask provided must be a BukkitTask. Got: " + var1.getClass().getName() + " instead.");
      } else {
         return new WrappedBukkitTask((BukkitTask)var1);
      }
   }

   private boolean isValid(Entity var1) {
      return var1.isValid() ? !(var1 instanceof Player) || ((Player)var1).isOnline() : var1 instanceof Projectile && !var1.isDead();
   }

   // $VF: synthetic method
   private void restoredLambda$teleportAsync$15(Entity var1, Location var2, CompletableFuture var3, WrappedTask var4) {
      if (this.isValid(var1)) {
         var1.teleport(var2);
         var3.complete(true);
      } else {
         var3.complete(false);
      }
   }

   // $VF: synthetic method
   private Player restoredLambda$getPlayer$14(UUID var1) {
      return this.plugin.getServer().getPlayer(var1);
   }

   // $VF: synthetic method
   private Player restoredLambda$getPlayerExact$13(String var1) {
      return this.plugin.getServer().getPlayerExact(var1);
   }

   // $VF: synthetic method
   private Player restoredLambda$getPlayer$12(String var1) {
      return this.plugin.getServer().getPlayer(var1);
   }

   // $VF: synthetic method
   private boolean restoredLambda$getAllTasks$11(BukkitTask var1) {
      return var1.getOwner().equals(this.plugin);
   }

   // $VF: synthetic method
   private void restoredLambda$runAtEntityTimer$10(Entity var1, Runnable var2, Consumer var3, WrappedTask var4) {
      if (!this.isValid(var1)) {
         if (var2 != null) {
            var2.run();
         }
      } else {
         var3.accept(var4);
      }
   }

   // $VF: synthetic method
   private void restoredLambda$runAtEntityTimer$9(Ref var1, Entity var2, Runnable var3, Runnable var4) {
      WrappedTask var5 = (WrappedTask)var1.get();
      if (var5 != null && !this.isValid(var2)) {
         var5.cancel();
         if (var3 != null) {
            var3.run();
         }
      } else {
         var4.run();
      }
   }

   // $VF: synthetic method
   private void restoredLambda$runAtEntityLater$8(Entity var1, Runnable var2, CompletableFuture var3, Consumer var4, WrappedTask var5) {
      if (!this.isValid(var1)) {
         if (var2 != null) {
            var2.run();
         }

         var3.complete(null);
      } else {
         var4.accept(var5);
         var3.complete(null);
      }
   }

   // $VF: synthetic method
   private void restoredLambda$runAtEntityWithFallback$7(Entity var1, Consumer var2, CompletableFuture var3, Runnable var4, WrappedTask var5) {
      if (this.isValid(var1)) {
         var2.accept(var5);
         var3.complete(EntityTaskResult.SUCCESS);
      } else {
         var4.run();
         var3.complete(EntityTaskResult.ENTITY_RETIRED);
      }
   }

   // $VF: synthetic method
   private static void restoredLambda$runAtEntity$6(Consumer var0, CompletableFuture var1, WrappedTask var2) {
      var0.accept(var2);
      var1.complete(EntityTaskResult.SUCCESS);
   }

   // $VF: synthetic method
   private void restoredLambda$runTimerAsync$5(Consumer var1, BukkitTask var2) {
      var1.accept(this.wrapTask(var2));
   }

   // $VF: synthetic method
   private void restoredLambda$runTimer$4(Consumer var1, BukkitTask var2) {
      var1.accept(this.wrapTask(var2));
   }

   // $VF: synthetic method
   private void restoredLambda$runLaterAsync$3(Consumer var1, CompletableFuture var2, BukkitTask var3) {
      var1.accept(this.wrapTask(var3));
      var2.complete(null);
   }

   // $VF: synthetic method
   private void restoredLambda$runLater$2(Consumer var1, CompletableFuture var2, BukkitTask var3) {
      var1.accept(this.wrapTask(var3));
      var2.complete(null);
   }

   // $VF: synthetic method
   private void restoredLambda$runAsync$1(Consumer var1, CompletableFuture var2, BukkitTask var3) {
      var1.accept(this.wrapTask(var3));
      var2.complete(null);
   }

   // $VF: synthetic method
   private void restoredLambda$runNextTick$0(Consumer var1, CompletableFuture var2, BukkitTask var3) {
      var1.accept(this.wrapTask(var3));
      var2.complete(null);
   }
}
