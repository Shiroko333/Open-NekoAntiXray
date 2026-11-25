package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.standard;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.Style;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.TextDecoration;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.SerializableResolver;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.StyleClaim;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.TokenEmitter;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;

final class DecorationTag {
   private static final String B = "b";
   private static final String I = "i";
   private static final String EM = "em";
   private static final String OBF = "obf";
   private static final String ST = "st";
   private static final String U = "u";
   public static final String REVERT = "!";
   static final Map<TextDecoration, TagResolver> RESOLVERS = (java.util.Map<sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.TextDecoration,sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver>)(Stream.of(
         resolvers(TextDecoration.OBFUSCATED, "obf"),
         resolvers(TextDecoration.BOLD, "b"),
         resolvers(TextDecoration.STRIKETHROUGH, "st"),
         resolvers(TextDecoration.UNDERLINED, "u"),
         resolvers(TextDecoration.ITALIC, "em", "i")
      )
      .collect(
         Collectors.toMap(
            var0 -> (TextDecoration)((java.util.Map.Entry)var0).getKey(),
            var0 -> restoredLambda$static$3((java.util.Map.Entry)(var0)),
            (var0, var1) -> restoredLambda$static$4((TagResolver)var0, (TagResolver)var1),
            () -> new LinkedHashMap()
         )
      ));
   static final TagResolver RESOLVER = TagResolver.resolver(RESOLVERS.values());

   static Entry<TextDecoration, Stream<TagResolver>> resolvers(TextDecoration var0, @Nullable String var1, @NotNull String... var2) {
      String var3 = TextDecoration.NAMES.key(var0);
      HashSet var4 = new HashSet();
      var4.add(var3);
      if (var1 != null) {
         var4.add(var1);
      }

      Collections.addAll(var4, var2);
      return new SimpleImmutableEntry<>(
         var0,
         Stream.concat(
            Stream.of(
               SerializableResolver.claimingStyle(
                  var4, (var1x, var2x) -> restoredLambda$resolvers$0(var0, var1x, var2x), claim(var0, (var2x, var3x) -> restoredLambda$resolvers$1(var3, var1, var2x, var3x))
               )
            ),
            var4.stream().map(var1x -> restoredLambda$resolvers$2(var0, (java.lang.String)(var1x)))
         )
      );
   }

   private DecorationTag() {
   }

   static Tag create(TextDecoration var0, ArgumentQueue var1, Context var2) {
      boolean var3 = !var1.hasNext() || !var1.pop().isFalse();
      return Tag.styling(var0.withState(var3));
   }

   static Tag createNegated(TextDecoration var0) {
      return Tag.styling(var0.withState(false));
   }

   @NotNull
   static StyleClaim<TextDecoration.State> claim(@NotNull TextDecoration var0, @NotNull BiConsumer<TextDecoration.State, TokenEmitter> var1) {
      Objects.requireNonNull(var0, "decoration");
      return StyleClaim.claim("decoration_" + TextDecoration.NAMES.key(var0), var1x -> restoredLambda$claim$5(var0, var1x), var0x -> restoredLambda$claim$6(var0x), var1);
   }

   static void emit(@NotNull String var0, @NotNull String var1, TextDecoration.@NotNull State var2, @NotNull TokenEmitter var3) {
      if (var2 == TextDecoration.State.FALSE) {
         var3.tag("!" + var0);
      } else {
         var3.tag(var0);
      }
   }

   // $VF: synthetic method
   private static boolean restoredLambda$claim$6(TextDecoration.State var0) {
      return var0 != TextDecoration.State.NOT_SET;
   }

   // $VF: synthetic method
   private static TextDecoration.State restoredLambda$claim$5(TextDecoration var0, Style var1) {
      return var1.decoration(var0);
   }

   // $VF: synthetic method
   private static TagResolver restoredLambda$static$4(TagResolver var0, TagResolver var1) {
      return TagResolver.builder().resolver(var0).resolver(var1).build();
   }

   // $VF: synthetic method
   private static TagResolver restoredLambda$static$3(Entry var0) {
      return (sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver)(((Stream)var0.getValue()).collect(TagResolver.toTagResolver()));
   }

   // $VF: synthetic method
   private static TagResolver.Single restoredLambda$resolvers$2(TextDecoration var0, String var1) {
      return TagResolver.resolver("!" + var1, createNegated(var0));
   }

   // $VF: synthetic method
   private static void restoredLambda$resolvers$1(String var0, String var1, TextDecoration.State var2, TokenEmitter var3) {
      emit(var0, var1 == null ? var0 : var1, var2, var3);
   }

   // $VF: synthetic method
   private static Tag restoredLambda$resolvers$0(TextDecoration var0, ArgumentQueue var1, Context var2) {
      return create(var0, var1, var2);
   }
}
