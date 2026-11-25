package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.pointer.Pointered;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.SerializableResolver;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tree.Node;
import sakuyaayane.nekoantixray.lib.adventure.adventure.util.Services;

final class MiniMessageImpl implements MiniMessage {
   private static final Optional<MiniMessage.Provider> SERVICE = Services.service(MiniMessage.Provider.class);
   static final Consumer<MiniMessage.Builder> BUILDER = SERVICE.<Consumer<MiniMessage.Builder>>map(var0 -> var0.builder()).orElseGet(() -> restoredLambda$static$1());
   static final UnaryOperator<String> DEFAULT_NO_OP = UnaryOperator.identity();
   static final UnaryOperator<Component> DEFAULT_COMPACTING_METHOD = var0 -> ((Component)var0).compact();
   private final boolean strict;
   @Nullable
   private final Consumer<String> debugOutput;
   private final UnaryOperator<Component> postProcessor;
   private final UnaryOperator<String> preProcessor;
   final MiniMessageParser parser;

   MiniMessageImpl(
      @NotNull TagResolver var1, boolean var2, @Nullable Consumer<String> var3, @NotNull UnaryOperator<String> var4, @NotNull UnaryOperator<Component> var5
   ) {
      this.parser = new MiniMessageParser(var1);
      this.strict = var2;
      this.debugOutput = var3;
      this.preProcessor = var4;
      this.postProcessor = var5;
   }

   @NotNull
   public Component deserialize(@NotNull String var1) {
      return this.parser.parseFormat(this.newContext(var1, null, null));
   }

   @NotNull
   @Override
   public Component deserialize(@NotNull String var1, @NotNull Pointered var2) {
      return this.parser.parseFormat(this.newContext(var1, Objects.requireNonNull(var2, "target"), null));
   }

   @NotNull
   @Override
   public Component deserialize(@NotNull String var1, @NotNull TagResolver var2) {
      return this.parser.parseFormat(this.newContext(var1, null, Objects.requireNonNull(var2, "tagResolver")));
   }

   @NotNull
   @Override
   public Component deserialize(@NotNull String var1, @NotNull Pointered var2, @NotNull TagResolver var3) {
      return this.parser.parseFormat(this.newContext(var1, Objects.requireNonNull(var2, "target"), Objects.requireNonNull(var3, "tagResolver")));
   }

   @Override
   public Node.@NotNull Root deserializeToTree(@NotNull String var1) {
      return this.parser.parseToTree(this.newContext(var1, null, null));
   }

   @Override
   public Node.@NotNull Root deserializeToTree(@NotNull String var1, @NotNull Pointered var2) {
      return this.parser.parseToTree(this.newContext(var1, Objects.requireNonNull(var2, "target"), null));
   }

   @Override
   public Node.@NotNull Root deserializeToTree(@NotNull String var1, @NotNull TagResolver var2) {
      return this.parser.parseToTree(this.newContext(var1, null, Objects.requireNonNull(var2, "tagResolver")));
   }

   @Override
   public Node.@NotNull Root deserializeToTree(@NotNull String var1, @NotNull Pointered var2, @NotNull TagResolver var3) {
      return this.parser.parseToTree(this.newContext(var1, Objects.requireNonNull(var2, "target"), Objects.requireNonNull(var3, "tagResolver")));
   }

   @NotNull
   public String serialize(@NotNull Component var1) {
      return MiniMessageSerializer.serialize(var1, this.serialResolver(null), this.strict);
   }

   private SerializableResolver serialResolver(@Nullable TagResolver var1) {
      if (var1 == null) {
         if (this.parser.tagResolver instanceof SerializableResolver) {
            return (SerializableResolver)this.parser.tagResolver;
         }
      } else {
         TagResolver var2 = TagResolver.resolver(this.parser.tagResolver, var1);
         if (var2 instanceof SerializableResolver) {
            return (SerializableResolver)var2;
         }
      }

      return (SerializableResolver)TagResolver.empty();
   }

   @NotNull
   @Override
   public String escapeTags(@NotNull String var1) {
      return this.parser.escapeTokens(this.newContext(var1, null, null));
   }

   @NotNull
   @Override
   public String escapeTags(@NotNull String var1, @NotNull TagResolver var2) {
      return this.parser.escapeTokens(this.newContext(var1, null, var2));
   }

   @NotNull
   @Override
   public String stripTags(@NotNull String var1) {
      return this.parser.stripTokens(this.newContext(var1, null, null));
   }

   @NotNull
   @Override
   public String stripTags(@NotNull String var1, @NotNull TagResolver var2) {
      return this.parser.stripTokens(this.newContext(var1, null, var2));
   }

   @Override
   public boolean strict() {
      return this.strict;
   }

   @NotNull
   @Override
   public TagResolver tags() {
      return this.parser.tagResolver;
   }

   @NotNull
   private ContextImpl newContext(@NotNull String var1, @Nullable Pointered var2, @Nullable TagResolver var3) {
      Objects.requireNonNull(var1, "input");
      return new ContextImpl(this.strict, this.debugOutput, var1, this, var2, var3, this.preProcessor, this.postProcessor);
   }

   // $VF: synthetic method
   private static Consumer restoredLambda$static$1() {
      return var0 -> restoredLambda$static$0((sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.MiniMessage.Builder)(var0));
   }

   // $VF: synthetic method
   private static void restoredLambda$static$0(MiniMessage.Builder var0) {
   }

   static final class BuilderImpl implements MiniMessage.Builder {
      private TagResolver tagResolver = TagResolver.standard();
      private boolean strict = false;
      private Consumer<String> debug = null;
      private UnaryOperator<Component> postProcessor = MiniMessageImpl.DEFAULT_COMPACTING_METHOD;
      private UnaryOperator<String> preProcessor = MiniMessageImpl.DEFAULT_NO_OP;

      BuilderImpl() {
         MiniMessageImpl.BUILDER.accept(this);
      }

      BuilderImpl(MiniMessageImpl var1) {
         this();
         this.tagResolver = var1.parser.tagResolver;
         this.strict = var1.strict;
         this.debug = var1.debugOutput;
         this.postProcessor = var1.postProcessor;
         this.preProcessor = var1.preProcessor;
      }

      @NotNull
      @Override
      public MiniMessage.Builder tags(@NotNull TagResolver var1) {
         this.tagResolver = Objects.requireNonNull(var1, "tags");
         return this;
      }

      @NotNull
      @Override
      public MiniMessage.Builder editTags(@NotNull Consumer<TagResolver.Builder> var1) {
         Objects.requireNonNull(var1, "adder");
         TagResolver.Builder var2 = TagResolver.builder().resolver(this.tagResolver);
         var1.accept(var2);
         this.tagResolver = var2.build();
         return this;
      }

      @NotNull
      @Override
      public MiniMessage.Builder strict(boolean var1) {
         this.strict = var1;
         return this;
      }

      @NotNull
      @Override
      public MiniMessage.Builder debug(@Nullable Consumer<String> var1) {
         this.debug = var1;
         return this;
      }

      @NotNull
      @Override
      public MiniMessage.Builder postProcessor(@NotNull UnaryOperator<Component> var1) {
         this.postProcessor = Objects.requireNonNull(var1, "postProcessor");
         return this;
      }

      @NotNull
      @Override
      public MiniMessage.Builder preProcessor(@NotNull UnaryOperator<String> var1) {
         this.preProcessor = Objects.requireNonNull(var1, "preProcessor");
         return this;
      }

      @NotNull
      @Override
      public MiniMessage build() {
         return new MiniMessageImpl(this.tagResolver, this.strict, this.debug, this.preProcessor, this.postProcessor);
      }
   }

   static final class Instances {
      static final MiniMessage INSTANCE = MiniMessageImpl.SERVICE.map(var0 -> var0.miniMessage()).orElseGet(() -> restoredLambda$static$0());

      // $VF: synthetic method
      private static MiniMessage restoredLambda$static$0() {
         return new MiniMessageImpl(TagResolver.standard(), false, null, MiniMessageImpl.DEFAULT_NO_OP, MiniMessageImpl.DEFAULT_COMPACTING_METHOD);
      }
   }
}
