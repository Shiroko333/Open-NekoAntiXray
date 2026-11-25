package sakuyaayane.nekoantixray;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class RealSeedStorage {
   private final NekoAntiXray plugin;
   private final File storageFile;
   private FileConfiguration storage;
   private final Map<String, Long> worldSeeds = new HashMap<>();

   public RealSeedStorage(NekoAntiXray plugin) {
      this.plugin = plugin;
      this.storageFile = new File(plugin.getDataFolder(), "real-seeds.yml");
      this.loadSeeds();
   }

   public Map reload() {
      return new HashMap<>(this.worldSeeds);
   }

   private void loadSeeds() {
      if (!this.storageFile.exists()) {
         try {
            this.storageFile.getParentFile().mkdirs();
            this.storageFile.createNewFile();
            this.plugin.logDebug("[RealSeedStorage] Created new real-seeds.yml file");
         } catch (IOException ex) {
            this.plugin.getLogger().severe("[RealSeedStorage] Failed to create real-seeds.yml: " + ex.getMessage());
         }
      }

      this.storage = YamlConfiguration.loadConfiguration(this.storageFile);
      if (this.storage.contains("worlds")) {
         for (String worldName : this.storage.getConfigurationSection("worlds").getKeys(false)) {
            long seed = this.storage.getLong("worlds." + worldName + ".seed", 0L);
            this.worldSeeds.put(worldName, seed);
            this.plugin.logDebug("[RealSeedStorage] Loaded seed for world: " + worldName);
         }
      }

      this.plugin.logDebug("[RealSeedStorage] Loaded " + this.worldSeeds.size() + " world seeds from storage");
   }

   private void saveStorage() {
      try {
         this.storage.save(this.storageFile);
         this.plugin.logDebug("[RealSeedStorage] Saved seed storage file");
      } catch (IOException ex) {
         this.plugin.getLogger().severe("[RealSeedStorage] Failed to save real-seeds.yml: " + ex.getMessage());
      }
   }

   public boolean storeWorldSeedIfAbsent(World world) {
      String worldName = world.getName();
      if (this.worldSeeds.containsKey(worldName)) {
         this.plugin.logDebug("[RealSeedStorage] Seed for world " + worldName + " already stored, skipping");
         return false;
      } else {
         long seed = this.extractRealSeed(world);
         this.worldSeeds.put(worldName, seed);
         this.storage.set("worlds." + worldName + ".seed", seed);
         this.storage.set("worlds." + worldName + ".stored-at", System.currentTimeMillis());
         this.storage.set("worlds." + worldName + ".dimension", world.getEnvironment().name());
         this.saveStorage();
         this.plugin.logDebug("[RealSeedStorage] Stored real seed for world: " + worldName + " = " + seed);
         return true;
      }
   }

   private long extractRealSeed(World world) {
      try {
         long seed = world.getSeed();
         this.plugin.logDebug("[RealSeedStorage] Extracted seed via Bukkit API: " + seed);
         return seed;
      } catch (Throwable bukkitError) {
         this.plugin.logDebug("[RealSeedStorage] Bukkit API failed: " + bukkitError.getMessage());

         try {
            Object worldHandle = world.getClass().getMethod("getHandle").invoke(world);
            Object levelData = null;

            for (String levelDataMethod : new String[]{"getLevelData", "q_", "r_", "levelData"}) {
               try {
                  levelData = worldHandle.getClass().getMethod(levelDataMethod).invoke(worldHandle);
                  break;
               } catch (Throwable ignored) {
               }
            }

            if (levelData != null) {
               for (String seedMethod : new String[]{"getSeed", "getLevelSeed", "m", "n"}) {
                  try {
                     Object seedResult = levelData.getClass().getMethod(seedMethod).invoke(levelData);
                     if (seedResult instanceof Long) {
                        long seed = (Long)seedResult;
                        this.plugin.logDebug("[RealSeedStorage] Extracted seed via NMS: " + seed);
                        return seed;
                     }
                  } catch (Throwable ignored) {
                  }
               }
            }
         } catch (Throwable reflectionError) {
            this.plugin.logDebug("[RealSeedStorage] NMS reflection failed: " + reflectionError.getMessage());
         }

         this.plugin.getLogger().warning("[RealSeedStorage] Failed to extract real seed for world: " + world.getName());
         return 0L;
      }
   }

   public Long getSeed(String worldName) {
      return this.worldSeeds.get(worldName);
   }

   public Long getSeed(World world) {
      return this.getSeed(world.getName());
   }

   public boolean hasSeed(String worldName) {
      return this.worldSeeds.containsKey(worldName);
   }

   public void setManualSeed(String worldName, long seed) {
      this.worldSeeds.put(worldName, seed);
      this.storage.set("worlds." + worldName + ".seed", seed);
      this.storage.set("worlds." + worldName + ".manually-set", true);
      this.storage.set("worlds." + worldName + ".set-at", System.currentTimeMillis());
      this.saveStorage();
      Logger logger = this.plugin.getLogger();
      logger.info("[RealSeedStorage] Manually set real seed for world: " + worldName + " = " + seed);
   }

   public void reloadFromDisk() {
      this.worldSeeds.clear();
      this.loadSeeds();
   }
}
