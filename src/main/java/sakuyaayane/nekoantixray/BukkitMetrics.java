package sakuyaayane.nekoantixray;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitMetrics {
   private final Plugin plugin;
   private final MetricsReporter metricsReporter;

   public BukkitMetrics(JavaPlugin plugin, int serviceId) {
      this.plugin = plugin;
      File bStatsFolder = new File(plugin.getDataFolder().getParentFile(), "bStats");
      File configFile = new File(bStatsFolder, "config.yml");
      YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
      if (!config.isSet("serverUuid")) {
         config.addDefault("enabled", true);
         config.addDefault("serverUuid", UUID.randomUUID().toString());
         config.addDefault("logFailedRequests", false);
         config.addDefault("logSentData", false);
         config.addDefault("logResponseStatusText", false);
         config.options()
            .header(
               "bStats (https://bStats.org) collects some basic information for plugin authors, like how\nmany people use their plugin and their total player count. It's recommended to keep bStats\nenabled, but if you're not comfortable with this, you can turn this setting off. There is no\nperformance penalty associated with having metrics enabled, and data sent to bStats is fully\nanonymous."
            )
            .copyDefaults(true);

         try {
            config.save(configFile);
         } catch (IOException ignored) {
         }
      }

      boolean enabled = config.getBoolean("enabled", true);
      String serverUuid = config.getString("serverUuid");
      boolean logErrors = config.getBoolean("logFailedRequests", false);
      boolean logSentData = config.getBoolean("logSentData", false);
      boolean logResponseStatusText = config.getBoolean("logResponseStatusText", false);
      this.metricsReporter = new MetricsReporter(
         "bukkit",
         serverUuid,
         serviceId,
         enabled,
         platformDataBuilder -> this.appendPlatformData((JsonBuilder)platformDataBuilder),
         serviceDataBuilder -> this.appendServiceData((JsonBuilder)serviceDataBuilder),
         task -> submitTaskOnMainThread(plugin, (Runnable)task),
         () -> plugin.isEnabled(),
         (errorMessage, error) -> this.logError((String)errorMessage, (Throwable)error),
         infoMessage -> this.logInfo((String)infoMessage),
         logErrors,
         logSentData,
         logResponseStatusText
      );
   }

   public void shutdown() {
      this.metricsReporter.shutdown();
   }

   private void appendPlatformData(JsonBuilder jsonBuilder) {
      int playerAmount = this.getPlayerAmount();
      jsonBuilder.appendField("playerAmount", playerAmount);
      int onlineMode = Bukkit.getOnlineMode() ? 1 : 0;
      jsonBuilder.appendField("onlineMode", onlineMode);
      String bukkitVersion = Bukkit.getVersion();
      jsonBuilder.appendField("bukkitVersion", bukkitVersion);
      String bukkitName = Bukkit.getName();
      jsonBuilder.appendField("bukkitName", bukkitName);
      String javaVersion = System.getProperty("java.version");
      jsonBuilder.appendField("javaVersion", javaVersion);
      String osName = System.getProperty("os.name");
      jsonBuilder.appendField("osName", osName);
      String osArch = System.getProperty("os.arch");
      jsonBuilder.appendField("osArch", osArch);
      String osVersion = System.getProperty("os.version");
      jsonBuilder.appendField("osVersion", osVersion);
      int coreCount = Runtime.getRuntime().availableProcessors();
      jsonBuilder.appendField("coreCount", coreCount);
   }

   private void appendServiceData(JsonBuilder jsonBuilder) {
      String pluginVersion = this.plugin.getDescription().getVersion();
      jsonBuilder.appendField("pluginVersion", pluginVersion);
   }

   private void logInfo(String message) {
      this.plugin.getLogger().log(Level.INFO, message);
   }

   private void logError(String message, Throwable error) {
      this.plugin.getLogger().log(Level.WARNING, message, error);
   }

   private static void submitTaskOnMainThread(JavaPlugin plugin, Runnable task) {
      Bukkit.getScheduler().runTask(plugin, task);
   }

   public void addCustomChart(MetricsCustomChart metricsCustomChart) {
      this.metricsReporter.addCustomChart(metricsCustomChart);
   }

   private int getPlayerAmount() {
      try {
         Method getOnlinePlayersMethod = Class.forName("org.bukkit.Server").getMethod("getOnlinePlayers");
         return getOnlinePlayersMethod.getReturnType().equals(Collection.class)
            ? ((Collection<?>)getOnlinePlayersMethod.invoke(Bukkit.getServer())).size()
            : ((Player[])getOnlinePlayersMethod.invoke(Bukkit.getServer())).length;
      } catch (Exception ex) {
         return Bukkit.getOnlinePlayers().size();
      }
   }
}
