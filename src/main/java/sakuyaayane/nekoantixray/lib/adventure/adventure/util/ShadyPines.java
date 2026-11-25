package sakuyaayane.nekoantixray.lib.adventure.adventure.util;

import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;

public final class ShadyPines {
   private ShadyPines() {
   }

   @Deprecated
   @SafeVarargs
   @ScheduledForRemoval(inVersion = "5.0.0")
   @NotNull
   public static <E extends Enum<E>> Set<E> enumSet(Class<E> var0, E @NotNull ... var1) {
      return MonkeyBars.enumSet(var0, (E[])var1);
   }

   public static boolean equals(double var0, double var2) {
      return Double.doubleToLongBits(var0) == Double.doubleToLongBits(var2);
   }

   public static boolean equals(float var0, float var1) {
      return Float.floatToIntBits(var0) == Float.floatToIntBits(var1);
   }
}
