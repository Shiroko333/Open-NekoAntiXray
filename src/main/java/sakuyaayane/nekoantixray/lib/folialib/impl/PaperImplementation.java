package sakuyaayane.nekoantixray.lib.folialib.impl;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import sakuyaayane.nekoantixray.lib.folialib.FoliaLib;
import sakuyaayane.nekoantixray.lib.folialib.util.ImplementationTestsUtil;

public class PaperImplementation extends SpigotImplementation {
   private Method teleportAsyncMethod;

   public PaperImplementation(FoliaLib var1) {
      super(var1);
      if (ImplementationTestsUtil.isAsyncTeleportSupported()) {
         try {
            this.teleportAsyncMethod = Entity.class.getMethod("teleportAsync", Location.class, TeleportCause.class);
         } catch (NoSuchMethodException var3) {
            throw new RuntimeException("Failed to initialize PaperImplementation", var3);
         }
      }
   }

   @Override
   public CompletableFuture<Boolean> teleportAsync(Entity var1, Location var2) {
      return this.teleportAsync(var1, var2, TeleportCause.PLUGIN);
   }

   @Override
   public CompletableFuture<Boolean> teleportAsync(Entity var1, Location var2, TeleportCause var3) {
      if (!ImplementationTestsUtil.isAsyncTeleportSupported()) {
         return super.teleportAsync(var1, var2, var3);
      }

      try {
         return (CompletableFuture<Boolean>)this.teleportAsyncMethod.invoke(var1, var2, var3);
      } catch (Exception var5) {
         var5.printStackTrace();
         return super.teleportAsync(var1, var2, var3);
      }
   }
}
