package sakuyaayane.nekoantixray.compat;

import java.lang.reflect.Method;
import java.util.Arrays;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.World;
import sakuyaayane.nekoantixray.NekoAntiXray;

class ChunkAccessWrapperFactory {
   private static final NamespacedKey SECURE_SLIME_CHUNK_KEY = new NamespacedKey("nekoantixray", "secure_slime_chunk");

   static ChunkAccessWrapper createWrapper(Object level, int chunkX, int chunkZ) {
      if (!ChunkAccessTransformer.isCompatible()) {
         return null;
      }

      try {
         Object chunk = ChunkAccessTransformer.getChunk(level, chunkX, chunkZ);
         if (chunk == null) {
            return null;
         } else {
            return ChunkAccessTransformer.isHighVersionMode()
               ? new ChunkAccessWrapperFactory.HighVersionChunkWrapper(chunk, chunkX, chunkZ)
               : new ChunkAccessWrapperFactory.LowVersionChunkWrapper(chunk, chunkX, chunkZ);
         }
      } catch (Exception ex) {
         NekoAntiXray plugin = NekoAntiXray.getInstance();
         if (plugin == null) {
            return null;
         }

         plugin.logDebug("[ChunkAccessWrapperFactory] Failed to create wrapper: " + ex.getMessage());
         return null;
      }
   }

   static class HighVersionChunkWrapper implements ChunkAccessWrapper {
      private final Object chunk;
      private final int chunkX;
      private final int chunkZ;

      HighVersionChunkWrapper(Object chunk, int chunkX, int chunkZ) {
         this.chunk = chunk;
         this.chunkX = chunkX;
         this.chunkZ = chunkZ;
      }

      @Override
      public boolean saveSecureSlimeChunkResult(boolean isSlimeChunk) {
         return this.storeSecureSlimeChunkResult(isSlimeChunk);
      }

      @Override
      public int getChunkX() {
         return this.chunkX;
      }

      @Override
      public int getChunkZ() {
         return this.chunkZ;
      }

      @Override
      public boolean isValid() {
         return this.hasChunk();
      }

      @Override
      public Object getChunk() {
         return this.chunk;
      }

      public boolean storeSecureSlimeChunkResult(boolean isSlimeChunk) {
         try {
            if (this.chunk instanceof LevelChunk levelChunk) {
               CraftWorld world = levelChunk.getLevel().getWorld();
               if (world != null) {
                  Chunk bukkitChunk = world.getChunkAt(this.chunkX, this.chunkZ);
                  if (bukkitChunk != null) {
                     bukkitChunk.getPersistentDataContainer().set(ChunkAccessWrapperFactory.SECURE_SLIME_CHUNK_KEY, PersistentDataType.BYTE, (byte)(isSlimeChunk ? 1 : 0));
                     return true;
                  }
               }
            }
         } catch (Exception ex) {
            NekoAntiXray plugin = NekoAntiXray.getInstance();
            if (plugin != null) {
               plugin.logDebug("[HighVersionChunkWrapper] Failed to store slime chunk result: " + ex.getMessage());
            }
         }

         return false;
      }

      @Override
      public Boolean getSecureSlimeChunkResult() {
         try {
            if (this.chunk instanceof LevelChunk levelChunk) {
               CraftWorld world = levelChunk.getLevel().getWorld();
               if (world != null) {
                  Chunk bukkitChunk = world.getChunkAt(this.chunkX, this.chunkZ);
                  if (bukkitChunk != null) {
                     Byte storedFlag = bukkitChunk.getPersistentDataContainer().get(ChunkAccessWrapperFactory.SECURE_SLIME_CHUNK_KEY, PersistentDataType.BYTE);
                     return storedFlag != null ? storedFlag == 1 : null;
                  }
               }
            }
         } catch (Exception ex) {
            NekoAntiXray plugin = NekoAntiXray.getInstance();
            if (plugin != null) {
               plugin.logDebug("[HighVersionChunkWrapper] Failed to get slime chunk result: " + ex.getMessage());
            }
         }

         return null;
      }

      public boolean hasChunk() {
         return this.chunk != null;
      }
   }

   static class LowVersionChunkWrapper implements ChunkAccessWrapper {
      private final Object chunk;
      private final int chunkX;
      private final int chunkZ;

      LowVersionChunkWrapper(Object chunk, int chunkX, int chunkZ) {
         this.chunk = chunk;
         this.chunkX = chunkX;
         this.chunkZ = chunkZ;
      }

      @Override
      public boolean saveSecureSlimeChunkResult(boolean isSlimeChunk) {
         return this.storeSecureSlimeChunkResult(isSlimeChunk);
      }

      @Override
      public int getChunkX() {
         return this.chunkX;
      }

      @Override
      public int getChunkZ() {
         return this.chunkZ;
      }

      @Override
      public boolean isValid() {
         return this.hasChunk();
      }

      @Override
      public Object getChunk() {
         return this.chunk;
      }

      public boolean storeSecureSlimeChunkResult(boolean isSlimeChunk) {
         try {
            Method getLevelMethod = this.chunk.getClass().getMethod("getLevel");
            Object level = getLevelMethod.invoke(this.chunk);
            Method getWorldMethod = level.getClass().getMethod("getWorld");
            World world = (World)getWorldMethod.invoke(level);
            if (world != null) {
               Chunk bukkitChunk = world.getChunkAt(this.chunkX, this.chunkZ);
               if (bukkitChunk != null) {
                  bukkitChunk.getPersistentDataContainer().set(ChunkAccessWrapperFactory.SECURE_SLIME_CHUNK_KEY, PersistentDataType.BYTE, (byte)(isSlimeChunk ? 1 : 0));
                  return true;
               }
            }
         } catch (Exception ex) {
            NekoAntiXray plugin = NekoAntiXray.getInstance();
            if (plugin != null) {
               plugin.logDebug("[LowVersionChunkWrapper] Failed to store slime chunk result using reflection: " + ex.getMessage());
               plugin.logDebug("[LowVersionChunkWrapper] Chunk class: " + this.chunk.getClass().getName());
               plugin.logDebug("[LowVersionChunkWrapper] Available methods: " + Arrays.toString(this.chunk.getClass().getDeclaredMethods()));
            }

            try {
               Bukkit.getWorlds().forEach(fallbackWorld -> {
                  try {
                     Chunk fallbackChunk = fallbackWorld.getChunkAt(this.chunkX, this.chunkZ);
                     if (fallbackChunk != null && fallbackChunk.isLoaded()) {
                        fallbackChunk.getPersistentDataContainer().set(ChunkAccessWrapperFactory.SECURE_SLIME_CHUNK_KEY, PersistentDataType.BYTE, (byte)(isSlimeChunk ? 1 : 0));
                     }
                  } catch (Exception ignored) {
                  }
               });
               return true;
            } catch (Exception fallbackEx) {
               if (plugin != null) {
                  plugin.logDebug("[LowVersionChunkWrapper] Fallback method also failed: " + fallbackEx.getMessage());
               }
            }
         }

         return false;
      }

      @Override
      public Boolean getSecureSlimeChunkResult() {
         try {
            Method getLevelMethod = this.chunk.getClass().getMethod("getLevel");
            Object level = getLevelMethod.invoke(this.chunk);
            Method getWorldMethod = level.getClass().getMethod("getWorld");
            World world = (World)getWorldMethod.invoke(level);
            if (world != null) {
               Chunk bukkitChunk = world.getChunkAt(this.chunkX, this.chunkZ);
               if (bukkitChunk != null) {
                  Byte storedFlag = bukkitChunk.getPersistentDataContainer().get(ChunkAccessWrapperFactory.SECURE_SLIME_CHUNK_KEY, PersistentDataType.BYTE);
                  return storedFlag != null ? storedFlag == 1 : null;
               }
            }
         } catch (Exception ex) {
            NekoAntiXray plugin = NekoAntiXray.getInstance();
            if (plugin != null) {
               plugin.logDebug("[LowVersionChunkWrapper] Failed to get slime chunk result: " + ex.getMessage());
            }
         }

         return null;
      }

      public boolean hasChunk() {
         return this.chunk != null;
      }
   }
}
