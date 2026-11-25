package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.standard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.NamedTextColor;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.TextColor;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Inserting;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;
import sakuyaayane.nekoantixray.lib.adventure.examination.Examinable;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

public final class TransitionTag implements Inserting, Examinable {
   public static final String TRANSITION = "transition";
   private final TextColor[] colors;
   private final float phase;
   private final boolean negativePhase;
   static final TagResolver RESOLVER = TagResolver.resolver("transition", (var0, var1) -> create(var0, var1));

   static Tag create(ArgumentQueue var0, Context var1) {
      float var2 = 0.0F;
      List var3;
      if (!var0.hasNext()) {
         var3 = Collections.emptyList();
      } else {
         var3 = new ArrayList();

         while (var0.hasNext()) {
            Tag.Argument var4 = var0.pop();
            if (!var0.hasNext()) {
               OptionalDouble var5 = var4.asDouble();
               if (var5.isPresent()) {
                  var2 = (float)var5.getAsDouble();
                  if (var2 < -1.0F || var2 > 1.0F) {
                     throw var1.newException(String.format("Gradient phase is out of range (%s). Must be in the range [-1.0f, 1.0f] (inclusive).", var2), var0);
                  }

                  if (var3.size() < 2) {
                     throw var1.newException("Invalid transition, not enough colors. Transitions must have at least two colors.", var0);
                  }
                  break;
               }
            }

            String var7 = var4.value();
            TextColor var6;
            if (var7.charAt(0) == '#') {
               var6 = TextColor.fromHexString(var7);
            } else {
               var6 = NamedTextColor.NAMES.value(var4.lowerValue());
            }

            if (var6 == null) {
               throw var1.newException(String.format("Unable to parse a color from '%s'. Please use named colors or hex (#RRGGBB) colors.", var7), var0);
            }

            var3.add(var6);
         }
      }

      return new TransitionTag(var2, var3);
   }

   private TransitionTag(float var1, List<TextColor> var2) {
      if (var1 < 0.0F) {
         this.negativePhase = true;
         this.phase = 1.0F + var1;
         Collections.reverse(var2);
      } else {
         this.negativePhase = false;
         this.phase = var1;
      }

      if (var2.isEmpty()) {
         this.colors = new TextColor[]{TextColor.color(16777215), TextColor.color(0)};
      } else {
         this.colors = var2.toArray(new TextColor[0]);
      }
   }

   @NotNull
   @Override
   public Component value() {
      return Component.text("", this.color());
   }

   private TextColor color() {
      float var1 = 1.0F / (this.colors.length - 1);

      for (int var2 = 1; var2 < this.colors.length; var2++) {
         float var3 = var2 * var1;
         if (var3 >= this.phase) {
            float var4 = 1.0F + (this.phase - var3) * (this.colors.length - 1);
            if (this.negativePhase) {
               return TextColor.lerp(1.0F - var4, this.colors[var2], this.colors[var2 - 1]);
            }

            return TextColor.lerp(var4, this.colors[var2 - 1], this.colors[var2]);
         }
      }

      return this.colors[0];
   }

   @NotNull
   @Override
   public Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.of(ExaminableProperty.of("phase", this.phase), ExaminableProperty.of("colors", this.colors));
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         TransitionTag var2 = (TransitionTag)var1;
         return this.phase == var2.phase && Arrays.equals(this.colors, var2.colors);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int var1 = Objects.hash(this.phase);
      return 31 * var1 + Arrays.hashCode(this.colors);
   }
}
