package sakuyaayane.nekoantixray.lib.folialib;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import sakuyaayane.nekoantixray.lib.folialib.enums.ImplementationType;
import sakuyaayane.nekoantixray.lib.folialib.impl.PlatformScheduler;
import sakuyaayane.nekoantixray.lib.folialib.util.InvalidTickDelayNotifier;

public class FoliaLib {
   private final Plugin plugin;
   private final ImplementationType implementationType;
   private final PlatformScheduler scheduler;

   public FoliaLib(Plugin var1) {
      this.plugin = var1;
      ImplementationType var2 = ImplementationType.UNKNOWN;

      for (ImplementationType var6 : ImplementationType.values()) {
         if (var6.selfCheck()) {
            var2 = var6;
            break;
         }
      }

      this.implementationType = var2;
      this.scheduler = this.createServerImpl(this.implementationType.getImplementationClassName());
      if (this.scheduler == null) {
         throw new IllegalStateException(
            "Failed to create server implementation. Please report this to the FoliaLib GitHub issues page. Forks of server software may not all be supported. If you are using an unofficial fork, please report this to the fork's developers first."
         );
      }

      String var7 = "com,tcoded,folialib,".replace(",", ".");
      if (this.getClass().getName().startsWith(var7)) {
         Logger var8 = this.plugin.getLogger();
         var8.severe("****************************************************************");
         var8.severe("FoliaLib is not relocated correctly! This will cause conflicts");
         var8.severe("with other plugins using FoliaLib. Please contact the developers");
         var8.severe(String.format("of '%s' and inform them of this issue immediately!", this.plugin.getDescription().getName()));
         var8.severe("****************************************************************");
      }
   }

   public ImplementationType getImplType() {
      return this.implementationType;
   }

   @Deprecated
   public PlatformScheduler getImpl() {
      return this.getScheduler();
   }

   public PlatformScheduler getScheduler() {
      return this.scheduler;
   }

   public boolean isFolia() {
      return this.implementationType == ImplementationType.FOLIA;
   }

   public boolean isPaper() {
      return this.implementationType == ImplementationType.PAPER || this.implementationType == ImplementationType.LEGACY_PAPER;
   }

   public boolean isSpigot() {
      return this.implementationType == ImplementationType.SPIGOT || this.implementationType == ImplementationType.LEGACY_SPIGOT;
   }

   public boolean isUnsupported() {
      return this.implementationType == ImplementationType.UNKNOWN;
   }

   public Plugin getPlugin() {
      return this.plugin;
   }

   public void disableInvalidTickValueWarning() {
      InvalidTickDelayNotifier.disableNotifications = true;
   }

   public void enableInvalidTickValueDebug() {
      InvalidTickDelayNotifier.debugMode = true;
   }

   private PlatformScheduler createServerImpl(String var1) {
      String var2 = this.getClass().getPackage().getName() + ".impl.";

      try {
         return (PlatformScheduler)Class.forName(var2 + var1).getConstructor(this.getClass()).newInstance(this);
      } catch (InstantiationException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException var4) {
         var4.printStackTrace();
         return null;
      }
   }
}
