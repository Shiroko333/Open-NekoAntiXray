package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tree.Node;

@OverrideOnly
public interface Modifying extends Tag {
   default void visit(@NotNull Node var1, int var2) {
   }

   default void postVisit() {
   }

   Component apply(@NotNull Component var1, int var2);
}
