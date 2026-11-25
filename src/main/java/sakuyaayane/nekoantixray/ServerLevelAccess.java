package sakuyaayane.nekoantixray;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class ServerLevelAccess {
   private ServerLevelAccess() {
   }

   public static ServerLevel getServerLevel(ServerPlayer serverPlayer) {
      try {
         Class<?>[] emptyParams = new Class[0];
         return (ServerLevel)ServerPlayer.class.getMethod("serverLevel", emptyParams).invoke(serverPlayer);
      } catch (ReflectiveOperationException primaryEx) {
         try {
            Class<?>[] fallbackParams = new Class[0];
            return (ServerLevel)ServerPlayer.class.getMethod("level", fallbackParams).invoke(serverPlayer);
         } catch (ReflectiveOperationException fallbackEx) {
            throw new IllegalStateException("无法解析 ServerPlayer#level/serverLevel 方法", fallbackEx);
         }
      }
   }
}
