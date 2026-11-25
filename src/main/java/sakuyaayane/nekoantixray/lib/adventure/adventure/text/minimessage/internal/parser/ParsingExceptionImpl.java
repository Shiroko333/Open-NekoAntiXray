package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser;

import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.ParsingException;

@Internal
public class ParsingExceptionImpl extends ParsingException {
   private static final long serialVersionUID = 2507190809441787202L;
   private final String originalText;
   private Token @NotNull [] tokens;

   public ParsingExceptionImpl(String var1, @Nullable String var2, @NotNull Token... var3) {
      super(var1, null, true, false);
      this.tokens = var3;
      this.originalText = var2;
   }

   public ParsingExceptionImpl(String var1, @Nullable String var2, @Nullable Throwable var3, boolean var4, @NotNull Token... var5) {
      super(var1, var3, true, var4);
      this.tokens = var5;
      this.originalText = var2;
   }

   @Override
   public String getMessage() {
      String var1 = this.tokens().length != 0 ? "\n\t" + this.arrow() : "";
      String var2 = this.originalText() != null ? "\n\t" + this.originalText() + var1 : "";
      return super.getMessage() + var2;
   }

   @Nullable
   @Override
   public String detailMessage() {
      return super.getMessage();
   }

   @Nullable
   @Override
   public String originalText() {
      return this.originalText;
   }

   @NotNull
   public Token[] tokens() {
      return this.tokens;
   }

   public void tokens(@NotNull Token[] var1) {
      this.tokens = var1;
   }

   private String arrow() {
      Token[] var1 = this.tokens();
      char[] var2 = new char[var1[var1.length - 1].endIndex()];
      int var3 = 0;

      for (Token var7 : var1) {
         Arrays.fill(var2, var3, var7.startIndex(), ' ');
         var2[var7.startIndex()] = '^';
         if (Math.abs(var7.startIndex() - var7.endIndex()) > 1) {
            Arrays.fill(var2, var7.startIndex() + 1, var7.endIndex() - 1, '~');
         }

         var2[var7.endIndex() - 1] = '^';
         var3 = var7.endIndex();
      }

      return new String(var2);
   }

   @Override
   public int startIndex() {
      return this.tokens.length == 0 ? -1 : this.tokens[0].startIndex();
   }

   @Override
   public int endIndex() {
      return this.tokens.length == 0 ? -1 : this.tokens[this.tokens.length - 1].endIndex();
   }
}
