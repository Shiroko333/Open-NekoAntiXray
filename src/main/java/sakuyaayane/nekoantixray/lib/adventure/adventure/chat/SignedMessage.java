package sakuyaayane.nekoantixray.lib.adventure.adventure.chat;

import java.time.Instant;
import java.util.stream.Stream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.identity.Identified;
import sakuyaayane.nekoantixray.lib.adventure.adventure.identity.Identity;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.ComponentLike;
import sakuyaayane.nekoantixray.lib.adventure.examination.Examinable;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

@NonExtendable
public interface SignedMessage extends Identified, Examinable {
   @Contract(value = "_ -> new", pure = true)
   @NotNull
   static SignedMessage.Signature signature(byte[] var0) {
      return new SignedMessageImpl.SignatureImpl(var0);
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static SignedMessage system(@NotNull String var0, @Nullable ComponentLike var1) {
      return new SignedMessageImpl(var0, ComponentLike.unbox(var1));
   }

   @Contract(pure = true)
   @NotNull
   Instant timestamp();

   @Contract(pure = true)
   long salt();

   @Contract(pure = true)
   @Nullable
   SignedMessage.Signature signature();

   @Contract(pure = true)
   @Nullable
   Component unsignedContent();

   @Contract(pure = true)
   @NotNull
   String message();

   @Contract(pure = true)
   default boolean isSystem() {
      return this.identity() == Identity.nil();
   }

   @Contract(pure = true)
   default boolean canDelete() {
      return this.signature() != null;
   }

   @NotNull
   @Override
   default Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.of(
         ExaminableProperty.of("timestamp", this.timestamp()),
         ExaminableProperty.of("salt", this.salt()),
         ExaminableProperty.of("signature", this.signature()),
         ExaminableProperty.of("unsignedContent", this.unsignedContent()),
         ExaminableProperty.of("message", this.message())
      );
   }

   @NonExtendable
   interface Signature extends Examinable {
      @Contract(pure = true)
      byte[] bytes();

      @NotNull
      @Override
      default Stream<? extends ExaminableProperty> examinableProperties() {
         return Stream.of(ExaminableProperty.of("bytes", this.bytes()));
      }
   }
}
