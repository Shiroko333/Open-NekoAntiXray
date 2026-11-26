package sakuyaayane.nekoantixray.seed;

import com.github.retrooper.packetevents.PacketEvents;
import java.util.List;
import java.util.stream.Collectors;
import sakuyaayane.nekoantixray.compat.ChunkAccessTransformer;
import sakuyaayane.nekoantixray.config.ConfigManager;
import sakuyaayane.nekoantixray.listener.EndSeedProtectionListener;
import sakuyaayane.nekoantixray.listener.LocateCommandInterceptor;
import sakuyaayane.nekoantixray.listener.SeedcrackerFirewallListener;
import sakuyaayane.nekoantixray.NekoAntiXray;
import sakuyaayane.nekoantixray.packet.AntiSeedCrackerPacketListener;
import sakuyaayane.nekoantixray.packet.DecoratorPacketListener;
import sakuyaayane.nekoantixray.packet.SeedHashPacketListener;
import sakuyaayane.nekoantixray.world.PlayerBlockStateCache;

public class AntiSeedCrackerManager {
   private static final boolean BIOME_OBFUSCATION_RUNTIME_ENABLED = true;
   private final NekoAntiXray plugin;
   private final ConfigManager configManager;
   private SeedHashPacketListener seedHashPacketListener;
   private EndSeedProtectionListener endSeedProtectionListener;
   private AsyncChunkSeedProtection asyncChunkSeedProtection;
   private AntiSeedCrackerPacketListener antiSeedCrackerPacketListener;
   private DecoratorPacketListener decoratorPacketListener;
   private SeedcrackerFirewallListener seedcrackerFirewallListener;
   private PlayerBlockStateCache playerBlockStateCache;
   private boolean enabled;
   private boolean modifyHashedSeedEnabled;
   private boolean modifyEndStructuresEnabled;
   private List<String> endStructureWorlds;
   private boolean modifyEndSpikesEnabled;
   private String endSpikesMode;
   private boolean modifyEndCitiesEnabled;
   private boolean netherBedrockProtectionEnabled;
   private boolean blockNetherRoofAccess;
   private int netherRoofAccessMaxY;
   private boolean secureSeedWorldGenerationHookEnabled;
   private boolean secureSeedBlake2HashingEnabled;

   public AntiSeedCrackerManager(NekoAntiXray plugin, ConfigManager configManager) {
      this.plugin = plugin;
      this.configManager = configManager;
      this.loadConfiguration();
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public List getEndStructureWorlds() {
      return this.endStructureWorlds;
   }

   public String getEndSpikesMode() {
      return this.endSpikesMode;
   }

   public int getNetherRoofAccessMaxY() {
      return this.netherRoofAccessMaxY;
   }

   private void loadConfiguration() {
      this.plugin.logDebug("[AntiSeedCracker] 正在通过 ConfigManager 加载配置...");
      this.enabled = this.configManager.isAntiSeedCrackerEnable();
      this.plugin.logDebug("[AntiSeedCracker] 主要组件已启用: " + this.enabled);
      if (this.enabled) {
         this.modifyHashedSeedEnabled = this.configManager.isAntiSeedCrackerModifyHashedSeedEnable();
         this.plugin.logDebug("[AntiSeedCracker] 修改哈希种子已启用: " + this.modifyHashedSeedEnabled);
         this.plugin.logDebug("[AntiSeedCracker] 生物群系数据包混淆已启用: " + this.configManager.isAntiSeedCrackerBiomePacketObfuscationEnabled());
         this.plugin
            .logDebug(
               "[AntiSeedCracker] 装饰特征数据包混淆已启用: "
                  + this.configManager.isAntiSeedCrackerDecoratorPacketObfuscationEnabled()
                  + " (mode="
                  + this.configManager.getAntiSeedCrackerDecoratorPacketObfuscationMode()
                  + ")"
            );
         this.plugin
            .logDebug(
               "[AntiSeedCracker] SeedcrackerX 结构种子签名防护已启用: "
                  + this.configManager.isAntiSeedCrackerStructureSeedProtectionEnabled()
                  + " (strategy="
                  + this.configManager.getAntiSeedCrackerStructureSeedProtectionStrategy()
                  + ")"
            );
         this.plugin.logDebug("[AntiSeedCracker] SeedcrackerX shadow-state 防火墙已启用: " + this.configManager.isAntiSeedCrackerSeedcrackerXFirewallEnabled());
         this.modifyEndStructuresEnabled = this.configManager.isAntiSeedCrackerModifyEndStructuresEnable();
         this.plugin.logDebug("[AntiSeedCracker] 修改末地结构已启用: " + this.modifyEndStructuresEnabled);
         if (this.modifyEndStructuresEnabled) {
            this.endStructureWorlds = this.configManager.getAntiSeedCrackerModifyEndStructuresWorlds();
            if (this.endStructureWorlds != null) {
               this.endStructureWorlds = this.endStructureWorlds
                  .stream()
                  .map(dimensionKey -> WorldSeedResolver.normalizeDimensionKey(dimensionKey))
                  .collect(Collectors.toList());
            }

            this.plugin.logDebug("[AntiSeedCracker] 末地结构修改世界(规范化): " + (this.endStructureWorlds != null ? this.endStructureWorlds.toString() : "null"));
            this.modifyEndSpikesEnabled = this.configManager.isAntiSeedCrackerModifyEndStructuresEndSpikesEnable();
            this.plugin.logDebug("[AntiSeedCracker] 末地尖刺修改已启用: " + this.modifyEndSpikesEnabled);
            if (this.modifyEndSpikesEnabled) {
               this.endSpikesMode = this.configManager.getAntiSeedCrackerModifyEndStructuresEndSpikesMode();
               this.plugin.logDebug("[AntiSeedCracker] 末地尖刺修改模式: " + this.endSpikesMode);
            }

            this.modifyEndCitiesEnabled = this.configManager.isAntiSeedCrackerModifyEndStructuresEndCitiesEnable();
            this.plugin.logDebug("[AntiSeedCracker] 末地城修改已启用: " + this.modifyEndCitiesEnabled);
         }

         this.netherBedrockProtectionEnabled = this.configManager.isAntiSeedCrackerNetherBedrockProtectionEnabled();
         this.blockNetherRoofAccess = this.configManager.isAntiSeedCrackerNetherBedrockProtectionBlockRoofAccess();
         this.netherRoofAccessMaxY = this.configManager.getAntiSeedCrackerNetherBedrockProtectionRoofAccessMaxY();
         this.plugin.logDebug("[AntiSeedCracker] Nether bedrock protection enabled: " + this.netherBedrockProtectionEnabled);
         this.plugin.logDebug("[AntiSeedCracker] Nether roof access guard: " + this.blockNetherRoofAccess + " (maxY=" + this.netherRoofAccessMaxY + ")");
         this.plugin.logDebug("[AntiSeedCracker] 修改生物群系已启用: " + this.configManager.isSecureSeedBiomeSourceXorEnabled());
         this.secureSeedWorldGenerationHookEnabled = this.plugin.getConfig().getBoolean("secure-seed.world-generation-hook-enabled", false);
         this.secureSeedBlake2HashingEnabled = this.plugin.getConfig().getBoolean("secure-seed.blake2-hashing-enabled", true);
         this.plugin.logDebug("[AntiSeedCracker] 安全世界生成已启用: " + this.secureSeedWorldGenerationHookEnabled);
         this.plugin.logDebug("[AntiSeedCracker] BLAKE2哈希已启用: " + this.secureSeedBlake2HashingEnabled);
      } else {
         this.plugin.logDebug("[AntiSeedCracker] 组件已在配置中禁用。");
      }
   }

   public void initialize() {
      if (!this.enabled) {
         this.plugin.logDebug("[AntiSeedCracker] AntiSeedCracker 已禁用。跳过功能初始化。");
      } else {
         this.plugin.logDebug("[AntiSeedCracker] 正在初始化功能...");

         try {
            ChunkAccessTransformer.initialize();
            this.plugin.logDebug("[AntiSeedCracker] 版本兼容性检测完成");
            this.plugin.logDebug("[AntiSeedCracker] 检测到的版本: " + ChunkAccessTransformer.getDetectedVersion());
            this.plugin.logDebug("[AntiSeedCracker] 兼容性状态: " + (ChunkAccessTransformer.isCompatible() ? "兼容" : "不兼容"));
            if (ChunkAccessTransformer.getChunkAccessClass() != null) {
               this.plugin.logDebug("[AntiSeedCracker] 可用的ChunkAccess类: " + ChunkAccessTransformer.getChunkAccessClass().getName());
            } else {
               this.plugin.logDebug("[AntiSeedCracker] 警告: 未找到ChunkAccess类，安全世界生成功能可能受限");
            }
         } catch (Exception ex) {
            this.plugin.logDebug("[AntiSeedCracker] 版本兼容性检测失败: " + ex.getMessage());
            this.plugin.logDebug("[AntiSeedCracker] 将尝试继续初始化，但功能可能受限");
         }

         if (this.secureSeedWorldGenerationHookEnabled) {
            this.plugin.logDebug("[AntiSeedCracker] 正在初始化安全世界生成钩子...");

            try {
               if (ChunkAccessTransformer.isCompatible()) {
                  this.asyncChunkSeedProtection = new AsyncChunkSeedProtection();
                  this.plugin.getServer().getPluginManager().registerEvents(this.asyncChunkSeedProtection, this.plugin);
                  this.plugin.logDebug("[AntiSeedCracker] 安全世界生成钩子已注册。");
                  this.plugin.logDebug("[AntiSeedCracker] - 1024位种子保护已激活");
                  this.plugin.logDebug("[AntiSeedCracker] - BLAKE2加密哈希已激活");
                  this.plugin.logDebug("[AntiSeedCracker] - 结构偏移保护已激活");
                  this.plugin
                     .logDebug(
                        "[AntiSeedCracker] - 版本: "
                           + ChunkAccessTransformer.getDetectedVersion()
                           + " (高版本模式: "
                           + ChunkAccessTransformer.isHighVersionMode()
                           + ")"
                     );
               } else {
                  this.plugin.logDebug("[AntiSeedCracker] 警告: 当前版本不完全兼容安全世界生成功能");
                  this.plugin.logDebug("[AntiSeedCracker] 检测到的版本: " + ChunkAccessTransformer.getDetectedVersion());
                  this.plugin.logDebug("[AntiSeedCracker] 继续注册钩子，但某些功能可能受限");
                  this.asyncChunkSeedProtection = new AsyncChunkSeedProtection();
                  this.plugin.getServer().getPluginManager().registerEvents(this.asyncChunkSeedProtection, this.plugin);
                  this.plugin.logDebug("[AntiSeedCracker] 安全世界生成钩子已注册（兼容模式）。");
               }
            } catch (Exception ex) {
               this.plugin.logDebug("[AntiSeedCracker] 注册安全世界生成钩子时出错: " + ex.getMessage());
               this.plugin.logDebug("[AntiSeedCracker] 错误详情: " + ex.getClass().getSimpleName());
               if (ex.getCause() != null) {
                  this.plugin.logDebug("[AntiSeedCracker] 根本原因: " + ex.getCause().getMessage());
               }

               ex.printStackTrace();
            }
         }

         if (this.configManager.isAntiSeedCrackerBiomePacketObfuscationEnabled()) {
            try {
               this.antiSeedCrackerPacketListener = new AntiSeedCrackerPacketListener(this.plugin);
               PacketEvents.getAPI().getEventManager().registerListener(this.antiSeedCrackerPacketListener);
               this.plugin.logDebug("[AntiSeedCracker] 生物群系数据包混淆监听器已注册(PacketEvents)。");
            } catch (Exception ex) {
               this.plugin.logDebug("[AntiSeedCracker] 注册生物群系数据包混淆监听器时出错: " + ex.getMessage());
            }
         } else if (this.configManager.isAntiSeedCrackerBiomePacketObfuscationEnabled()) {
            this.plugin.logDebug("[AntiSeedCracker] 生物群系数据包混淆监听器未注册：运行时开关已关闭。");
         }

         if (this.configManager.isAntiSeedCrackerDecoratorPacketObfuscationEnabled()
            || this.configManager.isAntiSeedCrackerStructureSeedProtectionEnabled()
            || this.configManager.isAntiSeedCrackerSeedcrackerXFirewallEnabled()) {
            this.playerBlockStateCache = new PlayerBlockStateCache(
               this.configManager.getAntiSeedCrackerSeedcrackerXFirewallShadowStateTtlSeconds(), this.configManager.getAntiSeedCrackerSeedcrackerXFirewallShadowStateMaxPositionsPerPlayer()
            );
         }

         if (this.configManager.isAntiSeedCrackerSeedcrackerXFirewallEnabled()) {
            try {
               this.seedcrackerFirewallListener = new SeedcrackerFirewallListener(this.plugin, this.playerBlockStateCache, this.configManager);
               PacketEvents.getAPI().getEventManager().registerListener(this.seedcrackerFirewallListener);
               this.plugin.logDebug("[AntiSeedCracker] SeedcrackerX shadow-state 防火墙监听器已注册(PacketEvents)。");
            } catch (Exception ex) {
               this.plugin.logDebug("[AntiSeedCracker] 注册 SeedcrackerX 防火墙监听器时出错: " + ex.getMessage());
            }
         }

         if (this.configManager.isAntiSeedCrackerDecoratorPacketObfuscationEnabled() || this.configManager.isAntiSeedCrackerStructureSeedProtectionEnabled()) {
            try {
               this.decoratorPacketListener = new DecoratorPacketListener(this.plugin, this.configManager, this.playerBlockStateCache);
               PacketEvents.getAPI().getEventManager().registerListener(this.decoratorPacketListener);
               this.plugin.logDebug("[AntiSeedCracker] 装饰/结构签名数据包混淆监听器已注册(PacketEvents)。");
            } catch (Exception ex) {
               this.plugin.logDebug("[AntiSeedCracker] 注册装饰/结构签名数据包混淆监听器时出错: " + ex.getMessage());
            }
         }

         if (this.isModifyHashedSeedEnabled()) {
            try {
               this.seedHashPacketListener = new SeedHashPacketListener(this.plugin, this);
               PacketEvents.getAPI().getEventManager().registerListener(this.seedHashPacketListener);
               this.plugin.logDebug("[AntiSeedCracker] 哈希种子数据包监听器已注册(PacketEvents)。");
            } catch (Exception ex) {
               this.plugin.logDebug("[AntiSeedCracker] 注册哈希种子数据包监听器时出错: " + ex.getMessage());
            }
         }

         if (this.isModifyEndStructuresEnabled()) {
            this.plugin.logDebug("[AntiSeedCracker] 正在设置末地结构修改器...");

            try {
               this.endSeedProtectionListener = new EndSeedProtectionListener(this.plugin, this);
               this.plugin.getServer().getPluginManager().registerEvents(this.endSeedProtectionListener, this.plugin);
               this.plugin.logDebug("[AntiSeedCracker] 末地结构修改器已注册。");
            } catch (Exception ex) {
               this.plugin.logDebug("[AntiSeedCracker] 注册末地结构修改器时出错: " + ex.getMessage());
            }
         }

         try {
            this.plugin.getServer().getPluginManager().registerEvents(new LocateCommandInterceptor(this.plugin), this.plugin);
            this.plugin.logDebug("[AntiSeedCracker] Locate command interceptor registered.");
         } catch (Exception ex) {
            this.plugin.logDebug("[AntiSeedCracker] Failed to register LocateCommandInterceptor: " + ex.getMessage());
         }

         if (this.netherBedrockProtectionEnabled) {
            this.plugin.logDebug("[AntiSeedCracker] Nether bedrock ASM protection is enabled.");
            if (this.blockNetherRoofAccess) {
               this.plugin.logDebug("[AntiSeedCracker] Nether roof access limit enabled (maxY=" + this.netherRoofAccessMaxY + ").");
            }
         } else {
            this.plugin.logDebug("[AntiSeedCracker] Nether bedrock ASM protection is disabled by config.");
         }

         this.plugin.logDebug("[AntiSeedCracker] 功能已初始化 (如果已启用)。");
      }
   }

   public void shutdown() {
      if (this.enabled) {
         this.plugin.logDebug("[AntiSeedCracker] 正在关闭功能...");
         if (this.asyncChunkSeedProtection != null) {
            try {
               AsyncChunkSeedProtection.cleanup();
               this.plugin.logDebug("[AntiSeedCracker] 安全世界生成钩子已清理。");
            } catch (Exception ex) {
               this.plugin.logDebug("[AntiSeedCracker] 清理安全世界生成钩子时出错: " + ex.getMessage());
            }
         }

         this.asyncChunkSeedProtection = null;
         if (this.seedHashPacketListener != null) {
            try {
               PacketEvents.getAPI().getEventManager().unregisterListener(this.seedHashPacketListener);
               this.plugin.logDebug("[AntiSeedCracker] 哈希种子数据包监听器已注销(PacketEvents)。");
            } catch (Exception ex) {
               this.plugin.logDebug("[AntiSeedCracker] 注销哈希种子数据包监听器时出错: " + ex.getMessage());
            } finally {
               this.seedHashPacketListener = null;
            }
         }

         if (this.antiSeedCrackerPacketListener != null) {
            try {
               PacketEvents.getAPI().getEventManager().unregisterListener(this.antiSeedCrackerPacketListener);
               this.plugin.logDebug("[AntiSeedCracker] 生物群系数据包混淆监听器已注销(PacketEvents)。");
            } catch (Exception ex) {
               this.plugin.logDebug("[AntiSeedCracker] 移除生物群系数据包混淆监听器时出错: " + ex.getMessage());
            } finally {
               this.antiSeedCrackerPacketListener = null;
            }
         }

         if (this.decoratorPacketListener != null) {
            try {
               PacketEvents.getAPI().getEventManager().unregisterListener(this.decoratorPacketListener);
               this.plugin.logDebug("[AntiSeedCracker] 装饰特征数据包混淆监听器已注销(PacketEvents)。");
            } catch (Exception ex) {
               this.plugin.logDebug("[AntiSeedCracker] 移除装饰特征数据包混淆监听器时出错: " + ex.getMessage());
            } finally {
               this.decoratorPacketListener = null;
            }
         }

         if (this.seedcrackerFirewallListener != null) {
            try {
               PacketEvents.getAPI().getEventManager().unregisterListener(this.seedcrackerFirewallListener);
               this.plugin.logDebug("[AntiSeedCracker] SeedcrackerX shadow-state 防火墙监听器已注销(PacketEvents)。");
            } catch (Exception ex) {
               this.plugin.logDebug("[AntiSeedCracker] 移除 SeedcrackerX 防火墙监听器时出错: " + ex.getMessage());
            } finally {
               this.seedcrackerFirewallListener = null;
            }
         }

         if (this.playerBlockStateCache != null) {
            this.playerBlockStateCache.clearAll();
            this.playerBlockStateCache = null;
         }

         this.endSeedProtectionListener = null;
         WorldSeedResolver.clearStructureOffsets();
      }
   }

   public boolean isModifyHashedSeedEnabled() {
      return this.enabled && this.modifyHashedSeedEnabled;
   }

   public boolean isModifyEndStructuresEnabled() {
      return this.enabled && this.modifyEndStructuresEnabled;
   }

   public boolean isModifyEndSpikesEnabled() {
      return this.enabled && this.modifyEndStructuresEnabled && this.modifyEndSpikesEnabled;
   }

   public boolean isModifyEndCitiesEnabled() {
      return this.enabled && this.modifyEndStructuresEnabled && this.modifyEndCitiesEnabled;
   }

   public boolean isNetherBedrockProtectionEnabled() {
      return this.enabled && this.netherBedrockProtectionEnabled;
   }

   public boolean isBlockNetherRoofAccessEnabled() {
      return this.enabled && this.netherBedrockProtectionEnabled && this.blockNetherRoofAccess;
   }

   public boolean isSecureSeedWorldGenHookEnabled() {
      return this.enabled && this.secureSeedWorldGenerationHookEnabled;
   }

   public boolean isSecureSeedBlake2HashingEnabled() {
      return this.enabled && this.secureSeedBlake2HashingEnabled;
   }

   public static boolean isSlimeChunk(String worldName, int chunkX, int chunkZ) {
      return AsyncChunkSeedProtection.isSlimeChunk(worldName, chunkX, chunkZ);
   }

   public static SecureSeedRandom createChunkRandom(String worldName, int chunkX, int chunkZ, WorldSeedResolver.SeedSalt seedSalt) {
      return AsyncChunkSeedProtection.createChunkRandom(worldName, chunkX, chunkZ, seedSalt);
   }

   public static int[] applyStructureOffset(String worldName, int chunkX, int chunkZ) {
      return AsyncChunkSeedProtection.applyStructureOffset(worldName, chunkX, chunkZ);
   }
}
