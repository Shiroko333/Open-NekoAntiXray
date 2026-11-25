package sakuyaayane.nekoantixray.lib.folialib.enums;

import java.util.function.Supplier;
import sakuyaayane.nekoantixray.lib.folialib.util.ImplementationTestsUtil;

public enum ImplementationType {
   FOLIA("FoliaImplementation", new Supplier[0], "io.papermc.paper.threadedregions.RegionizedServer"),
   PAPER(
      "PaperImplementation",
      new Supplier[]{() -> ImplementationTestsUtil.isTaskConsumersSupported()},
      "com.destroystokyo.paper.PaperConfig",
      "io.papermc.paper.configuration.Configuration"
   ),
   LEGACY_PAPER("LegacyPaperImplementation", new Supplier[0], "com.destroystokyo.paper.PaperConfig", "io.papermc.paper.configuration.Configuration"),
   SPIGOT("SpigotImplementation", new Supplier[]{() -> ImplementationTestsUtil.isTaskConsumersSupported()}, "org.spigotmc.SpigotConfig"),
   LEGACY_SPIGOT("LegacySpigotImplementation", new Supplier[0], "org.spigotmc.SpigotConfig"),
   UNKNOWN("UnsupportedImplementation", new Supplier[0]);

   private final String implementationClassName;
   private final Supplier<Boolean>[] tests;
   private final String[] classNames;

   ImplementationType(String var3, Supplier<Boolean>[] var4, String... var5) {
      this.implementationClassName = var3;
      this.tests = var4;
      this.classNames = var5;
   }

   public String getImplementationClassName() {
      return this.implementationClassName;
   }

   public Supplier<Boolean>[] getTests() {
      return this.tests;
   }

   public String[] getClassNames() {
      return this.classNames;
   }

   public boolean selfCheck() {
      for (Supplier var4 : this.getTests()) {
         if (!(Boolean)var4.get()) {
            return false;
         }
      }

      String[] var7 = this.getClassNames();

      for (String var5 : var7) {
         try {
            Class.forName(var5);
            return true;
         } catch (ClassNotFoundException var6) {
         }
      }

      return false;
   }

   // $VF: synthetic method
   private static ImplementationType[] $values() {
      return new ImplementationType[]{FOLIA, PAPER, LEGACY_PAPER, SPIGOT, LEGACY_SPIGOT, UNKNOWN};
   }
}
