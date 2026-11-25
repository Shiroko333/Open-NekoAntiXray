package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer;

import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;

public interface TokenEmitter {
   @NotNull
   TokenEmitter tag(@NotNull String var1);

   @NotNull
   TokenEmitter selfClosingTag(@NotNull String var1);

   @NotNull
   default TokenEmitter arguments(@NotNull String... var1) {
      for (String var5 : var1) {
         this.argument(var5);
      }

      return this;
   }

   @NotNull
   TokenEmitter argument(@NotNull String var1);

   @NotNull
   TokenEmitter argument(@NotNull String var1, @NotNull QuotingOverride var2);

   @NotNull
   TokenEmitter argument(@NotNull Component var1);

   @NotNull
   TokenEmitter text(@NotNull String var1);

   @NotNull
   TokenEmitter pop();
}
