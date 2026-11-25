package sakuyaayane.nekoantixray.lib.adventure.adventure.chat;

import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.key.Key;
import sakuyaayane.nekoantixray.lib.adventure.adventure.key.Keyed;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.ComponentLike;
import sakuyaayane.nekoantixray.lib.adventure.examination.Examinable;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

public interface ChatType extends Examinable, Keyed {
   ChatType CHAT = new ChatTypeImpl(Key.key("chat"));
   ChatType SAY_COMMAND = new ChatTypeImpl(Key.key("say_command"));
   ChatType MSG_COMMAND_INCOMING = new ChatTypeImpl(Key.key("msg_command_incoming"));
   ChatType MSG_COMMAND_OUTGOING = new ChatTypeImpl(Key.key("msg_command_outgoing"));
   ChatType TEAM_MSG_COMMAND_INCOMING = new ChatTypeImpl(Key.key("team_msg_command_incoming"));
   ChatType TEAM_MSG_COMMAND_OUTGOING = new ChatTypeImpl(Key.key("team_msg_command_outgoing"));
   ChatType EMOTE_COMMAND = new ChatTypeImpl(Key.key("emote_command"));

   @NotNull
   static ChatType chatType(@NotNull Keyed var0) {
      return var0 instanceof ChatType ? (ChatType)var0 : new ChatTypeImpl(Objects.requireNonNull(var0, "key").key());
   }

   @Contract(value = "_ -> new", pure = true)
   default ChatType.@NotNull Bound bind(@NotNull ComponentLike var1) {
      return this.bind(var1, null);
   }

   @Contract(value = "_, _ -> new", pure = true)
   default ChatType.@NotNull Bound bind(@NotNull ComponentLike var1, @Nullable ComponentLike var2) {
      return new ChatTypeImpl.BoundImpl(this, Objects.requireNonNull(var1.asComponent(), "name"), ComponentLike.unbox(var2));
   }

   @NotNull
   @Override
   default Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.of(ExaminableProperty.of("key", this.key()));
   }

   interface Bound extends Examinable {
      @Contract(pure = true)
      @NotNull
      ChatType type();

      @Contract(pure = true)
      @NotNull
      Component name();

      @Contract(pure = true)
      @Nullable
      Component target();

      @NotNull
      @Override
      default Stream<? extends ExaminableProperty> examinableProperties() {
         return Stream.of(
            ExaminableProperty.of("type", this.type()), ExaminableProperty.of("name", this.name()), ExaminableProperty.of("target", this.target())
         );
      }
   }
}
