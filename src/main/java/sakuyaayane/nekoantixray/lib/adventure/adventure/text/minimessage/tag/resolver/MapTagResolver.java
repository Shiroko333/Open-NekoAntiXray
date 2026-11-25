package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver;

import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;

final class MapTagResolver implements TagResolver.WithoutArguments, MappableResolver {
   private final Map<String, ? extends Tag> tagMap;

   MapTagResolver(@NotNull Map<String, ? extends Tag> var1) {
      this.tagMap = var1;
   }

   @Nullable
   @Override
   public Tag resolve(@NotNull String var1) {
      return this.tagMap.get(var1);
   }

   @Override
   public boolean contributeToMap(@NotNull Map<String, Tag> var1) {
      var1.putAll(this.tagMap);
      return true;
   }

   @Override
   public boolean equals(@Nullable Object var1) {
      if (this == var1) {
         return true;
      }

      if (!(var1 instanceof MapTagResolver)) {
         return false;
      }

      MapTagResolver var2 = (MapTagResolver)var1;
      return Objects.equals(this.tagMap, var2.tagMap);
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.tagMap);
   }
}
