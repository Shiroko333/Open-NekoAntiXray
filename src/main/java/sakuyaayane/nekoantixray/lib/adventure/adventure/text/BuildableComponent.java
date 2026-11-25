package sakuyaayane.nekoantixray.lib.adventure.adventure.text;

import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.util.Buildable;

public interface BuildableComponent<C extends BuildableComponent<C, B>, B extends ComponentBuilder<C, B>> extends Buildable<C, B>, Component {
   @NotNull
   B toBuilder();
}
