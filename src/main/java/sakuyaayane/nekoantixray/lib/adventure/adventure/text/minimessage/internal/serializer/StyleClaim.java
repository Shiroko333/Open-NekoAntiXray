package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.Style;

public interface StyleClaim<V> {
   @NotNull
   static <T> StyleClaim<T> claim(@NotNull String var0, @NotNull Function<Style, @Nullable T> var1, @NotNull BiConsumer<T, TokenEmitter> var2) {
      return claim(var0, var1, var0x -> restoredLambda$claim$0(var0x), var2);
   }

   @NotNull
   static <T> StyleClaim<T> claim(
      @NotNull String var0, @NotNull Function<Style, @Nullable T> var1, @NotNull Predicate<T> var2, @NotNull BiConsumer<T, TokenEmitter> var3
   ) {
      return new StyleClaimImpl(
         Objects.requireNonNull(var0, "claimKey"),
         Objects.requireNonNull(var1, "lens"),
         Objects.requireNonNull(var2, "filter"),
         Objects.requireNonNull(var3, "emitable")
      );
   }

   @NotNull
   String claimKey();

   @Nullable
   Emitable apply(@NotNull Style var1);

   // $VF: synthetic method
   private static boolean restoredLambda$claim$0(Object var0) {
      return true;
   }
}
