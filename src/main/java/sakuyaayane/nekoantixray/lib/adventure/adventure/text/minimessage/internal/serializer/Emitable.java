package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Emitable {
   void emit(@NotNull TokenEmitter var1);
}
