package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver;

import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.ParsingException;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.ClaimConsumer;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.SerializableResolver;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;

final class SequentialTagResolver implements TagResolver, SerializableResolver {
   final TagResolver[] resolvers;

   SequentialTagResolver(@NotNull TagResolver[] var1) {
      this.resolvers = var1;
   }

   @Nullable
   @Override
   public Tag resolve(@NotNull String var1, @NotNull ArgumentQueue var2, @NotNull Context var3) throws ParsingException {
      ParsingException var4 = null;

      for (TagResolver var8 : this.resolvers) {
         try {
            Tag var9 = var8.resolve(var1, var2, var3);
            if (var9 != null) {
               return var9;
            }
         } catch (ParsingException var11) {
            var2.reset();
            if (var4 == null) {
               var4 = var11;
            } else {
               var4.addSuppressed(var11);
            }
         } catch (Exception var12) {
            var2.reset();
            ParsingException var10 = var3.newException("Exception thrown while parsing <" + var1 + ">", var12, var2);
            if (var4 == null) {
               var4 = var10;
            } else {
               var4.addSuppressed(var10);
            }
         }
      }

      if (var4 != null) {
         throw var4;
      } else {
         return null;
      }
   }

   @Override
   public boolean has(@NotNull String var1) {
      for (TagResolver var5 : this.resolvers) {
         if (var5.has(var1)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public void handle(@NotNull Component var1, @NotNull ClaimConsumer var2) {
      for (TagResolver var6 : this.resolvers) {
         if (var6 instanceof SerializableResolver) {
            ((SerializableResolver)var6).handle(var1, var2);
         }
      }
   }

   @Override
   public boolean equals(@Nullable Object var1) {
      if (var1 == this) {
         return true;
      }

      if (!(var1 instanceof SequentialTagResolver)) {
         return false;
      }

      SequentialTagResolver var2 = (SequentialTagResolver)var1;
      return Arrays.equals(this.resolvers, var2.resolvers);
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.resolvers);
   }
}
