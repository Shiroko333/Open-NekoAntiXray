package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.standard;

import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.Style;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.ParsingException;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.SerializableResolver;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.StyleClaim;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.TokenEmitter;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;

final class InsertionTag {
   private static final String INSERTION = "insert";
   static final TagResolver RESOLVER = SerializableResolver.claimingStyle(
      "insert", (var0, var1) -> create(var0, var1), StyleClaim.claim("insert", var0 -> var0.insertion(), (var0, var1) -> emit(var0, var1))
   );

   private InsertionTag() {
   }

   static Tag create(ArgumentQueue var0, Context var1) throws ParsingException {
      String var2 = var0.popOr("A value is required to produce an insertion component").value();
      return Tag.styling(var1x -> restoredLambda$create$0(var2, var1x));
   }

   static void emit(String var0, TokenEmitter var1) {
      var1.tag("insert").argument(var0);
   }

   // $VF: synthetic method
   private static void restoredLambda$create$0(String var0, Style.Builder var1) {
      var1.insertion(var0);
   }
}
