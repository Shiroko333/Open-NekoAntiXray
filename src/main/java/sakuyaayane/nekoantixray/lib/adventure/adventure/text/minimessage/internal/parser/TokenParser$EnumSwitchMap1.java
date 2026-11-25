package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser;

import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.TokenParser.FirstPassState;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.TokenParser.SecondPassState;

public final class TokenParser$EnumSwitchMap1 {
   // $VF: synthetic field
   static final int[] $SwitchMap$net$kyori$adventure$text$minimessage$internal$parser$TokenParser$FirstPassState;
   // $VF: synthetic field
   static final int[] $SwitchMap$net$kyori$adventure$text$minimessage$internal$parser$TokenParser$SecondPassState;
   // $VF: synthetic field
   static final int[] $SwitchMap$net$kyori$adventure$text$minimessage$internal$parser$TokenType = new int[TokenType.values().length];

   static {
      try {
         $SwitchMap$net$kyori$adventure$text$minimessage$internal$parser$TokenType[TokenType.TEXT.ordinal()] = 1;
      } catch (NoSuchFieldError var8) {
      }

      try {
         $SwitchMap$net$kyori$adventure$text$minimessage$internal$parser$TokenType[TokenType.OPEN_TAG.ordinal()] = 2;
      } catch (NoSuchFieldError var7) {
      }

      try {
         $SwitchMap$net$kyori$adventure$text$minimessage$internal$parser$TokenType[TokenType.OPEN_CLOSE_TAG.ordinal()] = 3;
      } catch (NoSuchFieldError var6) {
      }

      try {
         $SwitchMap$net$kyori$adventure$text$minimessage$internal$parser$TokenType[TokenType.CLOSE_TAG.ordinal()] = 4;
      } catch (NoSuchFieldError var5) {
      }

      $SwitchMap$net$kyori$adventure$text$minimessage$internal$parser$TokenParser$SecondPassState = new int[SecondPassState.values().length];

      try {
         $SwitchMap$net$kyori$adventure$text$minimessage$internal$parser$TokenParser$SecondPassState[SecondPassState.NORMAL.ordinal()] = 1;
      } catch (NoSuchFieldError var4) {
      }

      try {
         $SwitchMap$net$kyori$adventure$text$minimessage$internal$parser$TokenParser$SecondPassState[SecondPassState.STRING.ordinal()] = 2;
      } catch (NoSuchFieldError var3) {
      }

      $SwitchMap$net$kyori$adventure$text$minimessage$internal$parser$TokenParser$FirstPassState = new int[FirstPassState.values().length];

      try {
         $SwitchMap$net$kyori$adventure$text$minimessage$internal$parser$TokenParser$FirstPassState[FirstPassState.NORMAL.ordinal()] = 1;
      } catch (NoSuchFieldError var2) {
      }

      try {
         $SwitchMap$net$kyori$adventure$text$minimessage$internal$parser$TokenParser$FirstPassState[FirstPassState.STRING.ordinal()] = 2;
      } catch (NoSuchFieldError var1) {
      }

      try {
         $SwitchMap$net$kyori$adventure$text$minimessage$internal$parser$TokenParser$FirstPassState[FirstPassState.TAG.ordinal()] = 3;
      } catch (NoSuchFieldError var0) {
      }
   }
}
