package net.minecraft.resources;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;

public final class NaxBedrockRemapBridge {
   private NaxBedrockRemapBridge() {
   }

   private static Object parseResourceLocation(Class resourceLocationClass, String resourceName) {
      try {
         Method parseMethod = resourceLocationClass.getMethod("parse", String.class);
         return parseMethod.invoke(null, resourceName);
      } catch (Throwable parseError) {
         String namespace = "minecraft";
         String path = resourceName;
         int colonIndex = resourceName.indexOf(58);
         if (colonIndex >= 0) {
            namespace = resourceName.substring(0, colonIndex);
            path = resourceName.substring(colonIndex + 1);
         }

         try {
            Constructor twoArgConstructor = resourceLocationClass.getConstructor(String.class, String.class);
            return twoArgConstructor.newInstance(namespace, path);
         } catch (Throwable twoArgError) {
            try {
               Constructor singleArgConstructor = resourceLocationClass.getConstructor(String.class);
               return singleArgConstructor.newInstance(resourceName);
            } catch (Throwable singleArgError) {
               return null;
            }
         }
      }
   }

   public static Object remapRandomName(Object resourceLocation, long seedSalt) {
      if (resourceLocation == null) {
         return null;
      }

      String rawName = String.valueOf(resourceLocation);
      if (rawName.isEmpty()) {
         return resourceLocation;
      }

      String lowerName = rawName.toLowerCase(Locale.ROOT);
      if (!lowerName.contains("bedrock")) {
         return resourceLocation;
      }

      if (!lowerName.startsWith("nax:bedrock_floor_") && !lowerName.startsWith("nax:bedrock_roof_")) {
         String hexSalt = Long.toUnsignedString(seedSalt, 16);
         String remappedName;
         if (lowerName.contains("bedrock_roof")) {
            remappedName = "nax:bedrock_roof_" + hexSalt;
         } else {
            remappedName = "nax:bedrock_floor_" + hexSalt;
         }

         Object remappedLocation = parseResourceLocation(resourceLocation.getClass(), remappedName);
         return remappedLocation != null ? remappedLocation : resourceLocation;
      } else {
         return resourceLocation;
      }
   }
}