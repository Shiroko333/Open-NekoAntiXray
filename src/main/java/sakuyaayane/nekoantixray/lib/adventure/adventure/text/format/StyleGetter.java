package sakuyaayane.nekoantixray.lib.adventure.adventure.text.format;

import java.util.EnumMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.key.Key;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.event.ClickEvent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.event.HoverEvent;

@NonExtendable
public interface StyleGetter {
   @Nullable
   Key font();

   @Nullable
   TextColor color();

   default boolean hasDecoration(@NotNull TextDecoration var1) {
      return this.decoration(var1) == TextDecoration.State.TRUE;
   }

   TextDecoration.@NotNull State decoration(@NotNull TextDecoration var1);

   @NotNull
   default @Unmodifiable Map<TextDecoration, TextDecoration.State> decorations() {
      EnumMap var1 = new EnumMap<>(TextDecoration.class);
      int var2 = 0;

      for (int var3 = DecorationMap.DECORATIONS.length; var2 < var3; var2++) {
         TextDecoration var4 = DecorationMap.DECORATIONS[var2];
         TextDecoration.State var5 = this.decoration(var4);
         var1.put(var4, var5);
      }

      return var1;
   }

   @Nullable
   ClickEvent clickEvent();

   @Nullable
   HoverEvent<?> hoverEvent();

   @Nullable
   String insertion();
}
