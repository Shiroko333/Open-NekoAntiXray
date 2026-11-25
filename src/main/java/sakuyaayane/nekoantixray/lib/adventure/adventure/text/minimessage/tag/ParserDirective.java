package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag;

import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public interface ParserDirective extends Tag {
   Tag RESET = new ParserDirective() {
      @Override
      public String toString() {
         return "ParserDirective.RESET";
      }
   };
}
