package sakuyaayane.nekoantixray.lib.adventure.adventure.text.renderer;

import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;

public interface ComponentRenderer<C> {
   @NotNull
   Component render(@NotNull Component var1, @NotNull C var2);

   default <T> ComponentRenderer<T> mapContext(Function<T, C> var1) {
      return (var2, var3) -> this.restoredLambda$mapContext$0(var1, var2, var3);
   }

   // $VF: synthetic method
   private Component restoredLambda$mapContext$0(Function var1, Component var2, Object var3) {
      return this.render(var2, (C)var1.apply(var3));
   }
}
