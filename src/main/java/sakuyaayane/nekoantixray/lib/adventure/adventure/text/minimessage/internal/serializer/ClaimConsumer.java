package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer;

import org.jetbrains.annotations.NotNull;

public interface ClaimConsumer {
   void style(@NotNull String var1, @NotNull Emitable var2);

   boolean component(@NotNull Emitable var1);

   boolean styleClaimed(@NotNull String var1);

   boolean componentClaimed();
}
