package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.pointer.Pointered;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;

@NonExtendable
public interface Context {
   @Nullable
   Pointered target();

   @NotNull
   Pointered targetOrThrow();

   @NotNull
   <T extends Pointered> T targetAsType(@NotNull Class<T> var1);

   @NotNull
   Component deserialize(@NotNull String var1);

   @NotNull
   Component deserialize(@NotNull String var1, @NotNull TagResolver var2);

   @NotNull
   Component deserialize(@NotNull String var1, @NotNull TagResolver... var2);

   @NotNull
   ParsingException newException(@NotNull String var1, @NotNull ArgumentQueue var2);

   @NotNull
   ParsingException newException(@NotNull String var1);

   @NotNull
   ParsingException newException(@NotNull String var1, @Nullable Throwable var2, @NotNull ArgumentQueue var3);
}
