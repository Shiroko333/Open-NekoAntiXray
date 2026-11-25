package sakuyaayane.nekoantixray.lib.adventure.adventure.text;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.event.ClickEvent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.event.HoverEventSource;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.Style;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.TextColor;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.TextDecoration;

public interface ScopedComponent<C extends Component> extends Component {
   @NotNull
   @Override
   C children(@NotNull List<? extends ComponentLike> var1);

   @NotNull
   @Override
   C style(@NotNull Style var1);

   @NotNull
   @Override
   default C style(@NotNull Consumer<Style.Builder> var1) {
      return (C)Component.super.style(var1);
   }

   @NotNull
   @Override
   default C style(Style.@NotNull Builder var1) {
      return (C)Component.super.style(var1);
   }

   @NotNull
   @Override
   default C mergeStyle(@NotNull Component var1) {
      return (C)Component.super.mergeStyle(var1);
   }

   @NotNull
   @Override
   default C mergeStyle(@NotNull Component var1, Style.@NotNull Merge @NotNull ... var2) {
      return (C)Component.super.mergeStyle(var1, var2);
   }

   @NotNull
   @Override
   default C append(@NotNull Component var1) {
      return (C)Component.super.append(var1);
   }

   @NotNull
   @Override
   default C append(@NotNull ComponentLike var1) {
      return (C)Component.super.append(var1);
   }

   @NotNull
   @Override
   default C append(@NotNull ComponentBuilder<?, ?> var1) {
      return (C)Component.super.append(var1);
   }

   @NotNull
   @Override
   default C mergeStyle(@NotNull Component var1, @NotNull Set<Style.Merge> var2) {
      return (C)Component.super.mergeStyle(var1, var2);
   }

   @NotNull
   @Override
   default C color(@Nullable TextColor var1) {
      return (C)Component.super.color(var1);
   }

   @NotNull
   @Override
   default C colorIfAbsent(@Nullable TextColor var1) {
      return (C)Component.super.colorIfAbsent(var1);
   }

   @NotNull
   @Override
   default C decorate(@NotNull TextDecoration var1) {
      return (C)Component.super.decorate(var1);
   }

   @NotNull
   @Override
   default C decoration(@NotNull TextDecoration var1, boolean var2) {
      return (C)Component.super.decoration(var1, var2);
   }

   @NotNull
   @Override
   default C decoration(@NotNull TextDecoration var1, TextDecoration.@NotNull State var2) {
      return (C)Component.super.decoration(var1, var2);
   }

   @NotNull
   @Override
   default C clickEvent(@Nullable ClickEvent var1) {
      return (C)Component.super.clickEvent(var1);
   }

   @NotNull
   @Override
   default C hoverEvent(@Nullable HoverEventSource<?> var1) {
      return (C)Component.super.hoverEvent(var1);
   }

   @NotNull
   @Override
   default C insertion(@Nullable String var1) {
      return (C)Component.super.insertion(var1);
   }
}
