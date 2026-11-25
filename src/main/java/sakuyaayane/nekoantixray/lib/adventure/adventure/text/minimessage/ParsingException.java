package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public abstract class ParsingException extends RuntimeException {
   private static final long serialVersionUID = 4502774670340827070L;
   public static final int LOCATION_UNKNOWN = -1;

   protected ParsingException(@Nullable String var1) {
      super(var1);
   }

   protected ParsingException(@Nullable String var1, @Nullable Throwable var2) {
      super(var1, var2);
   }

   protected ParsingException(String var1, Throwable var2, boolean var3, boolean var4) {
      super(var1, var2, var3, var4);
   }

   @NotNull
   public abstract String originalText();

   @Nullable
   public abstract String detailMessage();

   public abstract int startIndex();

   public abstract int endIndex();
}
