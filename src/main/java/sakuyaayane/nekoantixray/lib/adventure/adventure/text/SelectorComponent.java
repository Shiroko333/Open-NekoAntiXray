package sakuyaayane.nekoantixray.lib.adventure.adventure.text;

import java.util.stream.Stream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

public interface SelectorComponent extends BuildableComponent<SelectorComponent, SelectorComponent.Builder>, ScopedComponent<SelectorComponent> {
   @NotNull
   String pattern();

   @Contract(pure = true)
   @NotNull
   SelectorComponent pattern(@NotNull String var1);

   @Nullable
   Component separator();

   @NotNull
   SelectorComponent separator(@Nullable ComponentLike var1);

   @NotNull
   @Override
   default Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.concat(
         Stream.of(ExaminableProperty.of("pattern", this.pattern()), ExaminableProperty.of("separator", this.separator())),
         BuildableComponent.super.examinableProperties()
      );
   }

   interface Builder extends ComponentBuilder<SelectorComponent, SelectorComponent.Builder> {
      @Contract("_ -> this")
      @NotNull
      SelectorComponent.Builder pattern(@NotNull String var1);

      @Contract("_ -> this")
      @NotNull
      SelectorComponent.Builder separator(@Nullable ComponentLike var1);
   }
}
