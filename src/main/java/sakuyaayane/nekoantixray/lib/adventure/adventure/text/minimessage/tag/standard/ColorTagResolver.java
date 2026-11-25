package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.standard;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.NamedTextColor;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.TextColor;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.ParsingException;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.SerializableResolver;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.StyleClaim;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.TokenEmitter;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;

final class ColorTagResolver implements TagResolver, SerializableResolver.Single {
   private static final String COLOR_3 = "c";
   private static final String COLOR_2 = "colour";
   private static final String COLOR = "color";
   static final TagResolver INSTANCE = new ColorTagResolver();
   private static final StyleClaim<TextColor> STYLE = StyleClaim.claim("color", var0 -> var0.color(), (var0, var1) -> restoredLambda$static$0(var0, var1));
   private static final Map<String, TextColor> COLOR_ALIASES = new HashMap<>();

   private static boolean isColorOrAbbreviation(String var0) {
      return var0.equals("color") || var0.equals("colour") || var0.equals("c");
   }

   @Nullable
   @Override
   public Tag resolve(@NotNull String var1, @NotNull ArgumentQueue var2, @NotNull Context var3) throws ParsingException {
      if (!this.has(var1)) {
         return null;
      }

      String var4;
      if (isColorOrAbbreviation(var1)) {
         var4 = var2.popOr("Expected to find a color parameter: <name>|#RRGGBB").lowerValue();
      } else {
         var4 = var1;
      }

      TextColor var5 = resolveColor(var4, var3);
      return Tag.styling(var5);
   }

   @NotNull
   static TextColor resolveColor(@NotNull String var0, @NotNull Context var1) throws ParsingException {
      TextColor var2;
      if (COLOR_ALIASES.containsKey(var0)) {
         var2 = COLOR_ALIASES.get(var0);
      } else if (var0.charAt(0) == '#') {
         var2 = TextColor.fromHexString(var0);
      } else {
         var2 = NamedTextColor.NAMES.value(var0);
      }

      return var2;
   }

   @Override
   public boolean has(@NotNull String var1) {
      return isColorOrAbbreviation(var1)
         || TextColor.fromHexString(var1) != null
         || NamedTextColor.NAMES.value(var1) != null
         || COLOR_ALIASES.containsKey(var1);
   }

   @Nullable
   @Override
   public StyleClaim<?> claimStyle() {
      return STYLE;
   }

   // $VF: synthetic method
   private static void restoredLambda$static$0(TextColor var0, TokenEmitter var1) {
      if (var0 instanceof NamedTextColor) {
         var1.tag(NamedTextColor.NAMES.key((NamedTextColor)var0));
      } else {
         var1.tag(var0.asHexString());
      }
   }

   static {
      COLOR_ALIASES.put("dark_grey", NamedTextColor.DARK_GRAY);
      COLOR_ALIASES.put("grey", NamedTextColor.GRAY);
   }
}
