package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.standard;

import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.ScoreComponent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.ParsingException;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.Emitable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.SerializableResolver;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.TokenEmitter;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;

final class ScoreTag {
   public static final String SCORE = "score";
   static final TagResolver RESOLVER = SerializableResolver.claimingComponent("score", (var0, var1) -> create(var0, var1), var0 -> emit(var0));

   private ScoreTag() {
   }

   static Tag create(ArgumentQueue var0, Context var1) throws ParsingException {
      String var2 = var0.popOr("A scoreboard member name is required").value();
      String var3 = var0.popOr("An objective name is required").value();
      return Tag.inserting(Component.score(var2, var3));
   }

   @Nullable
   static Emitable emit(Component var0) {
      if (!(var0 instanceof ScoreComponent)) {
         return null;
      }

      ScoreComponent var1 = (ScoreComponent)var0;
      return var1x -> restoredLambda$emit$0(var1, var1x);
   }

   // $VF: synthetic method
   private static void restoredLambda$emit$0(ScoreComponent var0, TokenEmitter var1) {
      var1.tag("score").argument(var0.name()).argument(var0.objective());
   }
}
