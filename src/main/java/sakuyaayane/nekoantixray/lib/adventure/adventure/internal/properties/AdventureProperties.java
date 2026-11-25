package sakuyaayane.nekoantixray.lib.adventure.adventure.internal.properties;

import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@Internal
public final class AdventureProperties {
   public static final AdventureProperties.Property<Boolean> DEBUG = property("debug", var0 -> Boolean.parseBoolean(var0), false);
   public static final AdventureProperties.Property<String> DEFAULT_TRANSLATION_LOCALE = property("defaultTranslationLocale", Function.identity(), null);
   public static final AdventureProperties.Property<Boolean> SERVICE_LOAD_FAILURES_ARE_FATAL = property(
      "serviceLoadFailuresAreFatal", var0 -> Boolean.parseBoolean(var0), Boolean.TRUE
   );
   public static final AdventureProperties.Property<Boolean> TEXT_WARN_WHEN_LEGACY_FORMATTING_DETECTED = property(
      "text.warnWhenLegacyFormattingDetected", var0 -> Boolean.parseBoolean(var0), Boolean.FALSE
   );

   private AdventureProperties() {
   }

   @NotNull
   public static <T> AdventureProperties.Property<T> property(@NotNull String var0, @NotNull Function<String, T> var1, @Nullable T var2) {
      return AdventurePropertiesImpl.property(var0, var1, (T)var2);
   }

   @Internal
   @NonExtendable
   public interface Property<T> {
      @Nullable
      T value();
   }
}
