package sakuyaayane.nekoantixray;

import io.papermc.paper.antixray.ChunkPacketBlockController;
import io.papermc.paper.configuration.WorldConfiguration.Anticheat.AntiXray;
import io.papermc.paper.configuration.type.EngineMode;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;

public class WorldAntiXrayInitializer implements Listener {
   private final NekoAntiXray plugin;
   private final XrayDetector xrayDetector;

   public WorldAntiXrayInitializer(NekoAntiXray plugin, XrayDetector xrayDetector) {
      this.plugin = plugin;
      this.xrayDetector = xrayDetector;
      NekoAntiXray.getInstance().logDebug("World初始化事件监听器 初始化完成");
   }

   @EventHandler
   public void onWorld(WorldInitEvent event) {
      this.initializeWorld(event.getWorld());
   }

   private ServerLevel getServerLevel(World world) {
      try {
         Class<?> worldClass = world.getClass();
         Method getHandleMethod = worldClass.getMethod("getHandle");
         return (ServerLevel)getHandleMethod.invoke(world);
      } catch (Exception ex) {
         this.plugin.logDebug("Failed to get world handle using reflection: " + ex.getMessage());
         this.plugin.logDebug("World class: " + world.getClass().getName());
         return null;
      }
   }

   public void initializeAllWorlds() {
      for (World world : Bukkit.getWorlds()) {
         this.initializeWorld(world);
      }
   }

   public void initializeWorld(World world) {
      try {
         if (!this.plugin.hasPaperAntiXrayApi()) {
            this.plugin.logDebug("当前核心缺少 anti-xray API, 跳过世界 " + world.getName() + " 的增强反X光控制器安装");
            return;
         }

         this.plugin.logDebug("开始处理世界初始化事件: " + world.getName());
         String worldName = world.getName();
         boolean heavyModeActive = this.plugin.isEnabled() && this.plugin.getConfigManager().isHeavyModeEnabled();
         boolean enhancedAntiXrayEnabled = this.plugin.isEnabled() && this.plugin.isWorldSettingsDefaultEnabled(world);
         if (heavyModeActive && enhancedAntiXrayEnabled) {
            this.plugin.logDebug("插件已启用且反X-Ray系统已开启，开始初始化反X光系统");
            this.plugin.logDebug("Heavy Mode: " + heavyModeActive + ", Enhanced Anti-XRay: " + enhancedAntiXrayEnabled);

            try {
               ServerLevel paperLevel = this.getServerLevel(world);
               if (paperLevel != null) {
                  AntiXray antiXray = paperLevel.paperConfig().anticheat.antiXray;
                  if (!antiXray.enabled) {
                     this.plugin.getLogger().warning("[" + worldName + "] Paper anti-xray 未启用! 请在 paper.yml 设置 anti-xray.enabled: true, 否则 heavy mode 数据包混淆不会生效。");
                  }

                  if (antiXray.engineMode != EngineMode.HIDE) {
                     this.plugin
                        .getLogger()
                        .warning("[" + worldName + "] Paper anti-xray engine-mode 不是 1(HIDE)! 请在 paper.yml 设置 anti-xray.engine-mode: 1, 否则 Y 高度混淆可能异常。");
                  }

                  int paperMaxBlockHeight = antiXray.maxBlockHeight >> 4 << 4;
                  int configuredMaxBlockHeight = this.plugin
                        .getConfig()
                        .getInt(
                           "world-settings." + worldName + ".anti-xray.max-block-height",
                           this.plugin.getConfig().getInt("world-settings.default.anti-xray.max-block-height", paperMaxBlockHeight)
                        )
                     >> 4
                     << 4;
                  if (configuredMaxBlockHeight > paperMaxBlockHeight) {
                     Logger logger = this.plugin.getLogger();
                     logger.warning(
                        "["
                           + worldName
                           + "] 插件配置 max-block-height="
                           + configuredMaxBlockHeight
                           + " 高于 Paper 的 "
                           + paperMaxBlockHeight
                           + ". Paper 只对低于其 max-block-height 的 Y 生效! 请在 paper.yml 设置 anti-xray.max-block-height: "
                           + configuredMaxBlockHeight
                           + " (默认 64 会导致 Y>=64 不混淆)。"
                     );
                  }
               }
            } catch (Throwable ignored) {
            }

            FileConfiguration config = this.plugin.getConfig();
            this.plugin.logDebug("获取插件配置文件完成");
            this.plugin.logDebug("世界名称: " + worldName);
            boolean rehideBlocks;
            double rehideDistance;
            int maxRayTraceBlockCountPerChunk;
            List rayTraceBlocks;
            boolean rayTraceThirdPerson;
            double rayTraceDistance;
            if (heavyModeActive) {
               rayTraceThirdPerson = this.plugin.getConfigManager().isWorldSettingsDefaultAntiXrayRayTraceThirdPerson(worldName);
               rayTraceDistance = this.plugin.getConfigManager().getWorldSettingsDefaultAntiXrayRayTraceDistance(worldName);
               rehideBlocks = this.plugin.getConfigManager().isWorldSettingsDefaultAntiXrayRehideBlocks(worldName);
               rehideDistance = this.plugin.getConfigManager().getWorldSettingsDefaultAntiXrayRehideDistance(worldName);
               ConfigManager configManager = this.plugin.getConfigManager();
               maxRayTraceBlockCountPerChunk = configManager.getWorldSettingsDefaultAntiXrayMaxRayTraceBlockCountPerChunk(worldName);
               rayTraceBlocks = this.plugin.getConfigManager().getWorldSettingsDefaultAntiXrayRayTraceBlocks(worldName);
            } else {
               rayTraceThirdPerson = config.getBoolean(
                  "world-settings." + worldName + ".anti-xray.ray-trace-third-person", config.getBoolean("world-settings.default.anti-xray.ray-trace-third-person")
               );
               rayTraceDistance = Math.max(
                  config.getDouble(
                     "world-settings." + worldName + ".anti-xray.ray-trace-distance", config.getDouble("world-settings.default.anti-xray.ray-trace-distance")
                  ),
                  0.0
               );
               rehideBlocks = config.getBoolean(
                  "world-settings." + worldName + ".anti-xray.rehide-blocks", config.getBoolean("world-settings.default.anti-xray.rehide-blocks")
               );
               rehideDistance = Math.max(
                  config.getDouble("world-settings." + worldName + ".anti-xray.rehide-distance", config.getDouble("world-settings.default.anti-xray.rehide-distance")),
                  0.0
               );
               maxRayTraceBlockCountPerChunk = Math.max(
                  config.getInt(
                     "world-settings." + worldName + ".anti-xray.max-ray-trace-block-count-per-chunk",
                     config.getInt("world-settings.default.anti-xray.max-ray-trace-block-count-per-chunk")
                  ),
                  0
               );
               rayTraceBlocks = config.getList(
                     "world-settings." + worldName + ".anti-xray.ray-trace-blocks", config.getList("world-settings.default.anti-xray.ray-trace-blocks")
                  )
                  .stream()
                  .filter(entry -> Objects.nonNull(entry))
                  .map(entry -> String.valueOf(entry))
                  .collect(Collectors.toList());
            }

            this.plugin.logDebug("射线追踪第三人称视角设置: " + rayTraceThirdPerson);
            this.plugin.logDebug("射线追踪距离设置: " + rayTraceDistance);
            this.plugin.logDebug("重新隐藏方块设置: " + rehideBlocks);
            this.plugin.logDebug("重新隐藏距离设置: " + rehideDistance);
            this.plugin.logDebug("每个区块最大射线追踪方块数量: " + maxRayTraceBlockCountPerChunk);
            this.plugin.logDebug("射线追踪方块列表: " + rayTraceBlocks);
            ServerLevel serverLevel = this.getServerLevel(world);
            if (serverLevel == null) {
               this.plugin.logDebug("Failed to get world handle for: " + worldName);
               return;
            }

            this.plugin.logDebug("获取服务器世界实例完成");
            this.plugin.logDebug("开始创建ChunkPacketBlockControllerAntiXray实例");
            List effectiveRayTraceBlocks = rayTraceBlocks.isEmpty() ? null : rayTraceBlocks;
            Map fakeOreTypes = this.xrayDetector.getFakeOreTypes();
            ThreadPoolExecutor chunkObfuscationExecutor = this.plugin.getChunkObfuscationExecutor();
            EnhancedChunkObfuscationController enhancedChunkObfuscationController = new EnhancedChunkObfuscationController(
               this.plugin, rayTraceThirdPerson, rayTraceDistance, rehideBlocks, rehideDistance, maxRayTraceBlockCountPerChunk, effectiveRayTraceBlocks, serverLevel, fakeOreTypes, chunkObfuscationExecutor
            );
            this.plugin.logDebug("ChunkPacketBlockControllerAntiXray实例创建成功");

            try {
               this.plugin.logDebug("开始使用反射设置区块包控制器");
               Field controllerField = this.findChunkPacketBlockControllerField(serverLevel.getClass());
               if (controllerField == null) {
                  this.plugin.getLogger().warning("未找到 chunkPacketBlockController 字段，无法安装增强反X光控制器");
                  return;
               }

               controllerField.setAccessible(true);
               Object existingController = controllerField.get(serverLevel);
               if (existingController instanceof EnhancedChunkObfuscationController) {
                  this.plugin.getLogger().info("世界 " + worldName + " 已经安装了增强反X光控制器，跳过重复设置");
                  return;
               }

               if (!(existingController instanceof ChunkPacketBlockController)) {
                  Logger logger = this.plugin.getLogger();
                  String controllerTypeName = existingController == null ? "null" : existingController.getClass().getName();
                  logger.warning("chunkPacketBlockController 类型异常: " + controllerTypeName);
               } else {
                  this.plugin.logDebug("当前控制器类型: " + existingController.getClass().getName());
               }

               controllerField.set(serverLevel, enhancedChunkObfuscationController);
               this.plugin.getLogger().info("成功为世界 " + worldName + " 安装增强反X光控制器");
               if (rayTraceThirdPerson) {
                  NekoAntiXray.setRayTraceThirdPerson(true);
               }
            } catch (IllegalAccessException ex) {
               this.plugin.getLogger().severe("反射设置区块包控制器失败: " + ex.getMessage());
               throw new RuntimeException(ex);
            }

            this.plugin.logDebug("世界 " + worldName + " 的反X光系统初始化完成 (模式: " + (heavyModeActive ? "Heavy" : "Enhanced") + ")");
            return;
         }

         boolean pluginEnabled = this.plugin.isEnabled();
         boolean heavyModeEnabled = this.plugin.getConfigManager().isHeavyModeEnabled();
         boolean worldEnhancedEnabled = this.plugin.isWorldSettingsDefaultEnabled(world);
         this.plugin.logDebug("跳过反X光系统初始化 - 插件状态: " + pluginEnabled + ", 重型模式状态: " + heavyModeEnabled + ", Enhanced Anti-XRay状态: " + worldEnhancedEnabled);
      } catch (Exception ex) {
         this.plugin.getLogger().severe("Failed to initialize world " + world.getName() + ": " + ex.getMessage());
         ex.printStackTrace();
      }
   }

   private Field findChunkPacketBlockControllerField(Class targetClass) {
      Class<?> currentClass = targetClass;
      Field fallbackField = null;

      while (currentClass != null) {
         for (Field declaredField : currentClass.getDeclaredFields()) {
            if (declaredField.getType() == ChunkPacketBlockController.class) {
               return declaredField;
            }

            if (fallbackField == null && ChunkPacketBlockController.class.isAssignableFrom(declaredField.getType())) {
               fallbackField = declaredField;
            }
         }

         currentClass = currentClass.getSuperclass();
      }

      return fallbackField;
   }
}
