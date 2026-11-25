package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.ComponentLike;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.Style;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.StyleBuilderApplicable;

public interface Tag {
   @NotNull
   static PreProcess preProcessParsed(@NotNull String var0) {
      return new PreProcessTagImpl(Objects.requireNonNull(var0, "content"));
   }

   @NotNull
   static Tag inserting(@NotNull Component var0) {
      return new InsertingImpl(true, Objects.requireNonNull(var0, "content must not be null"));
   }

   @NotNull
   static Tag inserting(@NotNull ComponentLike var0) {
      return inserting(Objects.requireNonNull(var0, "value").asComponent());
   }

   @NotNull
   static Tag selfClosingInserting(@NotNull Component var0) {
      return new InsertingImpl(false, Objects.requireNonNull(var0, "content must not be null"));
   }

   @NotNull
   static Tag selfClosingInserting(@NotNull ComponentLike var0) {
      return selfClosingInserting(Objects.requireNonNull(var0, "value").asComponent());
   }

   @NotNull
   static Tag styling(Consumer<Style.Builder> var0) {
      return new CallbackStylingTagImpl(var0);
   }

   @NotNull
   static Tag styling(@NotNull StyleBuilderApplicable... var0) {
      Objects.requireNonNull(var0, "actions");
      int var1 = 0;

      for (int var2 = var0.length; var1 < var2; var1++) {
         if (var0[var1] == null) {
            throw new NullPointerException("actions[" + var1 + "]");
         }
      }

      return new StylingTagImpl(Arrays.copyOf(var0, var0.length));
   }

   @NonExtendable
   interface Argument {
      @NotNull
      String value();

      @NotNull
      default String lowerValue() {
         return this.value().toLowerCase(Locale.ROOT);
      }

      default boolean isTrue() {
         return "true".equals(this.value()) || "on".equals(this.value());
      }

      default boolean isFalse() {
         return "false".equals(this.value()) || "off".equals(this.value());
      }

      @NotNull
      default OptionalInt asInt() {
         try {
            return OptionalInt.of(Integer.parseInt(this.value()));
         } catch (NumberFormatException var1) {
            return OptionalInt.empty();
         }
      }

      @NotNull
      default OptionalDouble asDouble() {
         try {
            return OptionalDouble.of(Double.parseDouble(this.value()));
         } catch (NumberFormatException var1) {
            return OptionalDouble.empty();
         }
      }
   }
}
