package sakuyaayane.nekoantixray.lib.adventure.adventure.audience;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import sakuyaayane.nekoantixray.lib.adventure.adventure.chat.ChatType;
import sakuyaayane.nekoantixray.lib.adventure.adventure.chat.SignedMessage;
import sakuyaayane.nekoantixray.lib.adventure.adventure.identity.Identified;
import sakuyaayane.nekoantixray.lib.adventure.adventure.identity.Identity;
import sakuyaayane.nekoantixray.lib.adventure.adventure.inventory.Book;
import sakuyaayane.nekoantixray.lib.adventure.adventure.pointer.Pointer;
import sakuyaayane.nekoantixray.lib.adventure.adventure.resource.ResourcePackInfoLike;
import sakuyaayane.nekoantixray.lib.adventure.adventure.resource.ResourcePackRequest;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.ComponentLike;

final class EmptyAudience implements Audience {
   static final EmptyAudience INSTANCE = new EmptyAudience();

   @NotNull
   @Override
   public <T> Optional<T> get(@NotNull Pointer<T> var1) {
      return Optional.empty();
   }

   @Contract("_, null -> null; _, !null -> !null")
   @Nullable
   @Override
   public <T> T getOrDefault(@NotNull Pointer<T> var1, @Nullable T var2) {
      return (T)var2;
   }

   @Override
   public <T> @UnknownNullability T getOrDefaultFrom(@NotNull Pointer<T> var1, @NotNull Supplier<? extends T> var2) {
      return (T)var2.get();
   }

   @NotNull
   @Override
   public Audience filterAudience(@NotNull Predicate<? super Audience> var1) {
      return this;
   }

   @Override
   public void forEachAudience(@NotNull Consumer<? super Audience> var1) {
   }

   @Override
   public void sendMessage(@NotNull ComponentLike var1) {
   }

   @Override
   public void sendMessage(@NotNull Component var1) {
   }

   @Deprecated
   @Override
   public void sendMessage(@NotNull Identified var1, @NotNull Component var2, @NotNull MessageType var3) {
   }

   @Deprecated
   @Override
   public void sendMessage(@NotNull Identity var1, @NotNull Component var2, @NotNull MessageType var3) {
   }

   @Override
   public void sendMessage(@NotNull Component var1, ChatType.@NotNull Bound var2) {
   }

   @Override
   public void sendMessage(@NotNull SignedMessage var1, ChatType.@NotNull Bound var2) {
   }

   @Override
   public void deleteMessage(SignedMessage.@NotNull Signature var1) {
   }

   @Override
   public void sendActionBar(@NotNull ComponentLike var1) {
   }

   @Override
   public void sendPlayerListHeader(@NotNull ComponentLike var1) {
   }

   @Override
   public void sendPlayerListFooter(@NotNull ComponentLike var1) {
   }

   @Override
   public void sendPlayerListHeaderAndFooter(@NotNull ComponentLike var1, @NotNull ComponentLike var2) {
   }

   @Override
   public void openBook(Book.@NotNull Builder var1) {
   }

   @Override
   public void sendResourcePacks(@NotNull ResourcePackInfoLike var1, @NotNull ResourcePackInfoLike... var2) {
   }

   @Override
   public void removeResourcePacks(@NotNull ResourcePackRequest var1) {
   }

   @Override
   public void removeResourcePacks(@NotNull ResourcePackInfoLike var1, @NotNull ResourcePackInfoLike... var2) {
   }

   @Override
   public boolean equals(Object var1) {
      return this == var1;
   }

   @Override
   public int hashCode() {
      return 0;
   }

   @Override
   public String toString() {
      return "EmptyAudience";
   }
}
