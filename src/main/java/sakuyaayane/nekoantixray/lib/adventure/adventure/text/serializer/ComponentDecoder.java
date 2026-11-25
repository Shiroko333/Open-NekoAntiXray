package sakuyaayane.nekoantixray.lib.adventure.adventure.text.serializer;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;

public interface ComponentDecoder<S, O extends Component> {
   @NotNull
   O deserialize(@NotNull S var1);

   @Contract(value = "!null -> !null; null -> null", pure = true)
   @Nullable
   default O deserializeOrNull(@Nullable S var1) {
      return this.deserializeOr((S)var1, null);
   }

   @Contract(value = "!null, _ -> !null; null, _ -> param2", pure = true)
   @Nullable
   default O deserializeOr(@Nullable S var1, @Nullable O var2) {
      return (O)(var1 == null ? var2 : this.deserialize((S)var1));
   }
}
