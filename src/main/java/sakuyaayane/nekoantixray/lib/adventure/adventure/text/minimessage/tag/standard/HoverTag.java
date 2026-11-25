package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.standard;

import java.util.HashMap;
import java.util.UUID;
import java.util.Map.Entry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.key.InvalidKeyException;
import sakuyaayane.nekoantixray.lib.adventure.adventure.key.Key;
import sakuyaayane.nekoantixray.lib.adventure.adventure.nbt.api.BinaryTagHolder;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.event.DataComponentValue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.event.HoverEvent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.ParsingException;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.SerializableResolver;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.StyleClaim;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.TokenEmitter;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;

final class HoverTag {
   private static final String HOVER = "hover";
   static final TagResolver RESOLVER = SerializableResolver.claimingStyle(
      "hover", (var0, var1) -> create(var0, var1), StyleClaim.claim("hover", var0 -> var0.hoverEvent(), (var0, var1) -> emit(var0, var1))
   );

   private HoverTag() {
   }

   static Tag create(ArgumentQueue var0, Context var1) throws ParsingException {
      String var2 = var0.popOr("Hover event requires an action as its first argument").value();
      HoverEvent.Action var3 = HoverEvent.Action.NAMES.value(var2);
      HoverTag.ActionHandler var4 = actionHandler(var3);
      if (var4 == null) {
         throw var1.newException("Don't know how to turn '" + var0 + "' into a hover event", var0);
      } else {
         return Tag.styling(HoverEvent.hoverEvent(var3, var4.parse(var0, var1)));
      }
   }

   static void emit(HoverEvent<?> var0, TokenEmitter var1) {
      HoverTag.ActionHandler var2 = actionHandler(var0.action());
      var1.tag("hover").argument(HoverEvent.Action.NAMES.key(var0.action()));
      var2.emit(var0.value(), var1);
   }

   @Nullable
   static <V> HoverTag.ActionHandler<V> actionHandler(HoverEvent.Action<V> var0) {
      HoverTag.ActionHandler var1 = null;
      if (var0 == HoverEvent.Action.SHOW_TEXT) {
         var1 = HoverTag.ShowText.INSTANCE;
      } else if (var0 == HoverEvent.Action.SHOW_ITEM) {
         var1 = HoverTag.ShowItem.INSTANCE;
      } else if (var0 == HoverEvent.Action.SHOW_ENTITY) {
         var1 = HoverTag.ShowEntity.INSTANCE;
      }

      return var1;
   }

   @NotNull
   static String compactAsString(@NotNull Key var0) {
      return var0.namespace().equals("minecraft") ? var0.value() : var0.asString();
   }

   interface ActionHandler<V> {
      @NotNull
      V parse(@NotNull ArgumentQueue var1, @NotNull Context var2) throws ParsingException;

      void emit(V var1, TokenEmitter var2);
   }

   static final class ShowEntity implements HoverTag.ActionHandler<HoverEvent.ShowEntity> {
      static final HoverTag.ShowEntity INSTANCE = new HoverTag.ShowEntity();

      private ShowEntity() {
      }

      public HoverEvent.@NotNull ShowEntity parse(@NotNull ArgumentQueue var1, @NotNull Context var2) throws ParsingException {
         try {
            Key var3 = Key.key(var1.popOr("Show entity needs a type argument").value());
            UUID var4 = UUID.fromString(var1.popOr("Show entity needs an entity UUID").value());
            if (var1.hasNext()) {
               Component var5 = var2.deserialize(var1.pop().value());
               return HoverEvent.ShowEntity.showEntity(var3, var4, var5);
            } else {
               return HoverEvent.ShowEntity.showEntity(var3, var4);
            }
         } catch (IllegalArgumentException | InvalidKeyException var6) {
            throw var2.newException("Exception parsing show_entity hover", var6, var1);
         }
      }

      public void emit(HoverEvent.ShowEntity var1, TokenEmitter var2) {
         var2.argument(HoverTag.compactAsString(var1.type())).argument(var1.id().toString());
         if (var1.name() != null) {
            var2.argument(var1.name());
         }
      }
   }

   static final class ShowItem implements HoverTag.ActionHandler<HoverEvent.ShowItem> {
      private static final HoverTag.ShowItem INSTANCE = new HoverTag.ShowItem();

      private ShowItem() {
      }

      public HoverEvent.@NotNull ShowItem parse(@NotNull ArgumentQueue var1, @NotNull Context var2) throws ParsingException {
         try {
            Key var3 = Key.key(var1.popOr("Show item hover needs at least an item ID").value());
            int var4 = var1.hasNext() ? var1.pop().asInt().orElseThrow(() -> restoredLambda$parse$0(var2)) : 1;
            if (!var1.hasNext()) {
               return HoverEvent.ShowItem.showItem(var3, var4);
            }

            String var5 = var1.peek().value();
            if (var5.startsWith("{")) {
               var1.pop();
               return legacyShowItem(var3, var4, var5);
            }

            HashMap var6 = new HashMap();

            while (var1.hasNext()) {
               Key var7 = Key.key(var1.pop().value());
               String var8 = var1.popOr("a value was expected for key " + var7).value();
               var6.put(var7, BinaryTagHolder.binaryTagHolder(var8));
            }

            return HoverEvent.ShowItem.showItem(var3, var4, var6);
         } catch (InvalidKeyException | NumberFormatException var9) {
            throw var2.newException("Exception parsing show_item hover", var9, var1);
         }
      }

      private static HoverEvent.@NotNull ShowItem legacyShowItem(Key var0, int var1, String var2) {
         return HoverEvent.ShowItem.showItem(var0, var1, BinaryTagHolder.binaryTagHolder(var2));
      }

      public void emit(HoverEvent.ShowItem var1, TokenEmitter var2) {
         var2.argument(HoverTag.compactAsString(var1.item()));
         if (var1.count() != 1 || hasLegacy(var1) || !var1.dataComponents().isEmpty()) {
            var2.argument(Integer.toString(var1.count()));
            if (hasLegacy(var1)) {
               emitLegacyHover(var1, var2);
            } else {
               for (Entry var4 : var1.dataComponentsAs(DataComponentValue.TagSerializable.class).entrySet()) {
                  var2.argument(((Key)var4.getKey()).asMinimalString());
                  var2.argument(((DataComponentValue.TagSerializable)var4.getValue()).asBinaryTag().string());
               }
            }
         }
      }

      static boolean hasLegacy(HoverEvent.ShowItem var0) {
         return var0.nbt() != null;
      }

      static void emitLegacyHover(HoverEvent.ShowItem var0, TokenEmitter var1) {
         if (var0.nbt() != null) {
            var1.argument(var0.nbt().string());
         }
      }

      // $VF: synthetic method
      private static ParsingException restoredLambda$parse$0(Context var0) {
         return var0.newException("The count argument was not a valid integer");
      }
   }

   static final class ShowText implements HoverTag.ActionHandler<Component> {
      private static final HoverTag.ShowText INSTANCE = new HoverTag.ShowText();

      private ShowText() {
      }

      @NotNull
      public Component parse(@NotNull ArgumentQueue var1, @NotNull Context var2) throws ParsingException {
         return var2.deserialize(var1.popOr("show_text action requires a message").value());
      }

      public void emit(Component var1, TokenEmitter var2) {
         var2.argument(var1);
      }
   }
}
