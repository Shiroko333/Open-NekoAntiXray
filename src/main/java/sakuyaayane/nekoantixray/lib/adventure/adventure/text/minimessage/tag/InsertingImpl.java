package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag;

import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

final class InsertingImpl extends AbstractTag implements Inserting {
   private final boolean allowsChildren;
   private final Component value;

   InsertingImpl(boolean var1, Component var2) {
      this.allowsChildren = var1;
      this.value = var2;
   }

   @Override
   public boolean allowsChildren() {
      return this.allowsChildren;
   }

   @NotNull
   @Override
   public Component value() {
      return this.value;
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.allowsChildren, this.value);
   }

   @Override
   public boolean equals(@Nullable Object var1) {
      if (this == var1) {
         return true;
      }

      if (!(var1 instanceof InsertingImpl)) {
         return false;
      }

      InsertingImpl var2 = (InsertingImpl)var1;
      return this.allowsChildren == var2.allowsChildren && Objects.equals(this.value, var2.value);
   }

   @NotNull
   @Override
   public Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.of(ExaminableProperty.of("allowsChildren", this.allowsChildren), ExaminableProperty.of("value", this.value));
   }
}
