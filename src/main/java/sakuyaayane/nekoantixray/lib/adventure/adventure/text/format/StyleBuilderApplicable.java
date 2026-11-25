package sakuyaayane.nekoantixray.lib.adventure.adventure.text.format;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.ComponentBuilder;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.ComponentBuilderApplicable;

@FunctionalInterface
public interface StyleBuilderApplicable extends ComponentBuilderApplicable {
   @Contract(mutates = "param")
   void styleApply(Style.@NotNull Builder var1);

   @Override
   default void componentBuilderApply(@NotNull ComponentBuilder<?, ?> var1) {
      var1.style(var1x -> this.styleApply(var1x));
   }
}
