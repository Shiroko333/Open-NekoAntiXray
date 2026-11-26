package sakuyaayane.nekoantixray.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;

public class ChunkAccessTransformer {
   private static volatile boolean initialized = false;
   private static Class<?> chunkAccessClass = null;
   private static Class<?> levelChunkClass = null;
   private static String detectedVersion = "unknown";
   private static MethodHandle getChunk2ArgHandle = null;
   private static MethodHandle getChunk3ArgHandle = null;
   private static Object chunkStatusFull = null;
   private static int getChunkArgCount = 0;
   private static boolean compatible = false;
   private static boolean highVersionMode = false;
   private static final Lookup LOOKUP = MethodHandles.lookup();

   public static synchronized void initialize() {
      if (!initialized) {
         detectServerVersion();
         resolveChunkClasses();
         resolveGetChunkMethod();
         if (getChunkArgCount == 3 && chunkStatusFull == null) {
            chunkStatusFull = resolveFullChunkStatus();
         }

         boolean hasChunkClass = chunkAccessClass != null || levelChunkClass != null;
         boolean hasGetChunkHandle = getChunk2ArgHandle != null || getChunk3ArgHandle != null && chunkStatusFull != null;
         compatible = hasChunkClass && hasGetChunkHandle;
         highVersionMode = !detectedVersion.equals("1.20.4")
            && chunkAccessClass != null
            && chunkAccessClass.getName().equals("net.minecraft.world.level.chunk.ChunkAccess");
         initialized = true;
      }
   }

   private static void detectServerVersion() {
      try {
         String bukkitVersion = Bukkit.getBukkitVersion();
         if (bukkitVersion.contains("1.20.4")) {
            detectedVersion = "1.20.4";
         } else if (bukkitVersion.contains("1.20.5")) {
            detectedVersion = "1.20.5";
         } else if (bukkitVersion.contains("1.20.6")) {
            detectedVersion = "1.20.6";
         } else if (bukkitVersion.contains("1.21")) {
            detectedVersion = "1.21+";
         } else {
            String[] versionParts = bukkitVersion.split("-");
            if (versionParts.length > 0) {
               detectedVersion = versionParts[0];
            }
         }
      } catch (Exception ignored) {
      }
   }

   private static void resolveChunkClasses() {
      String[] chunkAccessClassNames = new String[]{
         "net.minecraft.world.level.chunk.ChunkAccess",
         "net.minecraft.world.level.chunk.IChunkAccess",
         "net.minecraft.server.level.ChunkAccess",
         "net.minecraft.server.v1_20_R3.ChunkAccess",
         "net.minecraft.world.chunk.ChunkAccess",
         "net.minecraft.world.chunk.IChunkAccess"
      };
      String[] levelChunkClassNames = new String[]{
         "net.minecraft.world.level.chunk.LevelChunk",
         "net.minecraft.world.level.chunk.Chunk",
         "net.minecraft.server.level.LevelChunk",
         "net.minecraft.server.v1_20_R3.LevelChunk",
         "net.minecraft.world.chunk.Chunk"
      };

      for (String className : chunkAccessClassNames) {
         try {
            chunkAccessClass = Class.forName(className);
            break;
         } catch (ClassNotFoundException ignored) {
         }
      }

      for (String className : levelChunkClassNames) {
         try {
            levelChunkClass = Class.forName(className);
            break;
         } catch (ClassNotFoundException ignored) {
         }
      }
   }

   private static void resolveGetChunkMethod() {
      String[] worldClassNames = new String[]{
         "net.minecraft.server.level.ServerLevel",
         "net.minecraft.server.v1_20_R3.WorldServer",
         "net.minecraft.world.level.World",
         "net.minecraft.server.level.WorldServer",
         "net.minecraft.world.level.Level",
         "net.minecraft.server.WorldServer"
      };

      for (String worldClassName : worldClassNames) {
         try {
            Class<?> worldClass = Class.forName(worldClassName);
            String[] methodNames = new String[]{"getChunk", "getChunkAt", "a"};

            for (String methodName : methodNames) {
               try {
                  Class<?>[] paramTypes = new Class[]{int.class, int.class};
                  Method twoArgMethod = findMethodBySignature(worldClass, methodName, 2, paramTypes);
                  if (twoArgMethod != null) {
                     twoArgMethod.setAccessible(true);
                     getChunk2ArgHandle = LOOKUP.unreflect(twoArgMethod);
                     getChunkArgCount = 2;
                     return;
                  }
               } catch (Exception ignored) {
               }
            }

            for (String methodName : methodNames) {
               try {
                  Method threeArgMethod = findThreeArgChunkMethod(worldClass, methodName);
                  if (threeArgMethod != null) {
                     threeArgMethod.setAccessible(true);
                     getChunk3ArgHandle = LOOKUP.unreflect(threeArgMethod);
                     getChunkArgCount = 3;
                     return;
                  }
               } catch (Exception ignored) {
               }
            }
         } catch (ClassNotFoundException ignored) {
         }
      }
   }

   private static Method findMethodBySignature(Class clazz, String methodName, int expectedParamCount, Class[] expectedParamTypes) {
      try {
         for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == expectedParamCount) {
               Class<?>[] parameterTypes = method.getParameterTypes();
               boolean matches = true;

               for (int i = 0; i < expectedParamTypes.length && i < parameterTypes.length; i++) {
                  if (parameterTypes[i] != expectedParamTypes[i]) {
                     matches = false;
                     break;
                  }
               }

               if (matches) {
                  return method;
               }
            }
         }
      } catch (Exception ignored) {
      }

      return null;
   }

   private static Method findThreeArgChunkMethod(Class clazz, String methodName) {
      try {
         for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 3) {
               Class<?>[] parameterTypes = method.getParameterTypes();
               if (parameterTypes[0] == int.class && parameterTypes[1] == int.class) {
                  return method;
               }
            }
         }
      } catch (Exception ignored) {
      }

      return null;
   }

   private static Object resolveFullChunkStatus() {
      String[] statusClassNames = new String[]{
         "net.minecraft.world.level.chunk.status.ChunkStatus",
         "net.minecraft.world.level.chunk.ChunkStatus",
         "net.minecraft.server.level.ChunkStatus",
         "net.minecraft.server.v1_20_R3.ChunkStatus",
         "net.minecraft.world.chunk.ChunkStatus"
      };

      for (String statusClassName : statusClassNames) {
         try {
            Class<?> statusClass = Class.forName(statusClassName);
            String[] fieldNames = new String[]{"FULL", "b", "c"};

            for (String fieldName : fieldNames) {
               try {
                  Field field = statusClass.getField(fieldName);
                  if (field.getType() == statusClass) {
                     return field.get(null);
                  }
               } catch (NoSuchFieldException ignored) {
               }
            }
         } catch (Exception ignored) {
         }
      }

      return null;
   }

   public static boolean isCompatible() {
      if (!initialized) {
         initialize();
      }

      return compatible;
   }

   public static Class getChunkAccessClass() {
      if (!initialized) {
         initialize();
      }

      return chunkAccessClass != null ? chunkAccessClass : levelChunkClass;
   }

   public static String getDetectedVersion() {
      if (!initialized) {
         initialize();
      }

      return detectedVersion;
   }

   public static boolean isHighVersionMode() {
      if (!initialized) {
         initialize();
      }

      return highVersionMode;
   }

   public static Object getChunk(Object worldHandle, int chunkX, int chunkZ) {
      if (!initialized) {
         initialize();
      }

      try {
         if (getChunkArgCount == 2 && getChunk2ArgHandle != null) {
            return (Object)getChunk2ArgHandle.invoke((Object)worldHandle, (int)chunkX, (int)chunkZ);
         }

         if (getChunkArgCount == 3 && getChunk3ArgHandle != null && chunkStatusFull != null) {
            return (Object)getChunk3ArgHandle.invoke((Object)worldHandle, (int)chunkX, (int)chunkZ, (Object)chunkStatusFull);
         }
      } catch (Throwable ignored) {
      }

      return null;
   }

   public static synchronized void reset() {
      initialized = false;
      chunkAccessClass = null;
      levelChunkClass = null;
      getChunk2ArgHandle = null;
      getChunk3ArgHandle = null;
      chunkStatusFull = null;
      getChunkArgCount = 0;
      compatible = false;
      highVersionMode = false;
      detectedVersion = "unknown";
   }
}
