package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.Style;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

final class CallbackStylingTagImpl extends AbstractTag implements Inserting {
   private final Consumer<Style.Builder> styles;

   CallbackStylingTagImpl(Consumer<Style.Builder> var1) {
      this.styles = var1;
   }

   @NotNull
   @Override
   public Component value() {
      return Component.text("", Style.style(this.styles));
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.styles);
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      }

      if (!(var1 instanceof CallbackStylingTagImpl)) {
         return false;
      }

      CallbackStylingTagImpl var2 = (CallbackStylingTagImpl)var1;
      return Objects.equals(this.styles, var2.styles);
   }

   @NotNull
   @Override
   public Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.of(ExaminableProperty.of("styles", this.styles));
   }
}
