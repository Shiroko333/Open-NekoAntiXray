package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.node;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.Token;

public abstract class ValueNode extends ElementNode {
   private final String value;

   ValueNode(@Nullable ElementNode var1, @Nullable Token var2, @NotNull String var3, @NotNull String var4) {
      super(var1, var2, var3);
      this.value = var4;
   }

   abstract String valueName();

   @NotNull
   public String value() {
      return this.value;
   }

   @NotNull
   @Override
   public Token token() {
      return Objects.requireNonNull(super.token(), "token is not set");
   }

   @NotNull
   @Override
   public StringBuilder buildToString(@NotNull StringBuilder var1, int var2) {
      char[] var3 = this.ident(var2);
      var1.append(var3).append(this.valueName()).append("('").append(this.value).append("')\n");
      return var1;
   }
}
