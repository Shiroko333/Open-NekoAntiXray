package sakuyaayane.nekoantixray.lib.adventure.adventure.resource;

import java.util.UUID;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.audience.Audience;

@FunctionalInterface
public interface ResourcePackCallback {
   @NotNull
   static ResourcePackCallback noOp() {
      return ResourcePackCallbacks.NO_OP;
   }

   @NotNull
   static ResourcePackCallback onTerminal(@NotNull BiConsumer<UUID, Audience> var0, @NotNull BiConsumer<UUID, Audience> var1) {
      return (var2, var3, var4) -> restoredLambda$onTerminal$0(var0, var1, var2, var3, var4);
   }

   void packEventReceived(@NotNull UUID var1, @NotNull ResourcePackStatus var2, @NotNull Audience var3);

   // $VF: synthetic method
   private static void restoredLambda$onTerminal$0(BiConsumer var0, BiConsumer var1, UUID var2, ResourcePackStatus var3, Audience var4) {
      if (var3 == ResourcePackStatus.SUCCESSFULLY_LOADED) {
         var0.accept(var2, var4);
      } else if (!var3.intermediate()) {
         var1.accept(var2, var4);
      }
   }
}
