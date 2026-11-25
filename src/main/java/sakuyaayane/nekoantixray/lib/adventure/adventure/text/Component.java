package sakuyaayane.nekoantixray.lib.adventure.adventure.text;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Stream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import sakuyaayane.nekoantixray.lib.adventure.adventure.builder.AbstractBuilder;
import sakuyaayane.nekoantixray.lib.adventure.adventure.key.Key;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.event.ClickEvent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.event.HoverEvent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.event.HoverEventSource;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.Style;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.StyleBuilderApplicable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.StyleGetter;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.StyleSetter;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.TextColor;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.TextDecoration;
import sakuyaayane.nekoantixray.lib.adventure.adventure.translation.Translatable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.util.ForwardingIterator;
import sakuyaayane.nekoantixray.lib.adventure.adventure.util.IntFunction2;
import sakuyaayane.nekoantixray.lib.adventure.adventure.util.MonkeyBars;
import sakuyaayane.nekoantixray.lib.adventure.examination.Examinable;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

@NonExtendable
public interface Component extends ComponentBuilderApplicable, ComponentLike, Examinable, HoverEventSource<Component>, StyleGetter, StyleSetter<Component> {
   BiPredicate<? super Component, ? super Component> EQUALS = (var0, var1) -> Objects.equals(var0, var1);
   BiPredicate<? super Component, ? super Component> EQUALS_IDENTITY = (var0, var1) -> restoredLambda$static$0(var0, var1);
   Predicate<? super Component> IS_NOT_EMPTY = var0 -> restoredLambda$static$1(var0);

   @NotNull
   static TextComponent empty() {
      return TextComponentImpl.EMPTY;
   }

   @NotNull
   static TextComponent newline() {
      return TextComponentImpl.NEWLINE;
   }

   @NotNull
   static TextComponent space() {
      return TextComponentImpl.SPACE;
   }

   @Deprecated
   @ScheduledForRemoval(inVersion = "5.0.0")
   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TextComponent join(@NotNull ComponentLike var0, @NotNull ComponentLike... var1) {
      return join(var0, Arrays.asList(var1));
   }

   @Deprecated
   @ScheduledForRemoval(inVersion = "5.0.0")
   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TextComponent join(@NotNull ComponentLike var0, Iterable<? extends ComponentLike> var1) {
      Component var2 = join(JoinConfiguration.separator(var0), var1);
      return var2 instanceof TextComponent ? (TextComponent)var2 : text().append(var2).build();
   }

   @Contract(pure = true)
   @NotNull
   static Component join(JoinConfiguration.@NotNull Builder var0, @NotNull ComponentLike... var1) {
      return join(var0, Arrays.asList(var1));
   }

   @Contract(pure = true)
   @NotNull
   static Component join(JoinConfiguration.@NotNull Builder var0, @NotNull Iterable<? extends ComponentLike> var1) {
      return JoinConfigurationImpl.join(var0.build(), var1);
   }

   @Contract(pure = true)
   @NotNull
   static Component join(@NotNull JoinConfiguration var0, @NotNull ComponentLike... var1) {
      return join(var0, Arrays.asList(var1));
   }

   @Contract(pure = true)
   @NotNull
   static Component join(@NotNull JoinConfiguration var0, @NotNull Iterable<? extends ComponentLike> var1) {
      return JoinConfigurationImpl.join(var0, var1);
   }

   @NotNull
   static Collector<Component, ? extends ComponentBuilder<?, ?>, Component> toComponent() {
      return toComponent(empty());
   }

   @NotNull
   static Collector<Component, ? extends ComponentBuilder<?, ?>, Component> toComponent(@NotNull Component var0) {
      return Collector.of(
         () -> text(),
         (var1, var2) -> restoredLambda$toComponent$2(var0, (TextComponent.Builder)var1, var2),
         (var1, var2) -> restoredLambda$toComponent$3(var0, (TextComponent.Builder)var1, (TextComponent.Builder)var2),
         var0x -> var0x.build()
      );
   }

   @Contract(pure = true)
   static BlockNBTComponent.@NotNull Builder blockNBT() {
      return new BlockNBTComponentImpl.BuilderImpl();
   }

   @Contract("_ -> new")
   @NotNull
   static BlockNBTComponent blockNBT(@NotNull Consumer<? super BlockNBTComponent.Builder> var0) {
      return AbstractBuilder.configureAndBuild(blockNBT(), var0);
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static BlockNBTComponent blockNBT(@NotNull String var0, BlockNBTComponent.@NotNull Pos var1) {
      return blockNBT(var0, false, var1);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static BlockNBTComponent blockNBT(@NotNull String var0, boolean var1, BlockNBTComponent.@NotNull Pos var2) {
      return blockNBT(var0, var1, null, var2);
   }

   @Contract(value = "_, _, _, _ -> new", pure = true)
   @NotNull
   static BlockNBTComponent blockNBT(@NotNull String var0, boolean var1, @Nullable ComponentLike var2, BlockNBTComponent.@NotNull Pos var3) {
      return BlockNBTComponentImpl.create(Collections.emptyList(), Style.empty(), var0, var1, var2, var3);
   }

   @Contract(pure = true)
   static EntityNBTComponent.@NotNull Builder entityNBT() {
      return new EntityNBTComponentImpl.BuilderImpl();
   }

   @Contract("_ -> new")
   @NotNull
   static EntityNBTComponent entityNBT(@NotNull Consumer<? super EntityNBTComponent.Builder> var0) {
      return AbstractBuilder.configureAndBuild(entityNBT(), var0);
   }

   @Contract("_, _ -> new")
   @NotNull
   static EntityNBTComponent entityNBT(@NotNull String var0, @NotNull String var1) {
      return entityNBT().nbtPath(var0).selector(var1).build();
   }

   @Contract(pure = true)
   static KeybindComponent.@NotNull Builder keybind() {
      return new KeybindComponentImpl.BuilderImpl();
   }

   @Contract("_ -> new")
   @NotNull
   static KeybindComponent keybind(@NotNull Consumer<? super KeybindComponent.Builder> var0) {
      return AbstractBuilder.configureAndBuild(keybind(), var0);
   }

   @Contract(value = "_ -> new", pure = true)
   @NotNull
   static KeybindComponent keybind(@NotNull String var0) {
      return keybind(var0, Style.empty());
   }

   @Contract(value = "_ -> new", pure = true)
   @NotNull
   static KeybindComponent keybind(KeybindComponent.@NotNull KeybindLike var0) {
      return keybind(Objects.requireNonNull(var0, "keybind").asKeybind(), Style.empty());
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static KeybindComponent keybind(@NotNull String var0, @NotNull Style var1) {
      return KeybindComponentImpl.create(Collections.emptyList(), Objects.requireNonNull(var1, "style"), var0);
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static KeybindComponent keybind(KeybindComponent.@NotNull KeybindLike var0, @NotNull Style var1) {
      return KeybindComponentImpl.create(Collections.emptyList(), Objects.requireNonNull(var1, "style"), Objects.requireNonNull(var0, "keybind").asKeybind());
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static KeybindComponent keybind(@NotNull String var0, @Nullable TextColor var1) {
      return keybind(var0, Style.style(var1));
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static KeybindComponent keybind(KeybindComponent.@NotNull KeybindLike var0, @Nullable TextColor var1) {
      return keybind(Objects.requireNonNull(var0, "keybind").asKeybind(), Style.style(var1));
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static KeybindComponent keybind(@NotNull String var0, @Nullable TextColor var1, TextDecoration @NotNull ... var2) {
      return keybind(var0, Style.style(var1, var2));
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static KeybindComponent keybind(KeybindComponent.@NotNull KeybindLike var0, @Nullable TextColor var1, TextDecoration @NotNull ... var2) {
      return keybind(Objects.requireNonNull(var0, "keybind").asKeybind(), Style.style(var1, var2));
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static KeybindComponent keybind(@NotNull String var0, @Nullable TextColor var1, @NotNull Set<TextDecoration> var2) {
      return keybind(var0, Style.style(var1, var2));
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static KeybindComponent keybind(KeybindComponent.@NotNull KeybindLike var0, @Nullable TextColor var1, @NotNull Set<TextDecoration> var2) {
      return keybind(Objects.requireNonNull(var0, "keybind").asKeybind(), Style.style(var1, var2));
   }

   @Contract(pure = true)
   static ScoreComponent.@NotNull Builder score() {
      return new ScoreComponentImpl.BuilderImpl();
   }

   @Contract("_ -> new")
   @NotNull
   static ScoreComponent score(@NotNull Consumer<? super ScoreComponent.Builder> var0) {
      return AbstractBuilder.configureAndBuild(score(), var0);
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static ScoreComponent score(@NotNull String var0, @NotNull String var1) {
      return score(var0, var1, null);
   }

   @Deprecated
   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static ScoreComponent score(@NotNull String var0, @NotNull String var1, @Nullable String var2) {
      return ScoreComponentImpl.create(Collections.emptyList(), Style.empty(), var0, var1, var2);
   }

   @Contract(pure = true)
   static SelectorComponent.@NotNull Builder selector() {
      return new SelectorComponentImpl.BuilderImpl();
   }

   @Contract("_ -> new")
   @NotNull
   static SelectorComponent selector(@NotNull Consumer<? super SelectorComponent.Builder> var0) {
      return AbstractBuilder.configureAndBuild(selector(), var0);
   }

   @Contract(value = "_ -> new", pure = true)
   @NotNull
   static SelectorComponent selector(@NotNull String var0) {
      return selector(var0, null);
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static SelectorComponent selector(@NotNull String var0, @Nullable ComponentLike var1) {
      return SelectorComponentImpl.create(Collections.emptyList(), Style.empty(), var0, var1);
   }

   @Contract(pure = true)
   static StorageNBTComponent.@NotNull Builder storageNBT() {
      return new StorageNBTComponentImpl.BuilderImpl();
   }

   @Contract("_ -> new")
   @NotNull
   static StorageNBTComponent storageNBT(@NotNull Consumer<? super StorageNBTComponent.Builder> var0) {
      return AbstractBuilder.configureAndBuild(storageNBT(), var0);
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static StorageNBTComponent storageNBT(@NotNull String var0, @NotNull Key var1) {
      return storageNBT(var0, false, var1);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static StorageNBTComponent storageNBT(@NotNull String var0, boolean var1, @NotNull Key var2) {
      return storageNBT(var0, var1, null, var2);
   }

   @Contract(value = "_, _, _, _ -> new", pure = true)
   @NotNull
   static StorageNBTComponent storageNBT(@NotNull String var0, boolean var1, @Nullable ComponentLike var2, @NotNull Key var3) {
      return StorageNBTComponentImpl.create(Collections.emptyList(), Style.empty(), var0, var1, var2, var3);
   }

   @Contract(pure = true)
   static TextComponent.@NotNull Builder text() {
      return new TextComponentImpl.BuilderImpl();
   }

   @NotNull
   static TextComponent textOfChildren(@NotNull ComponentLike... var0) {
      return var0.length == 0 ? empty() : TextComponentImpl.create(Arrays.asList(var0), Style.empty(), "");
   }

   @Contract("_ -> new")
   @NotNull
   static TextComponent text(@NotNull Consumer<? super TextComponent.Builder> var0) {
      return AbstractBuilder.configureAndBuild(text(), var0);
   }

   @Contract(value = "_ -> new", pure = true)
   @NotNull
   static TextComponent text(@NotNull String var0) {
      return var0.isEmpty() ? empty() : text(var0, Style.empty());
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TextComponent text(@NotNull String var0, @NotNull Style var1) {
      return TextComponentImpl.create(Collections.emptyList(), Objects.requireNonNull(var1, "style"), var0);
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TextComponent text(@NotNull String var0, @Nullable TextColor var1) {
      return text(var0, Style.style(var1));
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TextComponent text(@NotNull String var0, @Nullable TextColor var1, TextDecoration @NotNull ... var2) {
      return text(var0, Style.style(var1, var2));
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TextComponent text(@NotNull String var0, @Nullable TextColor var1, @NotNull Set<TextDecoration> var2) {
      return text(var0, Style.style(var1, var2));
   }

   @Contract(value = "_ -> new", pure = true)
   @NotNull
   static TextComponent text(boolean var0) {
      return text(String.valueOf(var0));
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TextComponent text(boolean var0, @NotNull Style var1) {
      return text(String.valueOf(var0), var1);
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TextComponent text(boolean var0, @Nullable TextColor var1) {
      return text(String.valueOf(var0), var1);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TextComponent text(boolean var0, @Nullable TextColor var1, TextDecoration @NotNull ... var2) {
      return text(String.valueOf(var0), var1, var2);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TextComponent text(boolean var0, @Nullable TextColor var1, @NotNull Set<TextDecoration> var2) {
      return text(String.valueOf(var0), var1, var2);
   }

   @Contract(pure = true)
   @NotNull
   static TextComponent text(char var0) {
      if (var0 == '\n') {
         return newline();
      } else {
         return var0 == ' ' ? space() : text(String.valueOf(var0));
      }
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TextComponent text(char var0, @NotNull Style var1) {
      return text(String.valueOf(var0), var1);
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TextComponent text(char var0, @Nullable TextColor var1) {
      return text(String.valueOf(var0), var1);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TextComponent text(char var0, @Nullable TextColor var1, TextDecoration @NotNull ... var2) {
      return text(String.valueOf(var0), var1, var2);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TextComponent text(char var0, @Nullable TextColor var1, @NotNull Set<TextDecoration> var2) {
      return text(String.valueOf(var0), var1, var2);
   }

   @Contract(value = "_ -> new", pure = true)
   @NotNull
   static TextComponent text(double var0) {
      return text(String.valueOf(var0));
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TextComponent text(double var0, @NotNull Style var2) {
      return text(String.valueOf(var0), var2);
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TextComponent text(double var0, @Nullable TextColor var2) {
      return text(String.valueOf(var0), var2);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TextComponent text(double var0, @Nullable TextColor var2, TextDecoration @NotNull ... var3) {
      return text(String.valueOf(var0), var2, var3);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TextComponent text(double var0, @Nullable TextColor var2, @NotNull Set<TextDecoration> var3) {
      return text(String.valueOf(var0), var2, var3);
   }

   @Contract(value = "_ -> new", pure = true)
   @NotNull
   static TextComponent text(float var0) {
      return text(String.valueOf(var0));
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TextComponent text(float var0, @NotNull Style var1) {
      return text(String.valueOf(var0), var1);
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TextComponent text(float var0, @Nullable TextColor var1) {
      return text(String.valueOf(var0), var1);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TextComponent text(float var0, @Nullable TextColor var1, TextDecoration @NotNull ... var2) {
      return text(String.valueOf(var0), var1, var2);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TextComponent text(float var0, @Nullable TextColor var1, @NotNull Set<TextDecoration> var2) {
      return text(String.valueOf(var0), var1, var2);
   }

   @Contract(value = "_ -> new", pure = true)
   @NotNull
   static TextComponent text(int var0) {
      return text(String.valueOf(var0));
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TextComponent text(int var0, @NotNull Style var1) {
      return text(String.valueOf(var0), var1);
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TextComponent text(int var0, @Nullable TextColor var1) {
      return text(String.valueOf(var0), var1);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TextComponent text(int var0, @Nullable TextColor var1, TextDecoration @NotNull ... var2) {
      return text(String.valueOf(var0), var1, var2);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TextComponent text(int var0, @Nullable TextColor var1, @NotNull Set<TextDecoration> var2) {
      return text(String.valueOf(var0), var1, var2);
   }

   @Contract(value = "_ -> new", pure = true)
   @NotNull
   static TextComponent text(long var0) {
      return text(String.valueOf(var0));
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TextComponent text(long var0, @NotNull Style var2) {
      return text(String.valueOf(var0), var2);
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TextComponent text(long var0, @Nullable TextColor var2) {
      return text(String.valueOf(var0), var2);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TextComponent text(long var0, @Nullable TextColor var2, TextDecoration @NotNull ... var3) {
      return text(String.valueOf(var0), var2, var3);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TextComponent text(long var0, @Nullable TextColor var2, @NotNull Set<TextDecoration> var3) {
      return text(String.valueOf(var0), var2, var3);
   }

   @Contract(pure = true)
   static TranslatableComponent.@NotNull Builder translatable() {
      return new TranslatableComponentImpl.BuilderImpl();
   }

   @Contract("_ -> new")
   @NotNull
   static TranslatableComponent translatable(@NotNull Consumer<? super TranslatableComponent.Builder> var0) {
      return AbstractBuilder.configureAndBuild(translatable(), var0);
   }

   @Contract(value = "_ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull String var0) {
      return translatable(var0, Style.empty());
   }

   @Contract(value = "_ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull Translatable var0) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), Style.empty());
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull String var0, @Nullable String var1) {
      return translatable(var0, var1, Style.empty());
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull Translatable var0, @Nullable String var1) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), var1, Style.empty());
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull String var0, @NotNull Style var1) {
      return TranslatableComponentImpl.create(Collections.emptyList(), Objects.requireNonNull(var1, "style"), var0, null, Collections.emptyList());
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull Translatable var0, @NotNull Style var1) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), var1);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull String var0, @Nullable String var1, @NotNull Style var2) {
      return TranslatableComponentImpl.create(Collections.emptyList(), Objects.requireNonNull(var2, "style"), var0, var1, Collections.emptyList());
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull Translatable var0, @Nullable String var1, @NotNull Style var2) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), var1, var2);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull String var0, @Nullable String var1, @NotNull StyleBuilderApplicable... var2) {
      return translatable(Objects.requireNonNull(var0, "key"), var1, Style.style(var2));
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull Translatable var0, @Nullable String var1, @NotNull Iterable<StyleBuilderApplicable> var2) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), var1, Style.style(var2));
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull String var0, @Nullable String var1, @NotNull ComponentLike... var2) {
      return translatable(var0, var1, Style.empty(), var2);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull Translatable var0, @Nullable String var1, @NotNull ComponentLike... var2) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), var1, var2);
   }

   @Contract(value = "_, _, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull String var0, @Nullable String var1, @NotNull Style var2, @NotNull ComponentLike... var3) {
      return TranslatableComponentImpl.create(Collections.emptyList(), Objects.requireNonNull(var2, "style"), var0, var1, Objects.requireNonNull(var3, "args"));
   }

   @Contract(value = "_, _, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull Translatable var0, @Nullable String var1, @NotNull Style var2, @NotNull ComponentLike... var3) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), var1, var2, var3);
   }

   @Contract(value = "_, _, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull String var0, @Nullable String var1, @NotNull Style var2, @NotNull List<? extends ComponentLike> var3) {
      return TranslatableComponentImpl.create(Collections.emptyList(), var2, var0, var1, Objects.requireNonNull(var3, "args"));
   }

   @Contract(value = "_, _, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(
      @NotNull Translatable var0, @Nullable String var1, @NotNull Style var2, @NotNull List<? extends ComponentLike> var3
   ) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), var1, var2, var3);
   }

   @Contract(value = "_, _, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(
      @NotNull String var0, @Nullable String var1, @NotNull List<? extends ComponentLike> var2, @NotNull Iterable<StyleBuilderApplicable> var3
   ) {
      return TranslatableComponentImpl.create(Collections.emptyList(), Style.style(var3), var0, var1, Objects.requireNonNull(var2, "args"));
   }

   @Contract(value = "_, _, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(
      @NotNull Translatable var0, @Nullable String var1, @NotNull List<? extends ComponentLike> var2, @NotNull Iterable<StyleBuilderApplicable> var3
   ) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), var1, var2, var3);
   }

   @Contract(value = "_, _, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(
      @NotNull String var0, @Nullable String var1, @NotNull List<? extends ComponentLike> var2, @NotNull StyleBuilderApplicable... var3
   ) {
      return TranslatableComponentImpl.create(Collections.emptyList(), Style.style(var3), var0, var1, Objects.requireNonNull(var2, "args"));
   }

   @Contract(value = "_, _, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(
      @NotNull Translatable var0, @Nullable String var1, @NotNull List<? extends ComponentLike> var2, @NotNull StyleBuilderApplicable... var3
   ) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), var1, var2, var3);
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull String var0, @Nullable TextColor var1) {
      return translatable(var0, Style.style(var1));
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull Translatable var0, @Nullable TextColor var1) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), var1);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull String var0, @Nullable TextColor var1, TextDecoration @NotNull ... var2) {
      return translatable(var0, Style.style(var1, var2));
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull Translatable var0, @Nullable TextColor var1, TextDecoration @NotNull ... var2) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), var1, var2);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull String var0, @Nullable TextColor var1, @NotNull Set<TextDecoration> var2) {
      return translatable(var0, Style.style(var1, var2));
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull Translatable var0, @Nullable TextColor var1, @NotNull Set<TextDecoration> var2) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), var1, var2);
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull String var0, @NotNull ComponentLike... var1) {
      return translatable(var0, Style.empty(), var1);
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull Translatable var0, @NotNull ComponentLike... var1) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), var1);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull String var0, @NotNull Style var1, @NotNull ComponentLike... var2) {
      return TranslatableComponentImpl.create(Collections.emptyList(), Objects.requireNonNull(var1, "style"), var0, null, Objects.requireNonNull(var2, "args"));
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull Translatable var0, @NotNull Style var1, @NotNull ComponentLike... var2) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), var1, var2);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull String var0, @Nullable TextColor var1, @NotNull ComponentLike... var2) {
      return translatable(var0, Style.style(var1), var2);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull Translatable var0, @Nullable TextColor var1, @NotNull ComponentLike... var2) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), var1, var2);
   }

   @Contract(value = "_, _, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull String var0, @Nullable TextColor var1, @NotNull Set<TextDecoration> var2, @NotNull ComponentLike... var3) {
      return translatable(var0, Style.style(var1, var2), var3);
   }

   @Contract(value = "_, _, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(
      @NotNull Translatable var0, @Nullable TextColor var1, @NotNull Set<TextDecoration> var2, @NotNull ComponentLike... var3
   ) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), var1, var2, var3);
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull String var0, @NotNull List<? extends ComponentLike> var1) {
      return TranslatableComponentImpl.create(Collections.emptyList(), Style.empty(), var0, null, Objects.requireNonNull(var1, "args"));
   }

   @Contract(value = "_, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull Translatable var0, @NotNull List<? extends ComponentLike> var1) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), var1);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull String var0, @NotNull Style var1, @NotNull List<? extends ComponentLike> var2) {
      return TranslatableComponentImpl.create(Collections.emptyList(), Objects.requireNonNull(var1, "style"), var0, null, Objects.requireNonNull(var2, "args"));
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(@NotNull Translatable var0, @NotNull Style var1, @NotNull List<? extends ComponentLike> var2) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), var1, var2);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   static TranslatableComponent translatable(@NotNull String var0, @Nullable TextColor var1, @NotNull List<? extends ComponentLike> var2) {
      return translatable(var0, Style.style(var1), var2);
   }

   @Contract(value = "_, _, _ -> new", pure = true)
   static TranslatableComponent translatable(@NotNull Translatable var0, @Nullable TextColor var1, @NotNull List<? extends ComponentLike> var2) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), var1, var2);
   }

   @Contract(value = "_, _, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(
      @NotNull String var0, @Nullable TextColor var1, @NotNull Set<TextDecoration> var2, @NotNull List<? extends ComponentLike> var3
   ) {
      return translatable(var0, Style.style(var1, var2), var3);
   }

   @Contract(value = "_, _, _, _ -> new", pure = true)
   @NotNull
   static TranslatableComponent translatable(
      @NotNull Translatable var0, @Nullable TextColor var1, @NotNull Set<TextDecoration> var2, @NotNull List<? extends ComponentLike> var3
   ) {
      return translatable(Objects.requireNonNull(var0, "translatable").translationKey(), var1, var2, var3);
   }

   @NotNull
   @Unmodifiable List<Component> children();

   @Contract(pure = true)
   @NotNull
   Component children(@NotNull List<? extends ComponentLike> var1);

   default boolean contains(@NotNull Component var1) {
      return this.contains(var1, EQUALS_IDENTITY);
   }

   default boolean contains(@NotNull Component var1, @NotNull BiPredicate<? super Component, ? super Component> var2) {
      if (var2.test(this, var1)) {
         return true;
      }

      for (Component var4 : this.children()) {
         if (var4.contains(var1, var2)) {
            return true;
         }
      }

      HoverEvent var8 = this.hoverEvent();
      if (var8 != null) {
         Object var9 = var8.value();
         Component var5 = null;
         if (var9 instanceof Component) {
            var5 = (Component)var8.value();
         } else if (var9 instanceof HoverEvent.ShowEntity) {
            var5 = ((HoverEvent.ShowEntity)var9).name();
         }

         if (var5 != null) {
            if (var2.test(var1, var5)) {
               return true;
            }

            for (Component var7 : var5.children()) {
               if (var7.contains(var1, var2)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   @Deprecated
   @ScheduledForRemoval(inVersion = "5.0.0")
   default void detectCycle(@NotNull Component var1) {
      if (var1.contains(this)) {
         throw new IllegalStateException("Component cycle detected between " + this + " and " + var1);
      }
   }

   @Contract(pure = true)
   @NotNull
   default Component append(@NotNull Component var1) {
      return this.append((ComponentLike)var1);
   }

   @NotNull
   default Component append(@NotNull ComponentLike var1) {
      Objects.requireNonNull(var1, "like");
      Component var2 = var1.asComponent();
      Objects.requireNonNull(var2, "component");
      if (var2 == empty()) {
         return this;
      }

      List var3 = this.children();
      return this.children(MonkeyBars.addOne(var3, var2));
   }

   @Contract(pure = true)
   @NotNull
   default Component append(@NotNull ComponentBuilder<?, ?> var1) {
      return this.append(var1.build());
   }

   @Contract(pure = true)
   @NotNull
   default Component appendNewline() {
      return this.append(newline());
   }

   @Contract(pure = true)
   @NotNull
   default Component appendSpace() {
      return this.append(space());
   }

   @Contract(pure = true)
   @NotNull
   default Component applyFallbackStyle(@NotNull Style var1) {
      Objects.requireNonNull(var1, "style");
      return this.style(this.style().merge(var1, Style.Merge.Strategy.IF_ABSENT_ON_TARGET));
   }

   @Contract(pure = true)
   @NotNull
   default Component applyFallbackStyle(@NotNull StyleBuilderApplicable... var1) {
      return this.applyFallbackStyle(Style.style(var1));
   }

   @NotNull
   Style style();

   @Contract(pure = true)
   @NotNull
   Component style(@NotNull Style var1);

   @Contract(pure = true)
   @NotNull
   default Component style(@NotNull Consumer<Style.Builder> var1) {
      return this.style(this.style().edit(var1));
   }

   @Contract(pure = true)
   @NotNull
   default Component style(@NotNull Consumer<Style.Builder> var1, Style.Merge.@NotNull Strategy var2) {
      return this.style(this.style().edit(var1, var2));
   }

   @Contract(pure = true)
   @NotNull
   default Component style(Style.@NotNull Builder var1) {
      return this.style(var1.build());
   }

   @Contract(pure = true)
   @NotNull
   default Component mergeStyle(@NotNull Component var1) {
      return this.mergeStyle(var1, Style.Merge.all());
   }

   @Contract(pure = true)
   @NotNull
   default Component mergeStyle(@NotNull Component var1, Style.@NotNull Merge @NotNull ... var2) {
      return this.mergeStyle(var1, Style.Merge.merges(var2));
   }

   @Contract(pure = true)
   @NotNull
   default Component mergeStyle(@NotNull Component var1, @NotNull Set<Style.Merge> var2) {
      return this.style(this.style().merge(var1.style(), var2));
   }

   @Nullable
   @Override
   default Key font() {
      return this.style().font();
   }

   @NotNull
   default Component font(@Nullable Key var1) {
      return this.style(this.style().font(var1));
   }

   @Nullable
   @Override
   default TextColor color() {
      return this.style().color();
   }

   @Contract(pure = true)
   @NotNull
   default Component color(@Nullable TextColor var1) {
      return this.style(this.style().color(var1));
   }

   @Contract(pure = true)
   @NotNull
   default Component colorIfAbsent(@Nullable TextColor var1) {
      return this.color() == null ? this.color(var1) : this;
   }

   @Override
   default boolean hasDecoration(@NotNull TextDecoration var1) {
      return StyleGetter.super.hasDecoration(var1);
   }

   @Contract(pure = true)
   @NotNull
   default Component decorate(@NotNull TextDecoration var1) {
      return (Component)StyleSetter.super.decorate(var1);
   }

   @Override
   default TextDecoration.@NotNull State decoration(@NotNull TextDecoration var1) {
      return this.style().decoration(var1);
   }

   @Contract(pure = true)
   @NotNull
   default Component decoration(@NotNull TextDecoration var1, boolean var2) {
      return (Component)StyleSetter.super.decoration(var1, var2);
   }

   @Contract(pure = true)
   @NotNull
   default Component decoration(@NotNull TextDecoration var1, TextDecoration.@NotNull State var2) {
      return this.style(this.style().decoration(var1, var2));
   }

   @NotNull
   default Component decorationIfAbsent(@NotNull TextDecoration var1, TextDecoration.@NotNull State var2) {
      Objects.requireNonNull(var2, "state");
      TextDecoration.State var3 = this.decoration(var1);
      return var3 == TextDecoration.State.NOT_SET ? this.style(this.style().decoration(var1, var2)) : this;
   }

   @NotNull
   @Override
   default Map<TextDecoration, TextDecoration.State> decorations() {
      return this.style().decorations();
   }

   @Contract(pure = true)
   @NotNull
   default Component decorations(@NotNull Map<TextDecoration, TextDecoration.State> var1) {
      return this.style(this.style().decorations(var1));
   }

   @Nullable
   @Override
   default ClickEvent clickEvent() {
      return this.style().clickEvent();
   }

   @Contract(pure = true)
   @NotNull
   default Component clickEvent(@Nullable ClickEvent var1) {
      return this.style(this.style().clickEvent(var1));
   }

   @Nullable
   @Override
   default HoverEvent<?> hoverEvent() {
      return this.style().hoverEvent();
   }

   @Contract(pure = true)
   @NotNull
   default Component hoverEvent(@Nullable HoverEventSource<?> var1) {
      return this.style(this.style().hoverEvent(var1));
   }

   @Nullable
   @Override
   default String insertion() {
      return this.style().insertion();
   }

   @Contract(pure = true)
   @NotNull
   default Component insertion(@Nullable String var1) {
      return this.style(this.style().insertion(var1));
   }

   default boolean hasStyling() {
      return !this.style().isEmpty();
   }

   @Contract(pure = true)
   @NotNull
   default Component replaceText(@NotNull Consumer<TextReplacementConfig.Builder> var1) {
      Objects.requireNonNull(var1, "configurer");
      return this.replaceText(AbstractBuilder.configureAndBuild(TextReplacementConfig.builder(), var1));
   }

   @Contract(pure = true)
   @NotNull
   default Component replaceText(@NotNull TextReplacementConfig var1) {
      Objects.requireNonNull(var1, "replacement");
      if (!(var1 instanceof TextReplacementConfigImpl)) {
         throw new IllegalArgumentException("Provided replacement was a custom TextReplacementConfig implementation, which is not supported.");
      } else {
         return TextReplacementRenderer.INSTANCE.render(this, ((TextReplacementConfigImpl)var1).createState());
      }
   }

   @NotNull
   default Component compact() {
      return ComponentCompaction.compact(this, null);
   }

   @NotNull
   default Iterable<Component> iterable(@NotNull ComponentIteratorType var1, @NotNull ComponentIteratorFlag @Nullable ... var2) {
      return this.iterable(var1, var2 == null ? Collections.emptySet() : MonkeyBars.enumSet(ComponentIteratorFlag.class, var2));
   }

   @NotNull
   default Iterable<Component> iterable(@NotNull ComponentIteratorType var1, @NotNull Set<ComponentIteratorFlag> var2) {
      Objects.requireNonNull(var1, "type");
      Objects.requireNonNull(var2, "flags");
      return new ForwardingIterator<>(() -> this.restoredLambda$iterable$4(var1, var2), () -> this.restoredLambda$iterable$5(var1, var2));
   }

   @NotNull
   default Iterator<Component> iterator(@NotNull ComponentIteratorType var1, @NotNull ComponentIteratorFlag @Nullable ... var2) {
      return this.iterator(var1, var2 == null ? Collections.emptySet() : MonkeyBars.enumSet(ComponentIteratorFlag.class, var2));
   }

   @NotNull
   default Iterator<Component> iterator(@NotNull ComponentIteratorType var1, @NotNull Set<ComponentIteratorFlag> var2) {
      return new ComponentIterator(this, Objects.requireNonNull(var1, "type"), Objects.requireNonNull(var2, "flags"));
   }

   @NotNull
   default Spliterator<Component> spliterator(@NotNull ComponentIteratorType var1, @NotNull ComponentIteratorFlag @Nullable ... var2) {
      return this.spliterator(var1, var2 == null ? Collections.emptySet() : MonkeyBars.enumSet(ComponentIteratorFlag.class, var2));
   }

   @NotNull
   default Spliterator<Component> spliterator(@NotNull ComponentIteratorType var1, @NotNull Set<ComponentIteratorFlag> var2) {
      return Spliterators.spliteratorUnknownSize(this.iterator(var1, var2), 1296);
   }

   @Deprecated
   @ScheduledForRemoval(inVersion = "5.0.0")
   @Contract(pure = true)
   @NotNull
   default Component replaceText(@NotNull String var1, @Nullable ComponentLike var2) {
      return this.replaceText(var2x -> restoredLambda$replaceText$6(var1, var2, var2x));
   }

   @Deprecated
   @ScheduledForRemoval(inVersion = "5.0.0")
   @Contract(pure = true)
   @NotNull
   default Component replaceText(@NotNull Pattern var1, @NotNull Function<TextComponent.Builder, @Nullable ComponentLike> var2) {
      return this.replaceText(var2x -> restoredLambda$replaceText$7(var1, var2, var2x));
   }

   @Deprecated
   @ScheduledForRemoval(inVersion = "5.0.0")
   @Contract(pure = true)
   @NotNull
   default Component replaceFirstText(@NotNull String var1, @Nullable ComponentLike var2) {
      return this.replaceText(var2x -> restoredLambda$replaceFirstText$8(var1, var2, var2x));
   }

   @Deprecated
   @ScheduledForRemoval(inVersion = "5.0.0")
   @Contract(pure = true)
   @NotNull
   default Component replaceFirstText(@NotNull Pattern var1, @NotNull Function<TextComponent.Builder, @Nullable ComponentLike> var2) {
      return this.replaceText(var2x -> restoredLambda$replaceFirstText$9(var1, var2, var2x));
   }

   @Deprecated
   @ScheduledForRemoval(inVersion = "5.0.0")
   @Contract(pure = true)
   @NotNull
   default Component replaceText(@NotNull String var1, @Nullable ComponentLike var2, int var3) {
      return this.replaceText(var3x -> restoredLambda$replaceText$10(var1, var3, var2, var3x));
   }

   @Deprecated
   @ScheduledForRemoval(inVersion = "5.0.0")
   @Contract(pure = true)
   @NotNull
   default Component replaceText(@NotNull Pattern var1, @NotNull Function<TextComponent.Builder, @Nullable ComponentLike> var2, int var3) {
      return this.replaceText(var3x -> restoredLambda$replaceText$11(var1, var3, var2, var3x));
   }

   @Deprecated
   @ScheduledForRemoval(inVersion = "5.0.0")
   @Contract(pure = true)
   @NotNull
   default Component replaceText(@NotNull String var1, @Nullable ComponentLike var2, @NotNull IntFunction2<PatternReplacementResult> var3) {
      return this.replaceText(var3x -> restoredLambda$replaceText$12(var1, var2, var3, var3x));
   }

   @Deprecated
   @ScheduledForRemoval(inVersion = "5.0.0")
   @Contract(pure = true)
   @NotNull
   default Component replaceText(
      @NotNull Pattern var1, @NotNull Function<TextComponent.Builder, @Nullable ComponentLike> var2, @NotNull IntFunction2<PatternReplacementResult> var3
   ) {
      return this.replaceText(var3x -> restoredLambda$replaceText$13(var1, var2, var3, var3x));
   }

   @Override
   default void componentBuilderApply(@NotNull ComponentBuilder<?, ?> var1) {
      var1.append(this);
   }

   @NotNull
   @Override
   default Component asComponent() {
      return this;
   }

   @NotNull
   @Override
   default HoverEvent<Component> asHoverEvent(@NotNull UnaryOperator<Component> var1) {
      return HoverEvent.showText(var1.apply(this));
   }

   @NotNull
   @Override
   default Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.of(ExaminableProperty.of("style", this.style()), ExaminableProperty.of("children", this.children()));
   }

   // $VF: synthetic method
   private static void restoredLambda$replaceText$13(Pattern var0, Function var1, IntFunction2 var2, TextReplacementConfig.Builder var3) {
      var3.match(var0).replacement(var1).condition(var2);
   }

   // $VF: synthetic method
   private static void restoredLambda$replaceText$12(String var0, ComponentLike var1, IntFunction2 var2, TextReplacementConfig.Builder var3) {
      var3.matchLiteral(var0).replacement(var1).condition(var2);
   }

   // $VF: synthetic method
   private static void restoredLambda$replaceText$11(Pattern var0, int var1, Function var2, TextReplacementConfig.Builder var3) {
      var3.match(var0).times(var1).replacement(var2);
   }

   // $VF: synthetic method
   private static void restoredLambda$replaceText$10(String var0, int var1, ComponentLike var2, TextReplacementConfig.Builder var3) {
      var3.matchLiteral(var0).times(var1).replacement(var2);
   }

   // $VF: synthetic method
   private static void restoredLambda$replaceFirstText$9(Pattern var0, Function var1, TextReplacementConfig.Builder var2) {
      var2.match(var0).once().replacement(var1);
   }

   // $VF: synthetic method
   private static void restoredLambda$replaceFirstText$8(String var0, ComponentLike var1, TextReplacementConfig.Builder var2) {
      var2.matchLiteral(var0).once().replacement(var1);
   }

   // $VF: synthetic method
   private static void restoredLambda$replaceText$7(Pattern var0, Function var1, TextReplacementConfig.Builder var2) {
      var2.match(var0).replacement(var1);
   }

   // $VF: synthetic method
   private static void restoredLambda$replaceText$6(String var0, ComponentLike var1, TextReplacementConfig.Builder var2) {
      var2.matchLiteral(var0).replacement(var1);
   }

   // $VF: synthetic method
   private Spliterator restoredLambda$iterable$5(ComponentIteratorType var1, Set var2) {
      return this.spliterator(var1, var2);
   }

   // $VF: synthetic method
   private Iterator restoredLambda$iterable$4(ComponentIteratorType var1, Set var2) {
      return this.iterator(var1, var2);
   }

   // $VF: synthetic method
   private static TextComponent.Builder restoredLambda$toComponent$3(Component var0, TextComponent.Builder var1, TextComponent.Builder var2) {
      List var3 = var1.children();
      TextComponent.Builder var4 = text().append(var3);
      if (!var3.isEmpty()) {
         var4.append(var0);
      }

      var4.append(var2.children());
      return var4;
   }

   // $VF: synthetic method
   private static void restoredLambda$toComponent$2(Component var0, TextComponent.Builder var1, Component var2) {
      if (var0 != empty() && !var1.children().isEmpty()) {
         var1.append(var0);
      }

      var1.append(var2);
   }

   // $VF: synthetic method
   private static boolean restoredLambda$static$1(Component var0) {
      return var0 != empty();
   }

   // $VF: synthetic method
   private static boolean restoredLambda$static$0(Component var0, Component var1) {
      return var0 == var1;
   }
}
