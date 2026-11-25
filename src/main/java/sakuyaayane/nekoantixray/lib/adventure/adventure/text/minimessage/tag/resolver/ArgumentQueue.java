package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver;

import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;

@NonExtendable
public interface ArgumentQueue {
   Tag.@NotNull Argument pop();

   Tag.@NotNull Argument popOr(@NotNull String var1);

   Tag.@NotNull Argument popOr(@NotNull Supplier<String> var1);

   Tag.@Nullable Argument peek();

   boolean hasNext();

   void reset();
}
