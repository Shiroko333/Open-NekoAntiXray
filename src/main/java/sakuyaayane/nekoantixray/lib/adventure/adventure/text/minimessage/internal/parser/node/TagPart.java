package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.node;

import java.util.function.IntPredicate;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.Token;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.TokenParser;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;

public final class TagPart implements Tag.Argument {
   private final String value;
   private final Token token;

   public TagPart(@NotNull String var1, @NotNull Token var2, TokenParser.@NotNull TagProvider var3) {
      String var4 = unquoteAndEscape(var1, var2.startIndex(), var2.endIndex());
      var4 = TokenParser.resolvePreProcessTags(var4, var3);
      this.value = var4;
      this.token = var2;
   }

   @NotNull
   @Override
   public String value() {
      return this.value;
   }

   @NotNull
   public Token token() {
      return this.token;
   }

   @NotNull
   public static String unquoteAndEscape(@NotNull String var0, int var1, int var2) {
      if (var1 == var2) {
         return "";
      }

      int var3 = var1;
      int var4 = var2;
      char var5 = var0.charAt(var3);
      char var6 = var0.charAt(var4 - 1);
      if (var5 != '\'' && var5 != '"') {
         return var0.substring(var3, var4);
      }

      var3++;
      if (var6 == '\'' || var6 == '"') {
         var4--;
      }

      return var3 > var4 ? var0.substring(var1, var2) : TokenParser.unescape(var0, var3, var4, createRestoredLambda1(var5));
   }

   @Override
   public String toString() {
      return this.value;
   }

   // $VF: synthetic method
   private static boolean restoredLambda$unquoteAndEscape$0(char var0, int var1) {
      return var1 == var0 || var1 == 92;
   }

   private static IntPredicate createRestoredLambda1(char var0) {
      return var1 -> restoredLambda$unquoteAndEscape$0(var0, var1);
   }
}
