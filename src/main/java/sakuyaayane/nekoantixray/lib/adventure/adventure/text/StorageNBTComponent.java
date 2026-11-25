package sakuyaayane.nekoantixray.lib.adventure.adventure.text;

import java.util.stream.Stream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.key.Key;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

public interface StorageNBTComponent extends NBTComponent<StorageNBTComponent, StorageNBTComponent.Builder>, ScopedComponent<StorageNBTComponent> {
   @NotNull
   Key storage();

   @Contract(pure = true)
   @NotNull
   StorageNBTComponent storage(@NotNull Key var1);

   @NotNull
   @Override
   default Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.concat(Stream.of(ExaminableProperty.of("storage", this.storage())), NBTComponent.super.examinableProperties());
   }

   interface Builder extends NBTComponentBuilder<StorageNBTComponent, StorageNBTComponent.Builder> {
      @Contract("_ -> this")
      @NotNull
      StorageNBTComponent.Builder storage(@NotNull Key var1);
   }
}
