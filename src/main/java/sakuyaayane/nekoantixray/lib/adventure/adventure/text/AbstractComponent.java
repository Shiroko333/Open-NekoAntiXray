package sakuyaayane.nekoantixray.lib.adventure.adventure.text;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.Debug.Renderer;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.Style;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;
import sakuyaayane.nekoantixray.lib.adventure.examination.string.StringExaminer;

@Deprecated
@ScheduledForRemoval(inVersion = "5.0.0")
@Renderer(text = "this.debuggerString()", childrenArray = "this.children().toArray()", hasChildren = "!this.children().isEmpty()")
public abstract class AbstractComponent implements Component {
   protected final List<Component> children;
   protected final Style style;

   protected AbstractComponent(@NotNull List<? extends ComponentLike> var1, @NotNull Style var2) {
      this.children = ComponentLike.asComponents(var1, IS_NOT_EMPTY);
      this.style = var2;
   }

   @NotNull
   @Override
   public final List<Component> children() {
      return this.children;
   }

   @NotNull
   @Override
   public final Style style() {
      return this.style;
   }

   @Override
   public boolean equals(@Nullable Object var1) {
      if (this == var1) {
         return true;
      }

      if (!(var1 instanceof AbstractComponent)) {
         return false;
      }

      AbstractComponent var2 = (AbstractComponent)var1;
      return Objects.equals(this.children, var2.children) && Objects.equals(this.style, var2.style);
   }

   @Override
   public int hashCode() {
      int var1 = this.children.hashCode();
      return 31 * var1 + this.style.hashCode();
   }

   @Override
   public abstract String toString();

   private String debuggerString() {
      Stream var1 = this.examinableProperties().filter(var0 -> restoredLambda$debuggerString$0(var0));
      return StringExaminer.simpleEscaping().examine(this.examinableName(), var1);
   }

   // $VF: synthetic method
   private static boolean restoredLambda$debuggerString$0(ExaminableProperty var0) {
      return !var0.name().equals("children");
   }
}
