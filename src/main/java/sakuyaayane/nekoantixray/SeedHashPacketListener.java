package sakuyaayane.nekoantixray;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import org.bukkit.entity.Player;

public class SeedHashPacketListener extends PacketListenerAbstract {
   private final NekoAntiXray plugin;
   private final AntiSeedCrackerManager antiSeedCrackerManager;
   private final Random random = new Random();
   private boolean loginFailureLogged = false;
   private boolean respawnFailureLogged = false;
   private String serverVersion = null;

   public SeedHashPacketListener(NekoAntiXray plugin, AntiSeedCrackerManager antiSeedCrackerManager) {
      super(PacketListenerPriority.HIGHEST);
      this.plugin = plugin;
      this.antiSeedCrackerManager = antiSeedCrackerManager;
   }

   @Override
   public void onPacketSend(PacketSendEvent event) {
      if (this.antiSeedCrackerManager.isModifyHashedSeedEnabled() && (event.getPacketType() == Server.JOIN_GAME || event.getPacketType() == Server.RESPAWN)) {
         if (this.serverVersion == null) {
            try {
               this.serverVersion = ServerVersionAccess.getMinecraftVersion(this.plugin.getServer());
               this.plugin.logDebug("[AntiSeedCracker] Detected Minecraft Server Version: " + this.serverVersion);
            } catch (Exception ex) {
               this.plugin.logDebug("[AntiSeedCracker] Error detecting Minecraft Server Version: " + ex.getMessage());
               this.serverVersion = "unknown";
            }
         }

         Player player = event.getPlayer() instanceof Player ? event.getPlayer() : null;
         String playerName = player != null ? player.getName() : "unknown";
         boolean isJoinGame = event.getPacketType() == Server.JOIN_GAME;
         String packetTypeName = isJoinGame ? "LOGIN" : "RESPAWN";

         try {
            long randomizedSeed = this.random.nextLong();
            long originalSeed;
            if (isJoinGame) {
               WrapperPlayServerJoinGame joinGamePacket = new WrapperPlayServerJoinGame(event);
               originalSeed = joinGamePacket.getHashedSeed();
               joinGamePacket.setHashedSeed(randomizedSeed);
            } else {
               WrapperPlayServerRespawn respawnPacket = new WrapperPlayServerRespawn(event);
               originalSeed = respawnPacket.getHashedSeed();
               respawnPacket.setHashedSeed(randomizedSeed);
            }

            event.markForReEncode(true);
            this.plugin
               .logDebug(
                  "[AntiSeedCracker] 已为玩家 "
                     + playerName
                     + " 修改 "
                     + packetTypeName
                     + " 数据包中的哈希种子 (PacketEvents Wrapper)，从 "
                     + originalSeed
                     + " 修改为 "
                     + randomizedSeed
               );
            this.loginFailureLogged = false;
            this.respawnFailureLogged = false;
         } catch (Exception ex) {
            boolean alreadyLogged = isJoinGame ? this.loginFailureLogged : this.respawnFailureLogged;
            if (!alreadyLogged) {
               this.plugin
                  .logDebug(
                     "[AntiSeedCracker] 未能为玩家 "
                        + playerName
                        + " 修改 "
                        + packetTypeName
                        + " 数据包的哈希种子。 原因: "
                        + ex.getClass().getSimpleName()
                        + " - "
                        + ex.getMessage()
                  );
               this.plugin.logDebug("[AntiSeedCracker] " + packetTypeName + " 失败的完整堆栈跟踪: EXCEPTION: " + ex);
               this.plugin
                  .logDebug(
                     "[AntiSeedCracker] 这可能表示 PacketEvents Wrapper 与当前服务端协议不兼容。此数据包类型 (" + packetTypeName + ") 的种子随机化可能无法正常工作。"
                  );
               if (isJoinGame) {
                  this.loginFailureLogged = true;
               } else {
                  this.respawnFailureLogged = true;
               }
            }
         }
      }
   }

   private boolean replaceLongSeedField(Object packetObject, String playerName, String packetTypeName) {
      for (Field field : this.getAllDeclaredFields(packetObject.getClass())) {
         try {
            if (field.getType() == long.class || field.getType() == Long.class) {
               field.setAccessible(true);
               long originalSeed = field.getLong(packetObject);
               long randomizedSeed = this.random.nextLong();
               field.setLong(packetObject, randomizedSeed);
               this.plugin
                  .logDebug(
                     "[AntiSeedCracker] 已为玩家 "
                        + playerName
                        + " 修改 "
                        + packetTypeName
                        + " 数据包中的哈希种子 (直接Long字段访问)，从 "
                        + originalSeed
                        + " 修改为 "
                        + randomizedSeed
                  );
               return true;
            }
         } catch (Throwable ignored) {
         }
      }

      return false;
   }

   private void logPacketFieldDetails(Object packetObject, String packetTypeName) {
      if (packetObject != null) {
         try {
            this.plugin.logDebug("[AntiSeedCracker] " + packetTypeName + " 数据包详细信息: " + packetObject.getClass().getName());
            int fieldIndex = 0;

            for (Field field : this.getAllDeclaredFields(packetObject.getClass())) {
               try {
                  field.setAccessible(true);
                  Object fieldValue = field.get(packetObject);
                  String fieldTypeName = fieldValue != null ? fieldValue.getClass().getName() : field.getType().getName();
                  String fieldName = field.getName();
                  String formattedValue = String.valueOf(fieldValue != null ? fieldValue : "null");
                  this.plugin
                     .logDebug(
                        "[AntiSeedCracker] "
                           + packetTypeName
                           + " 字段索引: "
                           + fieldIndex
                           + ", 名称: "
                           + fieldName
                           + ", 类型: "
                           + fieldTypeName
                           + ", 值: "
                           + formattedValue
                     );
               } catch (Throwable fieldEx) {
                  this.plugin.logDebug("[AntiSeedCracker] " + packetTypeName + " 字段索引: " + fieldIndex + ", 调试期间读取字段错误: " + fieldEx.getMessage());
               }

               fieldIndex++;
            }
         } catch (Throwable ex) {
            this.plugin.logDebug("[AntiSeedCracker] " + packetTypeName + " 数据包详细调试日志记录期间出错: " + ex.getMessage());
         }
      }
   }

   private Field findFieldByRuntimeClassName(Object targetObject, String targetClassName) {
      Field[] allDeclaredFields = this.getAllDeclaredFields(targetObject.getClass());

      for (Field field : allDeclaredFields) {
         try {
            field.setAccessible(true);
            Object fieldValue = field.get(targetObject);
            if (fieldValue != null && fieldValue.getClass().getName().equals(targetClassName)) {
               return field;
            }
         } catch (Throwable ignored) {
         }
      }

      return null;
   }

   private Field[] getAllDeclaredFields(Class targetClass) {
      ArrayList<Field> fields = new ArrayList<>();

      for (Class<?> currentClass = targetClass; currentClass != null && currentClass != Object.class; currentClass = currentClass.getSuperclass()) {
         for (Field field : currentClass.getDeclaredFields()) {
            fields.add(field);
         }
      }

      return fields.toArray(new Field[0]);
   }

   private Object rebuildSpawnInfo10Args(Class spawnInfoClass, Object originalSpawnInfo, long hashedSeed) throws ReflectiveOperationException {
      this.plugin.logDebug("[AntiSeedCracker] Attempting 10-parameter constructor path.");
      Field dimensionTypeField = this.getAccessibleDeclaredField(spawnInfoClass, "dimensionType");
      Field dimensionField = this.getAccessibleDeclaredField(spawnInfoClass, "dimension");
      Field gameTypeField = this.getAccessibleDeclaredField(spawnInfoClass, "gameType");
      Field previousGameTypeField = this.getAccessibleDeclaredField(spawnInfoClass, "previousGameType");
      Field isDebugField = this.getAccessibleDeclaredField(spawnInfoClass, "isDebug");
      Field isFlatField = this.getAccessibleDeclaredField(spawnInfoClass, "isFlat");
      Field lastDeathLocationField = this.getAccessibleDeclaredField(spawnInfoClass, "lastDeathLocation");
      Field portalCooldownField = this.getAccessibleDeclaredField(spawnInfoClass, "portalCooldown");
      Field seaLevelField = this.getAccessibleDeclaredField(spawnInfoClass, "seaLevel");
      Object dimensionType = dimensionTypeField.get(originalSpawnInfo);
      Object dimension = dimensionField.get(originalSpawnInfo);
      Object gameType = gameTypeField.get(originalSpawnInfo);
      Object previousGameType = previousGameTypeField.get(originalSpawnInfo);
      boolean isDebug = isDebugField.getBoolean(originalSpawnInfo);
      boolean isFlat = isFlatField.getBoolean(originalSpawnInfo);
      Optional lastDeathLocation = (Optional)lastDeathLocationField.get(originalSpawnInfo);
      int portalCooldown = portalCooldownField.getInt(originalSpawnInfo);
      int seaLevel = seaLevelField.getInt(originalSpawnInfo);
      Class[] parameterTypes = new Class[]{
         dimensionTypeField.getType(),
         dimensionField.getType(),
         long.class,
         gameTypeField.getType(),
         previousGameTypeField.getType(),
         boolean.class,
         boolean.class,
         Optional.class,
         int.class,
         seaLevelField.getType()
      };
      Constructor constructor = this.findMatchingConstructor(spawnInfoClass, parameterTypes);
      if (constructor == null) {
         this.logConstructorSearchFailure(spawnInfoClass, "10-parameter", parameterTypes);
         throw new NoSuchMethodException("10-parameter constructor for CommonPlayerSpawnInfo not found via findConstructor.");
      } else {
         constructor.setAccessible(true);
         return constructor.newInstance(
            dimensionType, dimension, hashedSeed, gameType, previousGameType, isDebug, isFlat, lastDeathLocation, portalCooldown, seaLevel
         );
      }
   }

   private Object rebuildSpawnInfo9Args(Class spawnInfoClass, Object originalSpawnInfo, long hashedSeed) throws ReflectiveOperationException {
      this.plugin.logDebug("[AntiSeedCracker] Attempting 9-parameter constructor path.");
      Field dimensionTypeField = this.getAccessibleDeclaredField(spawnInfoClass, "dimensionType");
      Field dimensionField = this.getAccessibleDeclaredField(spawnInfoClass, "dimension");
      Field gameTypeField = this.getAccessibleDeclaredField(spawnInfoClass, "gameType");
      Field previousGameTypeField = this.getAccessibleDeclaredField(spawnInfoClass, "previousGameType");
      Field isDebugField = this.getAccessibleDeclaredField(spawnInfoClass, "isDebug");
      Field isFlatField = this.getAccessibleDeclaredField(spawnInfoClass, "isFlat");
      Field lastDeathLocationField = this.getAccessibleDeclaredField(spawnInfoClass, "lastDeathLocation");
      Field portalCooldownField = this.getAccessibleDeclaredField(spawnInfoClass, "portalCooldown");
      Object dimensionType = dimensionTypeField.get(originalSpawnInfo);
      Object dimension = dimensionField.get(originalSpawnInfo);
      Object gameType = gameTypeField.get(originalSpawnInfo);
      Object previousGameType = previousGameTypeField.get(originalSpawnInfo);
      boolean isDebug = isDebugField.getBoolean(originalSpawnInfo);
      boolean isFlat = isFlatField.getBoolean(originalSpawnInfo);
      Optional lastDeathLocation = (Optional)lastDeathLocationField.get(originalSpawnInfo);
      int portalCooldown = portalCooldownField.getInt(originalSpawnInfo);
      Class[] parameterTypes = new Class[]{
         dimensionTypeField.getType(),
         dimensionField.getType(),
         long.class,
         gameTypeField.getType(),
         previousGameTypeField.getType(),
         boolean.class,
         boolean.class,
         Optional.class,
         int.class
      };
      Constructor constructor = this.findMatchingConstructor(spawnInfoClass, parameterTypes);
      if (constructor == null) {
         this.logConstructorSearchFailure(spawnInfoClass, "9-parameter", parameterTypes);
         throw new NoSuchMethodException("9-parameter constructor for CommonPlayerSpawnInfo not found via findConstructor.");
      } else {
         constructor.setAccessible(true);
         return constructor.newInstance(dimensionType, dimension, hashedSeed, gameType, previousGameType, isDebug, isFlat, lastDeathLocation, portalCooldown);
      }
   }

   private void logConstructorSearchFailure(Class targetClass, String constructorLabel, Class[] expectedTypes) {
      this.plugin.logDebug("[AntiSeedCracker] " + constructorLabel + " constructor search failed for " + targetClass.getName());
      StringBuilder searchedSignature = new StringBuilder("  Searched Signature (");

      for (int i = 0; i < expectedTypes.length; i++) {
         searchedSignature.append(expectedTypes[i].getName());
         if (i < expectedTypes.length - 1) {
            searchedSignature.append(", ");
         }
      }

      searchedSignature.append(")");
      this.plugin.logDebug(searchedSignature.toString());
      this.plugin.logDebug("[AntiSeedCracker] Available constructors for " + targetClass.getName() + ":");

      for (Constructor constructor : targetClass.getDeclaredConstructors()) {
         StringBuilder candidateSignature = new StringBuilder("    - ");
         Class[] parameterTypes = constructor.getParameterTypes();

         for (int j = 0; j < parameterTypes.length; j++) {
            candidateSignature.append(parameterTypes[j].getName());
            if (j < parameterTypes.length - 1) {
               candidateSignature.append(", ");
            }
         }

         this.plugin.logDebug(candidateSignature.toString());
      }
   }

   private Field getAccessibleDeclaredField(Class targetClass, String fieldName) throws NoSuchFieldException {
      Field field = targetClass.getDeclaredField(fieldName);
      field.setAccessible(true);
      return field;
   }

   private Constructor findMatchingConstructor(Class targetClass, Class[] expectedTypes) {
      for (Constructor constructor : targetClass.getDeclaredConstructors()) {
         Class[] parameterTypes = constructor.getParameterTypes();
         if (parameterTypes.length == expectedTypes.length) {
            boolean matches = true;

            for (int i = 0; i < expectedTypes.length; i++) {
               if (!parameterTypes[i].isAssignableFrom(expectedTypes[i]) && !this.isPrimitiveWrapperMatch(parameterTypes[i], expectedTypes[i])) {
                  matches = false;
                  break;
               }
            }

            if (matches) {
               return constructor;
            }
         }
      }

      return null;
   }

   private Class toPrimitiveClass(Class wrapperClass) {
      if (wrapperClass == Integer.class) {
         return int.class;
      } else if (wrapperClass == Long.class) {
         return long.class;
      } else if (wrapperClass == Boolean.class) {
         return boolean.class;
      } else if (wrapperClass == Character.class) {
         return char.class;
      } else if (wrapperClass == Byte.class) {
         return byte.class;
      } else if (wrapperClass == Short.class) {
         return short.class;
      } else if (wrapperClass == Float.class) {
         return float.class;
      } else {
         return wrapperClass == Double.class ? double.class : null;
      }
   }

   private boolean isVersionAtLeast1214(String versionString) {
      if (versionString != null && !versionString.equals("unknown")) {
         try {
            String[] parts = versionString.split("\\.");
            if (parts.length < 3) {
               return false;
            }

            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = Integer.parseInt(parts[2]);
            if (major > 1) {
               return true;
            }

            if (major == 1) {
               if (minor > 21) {
                  return true;
               }

               if (minor == 21) {
                  return patch >= 4;
               }
            }

            return false;
         } catch (NumberFormatException ex) {
            this.plugin.logDebug("[AntiSeedCracker] Error parsing version number: " + versionString + ", error: " + ex.getMessage());
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean isPrimitiveWrapperMatch(Class firstClass, Class secondClass) {
      if (firstClass.isPrimitive()) {
         return firstClass == this.toPrimitiveClass(secondClass);
      } else {
         return secondClass.isPrimitive() ? secondClass == this.toPrimitiveClass(firstClass) : false;
      }
   }
}
