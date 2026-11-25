package sakuyaayane.nekoantixray.lib.adventure.adventure.text.event;

import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.audience.Audience;
import sakuyaayane.nekoantixray.lib.adventure.adventure.permission.PermissionChecker;
import sakuyaayane.nekoantixray.lib.adventure.adventure.util.Services;
import sakuyaayane.nekoantixray.lib.adventure.adventure.util.TriState;

final class ClickCallbackInternals {
   static final PermissionChecker ALWAYS_FALSE = PermissionChecker.always(TriState.FALSE);
   static final ClickCallback.Provider PROVIDER = Services.service(ClickCallback.Provider.class).orElseGet(() -> new ClickCallbackInternals.Fallback());

   private ClickCallbackInternals() {
   }

   static final class Fallback implements ClickCallback.Provider {
      @NotNull
      @Override
      public ClickEvent create(@NotNull ClickCallback<Audience> var1, ClickCallback.@NotNull Options var2) {
         return ClickEvent.suggestCommand("Callbacks are not supported on this platform!");
      }
   }
}
