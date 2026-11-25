package sakuyaayane.nekoantixray.lib.adventure.adventure.key;

import java.util.Comparator;
import java.util.Objects;
import java.util.OptionalInt;
import org.jspecify.annotations.Nullable;

public interface Key extends Comparable<Key>, Namespaced, Keyed {
   String MINECRAFT_NAMESPACE = "minecraft";
   char DEFAULT_SEPARATOR = ':';

   static Key key(@KeyPattern String var0) {
      return key(var0, ':');
   }

   static Key key(String var0, char var1) {
      Objects.requireNonNull(var0, "string");
      int var2 = var0.indexOf(var1);
      String var3 = var2 >= 1 ? var0.substring(0, var2) : "minecraft";
      String var4 = var2 >= 0 ? var0.substring(var2 + 1) : var0;
      return key(var3, var4);
   }

   static Key key(Namespaced var0, @KeyPattern.Value String var1) {
      return key(Objects.requireNonNull(var0, "namespaced").namespace(), var1);
   }

   static Key key(@KeyPattern.Namespace String var0, @KeyPattern.Value String var1) {
      return new KeyImpl(var0, var1);
   }

   static Comparator<? super Key> comparator() {
      return KeyImpl.COMPARATOR;
   }

   static boolean parseable(@Nullable String var0) {
      if (var0 == null) {
         return false;
      }

      int var1 = var0.indexOf(58);
      String var2 = var1 >= 1 ? var0.substring(0, var1) : "minecraft";
      String var3 = var1 >= 0 ? var0.substring(var1 + 1) : var0;
      return parseableNamespace(var2) && parseableValue(var3);
   }

   static boolean parseableNamespace(String var0) {
      return checkNamespace(var0).isEmpty();
   }

   static OptionalInt checkNamespace(String var0) {
      Objects.requireNonNull(var0, "namespace");
      if (var0.equals("..")) {
         return OptionalInt.of(-1);
      }

      int var1 = 0;

      for (int var2 = var0.length(); var1 < var2; var1++) {
         if (!allowedInNamespace(var0.charAt(var1))) {
            return OptionalInt.of(var1);
         }
      }

      return OptionalInt.empty();
   }

   static boolean parseableValue(String var0) {
      return checkValue(var0).isEmpty();
   }

   static OptionalInt checkValue(String var0) {
      Objects.requireNonNull(var0, "value");
      int var1 = 0;

      for (int var2 = var0.length(); var1 < var2; var1++) {
         if (!allowedInValue(var0.charAt(var1))) {
            return OptionalInt.of(var1);
         }
      }

      return OptionalInt.empty();
   }

   static boolean allowedInNamespace(char var0) {
      return KeyImpl.allowedInNamespace(var0);
   }

   static boolean allowedInValue(char var0) {
      return KeyImpl.allowedInValue(var0);
   }

   @KeyPattern.Namespace
   @Override
   String namespace();

   @KeyPattern.Value
   String value();

   String asString();

   default String asMinimalString() {
      return this.namespace().equals("minecraft") ? this.value() : this.asString();
   }

   default int compareTo(Key var1) {
      return comparator().compare(this, var1);
   }

   @Override
   default Key key() {
      return this;
   }
}
