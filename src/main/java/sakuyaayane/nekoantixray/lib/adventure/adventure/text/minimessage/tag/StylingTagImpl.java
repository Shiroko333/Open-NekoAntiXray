package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag;

import java.util.Arrays;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.Style;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.StyleBuilderApplicable;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

final class StylingTagImpl extends AbstractTag implements Inserting {
   private final StyleBuilderApplicable[] styles;

   StylingTagImpl(StyleBuilderApplicable[] var1) {
      this.styles = var1;
   }

   @NotNull
   @Override
   public Component value() {
      return Component.text("", Style.style(this.styles));
   }

   @Override
   public int hashCode() {
      return 31 + Arrays.hashCode(this.styles);
   }

   @Override
   public boolean equals(@Nullable Object var1) {
      if (this == var1) {
         return true;
      }

      if (!(var1 instanceof StylingTagImpl)) {
         return false;
      }

      StylingTagImpl var2 = (StylingTagImpl)var1;
      return Arrays.equals(this.styles, var2.styles);
   }

   @NotNull
   @Override
   public Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.of(ExaminableProperty.of("styles", this.styles));
   }
}
