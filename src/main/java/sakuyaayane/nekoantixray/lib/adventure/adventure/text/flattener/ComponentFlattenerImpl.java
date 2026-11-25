package sakuyaayane.nekoantixray.lib.adventure.adventure.text.flattener;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.KeybindComponent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.ScoreComponent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.SelectorComponent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.TextComponent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.TranslatableComponent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.Style;
import sakuyaayane.nekoantixray.lib.adventure.adventure.util.InheritanceAwareMap;

final class ComponentFlattenerImpl implements ComponentFlattener {
   static final ComponentFlattener BASIC = new ComponentFlattenerImpl.BuilderImpl()
      .mapper(KeybindComponent.class, var0 -> restoredLambda$static$0(var0))
      .mapper(ScoreComponent.class, var0 -> restoredLambda$static$1(var0))
      .mapper(SelectorComponent.class, var0 -> var0.pattern())
      .mapper(TextComponent.class, var0 -> var0.content())
      .mapper(TranslatableComponent.class, var0 -> restoredLambda$static$2(var0))
      .build();
   static final ComponentFlattener TEXT_ONLY = new ComponentFlattenerImpl.BuilderImpl().mapper(TextComponent.class, var0 -> var0.content()).build();
   private static final int MAX_DEPTH = 512;
   private final InheritanceAwareMap<Component, ComponentFlattenerImpl.Handler> flatteners;
   private final Function<Component, String> unknownHandler;

   ComponentFlattenerImpl(InheritanceAwareMap<Component, ComponentFlattenerImpl.Handler> var1, @Nullable Function<Component, String> var2) {
      this.flatteners = var1;
      this.unknownHandler = var2;
   }

   @Override
   public void flatten(@NotNull Component var1, @NotNull FlattenerListener var2) {
      this.flatten0(var1, var2, 0);
   }

   private void flatten0(@NotNull Component var1, @NotNull FlattenerListener var2, int var3) {
      Objects.requireNonNull(var1, "input");
      Objects.requireNonNull(var2, "listener");
      if (var1 != Component.empty()) {
         if (var3 > 512) {
            throw new IllegalStateException("Exceeded maximum depth of 512 while attempting to flatten components!");
         }

         ComponentFlattenerImpl.Handler var4 = this.flattener(var1);
         Style var5 = var1.style();
         var2.pushStyle(var5);

         try {
            if (var4 != null) {
               var4.handle(this, var1, var2, var3 + 1);
            }

            if (!var1.children().isEmpty() && var2.shouldContinue()) {
               for (Component var7 : var1.children()) {
                  this.flatten0(var7, var2, var3 + 1);
               }
            }
         } finally {
            var2.popStyle(var5);
         }
      }
   }

   @Nullable
   private <T extends Component> ComponentFlattenerImpl.Handler flattener(T var1) {
      ComponentFlattenerImpl.Handler var2 = this.flatteners.get((Class<? extends Component>)var1.getClass());
      return var2 == null && this.unknownHandler != null ? (var1x, var2x, var3, var4) -> this.restoredLambda$flattener$3(var1x, var2x, var3, var4) : var2;
   }

   public ComponentFlattener.@NotNull Builder toBuilder() {
      return new ComponentFlattenerImpl.BuilderImpl(this.flatteners, this.unknownHandler);
   }

   // $VF: synthetic method
   private void restoredLambda$flattener$3(ComponentFlattenerImpl var1, Component var2, FlattenerListener var3, int var4) {
      var3.component(this.unknownHandler.apply(var2));
   }

   // $VF: synthetic method
   private static String restoredLambda$static$2(TranslatableComponent var0) {
      String var1 = var0.fallback();
      return var1 != null ? var1 : var0.key();
   }

   // $VF: synthetic method
   private static String restoredLambda$static$1(ScoreComponent var0) {
      String var1 = var0.value();
      return var1 != null ? var1 : "";
   }

   // $VF: synthetic method
   private static String restoredLambda$static$0(KeybindComponent var0) {
      return var0.keybind();
   }

   static final class BuilderImpl implements ComponentFlattener.Builder {
      private final InheritanceAwareMap.Builder<Component, ComponentFlattenerImpl.Handler> flatteners;
      @Nullable
      private Function<Component, String> unknownHandler;

      BuilderImpl() {
         this.flatteners = InheritanceAwareMap.<Component, ComponentFlattenerImpl.Handler>builder().strict(true);
      }

      BuilderImpl(InheritanceAwareMap<Component, ComponentFlattenerImpl.Handler> var1, @Nullable Function<Component, String> var2) {
         this.flatteners = InheritanceAwareMap.<Component, ComponentFlattenerImpl.Handler>builder(var1).strict(true);
         this.unknownHandler = var2;
      }

      @NotNull
      public ComponentFlattener build() {
         return new ComponentFlattenerImpl(this.flatteners.build(), this.unknownHandler);
      }

      @Override
      public <T extends Component> ComponentFlattener.@NotNull Builder mapper(@NotNull Class<T> var1, @NotNull Function<T, String> var2) {
         this.flatteners.put(var1, (var1x, var2x, var3, var4) -> restoredLambda$mapper$0(var2, var1x, var2x, var3, var4));
         return this;
      }

      @Override
      public <T extends Component> ComponentFlattener.@NotNull Builder complexMapper(@NotNull Class<T> var1, @NotNull BiConsumer<T, Consumer<Component>> var2) {
         this.flatteners.put(var1, (var1x, var2x, var3, var4) -> restoredLambda$complexMapper$2(var2, var1x, var2x, var3, var4));
         return this;
      }

      @Override
      public ComponentFlattener.@NotNull Builder unknownMapper(@Nullable Function<Component, String> var1) {
         this.unknownHandler = var1;
         return this;
      }

      // $VF: synthetic method
      private static void restoredLambda$complexMapper$2(BiConsumer var0, ComponentFlattenerImpl var1, Component var2, FlattenerListener var3, int var4) {
         var0.accept(var2, (java.util.function.Consumer<Component>)(var3x -> restoredLambda$complexMapper$1(var1, var3, var4, var3x)));
      }

      // $VF: synthetic method
      private static void restoredLambda$complexMapper$1(ComponentFlattenerImpl var0, FlattenerListener var1, int var2, Component var3) {
         var0.flatten0(var3, var1, var2);
      }

      // $VF: synthetic method
      private static void restoredLambda$mapper$0(Function var0, ComponentFlattenerImpl var1, Component var2, FlattenerListener var3, int var4) {
         var3.component((String)var0.apply(var2));
      }
   }

   @FunctionalInterface
   interface Handler {
      void handle(ComponentFlattenerImpl var1, Component var2, FlattenerListener var3, int var4);
   }
}
