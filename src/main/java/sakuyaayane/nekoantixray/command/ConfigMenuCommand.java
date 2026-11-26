package sakuyaayane.nekoantixray.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import sakuyaayane.nekoantixray.NekoAntiXray;

public class ConfigMenuCommand implements CommandExecutor, Listener {
   private static final String MENU_TITLE_PREFIX = "§8§lNAX·配置";
   private static final String ADMIN_PERMISSION = "nekoantixray.admin";
   private static final int ITEMS_PER_PAGE = 45;
   private final NekoAntiXray plugin;
   private static final List<ConfigMenuCommand.ConfigModule> CONFIG_MODULES = Arrays.asList(
      new ConfigMenuCommand.ConfigModule(
         "§6反矿透核心",
         Arrays.asList(
            ConfigMenuCommand.ConfigSetting.boolSetting("check.xray.enable", "反X-Ray检测总开关", Material.DIAMOND_PICKAXE),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.xray.same-chunk-detection", "同区块连续挖掘检测", Material.OBSIDIAN),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.line-break.length", "断链长度(格)", 1.0, 64.0),
            ConfigMenuCommand.ConfigSetting.cycleSetting("check.xray.heavy-mode", "Heavy模式(数据包混淆)", Material.BEDROCK, new String[]{"false", "true", "dynamic"}),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.mining-count-threshold", "挖掘计数阈值", 1.0, 100.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.spawn.ore", "生成矿石数", 1.0, 128.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.spawn.time", "生成时间窗口", 1.0, 600.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.spawn.distance-limit.ore", "矿石距离上限", 1.0, 64.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.spawn.distance-limit.player", "玩家距离上限", 1.0, 64.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.spawn.x-change.min", "X偏移最小", -100.0, 100.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.spawn.x-change.max", "X偏移最大", -100.0, 100.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.spawn.z-change.min", "Z偏移最小", -100.0, 100.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.spawn.z-change.max", "Z偏移最大", -100.0, 100.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.spawn.y-change.min", "Y偏移最小", -100.0, 100.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.spawn.y-change.max", "Y偏移最大", -100.0, 100.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.spawn.amount.min", "生成数量最小", 1.0, 64.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.spawn.amount.max", "生成数量最大", 1.0, 64.0),
            ConfigMenuCommand.ConfigSetting.doubleSetting("check.xray.vl.add.break-self", "自挖违规值增量", 0.0, 100.0, 0.5),
            ConfigMenuCommand.ConfigSetting.doubleSetting("check.xray.vl.add.break-other", "他人挖矿违规值增量", 0.0, 100.0, 0.5),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.vl.continuous-increase.count", "连续违规计数", 1.0, 100.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.vl.continuous-increase.interval", "连续违规间隔(秒)", 1.0, 600.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.xray.mass-excavation.enabled", "大规模挖掘检测", Material.NETHERITE_PICKAXE),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.mass-excavation.detection-time-window", "检测时间窗口(秒)", 5.0, 600.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.mass-excavation.block-count-threshold", "方块数量阈值", 10.0, 1000.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.mass-excavation.min-volume-threshold", "最小体积阈值", 10.0, 10000.0),
            ConfigMenuCommand.ConfigSetting.doubleSetting("check.xray.mass-excavation.min-density-threshold", "最小密度阈值", 0.1, 1.0, 0.05),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.mass-excavation.min-horizontal-area-threshold", "最小水平面积阈值", 10.0, 10000.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.mass-excavation.bypass-duration", "豁免时长(秒)", 10.0, 3600.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.mass-excavation.confirmation-cooldown", "确认冷却(秒)", 5.0, 600.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.mass-excavation.denial-cooldown-duration", "否认冷却(秒)", 10.0, 3600.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.xray.mass-excavation.notification.enabled", "大规模挖掘通知", Material.BELL),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.mass-excavation.diversity-threshold", "方块多样性阈值", 2.0, 50.0, 1.0),
            ConfigMenuCommand.ConfigSetting.doubleSetting("check.xray.mass-excavation.cuboid-ratio", "立方体比例", 0.1, 1.0, 0.05),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.xray.mass-excavation.detect-building-patterns", "建筑模式检测", Material.STRUCTURE_BLOCK),
            ConfigMenuCommand.ConfigSetting.doubleSetting("check.xray.mass-excavation.speed-variance-threshold", "速度波动阈值", 0.1, 2.0, 0.05),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.mass-excavation.vertical-excavation.min-y-range", "垂直挖掘最小Y范围", 1.0, 64.0, 1.0),
            ConfigMenuCommand.ConfigSetting.doubleSetting("check.xray.mass-excavation.vertical-excavation.y-xz-ratio", "垂直Y/XZ比", 0.1, 2.0, 0.05),
            ConfigMenuCommand.ConfigSetting.doubleSetting("check.xray.mass-excavation.vertical-excavation.xz-density", "垂直XZ密度", 0.1, 1.0, 0.05),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.mass-excavation.terrain-modification.min-y-layers", "地形改造最小层数", 1.0, 16.0, 1.0),
            ConfigMenuCommand.ConfigSetting.doubleSetting("check.xray.mass-excavation.terrain-modification.layer-similarity", "层相似度", 0.1, 1.0, 0.05),
            ConfigMenuCommand.ConfigSetting.doubleSetting("check.xray.mass-excavation.terrain-modification.xz-stability", "XZ稳定性", 0.1, 1.0, 0.05),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.unexposed-mining.enabled", "未暴露方块统计检测", Material.OAK_LOG),
            ConfigMenuCommand.ConfigSetting.intSetting("check.unexposed-mining.window-size", "统计窗口大小", 10.0, 10000.0, 10.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.unexposed-mining.minimum-samples", "最小样本数", 10.0, 10000.0, 10.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.unexposed-mining.minimum-ores", "最小矿石数", 1.0, 1000.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.unexposed-mining.minimum-host-blocks", "最小宿主方块数", 10.0, 10000.0, 10.0),
            ConfigMenuCommand.ConfigSetting.doubleSetting("check.unexposed-mining.maximum-ore-ratio", "最大矿石比例", 0.01, 1.0, 0.01),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("notification-system.algo.threshold-commands.10", "VL10阈值命令"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("notification-system.algo.threshold-commands.20", "VL20阈值命令"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("notification-system.algo.threshold-commands.30", "VL30阈值命令"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("check.xray.mass-excavation.message-prefix", "大规模挖掘消息前缀"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("check.xray.mass-excavation.confirmation-message", "大规模挖掘确认消息"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("check.xray.mass-excavation.confirm-button", "确认按钮文本"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("check.xray.mass-excavation.deny-button", "否认按钮文本"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("check.xray.mass-excavation.notification.message", "大规模挖掘通知消息")
         )
      ),
      new ConfigMenuCommand.ConfigModule(
         "§9Freecam检测",
         Arrays.asList(
            ConfigMenuCommand.ConfigSetting.readOnlySetting("check.antifreecam.warning-message", "Freecam警告消息"),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.antifreecam.kick-enabled", "Freecam踢出开关", Material.IRON_BOOTS),
            ConfigMenuCommand.ConfigSetting.intSetting("check.antifreecam.max-warnings", "最大警告次数", 1.0, 20.0, 1.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.antifreecam.kick-after-max-warnings", "超次数踢出", Material.IRON_DOOR),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("check.antifreecam.kick-message", "Freecam踢出消息"),
            ConfigMenuCommand.ConfigSetting.doubleSetting("check.antifreecam.yaw-threshold", "Yaw阈值(度)", 1.0, 180.0, 1.0),
            ConfigMenuCommand.ConfigSetting.doubleSetting("check.antifreecam.pitch-threshold", "Pitch阈值(度)", 1.0, 90.0, 1.0)
         )
      ),
      new ConfigMenuCommand.ConfigModule(
         "§d射线追踪",
         Arrays.asList(
            ConfigMenuCommand.ConfigSetting.boolSetting("world-settings.default.enabled", "世界增强开关", Material.GRASS_BLOCK),
            ConfigMenuCommand.ConfigSetting.intSetting("world-settings.default.anti-xray.max-block-height", "混淆最高Y坐标", Material.STONE, 16.0, 1024.0, 16.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("world-settings.default.anti-xray.ray-trace-third-person", "第三人称射线追踪", Material.ENDER_EYE),
            ConfigMenuCommand.ConfigSetting.doubleSetting("world-settings.default.anti-xray.ray-trace-distance", "射线追踪距离", 1.0, 512.0, 1.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("world-settings.default.anti-xray.rehide-blocks", "破坏后重新隐藏", Material.STICKY_PISTON),
            ConfigMenuCommand.ConfigSetting.doubleSetting("world-settings.default.anti-xray.rehide-distance", "重新隐藏距离", 1.0, 512.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting(
               "world-settings.default.anti-xray.max-ray-trace-block-count-per-chunk", "每区块最大追踪方块数", Material.COMPARATOR, 1.0, 4096.0, 1.0
            ),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("world-settings.default.anti-xray.ray-trace-blocks", "射线追踪方块列表(空=Paper默认)"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("world-settings.default.ray-trace-threads", "射线追踪线程数(auto/数字)"),
            ConfigMenuCommand.ConfigSetting.boolSetting("world-settings.default.enable-priority-queue", "优先级队列", Material.HOPPER),
            ConfigMenuCommand.ConfigSetting.intSetting("world-settings.default.queue-capacity", "队列容量", 64.0, 8192.0, 64.0),
            ConfigMenuCommand.ConfigSetting.intSetting("world-settings.default.chunk-obfuscation-queue-capacity", "混淆队列容量", 64.0, 4096.0, 32.0),
            ConfigMenuCommand.ConfigSetting.intSetting("world-settings.default.chunk-obfuscation-max-threads", "混淆最大线程数", 1.0, 32.0, 1.0),
            ConfigMenuCommand.ConfigSetting.doubleSetting("check.xray.heavy-mode-trigger-vl", "Heavy触发VL(dynamic模式)", 0.0, 100.0, 0.5),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.xray.heavy-mode-resend-on-trigger", "触发时强制重发区块", Material.ENDER_PEARL),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.heavy-mode-resend-radius", "重发区块半径", Material.COMPASS, 1.0, 32.0, 1.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.xray.heavy-mode-obfuscate-fake-ores", "Heavy混淆插件假矿石", Material.GOLD_ORE)
         )
      ),
      new ConfigMenuCommand.ConfigModule(
         "§c鬼手探测防护",
         Arrays.asList(
            ConfigMenuCommand.ConfigSetting.boolSetting("check.xray.probe-guard.enabled", "鬼手探测拦截总开关", Material.SPYGLASS),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.xray.probe-guard.require-heavy-mode", "仅在Heavy模式生效", Material.BEDROCK),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.xray.probe-guard.only-obfuscated-players", "仅拦截已混淆玩家(dynamic)", Material.SCULK_SENSOR),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.xray.probe-guard.use-gamemode-reach", "按游戏模式/属性取手长", Material.IRON_PICKAXE),
            ConfigMenuCommand.ConfigSetting.doubleSetting("check.xray.probe-guard.max-reach", "固定手长(格)", Material.STICK, 1.0, 64.0, 0.5),
            ConfigMenuCommand.ConfigSetting.doubleSetting("check.xray.probe-guard.reach-tolerance", "手长容差(格)", Material.STRING, 0.0, 8.0, 0.1),
            ConfigMenuCommand.ConfigSetting.doubleSetting("check.xray.probe-guard.crosshair-tolerance", "准星碰撞箱容差(格)", Material.TARGET, 0.0, 4.0, 0.1),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.xray.probe-guard.check-crosshair", "校验准星命中", Material.TARGET),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.xray.probe-guard.check-line-of-sight", "校验视线遮挡(反隔墙摸矿)", Material.ENDER_EYE),
            ConfigMenuCommand.ConfigSetting.intSetting("check.xray.probe-guard.abort-grace-ms", "ABORT宽限(毫秒)", Material.CLOCK, 0.0, 10000.0, 100.0),
            ConfigMenuCommand.ConfigSetting.doubleSetting("check.xray.probe-guard.vl-per-cancel", "每次拦截增加VL", Material.REDSTONE, 0.0, 100.0, 0.5),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.xray.probe-guard.debug-log", "拦截调试日志", Material.REDSTONE_TORCH)
         )
      ),
      new ConfigMenuCommand.ConfigModule(
         "§e假矿诱饵",
         Arrays.asList(
            ConfigMenuCommand.ConfigSetting.boolSetting("fake-ore-decoy-throw.enabled", "抛投诱饵矿石", Material.FISHING_ROD),
            ConfigMenuCommand.ConfigSetting.intSetting("fake-ore-decoy-throw.max-distance", "抛投最远距离", Material.ARROW, 1.0, 128.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("fake-ore-decoy-throw.min-distance", "抛投最近距离", Material.FEATHER, 0.0, 128.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("fake-ore-decoy-throw.behind-wall-layers", "穿透实心方块层数", Material.BRICKS, 0.0, 16.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("fake-ore-generation-cooldown", "假矿石生成冷却(秒)", Material.CLOCK, 0.0, 3600.0, 5.0)
         )
      ),
      new ConfigMenuCommand.ConfigModule("§2随机假矿石", buildRandomOreSettings()),
      new ConfigMenuCommand.ConfigModule(
         "§3挖矿行为分析",
         Arrays.asList(
            ConfigMenuCommand.ConfigSetting.boolSetting("check.mining-behavior-analyzer.active", "行为分析总开关", Material.COMPASS),
            ConfigMenuCommand.ConfigSetting.intSetting("check.mining-behavior-analyzer.data-collection.path-max-blocks", "路径最大方块数", 50.0, 5000.0, 50.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.mining-behavior-analyzer.data-collection.cleanup-after-minutes", "数据清理间隔(分)", 1.0, 120.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.mining-behavior-analyzer.data-collection.vein-cluster.distance", "矿脉聚类距离", 1.0, 32.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.mining-behavior-analyzer.data-collection.vein-cluster.small-size", "小型矿脉大小", 1.0, 32.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.mining-behavior-analyzer.data-collection.vein-cluster.trigger-count", "触发计数", 1.0, 32.0, 1.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.mining-behavior-analyzer.data-collection.explosion-tracking.enabled", "爆炸追踪", Material.TNT),
            ConfigMenuCommand.ConfigSetting.intSetting(
               "check.mining-behavior-analyzer.data-collection.explosion-tracking.retention-seconds", "爆炸记录时长(秒)", 10.0, 3600.0, 10.0
            ),
            ConfigMenuCommand.ConfigSetting.doubleSetting(
               "check.mining-behavior-analyzer.data-collection.explosion-tracking.suspicious-rate", "可疑命中率", 0.01, 1.0, 0.05
            ),
            ConfigMenuCommand.ConfigSetting.doubleSetting(
               "check.mining-behavior-analyzer.data-collection.explosion-tracking.penalty-multiplier", "可疑VL倍增", 1.0, 10.0, 0.5
            ),
            ConfigMenuCommand.ConfigSetting.intSetting("check.mining-behavior-analyzer.pattern-analyzer.turning-limit", "转向次数上限", 1.0, 100.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.mining-behavior-analyzer.pattern-analyzer.branching-limit", "分支次数上限", 1.0, 100.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.mining-behavior-analyzer.pattern-analyzer.y-shift-limit", "Y轴改变次数上限", 1.0, 100.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.mining-behavior-analyzer.pattern-analyzer.y-shift-min", "Y变化判定距离", 1.0, 32.0, 1.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.mining-behavior-analyzer.environment-filters.apply-filters", "环境过滤器", Material.WATER_BUCKET),
            ConfigMenuCommand.ConfigSetting.intSetting("check.mining-behavior-analyzer.environment-filters.cave.air-count", "洞穴空气数", 1.0, 64.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.mining-behavior-analyzer.environment-filters.cave.cave-air-multiplier", "洞穴空气倍率", 1.0, 32.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.mining-behavior-analyzer.environment-filters.cave.radius", "洞穴检测半径", 1.0, 16.0, 1.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.mining-behavior-analyzer.environment-filters.cave.reduce-vl", "洞穴降低VL", Material.SCULK_VEIN),
            ConfigMenuCommand.ConfigSetting.boolSetting(
               "check.mining-behavior-analyzer.environment-filters.cave.fake-cave-detection.enabled", "假洞穴检测", Material.SCULK_SENSOR
            ),
            ConfigMenuCommand.ConfigSetting.intSetting(
               "check.mining-behavior-analyzer.environment-filters.cave.fake-cave-detection.min-blocks", "假洞穴最小方块数", 1.0, 64.0, 1.0
            ),
            ConfigMenuCommand.ConfigSetting.doubleSetting(
               "check.mining-behavior-analyzer.environment-filters.cave.fake-cave-detection.air-ratio", "假洞穴空气比例", 0.05, 1.0, 0.05
            ),
            ConfigMenuCommand.ConfigSetting.doubleSetting(
               "check.mining-behavior-analyzer.environment-filters.cave.fake-cave-detection.vl-per-trigger", "每次触发VL", 0.1, 10.0, 0.5
            ),
            ConfigMenuCommand.ConfigSetting.intSetting(
               "check.mining-behavior-analyzer.environment-filters.cave.fake-cave-detection.max-triggers", "最大触发次数", 1.0, 100.0, 1.0
            ),
            ConfigMenuCommand.ConfigSetting.intSetting(
               "check.mining-behavior-analyzer.environment-filters.cave.fake-cave-detection.reset-minutes", "重置间隔(分)", 1.0, 120.0, 1.0
            ),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.mining-behavior-analyzer.environment-filters.water.include-flowing", "水流包含", Material.WATER),
            ConfigMenuCommand.ConfigSetting.intSetting("check.mining-behavior-analyzer.environment-filters.water.water-count", "水域数量阈值", 1.0, 64.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.mining-behavior-analyzer.environment-filters.water.radius", "水域检测半径", 1.0, 16.0, 1.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.mining-behavior-analyzer.environment-filters.water.reduce-vl", "水域降低VL", Material.KELP),
            ConfigMenuCommand.ConfigSetting.intSetting("check.mining-behavior-analyzer.environment-filters.lava.lava-count", "熔岩数量阈值", 1.0, 64.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("check.mining-behavior-analyzer.environment-filters.lava.radius", "熔岩检测半径", 1.0, 16.0, 1.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.mining-behavior-analyzer.environment-filters.lava.reduce-vl", "熔岩降低VL", Material.MAGMA_BLOCK),
            ConfigMenuCommand.ConfigSetting.boolSetting(
               "check.mining-behavior-analyzer.violation-system.auto-decay.enabled", "违规自动衰减", Material.DAYLIGHT_DETECTOR
            ),
            ConfigMenuCommand.ConfigSetting.intSetting(
               "check.mining-behavior-analyzer.violation-system.auto-decay.interval-minutes", "衰减间隔(分)", 1.0, 120.0, 1.0
            ),
            ConfigMenuCommand.ConfigSetting.intSetting("check.mining-behavior-analyzer.violation-system.auto-decay.amount", "衰减量", 1.0, 100.0, 1.0),
            ConfigMenuCommand.ConfigSetting.boolSetting(
               "check.mining-behavior-analyzer.violation-system.exponential-decay.enabled", "指数衰减", Material.EXPERIENCE_BOTTLE
            ),
            ConfigMenuCommand.ConfigSetting.doubleSetting("check.mining-behavior-analyzer.violation-system.exponential-decay.factor", "指数衰减系数", 0.5, 0.99, 0.01),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.mining-behavior-analyzer.world-configs._default.enabled", "默认世界启用", Material.GRASS_BLOCK),
            ConfigMenuCommand.ConfigSetting.intSetting("check.mining-behavior-analyzer.world-configs._default.y-limit", "默认世界Y上限", -1.0, 1024.0, 1.0),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("check.mining-behavior-analyzer.target-blocks", "目标矿石列表(列表项)"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("check.mining-behavior-analyzer.world-configs.world.enabled", "世界world启用"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("check.mining-behavior-analyzer.world-configs.world.y-limit", "世界world Y上限"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("check.mining-behavior-analyzer.world-configs.world_nether.enabled", "世界world_nether启用"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("check.mining-behavior-analyzer.world-configs.world_nether.y-limit", "世界world_nether Y上限"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("check.mining-behavior-analyzer.world-configs.world_the_end.enabled", "世界world_the_end启用"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("check.mining-behavior-analyzer.world-configs.world_the_end.y-limit", "世界world_the_end Y上限")
         )
      ),
      new ConfigMenuCommand.ConfigModule(
         "§5种子防护",
         Arrays.asList(
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.enable", "防种子破解总开关", Material.END_CRYSTAL),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.modifyHashedSeed.enable", "哈希种子修改", Material.NAME_TAG),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.modifyEndStructures.enable", "末地结构修改", Material.END_PORTAL_FRAME),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.modifyEndStructures.endSpikes.enable", "末地尖刺", Material.END_ROD),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.modifyEndStructures.endCities.enable", "末地城", Material.PURPUR_BLOCK),
            ConfigMenuCommand.ConfigSetting.cycleSetting(
               "antiSeedCracker.modifyEndStructures.endSpikes.mode", "末地尖刺模式", Material.END_ROD, new String[]{"swap", "off"}
            ),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.biomePacketObfuscation.enabled", "生物群系数据包混淆", Material.OAK_LEAVES),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.biomePacketObfuscation.overworldOnly", "仅主世界", Material.SUNFLOWER),
            ConfigMenuCommand.ConfigSetting.cycleSetting(
               "antiSeedCracker.biomePacketObfuscation.mode", "生物群系混淆模式", Material.OAK_LEAVES, new String[]{"moderate", "light", "aggressive", "off"}
            ),
            ConfigMenuCommand.ConfigSetting.intSetting("antiSeedCracker.biomePacketObfuscation.cellsPerSection", "每区块单元格数", 1.0, 16.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("antiSeedCracker.biomePacketObfuscation.minDistanceChunks", "最小距离区块数", 0.0, 64.0, 1.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.biomePacketObfuscation.perPlayerSalt", "每玩家盐值", Material.SUGAR),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.structureSeedProtection.enabled", "结构种子保护", Material.STRUCTURE_VOID),
            ConfigMenuCommand.ConfigSetting.cycleSetting(
               "antiSeedCracker.structureSeedProtection.strategy", "结构保护策略", Material.STRUCTURE_VOID, new String[]{"minimal", "maximal"}
            ),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.structureSeedProtection.structures.desertPyramid", "沙漠神殿", Material.SANDSTONE),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.structureSeedProtection.structures.jungleTemple", "丛林神庙", Material.JUNGLE_LEAVES),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.structureSeedProtection.structures.swampHut", "沼泽小屋", Material.OAK_STAIRS),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.structureSeedProtection.structures.igloo", "雪屋", Material.SNOW_BLOCK),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.structureSeedProtection.structures.pillagerOutpost", "掠夺者前哨站", Material.CROSSBOW),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.structureSeedProtection.structures.shipwreck", "沉船", Material.OAK_BOAT),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.decoratorPacketObfuscation.enabled", "装饰器数据包混淆", Material.DECORATED_POT),
            ConfigMenuCommand.ConfigSetting.cycleSetting(
               "antiSeedCracker.decoratorPacketObfuscation.mode", "装饰器混淆模式", Material.DECORATED_POT, new String[]{"balanced", "strict", "off"}
            ),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.decoratorPacketObfuscation.dungeons.enabled", "地牢", Material.SPAWNER),
            ConfigMenuCommand.ConfigSetting.boolSetting(
               "antiSeedCracker.decoratorPacketObfuscation.dungeons.stripSpawnerBlockEntity", "移除刷怪笼实体", Material.SKELETON_SKULL
            ),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.decoratorPacketObfuscation.dungeons.breakFloorCount", "破坏地板计数", Material.COBBLESTONE),
            ConfigMenuCommand.ConfigSetting.intSetting("antiSeedCracker.decoratorPacketObfuscation.dungeons.floorTargetCount", "地板目标数", 1.0, 64.0, 1.0),
            ConfigMenuCommand.ConfigSetting.boolSetting(
               "antiSeedCracker.decoratorPacketObfuscation.dungeons.replaceSpawnerBlockInStrictMode", "严格模式替换刷怪笼", Material.CRACKED_STONE_BRICKS
            ),
            ConfigMenuCommand.ConfigSetting.boolSetting(
               "antiSeedCracker.decoratorPacketObfuscation.dungeons.perturbFloorInStrictMode", "严格模式扰动地板", Material.MOSSY_COBBLESTONE
            ),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.decoratorPacketObfuscation.structures.buriedTreasure", "埋藏宝藏", Material.CHEST),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.decoratorPacketObfuscation.structures.shipwreck", "沉船(严格)", Material.OAK_PLANKS),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.decoratorPacketObfuscation.structures.monument", "海底神殿(严格)", Material.PRISMARINE),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.decoratorPacketObfuscation.endGateway.enabled", "末地折跃门", Material.END_CRYSTAL),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.decoratorPacketObfuscation.emeraldOre.enabled", "绿宝石矿隐藏", Material.EMERALD_ORE),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.decoratorPacketObfuscation.desertWell.enabled", "沙漠水井", Material.SAND),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.decoratorPacketObfuscation.warpedFungus.enabled", "诡异菌", Material.WARPED_FUNGUS),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.seedcrackerXFirewall.enabled", "SeedcrackerX防火墙", Material.SHIELD),
            ConfigMenuCommand.ConfigSetting.intSetting("antiSeedCracker.seedcrackerXFirewall.shadowState.ttlSeconds", "伪装TTL(秒)", 30.0, 3600.0, 30.0),
            ConfigMenuCommand.ConfigSetting.intSetting(
               "antiSeedCracker.seedcrackerXFirewall.shadowState.maxPositionsPerPlayer", "每玩家伪装坐标上限", 1000.0, 1000000.0, 1000.0
            ),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.seedcrackerXFirewall.rewriteBlockUpdates", "重写方块更新", Material.PISTON),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.seedcrackerXFirewall.rewriteBlockEntityUpdates", "重写方块实体更新", Material.STICKY_PISTON),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.seedcrackerXFirewall.interceptPlayerActions", "拦截玩家动作", Material.SPYGLASS),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.seedcrackerXFirewall.interceptStartStopDigging", "拦截挖掘动作", Material.DIAMOND_SHOVEL),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.fabricModDetection.enabled", "Fabric模组检测", Material.ELYTRA),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.fabricModDetection.debug", "Fabric检测调试", Material.DEBUG_STICK),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.fabricModDetection.kickOnFabricBrand", "拒绝Fabric品牌", Material.BARRIER),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.netherBedrockProtection.enabled", "下界基岩种子混淆", Material.BEDROCK),
            ConfigMenuCommand.ConfigSetting.boolSetting("antiSeedCracker.netherBedrockProtection.blockRoofAccess", "封锁基岩上层", Material.NETHERITE_BLOCK),
            ConfigMenuCommand.ConfigSetting.intSetting("antiSeedCracker.netherBedrockProtection.roofAccessMaxY", "基岩上层最大Y", 0.0, 320.0, 1.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("secure-seed.biome-source-xor-enabled", "生物群系坐标混淆", Material.PODZOL),
            ConfigMenuCommand.ConfigSetting.boolSetting("secure-seed.structure-placement-xor-enabled", "结构落点XOR", Material.ANVIL),
            ConfigMenuCommand.ConfigSetting.boolSetting("secure-seed.world-generation-hook-enabled", "世界生成钩子", Material.STRUCTURE_BLOCK),
            ConfigMenuCommand.ConfigSetting.boolSetting("secure-seed.blake2-hashing-enabled", "BLAKE2哈希增强", Material.BLAZE_POWDER),
            ConfigMenuCommand.ConfigSetting.boolSetting("secure-seed.async-chunk-protection", "异步区块保护", Material.CLOCK),
            ConfigMenuCommand.ConfigSetting.intSetting("secure-seed.hash-cache-size", "哈希缓存大小", 256.0, 65536.0, 256.0),
            ConfigMenuCommand.ConfigSetting.intSetting("secure-seed.position-cache-size", "位置缓存大小", 512.0, 131072.0, 512.0),
            ConfigMenuCommand.ConfigSetting.intSetting("secure-seed.random-pool-size", "随机数池大小", 4.0, 512.0, 4.0),
            ConfigMenuCommand.ConfigSetting.intSetting("secure-seed.max-queue-size", "队列最大长度", 64.0, 8192.0, 64.0),
            ConfigMenuCommand.ConfigSetting.intSetting("secure-seed.batch-size", "每tick处理区块数", 1.0, 128.0, 1.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("fake-seed.enable-fake-seed", "假种子", Material.WHEAT_SEEDS),
            ConfigMenuCommand.ConfigSetting.boolSetting("fake-seed.show-real-seed-to-op", "OP显示真实种子", Material.DIAMOND)
         )
      ),
      new ConfigMenuCommand.ConfigModule(
         "§a违规与封禁",
         Arrays.asList(
            ConfigMenuCommand.ConfigSetting.intSetting("notification-system.ban-messages.ban-duration", "封禁时长(天, -1=永久)", Material.IRON_DOOR, -1.0, 3650.0, 1.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("notification-system.ban-messages.show-location-in-console", "控制台显示位置", Material.MAP),
            ConfigMenuCommand.ConfigSetting.intSetting("violation-cooldown", "违规冷却(秒)", 0.0, 600.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("violation-decrease-interval", "违规降低间隔(秒)", 30.0, 3600.0, 30.0),
            ConfigMenuCommand.ConfigSetting.doubleSetting("violation-decrease-amount", "每次降低违规值", 0.1, 100.0, 0.5),
            ConfigMenuCommand.ConfigSetting.boolSetting("vertical-distribution.enabled", "垂直分布检测", Material.LADDER),
            ConfigMenuCommand.ConfigSetting.intSetting("vertical-distribution.range", "垂直分布范围", 1.0, 64.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("vertical-distribution.min-difference", "最小差异", 1.0, 32.0, 1.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("vertical-distribution.debug", "垂直分布调试", Material.DEBUG_STICK),
            ConfigMenuCommand.ConfigSetting.boolSetting("announcement-system.enabled", "公告系统", Material.NOTE_BLOCK),
            ConfigMenuCommand.ConfigSetting.intSetting("announcement-system.interval", "公告间隔(秒)", 30.0, 3600.0, 30.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("announcement-system.gradient-enabled", "渐变颜色", Material.MAGENTA_DYE),
            ConfigMenuCommand.ConfigSetting.boolSetting("announcement-system.show-in-chat", "聊天显示", Material.WRITABLE_BOOK),
            ConfigMenuCommand.ConfigSetting.boolSetting("announcement-system.show-as-actionbar", "动作栏显示", Material.GOLD_INGOT),
            ConfigMenuCommand.ConfigSetting.boolSetting("announcement-system.show-as-title", "标题显示", Material.OAK_SIGN),
            ConfigMenuCommand.ConfigSetting.intSetting("announcement-system.title.fade-in", "标题淡入(tick)", 0.0, 100.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("announcement-system.title.stay", "标题停留(tick)", 5.0, 600.0, 5.0),
            ConfigMenuCommand.ConfigSetting.intSetting("announcement-system.title.fade-out", "标题淡出(tick)", 0.0, 100.0, 1.0)
         )
      ),
      new ConfigMenuCommand.ConfigModule(
         "§b通知系统",
         Arrays.asList(
            ConfigMenuCommand.ConfigSetting.boolSetting("notification-system.enabled", "统一通知总开关", Material.BELL),
            ConfigMenuCommand.ConfigSetting.intSetting("notification-system.throttle-ms", "通知节流(毫秒)", Material.CLOCK, 0.0, 60000.0, 500.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("notification-system.offline-report.enabled", "OP离线报告", Material.WRITABLE_BOOK),
            ConfigMenuCommand.ConfigSetting.doubleSetting("notification-system.xray.notify.threshold", "XRay-通知阈值(VL)", 0.0, 1000.0, 0.5),
            ConfigMenuCommand.ConfigSetting.boolSetting("notification-system.xray.notify.notify-admins", "XRay-通知管理员", Material.SKELETON_SKULL),
            ConfigMenuCommand.ConfigSetting.boolSetting("notification-system.xray.notify.notify-player", "XRay-通知玩家本人", Material.SKELETON_SKULL),
            ConfigMenuCommand.ConfigSetting.boolSetting("notification-system.xray.notify.broadcast", "XRay-通知广播", Material.BELL),
            ConfigMenuCommand.ConfigSetting.doubleSetting("notification-system.xray.warn.threshold", "XRay-警告阈值(VL)", 0.0, 1000.0, 0.5),
            ConfigMenuCommand.ConfigSetting.boolSetting("notification-system.xray.warn.notify-admins", "XRay-警告管理员", Material.SKELETON_SKULL),
            ConfigMenuCommand.ConfigSetting.boolSetting("notification-system.xray.warn.notify-player", "XRay-警告玩家本人", Material.SKELETON_SKULL),
            ConfigMenuCommand.ConfigSetting.boolSetting("notification-system.xray.warn.broadcast", "XRay-警告广播", Material.BELL),
            ConfigMenuCommand.ConfigSetting.doubleSetting("notification-system.xray.alert.threshold", "XRay-严重阈值(VL)", 0.0, 1000.0, 0.5),
            ConfigMenuCommand.ConfigSetting.boolSetting("notification-system.xray.alert.notify-admins", "XRay-严重管理员", Material.SKELETON_SKULL),
            ConfigMenuCommand.ConfigSetting.boolSetting("notification-system.xray.alert.notify-player", "XRay-严重玩家本人", Material.SKELETON_SKULL),
            ConfigMenuCommand.ConfigSetting.boolSetting("notification-system.xray.alert.broadcast", "XRay-严重广播", Material.BELL),
            ConfigMenuCommand.ConfigSetting.boolSetting("notification-system.xray.ban.enabled", "XRay-自动封禁", Material.IRON_BARS),
            ConfigMenuCommand.ConfigSetting.doubleSetting("notification-system.xray.ban.threshold", "XRay-封禁阈值(VL)", 0.0, 1000.0, 0.5),
            ConfigMenuCommand.ConfigSetting.boolSetting("notification-system.xray.ban.broadcast", "XRay-封禁公告", Material.BELL),
            ConfigMenuCommand.ConfigSetting.boolSetting("notification-system.xray.ban.fallback-to-builtin-on-failure", "外部封禁失败时兜底", Material.BARRIER)
         )
      ),
      new ConfigMenuCommand.ConfigModule(
         "§7系统与调试",
         Arrays.asList(
            ConfigMenuCommand.ConfigSetting.boolSetting("debug", "调试日志", Material.REDSTONE_TORCH),
            ConfigMenuCommand.ConfigSetting.boolSetting("replay.enabled", "回放系统", Material.RECOVERY_COMPASS),
            ConfigMenuCommand.ConfigSetting.intSetting("replay.record-duration", "回放录制时长(秒)", 10.0, 3600.0, 10.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("ban-inventory-clean.enabled", "封禁背包清理", Material.CHEST_MINECART),
            ConfigMenuCommand.ConfigSetting.boolSetting("ore-stats.enabled", "矿石统计", Material.RAW_IRON),
            ConfigMenuCommand.ConfigSetting.boolSetting("ore-stats.track-all-ores", "追踪全部矿石", Material.RAW_GOLD),
            ConfigMenuCommand.ConfigSetting.boolSetting("ban-statistics.enabled", "封禁统计", Material.BOOKSHELF),
            ConfigMenuCommand.ConfigSetting.boolSetting("ban-statistics.record-details", "封禁统计记录详情", Material.WRITABLE_BOOK),
            ConfigMenuCommand.ConfigSetting.intSetting("ban-statistics.reset-period", "统计重置周期(-1=不重置)", -1.0, 365.0, 1.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("deeplearn.enable", "深度学习检测", Material.COPPER_BLOCK),
            ConfigMenuCommand.ConfigSetting.boolSetting("deeplearn.debug", "深度学习调试", Material.DEBUG_STICK),
            ConfigMenuCommand.ConfigSetting.boolSetting("deeplearn.violations.enable-punishment", "深度学习处罚", Material.IRON_AXE),
            ConfigMenuCommand.ConfigSetting.doubleSetting("deeplearn.violations.vl-multiplier", "VL倍率", 0.1, 100.0, 0.5),
            ConfigMenuCommand.ConfigSetting.intSetting("deeplearn.violations.min-vl-increase", "最小VL增量", 1.0, 100.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("deeplearn.violations.max-vl-increase", "最大VL增量", 1.0, 100.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("deeplearn.sequence_length", "序列长度", 5.0, 500.0, 5.0),
            ConfigMenuCommand.ConfigSetting.doubleSetting("deeplearn.prediction_threshold", "预测阈值", 0.1, 0.99, 0.01),
            ConfigMenuCommand.ConfigSetting.intSetting("deeplearn.cooldown_ms", "冷却(毫秒)", 100.0, 60000.0, 100.0),
            ConfigMenuCommand.ConfigSetting.intSetting("deeplearn.mining_time_window_ms", "挖掘时间窗(毫秒)", 1000.0, 3600000.0, 1000.0),
            ConfigMenuCommand.ConfigSetting.doubleSetting("deeplearn.vein_detection.max_distance", "矿脉最大距离", 0.5, 64.0, 0.5),
            ConfigMenuCommand.ConfigSetting.intSetting("deeplearn.vein_detection.max_time_ms", "矿脉最大时间(毫秒)", 100.0, 60000.0, 100.0),
            ConfigMenuCommand.ConfigSetting.intSetting("deeplearn.vein_detection.max_consecutive_skips", "最大连续跳过", 1.0, 100.0, 1.0),
            ConfigMenuCommand.ConfigSetting.doubleSetting("deeplearn.vein_detection.max_vertical_distance", "矿脉最大垂直距离", 0.5, 64.0, 0.5),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.antiBrightness.enable", "亮度检测", Material.TORCH),
            ConfigMenuCommand.ConfigSetting.intSetting("check.antiBrightness.lightThreshold", "亮度阈值", 0.0, 15.0, 1.0),
            ConfigMenuCommand.ConfigSetting.boolSetting("check.antiBrightness.useBlacklistedOresAsFallback", "黑名单矿石兜底", Material.REDSTONE_ORE),
            ConfigMenuCommand.ConfigSetting.intSetting("fawe-compatibility.refresh-delay-ticks", "FAWE刷新延迟(tick)", 0.0, 200.0, 1.0),
            ConfigMenuCommand.ConfigSetting.intSetting("fawe-compatibility.refresh-cooldown-ms", "FAWE刷新冷却(毫秒)", 0.0, 60000.0, 100.0),
            ConfigMenuCommand.ConfigSetting.intSetting("fawe-compatibility.max-chunks-per-cycle", "FAWE每周期区块数", 1.0, 1000.0, 1.0),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("notification-system.ban-messages.ban-reason", "封禁原因模板"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("notification-system.xray.ban.message", "封禁公告消息"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("notification-system.xray.ban.detail", "封禁公告详情"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("announcement-system.message", "公告消息"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("ore-stats.tracking-notification", "矿石追踪提示格式"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("fake-seed.messages.seed-message", "假种子消息"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("fake-seed.messages.no-permission", "无权限种子消息"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("check.antiBrightness.message", "亮度检测消息"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("antiSeedCracker.fabricModDetection.kickMessage", "Fabric检测踢出消息"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("secure-seed.world-seed-xor", "世界种子XOR(大数, 谨慎修改)"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("secure-seed.biome-coordinate-xor", "生物群系XOR(大数)"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("secure-seed.bedrock-seed-xor", "基岩种子XOR(空=随机)"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("antiSeedCracker.modifyEndStructures.worlds", "末地结构修改世界列表"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("antiSeedCracker.biomePacketObfuscation.salt", "生物群系盐值(0=随机)"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("check.antiBrightness.enabledWorlds", "亮度检测启用世界"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("blacklist.ores", "黑名单矿石列表"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("blacklist.worlds", "黑名单世界列表"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("notification-system.ban-messages.custom-ban-message", "自定义封禁消息"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("ban-statistics.file-path", "封禁统计文件路径"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("announcement-system.gradient-start", "公告渐变起始色"),
            ConfigMenuCommand.ConfigSetting.readOnlySetting("announcement-system.gradient-end", "公告渐变结束色")
         )
      )
   );

   public ConfigMenuCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender instanceof Player player) {
         if (!player.hasPermission("nekoantixray.admin")) {
            player.sendMessage("§c你没有权限执行此命令。");
            return true;
         } else {
            this.openMainMenu(player);
            return true;
         }
      } else {
         sender.sendMessage("§c只有玩家可以打开配置界面。");
         return true;
      }
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getInventory().getHolder() instanceof ConfigMenuCommand.MenuHolder) {
         event.setCancelled(true);
         if (event.getWhoClicked() instanceof Player) {
            Player whoClicked = (Player)event.getWhoClicked();
            if (!whoClicked.hasPermission("nekoantixray.admin")) {
               whoClicked.closeInventory();
               whoClicked.sendMessage("§c你没有权限执行此操作。");
            } else {
               ConfigMenuCommand.MenuHolder holder = (ConfigMenuCommand.MenuHolder)event.getInventory().getHolder();
               ItemStack currentItem = event.getCurrentItem();
               if (currentItem != null && currentItem.getType() != Material.AIR) {
                  if (!holder.mainMenu) {
                     ConfigMenuCommand.ConfigModule configModule = holder.module;
                     int currentPage = holder.page;
                     int clickedSlot = event.getSlot();
                     if (clickedSlot == 45) {
                        this.openMainMenu(whoClicked);
                     } else if (clickedSlot == 48) {
                        this.openModuleMenu(whoClicked, configModule, currentPage - 1);
                     } else if (clickedSlot == 50) {
                        this.openModuleMenu(whoClicked, configModule, currentPage + 1);
                     } else if (clickedSlot == 53) {
                        whoClicked.closeInventory();
                     } else if (clickedSlot >= 0 && clickedSlot < 45) {
                        int settingIndex = currentPage * 45 + clickedSlot;
                        if (settingIndex < configModule.settings.size()) {
                           ConfigMenuCommand.ConfigSetting configSetting = configModule.settings.get(settingIndex);
                           boolean leftClick = event.isLeftClick();
                           boolean shiftClick = event.isShiftClick();
                           boolean rightClick = event.isRightClick();
                           this.applyChange(whoClicked, configSetting, leftClick, shiftClick);
                           this.openModuleMenu(whoClicked, configModule, currentPage);
                        }
                     }
                  } else {
                     int mainMenuSlot = event.getSlot();
                     if (mainMenuSlot >= 0 && mainMenuSlot < CONFIG_MODULES.size()) {
                        this.openModuleMenu(whoClicked, CONFIG_MODULES.get(mainMenuSlot), 0);
                     } else if (mainMenuSlot == 17) {
                        whoClicked.closeInventory();
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean matchesOreNameIgnoreCase(String targetOreName, String candidateOreName) {
      return candidateOreName.equalsIgnoreCase(targetOreName);
   }

   private void openModuleMenu(Player player, ConfigMenuCommand.ConfigModule configModule, int requestedPage) {
      int maxPage = Math.max(0, (configModule.settings.size() - 1) / 45);
      int clampedPage = Math.max(0, Math.min(requestedPage, maxPage));
      ConfigMenuCommand.MenuHolder menuHolder = new ConfigMenuCommand.MenuHolder(false, configModule, clampedPage);
      int displayPage = clampedPage + 1;
      int totalPages = maxPage + 1;
      String moduleTitle = configModule.title;
      Inventory inventory = Bukkit.createInventory(menuHolder, 54, "§8§lNAX·配置 " + moduleTitle + " §8(" + displayPage + "/" + totalPages + ")");
      int startIndex = clampedPage * 45;
      int slotIndex = 0;

      for (int i = startIndex; i < configModule.settings.size() && slotIndex < 45; i++) {
         inventory.setItem(slotIndex++, this.createSettingItem(configModule.settings.get(i)));
      }

      inventory.setItem(45, this.createBackButton());
      if (clampedPage > 0) {
         inventory.setItem(48, this.createNavigationButton("§e上一页", Material.ARROW));
      }

      if (clampedPage < maxPage) {
         inventory.setItem(50, this.createNavigationButton("§e下一页", Material.ARROW));
      }

      inventory.setItem(53, this.createCloseButton());
      player.openInventory(inventory);
   }

   private ItemStack createModuleIcon(ConfigMenuCommand.ConfigModule configModule) {
      Material material = Material.BOOK;
      if (!material.isItem()) {
         material = Material.PAPER;
      }

      if (configModule.title.contains("反矿透")) {
         material = Material.BEDROCK;
      } else if (configModule.title.contains("射线")) {
         material = Material.ENDER_EYE;
      } else if (configModule.title.contains("鬼手")) {
         material = Material.SPYGLASS;
      } else if (configModule.title.contains("诱饵")) {
         material = Material.FISHING_ROD;
      } else if (configModule.title.contains("随机")) {
         material = Material.EMERALD;
      } else if (configModule.title.contains("行为")) {
         material = Material.COMPASS;
      } else if (configModule.title.contains("种子")) {
         material = Material.END_CRYSTAL;
      } else if (configModule.title.contains("封禁")) {
         material = Material.IRON_BARS;
      } else if (configModule.title.contains("调试")) {
         material = Material.REDSTONE_TORCH;
      }

      ItemStack item = new ItemStack(material);
      ItemMeta itemMeta = item.getItemMeta();
      itemMeta.setDisplayName("§f" + configModule.title);
      itemMeta.setLore(Arrays.asList("§7共 " + configModule.settings.size() + " 项配置", "", "§e左键点击进入"));
      item.setItemMeta(itemMeta);
      return item;
   }

   private ItemStack createSettingItem(ConfigMenuCommand.ConfigSetting configSetting) {
      Material material = configSetting.icon.isItem() ? configSetting.icon : Material.PAPER;
      ItemStack item = new ItemStack(material);
      ItemMeta itemMeta = item.getItemMeta();
      itemMeta.setDisplayName("§f" + configSetting.name);
      List<String> lore = new ArrayList<>();
      lore.add("");
      Object rawConfigValue = this.plugin.getConfig().get(configSetting.path);
      switch (configSetting.type) {
         case BOOL:
            boolean enabled = this.plugin.getConfig().getBoolean(configSetting.path, false);
            lore.add("§7当前值: " + (enabled ? "§a✔ 开启" : "§c✘ 关闭"));
            lore.add("");
            lore.add("§e点击切换");
            break;
         case INT:
         case DOUBLE:
            double numericConfigValue = this.getNumericConfigValue(configSetting.path, configSetting.min);
            lore.add("§7当前值: §b" + formatNumberValue(numericConfigValue));
            lore.add("");
            lore.add("§e左键 +" + formatStepValue(configSetting.step) + "  §c右键 -" + formatStepValue(configSetting.step));
            lore.add("§eShift+左键 +" + formatStepValue(configSetting.step * 10.0) + "  §cShift+右键 -" + formatStepValue(configSetting.step * 10.0));
            lore.add("§7范围: " + formatNumberValue(configSetting.min) + " ~ " + formatNumberValue(configSetting.max));
            break;
         case READONLY:
            lore.add("§7当前值: §b" + (rawConfigValue == null ? "未设置" : String.valueOf(rawConfigValue)));
            lore.add("");
            lore.add("§7字符串/列表项请在 config.yml 中修改");
            break;
         case CYCLE:
            String currentOption = rawConfigValue == null ? (configSetting.values.length > 0 ? configSetting.values[0] : "") : String.valueOf(rawConfigValue);
            lore.add("§7当前值: §b" + currentOption);
            lore.add("");
            StringBuilder optionsBuilder = new StringBuilder("§7可选: ");

            for (String option : configSetting.values) {
               optionsBuilder.append("§f").append(option).append("§7 ");
            }

            lore.add(optionsBuilder.toString());
            lore.add("");
            lore.add("§e点击切换下一个");
            break;
         case ORE_TOGGLE:
            boolean oreInPool = this.isOreInPool(configSetting.path, configSetting.icon);
            lore.add("§7状态: " + (oreInPool ? "§a✔ 在矿石池中" : "§c✘ 不在矿石池"));
            lore.add("");
            lore.add("§e点击" + (oreInPool ? "§c移除" : "§a加入") + "§e该矿石");
            break;
         case HEADER:
            lore.add("§7分组标题(不可操作)");
            lore.add("");
            lore.add("§e点击该维度下的矿石进行开关");
      }

      lore.add("");
      lore.add("§8" + configSetting.path);
      itemMeta.setLore(lore);
      item.setItemMeta(itemMeta);
      return item;
   }

   private ItemStack createBackButton() {
      ItemStack item = new ItemStack(Material.OAK_DOOR);
      ItemMeta itemMeta = item.getItemMeta();
      itemMeta.setDisplayName("§e返回主菜单");
      item.setItemMeta(itemMeta);
      return item;
   }

   private ItemStack createNavigationButton(String displayName, Material material) {
      ItemStack item = new ItemStack(material);
      ItemMeta itemMeta = item.getItemMeta();
      itemMeta.setDisplayName(displayName);
      item.setItemMeta(itemMeta);
      return item;
   }

   private ItemStack createCloseButton() {
      ItemStack item = new ItemStack(Material.BARRIER);
      ItemMeta itemMeta = item.getItemMeta();
      itemMeta.setDisplayName("§c关闭");
      item.setItemMeta(itemMeta);
      return item;
   }

   private void applyChange(Player player, ConfigMenuCommand.ConfigSetting configSetting, boolean isIncrease, boolean isLargeStep) {
      FileConfiguration config = this.plugin.getConfig();
      if (configSetting.type == ConfigMenuCommand.SettingType.BOOL) {
         boolean currentEnabled = config.getBoolean(configSetting.path, false);
         config.set(configSetting.path, !currentEnabled);
         this.plugin.saveConfig();
         player.sendMessage("§a" + configSetting.name + " §7→ §" + (!currentEnabled ? "a✔ 开启" : "c✘ 关闭"));
      } else if (configSetting.type == ConfigMenuCommand.SettingType.READONLY) {
         player.sendMessage("§7" + configSetting.name + " 为字符串/列表项, 请在 config.yml 中修改。");
      } else if (configSetting.type != ConfigMenuCommand.SettingType.HEADER) {
         if (configSetting.type == ConfigMenuCommand.SettingType.ORE_TOGGLE) {
            List<String> orePool = config.getStringList(configSetting.path);
            String oreMaterialName = configSetting.icon.name();
            boolean alreadyInPool = false;

            for (String existingOre : orePool) {
               if (existingOre.equalsIgnoreCase(oreMaterialName)) {
                  alreadyInPool = true;
                  break;
               }
            }

            if (alreadyInPool) {
               orePool.removeIf(entry -> matchesOreNameIgnoreCase(oreMaterialName, entry));
               config.set(configSetting.path, orePool);
               this.plugin.saveConfig();
               player.sendMessage("§a" + configSetting.name + " §7已从矿石池移除: §c✘ " + oreMaterialName);
            } else {
               orePool.add(oreMaterialName);
               config.set(configSetting.path, orePool);
               this.plugin.saveConfig();
               player.sendMessage("§a" + configSetting.name + " §7已加入矿石池: §a✔ " + oreMaterialName);
            }

            player.sendMessage("§7保存路径: §8" + configSetting.path);
         } else if (configSetting.type != ConfigMenuCommand.SettingType.CYCLE) {
            double numericConfigValue = this.getNumericConfigValue(configSetting.path, configSetting.min);
            double delta;
            if (isLargeStep) {
               delta = isIncrease ? configSetting.step * 10.0 : -configSetting.step * 10.0;
            } else {
               delta = isIncrease ? configSetting.step : -configSetting.step;
            }

            double updatedValue = Math.max(configSetting.min, Math.min(configSetting.max, numericConfigValue + delta));
            if (configSetting.type == ConfigMenuCommand.SettingType.INT) {
               config.set(configSetting.path, (long)updatedValue);
            } else {
               config.set(configSetting.path, Math.round(updatedValue * 100.0) / 100.0);
            }

            this.plugin.saveConfig();
            player.sendMessage("§a" + configSetting.name + " §7→ §b" + formatNumberValue(updatedValue));
            player.sendMessage("§7已保存到 config.yml。§e射线追踪/Heavy 相关参数需 §f/nax reload §e或重启服务器后完全生效。");
         } else {
            String currentOption = config.getString(configSetting.path, configSetting.values.length > 0 ? configSetting.values[0] : "");
            int currentIndex = 0;

            for (int i = 0; i < configSetting.values.length; i++) {
               if (configSetting.values[i].equalsIgnoreCase(currentOption)) {
                  currentIndex = i;
                  break;
               }
            }

            int nextIndex = (currentIndex + 1) % configSetting.values.length;
            config.set(configSetting.path, configSetting.values[nextIndex]);
            this.plugin.saveConfig();
            player.sendMessage("§a" + configSetting.name + " §7→ §b" + configSetting.values[nextIndex]);
         }
      }
   }

   private static List buildRandomOreSettings() {
      List<ConfigMenuCommand.ConfigSetting> settings = new ArrayList<>();
      Material[] overworldSurfaceOres = new Material[]{
         Material.COAL_ORE,
         Material.IRON_ORE,
         Material.COPPER_ORE,
         Material.GOLD_ORE,
         Material.LAPIS_ORE,
         Material.REDSTONE_ORE,
         Material.DIAMOND_ORE,
         Material.EMERALD_ORE
      };
      settings.addAll(buildOrePoolGroup("§6—— 主世界表层 (Y ≥ 0) ——", "check.xray.spawn.random-ore.overworld-surface", overworldSurfaceOres));
      Material[] overworldDeepOres = new Material[]{
         Material.DEEPSLATE_COAL_ORE,
         Material.DEEPSLATE_IRON_ORE,
         Material.DEEPSLATE_COPPER_ORE,
         Material.DEEPSLATE_GOLD_ORE,
         Material.DEEPSLATE_LAPIS_ORE,
         Material.DEEPSLATE_REDSTONE_ORE,
         Material.DEEPSLATE_DIAMOND_ORE,
         Material.DEEPSLATE_EMERALD_ORE
      };
      settings.addAll(buildOrePoolGroup("§3—— 主世界深层 (Y < 0) ——", "check.xray.spawn.random-ore.overworld-deep", overworldDeepOres));
      Material[] netherOres = new Material[]{Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE, Material.ANCIENT_DEBRIS};
      settings.addAll(buildOrePoolGroup("§c—— 下界 ——", "check.xray.spawn.random-ore.nether", netherOres));
      settings.add(ConfigMenuCommand.ConfigSetting.boolSetting("check.xray.spawn.random-ore.enabled", "随机假矿石总开关", Material.EMERALD));
      return settings;
   }

   private static String getOreDisplayName(Material material) {
      String materialName = material.name();
      if (materialName.equals("ANCIENT_DEBRIS")) {
         return "远古残骸";
      }

      if (materialName.equals("NETHER_QUARTZ_ORE")) {
         return "下界石英矿";
      }

      if (materialName.equals("NETHER_GOLD_ORE")) {
         return "下界金矿";
      }

      boolean isDeepslate = materialName.startsWith("DEEPSLATE_");
      String baseOreName = isDeepslate ? materialName.substring(10) : materialName;

      String displayName = switch (baseOreName) {
         case "COAL_ORE" -> "煤矿";
         case "IRON_ORE" -> "铁矿";
         case "COPPER_ORE" -> "铜矿";
         case "GOLD_ORE" -> "金矿";
         case "LAPIS_ORE" -> "青金石矿";
         case "REDSTONE_ORE" -> "红石矿";
         case "DIAMOND_ORE" -> "钻石矿";
         case "EMERALD_ORE" -> "绿宝石矿";
         default -> materialName;
      };
      return isDeepslate ? "深层" + displayName : displayName;
   }

   private boolean isOreInPool(String configPath, Material material) {
      for (String oreName : this.plugin.getConfig().getStringList(configPath)) {
         if (oreName.equalsIgnoreCase(material.name())) {
            return true;
         }
      }

      return false;
   }

   private double getNumericConfigValue(String configPath, double defaultValue) {
      FileConfiguration config = this.plugin.getConfig();
      Object rawValue = config.get(configPath);
      if (rawValue instanceof Number) {
         return ((Number)rawValue).doubleValue();
      }

      if (rawValue instanceof String) {
         try {
            return Double.parseDouble((String)rawValue);
         } catch (NumberFormatException ignored) {
         }
      }

      return defaultValue;
   }

   private static boolean isWholeNumber(double number) {
      return number == Math.floor(number) && !Double.isInfinite(number);
   }

   private static String formatNumberValue(double number) {
      return isWholeNumber(number) ? String.valueOf((long)number) : String.valueOf(number);
   }

   private static String formatStepValue(double step) {
      return isWholeNumber(step) ? String.valueOf((long)step) : String.valueOf(step);
   }

   private void openMainMenu(Player player) {
      ConfigMenuCommand.MenuHolder menuHolder = new ConfigMenuCommand.MenuHolder(true, null, 0);
      Inventory inventory = Bukkit.createInventory(menuHolder, 18, "§8§lNAX·配置 §7主菜单");
      int slotIndex = 0;

      for (ConfigMenuCommand.ConfigModule configModule : CONFIG_MODULES) {
         if (slotIndex >= 16) {
            break;
         }

         inventory.setItem(slotIndex++, this.createModuleIcon(configModule));
      }

      inventory.setItem(17, this.createCloseButton());
      player.openInventory(inventory);
   }

   private static List buildOrePoolGroup(String groupTitle, String configPath, Material[] oreMaterials) {
      List<ConfigMenuCommand.ConfigSetting> groupSettings = new ArrayList<>();
      groupSettings.add(ConfigMenuCommand.ConfigSetting.headerSetting(groupTitle, configPath));

      for (Material material : oreMaterials) {
         groupSettings.add(ConfigMenuCommand.ConfigSetting.oreToggleSetting(configPath, material, getOreDisplayName(material)));
      }

      return groupSettings;
   }

   private static final class ConfigModule {
      final String title;
      final List<ConfigMenuCommand.ConfigSetting> settings;

      ConfigModule(String title, List<ConfigMenuCommand.ConfigSetting> settings) {
         this.title = title;
         this.settings = settings;
      }
   }

   private static final class ConfigSetting {
      final String path;
      final String name;
      final Material icon;
      final ConfigMenuCommand.SettingType type;
      final double min;
      final double max;
      final double step;
      final String[] values;

      ConfigSetting(String path, String name, Material icon, ConfigMenuCommand.SettingType type, double min, double max, double step, String[] values) {
         this.path = path;
         this.name = name;
         this.icon = icon;
         this.type = type;
         this.min = min;
         this.max = max;
         this.step = step;
         this.values = values;
      }

      static ConfigMenuCommand.ConfigSetting boolSetting(String path, String name, Material icon) {
         return new ConfigMenuCommand.ConfigSetting(path, name, icon, ConfigMenuCommand.SettingType.BOOL, 0.0, 0.0, 0.0, null);
      }

      static ConfigMenuCommand.ConfigSetting intSetting(String path, String name, double min, double max) {
         return new ConfigMenuCommand.ConfigSetting(path, name, Material.PAPER, ConfigMenuCommand.SettingType.INT, min, max, 1.0, null);
      }

      static ConfigMenuCommand.ConfigSetting intSetting(String path, String name, double min, double max, double step) {
         return new ConfigMenuCommand.ConfigSetting(path, name, Material.PAPER, ConfigMenuCommand.SettingType.INT, min, max, step, null);
      }

      static ConfigMenuCommand.ConfigSetting intSetting(String path, String name, Material icon, double min, double max, double step) {
         return new ConfigMenuCommand.ConfigSetting(path, name, icon, ConfigMenuCommand.SettingType.INT, min, max, step, null);
      }

      static ConfigMenuCommand.ConfigSetting doubleSetting(String path, String name, double min, double max, double step) {
         return new ConfigMenuCommand.ConfigSetting(path, name, Material.SLIME_BALL, ConfigMenuCommand.SettingType.DOUBLE, min, max, step, null);
      }

      static ConfigMenuCommand.ConfigSetting doubleSetting(String path, String name, Material icon, double min, double max, double step) {
         return new ConfigMenuCommand.ConfigSetting(path, name, icon, ConfigMenuCommand.SettingType.DOUBLE, min, max, step, null);
      }

      static ConfigMenuCommand.ConfigSetting readOnlySetting(String path, String name) {
         return new ConfigMenuCommand.ConfigSetting(path, name, Material.BOOK, ConfigMenuCommand.SettingType.READONLY, 0.0, 0.0, 0.0, null);
      }

      static ConfigMenuCommand.ConfigSetting cycleSetting(String path, String name, Material icon, String[] values) {
         return new ConfigMenuCommand.ConfigSetting(path, name, icon, ConfigMenuCommand.SettingType.CYCLE, 0.0, 0.0, 0.0, values);
      }

      static ConfigMenuCommand.ConfigSetting oreToggleSetting(String path, Material icon, String name) {
         return new ConfigMenuCommand.ConfigSetting(path, name, icon, ConfigMenuCommand.SettingType.ORE_TOGGLE, 0.0, 0.0, 0.0, null);
      }

      static ConfigMenuCommand.ConfigSetting headerSetting(String title, String configPath) {
         return new ConfigMenuCommand.ConfigSetting(configPath, title, Material.GRAY_STAINED_GLASS_PANE, ConfigMenuCommand.SettingType.HEADER, 0.0, 0.0, 0.0, null);
      }
   }

   private static final class MenuHolder implements InventoryHolder {
      final boolean mainMenu;
      final ConfigMenuCommand.ConfigModule module;
      final int page;

      MenuHolder(boolean mainMenu, ConfigMenuCommand.ConfigModule module, int page) {
         this.mainMenu = mainMenu;
         this.module = module;
         this.page = page;
      }

      @Override
      public Inventory getInventory() {
         return null;
      }
   }

   private enum SettingType {
      BOOL,
      INT,
      DOUBLE,
      READONLY,
      CYCLE,
      ORE_TOGGLE,
      HEADER;

      private static ConfigMenuCommand.SettingType[] createValuesArray() {
         return new ConfigMenuCommand.SettingType[]{BOOL, INT, DOUBLE, READONLY, CYCLE, ORE_TOGGLE, HEADER};
      }
   }
}
