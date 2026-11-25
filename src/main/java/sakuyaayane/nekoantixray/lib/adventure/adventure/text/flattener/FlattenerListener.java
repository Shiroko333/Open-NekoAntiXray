package sakuyaayane.nekoantixray.lib.adventure.adventure.text.flattener;

import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.Style;

@FunctionalInterface
public interface FlattenerListener {
   default void pushStyle(@NotNull Style var1) {
   }

   void component(@NotNull String var1);

   default boolean shouldContinue() {
      return true;
   }

   default void popStyle(@NotNull Style var1) {
   }
}
