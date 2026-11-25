package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.pointer.Pointered;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.ParsingExceptionImpl;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.Token;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.node.TagPart;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;

class ContextImpl implements Context {
   private static final Token[] EMPTY_TOKEN_ARRAY = new Token[0];
   private final boolean strict;
   private final Consumer<String> debugOutput;
   private String message;
   private final MiniMessage miniMessage;
   @Nullable
   private final Pointered target;
   private final TagResolver tagResolver;
   private final UnaryOperator<String> preProcessor;
   private final UnaryOperator<Component> postProcessor;

   ContextImpl(
      boolean var1,
      Consumer<String> var2,
      String var3,
      MiniMessage var4,
      @Nullable Pointered var5,
      @Nullable TagResolver var6,
      @Nullable UnaryOperator<String> var7,
      @Nullable UnaryOperator<Component> var8
   ) {
      this.strict = var1;
      this.debugOutput = var2;
      this.message = var3;
      this.miniMessage = var4;
      this.target = var5;
      this.tagResolver = var6 == null ? TagResolver.empty() : var6;
      this.preProcessor = var7 == null ? UnaryOperator.identity() : var7;
      this.postProcessor = var8 == null ? UnaryOperator.identity() : var8;
   }

   public boolean strict() {
      return this.strict;
   }

   public Consumer<String> debugOutput() {
      return this.debugOutput;
   }

   @NotNull
   public String message() {
      return this.message;
   }

   void message(@NotNull String var1) {
      this.message = var1;
   }

   @NotNull
   public TagResolver extraTags() {
      return this.tagResolver;
   }

   public UnaryOperator<Component> postProcessor() {
      return this.postProcessor;
   }

   public UnaryOperator<String> preProcessor() {
      return this.preProcessor;
   }

   @Nullable
   @Override
   public Pointered target() {
      return this.target;
   }

   @NotNull
   @Override
   public Pointered targetOrThrow() {
      return this.target;
   }

   @NotNull
   @Override
   public <T extends Pointered> T targetAsType(@NotNull Class<T> var1) {
      if (Objects.requireNonNull(var1, "targetClass").isInstance(this.target)) {
         return (T)var1.cast(this.target);
      } else {
         throw this.newException("A target with type " + var1.getSimpleName() + " is required for this deserialization attempt");
      }
   }

   @NotNull
   @Override
   public Component deserialize(@NotNull String var1) {
      return this.miniMessage.deserialize(Objects.requireNonNull(var1, "message"), this.tagResolver);
   }

   @NotNull
   @Override
   public Component deserialize(@NotNull String var1, @NotNull TagResolver var2) {
      return this.miniMessage
         .deserialize(
            Objects.requireNonNull(var1, "message"),
            TagResolver.builder().resolver(this.tagResolver).resolver(Objects.requireNonNull(var2, "resolver")).build()
         );
   }

   @NotNull
   @Override
   public Component deserialize(@NotNull String var1, @NotNull TagResolver... var2) {
      return this.miniMessage
         .deserialize(
            Objects.requireNonNull(var1, "message"),
            TagResolver.builder().resolver(this.tagResolver).resolvers(Objects.requireNonNull(var2, "resolvers")).build()
         );
   }

   @NotNull
   @Override
   public ParsingException newException(@NotNull String var1) {
      return new ParsingExceptionImpl(var1, this.message, null, false, EMPTY_TOKEN_ARRAY);
   }

   @NotNull
   @Override
   public ParsingException newException(@NotNull String var1, @NotNull ArgumentQueue var2) {
      return new ParsingExceptionImpl(var1, this.message, null, false, tagsToTokens(((ArgumentQueueImpl)var2).args));
   }

   @NotNull
   @Override
   public ParsingException newException(@NotNull String var1, @Nullable Throwable var2, @NotNull ArgumentQueue var3) {
      return new ParsingExceptionImpl(var1, this.message, var2, false, tagsToTokens(((ArgumentQueueImpl)var3).args));
   }

   private static Token[] tagsToTokens(List<? extends Tag.Argument> var0) {
      Token[] var1 = new Token[var0.size()];
      int var2 = 0;

      for (int var3 = var1.length; var2 < var3; var2++) {
         var1[var2] = ((TagPart)var0.get(var2)).token();
      }

      return var1;
   }
}
