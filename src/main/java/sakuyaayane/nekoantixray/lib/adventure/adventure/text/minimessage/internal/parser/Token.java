package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.internal.Internals;
import sakuyaayane.nekoantixray.lib.adventure.examination.Examinable;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

public final class Token implements Examinable {
   private final int startIndex;
   private final int endIndex;
   private final TokenType type;
   private List<Token> childTokens = null;

   public Token(int var1, int var2, TokenType var3) {
      this.startIndex = var1;
      this.endIndex = var2;
      this.type = var3;
   }

   public int startIndex() {
      return this.startIndex;
   }

   public int endIndex() {
      return this.endIndex;
   }

   public TokenType type() {
      return this.type;
   }

   public List<Token> childTokens() {
      return this.childTokens;
   }

   public void childTokens(List<Token> var1) {
      this.childTokens = var1;
   }

   public CharSequence get(CharSequence var1) {
      return var1.subSequence(this.startIndex, this.endIndex);
   }

   @NotNull
   @Override
   public Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.of(
         ExaminableProperty.of("startIndex", this.startIndex), ExaminableProperty.of("endIndex", this.endIndex), ExaminableProperty.of("type", this.type)
      );
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      }

      if (!(var1 instanceof Token)) {
         return false;
      }

      Token var2 = (Token)var1;
      return this.startIndex == var2.startIndex && this.endIndex == var2.endIndex && this.type == var2.type;
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.startIndex, this.endIndex, this.type);
   }

   @Override
   public String toString() {
      return Internals.toString(this);
   }
}
