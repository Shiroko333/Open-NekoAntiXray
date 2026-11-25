package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.standard;

import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.key.Key;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.BlockNBTComponent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.Component;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.EntityNBTComponent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.NBTComponent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.NBTComponentBuilder;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.StorageNBTComponent;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.ParsingException;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.Emitable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.SerializableResolver;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.serializer.TokenEmitter;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver.TagResolver;

final class NbtTag {
   private static final String NBT = "nbt";
   private static final String DATA = "data";
   private static final String BLOCK = "block";
   private static final String ENTITY = "entity";
   private static final String STORAGE = "storage";
   private static final String INTERPRET = "interpret";
   static final TagResolver RESOLVER = SerializableResolver.claimingComponent(
      StandardTags.names("nbt", "data"), (var0, var1) -> resolve(var0, var1), var0 -> emit(var0)
   );

   private NbtTag() {
   }

   static Tag resolve(ArgumentQueue var0, Context var1) throws ParsingException {
      String var2 = var0.popOr("a type of block, entity, or storage is required").lowerValue();
      NBTComponentBuilder var3;
      if ("block".equals(var2)) {
         String var4 = var0.popOr("A position is required").value();

         try {
            var3 = Component.blockNBT().pos(BlockNBTComponent.Pos.fromString(var4));
         } catch (IllegalArgumentException var6) {
            throw var1.newException(var6.getMessage(), var0);
         }
      } else if ("entity".equals(var2)) {
         var3 = Component.entityNBT().selector(var0.popOr("A selector is required").value());
      } else {
         if (!"storage".equals(var2)) {
            throw var1.newException("Unknown nbt tag type '" + var2 + "'", var0);
         }

         var3 = Component.storageNBT().storage(Key.key(var0.popOr("A storage key is required").value()));
      }

      var3.nbtPath(var0.popOr("An NBT path is required").value());
      if (var0.hasNext()) {
         String var7 = var0.pop().value();
         if ("interpret".equalsIgnoreCase(var7)) {
            var3.interpret(true);
         } else {
            var3.separator(var1.deserialize(var7));
            if (var0.hasNext() && var0.pop().value().equalsIgnoreCase("interpret")) {
               var3.interpret(true);
            }
         }
      }

      return Tag.inserting(var3.build());
   }

   @Nullable
   static Emitable emit(Component var0) {
      String var1;
      String var2;
      if (var0 instanceof BlockNBTComponent) {
         var1 = "block";
         var2 = ((BlockNBTComponent)var0).pos().asString();
      } else if (var0 instanceof EntityNBTComponent) {
         var1 = "entity";
         var2 = ((EntityNBTComponent)var0).selector();
      } else {
         if (!(var0 instanceof StorageNBTComponent)) {
            return null;
         }

         var1 = "storage";
         var2 = ((StorageNBTComponent)var0).storage().asString();
      }

      return var3 -> restoredLambda$emit$0(var0, var1, var2, var3);
   }

   // $VF: synthetic method
   private static void restoredLambda$emit$0(Component var0, String var1, String var2, TokenEmitter var3) {
      NBTComponent var4 = (NBTComponent)var0;
      var3.tag("nbt").argument(var1).argument(var2).argument(var4.nbtPath());
      if (var4.separator() != null) {
         var3.argument(var4.separator());
      }

      if (var4.interpret()) {
         var3.argument("interpret");
      }
   }
}
