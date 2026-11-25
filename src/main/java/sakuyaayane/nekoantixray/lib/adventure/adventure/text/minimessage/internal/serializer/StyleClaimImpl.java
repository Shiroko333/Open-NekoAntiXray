package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.Style;

class StyleClaimImpl<V> implements StyleClaim<V> {
   private final String claimKey;
   private final Function<Style, V> lens;
   private final Predicate<V> filter;
   private final BiConsumer<V, TokenEmitter> emitable;

   StyleClaimImpl(String var1, Function<Style, @Nullable V> var2, Predicate<V> var3, BiConsumer<V, TokenEmitter> var4) {
      this.claimKey = var1;
      this.lens = var2;
      this.filter = var3;
      this.emitable = var4;
   }

   @NotNull
   @Override
   public String claimKey() {
      return this.claimKey;
   }

   @Nullable
   @Override
   public Emitable apply(@NotNull Style var1) {
      Object var2 = this.lens.apply(var1);
      return var2 != null && this.filter.test((V)var2) ? var2x -> this.restoredLambda$apply$0(var2, var2x) : null;
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.claimKey);
   }

   @Override
   public boolean equals(@Nullable Object var1) {
      if (this == var1) {
         return true;
      }

      if (!(var1 instanceof StyleClaimImpl)) {
         return false;
      }

      StyleClaimImpl var2 = (StyleClaimImpl)var1;
      return Objects.equals(this.claimKey, var2.claimKey);
   }

   // $VF: synthetic method
   private void restoredLambda$apply$0(Object var1, TokenEmitter var2) {
      this.emitable.accept((V)var1, var2);
   }
}
