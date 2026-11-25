package sakuyaayane.nekoantixray.lib.adventure.adventure.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;

public interface Codec<D, E, DX extends Throwable, EX extends Throwable> {
   @NotNull
   static <D, E, DX extends Throwable, EX extends Throwable> Codec<D, E, DX, EX> codec(
      @NotNull final Codec.Decoder<D, E, DX> var0, @NotNull final Codec.Encoder<D, E, EX> var1
   ) {
      return new Codec<D, E, DX, EX>() {
         @NotNull
         @Override
         public D decode(@NotNull E var1x) throws DX {
            return (D)var0.decode(var1x);
         }

         @NotNull
         @Override
         public E encode(@NotNull D var1x) throws EX {
            return (E)var1.encode(var1x);
         }
      };
   }

   @Deprecated
   @ScheduledForRemoval(inVersion = "5.0.0")
   @NotNull
   static <D, E, DX extends Throwable, EX extends Throwable> Codec<D, E, DX, EX> of(
      @NotNull final Codec.Decoder<D, E, DX> var0, @NotNull final Codec.Encoder<D, E, EX> var1
   ) {
      return new Codec<D, E, DX, EX>() {
         @NotNull
         @Override
         public D decode(@NotNull E var1x) throws DX {
            return (D)var0.decode(var1x);
         }

         @NotNull
         @Override
         public E encode(@NotNull D var1x) throws EX {
            return (E)var1.encode(var1x);
         }
      };
   }

   @NotNull
   D decode(@NotNull E var1) throws DX;

   @NotNull
   E encode(@NotNull D var1) throws EX;

   interface Decoder<D, E, X extends Throwable> {
      @NotNull
      D decode(@NotNull E var1) throws X;
   }

   interface Encoder<D, E, X extends Throwable> {
      @NotNull
      E encode(@NotNull D var1) throws X;
   }
}
