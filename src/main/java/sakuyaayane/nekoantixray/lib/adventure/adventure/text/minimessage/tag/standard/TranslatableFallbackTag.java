package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.standard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.TranslatableComponent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.TranslationArgument;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.ParsingException;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.Emitable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.SerializableResolver;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.TokenEmitter;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;

final class TranslatableFallbackTag {
   private static final String TR_OR = "tr_or";
   private static final String TRANSLATE_OR = "translate_or";
   private static final String LANG_OR = "lang_or";
   static final TagResolver RESOLVER = SerializableResolver.claimingComponent(
      StandardTags.names("lang_or", "translate_or", "tr_or"), (var0, var1) -> create(var0, var1), var0 -> claim(var0)
   );

   private TranslatableFallbackTag() {
   }

   static Tag create(ArgumentQueue var0, Context var1) throws ParsingException {
      String var2 = var0.popOr("A translation key is required").value();
      String var3 = var0.popOr("A fallback messages is required").value();
      List var4;
      if (var0.hasNext()) {
         var4 = new ArrayList();

         while (var0.hasNext()) {
            var4.add(var1.deserialize(var0.pop().value()));
         }
      } else {
         var4 = Collections.emptyList();
      }

      return Tag.inserting(Component.translatable(var2, var3, var4));
   }

   @Nullable
   static Emitable claim(Component var0) {
      if (var0 instanceof TranslatableComponent && ((TranslatableComponent)var0).fallback() != null) {
         TranslatableComponent var1 = (TranslatableComponent)var0;
         return var1x -> restoredLambda$claim$0(var1, var1x);
      } else {
         return null;
      }
   }

   // $VF: synthetic method
   private static void restoredLambda$claim$0(TranslatableComponent var0, TokenEmitter var1) {
      var1.tag("lang_or");
      var1.argument(var0.key());
      var1.argument(var0.fallback());

      for (TranslationArgument var3 : var0.arguments()) {
         var1.argument(var3.asComponent());
      }
   }
}
