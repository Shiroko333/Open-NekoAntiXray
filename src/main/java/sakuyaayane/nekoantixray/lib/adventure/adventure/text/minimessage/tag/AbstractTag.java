package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag;

import sakuyaayane.nekoantixray.lib.adventure.adventure.internal.Internals;
import sakuyaayane.nekoantixray.lib.adventure.examination.Examinable;

abstract class AbstractTag implements Tag, Examinable {
   @Override
   public final String toString() {
      return Internals.toString(this);
   }
}
