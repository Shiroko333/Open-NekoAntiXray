package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.TextComponent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.ClaimConsumer;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.Emitable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.QuotingOverride;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.SerializableResolver;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.TokenEmitter;

final class MiniMessageSerializer {
   private MiniMessageSerializer() {
   }

   @NotNull
   static String serialize(@NotNull Component var0, @NotNull SerializableResolver var1, boolean var2) {
      StringBuilder var3 = new StringBuilder();
      MiniMessageSerializer.Collector var4 = new MiniMessageSerializer.Collector(var1, var2, var3);
      var4.mark();
      visit(var0, var4, var1, true);
      if (var2) {
         var4.popAll();
      } else {
         var4.completeTag();
      }

      return var3.toString();
   }

   private static void visit(@NotNull Component var0, MiniMessageSerializer.Collector var1, SerializableResolver var2, boolean var3) {
      var2.handle(var0, var1);
      var1.flushClaims(var0);
      Iterator var4 = var0.children().iterator();

      while (var4.hasNext()) {
         var1.mark();
         visit((Component)var4.next(), var1, var2, var3 && !var4.hasNext());
      }

      if (!var3) {
         var1.popToMark();
      }
   }

   static final class Collector implements TokenEmitter, ClaimConsumer {
      private static final String MARK = "__<'\"\\MARK__";
      private static final char[] TEXT_ESCAPES = new char[]{'\\', '<'};
      private static final char[] TAG_TOKENS = new char[]{'>', ':'};
      private static final char[] SINGLE_QUOTED_ESCAPES = new char[]{'\\', '\''};
      private static final char[] DOUBLE_QUOTED_ESCAPES = new char[]{'\\', '"'};
      private final SerializableResolver resolver;
      private final boolean strict;
      private final StringBuilder consumer;
      private String[] activeTags = new String[4];
      private int tagLevel = 0;
      private MiniMessageSerializer.Collector.TagState tagState = MiniMessageSerializer.Collector.TagState.TEXT;
      @Nullable
      Emitable componentClaim;
      final Set<String> claimedStyleElements = new HashSet<>();

      Collector(SerializableResolver var1, boolean var2, StringBuilder var3) {
         this.resolver = var1;
         this.strict = var2;
         this.consumer = var3;
      }

      private void pushActiveTag(String var1) {
         if (this.tagLevel >= this.activeTags.length) {
            this.activeTags = Arrays.copyOf(this.activeTags, this.activeTags.length * 2);
         }

         this.activeTags[this.tagLevel++] = var1;
      }

      private String popTag(boolean var1) {
         if (this.tagLevel-- <= 0) {
            throw new IllegalStateException("Unbalanced tags, tried to pop below depth");
         } else {
            String var2 = this.activeTags[this.tagLevel];
            if (!var1 && var2 == "__<'\"\\MARK__") {
               throw new IllegalStateException("Tried to pop past mark, tag stack: " + Arrays.toString(this.activeTags) + " @ " + this.tagLevel);
            } else {
               return var2;
            }
         }
      }

      void mark() {
         this.pushActiveTag("__<'\"\\MARK__");
      }

      void popToMark() {
         if (this.tagLevel != 0) {
            String var1;
            while ((var1 = this.popTag(true)) != "__<'\"\\MARK__") {
               this.emitClose(var1);
            }
         }
      }

      void popAll() {
         while (this.tagLevel > 0) {
            String var1 = this.activeTags[--this.tagLevel];
            if (var1 != "__<'\"\\MARK__") {
               this.emitClose(var1);
            }
         }
      }

      void completeTag() {
         if (this.tagState.isTag) {
            this.consumer.append('>');
            this.tagState = MiniMessageSerializer.Collector.TagState.TEXT;
         }
      }

      @NotNull
      public MiniMessageSerializer.Collector tag(@NotNull String var1) {
         this.completeTag();
         this.consumer.append('<');
         this.escapeTagContent(var1, QuotingOverride.UNQUOTED);
         this.tagState = MiniMessageSerializer.Collector.TagState.MID;
         this.pushActiveTag(var1);
         return this;
      }

      @NotNull
      @Override
      public TokenEmitter selfClosingTag(@NotNull String var1) {
         this.completeTag();
         this.consumer.append('<');
         this.escapeTagContent(var1, QuotingOverride.UNQUOTED);
         this.tagState = MiniMessageSerializer.Collector.TagState.MID_SELF_CLOSING;
         return this;
      }

      @NotNull
      @Override
      public TokenEmitter argument(@NotNull String var1) {
         if (!this.tagState.isTag) {
            throw new IllegalStateException("Not within a tag!");
         }

         this.consumer.append(':');
         this.escapeTagContent(var1, null);
         return this;
      }

      @NotNull
      @Override
      public TokenEmitter argument(@NotNull String var1, @NotNull QuotingOverride var2) {
         if (!this.tagState.isTag) {
            throw new IllegalStateException("Not within a tag!");
         }

         this.consumer.append(':');
         this.escapeTagContent(var1, Objects.requireNonNull(var2, "quotingPreference"));
         return this;
      }

      @NotNull
      @Override
      public TokenEmitter argument(@NotNull Component var1) {
         String var2 = MiniMessageSerializer.serialize(var1, this.resolver, this.strict);
         return this.argument(var2, QuotingOverride.QUOTED);
      }

      @NotNull
      public MiniMessageSerializer.Collector text(@NotNull String var1) {
         this.completeTag();
         appendEscaping(this.consumer, var1, TEXT_ESCAPES, true);
         return this;
      }

      private void escapeTagContent(String var1, @Nullable QuotingOverride var2) {
         boolean var3 = var2 == QuotingOverride.QUOTED;
         boolean var4 = false;
         boolean var5 = false;

         for (int var6 = 0; var6 < var1.length(); var6++) {
            char var7 = var1.charAt(var6);
            if (var7 != '>' && var7 != ':' && var7 != ' ') {
               if (var7 == '\'') {
                  var4 = true;
                  break;
               }

               if (var7 == '"') {
                  var5 = true;
                  if (var3) {
                  }
               }
            } else {
               var3 = true;
            }
         }

         if (var4) {
            this.consumer.append('"');
            appendEscaping(this.consumer, var1, DOUBLE_QUOTED_ESCAPES, true);
            this.consumer.append('"');
         } else if (!var5 && !var3) {
            appendEscaping(this.consumer, var1, TAG_TOKENS, false);
         } else {
            this.consumer.append('\'');
            appendEscaping(this.consumer, var1, SINGLE_QUOTED_ESCAPES, true);
            this.consumer.append('\'');
         }
      }

      static void appendEscaping(StringBuilder var0, String var1, char[] var2, boolean var3) {
         int var4 = 0;
         boolean var5 = false;

         for (int var6 = 0; var6 < var1.length(); var6++) {
            char var7 = var1.charAt(var6);
            boolean var8 = false;

            for (char var12 : var2) {
               if (var7 == var12) {
                  if (!var3) {
                     throw new IllegalArgumentException("Invalid escapable character '" + var7 + "' found at index " + var6 + " in string '" + var1 + "'");
                  }

                  var8 = true;
                  break;
               }
            }

            if (var8) {
               if (var5) {
                  var0.append(var1, var4, var6);
               }

               var4 = var6 + 1;
               var0.append('\\').append(var7);
            } else {
               var5 = true;
            }
         }

         if (var4 < var1.length() && var5) {
            var0.append(var1, var4, var1.length());
         }
      }

      @NotNull
      public MiniMessageSerializer.Collector pop() {
         this.emitClose(this.popTag(false));
         return this;
      }

      private void emitClose(@NotNull String var1) {
         if (this.tagState.isTag) {
            if (this.tagState == MiniMessageSerializer.Collector.TagState.MID) {
               this.consumer.append('/');
            }

            this.consumer.append('>');
            this.tagState = MiniMessageSerializer.Collector.TagState.TEXT;
         } else {
            this.consumer.append('<').append('/');
            this.escapeTagContent(var1, QuotingOverride.UNQUOTED);
            this.consumer.append('>');
         }
      }

      @Override
      public void style(@NotNull String var1, @NotNull Emitable var2) {
         if (this.claimedStyleElements.add(Objects.requireNonNull(var1, "claimKey"))) {
            var2.emit(this);
         }
      }

      @Override
      public boolean component(@NotNull Emitable var1) {
         if (this.componentClaim != null) {
            return false;
         }

         this.componentClaim = Objects.requireNonNull(var1, "componentClaim");
         return true;
      }

      @Override
      public boolean componentClaimed() {
         return this.componentClaim != null;
      }

      @Override
      public boolean styleClaimed(@NotNull String var1) {
         return this.claimedStyleElements.contains(var1);
      }

      void flushClaims(Component var1) {
         if (this.componentClaim != null) {
            this.componentClaim.emit(this);
            this.componentClaim = null;
         } else {
            if (!(var1 instanceof TextComponent)) {
               throw new IllegalStateException("Unclaimed component " + var1);
            }

            this.text(((TextComponent)var1).content());
         }

         this.claimedStyleElements.clear();
      }

      enum TagState {
         TEXT(false),
         MID(true),
         MID_SELF_CLOSING(true);

         final boolean isTag;

         TagState(boolean var3) {
            this.isTag = var3;
         }
      }
   }
}
