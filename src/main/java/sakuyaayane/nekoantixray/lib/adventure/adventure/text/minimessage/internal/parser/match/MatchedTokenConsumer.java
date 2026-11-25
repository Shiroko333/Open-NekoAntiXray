package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.match;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.TokenType;

public abstract class MatchedTokenConsumer<T> {
   protected final String input;
   private int lastIndex = -1;

   public MatchedTokenConsumer(@NotNull String var1) {
      this.input = var1;
   }

   @MustBeInvokedByOverriders
   public void accept(int var1, int var2, @NotNull TokenType var3) {
      this.lastIndex = var2;
   }

   public abstract @UnknownNullability T result();

   public final int lastEndIndex() {
      return this.lastIndex;
   }
}
