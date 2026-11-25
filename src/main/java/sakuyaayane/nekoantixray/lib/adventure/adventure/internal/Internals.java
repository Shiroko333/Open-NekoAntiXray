package sakuyaayane.nekoantixray.lib.adventure.adventure.internal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.ApiStatus.Internal;
import sakuyaayane.nekoantixray.lib.adventure.examination.Examinable;
import sakuyaayane.nekoantixray.lib.adventure.examination.string.StringExaminer;

@Internal
public final class Internals {
   private Internals() {
   }

   @NotNull
   public static String toString(@NotNull Examinable var0) {
      return var0.examine(StringExaminer.simpleEscaping());
   }
}
