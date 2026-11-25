package sakuyaayane.nekoantixray.lib.adventure.adventure.nbt.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.event.DataComponentValue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.util.Codec;

public interface BinaryTagHolder extends DataComponentValue.TagSerializable {
   @NotNull
   static <T, EX extends Exception> BinaryTagHolder encode(@NotNull T var0, @NotNull Codec<? super T, String, ?, EX> var1) throws EX {
      return new BinaryTagHolderImpl((String)var1.encode(var0));
   }

   @NotNull
   static BinaryTagHolder binaryTagHolder(@NotNull String var0) {
      return new BinaryTagHolderImpl(var0);
   }

   @Deprecated
   @ScheduledForRemoval(inVersion = "5.0.0")
   @NotNull
   static BinaryTagHolder of(@NotNull String var0) {
      return new BinaryTagHolderImpl(var0);
   }

   @NotNull
   String string();

   @NotNull
   @Override
   default BinaryTagHolder asBinaryTag() {
      return this;
   }

   @NotNull
   <T, DX extends Exception> T get(@NotNull Codec<T, String, DX, ?> var1) throws DX;
}
