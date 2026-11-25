package cn.nekosoft.dependency;

public final class RelocationConfig {
   private final boolean enable;
   private final boolean relocateGroup;
   private final String[] relocator;

   public RelocationConfig(boolean enable, boolean relocateGroup, String... relocator) {
      this.enable = enable;
      this.relocateGroup = relocateGroup;
      this.relocator = relocator;
   }

   public RelocationConfig(boolean enable) {
      this(enable, true);
   }

   public boolean isEnable() {
      return this.enable;
   }

   public boolean isRelocateGroup() {
      return this.relocateGroup;
   }

   public String[] getRelocator() {
      return this.relocator;
   }
}
