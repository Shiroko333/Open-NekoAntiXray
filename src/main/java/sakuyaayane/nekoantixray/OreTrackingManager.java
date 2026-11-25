package sakuyaayane.nekoantixray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class OreTrackingManager {
   private final NekoAntiXray plugin;
   private final OreStatsStorage oreStatsStorage;
   private final Map<String, List<String>> trackingSubscriptions;

   public OreTrackingManager(NekoAntiXray plugin, OreStatsStorage oreStatsStorage) {
      this.plugin = plugin;
      this.oreStatsStorage = oreStatsStorage;
      this.trackingSubscriptions = new HashMap<>();
      this.loadTrackingConfig();
   }

   private void loadTrackingConfig() {
      this.trackingSubscriptions.clear();
      if (this.plugin.getConfig().contains("ore-tracking")) {
         ConfigurationSection section = this.plugin.getConfig().getConfigurationSection("ore-tracking");
         if (section != null) {
            for (String trackerName : section.getKeys(false)) {
               List<String> trackedPlayers = this.plugin.getConfig().getStringList("ore-tracking." + trackerName);
               this.trackingSubscriptions.put(trackerName, new ArrayList<>(trackedPlayers));
            }
         }
      }
   }

   private void saveTrackingConfig() {
      this.plugin.getConfig().set("ore-tracking", null);

      for (Entry<String, List<String>> entry : this.trackingSubscriptions.entrySet()) {
         this.plugin.getConfig().set("ore-tracking." + entry.getKey(), entry.getValue());
      }

      this.plugin.saveConfig();
   }

   public boolean toggleTracking(Player player, String targetName) {
      String trackerName = player.getName();
      List<String> trackedPlayers = this.trackingSubscriptions.computeIfAbsent(trackerName, ignored -> new ArrayList<>());
      boolean nowTracking;
      if (trackedPlayers.contains(targetName)) {
         trackedPlayers.remove(targetName);
         nowTracking = false;
      } else {
         trackedPlayers.add(targetName);
         nowTracking = true;
      }

      this.saveTrackingConfig();
      return nowTracking;
   }

   public boolean isTrackingPlayer(Player player, String targetName) {
      List<String> trackedPlayers = this.trackingSubscriptions.get(player.getName());
      return trackedPlayers != null && trackedPlayers.contains(targetName);
   }

   public List getTrackersOf(String targetName) {
      List<String> trackers = new ArrayList<>();

      for (Entry<String, List<String>> entry : this.trackingSubscriptions.entrySet()) {
         if (entry.getValue().contains(targetName)) {
            trackers.add(entry.getKey());
         }
      }

      return trackers;
   }

   public void notifyTrackers(Player player, Material material, Location location) {
      ConfigManager configManager = this.plugin.getConfigManager();
      if (configManager.isOreStatsEnabled()) {
         String minerName = player.getName();
         List<String> trackers = this.getTrackersOf(minerName);
         if (!trackers.isEmpty()) {
            String template = this.plugin
               .getConfig()
               .getString("ore-stats.tracking-notification", "§6[OreTracker] §e%player% §7mined §b%ore% §7at §aX:%x% Y:%y% Z:%z% §7(%world%)");
            String notification = template.replace("%player%", minerName)
               .replace("%ore%", this.formatMaterialName(material))
               .replace("%x%", String.valueOf(location.getBlockX()))
               .replace("%y%", String.valueOf(location.getBlockY()))
               .replace("%z%", String.valueOf(location.getBlockZ()))
               .replace("%world%", location.getWorld().getName());

            for (String trackerName : trackers) {
               Player trackerPlayer = Bukkit.getPlayerExact(trackerName);
               if (trackerPlayer != null && trackerPlayer.isOnline()) {
                  this.plugin.getFoliaScheduler().getImpl().runAtEntity(trackerPlayer, wrappedTask -> trackerPlayer.sendMessage(notification));
               }
            }
         }
      }
   }

   private String formatMaterialName(Material material) {
      String spacedName = material.name().replace("_", " ");
      String[] words = spacedName.split(" ");
      StringBuilder formatted = new StringBuilder();

      for (String word : words) {
         if (word.length() > 0) {
            formatted.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()).append(" ");
         }
      }

      return formatted.toString().trim();
   }

   public List getTrackedPlayers(String trackerName) {
      return this.trackingSubscriptions.getOrDefault(trackerName, new ArrayList<>());
   }

   public void reload() {
      this.loadTrackingConfig();
   }

   public void clearTrackingFor(String trackerName) {
      if (this.trackingSubscriptions.remove(trackerName) != null) {
         this.saveTrackingConfig();
      }
   }
}
