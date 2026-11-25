package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.ParsingExceptionImpl;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.Token;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.TokenParser;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.TokenType;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.node.ElementNode;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.node.RootNode;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.node.TagNode;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.node.ValueNode;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Inserting;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Modifying;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;
import sakuyaayane.nekoantixray.lib.adventure.examination.Examinable;
import sakuyaayane.nekoantixray.lib.adventure.examination.string.MultiLineStringExaminer;

final class MiniMessageParser {
   final TagResolver tagResolver;

   MiniMessageParser() {
      this.tagResolver = TagResolver.standard();
   }

   MiniMessageParser(TagResolver var1) {
      this.tagResolver = var1;
   }

   @NotNull
   String escapeTokens(@NotNull ContextImpl var1) {
      StringBuilder var2 = new StringBuilder(var1.message().length());
      this.escapeTokens(var2, var1);
      return var2.toString();
   }

   void escapeTokens(StringBuilder var1, @NotNull ContextImpl var2) {
      this.escapeTokens(var1, var2.message(), var2);
   }

   private void escapeTokens(StringBuilder var1, String var2, ContextImpl var3) {
      this.processTokens(var1, var2, var3, (var3x, var4) -> this.restoredLambda$escapeTokens$0(var2, var3, var3x, var4));
   }

   @NotNull
   String stripTokens(@NotNull ContextImpl var1) {
      StringBuilder var2 = new StringBuilder(var1.message().length());
      this.processTokens(var2, var1, (var0, var1x) -> restoredLambda$stripTokens$1(var0, var1x));
      return var2.toString();
   }

   private void processTokens(@NotNull StringBuilder var1, @NotNull ContextImpl var2, BiConsumer<Token, StringBuilder> var3) {
      this.processTokens(var1, var2.message(), var2, var3);
   }

   private void processTokens(@NotNull StringBuilder var1, @NotNull String var2, @NotNull ContextImpl var3, BiConsumer<Token, StringBuilder> var4) {
      TagResolver var5 = TagResolver.resolver(this.tagResolver, var3.extraTags());

      for (Token var8 : TokenParser.tokenize(var2, true)) {
         switch (var8.type()) {
            case TEXT:
               var1.append(var2, var8.startIndex(), var8.endIndex());
               break;
            case OPEN_TAG:
            case CLOSE_TAG:
            case OPEN_CLOSE_TAG:
               if (var8.childTokens().isEmpty()) {
                  var1.append(var2, var8.startIndex(), var8.endIndex());
               } else {
                  String var9 = TokenParser.TagProvider.sanitizePlaceholderName(var8.childTokens().get(0).get(var2).toString());
                  if (var5.has(var9)) {
                     var4.accept(var8, var1);
                  } else {
                     var1.append(var2, var8.startIndex(), var8.endIndex());
                  }
               }
               break;
            default:
               throw new IllegalArgumentException("Unsupported token type " + var8.type());
         }
      }
   }

   @NotNull
   RootNode parseToTree(@NotNull ContextImpl var1) {
      TagResolver var2 = TagResolver.resolver(this.tagResolver, var1.extraTags());
      String var3 = var1.preProcessor().apply(var1.message());
      Consumer var4 = var1.debugOutput();
      if (var4 != null) {
         var4.accept("Beginning parsing message ");
         var4.accept(var3);
         var4.accept("\n");
      }

      TokenParser.TagProvider var5;
      if (var4 != null) {
         var5 = (var3x, var4x, var5x) -> restoredLambda$parseToTree$2(var4, var2, var1, var3x, var4x, var5x);
      } else {
         var5 = (var2x, var3x, var4x) -> restoredLambda$parseToTree$3(var2, var1, var2x, var3x, var4x);
      }

      Predicate var6 = var1x -> restoredLambda$parseToTree$4(var2, (java.lang.String)(var1x));
      String var7 = TokenParser.resolvePreProcessTags(var3, var5);
      var1.message(var7);
      RootNode var8 = TokenParser.parse(var5, var6, var7, var3, var1.strict());
      if (var4 != null) {
         var4.accept("Text parsed into element tree:\n");
         var4.accept(var8.toString());
      }

      return var8;
   }

   @NotNull
   Component parseFormat(@NotNull ContextImpl var1) {
      RootNode var2 = this.parseToTree(var1);
      return Objects.requireNonNull(var1.postProcessor().apply(this.treeToComponent(var2, var1)), "Post-processor must not return null");
   }

   @NotNull
   Component treeToComponent(@NotNull ElementNode var1, @NotNull ContextImpl var2) {
      Component var3 = Component.empty();
      Tag var4 = null;
      if (var1 instanceof ValueNode) {
         var3 = Component.text(((ValueNode)var1).value());
      } else if (var1 instanceof TagNode) {
         TagNode var5 = (TagNode)var1;
         var4 = var5.tag();
         if (var4 instanceof Modifying) {
            Modifying var6 = (Modifying)var4;
            this.visitModifying(var6, var5, 0);
            var6.postVisit();
         }

         if (var4 instanceof Inserting) {
            var3 = ((Inserting)var4).value();
         }
      }

      if (!var1.unsafeChildren().isEmpty()) {
         ArrayList var8 = new ArrayList(var3.children().size() + var1.children().size());
         var8.addAll(var3.children());

         for (ElementNode var7 : var1.unsafeChildren()) {
            var8.add(this.treeToComponent(var7, var2));
         }

         var3 = var3.children(var8);
      }

      if (var4 instanceof Modifying) {
         var3 = this.handleModifying((Modifying)var4, var3, 0);
      }

      Consumer var9 = var2.debugOutput();
      if (var9 != null) {
         var9.accept("==========\ntreeToComponent \n");
         var9.accept(var1.toString());
         var9.accept("\n");
         var9.accept(var3.examine(MultiLineStringExaminer.simpleEscaping()).collect(Collectors.joining("\n")));
         var9.accept("\n==========\n");
      }

      return var3;
   }

   private void visitModifying(Modifying var1, ElementNode var2, int var3) {
      var1.visit(var2, var3);

      for (ElementNode var5 : var2.unsafeChildren()) {
         this.visitModifying(var1, var5, var3 + 1);
      }
   }

   private Component handleModifying(Modifying var1, Component var2, int var3) {
      Component var4 = var1.apply(var2, var3);

      for (Component var6 : var2.children()) {
         var4 = var4.append(this.handleModifying(var1, var6, var3 + 1));
      }

      return var4;
   }

   // $VF: synthetic method
   private static boolean restoredLambda$parseToTree$4(TagResolver var0, String var1) {
      String var2 = TokenParser.TagProvider.sanitizePlaceholderName(var1);
      return var0.has(var2);
   }

   // $VF: synthetic method
   private static Tag restoredLambda$parseToTree$3(TagResolver var0, ContextImpl var1, String var2, List var3, Token var4) {
      try {
         return var0.resolve(var2, new ArgumentQueueImpl(var1, var3), var1);
      } catch (ParsingException var5) {
         return null;
      }
   }

   // $VF: synthetic method
   private static Tag restoredLambda$parseToTree$2(Consumer var0, TagResolver var1, ContextImpl var2, String var3, List var4, Token var5) {
      try {
         var0.accept("Attempting to match node '");
         var0.accept(var3);
         var0.accept("'");
         if (var5 != null) {
            var0.accept(" at column ");
            var0.accept(String.valueOf(var5.startIndex()));
         }

         var0.accept("\n");
         Tag var6 = var1.resolve(var3, new ArgumentQueueImpl(var2, var4), var2);
         if (var6 == null) {
            var0.accept("Could not match node '");
            var0.accept(var3);
            var0.accept("'\n");
         } else {
            var0.accept("Successfully matched node '");
            var0.accept(var3);
            var0.accept("' to tag ");
            var0.accept(var6 instanceof Examinable ? ((Examinable)var6).examinableName() : var6.getClass().getName());
            var0.accept("\n");
         }

         return var6;
      } catch (ParsingException var8) {
         if (var5 != null && var8 instanceof ParsingExceptionImpl) {
            ParsingExceptionImpl var7 = (ParsingExceptionImpl)var8;
            if (var7.tokens().length == 0) {
               var7.tokens(new Token[]{var5});
            }
         }

         var0.accept("Could not match node '");
         var0.accept(var3);
         var0.accept("' - ");
         var0.accept(var8.getMessage());
         var0.accept("\n");
         return null;
      }
   }

   // $VF: synthetic method
   private static void restoredLambda$stripTokens$1(Token var0, StringBuilder var1) {
   }

   // $VF: synthetic method
   private void restoredLambda$escapeTokens$0(String var1, ContextImpl var2, Token var3, StringBuilder var4) {
      var4.append('\\').append('<');
      if (var3.type() == TokenType.CLOSE_TAG) {
         var4.append('/');
      }

      List var5 = var3.childTokens();

      for (int var6 = 0; var6 < var5.size(); var6++) {
         if (var6 != 0) {
            var4.append(':');
         }

         this.escapeTokens(var4, ((Token)var5.get(var6)).get(var1).toString(), var2);
      }

      var4.append('>');
   }

   // $VF: synthetic class

}
