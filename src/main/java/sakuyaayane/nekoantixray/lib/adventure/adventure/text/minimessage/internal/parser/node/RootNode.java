package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.node;

import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tree.Node;

public final class RootNode extends ElementNode implements Node.Root {
   private final String beforePreprocessing;

   public RootNode(@NotNull String var1, @NotNull String var2) {
      super(null, null, var1);
      this.beforePreprocessing = var2;
   }

   @NotNull
   @Override
   public String input() {
      return this.beforePreprocessing;
   }
}
