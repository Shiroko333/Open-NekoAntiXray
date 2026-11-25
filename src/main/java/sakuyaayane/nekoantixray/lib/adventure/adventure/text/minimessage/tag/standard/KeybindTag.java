package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.standard;

import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.KeybindComponent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.ParsingException;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.Emitable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.SerializableResolver;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.TokenEmitter;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;

final class KeybindTag {
   public static final String KEYBIND = "key";
   static final TagResolver RESOLVER = SerializableResolver.claimingComponent("key", (var0, var1) -> create(var0, var1), var0 -> emit(var0));

   private KeybindTag() {
   }

   static Tag create(ArgumentQueue var0, Context var1) throws ParsingException {
      return Tag.inserting(Component.keybind(var0.popOr("A keybind id is required").value()));
   }

   @Nullable
   static Emitable emit(Component var0) {
      if (!(var0 instanceof KeybindComponent)) {
         return null;
      }

      String var1 = ((KeybindComponent)var0).keybind();
      return var1x -> restoredLambda$emit$0(var1, var1x);
   }

   // $VF: synthetic method
   private static void restoredLambda$emit$0(String var0, TokenEmitter var1) {
      var1.tag("key").argument(var0);
   }
}
