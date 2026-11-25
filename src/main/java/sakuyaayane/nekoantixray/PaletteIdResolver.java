package sakuyaayane.nekoantixray;

import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PaletteResize;

final class PaletteIdResolver {
   private PaletteIdResolver() {
   }

   static int resolveId(Palette palette, Object entry) {
      return palette.idFor(entry, PaletteResize.noResizeExpected());
   }
}
