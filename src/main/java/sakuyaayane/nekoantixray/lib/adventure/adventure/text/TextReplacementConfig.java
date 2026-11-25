package sakuyaayane.nekoantixray.lib.adventure.adventure.text;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.builder.AbstractBuilder;
import sakuyaayane.nekoantixray.lib.adventure.adventure.util.Buildable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.util.IntFunction2;
import sakuyaayane.nekoantixray.lib.adventure.examination.Examinable;

public interface TextReplacementConfig extends Buildable<TextReplacementConfig, TextReplacementConfig.Builder>, Examinable {
   @NotNull
   static TextReplacementConfig.Builder builder() {
      return new TextReplacementConfigImpl.Builder();
   }

   @NotNull
   Pattern matchPattern();

   interface Builder extends AbstractBuilder<TextReplacementConfig>, Buildable.Builder<TextReplacementConfig> {
      @Contract("_ -> this")
      default TextReplacementConfig.Builder matchLiteral(String var1) {
         return this.match(Pattern.compile(var1, 16));
      }

      @Contract("_ -> this")
      @NotNull
      default TextReplacementConfig.Builder match(@NotNull @RegExp String var1) {
         return this.match(Pattern.compile(var1));
      }

      @Contract("_ -> this")
      @NotNull
      TextReplacementConfig.Builder match(@NotNull Pattern var1);

      @NotNull
      default TextReplacementConfig.Builder once() {
         return this.times(1);
      }

      @Contract("_ -> this")
      @NotNull
      default TextReplacementConfig.Builder times(int var1) {
         return this.condition((var1x, var2) -> restoredLambda$times$0(var1, var1x, var2));
      }

      @Contract("_ -> this")
      @NotNull
      default TextReplacementConfig.Builder condition(@NotNull IntFunction2<PatternReplacementResult> var1) {
         return this.condition((var1x, var2, var3) -> restoredLambda$condition$1(var1, var1x, var2, var3));
      }

      @Contract("_ -> this")
      @NotNull
      TextReplacementConfig.Builder condition(@NotNull TextReplacementConfig.Condition var1);

      @Contract("_ -> this")
      @NotNull
      default TextReplacementConfig.Builder replacement(@NotNull String var1) {
         Objects.requireNonNull(var1, "replacement");
         return this.replacement(var1x -> restoredLambda$replacement$2(var1, var1x));
      }

      @Contract("_ -> this")
      @NotNull
      default TextReplacementConfig.Builder replacement(@Nullable ComponentLike var1) {
         Component var2 = ComponentLike.unbox(var1);
         return this.replacement((var1x, var2x) -> restoredLambda$replacement$3(var2, var1x, var2x));
      }

      @Contract("_ -> this")
      @NotNull
      default TextReplacementConfig.Builder replacement(@NotNull Function<TextComponent.Builder, @Nullable ComponentLike> var1) {
         Objects.requireNonNull(var1, "replacement");
         return this.replacement((var1x, var2) -> restoredLambda$replacement$4(var1, var1x, var2));
      }

      @Contract("_ -> this")
      @NotNull
      TextReplacementConfig.Builder replacement(@NotNull BiFunction<MatchResult, TextComponent.Builder, @Nullable ComponentLike> var1);

      // $VF: synthetic method
      private static ComponentLike restoredLambda$replacement$4(Function var0, MatchResult var1, TextComponent.Builder var2) {
         return (ComponentLike)var0.apply(var2);
      }

      // $VF: synthetic method
      private static ComponentLike restoredLambda$replacement$3(Component var0, MatchResult var1, TextComponent.Builder var2) {
         return var0;
      }

      // $VF: synthetic method
      private static ComponentLike restoredLambda$replacement$2(String var0, TextComponent.Builder var1) {
         return var1.content(var0);
      }

      // $VF: synthetic method
      private static PatternReplacementResult restoredLambda$condition$1(IntFunction2 var0, MatchResult var1, int var2, int var3) {
         return (PatternReplacementResult)var0.apply(var2, var3);
      }

      // $VF: synthetic method
      private static PatternReplacementResult restoredLambda$times$0(int var0, int var1, int var2) {
         return var2 < var0 ? PatternReplacementResult.REPLACE : PatternReplacementResult.STOP;
      }
   }

   @FunctionalInterface
   interface Condition {
      @NotNull
      PatternReplacementResult shouldReplace(@NotNull MatchResult var1, int var2, int var3);
   }
}
