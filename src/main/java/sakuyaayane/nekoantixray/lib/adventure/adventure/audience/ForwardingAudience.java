package sakuyaayane.nekoantixray.lib.adventure.adventure.audience;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import sakuyaayane.nekoantixray.lib.adventure.adventure.bossbar.BossBar;
import sakuyaayane.nekoantixray.lib.adventure.adventure.chat.ChatType;
import sakuyaayane.nekoantixray.lib.adventure.adventure.chat.SignedMessage;
import sakuyaayane.nekoantixray.lib.adventure.adventure.identity.Identified;
import sakuyaayane.nekoantixray.lib.adventure.adventure.identity.Identity;
import sakuyaayane.nekoantixray.lib.adventure.adventure.inventory.Book;
import sakuyaayane.nekoantixray.lib.adventure.adventure.pointer.Pointer;
import sakuyaayane.nekoantixray.lib.adventure.adventure.pointer.Pointers;
import sakuyaayane.nekoantixray.lib.adventure.adventure.resource.ResourcePackRequest;
import sakuyaayane.nekoantixray.lib.adventure.adventure.sound.Sound;
import sakuyaayane.nekoantixray.lib.adventure.adventure.sound.SoundStop;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.title.TitlePart;

@FunctionalInterface
public interface ForwardingAudience extends Audience {
   @OverrideOnly
   @NotNull
   Iterable<? extends Audience> audiences();

   @NotNull
   @Override
   default Pointers pointers() {
      return Pointers.empty();
   }

   @NotNull
   @Override
   default Audience filterAudience(@NotNull Predicate<? super Audience> var1) {
      ArrayList var2 = null;

      for (Audience var4 : this.audiences()) {
         if (var1.test(var4)) {
            Audience var5 = var4.filterAudience(var1);
            if (var5 != Audience.empty()) {
               if (var2 == null) {
                  var2 = new ArrayList();
               }

               var2.add(var5);
            }
         }
      }

      return var2 != null ? Audience.audience(var2) : Audience.empty();
   }

   @Override
   default void forEachAudience(@NotNull Consumer<? super Audience> var1) {
      for (Audience var3 : this.audiences()) {
         var3.forEachAudience(var1);
      }
   }

   @Override
   default void sendMessage(@NotNull Component var1) {
      for (Audience var3 : this.audiences()) {
         var3.sendMessage(var1);
      }
   }

   @Override
   default void sendMessage(@NotNull Component var1, ChatType.@NotNull Bound var2) {
      for (Audience var4 : this.audiences()) {
         var4.sendMessage(var1, var2);
      }
   }

   @Override
   default void sendMessage(@NotNull SignedMessage var1, ChatType.@NotNull Bound var2) {
      for (Audience var4 : this.audiences()) {
         var4.sendMessage(var1, var2);
      }
   }

   @Override
   default void deleteMessage(SignedMessage.@NotNull Signature var1) {
      for (Audience var3 : this.audiences()) {
         var3.deleteMessage(var1);
      }
   }

   @Deprecated
   @Override
   default void sendMessage(@NotNull Identified var1, @NotNull Component var2, @NotNull MessageType var3) {
      for (Audience var5 : this.audiences()) {
         var5.sendMessage(var1, var2, var3);
      }
   }

   @Deprecated
   @Override
   default void sendMessage(@NotNull Identity var1, @NotNull Component var2, @NotNull MessageType var3) {
      for (Audience var5 : this.audiences()) {
         var5.sendMessage(var1, var2, var3);
      }
   }

   @Override
   default void sendActionBar(@NotNull Component var1) {
      for (Audience var3 : this.audiences()) {
         var3.sendActionBar(var1);
      }
   }

   @Override
   default void sendPlayerListHeader(@NotNull Component var1) {
      for (Audience var3 : this.audiences()) {
         var3.sendPlayerListHeader(var1);
      }
   }

   @Override
   default void sendPlayerListFooter(@NotNull Component var1) {
      for (Audience var3 : this.audiences()) {
         var3.sendPlayerListFooter(var1);
      }
   }

   @Override
   default void sendPlayerListHeaderAndFooter(@NotNull Component var1, @NotNull Component var2) {
      for (Audience var4 : this.audiences()) {
         var4.sendPlayerListHeaderAndFooter(var1, var2);
      }
   }

   @Override
   default <T> void sendTitlePart(@NotNull TitlePart<T> var1, @NotNull T var2) {
      for (Audience var4 : this.audiences()) {
         var4.sendTitlePart(var1, var2);
      }
   }

   @Override
   default void clearTitle() {
      for (Audience var2 : this.audiences()) {
         var2.clearTitle();
      }
   }

   @Override
   default void resetTitle() {
      for (Audience var2 : this.audiences()) {
         var2.resetTitle();
      }
   }

   @Override
   default void showBossBar(@NotNull BossBar var1) {
      for (Audience var3 : this.audiences()) {
         var3.showBossBar(var1);
      }
   }

   @Override
   default void hideBossBar(@NotNull BossBar var1) {
      for (Audience var3 : this.audiences()) {
         var3.hideBossBar(var1);
      }
   }

   @Override
   default void playSound(@NotNull Sound var1) {
      for (Audience var3 : this.audiences()) {
         var3.playSound(var1);
      }
   }

   @Override
   default void playSound(@NotNull Sound var1, double var2, double var4, double var6) {
      for (Audience var9 : this.audiences()) {
         var9.playSound(var1, var2, var4, var6);
      }
   }

   @Override
   default void playSound(@NotNull Sound var1, Sound.@NotNull Emitter var2) {
      for (Audience var4 : this.audiences()) {
         var4.playSound(var1, var2);
      }
   }

   @Override
   default void stopSound(@NotNull SoundStop var1) {
      for (Audience var3 : this.audiences()) {
         var3.stopSound(var1);
      }
   }

   @Override
   default void openBook(@NotNull Book var1) {
      for (Audience var3 : this.audiences()) {
         var3.openBook(var1);
      }
   }

   @Override
   default void sendResourcePacks(@NotNull ResourcePackRequest var1) {
      for (Audience var3 : this.audiences()) {
         var3.sendResourcePacks(var1);
      }
   }

   @Override
   default void removeResourcePacks(@NotNull Iterable<UUID> var1) {
      for (Audience var3 : this.audiences()) {
         var3.removeResourcePacks(var1);
      }
   }

   @Override
   default void removeResourcePacks(@NotNull UUID var1, @NotNull UUID... var2) {
      for (Audience var4 : this.audiences()) {
         var4.removeResourcePacks(var1, var2);
      }
   }

   @Override
   default void clearResourcePacks() {
      for (Audience var2 : this.audiences()) {
         var2.clearResourcePacks();
      }
   }

   interface Single extends ForwardingAudience {
      @OverrideOnly
      @NotNull
      Audience audience();

      @Deprecated
      @NotNull
      @Override
      default Iterable<? extends Audience> audiences() {
         return Collections.singleton(this.audience());
      }

      @NotNull
      @Override
      default <T> Optional<T> get(@NotNull Pointer<T> var1) {
         return this.audience().get(var1);
      }

      @Contract("_, null -> null; _, !null -> !null")
      @Nullable
      @Override
      default <T> T getOrDefault(@NotNull Pointer<T> var1, @Nullable T var2) {
         return this.audience().getOrDefault(var1, (T)var2);
      }

      @Override
      default <T> @UnknownNullability T getOrDefaultFrom(@NotNull Pointer<T> var1, @NotNull Supplier<? extends T> var2) {
         return this.audience().getOrDefaultFrom(var1, var2);
      }

      @NotNull
      @Override
      default Audience filterAudience(@NotNull Predicate<? super Audience> var1) {
         Audience var2 = this.audience();
         return var1.test(var2) ? this : Audience.empty();
      }

      @Override
      default void forEachAudience(@NotNull Consumer<? super Audience> var1) {
         this.audience().forEachAudience(var1);
      }

      @NotNull
      @Override
      default Pointers pointers() {
         return this.audience().pointers();
      }

      @Override
      default void sendMessage(@NotNull Component var1) {
         this.audience().sendMessage(var1);
      }

      @Override
      default void sendMessage(@NotNull Component var1, ChatType.@NotNull Bound var2) {
         this.audience().sendMessage(var1, var2);
      }

      @Override
      default void sendMessage(@NotNull SignedMessage var1, ChatType.@NotNull Bound var2) {
         this.audience().sendMessage(var1, var2);
      }

      @Override
      default void deleteMessage(SignedMessage.@NotNull Signature var1) {
         this.audience().deleteMessage(var1);
      }

      @Deprecated
      @Override
      default void sendMessage(@NotNull Identified var1, @NotNull Component var2, @NotNull MessageType var3) {
         this.audience().sendMessage(var1, var2, var3);
      }

      @Deprecated
      @Override
      default void sendMessage(@NotNull Identity var1, @NotNull Component var2, @NotNull MessageType var3) {
         this.audience().sendMessage(var1, var2, var3);
      }

      @Override
      default void sendActionBar(@NotNull Component var1) {
         this.audience().sendActionBar(var1);
      }

      @Override
      default void sendPlayerListHeader(@NotNull Component var1) {
         this.audience().sendPlayerListHeader(var1);
      }

      @Override
      default void sendPlayerListFooter(@NotNull Component var1) {
         this.audience().sendPlayerListFooter(var1);
      }

      @Override
      default void sendPlayerListHeaderAndFooter(@NotNull Component var1, @NotNull Component var2) {
         this.audience().sendPlayerListHeaderAndFooter(var1, var2);
      }

      @Override
      default <T> void sendTitlePart(@NotNull TitlePart<T> var1, @NotNull T var2) {
         this.audience().sendTitlePart(var1, var2);
      }

      @Override
      default void clearTitle() {
         this.audience().clearTitle();
      }

      @Override
      default void resetTitle() {
         this.audience().resetTitle();
      }

      @Override
      default void showBossBar(@NotNull BossBar var1) {
         this.audience().showBossBar(var1);
      }

      @Override
      default void hideBossBar(@NotNull BossBar var1) {
         this.audience().hideBossBar(var1);
      }

      @Override
      default void playSound(@NotNull Sound var1) {
         this.audience().playSound(var1);
      }

      @Override
      default void playSound(@NotNull Sound var1, double var2, double var4, double var6) {
         this.audience().playSound(var1, var2, var4, var6);
      }

      @Override
      default void playSound(@NotNull Sound var1, Sound.@NotNull Emitter var2) {
         this.audience().playSound(var1, var2);
      }

      @Override
      default void stopSound(@NotNull SoundStop var1) {
         this.audience().stopSound(var1);
      }

      @Override
      default void openBook(@NotNull Book var1) {
         this.audience().openBook(var1);
      }

      @Override
      default void sendResourcePacks(@NotNull ResourcePackRequest var1) {
         this.audience().sendResourcePacks(var1.callback(Audiences.unwrapCallback(this, this.audience(), var1.callback())));
      }

      @Override
      default void removeResourcePacks(@NotNull Iterable<UUID> var1) {
         this.audience().removeResourcePacks(var1);
      }

      @Override
      default void removeResourcePacks(@NotNull UUID var1, @NotNull UUID... var2) {
         this.audience().removeResourcePacks(var1, var2);
      }

      @Override
      default void clearResourcePacks() {
         this.audience().clearResourcePacks();
      }
   }
}
