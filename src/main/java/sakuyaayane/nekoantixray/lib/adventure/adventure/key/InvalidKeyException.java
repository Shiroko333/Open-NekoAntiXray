package sakuyaayane.nekoantixray.lib.adventure.adventure.key;

import org.jspecify.annotations.Nullable;

public final class InvalidKeyException extends RuntimeException {
   private static final long serialVersionUID = -5413304087321449434L;
   private final String keyNamespace;
   private final String keyValue;

   InvalidKeyException(String var1, String var2, @Nullable String var3) {
      super(var3);
      this.keyNamespace = var1;
      this.keyValue = var2;
   }

   public String keyNamespace() {
      return this.keyNamespace;
   }

   public String keyValue() {
      return this.keyValue;
   }
}
