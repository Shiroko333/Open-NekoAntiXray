package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver;

import java.util.Map;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;

interface MappableResolver {
   boolean contributeToMap(@NotNull Map<String, Tag> var1);
}
