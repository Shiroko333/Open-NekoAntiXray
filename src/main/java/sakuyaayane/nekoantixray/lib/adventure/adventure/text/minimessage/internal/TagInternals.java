package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.ApiStatus.Internal;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.TagPattern;

@Internal
public final class TagInternals {
   @RegExp
   public static final String TAG_NAME_REGEX = "[!?#]?[a-z0-9_-]*";
   private static final Pattern TAG_NAME_PATTERN = Pattern.compile("[!?#]?[a-z0-9_-]*");

   private TagInternals() {
   }

   public static void assertValidTagName(@TagPattern @NotNull String var0) {
      if (!TAG_NAME_PATTERN.matcher(Objects.requireNonNull(var0)).matches()) {
         throw new IllegalArgumentException("Tag name must match pattern " + TAG_NAME_PATTERN.pattern() + ", was " + var0);
      }
   }

   public static boolean sanitizeAndCheckValidTagName(@TagPattern @NotNull String var0) {
      return TAG_NAME_PATTERN.matcher(Objects.requireNonNull(var0).toLowerCase(Locale.ROOT)).matches();
   }

   public static void sanitizeAndAssertValidTagName(@TagPattern @NotNull String var0) {
      assertValidTagName(Objects.requireNonNull(var0).toLowerCase(Locale.ROOT));
   }
}
