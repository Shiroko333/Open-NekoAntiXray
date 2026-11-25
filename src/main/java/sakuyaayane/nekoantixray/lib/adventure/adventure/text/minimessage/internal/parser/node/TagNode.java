package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.ParsingExceptionImpl;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.Token;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.TokenParser;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;

public final class TagNode extends ElementNode {
   private final List<TagPart> parts;
   @Nullable
   private Tag tag = null;

   public TagNode(@NotNull ElementNode var1, @NotNull Token var2, @NotNull String var3, TokenParser.@NotNull TagProvider var4) {
      super(var1, var2, var3);
      this.parts = genParts(var2, var3, var4);
      if (this.parts.isEmpty()) {
         throw new ParsingExceptionImpl("Tag has no parts? " + this, this.sourceMessage(), this.token());
      }
   }

   @NotNull
   private static List<TagPart> genParts(@NotNull Token var0, @NotNull String var1, TokenParser.@NotNull TagProvider var2) {
      ArrayList var3 = new ArrayList();
      if (var0.childTokens() != null) {
         for (Token var5 : var0.childTokens()) {
            var3.add(new TagPart(var1, var5, var2));
         }
      }

      return var3;
   }

   @NotNull
   public List<TagPart> parts() {
      return this.parts;
   }

   @NotNull
   public String name() {
      return this.parts.get(0).value();
   }

   @NotNull
   @Override
   public Token token() {
      return Objects.requireNonNull(super.token(), "token is not set");
   }

   @NotNull
   public Tag tag() {
      return Objects.requireNonNull(this.tag, "no tag set");
   }

   public void tag(@NotNull Tag var1) {
      this.tag = var1;
   }

   @NotNull
   @Override
   public StringBuilder buildToString(@NotNull StringBuilder var1, int var2) {
      char[] var3 = this.ident(var2);
      var1.append(var3).append("TagNode(");
      int var4 = this.parts.size();

      for (int var5 = 0; var5 < var4; var5++) {
         TagPart var6 = this.parts.get(var5);
         var1.append('\'').append(var6.value()).append('\'');
         if (var5 != var4 - 1) {
            var1.append(", ");
         }
      }

      var1.append(") {\n");

      for (ElementNode var8 : this.children()) {
         var8.buildToString(var1, var2 + 1);
      }

      var1.append(var3).append("}\n");
      return var1;
   }
}
