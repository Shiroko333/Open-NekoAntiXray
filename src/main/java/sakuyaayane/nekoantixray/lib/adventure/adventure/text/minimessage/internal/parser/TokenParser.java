package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.ParsingException;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.TagInternals;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.match.MatchedTokenConsumer;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.match.StringResolvingMatchedTokenConsumer;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.match.TokenListProducingMatchedTokenConsumer;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.node.ElementNode;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.node.RootNode;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.node.TagNode;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.node.TagPart;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.node.TextNode;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Inserting;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.ParserDirective;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;

@Internal
public final class TokenParser {
   private static final int MAX_DEPTH = 16;
   public static final char TAG_START = '<';
   public static final char TAG_END = '>';
   public static final char CLOSE_TAG = '/';
   public static final char SEPARATOR = ':';
   public static final char ESCAPE = '\\';

   private TokenParser() {
   }

   public static RootNode parse(
      @NotNull TokenParser.TagProvider var0, @NotNull Predicate<String> var1, @NotNull String var2, @NotNull String var3, boolean var4
   ) throws ParsingException {
      List var5 = tokenize(var2, false);
      return buildTree(var0, var1, var5, var2, var3, var4);
   }

   public static String resolvePreProcessTags(String var0, TokenParser.TagProvider var1) {
      int var2 = 0;
      String var4 = var0;

      String var3;
      do {
         var3 = var4;
         StringResolvingMatchedTokenConsumer var5 = new StringResolvingMatchedTokenConsumer(var3, var1);
         parseString(var3, false, var5);
         var4 = var5.result();
      } while (++var2 < 16 && !var3.equals(var4));

      return var3;
   }

   public static List<Token> tokenize(String var0, boolean var1) {
      TokenListProducingMatchedTokenConsumer var2 = new TokenListProducingMatchedTokenConsumer(var0);
      parseString(var0, var1, var2);
      List var3 = var2.result();
      parseSecondPass(var0, var3);
      return var3;
   }

   public static void parseString(String var0, boolean var1, MatchedTokenConsumer<?> var2) {
      TokenParser.FirstPassState var3 = TokenParser.FirstPassState.NORMAL;
      boolean var4 = false;
      int var5 = 0;
      int var6 = -1;
      char var7 = 0;
      int var8 = var0.length();

      for (int var9 = 0; var9 < var8; var9++) {
         int var10 = var0.codePointAt(var9);
         if (!var1 && var10 == 167 && var9 + 1 < var8) {
            int var11 = Character.toLowerCase(var0.codePointAt(var9 + 1));
            if (var11 >= 48 && var11 <= 57 || var11 >= 97 && var11 <= 102 || var11 == 114 || var11 >= 107 && var11 <= 111) {
               throw new ParsingExceptionImpl(
                  "Legacy formatting codes have been detected in a MiniMessage string - this is unsupported behaviour. Please refer to the Adventure documentation (https://docs.advntr.dev) for more information.",
                  var0,
                  null,
                  true,
                  new Token(var9, var9 + 2, TokenType.TEXT)
               );
            }
         }

         if (!Character.isBmpCodePoint(var10)) {
            var9++;
         }

         if (!var4) {
            if (var10 == 92 && var9 + 1 < var0.length()) {
               int var13 = var0.codePointAt(var9 + 1);
               switch (var3) {
                  case NORMAL:
                     var4 = var13 == 60 || var13 == 92;
                     break;
                  case STRING:
                     var4 = var7 == var13 || var13 == 92;
                     break;
                  case TAG:
                     if (var13 == 60) {
                        var4 = true;
                        var3 = TokenParser.FirstPassState.NORMAL;
                     }
               }

               if (var4) {
                  continue;
               }
            }

            switch (var3) {
               case NORMAL:
                  if (var10 == 60) {
                     var6 = var9;
                     var3 = TokenParser.FirstPassState.TAG;
                  }
                  break;
               case STRING:
                  if (var10 == var7) {
                     var3 = TokenParser.FirstPassState.TAG;
                  }
                  break;
               case TAG:
                  switch (var10) {
                     case 34:
                     case 39:
                        var7 = (char)var10;
                        if (var0.indexOf(var10, var9 + 1) != -1) {
                           var3 = TokenParser.FirstPassState.STRING;
                        }
                        break;
                     case 60:
                        var6 = var9;
                        break;
                     case 62:
                        if (var9 == var6 + 1) {
                           var3 = TokenParser.FirstPassState.NORMAL;
                        } else {
                           if (var5 != var6) {
                              var2.accept(var5, var6, TokenType.TEXT);
                           }

                           var5 = var9 + 1;
                           TokenType var14 = TokenType.OPEN_TAG;
                           if (boundsCheck(var0, var6, 1) && var0.charAt(var6 + 1) == '/') {
                              var14 = TokenType.CLOSE_TAG;
                           } else if (boundsCheck(var0, var6, 2) && var0.charAt(var9 - 1) == '/') {
                              var14 = TokenType.OPEN_CLOSE_TAG;
                           }

                           var2.accept(var6, var5, var14);
                           var3 = TokenParser.FirstPassState.NORMAL;
                        }
                  }
            }

            if (var9 == var8 - 1 && var3 == TokenParser.FirstPassState.TAG) {
               var9 = var6;
               var3 = TokenParser.FirstPassState.NORMAL;
            }
         } else {
            var4 = false;
         }
      }

      int var12 = var2.lastEndIndex();
      if (var12 == -1) {
         var2.accept(0, var0.length(), TokenType.TEXT);
      } else if (var12 != var0.length()) {
         var2.accept(var12, var0.length(), TokenType.TEXT);
      }
   }

   private static void parseSecondPass(String var0, List<Token> var1) {
      for (Token var3 : var1) {
         TokenType var4 = var3.type();
         if (var4 == TokenType.OPEN_TAG || var4 == TokenType.OPEN_CLOSE_TAG || var4 == TokenType.CLOSE_TAG) {
            int var5 = var4 == TokenType.CLOSE_TAG ? var3.startIndex() + 2 : var3.startIndex() + 1;
            int var6 = var4 == TokenType.OPEN_CLOSE_TAG ? var3.endIndex() - 2 : var3.endIndex() - 1;
            TokenParser.SecondPassState var7 = TokenParser.SecondPassState.NORMAL;
            boolean var8 = false;
            char var9 = 0;
            int var10 = var5;

            for (int var11 = var5; var11 < var6; var11++) {
               int var12 = var0.codePointAt(var11);
               if (!Character.isBmpCodePoint(var11)) {
                  var11++;
               }

               if (!var8) {
                  if (var12 == 92 && var11 + 1 < var0.length()) {
                     int var13 = var0.codePointAt(var11 + 1);
                     switch (var7) {
                        case NORMAL:
                           var8 = var13 == 60 || var13 == 92;
                           break;
                        case STRING:
                           var8 = var9 == var13 || var13 == 92;
                     }

                     if (var8) {
                        continue;
                     }
                  }

                  switch (var7) {
                     case NORMAL:
                        if (var12 == 58) {
                           if (!boundsCheck(var0, var11, 2) || var0.charAt(var11 + 1) != '/' || var0.charAt(var11 + 2) != '/') {
                              if (var10 == var11) {
                                 insert(var3, new Token(var11, var11, TokenType.TAG_VALUE));
                                 var10++;
                              } else {
                                 insert(var3, new Token(var10, var11, TokenType.TAG_VALUE));
                                 var10 = var11 + 1;
                              }
                           }
                        } else if (var12 == 39 || var12 == 34) {
                           var7 = TokenParser.SecondPassState.STRING;
                           var9 = (char)var12;
                        }
                        break;
                     case STRING:
                        if (var12 == var9) {
                           var7 = TokenParser.SecondPassState.NORMAL;
                        }
                  }
               } else {
                  var8 = false;
               }
            }

            if (var3.childTokens() != null && !var3.childTokens().isEmpty()) {
               int var14 = var3.childTokens().get(var3.childTokens().size() - 1).endIndex();
               if (var14 != var6) {
                  insert(var3, new Token(var14 + 1, var6, TokenType.TAG_VALUE));
               }
            } else {
               insert(var3, new Token(var5, var6, TokenType.TAG_VALUE));
            }
         }
      }
   }

   private static RootNode buildTree(
      @NotNull TokenParser.TagProvider var0,
      @NotNull Predicate<String> var1,
      @NotNull List<Token> var2,
      @NotNull String var3,
      @NotNull String var4,
      boolean var5
   ) throws ParsingException {
        sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.node.ElementNode var30;

      RootNode var6 = new RootNode(var3, var4);
      ElementNode var7 = var6;

      for (Token var9 : var2) {
         TokenType var10 = var9.type();
         switch (var10) {
            case TEXT:
               var7.addChild(new TextNode(var7, var9, var3));
               break;
            case OPEN_TAG:
            case OPEN_CLOSE_TAG:
               Token var11 = var9.childTokens().get(0);
               String var12 = var3.substring(var11.startIndex(), var11.endIndex());
               if (!TagInternals.sanitizeAndCheckValidTagName(var12)) {
                  var7.addChild(new TextNode(var7, var9, var3));
               } else {
                  TagNode var13 = new TagNode(var7, var9, var3, var0);
                  if (var1.test(var13.name())) {
                     Tag var27 = var0.resolve(var13);
                     if (var27 == null) {
                        var7.addChild(new TextNode(var7, var9, var3));
                        continue;
                     }

                     if (var27 == ParserDirective.RESET) {
                        if (var5) {
                           throw new ParsingExceptionImpl("<reset> tags are not allowed when strict mode is enabled", var3, var9);
                        }

                        var7 = var6;
                        continue;
                     }

                     var13.tag(var27);
                     var7.addChild(var13);
                     if (var10 != TokenType.OPEN_CLOSE_TAG && (!(var27 instanceof Inserting) || ((Inserting)var27).allowsChildren())) {
                        var7 = var13;
                     }
                     continue;
                  }

                  var7.addChild(new TextNode(var7, var9, var3));
               }
               break;
            case CLOSE_TAG:
               List var14 = var9.childTokens();
               if (var14.isEmpty()) {
                  throw new IllegalStateException("CLOSE_TAG token somehow has no children - the parser should not allow this. Original text: " + var3);
               }

               ArrayList var15 = new ArrayList(var14.size());

               for (Token var17 : (java.lang.Iterable<sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.Token>)(java.lang.Iterable<?>)(var14)) {
                  var15.add(TagPart.unquoteAndEscape(var3, var17.startIndex(), var17.endIndex()));
               }

               String var28 = (String)var15.get(0);
               if (var1.test(var28)) {
                  Tag var29 = var0.resolve(var28);
                  if (var29 != ParserDirective.RESET) {
                     for (var30 = var7; var30 instanceof TagNode; var30 = var30.parent()) {
                        List var18 = ((TagNode)var30).parts();
                        if (tagCloses(var15, var18)) {
                           if (var30 != var7 && var5) {
                              String var31 = "Unclosed tag encountered; "
                                 + ((TagNode)var7).name()
                                 + " is not closed, because "
                                 + (String)var15.get(0)
                                 + " was closed first.";
                              throw new ParsingExceptionImpl(var31, var3, var30.token(), var7.token(), var9);
                           }

                           ElementNode var19 = var30.parent();
                           if (var19 == null) {
                              throw new IllegalStateException("Root node matched with close tag value, this should not be possible. Original text: " + var3);
                           }

                           var7 = var19;
                           break;
                        }
                     }

                     if (var30 == null || var30 instanceof RootNode) {
                        var7.addChild(new TextNode(var7, var9, var3));
                     }
                  }
               } else {
                  var7.addChild(new TextNode(var7, var9, var3));
               }
         }
      }

      if (var5 && var6 != var7) {
         ArrayList var20 = new ArrayList();

         for (ElementNode var21 = var7; var21 != null && var21 instanceof TagNode; var21 = var21.parent()) {
            var20.add((TagNode)var21);
         }

         Token[] var22 = new Token[var20.size()];
         StringBuilder var23 = new StringBuilder("All tags must be explicitly closed while in strict mode. End of string found with open tags: ");
         int var24 = 0;
         ListIterator var25 = var20.listIterator(var20.size());

         while (var25.hasPrevious()) {
            TagNode var26 = (TagNode)var25.previous();
            var22[var24++] = var26.token();
            var23.append(var26.name());
            if (var25.hasPrevious()) {
               var23.append(", ");
            }
         }

         throw new ParsingExceptionImpl(var23.toString(), var3, var22);
      } else {
         return var6;
      }
   }

   private static boolean tagCloses(List<String> var0, List<TagPart> var1) {
      if (var0.size() > var1.size()) {
         return false;
      }

      if (!((String)var0.get(0)).equalsIgnoreCase(((TagPart)var1.get(0)).value())) {
         return false;
      }

      for (int var2 = 1; var2 < var0.size(); var2++) {
         if (!((String)var0.get(var2)).equals(((TagPart)var1.get(var2)).value())) {
            return false;
         }
      }

      return true;
   }

   private static boolean boundsCheck(String var0, int var1, int var2) {
      return var1 + var2 < var0.length();
   }

   private static void insert(Token var0, Token var1) {
      if (var0.childTokens() == null) {
         var0.childTokens(Collections.singletonList(var1));
      } else {
         if (var0.childTokens().size() == 1) {
            ArrayList var2 = new ArrayList(3);
            var2.add(var0.childTokens().get(0));
            var2.add(var1);
            var0.childTokens(var2);
         } else {
            var0.childTokens().add(var1);
         }
      }
   }

   public static String unescape(String var0, int var1, int var2, IntPredicate var3) {
      int var4 = var1;
      int var5 = var0.indexOf(92, var4);
      if (var5 != -1 && var5 < var2) {
         StringBuilder var6 = new StringBuilder(var2 - var1);

         while (var5 != -1 && var5 + 1 < var2) {
            if (var3.test(var0.codePointAt(var5 + 1))) {
               var6.append(var0, var4, var5);
               if (++var5 >= var2) {
                  var4 = var2;
                  break;
               }

               int var7 = var0.codePointAt(var5);
               var6.appendCodePoint(var7);
               if (Character.isBmpCodePoint(var7)) {
                  var5++;
               } else {
                  var5 += 2;
               }

               if (var5 >= var2) {
                  var4 = var2;
                  break;
               }
            } else {
               var6.append(var0, var4, ++var5);
            }

            var4 = var5;
            var5 = var0.indexOf(92, var4);
         }

         var6.append(var0, var4, var2);
         return var6.toString();
      } else {
         return var0.substring(var4, var2);
      }
   }

   // $VF: synthetic class


   enum FirstPassState {
      NORMAL,
      TAG,
      STRING;
   }

   enum SecondPassState {
      NORMAL,
      STRING;
   }

   @Internal
   public interface TagProvider {
      @Nullable
      Tag resolve(@NotNull String var1, @NotNull List<? extends Tag.Argument> var2, @Nullable Token var3);

      @Nullable
      default Tag resolve(@NotNull String var1) {
         return this.resolve(var1, Collections.emptyList(), null);
      }

      @Nullable
      default Tag resolve(@NotNull TagNode var1) {
         return this.resolve(sanitizePlaceholderName(var1.name()), var1.parts().subList(1, var1.parts().size()), var1.token());
      }

      @NotNull
      static String sanitizePlaceholderName(@NotNull String var0) {
         return var0.toLowerCase(Locale.ROOT);
      }
   }
}
