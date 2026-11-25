package sakuyaayane.nekoantixray.lib.adventure.adventure.pointer;

import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.key.Key;
import sakuyaayane.nekoantixray.lib.adventure.examination.Examinable;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

public interface Pointer<V> extends Examinable {
   @NotNull
   static <V> Pointer<V> pointer(@NotNull Class<V> var0, @NotNull Key var1) {
      return new PointerImpl<>(var0, var1);
   }

   @NotNull
   Class<V> type();

   @NotNull
   Key key();

   @NotNull
   @Override
   default Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.of(ExaminableProperty.of("type", this.type()), ExaminableProperty.of("key", this.key()));
   }
}
