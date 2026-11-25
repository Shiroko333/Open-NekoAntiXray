package sakuyaayane.nekoantixray.lib.adventure.adventure.bossbar;

import java.util.Collections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface BossBarImplementation {
   @Internal
   @NotNull
   static <I extends BossBarImplementation> I get(@NotNull BossBar var0, @NotNull Class<I> var1) {
      return BossBarImpl.ImplementationAccessor.get(var0, var1);
   }

   @Internal
   @NotNull
   default Iterable<? extends BossBarViewer> viewers() {
      return Collections.emptyList();
   }

   @Internal
   interface Provider {
      @Internal
      @NotNull
      BossBarImplementation create(@NotNull BossBar var1);
   }
}
