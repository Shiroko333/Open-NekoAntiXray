package sakuyaayane.nekoantixray.lib.adventure.adventure.text;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.internal.Internals;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

final class TextReplacementConfigImpl implements TextReplacementConfig {
   private final Pattern matchPattern;
   private final BiFunction<MatchResult, TextComponent.Builder, @Nullable ComponentLike> replacement;
   private final TextReplacementConfig.Condition continuer;

   TextReplacementConfigImpl(TextReplacementConfigImpl.Builder var1) {
      this.matchPattern = var1.matchPattern;
      this.replacement = var1.replacement;
      this.continuer = var1.continuer;
   }

   @NotNull
   @Override
   public Pattern matchPattern() {
      return this.matchPattern;
   }

   TextReplacementRenderer.State createState() {
      return new TextReplacementRenderer.State(this.matchPattern, this.replacement, this.continuer);
   }

   public TextReplacementConfig.@NotNull Builder toBuilder() {
      return new TextReplacementConfigImpl.Builder(this);
   }

   @NotNull
   @Override
   public Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.of(
         ExaminableProperty.of("matchPattern", this.matchPattern),
         ExaminableProperty.of("replacement", this.replacement),
         ExaminableProperty.of("continuer", this.continuer)
      );
   }

   @Override
   public String toString() {
      return Internals.toString(this);
   }

   static final class Builder implements TextReplacementConfig.Builder {
      @Nullable
      Pattern matchPattern;
      @Nullable
      BiFunction<MatchResult, TextComponent.Builder, ComponentLike> replacement;
      TextReplacementConfig.Condition continuer;

      Builder() {
         this.continuer = (var0, var1, var2) -> restoredLambda$new$0(var0, var1, var2);
      }

      Builder(TextReplacementConfigImpl var1) {
         this.continuer = (var0, var1x, var2) -> restoredLambda$new$0(var0, var1x, var2);
         this.matchPattern = var1.matchPattern;
         this.replacement = var1.replacement;
         this.continuer = var1.continuer;
      }

      @NotNull
      public TextReplacementConfigImpl.Builder match(@NotNull Pattern var1) {
         this.matchPattern = Objects.requireNonNull(var1, "pattern");
         return this;
      }

      @NotNull
      public TextReplacementConfigImpl.Builder condition(TextReplacementConfig.@NotNull Condition var1) {
         this.continuer = Objects.requireNonNull(var1, "continuation");
         return this;
      }

      @NotNull
      public TextReplacementConfigImpl.Builder replacement(@NotNull BiFunction<MatchResult, TextComponent.Builder, @Nullable ComponentLike> var1) {
         this.replacement = Objects.requireNonNull(var1, "replacement");
         return this;
      }

      @NotNull
      public TextReplacementConfig build() {
         if (this.matchPattern == null) {
            throw new IllegalStateException("A pattern must be provided to match against");
         } else if (this.replacement == null) {
            throw new IllegalStateException("A replacement action must be provided");
         } else {
            return new TextReplacementConfigImpl(this);
         }
      }

      // $VF: synthetic method
      private static PatternReplacementResult restoredLambda$new$0(MatchResult var0, int var1, int var2) {
         return PatternReplacementResult.REPLACE;
      }
   }
}
