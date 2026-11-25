package sakuyaayane.nekoantixray.lib.adventure.adventure.text.flattener;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.builder.AbstractBuilder;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.util.Buildable;

public interface ComponentFlattener extends Buildable<ComponentFlattener, ComponentFlattener.Builder> {
   @NotNull
   static ComponentFlattener.Builder builder() {
      return new ComponentFlattenerImpl.BuilderImpl();
   }

   @NotNull
   static ComponentFlattener basic() {
      return ComponentFlattenerImpl.BASIC;
   }

   @NotNull
   static ComponentFlattener textOnly() {
      return ComponentFlattenerImpl.TEXT_ONLY;
   }

   void flatten(@NotNull Component var1, @NotNull FlattenerListener var2);

   interface Builder extends AbstractBuilder<ComponentFlattener>, Buildable.Builder<ComponentFlattener> {
      @NotNull
      <T extends Component> ComponentFlattener.Builder mapper(@NotNull Class<T> var1, @NotNull Function<T, String> var2);

      @NotNull
      <T extends Component> ComponentFlattener.Builder complexMapper(@NotNull Class<T> var1, @NotNull BiConsumer<T, Consumer<Component>> var2);

      @NotNull
      ComponentFlattener.Builder unknownMapper(@Nullable Function<Component, String> var1);
   }
}
