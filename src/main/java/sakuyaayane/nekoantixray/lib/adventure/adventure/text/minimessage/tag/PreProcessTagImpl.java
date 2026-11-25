package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag;

import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

final class PreProcessTagImpl extends AbstractTag implements PreProcess {
   private final String value;

   PreProcessTagImpl(String var1) {
      this.value = var1;
   }

   @NotNull
   @Override
   public String value() {
      return this.value;
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.value);
   }

   @Override
   public boolean equals(@Nullable Object var1) {
      if (this == var1) {
         return true;
      }

      if (!(var1 instanceof PreProcessTagImpl)) {
         return false;
      }

      PreProcessTagImpl var2 = (PreProcessTagImpl)var1;
      return Objects.equals(this.value, var2.value);
   }

   @NotNull
   @Override
   public Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.of(ExaminableProperty.of("value", this.value));
   }
}
