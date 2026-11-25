package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.node;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.Token;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.TokenParser;

public final class TextNode extends ValueNode {
   private static boolean isEscape(int var0) {
      return var0 == 60 || var0 == 92;
   }

   public TextNode(@Nullable ElementNode var1, @NotNull Token var2, @NotNull String var3) {
      super(var1, var2, var3, TokenParser.unescape(var3, var2.startIndex(), var2.endIndex(), var0 -> isEscape(var0)));
   }

   @Override
   String valueName() {
      return "TextNode";
   }
}
