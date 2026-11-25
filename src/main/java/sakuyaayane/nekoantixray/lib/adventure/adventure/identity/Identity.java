package sakuyaayane.nekoantixray.lib.adventure.adventure.identity;

import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.key.Key;
import sakuyaayane.nekoantixray.lib.adventure.adventure.pointer.Pointer;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.examination.Examinable;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

public interface Identity extends Examinable, Identified {
   Pointer<String> NAME = Pointer.pointer(String.class, Key.key("adventure", "name"));
   Pointer<UUID> UUID = Pointer.pointer(UUID.class, Key.key("adventure", "uuid"));
   Pointer<Component> DISPLAY_NAME = Pointer.pointer(Component.class, Key.key("adventure", "display_name"));
   Pointer<Locale> LOCALE = Pointer.pointer(Locale.class, Key.key("adventure", "locale"));

   @NotNull
   static Identity nil() {
      return NilIdentity.INSTANCE;
   }

   @NotNull
   static Identity identity(@NotNull UUID var0) {
      return var0.equals(NilIdentity.NIL_UUID) ? NilIdentity.INSTANCE : new IdentityImpl(var0);
   }

   @NotNull
   UUID uuid();

   @NotNull
   @Override
   default Identity identity() {
      return this;
   }

   @NotNull
   @Override
   default Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.of(ExaminableProperty.of("uuid", this.uuid()));
   }
}
