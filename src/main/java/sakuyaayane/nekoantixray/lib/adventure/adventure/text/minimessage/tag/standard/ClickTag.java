package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.standard;

import sakuyaayane.nekoantixray.lib.adventure.adventure.text.event.ClickEvent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.ParsingException;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.QuotingOverride;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.SerializableResolver;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.StyleClaim;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.TokenEmitter;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;

final class ClickTag {
   private static final String CLICK = "click";
   static final TagResolver RESOLVER = SerializableResolver.claimingStyle(
      "click", (var0, var1) -> create(var0, var1), StyleClaim.claim("click", var0 -> var0.clickEvent(), (var0, var1) -> restoredLambda$static$0(var0, var1))
   );

   private ClickTag() {
   }

   static Tag create(ArgumentQueue var0, Context var1) throws ParsingException {
      String var2 = var0.popOr(() -> restoredLambda$create$1()).lowerValue();
      ClickEvent.Action var3 = ClickEvent.Action.NAMES.value(var2);
      if (var3 == null) {
         throw var1.newException("Unknown click event action '" + var2 + "'", var0);
      }

      String var4 = var0.popOr("Click event actions require a value").value();
      return Tag.styling(ClickEvent.clickEvent(var3, var4));
   }

   // $VF: synthetic method
   private static String restoredLambda$create$1() {
      return "A click tag requires an action of one of " + ClickEvent.Action.NAMES.keys();
   }

   // $VF: synthetic method
   private static void restoredLambda$static$0(ClickEvent var0, TokenEmitter var1) {
      var1.tag("click").argument(ClickEvent.Action.NAMES.key(var0.action())).argument(var0.value(), QuotingOverride.QUOTED);
   }
}
