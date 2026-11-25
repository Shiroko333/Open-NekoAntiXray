package sakuyaayane.nekoantixray.lib.adventure.adventure.text.serializer;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;

public interface ComponentEncoder<I extends Component, R> {
   @NotNull
   R serialize(@NotNull I var1);

   @Contract(value = "!null -> !null; null -> null", pure = true)
   @Nullable
   default R serializeOrNull(@Nullable I var1) {
      return this.serializeOr((I)var1, null);
   }

   @Contract(value = "!null, _ -> !null; null, _ -> param2", pure = true)
   @Nullable
   default R serializeOr(@Nullable I var1, @Nullable R var2) {
      return (R)(var1 == null ? var2 : this.serialize((I)var1));
   }
}
