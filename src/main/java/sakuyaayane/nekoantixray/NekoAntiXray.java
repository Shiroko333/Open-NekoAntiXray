package sakuyaayane.nekoantixray;

import cn.ghostkokoro.jnic.JNICObfuscate;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.github.retrooper.packetevents.PacketEvents;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.UUID;
import org.bukkit.BanEntry;
import org.bukkit.BanList.Type;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;
import sakuyaayane.nekoantixray.chunk.ChunkObfuscationTask;
import sakuyaayane.nekoantixray.chunk.ChunkRefreshService;
import sakuyaayane.nekoantixray.chunk.FaweChunkRefresher;
import sakuyaayane.nekoantixray.command.AnnouncementCommand;
import sakuyaayane.nekoantixray.command.BanCommand;
import sakuyaayane.nekoantixray.command.CheckStatusCommand;
import sakuyaayane.nekoantixray.command.ConfigMenuCommand;
import sakuyaayane.nekoantixray.command.DebugChunksCommand;
import sakuyaayane.nekoantixray.command.DecoyOreCommand;
import sakuyaayane.nekoantixray.command.DetectionHistoryCommand;
import sakuyaayane.nekoantixray.command.DetectionOverviewCommand;
import sakuyaayane.nekoantixray.command.DiagnosticsCommand;
import sakuyaayane.nekoantixray.command.FakeSeedCommand;
import sakuyaayane.nekoantixray.command.HelpCommand;
import sakuyaayane.nekoantixray.command.InternalBypassCommand;
import sakuyaayane.nekoantixray.command.NaxCommandDispatcher;
import sakuyaayane.nekoantixray.command.NaxTabCompleter;
import sakuyaayane.nekoantixray.command.ObfuscationStatsCommand;
import sakuyaayane.nekoantixray.command.OreStatsCommand;
import sakuyaayane.nekoantixray.command.OreTrackCommand;
import sakuyaayane.nekoantixray.command.PlayerOreStatsCommand;
import sakuyaayane.nekoantixray.command.ProbeGuardCommand;
import sakuyaayane.nekoantixray.command.RealSeedCommand;
import sakuyaayane.nekoantixray.command.ReloadCommand;
import sakuyaayane.nekoantixray.command.ReplayCleanupCommand;
import sakuyaayane.nekoantixray.command.ResendCommand;
import sakuyaayane.nekoantixray.command.ResetViolationCommand;
import sakuyaayane.nekoantixray.command.SecureSeedCommand;
import sakuyaayane.nekoantixray.command.TrainingRecordCommand;
import sakuyaayane.nekoantixray.command.UpdateCommand;
import sakuyaayane.nekoantixray.command.ViolationLevelCommand;
import sakuyaayane.nekoantixray.compat.LeafCompatibilitySupport;
import sakuyaayane.nekoantixray.config.ConfigManager;
import sakuyaayane.nekoantixray.core.InternalBypassManager;
import sakuyaayane.nekoantixray.core.RuntimeDependencyManager;
import sakuyaayane.nekoantixray.core.WorldAntiXrayInitializer;
import sakuyaayane.nekoantixray.detection.ai.DeepLearningBlockListener;
import sakuyaayane.nekoantixray.detection.ai.OnnxInferenceEngine;
import sakuyaayane.nekoantixray.detection.ai.TrainingDataRecorder;
import sakuyaayane.nekoantixray.detection.BanInventoryCleaner;
import sakuyaayane.nekoantixray.detection.DetectionAnnouncer;
import sakuyaayane.nekoantixray.detection.MiningBehaviorDetector;
import sakuyaayane.nekoantixray.detection.XrayDetector;
import sakuyaayane.nekoantixray.lib.folialib.FoliaLib;
import sakuyaayane.nekoantixray.listener.AntiBrightnessListener;
import sakuyaayane.nekoantixray.listener.ChunkResendListener;
import sakuyaayane.nekoantixray.listener.CrossServerMessageListener;
import sakuyaayane.nekoantixray.listener.ExplosionAttributionListener;
import sakuyaayane.nekoantixray.listener.FakeOreProtectionListener;
import sakuyaayane.nekoantixray.listener.MassExcavationListener;
import sakuyaayane.nekoantixray.listener.OreStatisticsListener;
import sakuyaayane.nekoantixray.listener.RayTracePlayerListener;
import sakuyaayane.nekoantixray.listener.UnexposedMiningListener;
import sakuyaayane.nekoantixray.metrics.BukkitMetrics;
import sakuyaayane.nekoantixray.notification.DetectionNotificationManager;
import sakuyaayane.nekoantixray.notification.OfflineDetectionReporter;
import sakuyaayane.nekoantixray.notification.PeriodicAnnouncementManager;
import sakuyaayane.nekoantixray.packet.ChunkPacketListener;
import sakuyaayane.nekoantixray.packet.ProbeGuardPacketListener;
import sakuyaayane.nekoantixray.raytrace.PlayerRayTraceState;
import sakuyaayane.nekoantixray.raytrace.PlayerViewSnapshot;
import sakuyaayane.nekoantixray.raytrace.RayTraceHeartbeatTask;
import sakuyaayane.nekoantixray.raytrace.RayTracePlayerUpdateTask;
import sakuyaayane.nekoantixray.replay.ReplayCommand;
import sakuyaayane.nekoantixray.replay.ReplayGUIListener;
import sakuyaayane.nekoantixray.replay.ReplayManager;
import sakuyaayane.nekoantixray.replay.ReplaySessionManager;
import sakuyaayane.nekoantixray.seed.AntiSeedCrackerManager;
import sakuyaayane.nekoantixray.seed.BedrockSeedTransformer;
import sakuyaayane.nekoantixray.seed.BiomeNoiseTransformer;
import sakuyaayane.nekoantixray.seed.RealSeedStorage;
import sakuyaayane.nekoantixray.seed.WorldgenRandomTransformer;
import sakuyaayane.nekoantixray.stats.BanStatisticsManager;
import sakuyaayane.nekoantixray.stats.ObfuscationStatistics;
import sakuyaayane.nekoantixray.stats.OreStatsStorage;
import sakuyaayane.nekoantixray.stats.OreTrackingManager;
import sakuyaayane.nekoantixray.util.concurrent.ThreadPoolManager;
import sakuyaayane.nekoantixray.world.PlayerChunkKey;
import sakuyaayane.nekoantixray.world.TrackedChunkVisibility;

public class NekoAntiXray extends JavaPlugin implements Listener {
   private static NekoAntiXray instance;
   private static final String BAN_SOURCE = "NekoAntiXray";
   public final String pluginVersion = this.getDescription().getVersion();
   private ConfigManager configManager;
   private XrayDetector xrayDetector;
   private String customBanMessage;
   private boolean licenseExpired = false;
   private String licenseExpireDate;
   public boolean licenseVerified;
   private File logsDirectory;
   private ReplayManager replayManager;
   private ReplaySessionManager sessionManager;
   private ReplayCommand replayCommand;
   private FoliaLib foliaLib;
   private final AtomicBoolean disablingPlugin = new AtomicBoolean(false);
   private XrayDetector legacyXrayDetector;
   private ExplosionAttributionListener explosionAttributionListener;
   private WorldAntiXrayInitializer worldInitiationListener;
   private FakeSeedCommand fakeSeedCommand;
   private BanInventoryCleaner banInventoryCleaner;
   private MiningBehaviorDetector miningBehaviorDetector;
   private UnexposedMiningListener unexposedMiningListener;
   private MassExcavationListener massExcavationListener;
   private InternalBypassManager internalBypassManager;
   private AntiSeedCrackerManager antiSeedCrackerManager;
   private AntiBrightnessListener antiBrightness;
   private TrainingDataRecorder trainingDataRecorder;
   private OnnxInferenceEngine inferenceManager;
   private BanStatisticsManager banStatisticsManager;
   private PeriodicAnnouncementManager periodicAnnouncementManager;
   private OreStatsStorage oreStatsStorage;
   private OreTrackingManager oreTrackingManager;
   private OreStatisticsListener oreStatisticsListener;
   private RealSeedStorage realSeedStorage;
   private FaweChunkRefresher faweChunkRefresher;
   private static int structureSaltXor = 0;
   private static long worldSeedXor = 0L;
   private final ConcurrentMap<PlayerChunkKey, TrackedChunkVisibility> trackedChunkCache = new ConcurrentHashMap<>();
   private static volatile boolean rayTraceThirdPerson = false;
   private final ObfuscationStatistics obfuscationStatistics = new ObfuscationStatistics();
   private final ConcurrentHashMap<Long, Boolean> chunkObfuscationStates = new ConcurrentHashMap<>();
   private volatile boolean running = false;
   private volatile boolean rayTraceTimingDebug = false;
   private final ConcurrentMap<UUID, PlayerRayTraceState> playerRayTraceStates = new ConcurrentHashMap<>();
   private DetectionNotificationManager detectionNotificationManager;
   private OfflineDetectionReporter offlineDetectionReporter;
   private ChunkRefreshService chunkRefreshService;
   private ThreadPoolExecutor executorService;
   private ChunkPacketListener enhancedPacketListener;
   private ProbeGuardPacketListener diggingProbeGuardListener;
   private ThreadPoolExecutor chunkObfuscationExecutor;
   private Timer timer;
   private long rayTraceUpdateTicks = 1L;
   private final Set<UUID> heavyModeResentPlayers = ConcurrentHashMap.newKeySet();
   private final Set<UUID> obfuscationCancelledPlayers = ConcurrentHashMap.newKeySet();
   private final ConcurrentMap<UUID, Double> obfuscationCancelBaselineVl = new ConcurrentHashMap<>();
   private final Set<UUID> forcedObfuscationPlayers = ConcurrentHashMap.newKeySet();
   private static final double THIRD_PERSON_CAMERA_DISTANCE = 4.0;

   @Override
   public void onLoad() {
      RuntimeDependencyManager runtimeDependencyManager = new RuntimeDependencyManager(this);
      runtimeDependencyManager.loadDependencies();

      try {
         Instrumentation instrumentation = this.installByteBuddyAgent();
         int configuredStructureSaltXor = (int)(System.nanoTime() & 2147483647L);
         long configuredWorldSeedXor = new Random(System.nanoTime()).nextLong();
         long bedrockSeedXor = new Random(System.nanoTime()).nextLong();
         long biomeCoordinateXor = new Random(System.nanoTime() ^ 3114645857374772L).nextLong();
         boolean netherBedrockProtectionEnabled = true;
         boolean biomeSourceXorEnabled = true;
         boolean structurePlacementXorEnabled = true;
         if (this.getConfig() != null) {
            try {
               configuredStructureSaltXor = this.getConfig().getInt("secure-seed.structure-salt-xor", configuredStructureSaltXor);
            } catch (Exception ignored) {
            }

            try {
               configuredWorldSeedXor = this.getConfig().getLong("secure-seed.world-seed-xor", configuredWorldSeedXor);
            } catch (Exception ignored) {
            }

            try {
               bedrockSeedXor = this.getConfig().getLong("secure-seed.bedrock-seed-xor", bedrockSeedXor);
            } catch (Exception ignored) {
            }

            try {
               biomeCoordinateXor = this.getConfig().getLong("secure-seed.biome-coordinate-xor", biomeCoordinateXor);
            } catch (Exception ignored) {
            }

            try {
               netherBedrockProtectionEnabled = this.getConfig().getBoolean("antiSeedCracker.netherBedrockProtection.enabled", true);
            } catch (Exception ignored) {
            }

            try {
               biomeSourceXorEnabled = this.getConfig().getBoolean("secure-seed.biome-source-xor-enabled", true);
            } catch (Exception ignored) {
            }

            try {
               structurePlacementXorEnabled = this.getConfig().getBoolean("secure-seed.structure-placement-xor-enabled", true);
            } catch (Exception ignored) {
            }
         }

         structureSaltXor = configuredStructureSaltXor;
         worldSeedXor = configuredWorldSeedXor;
         if (biomeCoordinateXor == 0L) {
            biomeCoordinateXor = new Random(System.nanoTime() ^ 3114645857374772L).nextLong();
         }

         WorldgenRandomTransformer worldgenRandomTransformer = new WorldgenRandomTransformer(configuredWorldSeedXor, structurePlacementXorEnabled);
         BedrockSeedTransformer bedrockSeedTransformer = new BedrockSeedTransformer(bedrockSeedXor);
         BiomeNoiseTransformer biomeNoiseTransformer = new BiomeNoiseTransformer(biomeCoordinateXor);
         instrumentation.addTransformer(worldgenRandomTransformer, true);
         if (netherBedrockProtectionEnabled) {
            instrumentation.addTransformer(bedrockSeedTransformer, true);
         }

         if (biomeSourceXorEnabled) {
            instrumentation.addTransformer(biomeNoiseTransformer, true);
         }

         for (Class loadedClass : instrumentation.getAllLoadedClasses()) {
            String className = loadedClass.getName();
            if ("net.minecraft.world.level.levelgen.WorldgenRandom".equals(className)
               || netherBedrockProtectionEnabled && "net.minecraft.world.level.levelgen.SurfaceRules$VerticalGradientConditionSource".equals(className)
               || biomeSourceXorEnabled && "net.minecraft.world.level.biome.MultiNoiseBiomeSource".equals(className)) {
               try {
                  instrumentation.retransformClasses(loadedClass);
               } catch (Throwable ignored) {
               }
            }
         }

         String bedrockXorLabel = netherBedrockProtectionEnabled ? Long.toString(bedrockSeedXor) : "disabled";
         String biomeXorLabel = biomeSourceXorEnabled ? Long.toString(biomeCoordinateXor) : "disabled";
         String structurePlacementLabel = structurePlacementXorEnabled ? "on" : "off";
         this.logDebug(
            "[Agent] Early ASM transformer installed, world xor="
               + configuredWorldSeedXor
               + ", bedrock xor="
               + bedrockXorLabel
               + ", biome xor="
               + biomeXorLabel
               + ", structure-placement-xor="
               + structurePlacementLabel
         );
         this.logDebug("[Agent] WorldgenRandom ASM hook stats: " + WorldgenRandomTransformer.getHookStats());
      } catch (Throwable ex) {
         this.logDebug("[Agent] Early ASM transformer failed: " + ex.getMessage());
      }
   }

   @JNICObfuscate
   @Override
   public void onEnable() {
      this.foliaLib = new FoliaLib(this);
      instance = this;
      this.configManager = new ConfigManager(this);
      this.configManager.loadConfig();
      this.detectionNotificationManager = new DetectionNotificationManager(this);
      this.offlineDetectionReporter = new OfflineDetectionReporter(this);
      this.offlineDetectionReporter.initialize();
      this.chunkRefreshService = new ChunkRefreshService(this);
      this.realSeedStorage = new RealSeedStorage(this);
      this.logDebug("真实种子存储系统已初始化。");
      if (this.isPacketEventsAvailable()) {
         try {
            this.antiSeedCrackerManager = new AntiSeedCrackerManager(this, this.configManager);
            this.antiSeedCrackerManager.initialize();
            this.getLogger().info("PacketEvents 已找到，AntiSeedCracker 功能已启用。");
         } catch (Throwable ex) {
            this.getLogger().severe("AntiSeedCracker 初始化失败: " + ex.getMessage());
            ex.printStackTrace();
         }
      } else {
         this.getLogger().warning("PacketEvents 尚未启用，等待 PluginEnableEvent 延迟初始化 AntiSeedCracker...");
      }

      this.logDebug("Ban announcement message: " + this.configManager.getNotificationSystemXrayBanMessage());
      this.logDebug("Ban announcement detail: " + this.configManager.getNotificationSystemXrayBanDetail());
      this.licenseVerified = false;
      this.licenseVerified = true;
      this.explosionAttributionListener = new ExplosionAttributionListener(this);
      this.xrayDetector = new XrayDetector();
      this.replayManager = new ReplayManager(this);
      this.banInventoryCleaner = new BanInventoryCleaner(this);
      this.miningBehaviorDetector = new MiningBehaviorDetector(this);
      this.unexposedMiningListener = new UnexposedMiningListener(this);
      Bukkit.getPluginManager().registerEvents(this.unexposedMiningListener, this);
      this.trainingDataRecorder = new TrainingDataRecorder(this);
      this.inferenceManager = new OnnxInferenceEngine(this);
      this.banStatisticsManager = new BanStatisticsManager(this);
      this.periodicAnnouncementManager = new PeriodicAnnouncementManager(this, this.banStatisticsManager);
      this.oreStatsStorage = new OreStatsStorage(this);
      this.oreTrackingManager = new OreTrackingManager(this, this.oreStatsStorage);
      this.oreStatisticsListener = new OreStatisticsListener(this, this.oreStatsStorage, this.oreTrackingManager);
      this.getServer().getPluginManager().registerEvents(this.oreStatisticsListener, this);
      this.logDebug("矿石统计和追踪系统已初始化。");
      this.massExcavationListener = new MassExcavationListener(this, this.foliaLib, this.xrayDetector.getInternalBypassManager());
      if (this.configManager.isAntiBrightnessEnabled()) {
         this.antiBrightness = new AntiBrightnessListener(this, this.configManager);
         this.getServer().getPluginManager().registerEvents(this.antiBrightness, this);
         this.logDebug("AntiBrightness feature is enabled and registered.");
      } else {
         this.logDebug("AntiBrightness feature is disabled in config.");
      }

      new BukkitMetrics(this, 22719);
      this.initializeChunkObfuscationPool();
      this.getServer().getPluginManager().registerEvents(new DeepLearningBlockListener(this), this);
      this.getServer().getPluginManager().registerEvents(this, this);
      if (this.isEnhancedAntiXrayActive() && this.isPacketEventsAvailable() && this.hasPaperAntiXrayApi()) {
         this.worldInitiationListener = new WorldAntiXrayInitializer(this, this.xrayDetector);
         this.getServer().getPluginManager().registerEvents(this.worldInitiationListener, this);
         this.logDebug("WorldInitiationListener initialized (heavy-mode or enhanced-anti-xray enabled and PacketEvents available).");
         this.worldInitiationListener.initializeAllWorlds();
      } else if (!this.isEnhancedAntiXrayActive()) {
         this.logDebug("WorldInitiationListener skipped (heavy-mode and enhanced-anti-xray both disabled).");
      } else {
         this.logDebug("WorldInitiationListener skipped (PacketEvents not available).");
      }

      this.getServer().getPluginManager().registerEvents(this.trainingDataRecorder, this);
      this.getServer().getPluginManager().registerEvents(this.explosionAttributionListener, this);
      this.sessionManager = new ReplaySessionManager(this);
      this.replayCommand = new ReplayCommand(this, this.sessionManager);
      this.getServer().getPluginManager().registerEvents(this.sessionManager, this);
      this.getServer().getPluginManager().registerEvents(new ReplayGUIListener(this, this.replayCommand, this.sessionManager), this);
      LeafCompatibilitySupport.logCompatibilityDiagnostics(this);
      this.registerCommands();
      this.setupFoliaTasks();
      this.periodicAnnouncementManager.start();
      this.customBanMessage = this.configManager.getNotificationSystemBanMessagesCustomBanMessage();
      this.initializeLogsDirectory();
      this.foliaLib.getImpl().runTimer(wrappedTask -> this.pardonExpiredBans(), 1L, 72000L);
      if (this.isEnhancedAntiXrayActive()) {
         this.initializeRayTraceSystem();
      }

      this.registerProbeGuard();
      if (this.getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") != null || this.getServer().getPluginManager().getPlugin("WorldEdit") != null) {
         this.faweChunkRefresher = new FaweChunkRefresher(this);
         this.faweChunkRefresher.start();
         this.getLogger().info("检测到 FAWE/WorldEdit，启动Fuck FAWE,监听事件以确保Heavymode兼容。");
      }

      this.getServer().getMessenger().registerOutgoingPluginChannel(this, "bungeecord:main");
      this.getServer().getMessenger().registerIncomingPluginChannel(this, "nekoantixray:bungee", new CrossServerMessageListener());
      StringBuilder bannerInfo = new StringBuilder();
      bannerInfo.append("\n");
      bannerInfo.append("╔═══════════════════ Plugin Information ═══════════════════╗\n");
      bannerInfo.append("║ Status: Plugin Enabled!                                  ║\n");
      bannerInfo.append(String.format("║ Version: %-47s ║\n", this.getDescription().getVersion()));
      bannerInfo.append(String.format("║ Author: %-48s ║\n", this.getDescription().getAuthors().get(0)));
      bannerInfo.append("║ Server: Folia Compatible                                 ║\n");
      bannerInfo.append(String.format("║ Debug Mode: %-45s║\n", this.getConfigManager().isDebug() ? "Enabled" : "Disabled"));
      bannerInfo.append("╚══════════════════════════════════════════════════════════╝");
      this.getLogger().info("\n╔══════════════════════════════════════════════════════════════════╗\n║                    _   _      _           __  ___                ║\n║         ███╗   ██╗███████╗██╗  ██╗ ██████╗ ██╗  ██╗██████╗       ║\n║         ████╗  ██║██╔════╝██║ ██╔╝██╔═══██╗╚██╗██╔╝██╔══██╗      ║\n║         ██╔██╗ ██║█████╗  █████╔╝ ██║   ██║ ╚███╔╝ ██████╔╝      ║\n║         ██║╚██╗██║██╔══╝  ██╔═██╗ ██║   ██║ ██╔██╗ ██╔══██╗      ║\n║         ██║ ╚████║███████╗██║  ██╗╚██████╔╝██╔╝ ██╗██║  ██║      ║\n║         ╚═╝  ╚═══╝╚══════╝╚═╝  ╚═╝ ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝      ║\n║                     Anti X-Ray Protection System                 ║\n╚══════════════════════════════════════════════════════════════════╝\n" + bannerInfo.toString());
   }

   @EventHandler
   public void onPluginEnable(PluginEnableEvent event) {
      Plugin plugin = event.getPlugin();
      if (plugin.getName().equalsIgnoreCase("PacketEvents") && this.antiSeedCrackerManager == null) {
         this.getLogger().info("检测到 PacketEvents 已启用，正在延迟初始化...");

         try {
            this.antiSeedCrackerManager = new AntiSeedCrackerManager(this, this.configManager);
            this.antiSeedCrackerManager.initialize();
            this.getLogger().info("AntiSeedCracker 延迟初始化成功。");
         } catch (Throwable ex) {
            this.getLogger().severe("AntiSeedCracker 延迟初始化失败: " + ex.getMessage());
            ex.printStackTrace();
         }

         this.registerProbeGuard();
         if (this.isEnhancedAntiXrayActive() && this.hasPaperAntiXrayApi()) {
            this.worldInitiationListener = new WorldAntiXrayInitializer(this, this.xrayDetector);
            this.getServer().getPluginManager().registerEvents(this.worldInitiationListener, this);
            this.getLogger().info("WorldInitiation 监听器延迟初始化成功。");
            this.logDebug("WorldInitiationListener initialized (heavy-mode or enhanced-anti-xray enabled and PacketEvents available).");
            this.worldInitiationListener.initializeAllWorlds();
         } else if (!this.isEnhancedAntiXrayActive()) {
            this.logDebug("WorldInitiationListener skipped (heavy-mode and enhanced-anti-xray both disabled).");
         } else {
            this.logDebug("WorldInitiationListener skipped (PacketEvents not available).");
         }

         if (this.isEnhancedAntiXrayActive() && !this.running) {
            this.initializeRayTraceSystem();
         }
      }
   }

   @Override
   public void onDisable() {
      this.unregisterProbeGuard();
      if (this.replayCommand != null) {
         this.replayCommand.shutdown();
      }

      if (this.sessionManager != null) {
         this.sessionManager.cleanupAllSessions(true);
      }

      if (this.foliaLib != null) {
         this.foliaLib.getImpl().cancelAllTasks();
      }

      this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
      this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
      if (this.antiSeedCrackerManager != null) {
         this.antiSeedCrackerManager.shutdown();
      }

      if (this.xrayDetector != null) {
         this.xrayDetector.shutdownExecutor();
         this.xrayDetector.cleanupFakeOres();
         this.xrayDetector.clearAllData();
      }

      if (this.offlineDetectionReporter != null) {
         this.offlineDetectionReporter.saveAllOnlineLastSeen();
      }

      if (this.replayManager != null) {
         this.replayManager.cleanupAllReplays();
      }

      if (this.massExcavationListener != null) {
         this.massExcavationListener.clearAllData();
      }

      if (this.inferenceManager != null) {
         this.inferenceManager.shutdown();
      }

      if (this.antiBrightness != null) {
         HandlerList.unregisterAll(this.antiBrightness);
         this.getLogger().info("AntiBrightness feature has been unregistered.");
      }

      if (this.trackedChunkCache != null) {
         this.trackedChunkCache.clear();
         this.getLogger().info("ChunkPacket块缓存已清理完毕。");
      }

      if (this.periodicAnnouncementManager != null) {
         this.periodicAnnouncementManager.stop();
      }

      if (this.oreStatsStorage != null) {
         this.oreStatsStorage.save();
         this.getLogger().info("矿石统计数据已保存。");
      }

      if (this.faweChunkRefresher != null) {
         this.faweChunkRefresher.shutdown();
         this.faweChunkRefresher = null;
      }

      this.shutdownRayTraceSystem();
      this.shutdownChunkObfuscationPool();
      this.getLogger().info("所有回放会话和资源已清理完毕。");
      this.getLogger().info("NekoAntiXRay 插件已禁用!");
   }

   private void handleExternalBanFailure(
      Player player, String category, String commandTemplate, @Nullable String executedCommand, @Nullable Throwable cause
   ) {
      String failureReason = cause == null ? "指令未被本服务器执行(未注册/未知指令)" : this.getRootCauseMessage(cause);
      this.getLogger().severe("外部封禁指令执行失败, 玩家并未被封禁: " + player.getName());
      this.getLogger().severe("  实际执行: " + executedCommand);
      this.getLogger().severe("  配置模板: " + commandTemplate);
      this.getLogger().severe("  失败原因: " + failureReason);
      this.getLogger().severe("  排查建议: ①占位符数量需与指令声明的参数数量一致(如 CommandBridge 脚本只声明 1 个参数时不要传 %duration%); ②确认该指令已在当前子服注册(代理网络需单独注册); ③把指令贴进控制台手动执行一次即可复现问题。");
      String adminAlertHeader = "§c[NekoAntiXray] 外部封禁指令执行失败: §f" + player.getName() + " §c未被封禁, 请手动处理";
      String adminAlertDetail = "§7指令: §8" + executedCommand + " §7| 原因: §8" + failureReason;

      for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
         if (onlinePlayer.hasPermission("nekoantixray.notify")
            || onlinePlayer.hasPermission("nekoantixray.alerts")
            || onlinePlayer.hasPermission("nekoantixray.admin")) {
            onlinePlayer.sendMessage(adminAlertHeader);
            onlinePlayer.sendMessage(adminAlertDetail);
         }
      }

      if (this.configManager.isBanFallbackToBuiltinEnabled(category)) {
         this.getLogger().warning("已启用兜底策略: 改用内置封禁处理玩家 " + player.getName());
         this.banPlayer(player, "外部封禁指令失败兜底 (" + failureReason + ")");
      }
   }

   private String formatBanMessage(String reason, @Nullable Date expirationDate) {
      int banDurationDays = this.configManager.getNotificationSystemBanMessagesBanDuration();
      String durationText = banDurationDays == -1 ? "永久" : banDurationDays + "天";
      String unbanTimeText = expirationDate == null ? "永久" : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(expirationDate);
      String template = this.customBanMessage == null ? "" : this.customBanMessage;
      String effectiveReason = reason != null && !reason.isBlank() ? reason : "未知原因";
      return template.replace("{reason}", effectiveReason).replace("{duration}", durationText).replace("{unban_time}", unbanTimeText);
   }

   @EventHandler
   public void onPlayerLogin(PlayerLoginEvent event) {
      if (event.getResult() == Result.KICK_BANNED) {
         BanList banList = Bukkit.getBanList(Type.NAME);
         BanEntry banEntry = banList.getBanEntry(event.getPlayer().getName());
         if (banEntry != null && "NekoAntiXray".equalsIgnoreCase(banEntry.getSource())) {
            event.setKickMessage(this.formatBanMessage(banEntry.getReason(), banEntry.getExpiration()));
         }
      }
   }

   public static NekoAntiXray getInstance() {
      return instance;
   }

   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   public FaweChunkRefresher getFaweChunkRefresher() {
      return this.faweChunkRefresher;
   }

   public XrayDetector getXrayDetector() {
      return this.xrayDetector;
   }

   private void schedulePluginDisable() {
      Runnable task = () -> {
         if (this.isEnabled()) {
            this.getServer().getPluginManager().disablePlugin(this);
         }
      };

      try {
         if (this.foliaLib == null) {
            if (Bukkit.isPrimaryThread()) {
               task.run();
            } else {
               Bukkit.getScheduler().runTask(this, task);
            }

            return;
         }

         if (this.foliaLib.getImpl().isGlobalTickThread()) {
            task.run();
            return;
         }

         this.foliaLib.getImpl().runNextTick(wrappedTask -> task.run()).exceptionally(asyncError -> {
            this.disablingPlugin.set(false);
            this.getLogger().severe("无法在服务器调度线程禁用插件: " + this.getRootCauseMessage(asyncError));
            return null;
         });
      } catch (Exception scheduleEx) {
         this.disablingPlugin.set(false);
         this.getLogger().severe("无法调度插件禁用任务: " + scheduleEx.getMessage());
      }
   }

   public ReplaySessionManager getSessionManager() {
      return this.sessionManager;
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      if (this.faweChunkRefresher != null) {
         this.faweChunkRefresher.recordPlayerEdit(player.getUniqueId());
      }

      this.sessionManager.updateArmorStandVisibility(player);
      if (this.realSeedStorage != null) {
         World world = player.getWorld();
         if (!this.realSeedStorage.hasSeed(world.getName())) {
            this.realSeedStorage.storeWorldSeedIfAbsent(world);
         }
      }

      if (player.hasPermission("nekoantixray.admin")) {
         SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

         try {
            if (this.licenseExpireDate == null) {
               return;
            }

            Date expireDate = simpleDateFormat.parse(this.licenseExpireDate);
            long daysRemaining = (expireDate.getTime() - System.currentTimeMillis()) / 86400000L;
            this.runPlayerLater(player, () -> {
               if (daysRemaining <= 7L) {
                  player.sendMessage("§c[NekoAntiXRay] §4警告：插件授权即将过期，请及时续期！");
               }
            }, 20L);
         } catch (ParseException ex) {
            player.sendMessage("§c[NekoAntiXRay] §4无法解析过期日期，请检查配置！");
            this.getLogger().severe("无法解析过期日期: " + ex.getMessage());
         }
      }

      if (this.offlineDetectionReporter != null) {
         this.runPlayerLater(player, () -> {
            if (player.isOnline()) {
               this.offlineDetectionReporter.sendOfflineReportOnJoin(player);
            }
         }, 30L);
      }
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      if (this.faweChunkRefresher != null) {
         this.faweChunkRefresher.clearPlayerData(event.getPlayer().getUniqueId());
      }

      if (this.offlineDetectionReporter != null) {
         this.offlineDetectionReporter.markPlayerLastSeen(event.getPlayer());
      }
   }

   public XrayDetector getLegacyXrayDetector() {
      return this.legacyXrayDetector;
   }

   public ExplosionAttributionListener getExplosionAttributionListener() {
      return this.explosionAttributionListener;
   }

   public BanInventoryCleaner getBanInventoryCleaner() {
      return this.banInventoryCleaner;
   }

   public ReplayManager getReplayManager() {
      return this.replayManager;
   }

   public MassExcavationListener getMassExcavationListener() {
      return this.massExcavationListener;
   }

   public FoliaLib getFoliaScheduler() {
      return this.foliaLib;
   }

   public OnnxInferenceEngine getOnnxInferenceEngine() {
      return this.inferenceManager;
   }

   public MiningBehaviorDetector getMiningBehaviorDetector() {
      return this.miningBehaviorDetector;
   }

   public UnexposedMiningListener getUnexposedMiningListener() {
      return this.unexposedMiningListener;
   }

   public InternalBypassManager getInternalBypassManager() {
      return this.xrayDetector.getInternalBypassManager();
   }

   public ConcurrentMap getTrackedChunkCache() {
      return this.trackedChunkCache;
   }

   public ObfuscationStatistics getObfuscationStatistics() {
      return this.obfuscationStatistics;
   }

   public ConcurrentHashMap getChunkObfuscationStates() {
      return this.chunkObfuscationStates;
   }

   public DetectionNotificationManager getDetectionNotificationManager() {
      return this.detectionNotificationManager;
   }

   public OfflineDetectionReporter getOfflineDetectionReporter() {
      return this.offlineDetectionReporter;
   }

   public ChunkRefreshService getChunkRefreshService() {
      return this.chunkRefreshService;
   }

   public ThreadPoolExecutor getChunkObfuscationExecutor() {
      return this.chunkObfuscationExecutor;
   }

   public ProbeGuardPacketListener getProbeGuardPacketListener() {
      return this.diggingProbeGuardListener;
   }

   public boolean isRayTraceRunning() {
      return this.running;
   }

   public boolean isRayTraceTimingDebug() {
      return this.rayTraceTimingDebug;
   }

   public ConcurrentMap getPlayerRayTraceStates() {
      return this.playerRayTraceStates;
   }

   public ThreadPoolExecutor getRayTraceExecutor() {
      return this.executorService;
   }

   public long getRayTraceUpdateTicks() {
      return this.rayTraceUpdateTicks;
   }

   public boolean hasRayTraceState(Player player, PlayerRayTraceState playerRayTraceState) {
      if (playerRayTraceState == null) {
         return this.isRealPlayer(player) ? false : false;
      } else {
         return true;
      }
   }

   public static PlayerViewSnapshot[] createViewSnapshots(PlayerViewSnapshot playerViewSnapshot) {
      if (!rayTraceThirdPerson) {
         return new PlayerViewSnapshot[]{playerViewSnapshot};
      }

      Vector cameraPosition = playerViewSnapshot.getVector().clone().subtract(playerViewSnapshot.getDirection().clone().multiply(4.0));
      PlayerViewSnapshot thirdPersonSnapshot = new PlayerViewSnapshot(playerViewSnapshot.getWorld(), cameraPosition, playerViewSnapshot.getDirection());
      return new PlayerViewSnapshot[]{playerViewSnapshot, thirdPersonSnapshot};
   }

   public BanStatisticsManager getBanStatisticsManager() {
      return this.banStatisticsManager;
   }

   public PeriodicAnnouncementManager getPeriodicAnnouncementManager() {
      return this.periodicAnnouncementManager;
   }

   public OreStatsStorage getOreStatsStorage() {
      return this.oreStatsStorage;
   }

   public OreTrackingManager getOreTrackingManager() {
      return this.oreTrackingManager;
   }

   public RealSeedStorage getRealSeedStorage() {
      return this.realSeedStorage;
   }

   public static int getStructureSaltXor() {
      return structureSaltXor;
   }

   public static long getWorldSeedXor() {
      return worldSeedXor;
   }

   private Void handleReplayCleanupFailure(Throwable error) {
      this.getLogger().warning("禁用前清理回放实体失败: " + this.getRootCauseMessage(error));
      return null;
   }

   private Instrumentation installByteBuddyAgent() throws Exception {
      ClassLoader classLoader = this.getClass().getClassLoader();
      Class<?> agentClass = Class.forName("net.bytebuddy.agent.ByteBuddyAgent", true, classLoader);
      Method installMethod = agentClass.getMethod("install");
      return (Instrumentation)installMethod.invoke(null);
   }

   private void pardonExpiredBans() {
      BanList banList = Bukkit.getBanList(Type.NAME);
      Date now = new Date();

      for (Object rawEntry : banList.getBanEntries()) {
         BanEntry banEntry = (BanEntry)rawEntry;
         if (banEntry.getExpiration() != null && banEntry.getExpiration().before(now)) {
            banList.pardon(banEntry.getTarget());
            this.getLogger().info("已解除过期封禁: " + banEntry.getTarget());
         }
      }
   }

   private void handleUpdateCheckResult(String latestVersion) {
      if (latestVersion == null) {
         this.getLogger().warning("无法检查更新。");
      } else {
         String currentVersion = this.getDescription().getVersion();
         if (!currentVersion.equals(latestVersion)) {
            this.getLogger().info("发现新版本: " + latestVersion + "。当前版本: " + currentVersion);
            this.getLogger().info("你可以使用 /nekoantixrayupdate 命令来更新插件。");
         } else {
            this.getLogger().info("插件已是最新版本。");
         }
      }
   }

   private void setupFoliaTasks() {
      this.foliaLib.getImpl().runTimer(() -> {
         this.xrayDetector.cleanupInactiveMiningData();
         this.xrayDetector.decayPlayerViolations();
         FakeOreProtectionListener.cleanupExpiredOres();
      }, 1L, 1200L);
      this.foliaLib.getImpl().runTimer(() -> {
         if (this.banStatisticsManager != null) {
            this.banStatisticsManager.checkAutoReset();
         }
      }, 1L, 72000L);
   }

   public void reloadPluginConfig() {
      this.reloadConfig();
      this.configManager.loadConfig();
      this.customBanMessage = this.configManager.getNotificationSystemBanMessagesCustomBanMessage();
      if (this.miningBehaviorDetector != null) {
         this.miningBehaviorDetector.loadConfig();
      }

      if (this.fakeSeedCommand != null) {
         this.fakeSeedCommand.reloadConfig();
      }

      if (this.periodicAnnouncementManager != null) {
         this.periodicAnnouncementManager.reload();
      }

      if (this.oreTrackingManager != null) {
         this.oreTrackingManager.reload();
      }

      if (this.banInventoryCleaner != null) {
         this.banInventoryCleaner.reloadConfig();
      }

      if (this.configManager.isCheckXrayProbeGuardEnabled()) {
         this.registerProbeGuard();
      } else {
         this.unregisterProbeGuard();
      }

      this.getLogger().info("NekoAntiXRay 配置已重新加载");
   }

   public void banPlayer(Player player, String reason) {
      int banDurationDays = this.configManager.getNotificationSystemBanMessagesBanDuration();
      Date expirationDate = banDurationDays != -1 ? new Date(System.currentTimeMillis() + banDurationDays * 24L * 60L * 60L * 1000L) : null;
      String kickMessage = this.formatBanMessage(reason, expirationDate);
      ConfigManager currentConfigManager = this.configManager;
      ReplayManager currentReplayManager = this.replayManager;
      BanInventoryCleaner currentInventoryCleaner = this.banInventoryCleaner;
      this.logDebug("准备执行 runAtEntity 封禁流程: 玩家=" + player.getName() + ", UUID=" + String.valueOf(player.getUniqueId()) + ", 在线=" + player.isOnline());
      this.foliaLib.getImpl().runAtEntity(player, wrappedTask -> {
         this.logDebug("runAtEntity 回调开始: 玩家=" + player.getName() + ", UUID=" + String.valueOf(player.getUniqueId()) + ", 在线=" + player.isOnline());
         if (currentInventoryCleaner.isEnabled()) {
            this.logDebug("开始清理背包矿物: 玩家=" + player.getName() + ", UUID=" + String.valueOf(player.getUniqueId()) + ", 在线=" + player.isOnline());
            currentInventoryCleaner.cleanPlayerInventory(player);
            this.logDebug("完成清理背包矿物: 玩家=" + player.getName() + ", UUID=" + String.valueOf(player.getUniqueId()) + ", 在线=" + player.isOnline());
            this.logDebug("已为玩家 " + player.getName() + " 清理背包");
         }

         Bukkit.getBanList(Type.NAME).addBan(player.getName(), reason, expirationDate, "NekoAntiXray");
         this.logDebug("已将玩家 " + player.getName() + " 添加到封禁列表");
         if (this.banStatisticsManager != null) {
            double playerViolationLevel = this.xrayDetector.getPlayerViolationLevel(player);
            this.banStatisticsManager.recordBan(player, reason, playerViolationLevel);
         }

         DetectionAnnouncer.announceBan(player, reason);
         if (currentConfigManager.isReplayEnabled()) {
            currentReplayManager.saveReplay(player);
            this.logDebug("已为玩家 " + player.getName() + " 保存回放");
         }

         if (player.isOnline()) {
            player.kickPlayer(kickMessage);
            this.logDebug("已尝试使用自定义消息踢出玩家 " + player.getName());
         } else {
            this.logDebug("玩家 " + player.getName() + " 在尝试踢出前已下线");
         }

         this.logDebug("封禁任务已在玩家 " + player.getName() + " 的区域执行完毕");
      });
   }

   public void banPlayerOrCommand(Player player, String category, String reason, double violationLevel) {
      String effectiveReason = reason != null && !reason.isBlank() ? reason : "未知原因";
      String banCommandTemplate = this.configManager.getBanCommand(category);
      if (banCommandTemplate != null && !banCommandTemplate.isEmpty()) {
         String effectiveMode = category != null && !category.isEmpty() ? category : "xray";
         String durationText = this.configManager.getNotificationSystemBanMessagesBanDuration() == -1
            ? "永久"
            : String.valueOf(this.configManager.getNotificationSystemBanMessagesBanDuration());
         String resolvedCommand = banCommandTemplate.replace("%player%", player.getName())
            .replace("%reason%", effectiveReason)
            .replace("%duration%", durationText)
            .replace("%vl%", String.format("%.1f", violationLevel))
            .replace("%mode%", effectiveMode)
            .replaceFirst("^/+", "");
         this.logDebug("已配置外部封禁指令, 交由外部插件封禁: 玩家=" + player.getName() + ", 原因=" + effectiveReason + ", 指令=" + resolvedCommand);
         this.foliaLib.getImpl().runNextTick(wrappedTask -> {
            boolean dispatched = false;
            Throwable dispatchError = null;

            try {
               dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolvedCommand);
            } catch (Throwable ex) {
               dispatchError = ex;
            }

            if (dispatchError == null && dispatched) {
               this.logDebug("外部封禁指令执行完成: " + resolvedCommand);
               this.foliaLib.getImpl().runAtEntity(player, entityTask -> {
                  try {
                     if (this.banStatisticsManager != null) {
                        this.banStatisticsManager.recordBan(player, effectiveReason, violationLevel);
                     }

                     DetectionAnnouncer.announceBan(player, effectiveReason);
                     if (this.configManager.isReplayEnabled() && this.replayManager != null) {
                        this.replayManager.saveReplay(player);
                     }
                  } catch (Throwable postBanError) {
                     this.getLogger().severe("外部封禁统计/公告执行失败: " + postBanError.getMessage());
                  }
               });
            } else {
               this.handleExternalBanFailure(player, effectiveMode, banCommandTemplate, resolvedCommand, dispatchError);
            }
         });
      } else {
         this.logDebug("未配置外部封禁指令, 使用内置封禁: 玩家=" + player.getName() + ", 原因=" + effectiveReason);
         this.banPlayer(player, effectiveReason);
      }
   }

   private String formatBanMessageWithDefaultDuration(String reason) {
      int banDurationDays = this.configManager.getNotificationSystemBanMessagesBanDuration();
      Date expirationDate = banDurationDays != -1 ? new Date(System.currentTimeMillis() + banDurationDays * 24L * 60L * 60L * 1000L) : null;
      return this.formatBanMessage(reason, expirationDate);
   }

   private String resolvePlayerName(UUID uuid) {
      Player player = Bukkit.getPlayer(uuid);
      if (player != null) {
         return player.getName();
      }

      PlayerProfile playerProfile = Bukkit.createProfile(uuid);
      if (!playerProfile.completeFromCache()) {
         return null;
      }

      String name = playerProfile.getName();
      return name != null && !name.isEmpty() ? name : null;
   }

   private BanEntry getActivePluginBanEntry(String playerName) {
      if (playerName != null && !playerName.isEmpty()) {
         BanList banList = Bukkit.getBanList(Type.NAME);
         if (!banList.isBanned(playerName)) {
            return null;
         } else {
            BanEntry banEntry = banList.getBanEntry(playerName);
            if (banEntry == null) {
               return null;
            } else {
               String source = banEntry.getSource();
               if (source != null && "NekoAntiXray".equalsIgnoreCase(source)) {
                  Date expiration = banEntry.getExpiration();
                  return expiration != null && expiration.getTime() <= System.currentTimeMillis() ? null : banEntry;
               } else {
                  return null;
               }
            }
         }
      } else {
         return null;
      }
   }

   public void disablePluginSafely() {
      if (this.isEnabled() && this.disablingPlugin.compareAndSet(false, true)) {
         if (this.replayCommand != null) {
            this.replayCommand.shutdown();
         }

         CompletableFuture<Void> cleanupFuture = this.sessionManager != null
            ? this.sessionManager.cleanupBeforeDisable()
            : CompletableFuture.completedFuture(null);
         cleanupFuture.completeOnTimeout(null, 2L, TimeUnit.SECONDS)
            .exceptionally(error -> this.handleReplayCleanupFailure(error))
            .thenRun(() -> this.schedulePluginDisable());
      }
   }

   private String getRootCauseMessage(Throwable error) {
      Throwable rootCause = error;

      while (rootCause.getCause() != null) {
         rootCause = rootCause.getCause();
      }

      return rootCause.getMessage() != null ? rootCause.getMessage() : rootCause.getClass().getSimpleName();
   }

   private void initializeLogsDirectory() {
      this.logsDirectory = new File(this.getDataFolder(), "logs");
      if (!this.logsDirectory.exists()) {
         this.logsDirectory.mkdirs();
      }
   }

   public void logDetectionEvent(Player player, String checkName, double violationLevel, String details) {
      SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      String timestamp = timestampFormat.format(new Date());
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      String logFileName = dateFormat.format(new Date()) + ".log";
      File logFile = new File(this.logsDirectory, logFileName);
      String logEntry = String.format("[%s] %s - %s - VL: %.2f - %s", timestamp, player.getName(), checkName, violationLevel, details);

      try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
         writer.println(logEntry);
      } catch (IOException ex) {
         this.getLogger().severe("无法写入日志文件: " + ex.getMessage());
      }
   }

   public static void TrueXrayCheck() {
      Bukkit.getScheduler().runTaskTimer(getInstance(), () -> {
         for (int i = 0; i < 100; i++) {
            String randomMessage = generateRandomString(50);
            Bukkit.getServer().getConsoleSender().sendMessage(randomMessage);

            for (Player player : Bukkit.getOnlinePlayers()) {
               player.sendMessage(randomMessage);
            }
         }
      }, 20L, 20L);
   }

   private static String generateRandomString(int length) {
      StringBuilder builder = new StringBuilder(length);
      Random random = new Random();

      for (int i = 0; i < length; i++) {
         int charIndex = random.nextInt(88);
         builder.append("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{}|;:,.<>?".charAt(charIndex));
      }

      return builder.toString();
   }

   public void schedulePlayerTask(Player player, Runnable task, long delayTicks) {
      this.runPlayerLater(player, task, delayTicks);
   }

   public boolean hasEntityScheduler() {
      try {
         Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
         return true;
      } catch (ClassNotFoundException ex) {
         return false;
      }
   }

   public boolean isForcedObfuscation(Player player) {
      return player != null && this.forcedObfuscationPlayers.contains(player.getUniqueId());
   }

   public boolean isObfuscationCancelled(Player player) {
      return player != null && this.obfuscationCancelledPlayers.contains(player.getUniqueId());
   }

   public double getObfuscationCancelBaselineVl(Player player) {
      return player == null ? 0.0 : this.obfuscationCancelBaselineVl.getOrDefault(player.getUniqueId(), 0.0);
   }

   public void cancelPlayerObfuscation(Player player) {
      if (player != null && player.isOnline()) {
         this.obfuscationCancelledPlayers.add(player.getUniqueId());
         this.forcedObfuscationPlayers.remove(player.getUniqueId());
         double baselineVl = 0.0;

         try {
            baselineVl = this.xrayDetector.getPlayerViolationLevel(player);
         } catch (Throwable ignored) {
         }

         this.obfuscationCancelBaselineVl.put(player.getUniqueId(), baselineVl);
         this.heavyModeResentPlayers.remove(player.getUniqueId());
         int resendRadius = Math.max(1, this.getConfig().getInt("check.xray.heavy-mode-resend-radius", 4));
         this.chunkRefreshService.resendChunks(player, resendRadius);
      }
   }

   public void forcePlayerObfuscation(Player player) {
      if (player != null) {
         this.obfuscationCancelledPlayers.remove(player.getUniqueId());
         this.obfuscationCancelBaselineVl.remove(player.getUniqueId());
         this.heavyModeResentPlayers.add(player.getUniqueId());
         this.forcedObfuscationPlayers.add(player.getUniqueId());
      }
   }

   public void clearObfuscationCancelState(Player player) {
      if (player != null) {
         this.obfuscationCancelledPlayers.remove(player.getUniqueId());
         this.obfuscationCancelBaselineVl.remove(player.getUniqueId());
         this.heavyModeResentPlayers.remove(player.getUniqueId());
      }
   }

   public void triggerHeavyModeResend(Player player) {
      if (player != null
         && player.isOnline()
         && this.getConfig().getBoolean("check.xray.heavy-mode-resend-on-trigger", true)
         && this.heavyModeResentPlayers.add(player.getUniqueId())) {
         int resendRadius = Math.max(1, this.getConfig().getInt("check.xray.heavy-mode-resend-radius", 4));
         this.getLogger().info("玩家 " + player.getName() + " 触发 heavy 混淆, 重新隐藏/重发周围 " + resendRadius + " 格");
         this.chunkRefreshService.refreshBlocks(player, resendRadius, false);
         this.chunkRefreshService.hideObfuscationTargets(player, resendRadius);
         this.chunkRefreshService.resendChunks(player, resendRadius);
      }
   }

   public void clearHeavyModeResentFlag(Player player) {
      if (player != null) {
         this.heavyModeResentPlayers.remove(player.getUniqueId());
      }
   }

   public void recordChunkObfuscationState(World world, int chunkX, int chunkZ, boolean obfuscated) {
      long chunkKey = world.getUID().getMostSignificantBits() << 32
         ^ world.getUID().getLeastSignificantBits() & 4294967295L
         ^ (long)chunkX << 32
         ^ chunkZ & 4294967295L;
      if (this.chunkObfuscationStates.size() > 10000) {
         this.chunkObfuscationStates.clear();
      }

      this.chunkObfuscationStates.put(chunkKey, obfuscated);
   }

   public boolean isChunkObfuscated(World world, int chunkX, int chunkZ) {
      long chunkKey = world.getUID().getMostSignificantBits() << 32
         ^ world.getUID().getLeastSignificantBits() & 4294967295L
         ^ (long)chunkX << 32
         ^ chunkZ & 4294967295L;
      return Boolean.TRUE.equals(this.chunkObfuscationStates.get(chunkKey));
   }

   private boolean isPacketEventsAvailable() {
      Plugin lowercasePlugin = Bukkit.getPluginManager().getPlugin("packetevents");
      if (lowercasePlugin != null && lowercasePlugin.isEnabled()) {
         return true;
      }

      Plugin capitalizedPlugin = Bukkit.getPluginManager().getPlugin("PacketEvents");
      return capitalizedPlugin != null && capitalizedPlugin.isEnabled();
   }

   public boolean hasPaperAntiXrayApi() {
      try {
         Class.forName("io.papermc.paper.antixray.ChunkPacketBlockController", false, this.getClass().getClassLoader());
         return true;
      } catch (ClassNotFoundException | LinkageError ex) {
         return false;
      }
   }

   private void initializeChunkObfuscationPool() {
      if (this.chunkObfuscationExecutor == null || this.chunkObfuscationExecutor.isShutdown()) {
         int availableProcessors = Runtime.getRuntime().availableProcessors();
         int maxThreads = this.getConfig().getInt("world-settings.default.chunk-obfuscation-max-threads", 8);
         int threadCount = Math.max(1, Math.min(maxThreads, availableProcessors / 2));
         int queueCapacity = Math.max(64, this.getConfig().getInt("world-settings.default.chunk-obfuscation-queue-capacity", 256));
         this.chunkObfuscationExecutor = new ThreadPoolExecutor(
            threadCount,
            threadCount,
            30L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(queueCapacity),
            new ThreadFactoryBuilder().setNameFormat("NekoAntiXray-Chunk-%d").setDaemon(true).setPriority(4).build(),
            new AbortPolicy()
         );
         this.chunkObfuscationExecutor.allowCoreThreadTimeOut(true);
         this.getLogger().info("区块混淆异步线程池已创建: 线程=" + threadCount + ", 队列=" + queueCapacity);
      }
   }

   private void startRayTraceSystem() {
      this.initializeRayTraceSystem();
   }

   public void setRayTraceTimingDebug(boolean enabled) {
      this.rayTraceTimingDebug = enabled;
   }

   public boolean isRealPlayer(Player player) {
      return !player.hasMetadata("NPC");
   }

   public static void setRayTraceThirdPerson(boolean enabled) {
      rayTraceThirdPerson = enabled;
   }

   public boolean isWorldSettingsDefaultEnabled(World world) {
      try {
         boolean defaultEnabled = this.getConfig().getBoolean("world-settings.default.enabled", false);
         return this.getConfig().getBoolean("world-settings." + world.getName() + ".enabled", defaultEnabled);
      } catch (Exception ex) {
         return false;
      }
   }

   public boolean isAnyWorldEnhancedAntiXrayEnabled() {
      return this.hasAnyEnabledAntiXrayWorld();
   }

   public boolean isFoliaServer() {
      return this.foliaLib != null && this.foliaLib.isFolia();
   }

   public Long getBanExpirationMillis(String playerName) {
      BanEntry activePluginBanEntry = this.getActivePluginBanEntry(playerName);
      if (activePluginBanEntry == null) {
         return null;
      }

      Date expiration = activePluginBanEntry.getExpiration();
      return expiration == null ? null : expiration.getTime();
   }

   public boolean isPlayerBannedByPlugin(String playerName) {
      return this.getActivePluginBanEntry(playerName) != null;
   }

   public void logDebug(String message) {
      if (this.configManager == null || this.configManager.isDebug()) {
         this.getLogger().info("[Debug] " + message);
      }
   }

   public void runPlayerLater(Player player, Runnable task, long delayTicks) {
      if (player != null && task != null) {
         if (this.hasEntityScheduler()) {
            player.getScheduler().runDelayed(this, scheduledTask -> {
               if (player.isOnline()) {
                  task.run();
               }
            }, null, delayTicks);
         } else {
            Bukkit.getScheduler().runTaskLater(this, () -> {
               if (player.isOnline()) {
                  task.run();
               }
            }, delayTicks);
         }
      }
   }

   private void markAbortedChunkTaskReady(Runnable task) {
      if (this.hasPaperAntiXrayApi() && task instanceof ChunkObfuscationTask) {
         ((ChunkObfuscationTask)task).getChunkPacket().setReady(true);
      }
   }

   public boolean hasAnyEnabledAntiXrayWorld() {
      for (World world : Bukkit.getWorlds()) {
         if (this.isWorldSettingsDefaultEnabled(world)) {
            return true;
         }
      }

      return false;
   }

   public Long getBanExpirationMillis(UUID uuid) {
      if (uuid == null) {
         return null;
      }

      String playerName = this.resolvePlayerName(uuid);
      return playerName != null && !playerName.isEmpty() ? this.getBanExpirationMillis(playerName) : null;
   }

   public boolean isPlayerBannedByPlugin(UUID uuid) {
      if (uuid == null) {
         return false;
      }

      String playerName = this.resolvePlayerName(uuid);
      return playerName != null && !playerName.isEmpty() ? this.isPlayerBannedByPlugin(playerName) : false;
   }

   private void registerCommands() {
      NaxCommandDispatcher naxCommandDispatcher = new NaxCommandDispatcher(this);
      naxCommandDispatcher.registerSubCommand("reload", new ReloadCommand(this));
      naxCommandDispatcher.registerSubCommand("ban", new BanCommand(this));
      naxCommandDispatcher.registerSubCommand("resetviolation", new ResetViolationCommand(this));
      naxCommandDispatcher.registerSubCommand("replay", this.replayCommand);
      naxCommandDispatcher.registerSubCommand("togglemassexcavation", new CheckStatusCommand(this));
      naxCommandDispatcher.registerSubCommand("record", new TrainingRecordCommand(this.trainingDataRecorder));
      naxCommandDispatcher.registerSubCommand("internalbypass", new InternalBypassCommand(this));
      naxCommandDispatcher.registerSubCommand("secureseed", new SecureSeedCommand(this));
      naxCommandDispatcher.registerSubCommand("cleanup", new ReplayCleanupCommand(this));
      naxCommandDispatcher.registerSubCommand("announcement", new AnnouncementCommand(this));
      naxCommandDispatcher.registerSubCommand("realseed", new RealSeedCommand());
      naxCommandDispatcher.registerSubCommand("orestats", new PlayerOreStatsCommand(this));
      naxCommandDispatcher.registerSubCommand("oretop", new OreStatsCommand(this));
      naxCommandDispatcher.registerSubCommand("oretrack", new OreTrackCommand(this));
      naxCommandDispatcher.registerSubCommand("diagnose", new DiagnosticsCommand(this));
      naxCommandDispatcher.registerSubCommand("stats", new ObfuscationStatsCommand(this));
      naxCommandDispatcher.registerSubCommand("debugchunks", new DebugChunksCommand(this));
      ConfigMenuCommand configMenuCommand = new ConfigMenuCommand(this);
      naxCommandDispatcher.registerSubCommand("gui", configMenuCommand);
      this.getServer().getPluginManager().registerEvents(configMenuCommand, this);
      naxCommandDispatcher.registerSubCommand("vl", new ViolationLevelCommand(this));
      naxCommandDispatcher.registerSubCommand("decoy", new DecoyOreCommand(this));
      naxCommandDispatcher.registerSubCommand("resend", new ResendCommand(this));
      naxCommandDispatcher.registerSubCommand("probeguard", new ProbeGuardCommand(this));
      naxCommandDispatcher.registerSubCommand("report", new DetectionHistoryCommand(this));
      naxCommandDispatcher.registerSubCommand("summary", new DetectionOverviewCommand(this));
      naxCommandDispatcher.registerSubCommand("help", new HelpCommand(naxCommandDispatcher));
      this.getCommand("nekoantixrayupdate").setExecutor(new UpdateCommand(this));
      this.getCommand("nax").setExecutor(naxCommandDispatcher);
      FakeSeedCommand seedCommand = new FakeSeedCommand(this);
      this.getCommand("seed").setExecutor(seedCommand);
      this.getCommand("fakeseedreload").setExecutor(seedCommand);
      this.getServer().getPluginManager().registerEvents(seedCommand, this);
      NaxTabCompleter naxTabCompleter = new NaxTabCompleter(this.getDataFolder());
      this.getCommand("nax").setTabCompleter(naxTabCompleter);
      this.getCommand("nekoantixrayupdate").setTabCompleter(naxTabCompleter);
   }

   private void shutdownChunkObfuscationPool() {
      ThreadPoolExecutor threadPoolExecutor = this.chunkObfuscationExecutor;
      this.chunkObfuscationExecutor = null;
      if (threadPoolExecutor != null) {
         threadPoolExecutor.shutdown();

         try {
            if (!threadPoolExecutor.awaitTermination(5L, TimeUnit.SECONDS)) {
               for (Runnable pendingTask : threadPoolExecutor.shutdownNow()) {
                  this.markAbortedChunkTaskReady(pendingTask);
               }
            }
         } catch (InterruptedException ex) {
            for (Runnable abortedTask : threadPoolExecutor.shutdownNow()) {
               this.markAbortedChunkTaskReady(abortedTask);
            }

            Thread.currentThread().interrupt();
         }
      }
   }

   public void registerProbeGuard() {
      if (this.diggingProbeGuardListener == null) {
         if (!this.isPacketEventsAvailable()) {
            this.logDebug("鬼手探测防护未注册: PacketEvents 不可用");
         } else if (!this.configManager.isCheckXrayProbeGuardEnabled()) {
            this.getLogger().info("鬼手探测防护(反 TrueSight)未启用 (check.xray.probe-guard.enabled: false)");
         } else {
            try {
               this.diggingProbeGuardListener = new ProbeGuardPacketListener(this);
               PacketEvents.getAPI().getEventManager().registerListener(this.diggingProbeGuardListener);
               this.getServer().getPluginManager().registerEvents(this.diggingProbeGuardListener, this);
               String heavyModeSetting = this.configManager.getHeavyModeSetting();
               boolean requireHeavyMode = this.configManager.isCheckXrayProbeGuardRequireHeavyMode();
               boolean onlyObfuscatedPlayers = this.configManager.isCheckXrayProbeGuardOnlyObfuscatedPlayers();
               this.getLogger()
                  .info(
                     "鬼手探测防护(反 TrueSight)已注册: heavy-mode="
                        + heavyModeSetting
                        + ", require-heavy-mode="
                        + requireHeavyMode
                        + ", only-obfuscated-players="
                        + onlyObfuscatedPlayers
                  );
               if (this.configManager.isCheckXrayProbeGuardRequireHeavyMode() && !this.configManager.isHeavyModeEnabled()) {
                  this.getLogger()
                     .warning(
                        "鬼手探测防护当前不会拦截任何数据包: require-heavy-mode=true 但 heavy-mode 未开启。若服务器只使用 Paper 自带反矿透, 请把 check.xray.probe-guard.require-heavy-mode 设为 false。"
                     );
               }
            } catch (Throwable ex) {
               this.getLogger().severe("鬼手探测防护注册失败: " + ex.getMessage());
               this.diggingProbeGuardListener = null;
            }
         }
      }
   }

   public void unregisterProbeGuard() {
      if (this.diggingProbeGuardListener != null) {
         try {
            PacketEvents.getAPI().getEventManager().unregisterListener(this.diggingProbeGuardListener);
         } catch (Exception ex) {
            this.logDebug("注销DiggingProbeGuardListener时出错: " + ex.getMessage());
         } finally {
            this.diggingProbeGuardListener = null;
         }
      }
   }

   private void shutdownRayTraceSystem() {
      this.running = false;
      if (this.timer != null) {
         this.timer.cancel();
      }

      if (this.executorService != null) {
         ThreadPoolManager.shutdownPool(this.executorService, 5L);
      }

      this.playerRayTraceStates.clear();
      if (this.enhancedPacketListener != null) {
         try {
            PacketEvents.getAPI().getEventManager().unregisterListener(this.enhancedPacketListener);
         } catch (Exception ex) {
            this.logDebug("注销EnhancedPacketListener时出错: " + ex.getMessage());
         } finally {
            this.enhancedPacketListener = null;
         }
      }

      this.getLogger().info("射线追踪系统已关闭");
   }

   public boolean isEnhancedAntiXrayActive() {
      return this.configManager.isHeavyModeEnabled() && this.hasAnyEnabledAntiXrayWorld();
   }

   private void initializeRayTraceSystem() {
      if (!this.running) {
         if (!this.hasPaperAntiXrayApi()) {
            this.getLogger().warning("当前核心不支持数据包混淆 API (io.papermc.paper.antixray, 需要 Paper/Leaf 1.21.4+), 已跳过增强反X光/射线追踪系统。其他检测功能正常。");
         } else {
            this.running = true;
            int rayTraceThreads = this.configManager.getRayTraceThreads();
            long msPerRayTraceTick = Math.max(this.getConfig().getLong("settings.enhanced-anti-xray.ms-per-ray-trace-tick", 50L), 1L);
            this.rayTraceUpdateTicks = Math.max(this.getConfig().getLong("settings.enhanced-anti-xray.update-ticks", 1L), 1L);
            boolean enablePriorityQueue = this.getConfig().getBoolean("world-settings.default.enable-priority-queue", true);
            int queueCapacity = this.getConfig().getInt("world-settings.default.queue-capacity", 512);
            int maxThreads = Math.max(rayTraceThreads, rayTraceThreads * 2);
            this.executorService = ThreadPoolManager.createDynamicPool(
               rayTraceThreads, maxThreads, queueCapacity, "NekoAntiXray-RayTrace-%d", enablePriorityQueue
            );
            this.getLogger()
               .info("动态线程池已创建: 核心线程=" + rayTraceThreads + ", 最大线程=" + maxThreads + ", 队列容量=" + queueCapacity + ", 优先级队列=" + enablePriorityQueue);
            this.timer = new Timer("NekoAntiXray tick thread", true);
            this.timer.schedule(new RayTraceHeartbeatTask(this), 0L, msPerRayTraceTick);
            if (!this.isFoliaServer()) {
               new RayTracePlayerUpdateTask(this).runTaskTimer(this, 0L, this.rayTraceUpdateTicks);
            } else {
               this.foliaLib.getImpl().runTimer(wrappedTask -> {
                  for (Player player : this.getServer().getOnlinePlayers()) {
                     this.foliaLib.getImpl().runAtEntity(player, entityTask -> new RayTracePlayerUpdateTask(this, player).run());
                  }
               }, 1L, this.rayTraceUpdateTicks);
            }

            this.logDebug("RayTrace heartbeat日志间隔: 30000ms (仅当开启debug时输出)");
            if (this.isPacketEventsAvailable()) {
               this.enhancedPacketListener = new ChunkPacketListener(this);
               PacketEvents.getAPI().getEventManager().registerListener(this.enhancedPacketListener);
               this.getServer().getPluginManager().registerEvents(new RayTracePlayerListener(this), this);
               this.getLogger().info("射线追踪系统已注册");
            } else {
               this.getLogger().warning("PacketEvents未找到，射线追踪系统将受限");
            }

            this.getServer().getPluginManager().registerEvents(new ChunkResendListener(this), this);
            this.getLogger().info("射线追踪系统已初始化，线程数: " + rayTraceThreads + "，间隔: " + msPerRayTraceTick + "ms");
         }
      }
   }
}
