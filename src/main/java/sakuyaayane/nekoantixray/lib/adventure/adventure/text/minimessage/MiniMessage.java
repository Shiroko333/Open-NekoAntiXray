package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import sakuyaayane.nekoantixray.lib.adventure.adventure.builder.AbstractBuilder;
import sakuyaayane.nekoantixray.lib.adventure.adventure.pointer.Pointered;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tree.Node;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.serializer.ComponentSerializer;
import sakuyaayane.nekoantixray.lib.adventure.adventure.util.PlatformAPI;

public interface MiniMessage extends ComponentSerializer<Component, Component, String> {
   @NotNull
   static MiniMessage miniMessage() {
      return MiniMessageImpl.Instances.INSTANCE;
   }

   @NotNull
   String escapeTags(@NotNull String var1);

   @NotNull
   String escapeTags(@NotNull String var1, @NotNull TagResolver var2);

   @NotNull
   default String escapeTags(@NotNull String var1, @NotNull TagResolver... var2) {
      return this.escapeTags(var1, TagResolver.resolver(var2));
   }

   @NotNull
   String stripTags(@NotNull String var1);

   @NotNull
   String stripTags(@NotNull String var1, @NotNull TagResolver var2);

   @NotNull
   default String stripTags(@NotNull String var1, @NotNull TagResolver... var2) {
      return this.stripTags(var1, TagResolver.resolver(var2));
   }

   @NotNull
   Component deserialize(@NotNull String var1, @NotNull Pointered var2);

   @NotNull
   Component deserialize(@NotNull String var1, @NotNull TagResolver var2);

   @NotNull
   Component deserialize(@NotNull String var1, @NotNull Pointered var2, @NotNull TagResolver var3);

   @NotNull
   default Component deserialize(@NotNull String var1, @NotNull TagResolver... var2) {
      return this.deserialize(var1, TagResolver.resolver(var2));
   }

   @NotNull
   default Component deserialize(@NotNull String var1, @NotNull Pointered var2, @NotNull TagResolver... var3) {
      return this.deserialize(var1, var2, TagResolver.resolver(var3));
   }

   Node.@NotNull Root deserializeToTree(@NotNull String var1);

   Node.@NotNull Root deserializeToTree(@NotNull String var1, @NotNull Pointered var2);

   Node.@NotNull Root deserializeToTree(@NotNull String var1, @NotNull TagResolver var2);

   Node.@NotNull Root deserializeToTree(@NotNull String var1, @NotNull Pointered var2, @NotNull TagResolver var3);

   default Node.@NotNull Root deserializeToTree(@NotNull String var1, @NotNull TagResolver... var2) {
      return this.deserializeToTree(var1, TagResolver.resolver(var2));
   }

   default Node.@NotNull Root deserializeToTree(@NotNull String var1, @NotNull Pointered var2, @NotNull TagResolver... var3) {
      return this.deserializeToTree(var1, var2, TagResolver.resolver(var3));
   }

   boolean strict();

   @NotNull
   TagResolver tags();

   static MiniMessage.Builder builder() {
      return new MiniMessageImpl.BuilderImpl();
   }

   interface Builder extends AbstractBuilder<MiniMessage> {
      @NotNull
      MiniMessage.Builder tags(@NotNull TagResolver var1);

      @NotNull
      MiniMessage.Builder editTags(@NotNull Consumer<TagResolver.Builder> var1);

      @NotNull
      MiniMessage.Builder strict(boolean var1);

      @NotNull
      MiniMessage.Builder debug(@Nullable Consumer<String> var1);

      @NotNull
      MiniMessage.Builder postProcessor(@NotNull UnaryOperator<Component> var1);

      @NotNull
      MiniMessage.Builder preProcessor(@NotNull UnaryOperator<String> var1);

      @NotNull
      MiniMessage build();
   }

   @PlatformAPI
   @Internal
   interface Provider {
      @PlatformAPI
      @Internal
      @NotNull
      MiniMessage miniMessage();

      @PlatformAPI
      @Internal
      @NotNull
      Consumer<MiniMessage.Builder> builder();
   }
}
