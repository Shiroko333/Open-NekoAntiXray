package sakuyaayane.nekoantixray.lib.adventure.adventure.text.event;

import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.nbt.api.BinaryTagHolder;
import sakuyaayane.nekoantixray.lib.adventure.examination.Examinable;

public interface DataComponentValue extends Examinable {
   static DataComponentValue.@NotNull Removed removed() {
      return RemovedDataComponentValueImpl.REMOVED;
   }

   interface Removed extends DataComponentValue {
   }

   interface TagSerializable extends DataComponentValue {
      @NotNull
      BinaryTagHolder asBinaryTag();
   }
}
