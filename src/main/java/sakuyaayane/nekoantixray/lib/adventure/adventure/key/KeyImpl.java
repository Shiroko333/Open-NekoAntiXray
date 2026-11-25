package sakuyaayane.nekoantixray.lib.adventure.adventure.key;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.OptionalInt;

record KeyImpl(String namespace, String value) implements Key {
   static final Comparator<? super Key> COMPARATOR = Comparator.<Key, String>comparing(var0 -> var0.value()).thenComparing(var0 -> var0.namespace());

   KeyImpl(String namespace, String value) {
      checkError("namespace", namespace, namespace, value, Key.checkNamespace(namespace), "^(?!\\.\\.$)[a-z0-9_.-]+$");
      checkError("value", value, namespace, value, Key.checkValue(value), "[a-z0-9_\\-./]+");
      this.namespace = namespace;
      this.value = value;
   }

   static void checkError(String var0, String var1, String var2, String var3, OptionalInt var4, String var5) {
      Objects.requireNonNull(var1, var0);
      if (var4.isPresent()) {
         int var6 = var4.getAsInt();
         if (var6 == -1) {
            throw new InvalidKeyException(var2, var3, String.format("'%s' is not a valid value for %s", var1, var0));
         }

         char var7 = var1.charAt(var6);
         throw new InvalidKeyException(
            var2,
            var3,
            String.format(
               "Non " + var5 + " character in %s of Key[%s] at index %d ('%s', bytes: %s)",
               var0,
               asString(var2, var3),
               var6,
               var7,
               Arrays.toString(String.valueOf(var7).getBytes(StandardCharsets.UTF_8))
            )
         );
      }
   }

   static boolean allowedInNamespace(char var0) {
      return var0 == '_' || var0 == '-' || var0 >= 'a' && var0 <= 'z' || var0 >= '0' && var0 <= '9' || var0 == '.';
   }

   static boolean allowedInValue(char var0) {
      return var0 == '_' || var0 == '-' || var0 >= 'a' && var0 <= 'z' || var0 >= '0' && var0 <= '9' || var0 == '.' || var0 == '/';
   }

   @Override
   public String asString() {
      return asString(this.namespace, this.value);
   }

   private static String asString(String var0, String var1) {
      return var0 + ":" + var1;
   }

   @Override
   public String toString() {
      return this.asString();
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else {
         return !(var1 instanceof Key var2) ? false : Objects.equals(this.namespace, var2.namespace()) && Objects.equals(this.value, var2.value());
      }
   }

   @Override
   public int compareTo(Key var1) {
      return Key.super.compareTo(var1);
   }
}
