package sakuyaayane.nekoantixray;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigManager {
   private final NekoAntiXray plugin;
   private final Set<String> warnedConfigPaths = ConcurrentHashMap.newKeySet();
   private FileConfiguration config;
   private List<Material> blacklistedOres;
   private List<String> blacklistWorlds;
   private List<Material> massExcavationTargetBlocks;
   private File configFile;
   private boolean antiSeedCrackerEnable;
   private boolean antiSeedCrackerModifyHashedSeedEnable;
   private boolean antiSeedCrackerModifyEndStructuresEnable;
   private List<String> antiSeedCrackerModifyEndStructuresWorlds;
   private boolean antiSeedCrackerModifyEndStructuresEndSpikesEnable;
   private String antiSeedCrackerModifyEndStructuresEndSpikesMode;
   private boolean antiSeedCrackerModifyEndStructuresEndCitiesEnable;
   private boolean antiSeedCrackerNetherBedrockProtectionEnabled;
   private boolean antiSeedCrackerNetherBedrockProtectionBlockRoofAccess;
   private int antiSeedCrackerNetherBedrockProtectionRoofAccessMaxY;
   private boolean secureSeedBiomeSourceXorEnabled;
   private long secureSeedBiomeCoordinateXor;
   private boolean antiSeedCrackerBiomePacketObfuscationEnabled;
   private boolean antiSeedCrackerStructureSeedProtectionEnabled;
   private String antiSeedCrackerStructureSeedProtectionStrategy;
   private boolean antiSeedCrackerStructureSeedProtectionStructuresDesertPyramid;
   private boolean antiSeedCrackerStructureSeedProtectionStructuresJungleTemple;
   private boolean antiSeedCrackerStructureSeedProtectionStructuresSwampHut;
   private boolean antiSeedCrackerStructureSeedProtectionStructuresIgloo;
   private boolean antiSeedCrackerStructureSeedProtectionStructuresPillagerOutpost;
   private boolean antiSeedCrackerStructureSeedProtectionStructuresShipwreck;
   private boolean antiSeedCrackerDecoratorPacketObfuscationEnabled;
   private String antiSeedCrackerDecoratorPacketObfuscationMode;
   private boolean antiSeedCrackerDecoratorPacketObfuscationDungeonsEnabled;
   private boolean antiSeedCrackerDecoratorPacketObfuscationDungeonsStripSpawnerBlockEntity;
   private boolean antiSeedCrackerDecoratorPacketObfuscationDungeonsReplaceSpawnerBlockInStrictMode;
   private boolean antiSeedCrackerDecoratorPacketObfuscationDungeonsPerturbFloorInStrictMode;
   private boolean antiSeedCrackerDecoratorPacketObfuscationDungeonsBreakFloorCount;
   private int antiSeedCrackerDecoratorPacketObfuscationDungeonsFloorTargetCount;
   private boolean antiSeedCrackerDecoratorPacketObfuscationStructuresBuriedTreasure;
   private boolean antiSeedCrackerDecoratorPacketObfuscationStructuresShipwreck;
   private boolean antiSeedCrackerDecoratorPacketObfuscationStructuresMonument;
   private boolean antiSeedCrackerDecoratorPacketObfuscationEndGatewayEnabled;
   private boolean antiSeedCrackerDecoratorPacketObfuscationEmeraldOreEnabled;
   private boolean antiSeedCrackerDecoratorPacketObfuscationDesertWellEnabled;
   private boolean antiSeedCrackerDecoratorPacketObfuscationWarpedFungusEnabled;
   private boolean antiSeedCrackerSeedcrackerXFirewallEnabled;
   private long antiSeedCrackerSeedcrackerXFirewallShadowStateTtlSeconds;
   private int antiSeedCrackerSeedcrackerXFirewallShadowStateMaxPositionsPerPlayer;
   private boolean antiSeedCrackerSeedcrackerXFirewallRewriteBlockUpdates;
   private boolean antiSeedCrackerSeedcrackerXFirewallRewriteBlockEntityUpdates;
   private boolean antiSeedCrackerSeedcrackerXFirewallInterceptPlayerActions;
   private boolean antiSeedCrackerSeedcrackerXFirewallInterceptStartStopDigging;
   private boolean antiBrightnessEnabled;
   private int antiBrightnessLightThreshold;
   private List<Material> antiBrightnessTargetOres;
   private boolean antiBrightnessUseBlacklistedOresAsFallback;
   private List<String> antiBrightnessEnabledWorlds;
   private String antiBrightnessMessage;
   private boolean deeplearnEnable;
   private boolean deeplearnDebug;
   private boolean deeplearnViolationsEnablePunishment;
   private double deeplearnViolationsVlMultiplier;
   private int deeplearnViolationsMinVlIncrease;
   private int deeplearnViolationsMaxVlIncrease;
   private int deeplearnSequenceLength;
   private double deeplearnPredictionThreshold;
   private long deeplearnCooldownMs;
   private long deeplearnMiningTimeWindowMs;
   private double deeplearnVeinDetectionMaxDistance;
   private long deeplearnVeinDetectionMaxTimeMs;
   private int deeplearnVeinDetectionMaxConsecutiveSkips;
   private double deeplearnVeinDetectionMaxVerticalDistance;
   private List<String> deeplearnValuableMaterials;
   private static final String[] RANDOM_ORE_CONFIG_PATHS = new String[]{
      "check.xray.spawn.random-ore", "check.xray.random-ore", "check.spawn.random-ore", "random-ore"
   };

   public List getBlacklistedOres() {
      return this.blacklistedOres;
   }

   public List getBlacklistWorlds() {
      return this.blacklistWorlds;
   }

   public boolean isAntiSeedCrackerEnable() {
      return this.antiSeedCrackerEnable;
   }

   public boolean isAntiSeedCrackerModifyHashedSeedEnable() {
      return this.antiSeedCrackerModifyHashedSeedEnable;
   }

   public boolean isAntiSeedCrackerModifyEndStructuresEnable() {
      return this.antiSeedCrackerModifyEndStructuresEnable;
   }

   public List getAntiSeedCrackerModifyEndStructuresWorlds() {
      return this.antiSeedCrackerModifyEndStructuresWorlds;
   }

   public boolean isAntiSeedCrackerModifyEndStructuresEndSpikesEnable() {
      return this.antiSeedCrackerModifyEndStructuresEndSpikesEnable;
   }

   public String getAntiSeedCrackerModifyEndStructuresEndSpikesMode() {
      return this.antiSeedCrackerModifyEndStructuresEndSpikesMode;
   }

   public boolean isAntiSeedCrackerModifyEndStructuresEndCitiesEnable() {
      return this.antiSeedCrackerModifyEndStructuresEndCitiesEnable;
   }

   public boolean isAntiSeedCrackerNetherBedrockProtectionEnabled() {
      return this.antiSeedCrackerNetherBedrockProtectionEnabled;
   }

   public boolean isAntiSeedCrackerNetherBedrockProtectionBlockRoofAccess() {
      return this.antiSeedCrackerNetherBedrockProtectionBlockRoofAccess;
   }

   public int getAntiSeedCrackerNetherBedrockProtectionRoofAccessMaxY() {
      return this.antiSeedCrackerNetherBedrockProtectionRoofAccessMaxY;
   }

   public boolean isSecureSeedBiomeSourceXorEnabled() {
      return this.secureSeedBiomeSourceXorEnabled;
   }

   public long getSecureSeedBiomeCoordinateXor() {
      return this.secureSeedBiomeCoordinateXor;
   }

   public boolean isAntiSeedCrackerBiomePacketObfuscationEnabled() {
      return this.antiSeedCrackerBiomePacketObfuscationEnabled;
   }

   public boolean isAntiSeedCrackerStructureSeedProtectionEnabled() {
      return this.antiSeedCrackerStructureSeedProtectionEnabled;
   }

   public boolean isAntiSeedCrackerStructureSeedProtectionStructuresDesertPyramid() {
      return this.antiSeedCrackerStructureSeedProtectionStructuresDesertPyramid;
   }

   public boolean isAntiSeedCrackerStructureSeedProtectionStructuresJungleTemple() {
      return this.antiSeedCrackerStructureSeedProtectionStructuresJungleTemple;
   }

   public boolean isAntiSeedCrackerStructureSeedProtectionStructuresSwampHut() {
      return this.antiSeedCrackerStructureSeedProtectionStructuresSwampHut;
   }

   public boolean isAntiSeedCrackerStructureSeedProtectionStructuresIgloo() {
      return this.antiSeedCrackerStructureSeedProtectionStructuresIgloo;
   }

   public boolean isAntiSeedCrackerStructureSeedProtectionStructuresPillagerOutpost() {
      return this.antiSeedCrackerStructureSeedProtectionStructuresPillagerOutpost;
   }

   public boolean isAntiSeedCrackerStructureSeedProtectionStructuresShipwreck() {
      return this.antiSeedCrackerStructureSeedProtectionStructuresShipwreck;
   }

   public boolean isAntiSeedCrackerDecoratorPacketObfuscationEnabled() {
      return this.antiSeedCrackerDecoratorPacketObfuscationEnabled;
   }

   public boolean isAntiSeedCrackerDecoratorPacketObfuscationDungeonsEnabled() {
      return this.antiSeedCrackerDecoratorPacketObfuscationDungeonsEnabled;
   }

   public boolean isAntiSeedCrackerDecoratorPacketObfuscationDungeonsStripSpawnerBlockEntity() {
      return this.antiSeedCrackerDecoratorPacketObfuscationDungeonsStripSpawnerBlockEntity;
   }

   public boolean isAntiSeedCrackerDecoratorPacketObfuscationDungeonsReplaceSpawnerBlockInStrictMode() {
      return this.antiSeedCrackerDecoratorPacketObfuscationDungeonsReplaceSpawnerBlockInStrictMode;
   }

   public boolean isAntiSeedCrackerDecoratorPacketObfuscationDungeonsPerturbFloorInStrictMode() {
      return this.antiSeedCrackerDecoratorPacketObfuscationDungeonsPerturbFloorInStrictMode;
   }

   public boolean isAntiSeedCrackerDecoratorPacketObfuscationDungeonsBreakFloorCount() {
      return this.antiSeedCrackerDecoratorPacketObfuscationDungeonsBreakFloorCount;
   }

   public int getAntiSeedCrackerDecoratorPacketObfuscationDungeonsFloorTargetCount() {
      return this.antiSeedCrackerDecoratorPacketObfuscationDungeonsFloorTargetCount;
   }

   public boolean isAntiSeedCrackerDecoratorPacketObfuscationStructuresBuriedTreasure() {
      return this.antiSeedCrackerDecoratorPacketObfuscationStructuresBuriedTreasure;
   }

   public boolean isAntiSeedCrackerDecoratorPacketObfuscationStructuresShipwreck() {
      return this.antiSeedCrackerDecoratorPacketObfuscationStructuresShipwreck;
   }

   public boolean isAntiSeedCrackerDecoratorPacketObfuscationStructuresMonument() {
      return this.antiSeedCrackerDecoratorPacketObfuscationStructuresMonument;
   }

   public boolean isAntiSeedCrackerDecoratorPacketObfuscationEndGatewayEnabled() {
      return this.antiSeedCrackerDecoratorPacketObfuscationEndGatewayEnabled;
   }

   public boolean isAntiSeedCrackerDecoratorPacketObfuscationEmeraldOreEnabled() {
      return this.antiSeedCrackerDecoratorPacketObfuscationEmeraldOreEnabled;
   }

   public boolean isAntiSeedCrackerDecoratorPacketObfuscationDesertWellEnabled() {
      return this.antiSeedCrackerDecoratorPacketObfuscationDesertWellEnabled;
   }

   public boolean isAntiSeedCrackerDecoratorPacketObfuscationWarpedFungusEnabled() {
      return this.antiSeedCrackerDecoratorPacketObfuscationWarpedFungusEnabled;
   }

   public boolean isAntiSeedCrackerSeedcrackerXFirewallEnabled() {
      return this.antiSeedCrackerSeedcrackerXFirewallEnabled;
   }

   public long getAntiSeedCrackerSeedcrackerXFirewallShadowStateTtlSeconds() {
      return this.antiSeedCrackerSeedcrackerXFirewallShadowStateTtlSeconds;
   }

   public int getAntiSeedCrackerSeedcrackerXFirewallShadowStateMaxPositionsPerPlayer() {
      return this.antiSeedCrackerSeedcrackerXFirewallShadowStateMaxPositionsPerPlayer;
   }

   public boolean isAntiSeedCrackerSeedcrackerXFirewallRewriteBlockUpdates() {
      return this.antiSeedCrackerSeedcrackerXFirewallRewriteBlockUpdates;
   }

   public boolean isAntiSeedCrackerSeedcrackerXFirewallRewriteBlockEntityUpdates() {
      return this.antiSeedCrackerSeedcrackerXFirewallRewriteBlockEntityUpdates;
   }

   public boolean isAntiSeedCrackerSeedcrackerXFirewallInterceptPlayerActions() {
      return this.antiSeedCrackerSeedcrackerXFirewallInterceptPlayerActions;
   }

   public boolean isAntiSeedCrackerSeedcrackerXFirewallInterceptStartStopDigging() {
      return this.antiSeedCrackerSeedcrackerXFirewallInterceptStartStopDigging;
   }

   public boolean isAntiBrightnessEnabled() {
      return this.antiBrightnessEnabled;
   }

   public int getAntiBrightnessLightThreshold() {
      return this.antiBrightnessLightThreshold;
   }

   public boolean isAntiBrightnessUseBlacklistedOresAsFallback() {
      return this.antiBrightnessUseBlacklistedOresAsFallback;
   }

   public String getAntiBrightnessMessage() {
      return this.antiBrightnessMessage;
   }

   public boolean isDeeplearnEnable() {
      return this.deeplearnEnable;
   }

   public boolean isDeeplearnDebug() {
      return this.deeplearnDebug;
   }

   public boolean isDeeplearnViolationsEnablePunishment() {
      return this.deeplearnViolationsEnablePunishment;
   }

   public double getDeeplearnViolationsVlMultiplier() {
      return this.deeplearnViolationsVlMultiplier;
   }

   public int getDeeplearnViolationsMinVlIncrease() {
      return this.deeplearnViolationsMinVlIncrease;
   }

   public int getDeeplearnViolationsMaxVlIncrease() {
      return this.deeplearnViolationsMaxVlIncrease;
   }

   public int getDeeplearnSequenceLength() {
      return this.deeplearnSequenceLength;
   }

   public double getDeeplearnPredictionThreshold() {
      return this.deeplearnPredictionThreshold;
   }

   public long getDeeplearnCooldownMs() {
      return this.deeplearnCooldownMs;
   }

   public long getDeeplearnMiningTimeWindowMs() {
      return this.deeplearnMiningTimeWindowMs;
   }

   public double getDeeplearnVeinDetectionMaxDistance() {
      return this.deeplearnVeinDetectionMaxDistance;
   }

   public long getDeeplearnVeinDetectionMaxTimeMs() {
      return this.deeplearnVeinDetectionMaxTimeMs;
   }

   public int getDeeplearnVeinDetectionMaxConsecutiveSkips() {
      return this.deeplearnVeinDetectionMaxConsecutiveSkips;
   }

   public double getDeeplearnVeinDetectionMaxVerticalDistance() {
      return this.deeplearnVeinDetectionMaxVerticalDistance;
   }

   private Material parseAntiBrightnessTargetOre(String oreName) {
      try {
         return Material.valueOf(oreName.toUpperCase());
      } catch (IllegalArgumentException ex) {
         this.plugin.getLogger().warning("Invalid AntiBrightness target ore type: " + oreName + ", ignoring.");
         return null;
      }
   }

   private static Material resolveMassExcavationMaterial(String materialName) {
      return Material.getMaterial(materialName.toUpperCase());
   }

   private boolean isValidMassExcavationTargetBlock(String materialName) {
      try {
         Material.valueOf(materialName.toUpperCase());
         return true;
      } catch (IllegalArgumentException ex) {
         this.plugin.getLogger().warning("无效的大规模挖掘目标方块类型: " + materialName + "，已忽略。");
         return false;
      }
   }

   private boolean isValidBlacklistedOre(String oreName) {
      try {
         Material.valueOf(oreName);
         return true;
      } catch (IllegalArgumentException ex) {
         this.plugin.getLogger().warning("无效的矿物类型: " + oreName + "，已忽略。");
         return false;
      }
   }

   public void loadConfig() {
      if (!this.plugin.getDataFolder().exists()) {
         this.plugin.getDataFolder().mkdirs();
      }

      if (!this.configFile.exists()) {
         this.plugin.saveDefaultConfig();
      }

      this.config = YamlConfiguration.loadConfiguration(this.configFile);
      InputStream resource = this.plugin.getResource("config.yml");
      if (resource != null) {
         YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(resource));
         if (this.mergeMissingConfigKeys(this.config, defaultConfig)) {
            try {
               this.config.save(this.configFile);
               this.plugin.getLogger().info("配置文件已更新，添加了新的配置项");
            } catch (IOException ex) {
               this.plugin.getLogger().severe("无法保存更新的配置文件: " + ex.getMessage());
            }
         }
      }

      this.plugin.reloadConfig();
      this.config = this.plugin.getConfig();
      this.loadBlacklistedOres();
      this.loadBlacklistedWorlds();
      this.loadMassExcavationTargetBlocks();
      this.loadAntiSeedCrackerConfig();
      this.loadAntiBrightnessConfig();
      this.loadDeepLearnConfig();
   }

   private boolean mergeMissingConfigKeys(FileConfiguration targetConfig, YamlConfiguration defaultConfig) {
      boolean updated = false;
      String apiKeyPath = "api-key";
      if (defaultConfig.contains(apiKeyPath) && !targetConfig.contains(apiKeyPath)) {
         Object defaultApiKey = defaultConfig.get(apiKeyPath);
         targetConfig.set(apiKeyPath, defaultApiKey);
         this.plugin.getLogger().info("添加/重置缺失的配置项: " + apiKeyPath + " 为默认值: '" + defaultApiKey + "'");
         updated = true;
      }

      for (String configPath : defaultConfig.getKeys(true)) {
         if (!configPath.equals(apiKeyPath) && !defaultConfig.isConfigurationSection(configPath) && !targetConfig.contains(configPath)) {
            Object defaultValue = defaultConfig.get(configPath);
            targetConfig.set(configPath, defaultValue);
            this.plugin.getLogger().info("添加缺失的配置项: " + configPath + "，值为: '" + defaultValue + "'");
            updated = true;
         }
      }

      return updated;
   }

   private void loadBlacklistedOres() {
      List<String> oreNames = this.config.getStringList("blacklist.ores");
      this.blacklistedOres = oreNames.stream()
         .filter(this::isValidBlacklistedOre)
         .map(oreName -> Material.getMaterial(oreName))
         .filter(Objects::nonNull)
         .collect(Collectors.toList());
   }

   private void loadBlacklistedWorlds() {
      this.blacklistWorlds = this.config.getStringList("blacklist.worlds");
   }

   public void reloadConfig() {
      if (!this.configFile.exists()) {
         this.plugin.saveDefaultConfig();
      }

      this.plugin.reloadConfig();
      this.config = this.plugin.getConfig();

      try {
         InputStream resource = this.plugin.getResource("config.yml");
         if (resource != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(resource));
            if (this.mergeMissingConfigKeys(this.config, defaultConfig)) {
               this.config.save(this.configFile);
               this.plugin.getLogger().info("配置文件已更新，添加了新的配置项");
            }
         }
      } catch (Exception ex) {
         this.plugin.getLogger().warning("reload 时更新配置文件失败: " + ex.getMessage());
      }

      this.loadBlacklistedOres();
      this.loadBlacklistedWorlds();
      this.loadMassExcavationTargetBlocks();
      this.loadAntiSeedCrackerConfig();
      this.loadAntiBrightnessConfig();
      this.loadDeepLearnConfig();
   }

   public String getNotificationSystemBanMessagesBanReason() {
      return this.config.getString("notification-system.ban-messages.ban-reason", "使用作弊客户端 (VL: %vl%)");
   }

   public boolean isNotificationSystemBanMessagesShowLocationInConsole() {
      return this.config.getBoolean("notification-system.ban-messages.show-location-in-console", false);
   }

   public boolean isNotificationSystemXrayBanBroadcast() {
      return this.config.getBoolean("notification-system.xray.ban.broadcast", true);
   }

   public double getNotificationSystemXrayBanThreshold() {
      return this.config.getDouble("notification-system.xray.ban.threshold", 10.0);
   }

   public int getCheckXrayLineBreakLength() {
      return this.config.getInt("check.xray.line-break.length", 5);
   }

   public int getCheckXraySpawnOre() {
      return this.config.getInt("check.xray.spawn.ore", 10);
   }

   public long getViolationDecreaseInterval() {
      return this.config.getLong("violation-decrease-interval", 300L);
   }

   public double getViolationDecreaseAmount() {
      return this.config.getDouble("violation-decrease-amount", 1.0);
   }

   public int getCheckXraySpawnTime() {
      return this.config.getInt("check.xray.spawn.time", 300);
   }

   public int getCheckXraySpawnDistanceLimitOre() {
      return this.config.getInt("check.xray.spawn.distance-limit.ore", 5);
   }

   public int getCheckXraySpawnDistanceLimitPlayer() {
      return this.config.getInt("check.xray.spawn.distance-limit.player", 8);
   }

   public int getCheckXraySpawnXChangeMin() {
      return this.config.getInt("check.xray.spawn.x-change.min", -9);
   }

   public int getCheckXraySpawnXChangeMax() {
      return this.config.getInt("check.xray.spawn.x-change.max", 10);
   }

   public int getCheckXraySpawnZChangeMin() {
      return this.config.getInt("check.xray.spawn.z-change.min", -9);
   }

   public int getCheckXraySpawnZChangeMax() {
      return this.config.getInt("check.xray.spawn.z-change.max", 10);
   }

   public int getCheckXraySpawnYChangeMin() {
      return this.config.getInt("check.xray.spawn.y-change.min", -10);
   }

   public int getCheckXraySpawnYChangeMax() {
      return this.config.getInt("check.xray.spawn.y-change.max", 10);
   }

   public int getCheckXrayMiningCountThreshold() {
      return this.config.getInt("check.xray.mining-count-threshold", 5);
   }

   public int getFakeOreGenerationCooldown() {
      return this.config.getInt("fake-ore-generation-cooldown", 120);
   }

   public boolean isFakeOreDecoyThrowEnabled() {
      return this.config.getBoolean("fake-ore-decoy-throw.enabled", false);
   }

   public int getFakeOreDecoyThrowMaxDistance() {
      return Math.max(1, this.config.getInt("fake-ore-decoy-throw.max-distance", 24));
   }

   public int getFakeOreDecoyThrowMinDistance() {
      return Math.max(1, this.config.getInt("fake-ore-decoy-throw.min-distance", 6));
   }

   public int getFakeOreDecoyThrowBehindWallLayers() {
      return Math.max(1, this.config.getInt("fake-ore-decoy-throw.behind-wall-layers", 2));
   }

   public boolean isCheckXraySpawnRandomOreEnabled() {
      return this.config.getBoolean("check.xray.spawn.random-ore.enabled", true);
   }

   private String resolveRandomOrePoolKey(Environment environment, int blockY) {
      switch (environment) {
         case NETHER:
            return "nether";
         case THE_END:
            return "end";
         default:
            return blockY < 0 ? "overworld-deep" : "overworld-surface";
      }
   }

   private List getDefaultRandomOreNames(Environment environment, int blockY) {
      switch (environment) {
         case NETHER:
            return Arrays.asList("NETHER_QUARTZ_ORE", "NETHER_GOLD_ORE", "ANCIENT_DEBRIS");
         case THE_END:
            return Collections.emptyList();
         default:
            return blockY < 0
               ? Arrays.asList(
                  "DEEPSLATE_COAL_ORE",
                  "DEEPSLATE_IRON_ORE",
                  "DEEPSLATE_COPPER_ORE",
                  "DEEPSLATE_GOLD_ORE",
                  "DEEPSLATE_LAPIS_ORE",
                  "DEEPSLATE_REDSTONE_ORE",
                  "DEEPSLATE_DIAMOND_ORE",
                  "DEEPSLATE_EMERALD_ORE"
               )
               : Arrays.asList("COAL_ORE", "IRON_ORE", "COPPER_ORE", "GOLD_ORE", "LAPIS_ORE", "REDSTONE_ORE", "DIAMOND_ORE", "EMERALD_ORE");
      }
   }

   public String getHeavyModeSetting() {
      Object rawSetting = this.config.get("check.xray.heavy-mode");
      if (!(rawSetting instanceof String)) {
         return this.config.getBoolean("check.xray.heavy-mode", false) ? "true" : "false";
      }

      String normalizedSetting = ((String)rawSetting).trim().toLowerCase(Locale.ROOT);
      return !"true".equals(normalizedSetting) && !"dynamic".equals(normalizedSetting) ? "false" : normalizedSetting;
   }

   public boolean isCheckXrayHeavyModeEnabled() {
      return this.isHeavyModeEnabled();
   }

   public boolean isCheckXrayHeavyModeDynamic() {
      return this.isHeavyModeDynamic();
   }

   public double getCheckXrayHeavyModeTriggerVl() {
      return Math.max(0.0, this.config.getDouble("check.xray.heavy-mode-trigger-vl", 5.0));
   }

   public boolean isCheckXrayHeavyModeObfuscateFakeOres() {
      return this.config.getBoolean("check.xray.heavy-mode-obfuscate-fake-ores", true);
   }

   public boolean isCheckXrayProbeGuardEnabled() {
      return this.config.getBoolean("check.xray.probe-guard.enabled", true);
   }

   public boolean isCheckXrayProbeGuardRequireHeavyMode() {
      return this.config.getBoolean("check.xray.probe-guard.require-heavy-mode", false);
   }

   public boolean isCheckXrayProbeGuardOnlyObfuscatedPlayers() {
      return this.config.getBoolean("check.xray.probe-guard.only-obfuscated-players", true);
   }

   public boolean isCheckXrayProbeGuardUseGamemodeReach() {
      return this.config.getBoolean("check.xray.probe-guard.use-gamemode-reach", true);
   }

   public double getCheckXrayProbeGuardMaxReach() {
      return Math.max(1.0, this.config.getDouble("check.xray.probe-guard.max-reach", 6.0));
   }

   public double getCheckXrayProbeGuardReachTolerance() {
      return Math.max(0.0, this.config.getDouble("check.xray.probe-guard.reach-tolerance", 1.0));
   }

   public double getCheckXrayProbeGuardCrosshairTolerance() {
      return Math.max(0.0, this.config.getDouble("check.xray.probe-guard.crosshair-tolerance", 0.4));
   }

   public boolean isCheckXrayProbeGuardCheckCrosshair() {
      return this.config.getBoolean("check.xray.probe-guard.check-crosshair", true);
   }

   public boolean isCheckXrayProbeGuardCheckLineOfSight() {
      return this.config.getBoolean("check.xray.probe-guard.check-line-of-sight", true);
   }

   public long getCheckXrayProbeGuardAbortGraceMs() {
      long graceMs = this.config.getLong("check.xray.probe-guard.abort-grace-ms", 30000L);
      return graceMs <= 0L ? 0L : Math.max(5000L, graceMs);
   }

   public double getCheckXrayProbeGuardVlPerCancel() {
      return Math.max(0.0, this.config.getDouble("check.xray.probe-guard.vl-per-cancel", 1.0));
   }

   public boolean isCheckXrayProbeGuardDebugLog() {
      return this.config.getBoolean("check.xray.probe-guard.debug-log", false);
   }

   public int getCheckXrayProbeGuardMinInvalidPerWindow() {
      return Math.max(1, this.config.getInt("check.xray.probe-guard.min-invalid-per-window", 3));
   }

   public long getCheckXrayProbeGuardInvalidWindowMs() {
      return Math.max(100L, this.config.getLong("check.xray.probe-guard.invalid-window-ms", 1000L));
   }

   public boolean isNotificationSystemOfflineReportEnabled() {
      return this.config.getBoolean("notification-system.offline-report.enabled", true);
   }

   public int getNotificationSystemOfflineReportMaxEntries() {
      return Math.max(100, this.config.getInt("notification-system.offline-report.max-entries", 500));
   }

   public boolean isCheckXrayEnable() {
      return this.config.getBoolean("check.xray.enable", true);
   }

   public boolean isCheckMiningBehaviorAnalyzerActive() {
      return this.config.getBoolean("check.mining-behavior-analyzer.active", false);
   }

   public boolean isCheckMiningBehaviorAnalyzerWorldConfigsDefaultEnabled(String worldName) {
      String worldConfigPath = "check.mining-behavior-analyzer.world-configs." + worldName + ".enabled";
      return this.config.contains(worldConfigPath)
         ? this.config.getBoolean(worldConfigPath, false)
         : this.config.getBoolean("check.mining-behavior-analyzer.world-configs._default.enabled", false);
   }

   public int getCheckMiningBehaviorAnalyzerWorldConfigsDefaultYLimit(String worldName) {
      String worldConfigPath = "check.mining-behavior-analyzer.world-configs." + worldName + ".y-limit";
      return this.config.contains(worldConfigPath)
         ? this.config.getInt(worldConfigPath, -1)
         : this.config.getInt("check.mining-behavior-analyzer.world-configs._default.y-limit", -1);
   }

   public List getCheckMiningBehaviorAnalyzerTargetBlocks() {
      return this.config.getStringList("check.mining-behavior-analyzer.target-blocks");
   }

   public int getCheckMiningBehaviorAnalyzerDataCollectionPathMaxBlocks() {
      return this.config.getInt("check.mining-behavior-analyzer.data-collection.path-max-blocks", 500);
   }

   public int getCheckMiningBehaviorAnalyzerDataCollectionCleanupAfterMinutes() {
      return this.config.getInt("check.mining-behavior-analyzer.data-collection.cleanup-after-minutes", 15);
   }

   public int getCheckMiningBehaviorAnalyzerDataCollectionVeinClusterDistance() {
      return this.config.getInt("check.mining-behavior-analyzer.data-collection.vein-cluster.distance", 5);
   }

   public int getCheckMiningBehaviorAnalyzerDataCollectionVeinClusterSmallSize() {
      return this.config.getInt("check.mining-behavior-analyzer.data-collection.vein-cluster.small-size", 4);
   }

   public int getCheckMiningBehaviorAnalyzerDataCollectionVeinClusterTriggerCount() {
      return this.config.getInt("check.mining-behavior-analyzer.data-collection.vein-cluster.trigger-count", 3);
   }

   public int getCheckMiningBehaviorAnalyzerPatternAnalyzerTurningLimit() {
      return this.config.getInt("check.mining-behavior-analyzer.pattern-analyzer.turning-limit", 10);
   }

   public int getCheckMiningBehaviorAnalyzerPatternAnalyzerBranchingLimit() {
      return this.config.getInt("check.mining-behavior-analyzer.pattern-analyzer.branching-limit", 6);
   }

   public int getCheckMiningBehaviorAnalyzerPatternAnalyzerYShiftLimit() {
      return this.config.getInt("check.mining-behavior-analyzer.pattern-analyzer.y-shift-limit", 4);
   }

   public int getCheckMiningBehaviorAnalyzerPatternAnalyzerYShiftMin() {
      return this.config.getInt("check.mining-behavior-analyzer.pattern-analyzer.y-shift-min", 3);
   }

   public boolean isCheckMiningBehaviorAnalyzerEnvironmentFiltersApplyFilters() {
      return this.config.getBoolean("check.mining-behavior-analyzer.environment-filters.apply-filters", true);
   }

   public int getCheckMiningBehaviorAnalyzerEnvironmentFiltersCaveAirCount() {
      return this.config.getInt("check.mining-behavior-analyzer.environment-filters.cave.air-count", 14);
   }

   public int getCheckMiningBehaviorAnalyzerEnvironmentFiltersCaveCaveAirMultiplier() {
      return this.config.getInt("check.mining-behavior-analyzer.environment-filters.cave.cave-air-multiplier", 5);
   }

   public int getCheckMiningBehaviorAnalyzerEnvironmentFiltersCaveRadius() {
      return this.config.getInt("check.mining-behavior-analyzer.environment-filters.cave.radius", 3);
   }

   public boolean isCheckMiningBehaviorAnalyzerEnvironmentFiltersCaveReduceVl() {
      return this.config.getBoolean("check.mining-behavior-analyzer.environment-filters.cave.reduce-vl", true);
   }

   public boolean isCheckMiningBehaviorAnalyzerEnvironmentFiltersCaveFakeCaveDetectionEnabled() {
      return this.config.getBoolean("check.mining-behavior-analyzer.environment-filters.cave.fake-cave-detection.enabled", true);
   }

   public int getCheckMiningBehaviorAnalyzerEnvironmentFiltersCaveFakeCaveDetectionMinBlocks() {
      return this.config.getInt("check.mining-behavior-analyzer.environment-filters.cave.fake-cave-detection.min-blocks", 10);
   }

   public double getCheckMiningBehaviorAnalyzerEnvironmentFiltersCaveFakeCaveDetectionAirRatio() {
      return this.config.getDouble("check.mining-behavior-analyzer.environment-filters.cave.fake-cave-detection.air-ratio", 0.3);
   }

   public int getCheckMiningBehaviorAnalyzerEnvironmentFiltersCaveFakeCaveDetectionVlPerTrigger() {
      return this.config.getInt("check.mining-behavior-analyzer.environment-filters.cave.fake-cave-detection.vl-per-trigger", 1);
   }

   public int getCheckMiningBehaviorAnalyzerEnvironmentFiltersCaveFakeCaveDetectionMaxTriggers() {
      return this.config.getInt("check.mining-behavior-analyzer.environment-filters.cave.fake-cave-detection.max-triggers", 5);
   }

   public long getCheckMiningBehaviorAnalyzerEnvironmentFiltersCaveFakeCaveDetectionResetMinutes() {
      return this.config.getLong("check.mining-behavior-analyzer.environment-filters.cave.fake-cave-detection.reset-minutes", 20L);
   }

   public boolean isCheckMiningBehaviorAnalyzerEnvironmentFiltersWaterIncludeFlowing() {
      return this.config.getBoolean("check.mining-behavior-analyzer.environment-filters.water.include-flowing", false);
   }

   public int getCheckMiningBehaviorAnalyzerEnvironmentFiltersWaterWaterCount() {
      return this.config.getInt("check.mining-behavior-analyzer.environment-filters.water.water-count", 14);
   }

   public int getCheckMiningBehaviorAnalyzerEnvironmentFiltersWaterRadius() {
      return this.config.getInt("check.mining-behavior-analyzer.environment-filters.water.radius", 3);
   }

   public boolean isCheckMiningBehaviorAnalyzerEnvironmentFiltersWaterReduceVl() {
      return this.config.getBoolean("check.mining-behavior-analyzer.environment-filters.water.reduce-vl", true);
   }

   public int getCheckMiningBehaviorAnalyzerEnvironmentFiltersLavaLavaCount() {
      return this.config.getInt("check.mining-behavior-analyzer.environment-filters.lava.lava-count", 14);
   }

   public int getCheckMiningBehaviorAnalyzerEnvironmentFiltersLavaRadius() {
      return this.config.getInt("check.mining-behavior-analyzer.environment-filters.lava.radius", 3);
   }

   public boolean isCheckMiningBehaviorAnalyzerEnvironmentFiltersLavaReduceVl() {
      return this.config.getBoolean("check.mining-behavior-analyzer.environment-filters.lava.reduce-vl", true);
   }

   public int getCheckMiningBehaviorAnalyzerDataCollectionExplosionTrackingRetentionSeconds() {
      return this.config.getInt("check.mining-behavior-analyzer.data-collection.explosion-tracking.retention-seconds", 600);
   }

   public boolean isCheckMiningBehaviorAnalyzerDataCollectionExplosionTrackingEnabled() {
      return this.config.getBoolean("check.mining-behavior-analyzer.data-collection.explosion-tracking.enabled", false);
   }

   public double getCheckMiningBehaviorAnalyzerDataCollectionExplosionTrackingPenaltyMultiplier() {
      return this.config.getDouble("check.mining-behavior-analyzer.data-collection.explosion-tracking.penalty-multiplier", 2.0);
   }

   public double getCheckMiningBehaviorAnalyzerDataCollectionExplosionTrackingSuspiciousRate() {
      return this.config.getDouble("check.mining-behavior-analyzer.data-collection.explosion-tracking.suspicious-rate", 0.1);
   }

   public int getCheckMiningBehaviorAnalyzerViolationSystemAutoDecayIntervalMinutes() {
      return this.config.getInt("check.mining-behavior-analyzer.violation-system.auto-decay.interval-minutes", 3);
   }

   public int getCheckMiningBehaviorAnalyzerViolationSystemAutoDecayAmount() {
      return this.config.getInt("check.mining-behavior-analyzer.violation-system.auto-decay.amount", 1);
   }

   public boolean isCheckMiningBehaviorAnalyzerViolationSystemExponentialDecayEnabled() {
      return this.config.getBoolean("check.mining-behavior-analyzer.violation-system.exponential-decay.enabled", false);
   }

   public double getCheckMiningBehaviorAnalyzerViolationSystemExponentialDecayFactor() {
      return this.config.getDouble("check.mining-behavior-analyzer.violation-system.exponential-decay.factor", 0.9);
   }

   public ConfigurationSection getAlgoThresholdCommandsSection() {
      ConfigurationSection configurationSection = this.config.getConfigurationSection("notification-system.algo.threshold-commands");
      return configurationSection != null ? configurationSection : this.config.getConfigurationSection("check.mining-behavior-analyzer.violation-system.threshold-commands");
   }

   public boolean isCheckXraySameChunkDetection() {
      return this.config.getBoolean("check.xray.same-chunk-detection", true);
   }

   public String getCheckAntifreecamWarningMessage() {
      String warningMessage = this.config.getString("check.antifreecam.warning-message", "§c警告：检测到可疑的Freecam行为。请停止使用，否则可能会被处罚。");
      return ChatColor.translateAlternateColorCodes('&', warningMessage);
   }

   public boolean isCheckAntifreecamKickEnabled() {
      return this.config.getBoolean("check.antifreecam.kick-enabled", false);
   }

   public int getCheckAntifreecamMaxWarnings() {
      return this.config.getInt("check.antifreecam.max-warnings", 3);
   }

   public boolean isCheckAntifreecamKickAfterMaxWarnings() {
      return this.config.getBoolean("check.antifreecam.kick-after-max-warnings", true);
   }

   public String getCheckAntifreecamKickMessage() {
      String kickMessage = this.config.getString("check.antifreecam.kick-message", "§c您因为可能使用Freecam而被踢出服务器。");
      return ChatColor.translateAlternateColorCodes('&', kickMessage);
   }

   public double getCheckAntifreecamYawThreshold() {
      return this.config.getDouble("check.antifreecam.yaw-threshold", 45.0);
   }

   public double getCheckAntifreecamPitchThreshold() {
      return this.config.getDouble("check.antifreecam.pitch-threshold", 30.0);
   }

   public double getCheckXrayVlAddBreakSelf() {
      return this.config.getDouble("check.xray.vl.add.break-self", 1.0);
   }

   public double getCheckXrayVlAddBreakOther() {
      return this.config.getDouble("check.xray.vl.add.break-other", 2.0);
   }

   public boolean isNotificationSystemXrayBanEnabled() {
      return this.config.getBoolean("notification-system.xray.ban.enabled", true);
   }

   public String getBanCommand(String detectionType) {
      String sectionKey = detectionType != null && !detectionType.isEmpty() ? detectionType : "xray";
      String command = this.config.getString("notification-system." + sectionKey + ".ban.command", "");
      return command == null ? "" : command.trim();
   }

   public boolean isBanFallbackToBuiltinEnabled(String detectionType) {
      String sectionKey = detectionType != null && !detectionType.isEmpty() ? detectionType : "xray";
      return this.config.getBoolean("notification-system." + sectionKey + ".ban.fallback-to-builtin-on-failure", false);
   }

   public boolean isCheckXrayVlContinuousIncreaseEnable() {
      return this.config.getBoolean("check.xray.vl.continuous-increase.enable", true);
   }

   public int getCheckXrayVlContinuousIncreaseCount() {
      return this.config.getInt("check.xray.vl.continuous-increase.count", 3);
   }

   public int getCheckXrayVlContinuousIncreaseInterval() {
      return this.config.getInt("check.xray.vl.continuous-increase.interval", 60);
   }

   public int getViolationCooldown() {
      return this.config.getInt("violation-cooldown", 5);
   }

   public boolean isDebug() {
      return this.config.getBoolean("debug", false);
   }

   public String getNotificationSystemXrayBanMessage() {
      String banMessage = this.config.getString("notification-system.xray.ban.message", "§c玩家 %player% 因使用作弊被封禁！");
      return ChatColor.translateAlternateColorCodes('&', banMessage);
   }

   public String getNotificationSystemXrayBanDetail() {
      String banDetail = this.config.getString("notification-system.xray.ban.detail", "§7原因: §f%reason%");
      return ChatColor.translateAlternateColorCodes('&', banDetail);
   }

   public String getNotificationSystemBanMessagesCustomBanMessage() {
      return ChatColor.translateAlternateColorCodes('&', this.config.getString("notification-system.ban-messages.custom-ban-message", "§c你已被此服务器封禁！\n\n§7原因: §f%s\n§7如果你认为这是一个错误，请联系服务器管理员。\n§d[NekoAntiXray] §6高效且轻量反Xray插件 By SakuyaAyane"));
   }

   public int getNotificationSystemBanMessagesBanDuration() {
      return this.config.getInt("notification-system.ban-messages.ban-duration", 30);
   }

   public boolean isReplayEnabled() {
      return this.config.getBoolean("replay.enabled", false);
   }

   public int getReplayRecordDuration() {
      return this.config.getInt("replay.record-duration", 300);
   }

   public String getApiKey() {
      return this.config.getString("api-key", "");
   }

   public boolean isBanInventoryCleanEnabled() {
      return this.config.getBoolean("ban-inventory-clean.enabled", false);
   }

   public int getCheckXrayMassExcavationDetectionTimeWindow() {
      return this.config.getInt("check.xray.mass-excavation.detection-time-window", 60);
   }

   public int getCheckXrayMassExcavationBlockCountThreshold() {
      return this.config.getInt("check.xray.mass-excavation.block-count-threshold", 40);
   }

   public int getCheckXrayMassExcavationDiversityThreshold() {
      return this.config.getInt("check.xray.mass-excavation.diversity-threshold", 5);
   }

   public int getCheckXrayMassExcavationBypassDuration() {
      return this.config.getInt("check.xray.mass-excavation.bypass-duration", 180);
   }

   public double getCheckXrayMassExcavationCuboidRatio() {
      return this.config.getDouble("check.xray.mass-excavation.cuboid-ratio", 0.5);
   }

   public boolean isCheckXrayMassExcavationNotificationEnabled() {
      return this.config.getBoolean("check.xray.mass-excavation.notification.enabled", true);
   }

   public String getCheckXrayMassExcavationNotificationMessage() {
      String notificationMessage = this.config.getString("check.xray.mass-excavation.notification.message", "§a经系统计算已确认您正在进行大规模挖掘，已降低反作弊对您行为的检测力度，但您仍在NAX监控之下。");
      return ChatColor.translateAlternateColorCodes('&', notificationMessage);
   }

   public boolean isCheckXrayMassExcavationEnabled() {
      return this.config.getBoolean("check.xray.mass-excavation.enabled", true);
   }

   public int getCheckXrayMassExcavationConfirmationCooldown() {
      return this.config.getInt("check.xray.mass-excavation.confirmation-cooldown", 60);
   }

   public String getCheckXrayMassExcavationMessagePrefix() {
      String messagePrefix = this.config.getString("check.xray.mass-excavation.message-prefix", "&6[NekoAntiXray] &f");
      return ChatColor.translateAlternateColorCodes('&', messagePrefix);
   }

   public String getCheckXrayMassExcavationConfirmationMessage() {
      String confirmationMessage = this.config.getString("check.xray.mass-excavation.confirmation-message", "检测到你可能在进行大规模挖掘，是这样吗？");
      return ChatColor.translateAlternateColorCodes('&', confirmationMessage);
   }

   public String getCheckXrayMassExcavationConfirmButton() {
      String confirmButton = this.config.getString("check.xray.mass-excavation.confirm-button", "&a[是]");
      return ChatColor.translateAlternateColorCodes('&', confirmButton);
   }

   public String getCheckXrayMassExcavationDenyButton() {
      String denyButton = this.config.getString("check.xray.mass-excavation.deny-button", "&c[否]");
      return ChatColor.translateAlternateColorCodes('&', denyButton);
   }

   public boolean isCheckXrayMassExcavationDetectBuildingPatterns() {
      return this.config.getBoolean("check.xray.mass-excavation.detect-building-patterns", true);
   }

   public double getCheckXrayMassExcavationSpeedVarianceThreshold() {
      return this.config.getDouble("check.xray.mass-excavation.speed-variance-threshold", 0.5);
   }

   public int getCheckXrayMassExcavationVerticalExcavationMinYRange() {
      return this.config.getInt("check.xray.mass-excavation.vertical-excavation.min-y-range", 5);
   }

   public double getCheckXrayMassExcavationVerticalExcavationYXzRatio() {
      return this.config.getDouble("check.xray.mass-excavation.vertical-excavation.y-xz-ratio", 0.8);
   }

   public double getCheckXrayMassExcavationVerticalExcavationXzDensity() {
      return this.config.getDouble("check.xray.mass-excavation.vertical-excavation.xz-density", 0.3);
   }

   public int getCheckXrayMassExcavationTerrainModificationMinYLayers() {
      return this.config.getInt("check.xray.mass-excavation.terrain-modification.min-y-layers", 3);
   }

   public double getCheckXrayMassExcavationTerrainModificationLayerSimilarity() {
      return this.config.getDouble("check.xray.mass-excavation.terrain-modification.layer-similarity", 0.6);
   }

   public double getCheckXrayMassExcavationTerrainModificationXzStability() {
      return this.config.getDouble("check.xray.mass-excavation.terrain-modification.xz-stability", 0.4);
   }

   public int getCheckXrayMassExcavationMinVolumeThreshold() {
      return this.config.getInt("check.xray.mass-excavation.min-volume-threshold", 1000);
   }

   public int getCheckXrayMassExcavationDenialCooldownDuration() {
      return this.config.getInt("check.xray.mass-excavation.denial-cooldown-duration", 300);
   }

   public double getCheckXrayMassExcavationMinDensityThreshold() {
      return this.config.getDouble("check.xray.mass-excavation.min-density-threshold", 0.4);
   }

   public int getCheckXrayMassExcavationMinHorizontalAreaThreshold() {
      return this.config.getInt("check.xray.mass-excavation.min-horizontal-area-threshold", 100);
   }

   private void loadMassExcavationTargetBlocks() {
      List<String> configuredBlocks = this.config.getStringList("check.xray.mass-excavation.target-blocks");
      if (configuredBlocks != null && !configuredBlocks.isEmpty()) {
         this.massExcavationTargetBlocks = configuredBlocks.stream()
            .filter(this::isValidMassExcavationTargetBlock)
            .map(ConfigManager::resolveMassExcavationMaterial)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
         this.plugin.getLogger().info("加载大规模挖掘目标方块: " + this.massExcavationTargetBlocks.stream().map(Material::name).collect(Collectors.joining(", ")));
      } else {
         this.massExcavationTargetBlocks = new ArrayList<>();
         this.plugin.getLogger().info("大规模挖掘目标方块列表未配置或为空，将检测所有方块类型。");
      }
   }

   public List getMassExcavationTargetBlocks() {
      return this.massExcavationTargetBlocks == null ? new ArrayList() : this.massExcavationTargetBlocks;
   }

   private void loadAntiSeedCrackerConfig() {
      this.antiSeedCrackerEnable = this.config.getBoolean("antiSeedCracker.enable", false);
      this.antiSeedCrackerModifyHashedSeedEnable = this.config.getBoolean("antiSeedCracker.modifyHashedSeed.enable", false);
      this.antiSeedCrackerModifyEndStructuresEnable = this.config.getBoolean("antiSeedCracker.modifyEndStructures.enable", false);
      this.antiSeedCrackerModifyEndStructuresWorlds = this.config.getStringList("antiSeedCracker.modifyEndStructures.worlds");
      this.antiSeedCrackerModifyEndStructuresEndSpikesEnable = this.config.getBoolean("antiSeedCracker.modifyEndStructures.endSpikes.enable", false);
      this.antiSeedCrackerModifyEndStructuresEndSpikesMode = this.config.getString("antiSeedCracker.modifyEndStructures.endSpikes.mode", "swap");
      this.antiSeedCrackerModifyEndStructuresEndCitiesEnable = this.config.getBoolean("antiSeedCracker.modifyEndStructures.endCities.enable", false);
      this.antiSeedCrackerNetherBedrockProtectionEnabled = this.config.getBoolean("antiSeedCracker.netherBedrockProtection.enabled", true);
      this.antiSeedCrackerNetherBedrockProtectionBlockRoofAccess = this.config.getBoolean("antiSeedCracker.netherBedrockProtection.blockRoofAccess", false);
      this.antiSeedCrackerNetherBedrockProtectionRoofAccessMaxY = this.config.getInt("antiSeedCracker.netherBedrockProtection.roofAccessMaxY", 127);
      this.secureSeedBiomeSourceXorEnabled = this.config.getBoolean("secure-seed.biome-source-xor-enabled", true);
      this.secureSeedBiomeCoordinateXor = this.config.getLong("secure-seed.biome-coordinate-xor", 0L);
      this.antiSeedCrackerBiomePacketObfuscationEnabled = this.config.getBoolean("antiSeedCracker.biomePacketObfuscation.enabled", true);
      this.antiSeedCrackerStructureSeedProtectionEnabled = this.config.getBoolean("antiSeedCracker.structureSeedProtection.enabled", true);
      this.antiSeedCrackerStructureSeedProtectionStrategy = this.config.getString("antiSeedCracker.structureSeedProtection.strategy", "minimal");
      this.antiSeedCrackerStructureSeedProtectionStructuresDesertPyramid = this.config
         .getBoolean("antiSeedCracker.structureSeedProtection.structures.desertPyramid", false);
      this.antiSeedCrackerStructureSeedProtectionStructuresJungleTemple = this.config
         .getBoolean("antiSeedCracker.structureSeedProtection.structures.jungleTemple", false);
      this.antiSeedCrackerStructureSeedProtectionStructuresSwampHut = this.config
         .getBoolean("antiSeedCracker.structureSeedProtection.structures.swampHut", false);
      this.antiSeedCrackerStructureSeedProtectionStructuresIgloo = this.config.getBoolean("antiSeedCracker.structureSeedProtection.structures.igloo", false);
      this.antiSeedCrackerStructureSeedProtectionStructuresPillagerOutpost = this.config
         .getBoolean("antiSeedCracker.structureSeedProtection.structures.pillagerOutpost", false);
      this.antiSeedCrackerStructureSeedProtectionStructuresShipwreck = this.config
         .getBoolean("antiSeedCracker.structureSeedProtection.structures.shipwreck", true);
      this.antiSeedCrackerDecoratorPacketObfuscationEnabled = this.config.getBoolean("antiSeedCracker.decoratorPacketObfuscation.enabled", true);
      this.antiSeedCrackerDecoratorPacketObfuscationMode = this.config.getString("antiSeedCracker.decoratorPacketObfuscation.mode", "balanced");
      this.antiSeedCrackerDecoratorPacketObfuscationDungeonsEnabled = this.config
         .getBoolean("antiSeedCracker.decoratorPacketObfuscation.dungeons.enabled", true);
      this.antiSeedCrackerDecoratorPacketObfuscationDungeonsStripSpawnerBlockEntity = this.config
         .getBoolean("antiSeedCracker.decoratorPacketObfuscation.dungeons.stripSpawnerBlockEntity", true);
      this.antiSeedCrackerDecoratorPacketObfuscationDungeonsReplaceSpawnerBlockInStrictMode = this.config
         .getBoolean("antiSeedCracker.decoratorPacketObfuscation.dungeons.replaceSpawnerBlockInStrictMode", true);
      this.antiSeedCrackerDecoratorPacketObfuscationDungeonsPerturbFloorInStrictMode = this.config
         .getBoolean("antiSeedCracker.decoratorPacketObfuscation.dungeons.perturbFloorInStrictMode", true);
      this.antiSeedCrackerDecoratorPacketObfuscationDungeonsBreakFloorCount = this.config
         .getBoolean("antiSeedCracker.decoratorPacketObfuscation.dungeons.breakFloorCount", true);
      this.antiSeedCrackerDecoratorPacketObfuscationDungeonsFloorTargetCount = Math.max(
         0, Math.min(19, this.config.getInt("antiSeedCracker.decoratorPacketObfuscation.dungeons.floorTargetCount", 19))
      );
      this.antiSeedCrackerDecoratorPacketObfuscationStructuresBuriedTreasure = this.config
         .getBoolean("antiSeedCracker.decoratorPacketObfuscation.structures.buriedTreasure", true);
      this.antiSeedCrackerDecoratorPacketObfuscationStructuresShipwreck = this.config
         .getBoolean("antiSeedCracker.decoratorPacketObfuscation.structures.shipwreck", false);
      this.antiSeedCrackerDecoratorPacketObfuscationStructuresMonument = this.config
         .getBoolean("antiSeedCracker.decoratorPacketObfuscation.structures.monument", false);
      this.antiSeedCrackerDecoratorPacketObfuscationEndGatewayEnabled = this.config
         .getBoolean("antiSeedCracker.decoratorPacketObfuscation.endGateway.enabled", false);
      this.antiSeedCrackerDecoratorPacketObfuscationEmeraldOreEnabled = this.config
         .getBoolean("antiSeedCracker.decoratorPacketObfuscation.emeraldOre.enabled", false);
      this.antiSeedCrackerDecoratorPacketObfuscationDesertWellEnabled = this.config
         .getBoolean("antiSeedCracker.decoratorPacketObfuscation.desertWell.enabled", true);
      this.antiSeedCrackerDecoratorPacketObfuscationWarpedFungusEnabled = this.config
         .getBoolean("antiSeedCracker.decoratorPacketObfuscation.warpedFungus.enabled", false);
      this.antiSeedCrackerSeedcrackerXFirewallEnabled = this.config.getBoolean("antiSeedCracker.seedcrackerXFirewall.enabled", true);
      this.antiSeedCrackerSeedcrackerXFirewallShadowStateTtlSeconds = Math.max(
         60L, this.config.getLong("antiSeedCracker.seedcrackerXFirewall.shadowState.ttlSeconds", 900L)
      );
      this.antiSeedCrackerSeedcrackerXFirewallShadowStateMaxPositionsPerPlayer = Math.max(
         1024, this.config.getInt("antiSeedCracker.seedcrackerXFirewall.shadowState.maxPositionsPerPlayer", 200000)
      );
      this.antiSeedCrackerSeedcrackerXFirewallRewriteBlockUpdates = this.config.getBoolean("antiSeedCracker.seedcrackerXFirewall.rewriteBlockUpdates", true);
      this.antiSeedCrackerSeedcrackerXFirewallRewriteBlockEntityUpdates = this.config
         .getBoolean("antiSeedCracker.seedcrackerXFirewall.rewriteBlockEntityUpdates", true);
      this.antiSeedCrackerSeedcrackerXFirewallInterceptPlayerActions = this.config
         .getBoolean("antiSeedCracker.seedcrackerXFirewall.interceptPlayerActions", true);
      this.antiSeedCrackerSeedcrackerXFirewallInterceptStartStopDigging = this.config
         .getBoolean("antiSeedCracker.seedcrackerXFirewall.interceptStartStopDigging", true);
   }

   public String getAntiSeedCrackerStructureSeedProtectionStrategy() {
      if (this.antiSeedCrackerStructureSeedProtectionStrategy == null) {
         return "minimal";
      } else {
         String strategy = this.antiSeedCrackerStructureSeedProtectionStrategy.trim().toLowerCase(Locale.ROOT);
         if (strategy.isEmpty()) {
            return "minimal";
         } else {
            return !"aggressive".equals(strategy) && !"legacy-signature".equals(strategy) && !"aggressive-signature".equals(strategy) && !"broad-signature".equals(strategy)
               ? "minimal"
               : "aggressive";
         }
      }
   }

   public String getAntiSeedCrackerDecoratorPacketObfuscationMode() {
      if (this.antiSeedCrackerDecoratorPacketObfuscationMode == null) {
         return "balanced";
      }

      String mode = this.antiSeedCrackerDecoratorPacketObfuscationMode.trim().toLowerCase(Locale.ROOT);
      return "strict".equals(mode) ? "strict" : "balanced";
   }

   public List getAntiBrightnessTargetOres() {
      if (this.antiBrightnessTargetOres == null) {
         return new ArrayList();
      } else if (this.antiBrightnessTargetOres.isEmpty() && this.antiBrightnessUseBlacklistedOresAsFallback) {
         return this.getBlacklistedOres() != null ? new ArrayList(this.getBlacklistedOres()) : new ArrayList();
      } else {
         return new ArrayList<>(this.antiBrightnessTargetOres);
      }
   }

   public List getAntiBrightnessEnabledWorlds() {
      return this.antiBrightnessEnabledWorlds != null ? new ArrayList<>(this.antiBrightnessEnabledWorlds) : new ArrayList();
   }

   private void loadAntiBrightnessConfig() {
      String prefix = "check.antiBrightness.";
      this.antiBrightnessEnabled = this.config.getBoolean(prefix + "enable", false);
      this.antiBrightnessLightThreshold = this.config.getInt(prefix + "lightThreshold", 2);
      List<String> configuredOres = this.config.getStringList(prefix + "targetOres");
      this.antiBrightnessTargetOres = configuredOres.stream()
         .map(this::parseAntiBrightnessTargetOre)
         .filter(Objects::nonNull)
         .collect(Collectors.toList());
      this.antiBrightnessUseBlacklistedOresAsFallback = this.config.getBoolean(prefix + "useBlacklistedOresAsFallback", true);
      this.antiBrightnessEnabledWorlds = this.config.getStringList(prefix + "enabledWorlds");
      this.antiBrightnessMessage = ChatColor.translateAlternateColorCodes(
         '&', this.config.getString(prefix + "message", "&c[NekoAntiXray] Light level is too low to break this ore!")
      );
      if (this.antiBrightnessEnabled) {
         Logger logger = this.plugin.getLogger();
         String worldsSummary = this.antiBrightnessEnabledWorlds.isEmpty() ? "All (except blacklist)" : this.antiBrightnessEnabledWorlds.toString();
         logger.info("AntiBrightness feature enabled. Threshold: " + this.antiBrightnessLightThreshold + ", Worlds: " + worldsSummary + ", Message: '" + this.antiBrightnessMessage + "'");
         if (this.antiBrightnessTargetOres.isEmpty() && this.antiBrightnessUseBlacklistedOresAsFallback) {
            this.plugin.getLogger().info("AntiBrightness targetOres is empty, using blacklist.ores as fallback.");
         } else if (this.antiBrightnessTargetOres.isEmpty()) {
            this.plugin
               .getLogger()
               .warning("AntiBrightness targetOres is empty and useBlacklistedOresAsFallback is false. The check might not protect any ores.");
         } else {
            this.plugin
               .getLogger()
               .info("AntiBrightness targetOres: " + this.antiBrightnessTargetOres.stream().map(Material::name).collect(Collectors.joining(", ")));
         }
      }
   }

   private void loadDeepLearnConfig() {
      this.deeplearnEnable = this.config.getBoolean("deeplearn.enable", false);
      this.deeplearnDebug = this.config.getBoolean("deeplearn.debug", false);
      this.deeplearnViolationsEnablePunishment = this.config.getBoolean("deeplearn.violations.enable-punishment", true);
      this.deeplearnViolationsVlMultiplier = this.config.getDouble("deeplearn.violations.vl-multiplier", 10.0);
      this.deeplearnViolationsMinVlIncrease = this.config.getInt("deeplearn.violations.min-vl-increase", 1);
      this.deeplearnViolationsMaxVlIncrease = this.config.getInt("deeplearn.violations.max-vl-increase", 15);
      this.deeplearnSequenceLength = this.config.getInt("deeplearn.sequence_length", 50);
      this.deeplearnPredictionThreshold = this.config.getDouble("deeplearn.prediction_threshold", 0.5);
      this.deeplearnCooldownMs = this.config.getLong("deeplearn.cooldown_ms", 5000L);
      this.deeplearnMiningTimeWindowMs = this.config.getLong("deeplearn.mining_time_window_ms", 600000L);
      this.deeplearnVeinDetectionMaxDistance = this.config.getDouble("deeplearn.vein_detection.max_distance", 2.0);
      this.deeplearnVeinDetectionMaxTimeMs = this.config.getLong("deeplearn.vein_detection.max_time_ms", 3000L);
      this.deeplearnVeinDetectionMaxConsecutiveSkips = this.config.getInt("deeplearn.vein_detection.max_consecutive_skips", 15);
      this.deeplearnVeinDetectionMaxVerticalDistance = this.config.getDouble("deeplearn.vein_detection.max_vertical_distance", 3.0);
      this.deeplearnValuableMaterials = this.config.getStringList("deeplearn.valuable_materials");
      if (this.deeplearnEnable) {
         Logger logger = this.plugin.getLogger();
         logger.info("深度学习功能已启用 - 序列长度: " + this.deeplearnSequenceLength + ", 预测阈值: " + this.deeplearnPredictionThreshold + ", 冷却时间: " + this.deeplearnCooldownMs + "ms, VL倍数: " + this.deeplearnViolationsVlMultiplier);
         if (this.deeplearnDebug) {
            this.plugin.getLogger().info("[DL-Config] 有价值材料: " + this.deeplearnValuableMaterials.toString());
         }
      }
   }

   public List getDeeplearnValuableMaterials() {
      return this.deeplearnValuableMaterials != null ? new ArrayList<>(this.deeplearnValuableMaterials) : new ArrayList();
   }

   public boolean isWorldSettingsDefaultAntiXrayRayTraceThirdPerson(String worldName) {
      return this.config
         .getBoolean(
            "world-settings." + worldName + ".anti-xray.ray-trace-third-person",
            this.config.getBoolean("world-settings.default.anti-xray.ray-trace-third-person", false)
         );
   }

   public double getWorldSettingsDefaultAntiXrayRayTraceDistance(String worldName) {
      return Math.max(
         this.config
            .getDouble(
               "world-settings." + worldName + ".anti-xray.ray-trace-distance", this.config.getDouble("world-settings.default.anti-xray.ray-trace-distance", 80.0)
            ),
         0.0
      );
   }

   public boolean isWorldSettingsDefaultAntiXrayRehideBlocks(String worldName) {
      return this.config
         .getBoolean("world-settings." + worldName + ".anti-xray.rehide-blocks", this.config.getBoolean("world-settings.default.anti-xray.rehide-blocks", false));
   }

   public double getWorldSettingsDefaultAntiXrayRehideDistance(String worldName) {
      return Math.max(
         this.config
            .getDouble(
               "world-settings." + worldName + ".anti-xray.rehide-distance",
               this.config.getDouble("world-settings.default.anti-xray.rehide-distance", Double.POSITIVE_INFINITY)
            ),
         0.0
      );
   }

   public int getWorldSettingsDefaultAntiXrayMaxRayTraceBlockCountPerChunk(String worldName) {
      return Math.max(
         this.config
            .getInt(
               "world-settings." + worldName + ".anti-xray.max-ray-trace-block-count-per-chunk",
               this.config.getInt("world-settings.default.anti-xray.max-ray-trace-block-count-per-chunk", 100)
            ),
         0
      );
   }

   public List getWorldSettingsDefaultAntiXrayRayTraceBlocks(String worldName) {
      return this.config
         .getList(
            "world-settings." + worldName + ".anti-xray.ray-trace-blocks", this.config.getList("world-settings.default.anti-xray.ray-trace-blocks", new ArrayList())
         )
         .stream()
         .filter(Objects::nonNull)
         .map(element -> String.valueOf(element))
         .collect(Collectors.toList());
   }

   public int getConfiguredRayTraceThreads() {
      return this.getWorldSettingsDefaultRayTraceThreads();
   }

   public boolean isOreStatsEnabled() {
      return this.config.getBoolean("ore-stats.enabled", true);
   }

   public int resolveRayTraceThreadCount() {
      return this.getRayTraceThreads();
   }

   public int getFaweCompatibilityRefreshDelayTicks() {
      return this.plugin.getConfig().getInt("fawe-compatibility.refresh-delay-ticks", 10);
   }

   public long getFaweCompatibilityRefreshCooldownMs() {
      return this.plugin.getConfig().getLong("fawe-compatibility.refresh-cooldown-ms", 2000L);
   }

   public int getFaweCompatibilityMaxChunksPerCycle() {
      return this.plugin.getConfig().getInt("fawe-compatibility.max-chunks-per-cycle", 10);
   }

   public ConfigManager(NekoAntiXray plugin) {
      this.plugin = plugin;
      this.configFile = new File(plugin.getDataFolder(), "config.yml");
   }

   public List getRandomFakeOrePool(Environment environment, int blockY) {
      String poolKey = this.resolveRandomOrePoolKey(environment, blockY);
      String resolvedPath = null;

      for (String basePath : RANDOM_ORE_CONFIG_PATHS) {
         if (this.config.get(basePath + "." + poolKey) instanceof List) {
            resolvedPath = basePath + "." + poolKey;
            break;
         }
      }

      List<String> oreNames;
      if (resolvedPath != null) {
         oreNames = this.config.getStringList(resolvedPath);
         if (!resolvedPath.startsWith("check.xray.spawn.random-ore") && this.warnedConfigPaths.add(resolvedPath)) {
            this.plugin
               .getLogger()
               .warning("随机假矿石: 矿石池位于非标准路径 " + resolvedPath + "(config.yml 中可能存在重复/错位的 random-ore 段), 已按该路径读取。建议把 random-ore 段统一放到 check.xray.spawn 下并删除重复段。");
         }
      } else {
         if (this.warnedConfigPaths.add("check.xray.spawn.random-ore." + poolKey)) {
            this.plugin.getLogger().warning("随机假矿石: 配置中缺少 check.xray.spawn.random-ore." + poolKey + ", 已使用内置默认矿石池(请检查 config.yml 中是否存在重复的 random-ore 段)。");
         }

         oreNames = this.getDefaultRandomOreNames(environment, blockY);
      }

      ArrayList<Material> orePool = new ArrayList<>();
      List<Material> blacklistedOreList = this.getBlacklistedOres();

      for (String oreName : oreNames) {
         Material material = Material.matchMaterial(oreName.trim().toUpperCase(Locale.ROOT));
         if (material != null && material.isItem() && (blacklistedOreList == null || !blacklistedOreList.contains(material))) {
            orePool.add(material);
         }
      }

      return orePool;
   }

   public boolean isHeavyModeEnabled() {
      String heavyModeSetting = this.getHeavyModeSetting();
      return "true".equals(heavyModeSetting) || "dynamic".equals(heavyModeSetting);
   }

   public boolean isHeavyModeDynamic() {
      return "dynamic".equals(this.getHeavyModeSetting());
   }

   private int clampAndValidateRayTraceThreads(int configuredThreads, boolean logValidation) {
      int clampedThreads = Math.max(1, Math.min(32, configuredThreads));
      if (clampedThreads != configuredThreads) {
         this.plugin.getLogger().warning(String.format("线程数 %d 超出范围 [1-32]，已调整为 %d", configuredThreads, clampedThreads));
      }

      if (logValidation) {
         String validationSummary = CpuInformation.validateThreadCount(clampedThreads);
         this.plugin.getLogger().info("射线追踪线程数: " + clampedThreads + " (手动配置)");
         this.plugin.getLogger().info("配置验证: " + validationSummary);
      }

      return clampedThreads;
   }

   public int getRayTraceThreads() {
      Object rawConfig = this.config.get("world-settings.default.ray-trace-threads");
      if (rawConfig == null) {
         this.plugin.getLogger().warning("未找到 ray-trace-threads 配置，使用自动模式");
         return CpuInformation.getRecommendedThreadCount();
      }

      if (rawConfig instanceof String) {
         String threadSetting = ((String)rawConfig).trim().toLowerCase();
         if ("auto".equals(threadSetting)) {
            int recommendedThreadCount = CpuInformation.getRecommendedThreadCount();
            this.plugin.getLogger().info("射线追踪线程数: AUTO 模式");
            CpuInformation.logCpuInfo();
            return recommendedThreadCount;
         }

         try {
            int parsedThreads = Integer.parseInt(threadSetting);
            return this.clampAndValidateRayTraceThreads(parsedThreads, true);
         } catch (NumberFormatException ex) {
            this.plugin.getLogger().warning("无效的 ray-trace-threads 配置: " + threadSetting + "，使用自动模式");
            return CpuInformation.getRecommendedThreadCount();
         }
      } else if (rawConfig instanceof Number) {
         int numericThreads = ((Number)rawConfig).intValue();
         return this.clampAndValidateRayTraceThreads(numericThreads, false);
      } else {
         this.plugin.getLogger().warning("ray-trace-threads 配置类型错误，使用自动模式");
         return CpuInformation.getRecommendedThreadCount();
      }
   }

   public int getWorldSettingsDefaultRayTraceThreads() {
      return this.getRayTraceThreads();
   }

   public static final class EnvironmentSwitchMap {
      static final int[] ENVIRONMENT_ORDINALS = new int[Environment.values().length];

      static {
         try {
            ENVIRONMENT_ORDINALS[Environment.NETHER.ordinal()] = 1;
         } catch (NoSuchFieldError ignored) {
         }

         try {
            ENVIRONMENT_ORDINALS[Environment.THE_END.ordinal()] = 2;
         } catch (NoSuchFieldError ignored) {
         }
      }
   }
}
