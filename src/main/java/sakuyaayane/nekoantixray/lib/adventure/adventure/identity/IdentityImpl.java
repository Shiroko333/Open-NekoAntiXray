package sakuyaayane.nekoantixray.lib.adventure.adventure.identity;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.internal.Internals;
import sakuyaayane.nekoantixray.lib.adventure.examination.Examinable;

final class IdentityImpl implements Examinable, Identity {
   private final UUID uuid;

   IdentityImpl(UUID var1) {
      this.uuid = var1;
   }

   @NotNull
   @Override
   public UUID uuid() {
      return this.uuid;
   }

   @Override
   public String toString() {
      return Internals.toString(this);
   }

   @Override
   public boolean equals(@Nullable Object var1) {
      if (this == var1) {
         return true;
      }

      if (!(var1 instanceof Identity)) {
         return false;
      }

      Identity var2 = (Identity)var1;
      return this.uuid.equals(var2.uuid());
   }

   @Override
   public int hashCode() {
      return this.uuid.hashCode();
   }
}
