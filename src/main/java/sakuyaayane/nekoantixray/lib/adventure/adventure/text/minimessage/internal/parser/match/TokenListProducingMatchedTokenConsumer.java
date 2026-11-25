package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.Token;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.TokenType;

public final class TokenListProducingMatchedTokenConsumer extends MatchedTokenConsumer<List<Token>> {
   private List<Token> result = null;

   public TokenListProducingMatchedTokenConsumer(@NotNull String var1) {
      super(var1);
   }

   @Override
   public void accept(int var1, int var2, @NotNull TokenType var3) {
      super.accept(var1, var2, var3);
      if (this.result == null) {
         this.result = new ArrayList<>();
      }

      this.result.add(new Token(var1, var2, var3));
   }

   @NotNull
   public List<Token> result() {
      return this.result == null ? Collections.emptyList() : this.result;
   }
}
