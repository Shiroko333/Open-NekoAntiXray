package sakuyaayane.nekoantixray.lib.adventure.adventure.bossbar;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import sakuyaayane.nekoantixray.lib.adventure.adventure.internal.Internals;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.util.Services;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

final class BossBarImpl extends HackyBossBarPlatformBridge implements BossBar {
   private final List<BossBar.Listener> listeners = new CopyOnWriteArrayList<>();
   private Component name;
   private float progress;
   private BossBar.Color color;
   private BossBar.Overlay overlay;
   private final Set<BossBar.Flag> flags = EnumSet.noneOf(BossBar.Flag.class);
   @Nullable
   BossBarImplementation implementation;

   BossBarImpl(@NotNull Component var1, float var2, @NotNull BossBar.Color var3, @NotNull BossBar.Overlay var4) {
      this.name = Objects.requireNonNull(var1, "name");
      this.progress = var2;
      this.color = Objects.requireNonNull(var3, "color");
      this.overlay = Objects.requireNonNull(var4, "overlay");
   }

   BossBarImpl(@NotNull Component var1, float var2, @NotNull BossBar.Color var3, @NotNull BossBar.Overlay var4, @NotNull Set<BossBar.Flag> var5) {
      this(var1, var2, var3, var4);
      this.flags.addAll(var5);
   }

   @NotNull
   @Override
   public Component name() {
      return this.name;
   }

   @NotNull
   @Override
   public BossBar name(@NotNull Component var1) {
      Objects.requireNonNull(var1, "name");
      Component var2 = this.name;
      if (!Objects.equals(var1, var2)) {
         this.name = var1;
         this.forEachListener(var3 -> this.restoredLambda$name$0(var2, var1, var3));
      }

      return this;
   }

   @Override
   public float progress() {
      return this.progress;
   }

   @NotNull
   @Override
   public BossBar progress(float var1) {
      checkProgress(var1);
      float var2 = this.progress;
      if (var1 != var2) {
         this.progress = var1;
         this.forEachListener(var3 -> this.restoredLambda$progress$1(var2, var1, var3));
      }

      return this;
   }

   static void checkProgress(float var0) {
      if (var0 < 0.0F || var0 > 1.0F) {
         throw new IllegalArgumentException("progress must be between 0.0 and 1.0, was " + var0);
      }
   }

   @NotNull
   @Override
   public BossBar.Color color() {
      return this.color;
   }

   @NotNull
   @Override
   public BossBar color(@NotNull BossBar.Color var1) {
      Objects.requireNonNull(var1, "color");
      BossBar.Color var2 = this.color;
      if (var1 != var2) {
         this.color = var1;
         this.forEachListener(var3 -> this.restoredLambda$color$2(var2, var1, var3));
      }

      return this;
   }

   @NotNull
   @Override
   public BossBar.Overlay overlay() {
      return this.overlay;
   }

   @NotNull
   @Override
   public BossBar overlay(@NotNull BossBar.Overlay var1) {
      Objects.requireNonNull(var1, "overlay");
      BossBar.Overlay var2 = this.overlay;
      if (var1 != var2) {
         this.overlay = var1;
         this.forEachListener(var3 -> this.restoredLambda$overlay$3(var2, var1, var3));
      }

      return this;
   }

   @NotNull
   @Override
   public Set<BossBar.Flag> flags() {
      return Collections.unmodifiableSet(this.flags);
   }

   @NotNull
   @Override
   public BossBar flags(@NotNull Set<BossBar.Flag> var1) {
      if (var1.isEmpty() && !this.flags.isEmpty()) {
         EnumSet var5 = EnumSet.copyOf(this.flags);
         this.flags.clear();
         this.forEachListener(var2x -> this.restoredLambda$flags$4(var5, var2x));
      } else if (!this.flags.equals(var1)) {
         EnumSet var2 = EnumSet.copyOf(this.flags);
         this.flags.clear();
         this.flags.addAll(var1);
         EnumSet var3 = EnumSet.copyOf(var1);
         var3.removeIf(var1x -> var2.contains(var1x));
         EnumSet var4 = EnumSet.copyOf((Collection)var2);
         var4.removeIf(var1x -> var1.contains(var1x));
         this.forEachListener(var3x -> this.restoredLambda$flags$5(var3, var4, var3x));
      }

      return this;
   }

   @Override
   public boolean hasFlag(@NotNull BossBar.Flag var1) {
      return this.flags.contains(var1);
   }

   @NotNull
   @Override
   public BossBar addFlag(@NotNull BossBar.Flag var1) {
      return this.editFlags(var1, (var0, var1x) -> var0.add(var1x), (var0, var1x) -> onFlagsAdded(var0, var1x));
   }

   @NotNull
   @Override
   public BossBar removeFlag(@NotNull BossBar.Flag var1) {
      return this.editFlags(var1, (var0, var1x) -> var0.remove(var1x), (var0, var1x) -> onFlagsRemoved(var0, var1x));
   }

   @NotNull
   private BossBar editFlags(
      @NotNull BossBar.Flag var1, @NotNull BiPredicate<Set<BossBar.Flag>, BossBar.Flag> var2, BiConsumer<BossBarImpl, Set<BossBar.Flag>> var3
   ) {
      if (var2.test(this.flags, var1)) {
         var3.accept(this, Collections.singleton(var1));
      }

      return this;
   }

   @NotNull
   @Override
   public BossBar addFlags(@NotNull BossBar.Flag... var1) {
      return this.editFlags(var1, (var0, var1x) -> var0.add(var1x), (var0, var1x) -> onFlagsAdded(var0, var1x));
   }

   @NotNull
   @Override
   public BossBar removeFlags(@NotNull BossBar.Flag... var1) {
      return this.editFlags(var1, (var0, var1x) -> var0.remove(var1x), (var0, var1x) -> onFlagsRemoved(var0, var1x));
   }

   @NotNull
   private BossBar editFlags(BossBar.Flag[] var1, BiPredicate<Set<BossBar.Flag>, BossBar.Flag> var2, BiConsumer<BossBarImpl, Set<BossBar.Flag>> var3) {
      if (var1.length == 0) {
         return this;
      }

      EnumSet var4 = null;
      int var5 = 0;

      for (int var6 = var1.length; var5 < var6; var5++) {
         if (var2.test(this.flags, var1[var5])) {
            if (var4 == null) {
               var4 = EnumSet.noneOf(BossBar.Flag.class);
            }

            var4.add(var1[var5]);
         }
      }

      if (var4 != null) {
         var3.accept(this, var4);
      }

      return this;
   }

   @NotNull
   @Override
   public BossBar addFlags(@NotNull Iterable<BossBar.Flag> var1) {
      return this.editFlags(var1, (var0, var1x) -> var0.add(var1x), (var0, var1x) -> onFlagsAdded(var0, var1x));
   }

   @NotNull
   @Override
   public BossBar removeFlags(@NotNull Iterable<BossBar.Flag> var1) {
      return this.editFlags(var1, (var0, var1x) -> var0.remove(var1x), (var0, var1x) -> onFlagsRemoved(var0, var1x));
   }

   @NotNull
   private BossBar editFlags(Iterable<BossBar.Flag> var1, BiPredicate<Set<BossBar.Flag>, BossBar.Flag> var2, BiConsumer<BossBarImpl, Set<BossBar.Flag>> var3) {
      EnumSet var4 = null;

      for (BossBar.Flag var6 : var1) {
         if (var2.test(this.flags, var6)) {
            if (var4 == null) {
               var4 = EnumSet.noneOf(BossBar.Flag.class);
            }

            var4.add(var6);
         }
      }

      if (var4 != null) {
         var3.accept(this, var4);
      }

      return this;
   }

   @NotNull
   @Override
   public BossBar addListener(@NotNull BossBar.Listener var1) {
      this.listeners.add(var1);
      return this;
   }

   @NotNull
   @Override
   public BossBar removeListener(@NotNull BossBar.Listener var1) {
      this.listeners.remove(var1);
      return this;
   }

   @NotNull
   @Override
   public Iterable<? extends BossBarViewer> viewers() {
      return this.implementation != null ? this.implementation.viewers() : Collections.emptyList();
   }

   private void forEachListener(@NotNull Consumer<BossBar.Listener> var1) {
      for (BossBar.Listener var3 : this.listeners) {
         var1.accept(var3);
      }
   }

   private static void onFlagsAdded(BossBarImpl var0, Set<BossBar.Flag> var1) {
      var0.forEachListener(var2 -> restoredLambda$onFlagsAdded$6(var0, var1, var2));
   }

   private static void onFlagsRemoved(BossBarImpl var0, Set<BossBar.Flag> var1) {
      var0.forEachListener(var2 -> restoredLambda$onFlagsRemoved$7(var0, var1, var2));
   }

   @NotNull
   @Override
   public Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.of(
         ExaminableProperty.of("name", this.name),
         ExaminableProperty.of("progress", this.progress),
         ExaminableProperty.of("color", this.color),
         ExaminableProperty.of("overlay", this.overlay),
         ExaminableProperty.of("flags", this.flags)
      );
   }

   @Override
   public String toString() {
      return Internals.toString(this);
   }

   // $VF: synthetic method
   private static void restoredLambda$onFlagsRemoved$7(BossBarImpl var0, Set var1, BossBar.Listener var2) {
      var2.bossBarFlagsChanged(var0, Collections.emptySet(), var1);
   }

   // $VF: synthetic method
   private static void restoredLambda$onFlagsAdded$6(BossBarImpl var0, Set var1, BossBar.Listener var2) {
      var2.bossBarFlagsChanged(var0, var1, Collections.emptySet());
   }

   // $VF: synthetic method
   private void restoredLambda$flags$5(Set var1, Set var2, BossBar.Listener var3) {
      var3.bossBarFlagsChanged(this, var1, var2);
   }

   // $VF: synthetic method
   private void restoredLambda$flags$4(Set var1, BossBar.Listener var2) {
      var2.bossBarFlagsChanged(this, Collections.emptySet(), var1);
   }

   // $VF: synthetic method
   private void restoredLambda$overlay$3(BossBar.Overlay var1, BossBar.Overlay var2, BossBar.Listener var3) {
      var3.bossBarOverlayChanged(this, var1, var2);
   }

   // $VF: synthetic method
   private void restoredLambda$color$2(BossBar.Color var1, BossBar.Color var2, BossBar.Listener var3) {
      var3.bossBarColorChanged(this, var1, var2);
   }

   // $VF: synthetic method
   private void restoredLambda$progress$1(float var1, float var2, BossBar.Listener var3) {
      var3.bossBarProgressChanged(this, var1, var2);
   }

   // $VF: synthetic method
   private void restoredLambda$name$0(Component var1, Component var2, BossBar.Listener var3) {
      var3.bossBarNameChanged(this, var1, var2);
   }

   @Internal
   static final class ImplementationAccessor {
      private static final Optional<BossBarImplementation.Provider> SERVICE = Services.service(BossBarImplementation.Provider.class);

      private ImplementationAccessor() {
      }

      @NotNull
      static <I extends BossBarImplementation> I get(@NotNull BossBar var0, @NotNull Class<I> var1) {
         BossBarImplementation var2 = ((BossBarImpl)var0).implementation;
         if (var2 == null) {
            var2 = SERVICE.get().create(var0);
            ((BossBarImpl)var0).implementation = var2;
         }

         return (I)var1.cast(var2);
      }
   }
}
