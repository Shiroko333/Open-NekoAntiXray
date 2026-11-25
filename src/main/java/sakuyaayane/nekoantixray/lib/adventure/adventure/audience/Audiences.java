package sakuyaayane.nekoantixray.lib.adventure.adventure.audience;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.resource.ResourcePackCallback;
import sakuyaayane.nekoantixray.lib.adventure.adventure.resource.ResourcePackStatus;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.ComponentLike;

public final class Audiences {
   static final Collector<? super Audience, ?, ForwardingAudience> COLLECTOR = Collectors.collectingAndThen(
      Collectors.toCollection(() -> new ArrayList()), var0 -> restoredLambda$static$0((java.util.ArrayList)(var0))
   );

   private Audiences() {
   }

   @NotNull
   public static Consumer<? super Audience> sendingMessage(@NotNull ComponentLike var0) {
      return var1 -> restoredLambda$sendingMessage$1(var0, var1);
   }

   @NotNull
   static ResourcePackCallback unwrapCallback(Audience var0, Audience var1, @NotNull ResourcePackCallback var2) {
      return var2 == ResourcePackCallback.noOp() ? var2 : (var3, var4, var5) -> restoredLambda$unwrapCallback$2(var2, var1, var0, var3, var4, var5);
   }

   // $VF: synthetic method
   private static void restoredLambda$unwrapCallback$2(ResourcePackCallback var0, Audience var1, Audience var2, UUID var3, ResourcePackStatus var4, Audience var5) {
      var0.packEventReceived(var3, var4, var5 == var1 ? var2 : var5);
   }

   // $VF: synthetic method
   private static void restoredLambda$sendingMessage$1(ComponentLike var0, Audience var1) {
      var1.sendMessage(var0);
   }

   // $VF: synthetic method
   private static ForwardingAudience restoredLambda$static$0(ArrayList var0) {
      return Audience.audience(Collections.unmodifiableCollection(var0));
   }
}
