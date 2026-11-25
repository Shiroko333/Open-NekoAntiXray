package sakuyaayane.nekoantixray.lib.adventure.adventure.text;

import java.util.stream.Stream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

public interface NBTComponent<C extends NBTComponent<C, B>, B extends NBTComponentBuilder<C, B>> extends BuildableComponent<C, B> {
   @NotNull
   String nbtPath();

   @Contract(pure = true)
   @NotNull
   C nbtPath(@NotNull String var1);

   boolean interpret();

   @Contract(pure = true)
   @NotNull
   C interpret(boolean var1);

   @Nullable
   Component separator();

   @NotNull
   C separator(@Nullable ComponentLike var1);

   @NotNull
   @Override
   default Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.concat(
         Stream.of(
            ExaminableProperty.of("nbtPath", this.nbtPath()),
            ExaminableProperty.of("interpret", this.interpret()),
            ExaminableProperty.of("separator", this.separator())
         ),
         BuildableComponent.super.examinableProperties()
      );
   }
}
