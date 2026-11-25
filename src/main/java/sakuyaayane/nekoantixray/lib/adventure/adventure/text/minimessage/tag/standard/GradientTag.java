package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.standard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.TextColor;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

final class GradientTag extends AbstractColorChangingTag {
   private static final String GRADIENT = "gradient";
   static final TagResolver RESOLVER = TagResolver.resolver("gradient", (var0, var1) -> create(var0, var1));
   private int index = 0;
   private double multiplier = 1.0;
   private final TextColor[] colors;
   private @Range(from = -1L, to = 1L) double phase;

   static Tag create(ArgumentQueue var0, Context var1) {
      double var2 = 0.0;
      List var4;
      if (!var0.hasNext()) {
         var4 = Collections.emptyList();
      } else {
         var4 = new ArrayList();

         while (var0.hasNext()) {
            Tag.Argument var5 = var0.pop();
            if (!var0.hasNext()) {
               OptionalDouble var6 = var5.asDouble();
               if (var6.isPresent()) {
                  var2 = var6.getAsDouble();
                  if (var2 < -1.0 || var2 > 1.0) {
                     throw var1.newException(String.format("Gradient phase is out of range (%s). Must be in the range [-1.0, 1.0] (inclusive).", var2), var0);
                  }

                  if (var4.size() == 1) {
                     throw var1.newException("Invalid gradient, not enough colors. Gradients must have at least two colors.", var0);
                  }
                  break;
               }
            }

            TextColor var7 = ColorTagResolver.resolveColor(var5.value(), var1);
            var4.add(var7);
         }
      }

      return new GradientTag(var2, var4);
   }

   private GradientTag(double var1, List<TextColor> var3) {
      if (var3.isEmpty()) {
         this.colors = new TextColor[]{TextColor.color(16777215), TextColor.color(0)};
      } else {
         this.colors = var3.toArray(new TextColor[0]);
      }

      if (var1 < 0.0) {
         this.phase = 1.0 + var1;
         Collections.reverse(Arrays.asList(this.colors));
      } else {
         this.phase = var1;
      }
   }

   @Override
   protected void init() {
      this.multiplier = this.size() == 1 ? 0.0 : (double)(this.colors.length - 1) / (this.size() - 1);
      this.phase = this.phase * (this.colors.length - 1);
      this.index = 0;
   }

   @Override
   protected void advanceColor() {
      this.index++;
   }

   @Override
   protected TextColor color() {
      double var1 = this.index * this.multiplier + this.phase;
      int var3 = (int)Math.floor(var1);
      int var4 = (int)Math.ceil(var1) % this.colors.length;
      int var5 = var3 % this.colors.length;
      return TextColor.lerp((float)var1 - var3, this.colors[var5], this.colors[var4]);
   }

   @NotNull
   @Override
   public Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.of(ExaminableProperty.of("phase", this.phase), ExaminableProperty.of("colors", this.colors));
   }

   @Override
   public boolean equals(@Nullable Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         GradientTag var2 = (GradientTag)var1;
         return this.index == var2.index && this.phase == var2.phase && Arrays.equals(this.colors, var2.colors);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int var1 = Objects.hash(this.index, this.phase);
      return 31 * var1 + Arrays.hashCode(this.colors);
   }
}
