package sakuyaayane.nekoantixray.lib.adventure.adventure.text;

import java.util.stream.Stream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

public interface EntityNBTComponent extends NBTComponent<EntityNBTComponent, EntityNBTComponent.Builder>, ScopedComponent<EntityNBTComponent> {
   @NotNull
   String selector();

   @Contract(pure = true)
   @NotNull
   EntityNBTComponent selector(@NotNull String var1);

   @NotNull
   @Override
   default Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.concat(Stream.of(ExaminableProperty.of("selector", this.selector())), NBTComponent.super.examinableProperties());
   }

   interface Builder extends NBTComponentBuilder<EntityNBTComponent, EntityNBTComponent.Builder> {
      @Contract("_ -> this")
      @NotNull
      EntityNBTComponent.Builder selector(@NotNull String var1);
   }
}
