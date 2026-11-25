package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.TagInternals;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;

public interface SerializableResolver {
   @NotNull
   static TagResolver claimingComponent(
      @NotNull String var0, @NotNull BiFunction<ArgumentQueue, Context, Tag> var1, @NotNull Function<Component, @Nullable Emitable> var2
   ) {
      return claimingComponent(Collections.singleton(var0), var1, var2);
   }

   @NotNull
   static TagResolver claimingComponent(
      @NotNull Set<String> var0, @NotNull BiFunction<ArgumentQueue, Context, Tag> var1, @NotNull Function<Component, @Nullable Emitable> var2
   ) {
      HashSet var3 = new HashSet(var0);

      for (String var5 : (java.lang.Iterable<java.lang.String>)(java.lang.Iterable<?>)(var3)) {
         TagInternals.assertValidTagName(var5);
      }

      Objects.requireNonNull(var1, "handler");
      return new ComponentClaimingResolverImpl(var3, var1, var2);
   }

   @NotNull
   static TagResolver claimingStyle(@NotNull String var0, @NotNull BiFunction<ArgumentQueue, Context, Tag> var1, @NotNull StyleClaim<?> var2) {
      return claimingStyle(Collections.singleton(var0), var1, var2);
   }

   @NotNull
   static TagResolver claimingStyle(@NotNull Set<String> var0, @NotNull BiFunction<ArgumentQueue, Context, Tag> var1, @NotNull StyleClaim<?> var2) {
      HashSet var3 = new HashSet(var0);

      for (String var5 : (java.lang.Iterable<java.lang.String>)(java.lang.Iterable<?>)(var3)) {
         TagInternals.assertValidTagName(var5);
      }

      Objects.requireNonNull(var1, "handler");
      return new StyleClaimingResolverImpl(var3, var1, var2);
   }

   void handle(@NotNull Component var1, @NotNull ClaimConsumer var2);

   interface Single extends SerializableResolver {
      @Override
      default void handle(@NotNull Component var1, @NotNull ClaimConsumer var2) {
         StyleClaim var3 = this.claimStyle();
         if (var3 != null && !var2.styleClaimed(var3.claimKey())) {
            Emitable var4 = var3.apply(var1.style());
            if (var4 != null) {
               var2.style(var3.claimKey(), var4);
            }
         }

         if (!var2.componentClaimed()) {
            Emitable var5 = this.claimComponent(var1);
            if (var5 != null) {
               var2.component(var5);
            }
         }
      }

      @Nullable
      default StyleClaim<?> claimStyle() {
         return null;
      }

      @Nullable
      default Emitable claimComponent(@NotNull Component var1) {
         return null;
      }
   }
}
