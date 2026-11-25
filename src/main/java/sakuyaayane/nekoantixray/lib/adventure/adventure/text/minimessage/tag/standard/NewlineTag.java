package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.standard;

import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.ParsingException;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.Emitable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.SerializableResolver;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.TokenEmitter;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;

final class NewlineTag {
   private static final String BR = "br";
   private static final String NEWLINE = "newline";
   static final TagResolver RESOLVER = SerializableResolver.claimingComponent(
      StandardTags.names("newline", "br"), (var0, var1) -> create(var0, var1), var0 -> claimComponent(var0)
   );

   private NewlineTag() {
   }

   static Tag create(ArgumentQueue var0, Context var1) throws ParsingException {
      return Tag.selfClosingInserting(Component.newline());
   }

   @Nullable
   static Emitable claimComponent(Component var0) {
      return Component.newline().equals(var0) ? var0x -> restoredLambda$claimComponent$0(var0x) : null;
   }

   // $VF: synthetic method
   private static void restoredLambda$claimComponent$0(TokenEmitter var0) {
      var0.selfClosingTag("br");
   }
}
