package sakuyaayane.nekoantixray.lib.adventure.adventure.text.format;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.internal.Internals;
import sakuyaayane.nekoantixray.lib.adventure.adventure.key.Key;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.event.ClickEvent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.event.HoverEvent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.event.HoverEventSource;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

final class StyleImpl implements Style {
   static final StyleImpl EMPTY = new StyleImpl(null, null, DecorationMap.EMPTY, null, null, null);
   @Nullable
   final Key font;
   @Nullable
   final TextColor color;
   @NotNull
   final DecorationMap decorations;
   @Nullable
   final ClickEvent clickEvent;
   @Nullable
   final HoverEvent<?> hoverEvent;
   @Nullable
   final String insertion;

   StyleImpl(
      @Nullable Key var1,
      @Nullable TextColor var2,
      @NotNull Map<TextDecoration, TextDecoration.State> var3,
      @Nullable ClickEvent var4,
      @Nullable HoverEvent<?> var5,
      @Nullable String var6
   ) {
      this.font = var1;
      this.color = var2;
      this.decorations = DecorationMap.fromMap(var3);
      this.clickEvent = var4;
      this.hoverEvent = var5;
      this.insertion = var6;
   }

   @Nullable
   @Override
   public Key font() {
      return this.font;
   }

   @NotNull
   @Override
   public Style font(@Nullable Key var1) {
      return Objects.equals(this.font, var1) ? this : new StyleImpl(var1, this.color, this.decorations, this.clickEvent, this.hoverEvent, this.insertion);
   }

   @Nullable
   @Override
   public TextColor color() {
      return this.color;
   }

   @NotNull
   @Override
   public Style color(@Nullable TextColor var1) {
      return Objects.equals(this.color, var1) ? this : new StyleImpl(this.font, var1, this.decorations, this.clickEvent, this.hoverEvent, this.insertion);
   }

   @NotNull
   @Override
   public Style colorIfAbsent(@Nullable TextColor var1) {
      return this.color == null ? this.color(var1) : this;
   }

   @Override
   public TextDecoration.@NotNull State decoration(@NotNull TextDecoration var1) {
      TextDecoration.State var2 = this.decorations.get(var1);
      if (var2 != null) {
         return var2;
      } else {
         throw new IllegalArgumentException(String.format("unknown decoration '%s'", var1));
      }
   }

   @NotNull
   @Override
   public Style decoration(@NotNull TextDecoration var1, TextDecoration.@NotNull State var2) {
      Objects.requireNonNull(var2, "state");
      return this.decoration(var1) == var2
         ? this
         : new StyleImpl(this.font, this.color, this.decorations.with(var1, var2), this.clickEvent, this.hoverEvent, this.insertion);
   }

   @NotNull
   @Override
   public Style decorationIfAbsent(@NotNull TextDecoration var1, TextDecoration.@NotNull State var2) {
      Objects.requireNonNull(var2, "state");
      TextDecoration.State var3 = this.decorations.get(var1);
      if (var3 == TextDecoration.State.NOT_SET) {
         return new StyleImpl(this.font, this.color, this.decorations.with(var1, var2), this.clickEvent, this.hoverEvent, this.insertion);
      } else if (var3 != null) {
         return this;
      } else {
         throw new IllegalArgumentException(String.format("unknown decoration '%s'", var1));
      }
   }

   @NotNull
   @Override
   public Map<TextDecoration, TextDecoration.State> decorations() {
      return this.decorations;
   }

   @NotNull
   @Override
   public Style decorations(@NotNull Map<TextDecoration, TextDecoration.State> var1) {
      return new StyleImpl(this.font, this.color, DecorationMap.merge(var1, this.decorations), this.clickEvent, this.hoverEvent, this.insertion);
   }

   @Nullable
   @Override
   public ClickEvent clickEvent() {
      return this.clickEvent;
   }

   @NotNull
   @Override
   public Style clickEvent(@Nullable ClickEvent var1) {
      return new StyleImpl(this.font, this.color, this.decorations, var1, this.hoverEvent, this.insertion);
   }

   @Nullable
   @Override
   public HoverEvent<?> hoverEvent() {
      return this.hoverEvent;
   }

   @NotNull
   @Override
   public Style hoverEvent(@Nullable HoverEventSource<?> var1) {
      return new StyleImpl(this.font, this.color, this.decorations, this.clickEvent, HoverEventSource.unbox(var1), this.insertion);
   }

   @Nullable
   @Override
   public String insertion() {
      return this.insertion;
   }

   @NotNull
   @Override
   public Style insertion(@Nullable String var1) {
      return Objects.equals(this.insertion, var1) ? this : new StyleImpl(this.font, this.color, this.decorations, this.clickEvent, this.hoverEvent, var1);
   }

   @NotNull
   @Override
   public Style merge(@NotNull Style var1, Style.Merge.@NotNull Strategy var2, @NotNull Set<Style.Merge> var3) {
      if (nothingToMerge(var1, var2, var3)) {
         return this;
      }

      if (this.isEmpty() && Style.Merge.hasAll(var3)) {
         return var1;
      }

      Style.Builder var4 = this.toBuilder();
      var4.merge(var1, var2, var3);
      return var4.build();
   }

   @NotNull
   @Override
   public Style unmerge(@NotNull Style var1) {
      if (this.isEmpty()) {
         return this;
      }

      StyleImpl.BuilderImpl var2 = new StyleImpl.BuilderImpl(this);
      if (Objects.equals(this.font(), var1.font())) {
         var2.font(null);
      }

      if (Objects.equals(this.color(), var1.color())) {
         var2.color(null);
      }

      int var3 = 0;

      for (int var4 = DecorationMap.DECORATIONS.length; var3 < var4; var3++) {
         TextDecoration var5 = DecorationMap.DECORATIONS[var3];
         if (this.decoration(var5) == var1.decoration(var5)) {
            var2.decoration(var5, TextDecoration.State.NOT_SET);
         }
      }

      if (Objects.equals(this.clickEvent(), var1.clickEvent())) {
         var2.clickEvent(null);
      }

      if (Objects.equals(this.hoverEvent(), var1.hoverEvent())) {
         var2.hoverEvent(null);
      }

      if (Objects.equals(this.insertion(), var1.insertion())) {
         var2.insertion(null);
      }

      return var2.build();
   }

   static boolean nothingToMerge(@NotNull Style var0, Style.Merge.@NotNull Strategy var1, @NotNull Set<Style.Merge> var2) {
      if (var1 == Style.Merge.Strategy.NEVER) {
         return true;
      } else {
         return var0.isEmpty() ? true : var2.isEmpty();
      }
   }

   @Override
   public boolean isEmpty() {
      return this == EMPTY;
   }

   @NotNull
   @Override
   public Style.Builder toBuilder() {
      return new StyleImpl.BuilderImpl(this);
   }

   @NotNull
   @Override
   public Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.concat(
         this.decorations.examinableProperties(),
         Stream.of(
            ExaminableProperty.of("color", this.color),
            ExaminableProperty.of("clickEvent", this.clickEvent),
            ExaminableProperty.of("hoverEvent", this.hoverEvent),
            ExaminableProperty.of("insertion", this.insertion),
            ExaminableProperty.of("font", this.font)
         )
      );
   }

   @NotNull
   @Override
   public String toString() {
      return Internals.toString(this);
   }

   @Override
   public boolean equals(@Nullable Object var1) {
      if (this == var1) {
         return true;
      }

      if (!(var1 instanceof StyleImpl)) {
         return false;
      }

      StyleImpl var2 = (StyleImpl)var1;
      return Objects.equals(this.color, var2.color)
         && this.decorations.equals(var2.decorations)
         && Objects.equals(this.clickEvent, var2.clickEvent)
         && Objects.equals(this.hoverEvent, var2.hoverEvent)
         && Objects.equals(this.insertion, var2.insertion)
         && Objects.equals(this.font, var2.font);
   }

   @Override
   public int hashCode() {
      int var1 = Objects.hashCode(this.color);
      var1 = 31 * var1 + this.decorations.hashCode();
      var1 = 31 * var1 + Objects.hashCode(this.clickEvent);
      var1 = 31 * var1 + Objects.hashCode(this.hoverEvent);
      var1 = 31 * var1 + Objects.hashCode(this.insertion);
      return 31 * var1 + Objects.hashCode(this.font);
   }

   static final class BuilderImpl implements Style.Builder {
      @Nullable
      Key font;
      @Nullable
      TextColor color;
      final Map<TextDecoration, TextDecoration.State> decorations;
      @Nullable
      ClickEvent clickEvent;
      @Nullable
      HoverEvent<?> hoverEvent;
      @Nullable
      String insertion;

      BuilderImpl() {
         this.decorations = new EnumMap<>(DecorationMap.EMPTY);
      }

      BuilderImpl(@NotNull StyleImpl var1) {
         this.color = var1.color;
         this.decorations = new EnumMap<>(var1.decorations);
         this.clickEvent = var1.clickEvent;
         this.hoverEvent = var1.hoverEvent;
         this.insertion = var1.insertion;
         this.font = var1.font;
      }

      @NotNull
      @Override
      public Style.Builder font(@Nullable Key var1) {
         this.font = var1;
         return this;
      }

      @NotNull
      @Override
      public Style.Builder color(@Nullable TextColor var1) {
         this.color = var1;
         return this;
      }

      @NotNull
      @Override
      public Style.Builder colorIfAbsent(@Nullable TextColor var1) {
         if (this.color == null) {
            this.color = var1;
         }

         return this;
      }

      @NotNull
      @Override
      public Style.Builder decoration(@NotNull TextDecoration var1, TextDecoration.@NotNull State var2) {
         Objects.requireNonNull(var2, "state");
         Objects.requireNonNull(var1, "decoration");
         this.decorations.put(var1, var2);
         return this;
      }

      @NotNull
      @Override
      public Style.Builder decorationIfAbsent(@NotNull TextDecoration var1, TextDecoration.@NotNull State var2) {
         Objects.requireNonNull(var2, "state");
         TextDecoration.State var3 = this.decorations.get(var1);
         if (var3 == TextDecoration.State.NOT_SET) {
            this.decorations.put(var1, var2);
         }

         if (var3 != null) {
            return this;
         } else {
            throw new IllegalArgumentException(String.format("unknown decoration '%s'", var1));
         }
      }

      @NotNull
      @Override
      public Style.Builder clickEvent(@Nullable ClickEvent var1) {
         this.clickEvent = var1;
         return this;
      }

      @NotNull
      @Override
      public Style.Builder hoverEvent(@Nullable HoverEventSource<?> var1) {
         this.hoverEvent = HoverEventSource.unbox(var1);
         return this;
      }

      @NotNull
      @Override
      public Style.Builder insertion(@Nullable String var1) {
         this.insertion = var1;
         return this;
      }

      @NotNull
      @Override
      public Style.Builder merge(@NotNull Style var1, Style.Merge.@NotNull Strategy var2, @NotNull Set<Style.Merge> var3) {
         Objects.requireNonNull(var1, "style");
         Objects.requireNonNull(var2, "strategy");
         Objects.requireNonNull(var3, "merges");
         if (StyleImpl.nothingToMerge(var1, var2, var3)) {
            return this;
         }

         if (var3.contains(Style.Merge.COLOR)) {
            TextColor var4 = var1.color();
            if (var4 != null && (var2 == Style.Merge.Strategy.ALWAYS || var2 == Style.Merge.Strategy.IF_ABSENT_ON_TARGET && this.color == null)) {
               this.color(var4);
            }
         }

         if (var3.contains(Style.Merge.DECORATIONS)) {
            int var8 = 0;

            for (int var5 = DecorationMap.DECORATIONS.length; var8 < var5; var8++) {
               TextDecoration var6 = DecorationMap.DECORATIONS[var8];
               TextDecoration.State var7 = var1.decoration(var6);
               if (var7 != TextDecoration.State.NOT_SET) {
                  if (var2 == Style.Merge.Strategy.ALWAYS) {
                     this.decoration(var6, var7);
                  } else if (var2 == Style.Merge.Strategy.IF_ABSENT_ON_TARGET) {
                     this.decorationIfAbsent(var6, var7);
                  }
               }
            }
         }

         if (var3.contains(Style.Merge.EVENTS)) {
            ClickEvent var9 = var1.clickEvent();
            if (var9 != null && (var2 == Style.Merge.Strategy.ALWAYS || var2 == Style.Merge.Strategy.IF_ABSENT_ON_TARGET && this.clickEvent == null)) {
               this.clickEvent(var9);
            }

            HoverEvent var12 = var1.hoverEvent();
            if (var12 != null && (var2 == Style.Merge.Strategy.ALWAYS || var2 == Style.Merge.Strategy.IF_ABSENT_ON_TARGET && this.hoverEvent == null)) {
               this.hoverEvent(var12);
            }
         }

         if (var3.contains(Style.Merge.INSERTION)) {
            String var10 = var1.insertion();
            if (var10 != null && (var2 == Style.Merge.Strategy.ALWAYS || var2 == Style.Merge.Strategy.IF_ABSENT_ON_TARGET && this.insertion == null)) {
               this.insertion(var10);
            }
         }

         if (var3.contains(Style.Merge.FONT)) {
            Key var11 = var1.font();
            if (var11 != null && (var2 == Style.Merge.Strategy.ALWAYS || var2 == Style.Merge.Strategy.IF_ABSENT_ON_TARGET && this.font == null)) {
               this.font(var11);
            }
         }

         return this;
      }

      @NotNull
      public StyleImpl build() {
         return this.isEmpty() ? StyleImpl.EMPTY : new StyleImpl(this.font, this.color, this.decorations, this.clickEvent, this.hoverEvent, this.insertion);
      }

      private boolean isEmpty() {
         return this.color == null
            && this.decorations.values().stream().allMatch(var0 -> restoredLambda$isEmpty$0(var0))
            && this.clickEvent == null
            && this.hoverEvent == null
            && this.insertion == null
            && this.font == null;
      }

      // $VF: synthetic method
      private static boolean restoredLambda$isEmpty$0(TextDecoration.State var0) {
         return var0 == TextDecoration.State.NOT_SET;
      }
   }
}
