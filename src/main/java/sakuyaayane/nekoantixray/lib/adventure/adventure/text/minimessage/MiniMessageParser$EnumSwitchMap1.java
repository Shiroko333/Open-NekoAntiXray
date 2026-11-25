package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage;

import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.TokenType;

public final class MiniMessageParser$EnumSwitchMap1 {
   // $VF: synthetic field
   static final int[] $SwitchMap$net$kyori$adventure$text$minimessage$internal$parser$TokenType = new int[TokenType.values().length];

   static {
      try {
         $SwitchMap$net$kyori$adventure$text$minimessage$internal$parser$TokenType[TokenType.TEXT.ordinal()] = 1;
      } catch (NoSuchFieldError var3) {
      }

      try {
         $SwitchMap$net$kyori$adventure$text$minimessage$internal$parser$TokenType[TokenType.OPEN_TAG.ordinal()] = 2;
      } catch (NoSuchFieldError var2) {
      }

      try {
         $SwitchMap$net$kyori$adventure$text$minimessage$internal$parser$TokenType[TokenType.CLOSE_TAG.ordinal()] = 3;
      } catch (NoSuchFieldError var1) {
      }

      try {
         $SwitchMap$net$kyori$adventure$text$minimessage$internal$parser$TokenType[TokenType.OPEN_CLOSE_TAG.ordinal()] = 4;
      } catch (NoSuchFieldError var0) {
      }
   }
}
