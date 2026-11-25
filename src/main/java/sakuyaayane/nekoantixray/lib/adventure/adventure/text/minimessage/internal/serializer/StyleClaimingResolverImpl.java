package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer;

import java.util.Set;
import java.util.function.BiFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.ParsingException;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;

final class StyleClaimingResolverImpl implements TagResolver, SerializableResolver.Single {
   @NotNull
   private final Set<String> names;
   @NotNull
   private final BiFunction<ArgumentQueue, Context, Tag> handler;
   @NotNull
   private final StyleClaim<?> styleClaim;

   StyleClaimingResolverImpl(@NotNull Set<String> var1, @NotNull BiFunction<ArgumentQueue, Context, Tag> var2, @NotNull StyleClaim<?> var3) {
      this.names = var1;
      this.handler = var2;
      this.styleClaim = var3;
   }

   @Nullable
   @Override
   public Tag resolve(@NotNull String var1, @NotNull ArgumentQueue var2, @NotNull Context var3) throws ParsingException {
      return !this.names.contains(var1) ? null : this.handler.apply(var2, var3);
   }

   @Override
   public boolean has(@NotNull String var1) {
      return this.names.contains(var1);
   }

   @Nullable
   @Override
   public StyleClaim<?> claimStyle() {
      return this.styleClaim;
   }
}
