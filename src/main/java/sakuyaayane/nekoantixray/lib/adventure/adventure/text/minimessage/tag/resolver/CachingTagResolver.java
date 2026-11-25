package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.ClaimConsumer;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.SerializableResolver;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Inserting;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;

final class CachingTagResolver implements TagResolver.WithoutArguments, MappableResolver, SerializableResolver {
   private static final Tag NULL_REPLACEMENT = (Inserting)() -> restoredLambda$static$0();
   private final Map<String, Tag> cache = new HashMap<>();
   private final TagResolver.WithoutArguments resolver;

   CachingTagResolver(TagResolver.WithoutArguments var1) {
      this.resolver = var1;
   }

   private Tag query(@NotNull String var1) {
      return this.cache.computeIfAbsent(var1, var1x -> this.restoredLambda$query$1(var1x));
   }

   @Nullable
   @Override
   public Tag resolve(@NotNull String var1) {
      Tag var2 = this.query(var1);
      return var2 == NULL_REPLACEMENT ? null : var2;
   }

   @Override
   public boolean has(@NotNull String var1) {
      return this.query(var1) != NULL_REPLACEMENT;
   }

   @Override
   public boolean contributeToMap(@NotNull Map<String, Tag> var1) {
      return this.resolver instanceof MappableResolver ? ((MappableResolver)this.resolver).contributeToMap(var1) : false;
   }

   @Override
   public void handle(@NotNull Component var1, @NotNull ClaimConsumer var2) {
      if (this.resolver instanceof SerializableResolver) {
         ((SerializableResolver)this.resolver).handle(var1, var2);
      }
   }

   @Override
   public boolean equals(@Nullable Object var1) {
      if (this == var1) {
         return true;
      }

      if (!(var1 instanceof CachingTagResolver)) {
         return false;
      }

      CachingTagResolver var2 = (CachingTagResolver)var1;
      return Objects.equals(this.resolver, var2.resolver);
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.resolver);
   }

   // $VF: synthetic method
   private Tag restoredLambda$query$1(String var1) {
      Tag var2 = this.resolver.resolve(var1);
      return var2 == null ? NULL_REPLACEMENT : var2;
   }

   // $VF: synthetic method
   private static Component restoredLambda$static$0() {
      throw new UnsupportedOperationException("no-op null tag");
   }
}
