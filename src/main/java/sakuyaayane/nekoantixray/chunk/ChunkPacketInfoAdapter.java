package sakuyaayane.nekoantixray.chunk;

import io.papermc.paper.antixray.ChunkPacketInfo;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.chunk.LevelChunk;

public final class ChunkPacketInfoAdapter {
   private static final Constructor<ChunkPacketInfo<?>> PACKET_AND_CHUNK_CONSTRUCTOR = findChunkPacketInfoConstructor(
      new Class[]{ClientboundLevelChunkWithLightPacket.class, LevelChunk.class}
   );
   private static final Constructor<ChunkPacketInfo<?>> CHUNK_ONLY_CONSTRUCTOR = findChunkPacketInfoConstructor(new Class[]{LevelChunk.class});
   private static final Method SET_CHUNK_PACKET_METHOD = findChunkPacketInfoMethod("setChunkPacket", new Class[]{ClientboundLevelChunkWithLightPacket.class});
   private static final Method GET_CHUNK_DATA_METHOD = findChunkPacketMethod("getChunkData");
   private static final Method CHUNK_DATA_RECORD_METHOD = findChunkPacketMethod("chunkData");

   private ChunkPacketInfoAdapter() {
   }

   private static Constructor findChunkPacketInfoConstructor(Class[] parameterTypes) {
      try {
         Constructor constructor = ChunkPacketInfo.class.getDeclaredConstructor(parameterTypes);
         constructor.setAccessible(true);
         return constructor;
      } catch (Throwable ex) {
         return null;
      }
   }

   private static Method findChunkPacketInfoMethod(String methodName, Class[] parameterTypes) {
      try {
         Method method = ChunkPacketInfo.class.getMethod(methodName, parameterTypes);
         method.setAccessible(true);
         return method;
      } catch (Throwable ex) {
         return null;
      }
   }

   private static Method findChunkPacketMethod(String methodName) {
      try {
         Method method = ClientboundLevelChunkWithLightPacket.class.getMethod(methodName);
         method.setAccessible(true);
         return method;
      } catch (Throwable ex) {
         return null;
      }
   }

   public static boolean usesChunkOnlyConstructor() {
      return CHUNK_ONLY_CONSTRUCTOR != null;
   }

   public static boolean usesRecordChunkDataAccessor() {
      return CHUNK_DATA_RECORD_METHOD != null && GET_CHUNK_DATA_METHOD == null;
   }

   public static ChunkPacketInfo createChunkPacketInfo(ClientboundLevelChunkWithLightPacket packet, LevelChunk levelChunk) {
      try {
         if (PACKET_AND_CHUNK_CONSTRUCTOR != null) {
            return PACKET_AND_CHUNK_CONSTRUCTOR.newInstance(packet, levelChunk);
         }

         if (CHUNK_ONLY_CONSTRUCTOR == null) {
            throw new IllegalStateException("未找到可用的 ChunkPacketInfo 构造器(当前核心不支持反矿透 API)");
         }

         ChunkPacketInfo chunkPacketInfo = CHUNK_ONLY_CONSTRUCTOR.newInstance(levelChunk);
         if (packet != null && SET_CHUNK_PACKET_METHOD != null) {
            SET_CHUNK_PACKET_METHOD.invoke(chunkPacketInfo, packet);
         }

         return chunkPacketInfo;
      } catch (IllegalStateException ex) {
         throw ex;
      } catch (Throwable cause) {
         throw new IllegalStateException("创建 ChunkPacketInfo 失败(当前核心不兼容): " + cause, cause);
      }
   }

   public static ClientboundLevelChunkPacketData getChunkPacketData(ClientboundLevelChunkWithLightPacket packet) {
      if (packet == null) {
         return null;
      }

      try {
         if (GET_CHUNK_DATA_METHOD != null) {
            return (ClientboundLevelChunkPacketData)GET_CHUNK_DATA_METHOD.invoke(packet);
         }

         if (CHUNK_DATA_RECORD_METHOD != null) {
            return (ClientboundLevelChunkPacketData)CHUNK_DATA_RECORD_METHOD.invoke(packet);
         }
      } catch (Throwable ignored) {
      }

      return null;
   }

   public static String describeDetectedApi() {
      return "ChunkPacketInfo="
         + (usesChunkOnlyConstructor() ? "26.3+(区块构造+setChunkPacket)" : "26.2(包+区块构造)")
         + ", chunkData="
         + (usesRecordChunkDataAccessor() ? "26.3+(record chunkData())" : "26.2(getChunkData())");
   }
}