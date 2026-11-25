package sakuyaayane.nekoantixray.lib.adventure.adventure.text.event;

import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.audience.Audience;
import sakuyaayane.nekoantixray.lib.adventure.adventure.builder.AbstractBuilder;
import sakuyaayane.nekoantixray.lib.adventure.adventure.permission.PermissionChecker;
import sakuyaayane.nekoantixray.lib.adventure.adventure.util.PlatformAPI;
import sakuyaayane.nekoantixray.lib.adventure.examination.Examinable;

@FunctionalInterface
public interface ClickCallback<T extends Audience> {
   Duration DEFAULT_LIFETIME = Duration.ofHours(12L);
   int UNLIMITED_USES = -1;

   @CheckReturnValue
   @Contract(pure = true)
   @NotNull
   static <W extends Audience, N extends W> ClickCallback<W> widen(
      @NotNull ClickCallback<N> var0, @NotNull Class<N> var1, @Nullable Consumer<? super Audience> var2
   ) {
      return var3 -> restoredLambda$widen$0(var1, var0, var2, var3);
   }

   @CheckReturnValue
   @Contract(pure = true)
   @NotNull
   static <W extends Audience, N extends W> ClickCallback<W> widen(@NotNull ClickCallback<N> var0, @NotNull Class<N> var1) {
      return widen(var0, var1, null);
   }

   void accept(@NotNull T var1);

   @CheckReturnValue
   @Contract(pure = true)
   @NotNull
   default ClickCallback<T> filter(@NotNull Predicate<T> var1) {
      return this.filter(var1, null);
   }

   @CheckReturnValue
   @Contract(pure = true)
   @NotNull
   default ClickCallback<T> filter(@NotNull Predicate<T> var1, @Nullable Consumer<? super Audience> var2) {
      return var3 -> this.restoredLambda$filter$1(var1, var2, var3);
   }

   @CheckReturnValue
   @Contract(pure = true)
   @NotNull
   default ClickCallback<T> requiringPermission(@NotNull String var1) {
      return this.requiringPermission(var1, null);
   }

   @CheckReturnValue
   @Contract(pure = true)
   @NotNull
   default ClickCallback<T> requiringPermission(@NotNull String var1, @Nullable Consumer<? super Audience> var2) {
      return this.filter(var1x -> restoredLambda$requiringPermission$2(var1, var1x), var2);
   }

   // $VF: synthetic method
   private static boolean restoredLambda$requiringPermission$2(String var0, Audience var1) {
      return var1.getOrDefault(PermissionChecker.POINTER, ClickCallbackInternals.ALWAYS_FALSE).test(var0);
   }

   // $VF: synthetic method
   private void restoredLambda$filter$1(Predicate var1, Consumer var2, Audience var3) {
      if (var1.test(var3)) {
         this.accept((T)var3);
      } else if (var2 != null) {
         var2.accept(var3);
      }
   }

   // $VF: synthetic method
   private static void restoredLambda$widen$0(Class var0, ClickCallback var1, Consumer var2, Audience var3) {
      if (var0.isInstance(var3)) {
         var1.accept((Audience)var0.cast(var3));
      } else if (var2 != null) {
         var2.accept(var3);
      }
   }

   @NonExtendable
   interface Options extends Examinable {
      @NotNull
      static ClickCallback.Options.Builder builder() {
         return new ClickCallbackOptionsImpl.BuilderImpl();
      }

      @NotNull
      static ClickCallback.Options.Builder builder(@NotNull ClickCallback.Options var0) {
         return new ClickCallbackOptionsImpl.BuilderImpl(var0);
      }

      int uses();

      @NotNull
      Duration lifetime();

      @NonExtendable
      interface Builder extends AbstractBuilder<ClickCallback.Options> {
         @NotNull
         ClickCallback.Options.Builder uses(int var1);

         @NotNull
         ClickCallback.Options.Builder lifetime(@NotNull TemporalAmount var1);
      }
   }

   @PlatformAPI
   @Internal
   interface Provider {
      @NotNull
      ClickEvent create(@NotNull ClickCallback<Audience> var1, @NotNull ClickCallback.Options var2);
   }
}
