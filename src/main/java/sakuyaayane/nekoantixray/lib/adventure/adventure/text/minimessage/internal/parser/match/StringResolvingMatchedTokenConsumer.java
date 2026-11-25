package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.match;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.TagInternals;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.Token;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.TokenParser;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.TokenType;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.node.TagPart;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.PreProcess;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;

public final class StringResolvingMatchedTokenConsumer extends MatchedTokenConsumer<String> {
   private final StringBuilder builder;
   private final TokenParser.TagProvider tagProvider;

   public StringResolvingMatchedTokenConsumer(@NotNull String var1, @NotNull TokenParser.TagProvider var2) {
      super(var1);
      this.builder = new StringBuilder(var1.length());
      this.tagProvider = var2;
   }

   @Override
   public void accept(int var1, int var2, @NotNull TokenType var3) {
      super.accept(var1, var2, var3);
      if (var3 != TokenType.OPEN_TAG) {
         this.builder.append(this.input, var1, var2);
      } else {
         String var4 = this.input.substring(var1, var2);
         String var5 = this.input.substring(var1 + 1, var2 - 1);
         int var6 = var5.indexOf(58);
         String var7 = var6 == -1 ? var5 : var5.substring(0, var6);
         if (TagInternals.sanitizeAndCheckValidTagName(var7)) {
            List var8 = TokenParser.tokenize(var4, false);
            ArrayList var9 = new ArrayList();
            List var10 = var8.isEmpty() ? null : ((Token)var8.get(0)).childTokens();
            if (var10 != null) {
               for (int var11 = 1; var11 < var10.size(); var11++) {
                  var9.add(new TagPart(var4, (Token)var10.get(var11), this.tagProvider));
               }
            }

            Tag var12 = this.tagProvider.resolve(TokenParser.TagProvider.sanitizePlaceholderName(var7), var9, (Token)var8.get(0));
            if (var12 instanceof PreProcess) {
               this.builder.append(Objects.requireNonNull(((PreProcess)var12).value(), "PreProcess replacements cannot return null"));
               return;
            }
         }

         this.builder.append(var4);
      }
   }

   @NotNull
   public String result() {
      return this.builder.toString();
   }
}
