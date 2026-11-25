package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.standard;

import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.TextColor;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;
import sakuyaayane.nekoantixray.lib.adventure.adventure.util.HSVLike;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

final class RainbowTag extends AbstractColorChangingTag {
   private static final String REVERSE = "!";
   private static final String RAINBOW = "rainbow";
   static final TagResolver RESOLVER = TagResolver.resolver("rainbow", (var0, var1) -> create(var0, var1));
   private final boolean reversed;
   private final double dividedPhase;
   private int colorIndex = 0;

   static Tag create(ArgumentQueue var0, Context var1) {
      boolean var2 = false;
      int var3 = 0;
      if (var0.hasNext()) {
         String var4 = var0.pop().value();
         if (var4.startsWith("!")) {
            var2 = true;
            var4 = var4.substring(1);
         }

         if (var4.length() > 0) {
            try {
               var3 = Integer.parseInt(var4);
            } catch (NumberFormatException var5) {
               throw var1.newException("Expected phase, got " + var4);
            }
         }
      }

      return new RainbowTag(var2, var3);
   }

   private RainbowTag(boolean var1, int var2) {
      this.reversed = var1;
      this.dividedPhase = var2 / 10.0;
   }

   @Override
   protected void init() {
      if (this.reversed) {
         this.colorIndex = this.size() - 1;
      }
   }

   @Override
   protected void advanceColor() {
      if (this.reversed) {
         if (this.colorIndex == 0) {
            this.colorIndex = this.size() - 1;
         } else {
            this.colorIndex--;
         }
      } else {
         this.colorIndex++;
      }
   }

   @Override
   protected TextColor color() {
      float var1 = this.colorIndex;
      float var2 = (float)((var1 / this.size() + this.dividedPhase) % 1.0);
      return TextColor.color(HSVLike.hsvLike(var2, 1.0F, 1.0F));
   }

   @NotNull
   @Override
   public Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.of(ExaminableProperty.of("phase", this.dividedPhase));
   }

   @Override
   public boolean equals(@Nullable Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         RainbowTag var2 = (RainbowTag)var1;
         return this.colorIndex == var2.colorIndex && this.dividedPhase == var2.dividedPhase;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.colorIndex, this.dividedPhase);
   }
}
