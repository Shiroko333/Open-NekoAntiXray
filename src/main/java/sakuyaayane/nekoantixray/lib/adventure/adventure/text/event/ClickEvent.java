package sakuyaayane.nekoantixray.lib.adventure.adventure.text.event;

import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.audience.Audience;
import sakuyaayane.nekoantixray.lib.adventure.adventure.builder.AbstractBuilder;
import sakuyaayane.nekoantixray.lib.adventure.adventure.internal.Internals;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.Style;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.format.StyleBuilderApplicable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.util.Index;
import sakuyaayane.nekoantixray.lib.adventure.examination.Examinable;
import sakuyaayane.nekoantixray.lib.adventure.examination.ExaminableProperty;

public final class ClickEvent implements Examinable, StyleBuilderApplicable {
   private final ClickEvent.Action action;
   private final String value;

   @NotNull
   public static ClickEvent openUrl(@NotNull String var0) {
      return new ClickEvent(ClickEvent.Action.OPEN_URL, var0);
   }

   @NotNull
   public static ClickEvent openUrl(@NotNull URL var0) {
      return openUrl(var0.toExternalForm());
   }

   @NotNull
   public static ClickEvent openFile(@NotNull String var0) {
      return new ClickEvent(ClickEvent.Action.OPEN_FILE, var0);
   }

   @NotNull
   public static ClickEvent runCommand(@NotNull String var0) {
      return new ClickEvent(ClickEvent.Action.RUN_COMMAND, var0);
   }

   @NotNull
   public static ClickEvent suggestCommand(@NotNull String var0) {
      return new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, var0);
   }

   @NotNull
   public static ClickEvent changePage(@NotNull String var0) {
      return new ClickEvent(ClickEvent.Action.CHANGE_PAGE, var0);
   }

   @NotNull
   public static ClickEvent changePage(int var0) {
      return changePage(String.valueOf(var0));
   }

   @NotNull
   public static ClickEvent copyToClipboard(@NotNull String var0) {
      return new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, var0);
   }

   @NotNull
   public static ClickEvent callback(@NotNull ClickCallback<Audience> var0) {
      return ClickCallbackInternals.PROVIDER.create(Objects.requireNonNull(var0, "function"), ClickCallbackOptionsImpl.DEFAULT);
   }

   @NotNull
   public static ClickEvent callback(@NotNull ClickCallback<Audience> var0, ClickCallback.@NotNull Options var1) {
      return ClickCallbackInternals.PROVIDER.create(Objects.requireNonNull(var0, "function"), Objects.requireNonNull(var1, "options"));
   }

   @NotNull
   public static ClickEvent callback(@NotNull ClickCallback<Audience> var0, @NotNull Consumer<ClickCallback.Options.Builder> var1) {
      return ClickCallbackInternals.PROVIDER
         .create(
            Objects.requireNonNull(var0, "function"),
            AbstractBuilder.configureAndBuild(ClickCallback.Options.builder(), Objects.requireNonNull(var1, "optionsBuilder"))
         );
   }

   @NotNull
   public static ClickEvent clickEvent(@NotNull ClickEvent.Action var0, @NotNull String var1) {
      return new ClickEvent(var0, var1);
   }

   private ClickEvent(@NotNull ClickEvent.Action var1, @NotNull String var2) {
      this.action = Objects.requireNonNull(var1, "action");
      this.value = Objects.requireNonNull(var2, "value");
   }

   @NotNull
   public ClickEvent.Action action() {
      return this.action;
   }

   @NotNull
   public String value() {
      return this.value;
   }

   @Override
   public void styleApply(Style.@NotNull Builder var1) {
      var1.clickEvent(this);
   }

   @Override
   public boolean equals(@Nullable Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         ClickEvent var2 = (ClickEvent)var1;
         return this.action == var2.action && Objects.equals(this.value, var2.value);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int var1 = this.action.hashCode();
      return 31 * var1 + this.value.hashCode();
   }

   @NotNull
   @Override
   public Stream<? extends ExaminableProperty> examinableProperties() {
      return Stream.of(ExaminableProperty.of("action", this.action), ExaminableProperty.of("value", this.value));
   }

   @Override
   public String toString() {
      return Internals.toString(this);
   }

   public enum Action {
      OPEN_URL("open_url", true),
      OPEN_FILE("open_file", false),
      RUN_COMMAND("run_command", true),
      SUGGEST_COMMAND("suggest_command", true),
      CHANGE_PAGE("change_page", true),
      COPY_TO_CLIPBOARD("copy_to_clipboard", true);

      public static final Index<String, ClickEvent.Action> NAMES = Index.create(ClickEvent.Action.class, var0 -> restoredLambda$static$0(var0));
      private final String name;
      private final boolean readable;

      Action(@NotNull String var3, boolean var4) {
         this.name = var3;
         this.readable = var4;
      }

      public boolean readable() {
         return this.readable;
      }

      @NotNull
      @Override
      public String toString() {
         return this.name;
      }

      // $VF: synthetic method
      private static String restoredLambda$static$0(ClickEvent.Action var0) {
         return var0.name;
      }
   }
}
