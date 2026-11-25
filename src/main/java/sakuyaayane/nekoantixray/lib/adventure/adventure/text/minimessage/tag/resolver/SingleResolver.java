package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver;

import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;

final class SingleResolver implements TagResolver.Single, MappableResolver {
   private final String key;
   private final Tag tag;

   SingleResolver(String var1, Tag var2) {
      this.key = var1;
      this.tag = var2;
   }

   @NotNull
   @Override
   public String key() {
      return this.key;
   }

   @NotNull
   @Override
   public Tag tag() {
      return this.tag;
   }

   @Override
   public boolean contributeToMap(@NotNull Map<String, Tag> var1) {
      var1.put(this.key, this.tag);
      return true;
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.key, this.tag);
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      }

      if (var1 == null) {
         return false;
      }

      if (this.getClass() != var1.getClass()) {
         return false;
      }

      SingleResolver var2 = (SingleResolver)var1;
      return Objects.equals(this.key, var2.key) && Objects.equals(this.tag, var2.tag);
   }
}
