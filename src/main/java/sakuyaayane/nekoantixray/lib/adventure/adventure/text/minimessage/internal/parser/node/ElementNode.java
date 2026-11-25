package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.Token;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.TokenType;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tree.Node;

public class ElementNode implements Node {
   @Nullable
   private final ElementNode parent;
   @Nullable
   private final Token token;
   private final String sourceMessage;
   private final List<ElementNode> children = new ArrayList<>();

   ElementNode(@Nullable ElementNode var1, @Nullable Token var2, @NotNull String var3) {
      this.parent = var1;
      this.token = var2;
      this.sourceMessage = var3;
   }

   @Nullable
   public ElementNode parent() {
      return this.parent;
   }

   @Nullable
   public Token token() {
      return this.token;
   }

   @NotNull
   public String sourceMessage() {
      return this.sourceMessage;
   }

   @NotNull
   @Override
   public List<ElementNode> children() {
      return Collections.unmodifiableList(this.children);
   }

   @NotNull
   public List<ElementNode> unsafeChildren() {
      return this.children;
   }

   public void addChild(@NotNull ElementNode var1) {
      int var2 = this.children.size() - 1;
      if (var1 instanceof TextNode && !this.children.isEmpty() && this.children.get(var2) instanceof TextNode) {
         TextNode var3 = (TextNode)this.children.remove(var2);
         if (var3.token().endIndex() == var1.token().startIndex()) {
            Token var4 = new Token(var3.token().startIndex(), var1.token().endIndex(), TokenType.TEXT);
            this.children.add(new TextNode(this, var4, var3.sourceMessage()));
         } else {
            this.children.add(var3);
            this.children.add(var1);
         }
      } else {
         this.children.add(var1);
      }
   }

   @NotNull
   public StringBuilder buildToString(@NotNull StringBuilder var1, int var2) {
      char[] var3 = this.ident(var2);
      var1.append(var3).append("Node {\n");

      for (ElementNode var5 : this.children) {
         var5.buildToString(var1, var2 + 1);
      }

      var1.append(var3).append("}\n");
      return var1;
   }

   char @NotNull [] ident(int var1) {
      char[] var2 = new char[var1 * 2];
      Arrays.fill(var2, ' ');
      return var2;
   }

   @NotNull
   @Override
   public String toString() {
      return this.buildToString(new StringBuilder(), 0).toString();
   }
}
