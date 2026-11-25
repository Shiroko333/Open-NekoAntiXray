package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver;

import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.ClaimConsumer;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.SerializableResolver;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;

final class EmptyTagResolver implements TagResolver, MappableResolver, SerializableResolver {
   static final EmptyTagResolver INSTANCE = new EmptyTagResolver();

   private EmptyTagResolver() {
   }

   @Nullable
   @Override
   public Tag resolve(@NotNull String var1, @NotNull ArgumentQueue var2, @NotNull Context var3) {
      return null;
   }

   @Override
   public boolean has(@NotNull String var1) {
      return false;
   }

   @Override
   public boolean contributeToMap(@NotNull Map<String, Tag> var1) {
      return true;
   }

   @Override
   public void handle(@NotNull Component var1, @NotNull ClaimConsumer var2) {
   }
}
