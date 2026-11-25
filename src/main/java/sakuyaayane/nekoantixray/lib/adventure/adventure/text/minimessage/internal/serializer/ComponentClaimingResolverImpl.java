package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.ParsingException;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;

class ComponentClaimingResolverImpl implements TagResolver, SerializableResolver.Single {
   @NotNull
   private final Set<String> names;
   @NotNull
   private final BiFunction<ArgumentQueue, Context, Tag> handler;
   @NotNull
   private final Function<Component, @Nullable Emitable> componentClaim;

   ComponentClaimingResolverImpl(Set<String> var1, BiFunction<ArgumentQueue, Context, Tag> var2, Function<Component, @Nullable Emitable> var3) {
      this.names = var1;
      this.handler = var2;
      this.componentClaim = var3;
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
   public Emitable claimComponent(@NotNull Component var1) {
      return this.componentClaim.apply(var1);
   }
}
