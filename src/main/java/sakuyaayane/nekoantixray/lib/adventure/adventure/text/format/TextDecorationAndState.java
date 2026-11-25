package sakuyaayane.nekoantixray.lib.adventure.adventure.text.format;

import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import sakuyaayane.nekoantixray.lib.adventure.examination.Examinable;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

@NonExtendable
public interface TextDecorationAndState extends Examinable, StyleBuilderApplicable {
   @NotNull
   TextDecoration decoration();

   TextDecoration.@NotNull State state();

   @Override
   default void styleApply(Style.@NotNull Builder var1) {
      var1.decoration(this.decoration(), this.state());
   }

   @NotNull
   @Override
   default Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.of(ExaminableProperty.of("decoration", this.decoration()), ExaminableProperty.of("state", this.state()));
   }
}
