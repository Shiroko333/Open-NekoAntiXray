package sakuyaayane.nekoantixray.lib.adventure.adventure.nbt.api;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.util.Codec;

final class BinaryTagHolderImpl implements BinaryTagHolder {
   private final String string;

   BinaryTagHolderImpl(String var1) {
      this.string = Objects.requireNonNull(var1, "string");
   }

   @NotNull
   @Override
   public String string() {
      return this.string;
   }

   @NotNull
   @Override
   public <T, DX extends Exception> T get(@NotNull Codec<T, String, DX, ?> var1) throws DX {
      return (T)var1.decode(this.string);
   }

   @Override
   public int hashCode() {
      return 31 * this.string.hashCode();
   }

   @Override
   public boolean equals(Object var1) {
      return !(var1 instanceof BinaryTagHolderImpl) ? false : this.string.equals(((BinaryTagHolderImpl)var1).string);
   }

   @Override
   public String toString() {
      return this.string;
   }
}
