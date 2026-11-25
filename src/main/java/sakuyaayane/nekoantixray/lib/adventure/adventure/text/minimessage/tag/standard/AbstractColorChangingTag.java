package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.standard;

import java.util.Collections;
import java.util.PrimitiveIterator.OfInt;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.internal.Internals;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.TextComponent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.flattener.ComponentFlattener;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.TextColor;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.node.TagNode;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.parser.node.ValueNode;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Inserting;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Modifying;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tree.Node;
import sakuyaayane.nekoantixray.lib.adventure.examination.Examinable;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

abstract class AbstractColorChangingTag implements Modifying, Examinable {
   private static final ComponentFlattener LENGTH_CALCULATOR = ComponentFlattener.builder()
      .mapper(TextComponent.class, var0 -> var0.content())
      .unknownMapper(var0 -> restoredLambda$static$0(var0))
      .build();
   private boolean visited;
   private int size = 0;
   private int disableApplyingColorDepth = -1;

   protected final int size() {
      return this.size;
   }

   @Override
   public final void visit(@NotNull Node var1, int var2) {
      if (this.visited) {
         throw new IllegalStateException("Color changing tag instances cannot be re-used, return a new one for each resolve");
      }

      if (var1 instanceof ValueNode) {
         String var3 = ((ValueNode)var1).value();
         this.size = this.size + var3.codePointCount(0, var3.length());
      } else if (var1 instanceof TagNode) {
         TagNode var4 = (TagNode)var1;
         if (var4.tag() instanceof Inserting) {
            LENGTH_CALCULATOR.flatten(((Inserting)var4.tag()).value(), var1x -> this.restoredLambda$visit$1(var1x));
         }
      }
   }

   @Override
   public final void postVisit() {
      this.visited = true;
      this.init();
   }

   @Override
   public final Component apply(@NotNull Component var1, int var2) {
      if ((this.disableApplyingColorDepth == -1 || var2 <= this.disableApplyingColorDepth) && var1.style().color() == null) {
         this.disableApplyingColorDepth = -1;
         if (var1 instanceof TextComponent && ((TextComponent)var1).content().length() > 0) {
            TextComponent var10 = (TextComponent)var1;
            String var11 = var10.content();
            TextComponent.Builder var12 = Component.text();
            int[] var6 = new int[1];
            OfInt var7 = var11.codePoints().iterator();

            while (var7.hasNext()) {
               var6[0] = var7.nextInt();
               TextComponent var8 = Component.text(new String(var6, 0, 1), var1.style().color(this.color()));
               this.advanceColor();
               var12.append(var8);
            }

            return var12.build();
         } else if (!(var1 instanceof TextComponent)) {
            Component var9 = var1.children(Collections.emptyList()).colorIfAbsent(this.color());
            this.advanceColor();
            return var9;
         } else {
            return Component.empty().mergeStyle(var1);
         }
      } else {
         if (this.disableApplyingColorDepth == -1 || var2 < this.disableApplyingColorDepth) {
            this.disableApplyingColorDepth = var2;
         }

         if (var1 instanceof TextComponent) {
            String var3 = ((TextComponent)var1).content();
            int var4 = var3.codePointCount(0, var3.length());

            for (int var5 = 0; var5 < var4; var5++) {
               this.advanceColor();
            }
         }

         return var1.children(Collections.emptyList());
      }
   }

   protected abstract void init();

   protected abstract void advanceColor();

   protected abstract TextColor color();

   @NotNull
   @Override
   public abstract Stream<? extends ExaminableProperty> examinableProperties();

   @NotNull
   @Override
   public final String toString() {
      return Internals.toString(this);
   }

   @Override
   public abstract boolean equals(@Nullable Object var1);

   @Override
   public abstract int hashCode();

   // $VF: synthetic method
   private void restoredLambda$visit$1(String var1) {
      this.size = this.size + var1.codePointCount(0, var1.length());
   }

   // $VF: synthetic method
   private static String restoredLambda$static$0(Component var0) {
      return "_";
   }
}
