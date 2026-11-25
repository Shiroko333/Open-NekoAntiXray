package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.standard;

import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.SelectorComponent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.ParsingException;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.Emitable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.SerializableResolver;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.TokenEmitter;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;

final class SelectorTag {
   private static final String SEL = "sel";
   private static final String SELECTOR = "selector";
   static final TagResolver RESOLVER = SerializableResolver.claimingComponent(
      StandardTags.names("sel", "selector"), (var0, var1) -> create(var0, var1), var0 -> claim(var0)
   );

   private SelectorTag() {
   }

   static Tag create(ArgumentQueue var0, Context var1) throws ParsingException {
      String var2 = var0.popOr("A selection key is required").value();
      Component var3 = null;
      if (var0.hasNext()) {
         var3 = var1.deserialize(var0.pop().value());
      }

      return Tag.inserting(Component.selector(var2, var3));
   }

   @Nullable
   static Emitable claim(Component var0) {
      if (!(var0 instanceof SelectorComponent)) {
         return null;
      }

      SelectorComponent var1 = (SelectorComponent)var0;
      return var1x -> restoredLambda$claim$0(var1, var1x);
   }

   // $VF: synthetic method
   private static void restoredLambda$claim$0(SelectorComponent var0, TokenEmitter var1) {
      var1.tag("sel");
      var1.argument(var0.pattern());
      if (var0.separator() != null) {
         var1.argument(var0.separator());
      }
   }
}
