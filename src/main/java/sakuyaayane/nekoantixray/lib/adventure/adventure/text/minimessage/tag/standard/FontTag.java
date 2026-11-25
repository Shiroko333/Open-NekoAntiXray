package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.standard;

import sakuyaayane.nekoantixray.lib.adventure.adventure.key.InvalidKeyException;
import sakuyaayane.nekoantixray.lib.adventure.adventure.key.Key;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.Style;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.ParsingException;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.SerializableResolver;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.StyleClaim;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.TokenEmitter;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;

final class FontTag {
   static final String FONT = "font";
   static final TagResolver RESOLVER = SerializableResolver.claimingStyle(
      "font", (var0, var1) -> create(var0, var1), StyleClaim.claim("font", var0 -> var0.font(), (var0, var1) -> emit(var0, var1))
   );

   private FontTag() {
   }

   static Tag create(ArgumentQueue var0, Context var1) throws ParsingException {
      String var3 = var0.popOr("A font tag must have either arguments of either <value> or <namespace:value>").value();

      Key var2;
      try {
         if (!var0.hasNext()) {
            var2 = Key.key(var3);
         } else {
            String var4 = var0.pop().value();
            var2 = Key.key(var3, var4);
         }
      } catch (InvalidKeyException var5) {
         throw var1.newException(var5.getMessage(), var0);
      }

      return Tag.styling(var1x -> restoredLambda$create$0(var2, var1x));
   }

   static void emit(Key var0, TokenEmitter var1) {
      var1.tag("font");
      if (var0.namespace().equals("minecraft")) {
         var1.argument(var0.value());
      } else {
         var1.arguments(var0.namespace(), var0.value());
      }
   }

   // $VF: synthetic method
   private static void restoredLambda$create$0(Key var0, Style.Builder var1) {
      var1.font(var0);
   }
}
