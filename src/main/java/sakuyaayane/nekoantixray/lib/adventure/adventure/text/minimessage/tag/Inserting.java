package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag;

import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;

public interface Inserting extends Tag {
   @NotNull
   Component value();

   default boolean allowsChildren() {
      return true;
   }
}
