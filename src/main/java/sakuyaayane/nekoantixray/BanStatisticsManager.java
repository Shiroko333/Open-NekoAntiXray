package sakuyaayane.nekoantixray;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class BanStatisticsManager {
   private final NekoAntiXray plugin;
   private final File statisticsFile;
   private FileConfiguration configuration;
   private int totalBans = 0;
   private static final long MILLIS_PER_DAY = 86400000L;

   public BanStatisticsManager(NekoAntiXray plugin) {
      this.plugin = plugin;
      this.statisticsFile = new File(plugin.getDataFolder(), plugin.getConfig().getString("ban-statistics.file-path", "ban-statistics.yml"));
      this.loadStatistics();
   }

   public int getTotalBans() {
      return this.totalBans;
   }

   public void recordBan(Player player, String reason, double violationLevel) {
      if (this.plugin.getConfig().getBoolean("ban-statistics.enabled", true)) {
         CompletableFuture.runAsync(() -> {
            synchronized (this) {
               ;
            }

            this.totalBans++;
            this.configuration.set("total-bans", this.totalBans);
            if (this.plugin.getConfig().getBoolean("ban-statistics.record-details", true)) {
               String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
               String entryPath = "bans." + timestamp + "_" + player.getName();
               this.configuration.set(entryPath + ".player", player.getName());
               this.configuration.set(entryPath + ".uuid", player.getUniqueId().toString());
               this.configuration.set(entryPath + ".reason", reason);
               this.configuration.set(entryPath + ".vl", violationLevel);
               this.configuration.set(entryPath + ".timestamp", timestamp);
               this.configuration.set(entryPath + ".location", this.formatPlayerLocation(player));
            }

            this.saveStatistics();
            this.plugin.logDebug("记录封禁统计: " + player.getName() + ", 总封禁数: " + this.totalBans);
         });
      }
   }

   public List getBanHistory(int limit) {
      return !this.configuration.contains("bans")
         ? List.of()
         : this.configuration.getConfigurationSection("bans").getKeys(false).stream().sorted((firstKey, secondKey) -> secondKey.compareTo(firstKey)).limit(limit).map(entryKey -> {
            String playerName = this.configuration.getString("bans." + entryKey + ".player");
            String reason = this.configuration.getString("bans." + entryKey + ".reason");
            String timestamp = this.configuration.getString("bans." + entryKey + ".timestamp");
            return String.format("[%s] %s - %s", timestamp, playerName, reason);
         }).toList();
   }

   private void saveStatistics() {
      try {
         this.configuration.save(this.statisticsFile);
      } catch (IOException ex) {
         this.plugin.getLogger().severe("无法保存封禁统计文件: " + ex.getMessage());
      }
   }

   private String formatPlayerLocation(Player player) {
      return String.format("%.1f,%.1f,%.1f,%s", player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), player.getWorld().getName());
   }

   private void loadStatistics() {
      if (!this.statisticsFile.exists()) {
         try {
            this.statisticsFile.getParentFile().mkdirs();
            this.statisticsFile.createNewFile();
         } catch (IOException ex) {
            this.plugin.getLogger().severe("无法创建封禁统计文件: " + ex.getMessage());
            return;
         }
      }

      this.configuration = YamlConfiguration.loadConfiguration(this.statisticsFile);
      if (!this.configuration.contains("total-bans")) {
         this.configuration.set("total-bans", 0);
         this.configuration.set("created-date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
         this.saveStatistics();
      }

      this.totalBans = this.configuration.getInt("total-bans", 0);
   }

   public void resetStatistics() {
      synchronized (this) {
         ;
      }

      this.totalBans = 0;
      this.configuration.set("total-bans", 0);
      if (this.configuration.contains("bans")) {
         this.configuration.set("bans", null);
      }

      this.configuration.set("last-reset", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
      this.saveStatistics();
      this.plugin.getLogger().info("封禁统计已重置");
   }

   public void checkAutoReset() {
      int resetPeriodDays = this.plugin.getConfig().getInt("ban-statistics.reset-period", -1);
      if (resetPeriodDays > 0) {
         String referenceDateText = this.configuration.getString("last-reset");
         if (referenceDateText == null) {
            referenceDateText = this.configuration.getString("created-date");
         }

         if (referenceDateText != null) {
            try {
               SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
               Date referenceDate = dateFormat.parse(referenceDateText);
               Date currentDate = new Date();
               long daysSinceReset = (currentDate.getTime() - referenceDate.getTime()) / 86400000L;
               if (daysSinceReset >= resetPeriodDays) {
                  this.resetStatistics();
                  this.plugin.getLogger().info("自动重置封禁统计，重置周期: " + resetPeriodDays + " 天");
               }
            } catch (Exception ex) {
               this.plugin.getLogger().warning("检查重置周期时发生错误: " + ex.getMessage());
            }
         }
      }
   }
}
