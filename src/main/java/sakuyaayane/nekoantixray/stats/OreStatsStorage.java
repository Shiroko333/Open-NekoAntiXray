package sakuyaayane.nekoantixray.stats;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import sakuyaayane.nekoantixray.NekoAntiXray;

public class OreStatsStorage {
   private final NekoAntiXray plugin;
   private File statsFile;
   private FileConfiguration configuration;

   public void reload() {
      this.configuration = YamlConfiguration.loadConfiguration(this.statsFile);
   }

   public FileConfiguration getConfiguration() {
      return this.configuration;
   }

   private static int compareByCountDescending(Entry firstEntry, Entry secondEntry) {
      return ((Integer)secondEntry.getValue()).compareTo((Integer)firstEntry.getValue());
   }

   private void initializeFile() {
      this.statsFile = new File(this.plugin.getDataFolder(), "ore-stats.yml");
      if (!this.statsFile.exists()) {
         this.statsFile.getParentFile().mkdirs();

         try {
            this.statsFile.createNewFile();
            this.plugin.getLogger().info("Created new ore-stats.yml file");
         } catch (IOException ex) {
            this.plugin.getLogger().severe("Failed to create ore-stats.yml: " + ex.getMessage());
         }
      }

      this.configuration = YamlConfiguration.loadConfiguration(this.statsFile);
   }

   public Map getPlayerOreCounts(String playerName) {
      Map<String, Integer> oreCounts = new HashMap<>();
      if (!this.configuration.contains(playerName)) {
         return oreCounts;
      }

      ConfigurationSection playerSection = this.configuration.getConfigurationSection(playerName);
      if (playerSection != null) {
         for (String oreName : playerSection.getKeys(false)) {
            oreCounts.put(oreName, this.configuration.getInt(playerName + "." + oreName));
         }
      }

      return oreCounts;
   }

   public List getTopPlayers(int limit) {
      Map<String, Integer> playerTotals = new HashMap<>();

      for (String playerName : this.configuration.getKeys(false)) {
         int totalOres = 0;
         ConfigurationSection playerSection = this.configuration.getConfigurationSection(playerName);
         if (playerSection != null) {
            for (String oreName : playerSection.getKeys(false)) {
               totalOres += this.configuration.getInt(playerName + "." + oreName, 0);
            }
         }

         playerTotals.put(playerName, totalOres);
      }

      return playerTotals.entrySet()
         .stream()
         .sorted((firstEntry, secondEntry) -> compareByCountDescending(firstEntry, secondEntry))
         .limit(limit)
         .collect(Collectors.toList());
   }

   public boolean hasPlayerStats(String playerName) {
      return this.configuration.contains(playerName);
   }

   public void save() {
      try {
         this.configuration.save(this.statsFile);
      } catch (IOException ex) {
         this.plugin.getLogger().severe("Failed to save ore-stats.yml: " + ex.getMessage());
      }
   }

   public OreStatsStorage(NekoAntiXray plugin) {
      this.plugin = plugin;
      this.initializeFile();
   }

   public void incrementOreCount(Player player, Material material) {
      if (player != null && material != null) {
         String playerName = player.getName();
         String statPath = playerName + "." + material.name();
         int currentCount = this.configuration.getInt(statPath, 0);
         this.configuration.set(statPath, currentCount + 1);
         this.save();
      }
   }

   public void clearPlayerStats(String playerName) {
      if (this.configuration.contains(playerName)) {
         this.configuration.set(playerName, null);
         this.save();
      }
   }
}
