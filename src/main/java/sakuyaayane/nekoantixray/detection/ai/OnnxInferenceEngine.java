package sakuyaayane.nekoantixray.detection.ai;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession.Result;
import ai.onnxruntime.OrtSession.SessionOptions;
import ai.onnxruntime.OrtSession;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;
import lombok.Generated;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import sakuyaayane.nekoantixray.detection.MiningBehaviorDetector;
import sakuyaayane.nekoantixray.NekoAntiXray;

public class OnnxInferenceEngine {
   private static final Map<String, String> FEATURE_NAME_OBFUSCATION_MAP = new HashMap<String, String>() {
      {
         this.put("rotation_difference", "XFEA");
         this.put("distance_from_last_ore", "YBGC");
         this.put("was_exposed_before_break", "ZDHA");
         this.put("recent_ore_count", "WEIB");
         this.put("label", "TFLAG");
         this.put("cheater", "HVAL1");
         this.put("normal", "HVAL0");
      }
   };
   private final NekoAntiXray plugin;
   private OrtEnvironment ortEnvironment;
   private OrtSession session;
   private OnnxInferenceEngine.FeatureStats featureStats;
   private boolean isReady = false;
   private int deeplearnSequenceLength;
   private double deeplearnPredictionThreshold;
   private long deeplearnCooldownMs;
   private long deeplearnMiningTimeWindowMs;
   private double deeplearnVeinDetectionMaxDistance;
   private long deeplearnVeinDetectionMaxTimeMs;
   private Set<Material> valuableMaterials;
   private boolean debugMode;
   private final Map<UUID, LinkedList<float[]>> playerFeatureSequences = new ConcurrentHashMap<>();
   private final Map<UUID, Long> inferenceCooldownUntil = new ConcurrentHashMap<>();
   private final Map<UUID, OnnxInferenceEngine.PlayerMiningState> playerMiningStates = new ConcurrentHashMap<>();
   private final Map<UUID, Set<Location>> revealedOres = new ConcurrentHashMap<>();
   private final Map<UUID, List<OnnxInferenceEngine.MinedOreRecord>> recentOreHistory = new ConcurrentHashMap<>();

   private boolean isOreRecordExpired(long nowMs, OnnxInferenceEngine.MinedOreRecord minedOreRecord) {
      return nowMs - minedOreRecord.timestamp > this.deeplearnMiningTimeWindowMs;
   }

   private Material parseConfiguredMaterial(String materialName) {
      try {
         Material material = Material.valueOf(materialName.toUpperCase());
         if (this.debugMode) {
            this.plugin.getLogger().info("[DL-Debug] Successfully loaded material: " + material);
         }

         return material;
      } catch (IllegalArgumentException ignored) {
         this.plugin.getLogger().warning("[DeepLearn] 配置中的材料名称无效: " + materialName);
         return null;
      }
   }

   private String mapFeatureKey(String featureName) {
      return FEATURE_NAME_OBFUSCATION_MAP.getOrDefault(featureName, featureName);
   }

   private void initializeDisabledDefaults() {
      this.debugMode = this.plugin.getConfig().getBoolean("deeplearn.debug", false);
      this.deeplearnSequenceLength = this.plugin.getConfig().getInt("deeplearn.sequence_length", 50);
      this.deeplearnPredictionThreshold = this.plugin.getConfig().getDouble("deeplearn.prediction_threshold", 0.8);
      this.deeplearnCooldownMs = this.plugin.getConfig().getLong("deeplearn.cooldown_ms", 5000L);
      this.deeplearnMiningTimeWindowMs = this.plugin.getConfig().getLong("deeplearn.mining_time_window_ms", 600000L);
      this.deeplearnVeinDetectionMaxDistance = this.plugin.getConfig().getDouble("deeplearn.vein_detection.max_distance", 2.0);
      this.deeplearnVeinDetectionMaxTimeMs = this.plugin.getConfig().getLong("deeplearn.vein_detection.max_time_ms", 3000L);
      this.valuableMaterials = EnumSet.noneOf(Material.class);
      if (this.debugMode) {
         this.plugin.getLogger().info("[DL-Debug] 基本字段已初始化，AI检测已禁用但不会产生错误。");
      }
   }

   private void loadConfiguration() {
      this.deeplearnSequenceLength = this.plugin.getConfig().getInt("deeplearn.sequence_length", 50);
      this.deeplearnPredictionThreshold = this.plugin.getConfig().getDouble("deeplearn.prediction_threshold", 0.8);
      this.deeplearnCooldownMs = this.plugin.getConfig().getLong("deeplearn.cooldown_ms", 10000L);
      this.deeplearnMiningTimeWindowMs = this.plugin.getConfig().getLong("deeplearn.mining_time_window_ms", 600000L);
      this.deeplearnVeinDetectionMaxDistance = this.plugin.getConfig().getDouble("deeplearn.vein_detection.max_distance", 2.0);
      this.deeplearnVeinDetectionMaxTimeMs = this.plugin.getConfig().getLong("deeplearn.vein_detection.max_time_ms", 5000L);
      this.debugMode = this.plugin.getConfig().getBoolean("deeplearn.debug", false);
      List<String> configuredMaterials = this.plugin.getConfig().getStringList("deeplearn.valuable_materials");
      if (configuredMaterials != null && !configuredMaterials.isEmpty()) {
         if (this.debugMode) {
            this.plugin.getLogger().info("[DL-Debug] Loading valuable materials from config: " + configuredMaterials);
         }

         this.valuableMaterials = configuredMaterials.stream()
            .map(materialName -> this.parseConfiguredMaterial(materialName))
            .filter(material -> Objects.nonNull(material))
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Material.class)));
      } else {
         this.valuableMaterials = EnumSet.of(
            Material.DIAMOND_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE,
            Material.DEEPSLATE_EMERALD_ORE,
            Material.GOLD_ORE,
            Material.DEEPSLATE_GOLD_ORE,
            Material.NETHER_GOLD_ORE,
            Material.IRON_ORE,
            Material.DEEPSLATE_IRON_ORE,
            Material.LAPIS_ORE,
            Material.DEEPSLATE_LAPIS_ORE,
            Material.COPPER_ORE,
            Material.DEEPSLATE_COPPER_ORE,
            Material.REDSTONE_ORE,
            Material.DEEPSLATE_REDSTONE_ORE,
            Material.COAL_ORE,
            Material.DEEPSLATE_COAL_ORE,
            Material.ANCIENT_DEBRIS,
            Material.NETHER_QUARTZ_ORE,
            Material.SPAWNER
         );
         if (this.debugMode) {
            this.plugin.getLogger().info("[DL-Debug] Using default valuable materials list");
         }
      }

      if (this.debugMode) {
         this.plugin.getLogger().info("[DL-Debug] Loaded valuable materials: " + this.valuableMaterials);
         Logger logger = this.plugin.getLogger();
         logger.info("[DL-Debug] Vein detection config - maxDistance: " + this.deeplearnVeinDetectionMaxDistance + ", maxTimeMs: " + this.deeplearnVeinDetectionMaxTimeMs);
      }
   }

   private boolean loadOnnxModel() {
      File modelFile = new File(this.plugin.getDataFolder(), "model.onnx");
      if (!modelFile.exists()) {
         this.plugin.getLogger().warning("模型文件 (model.onnx) 未找到。");
         return false;
      }

      try {
         this.ortEnvironment = OrtEnvironment.getEnvironment();
         this.session = this.ortEnvironment.createSession(modelFile.getAbsolutePath(), new SessionOptions());
         this.plugin.getLogger().info("ONNX 模型加载成功。");
         return true;
      } catch (OrtException ex) {
         this.plugin.getLogger().severe("加载ONNX模型时发生错误: " + ex.getMessage());
         this.plugin.getLogger().severe("错误详情: " + ex.toString());
         return false;
      }
   }

   private boolean loadFeatureStats() {
      File statsFile = new File(this.plugin.getDataFolder(), "feature_stats.json");
      if (!statsFile.exists()) {
         this.plugin.getLogger().warning("特征统计文件 (feature_stats.json) 未找到。");
         return false;
      }

      try (FileReader fileReader = new FileReader(statsFile)) {
         Gson gson = new Gson();
         this.featureStats = gson.fromJson(fileReader, OnnxInferenceEngine.FeatureStats.class);
         this.plugin.getLogger().info("特征统计文件加载成功。");
         return true;
      } catch (Exception ex) {
         this.plugin.getLogger().severe("加载特征统计文件时发生错误: " + ex.getMessage());
         this.plugin.getLogger().severe("错误详情: " + ex.toString());
         return false;
      }
   }

   public Map onPlayerBlockBreak(Player player, Block block) {
      if (!this.isReady) {
         if (this.debugMode) {
            this.plugin.getLogger().info("[DL-Debug] InferenceManager未就绪，跳过 " + player.getName() + " 的挖掘分析");
         }

         return null;
      } else {
         if (this.debugMode) {
            this.plugin.getLogger().info("[DL-Debug] onPlayerBlockBreak for " + player.getName() + " breaking " + block.getType());
         }

         boolean isValuable = this.valuableMaterials != null && this.valuableMaterials.contains(block.getType());
         if (this.debugMode) {
            this.plugin.getLogger().info("[DL-Debug] Checking block " + block.getType());
            this.plugin.getLogger().info("[DL-Debug] Final isValuable result: " + isValuable);
         }

         if (!isValuable) {
            if (this.debugMode) {
               this.plugin.getLogger().info("[DL-Debug] Block " + block.getType() + " is not a valuable material. Skipping.");
            }

            this.updateRevealedOres(player, block);
            return null;
         } else {
            boolean veinMiningContinuation = this.isVeinMiningContinuation(player, block);
            if (veinMiningContinuation) {
               if (this.debugMode) {
                  this.plugin.getLogger().info("[DL-Debug] Vein mining continuation detected for " + player.getName() + ". Skipping data recording.");
               }

               this.updatePlayerStateAfterFeatureGeneration(player, block);
               return null;
            } else {
               Map featureMap = this.generateFeatureMap(player, block);
               if (this.debugMode) {
                  this.plugin.getLogger().info("[DL-Debug] " + player.getName() + " -> Feature Map: " + featureMap.toString());
               }

               this.updatePlayerStateAfterFeatureGeneration(player, block);
               if (this.session != null && this.featureStats != null) {
                  long nowMs = System.currentTimeMillis();
                  if (nowMs < this.inferenceCooldownUntil.getOrDefault(player.getUniqueId(), 0L)) {
                     if (this.debugMode) {
                        this.plugin.getLogger().info("[DL-Debug] Player " + player.getName() + " is on inference cooldown.");
                     }

                     return featureMap;
                  } else {
                     if (this.session != null && this.featureStats != null) {
                        float[] preprocessedVector = this.preprocessFeatures(featureMap);
                        if (preprocessedVector == null) {
                           if (this.debugMode) {
                              this.plugin.getLogger().warning("[DL-Debug] Preprocessing failed for " + player.getName() + ", feature vector is null.");
                           }

                           return featureMap;
                        }

                        if (this.debugMode) {
                           this.plugin.getLogger().info("[DL-Debug] " + player.getName() + " -> Preprocessed Vector: " + Arrays.toString(preprocessedVector));
                        }

                        LinkedList<float[]> featureSequence = this.playerFeatureSequences.computeIfAbsent(player.getUniqueId(), ignoredUuid -> new LinkedList<>());
                        featureSequence.add(preprocessedVector);
                        if (featureSequence.size() > this.deeplearnSequenceLength) {
                           featureSequence.removeFirst();
                        }

                        if (featureSequence.size() == this.deeplearnSequenceLength) {
                           if (this.debugMode) {
                              this.plugin.getLogger().info("[DL-Debug] " + player.getName() + " -> Sequence full and not vein mining, running inference.");
                           }

                           this.runInference(player, featureSequence);
                           this.inferenceCooldownUntil.put(player.getUniqueId(), nowMs + this.deeplearnCooldownMs);
                        } else if (this.debugMode) {
                           String skipReason = featureSequence.size() < this.deeplearnSequenceLength
                              ? "Sequence not full"
                              : "Skipping inference for vein mining (cached result)";
                           Logger logger = this.plugin.getLogger();
                           String playerName = player.getName();
                           int sequenceSize = featureSequence.size();
                           logger.info("[DL-Debug] " + playerName + " -> " + skipReason + ". Sequence length: " + sequenceSize + "/" + this.deeplearnSequenceLength);
                        }
                     } else if (this.debugMode) {
                        this.plugin.getLogger().info("[DL-Debug] Skipping preprocessing and inference - model not fully ready.");
                     }

                     return featureMap;
                  }
               } else {
                  if (this.debugMode) {
                     this.plugin
                        .getLogger()
                        .info(
                           "[DL-Debug] Module not ready - session: "
                              + (this.session != null)
                              + ", featureStats: "
                              + (this.featureStats != null)
                              + ". Returning feature map without inference."
                        );
                  }

                  return featureMap;
               }
            }
         }
      }
   }

   private void updateRevealedOres(Player player, Block block) {
      if (this.valuableMaterials == null) {
         if (this.debugMode) {
            this.plugin.getLogger().warning("[DL-Debug] valuableMaterials为null，跳过暴露矿石更新");
         }
      } else {
         Set<Location> playerRevealedOres = this.revealedOres.computeIfAbsent(player.getUniqueId(), ignoredUuid -> new HashSet<>());

         for (BlockFace face : BlockFace.values()) {
            if (face.isCartesian()) {
               Block relative = block.getRelative(face);
               if (this.valuableMaterials.contains(relative.getType())) {
                  playerRevealedOres.add(relative.getLocation());
               }
            }
         }
      }
   }

   public Map generateFeatureMap(Player player, Block block) {
      HashMap<String, Float> featureMap = new HashMap<>();
      OnnxInferenceEngine.PlayerMiningState playerMiningState = this.playerMiningStates
         .computeIfAbsent(player.getUniqueId(), ignoredUuid -> new OnnxInferenceEngine.PlayerMiningState());
      Location blockLocation = block.getLocation();
      Material blockType = block.getType();
      double rotationDifference = 45.0;
      if (playerMiningState.lastOreLocation != null && playerMiningState.lastOreLocation.getWorld().equals(blockLocation.getWorld()) && !playerMiningState.lastOreLocation.equals(blockLocation)) {
         Vector toLastOre = playerMiningState.lastOreLocation.toVector().subtract(blockLocation.toVector()).normalize();
         Vector lookDirection = player.getEyeLocation().getDirection().normalize();
         double angleRadians = lookDirection.angle(toLastOre);
         rotationDifference = Math.toDegrees(angleRadians);
         if (this.debugMode) {
            Logger logger = this.plugin.getLogger();
            logger.info("[DL-Debug] Calculated rotation difference: " + rotationDifference + " from " + playerMiningState.lastOreLocation + " to " + blockLocation);
         }
      } else if (this.debugMode) {
         this.plugin.getLogger().info("[DL-Debug] No last ore location, using default rotation difference: " + rotationDifference);
      }

      featureMap.put(this.mapFeatureKey("rotation_difference"), (float)rotationDifference);
      float distanceFromLastOre = 0.0F;
      if (playerMiningState.lastOreLocation != null && playerMiningState.lastOreLocation.getWorld().equals(blockLocation.getWorld())) {
         distanceFromLastOre = (float)playerMiningState.lastOreLocation.distance(blockLocation);
      }

      featureMap.put(this.mapFeatureKey("distance_from_last_ore"), distanceFromLastOre);
      Set<Location> playerRevealedOres = this.revealedOres.getOrDefault(player.getUniqueId(), Collections.emptySet());
      boolean wasExposed = playerRevealedOres.contains(blockLocation);
      featureMap.put(this.mapFeatureKey("was_exposed_before_break"), wasExposed ? 1.0F : 0.0F);
      if (this.debugMode) {
         this.plugin.getLogger().info("[DL-Debug] wasExposed: " + wasExposed + ", revealedOres size: " + playerRevealedOres.size());
      }

      UUID playerUuid = player.getUniqueId();
      long nowMs = System.currentTimeMillis();
      int recentOreCount = this.countRecentOreMined(playerUuid, blockType, nowMs);
      featureMap.put(this.mapFeatureKey("recent_ore_count"), (float)recentOreCount);
      if (this.debugMode) {
         List<OnnxInferenceEngine.MinedOreRecord> oreHistory = this.recentOreHistory.get(player.getUniqueId());
         int historySize = oreHistory != null ? oreHistory.size() : 0;
         this.plugin.getLogger().info("[DL-Debug] Recent ore count: " + recentOreCount + ", total history size: " + historySize + ", ore type: " + blockType);
      }

      return featureMap;
   }

   private void updatePlayerStateAfterFeatureGeneration(Player player, Block block) {
      OnnxInferenceEngine.PlayerMiningState playerMiningState = this.playerMiningStates
         .computeIfAbsent(player.getUniqueId(), ignoredUuid -> new OnnxInferenceEngine.PlayerMiningState());
      List<OnnxInferenceEngine.MinedOreRecord> oreHistory = this.recentOreHistory.computeIfAbsent(player.getUniqueId(), ignoredKey -> new ArrayList<>());
      oreHistory.add(new OnnxInferenceEngine.MinedOreRecord(System.currentTimeMillis(), block.getType(), block.getLocation()));
      playerMiningState.lastOreLocation = block.getLocation();
   }

   private boolean isVeinMiningContinuation(Player player, Block block) {
      List<OnnxInferenceEngine.MinedOreRecord> oreHistory = this.recentOreHistory.get(player.getUniqueId());
      if (oreHistory != null && !oreHistory.isEmpty()) {
         long nowMs = System.currentTimeMillis();
         Location blockLocation = block.getLocation();
         Material blockType = block.getType();
         if (this.debugMode) {
            this.plugin.getLogger().info(String.format("[DL-Debug] 矿脉检测开始 - 玩家: %s, 矿物: %s, 历史记录: %d条", player.getName(), blockType, oreHistory.size()));
         }

         int consecutiveSameMaterialCount = 0;
         int maxConsecutiveSkips = this.plugin.getConfig().getInt("deeplearn.vein_detection.max_consecutive_skips", 8);

         for (int i = oreHistory.size() - 1; i >= 0 && consecutiveSameMaterialCount < maxConsecutiveSkips; i--) {
            OnnxInferenceEngine.MinedOreRecord minedOreRecord = oreHistory.get(i);
            if (!minedOreRecord.material.equals(blockType)) {
               break;
            }

            consecutiveSameMaterialCount++;
         }

         if (this.debugMode) {
            this.plugin.getLogger().info(String.format("[DL-Debug] 连续同类型挖掘计数: %d/%d", consecutiveSameMaterialCount, maxConsecutiveSkips));
         }

         if (consecutiveSameMaterialCount >= maxConsecutiveSkips) {
            if (this.debugMode) {
               this.plugin.getLogger().info(String.format("[DL-Debug] 连续挖掘 %s 达到 %d 次，强制进行AI检测", blockType, consecutiveSameMaterialCount));
            }

            return false;
         } else {
            int checkedCount = 0;

            for (OnnxInferenceEngine.MinedOreRecord record : oreHistory) {
               checkedCount++;
               long timeDeltaMs = nowMs - record.timestamp;
               if (timeDeltaMs > this.deeplearnVeinDetectionMaxTimeMs) {
                  if (this.debugMode && checkedCount <= 3) {
                     this.plugin.getLogger().info(String.format("[DL-Debug] 记录 %d: 超出时间窗口 (%dms > %dms)", checkedCount, timeDeltaMs, this.deeplearnVeinDetectionMaxTimeMs));
                  }
               } else if (!record.material.equals(blockType)) {
                  if (this.debugMode && checkedCount <= 3) {
                     this.plugin.getLogger().info(String.format("[DL-Debug] 记录 %d: 矿物类型不匹配 (%s != %s)", checkedCount, record.material, blockType));
                  }
               } else if (record.location != null) {
                  double euclideanDistance = record.location.distance(blockLocation);
                  double horizontalDistance = Math.sqrt(Math.pow(record.location.getX() - blockLocation.getX(), 2.0) + Math.pow(record.location.getZ() - blockLocation.getZ(), 2.0));
                  double verticalDistance = Math.abs(record.location.getY() - blockLocation.getY());
                  double maxVerticalDistance = this.plugin.getConfig().getDouble("deeplearn.vein_detection.max_vertical_distance", 3.0);
                  boolean withinHorizontalRange = horizontalDistance <= this.deeplearnVeinDetectionMaxDistance;
                  boolean withinVerticalRange = verticalDistance <= maxVerticalDistance;
                  boolean withinEuclideanRange = euclideanDistance <= this.deeplearnVeinDetectionMaxDistance;
                  if (this.debugMode && checkedCount <= 5) {
                     this.plugin
                        .getLogger()
                        .info(
                           String.format(
                              "[DL-Debug] 记录 %d: 3D=%.2f(≤%.1f), 水平=%.2f(≤%.1f), 垂直=%.2f(≤%.1f), 时间=%dms",
                              checkedCount,
                              euclideanDistance,
                              this.deeplearnVeinDetectionMaxDistance,
                              horizontalDistance,
                              this.deeplearnVeinDetectionMaxDistance,
                              verticalDistance,
                              maxVerticalDistance,
                              timeDeltaMs
                           )
                        );
                  }

                  if (withinHorizontalRange && withinVerticalRange && withinEuclideanRange) {
                     if (this.debugMode) {
                        this.plugin
                           .getLogger()
                           .info(String.format("[DL-Debug] ✅ 矿脉检测匹配 - 类型: %s, 3D距离: %.2f, 水平距离: %.2f, 垂直距离: %.2f, 时间差: %dms", blockType, euclideanDistance, horizontalDistance, verticalDistance, timeDeltaMs));
                     }

                     return true;
                  }
               }
            }

            if (this.debugMode) {
               this.plugin.getLogger().info(String.format("[DL-Debug] ❌ 非矿脉挖掘 - %s (检查了%d条记录)，将进行AI检测", blockType, checkedCount));
            }

            return false;
         }
      } else {
         if (this.debugMode) {
            this.plugin.getLogger().info(String.format("[DL-Debug] 矿脉检测 - 玩家 %s 无挖掘历史，判定为非矿脉", player.getName()));
         }

         return false;
      }
   }

   private int countRecentOreMined(UUID playerUuid, Material targetMaterial, long nowMs) {
      int matchedCount = 0;
      List<OnnxInferenceEngine.MinedOreRecord> oreHistory = this.recentOreHistory.get(playerUuid);
      if (oreHistory == null) {
         return 0;
      }

      oreHistory.removeIf(record -> this.isOreRecordExpired(nowMs, record));

      for (OnnxInferenceEngine.MinedOreRecord minedOreRecord : oreHistory) {
         if (minedOreRecord.material == targetMaterial) {
            matchedCount++;
         }
      }

      return matchedCount;
   }

   private float[] preprocessFeatures(Map featureMap) {
      try {
         if (this.featureStats != null
            && this.featureStats.numericalFeatures != null
            && this.featureStats.numericalMeans != null
            && this.featureStats.numericalStds != null) {
            List<String> numericalFeatures = this.featureStats.numericalFeatures;
            List<Double> numericalMeans = this.featureStats.numericalMeans;
            List<Double> numericalStds = this.featureStats.numericalStds;
            if (numericalFeatures.size() == numericalMeans.size() && numericalFeatures.size() == numericalStds.size()) {
               float[] normalizedVector = new float[numericalFeatures.size()];

               for (int i = 0; i < numericalFeatures.size(); i++) {
                  String featureName = numericalFeatures.get(i);
                  Object rawFeatureValue = featureMap.get(featureName);
                  if (rawFeatureValue == null) {
                     this.plugin.getLogger().warning("特征 '" + featureName + "' 在 featureMap 中未找到，使用默认值 0.0。");
                     normalizedVector[i] = 0.0F;
                  } else {
                     float rawFloat = ((Number)rawFeatureValue).floatValue();
                     double mean = numericalMeans.get(i);
                     double stdDev = numericalStds.get(i);
                     if (stdDev == 0.0) {
                        normalizedVector[i] = 0.0F;
                     } else {
                        normalizedVector[i] = (float)((rawFloat - mean) / stdDev);
                     }
                  }
               }

               return normalizedVector;
            } else {
               this.plugin.getLogger().warning("feature_stats.json 中数值特征、均值和标准差的数量不匹配。");
               return null;
            }
         } else {
            this.plugin.getLogger().warning("feature_stats.json 数据不完整，无法进行预处理。");
            return null;
         }
      } catch (Exception ex) {
         this.plugin.getLogger().warning("预处理玩家数据时发生错误: " + ex.getMessage());
         this.plugin.getLogger().warning("错误详情: " + ex.toString());
         return null;
      }
   }

   private void runInference(Player player, LinkedList featureSequence) {
      int featureCount = ((float[])featureSequence.getFirst()).length;
      float[][][] inputTensorData = new float[1][this.deeplearnSequenceLength][featureCount];

      for (int i = 0; i < this.deeplearnSequenceLength; i++) {
         inputTensorData[0][i] = (float[])featureSequence.get(i);
      }

      long[] inputShape = new long[]{1L, this.deeplearnSequenceLength, featureCount};

      try (
         OnnxTensor onnxTensor = OnnxTensor.createTensor(this.ortEnvironment, FloatBuffer.wrap(this.flattenTensor(inputTensorData)), inputShape);
         Result result = this.session.run(Collections.singletonMap("input", onnxTensor));
      ) {
         float[][] outputLogits = (float[][])result.get(0).getValue();
         float logit = outputLogits[0][0];
         float probability = (float)(1.0 / (1.0 + Math.exp(-logit)));
         if (this.debugMode) {
            Logger logger = this.plugin.getLogger();
            String playerName = player.getName();
            String formattedLogit = String.format("%.4f", logit);
            String formattedProbability = String.format("%.4f", probability);
            logger.info("[DL-Debug] " + playerName + " -> Inference Result | Logit: " + formattedLogit + ", Probability: " + formattedProbability + ", Threshold: " + this.deeplearnPredictionThreshold);
         }

         if (probability > this.deeplearnPredictionThreshold) {
            if (this.debugMode) {
               this.plugin.getLogger().info("[DL-Debug] " + player.getName() + " -> DETECTED! Probability exceeds threshold.");
            }

            boolean punishmentEnabled = this.plugin.getConfig().getBoolean("deeplearn.violations.enable-punishment", true);
            if (punishmentEnabled) {
               this.applyInferenceViolation(player, probability);
            } else {
               this.plugin.getLogger().info(String.format("[AI检测] 玩家 %s 触发AI警报但未执行处罚 (概率: %.2f)", player.getName(), probability));
            }

            this.plugin.getFoliaScheduler().getImpl().runNextTick(wrappedTask -> {
               String alertMessage = "§c[NAX-DL] §7玩家 " + player.getName() + " 的行为模式触发了AI警报 (P=" + String.format("%.2f", probability) + ")";

               for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                  if (onlinePlayer.hasPermission("nekoantixray.admin")) {
                     onlinePlayer.sendMessage(alertMessage);
                  }
               }

               this.plugin.getLogger().info("[NAX-DL] " + alertMessage);
            });
         }
      } catch (OrtException ex) {
         this.plugin.getLogger().warning("模型推理时发生错误: " + ex.getMessage());
      }
   }

   private void applyInferenceViolation(Player player, float probability) {
      try {
         double vlMultiplier = this.plugin.getConfig().getDouble("deeplearn.violations.vl-multiplier", 10.0);
         int minVlIncrease = this.plugin.getConfig().getInt("deeplearn.violations.min-vl-increase", 1);
         int maxVlIncrease = this.plugin.getConfig().getInt("deeplearn.violations.max-vl-increase", 15);
         double probabilityExcess = probability - this.deeplearnPredictionThreshold;
         int rawVlIncrease = (int)Math.ceil(probabilityExcess * vlMultiplier);
         int clampedVlIncrease = Math.max(minVlIncrease, Math.min(maxVlIncrease, rawVlIncrease));
         if (this.debugMode) {
            this.plugin.getLogger().info(String.format("[DL-Debug] AI检测 - 玩家: %s, 概率: %.3f, 概率差: %.3f, VL增加: %d", player.getName(), probability, probabilityExcess, clampedVlIncrease));
         }

         if (this.plugin.getMiningBehaviorDetector() != null) {
            MiningBehaviorDetector miningBehaviorDetector = this.plugin.getMiningBehaviorDetector();
            miningBehaviorDetector.addDeepLearningViolation(player, probability);
         } else {
            String reason = String.format("AI检测到可疑挖矿行为 (概率: %.2f)", probability);
            this.plugin.logDetectionEvent(player, "DeepLearning-AI", clampedVlIncrease, reason);
            if (this.debugMode) {
               this.plugin.getLogger().info(String.format("[DL-Debug] 独立AI检测 - 玩家 %s VL记录已保存", player.getName()));
            }
         }
      } catch (Exception ex) {
         this.plugin.getLogger().warning("处理AI检测结果时发生错误: " + ex.getMessage());
         if (this.debugMode) {
            ex.printStackTrace();
         }
      }
   }

   private float[] flattenTensor(float[][][] tensor) {
      int batchSize = tensor.length;
      int sequenceLength = tensor[0].length;
      int featureCount = tensor[0][0].length;
      float[] flattened = new float[batchSize * sequenceLength * featureCount];
      int writeIndex = 0;

      for (float[][] sequenceSlice : tensor) {
         for (float[] featureRow : sequenceSlice) {
            for (float element : featureRow) {
               flattened[writeIndex++] = element;
            }
         }
      }

      return flattened;
   }

   public void shutdown() {
      if (this.session != null) {
         try {
            this.session.close();
         } catch (OrtException ex) {
            this.plugin.getLogger().warning("关闭ONNX会话时发生错误: " + ex.getMessage());
         }
      }

      if (this.ortEnvironment != null) {
         this.ortEnvironment.close();
      }
   }

   public OnnxInferenceEngine(NekoAntiXray plugin) {
      this.plugin = plugin;
      boolean deepLearnEnabled = plugin.getConfig().getBoolean("deeplearn.enable", false);
      if (!deepLearnEnabled) {
         plugin.getLogger().info("深度学习检测模块已在配置中禁用。");
         this.initializeDisabledDefaults();
      } else {
         this.loadConfiguration();
         boolean statsLoaded = this.loadFeatureStats();
         boolean modelLoaded = this.loadOnnxModel();
         if (statsLoaded && modelLoaded) {
            plugin.getLogger().info("✅ 深度学习检测模块已准备就绪！AI反作弊系统已启动。");
            if (this.debugMode) {
               Logger logger = plugin.getLogger();
               logger.info("[DL-Debug] 模型配置 - 序列长度: " + this.deeplearnSequenceLength + ", 阈值: " + this.deeplearnPredictionThreshold);
            }

            this.isReady = true;
         } else {
            plugin.getLogger().warning("⚠️  深度学习检测模块启动失败:");
            if (!statsLoaded) {
               plugin.getLogger().warning("   - feature_stats.json 文件加载失败");
            }

            if (!modelLoaded) {
               plugin.getLogger().warning("   - model.onnx 文件加载失败");
            }

            plugin.getLogger().warning("   请确保模型文件正确放置在插件目录中。AI检测将被禁用。");
            plugin.getLogger().info("   深度学习模块将继续运行以防止错误，但不会执行AI推理。");
            this.isReady = false;
         }
      }
   }

   private static class FeatureStats {
      List<String> numericalFeatures;
      List<Double> numericalMeans;
      List<Double> numericalStds;
      List<String> categoricalFeatures;
      List<List<String>> categoricalCategories;
      List<String> featureNames;
      Map<String, Map<String, Double>> featureStatistics;

      @Generated
      public List getNumericalFeatures() {
         return this.numericalFeatures;
      }

      @Generated
      public List getNumericalMeans() {
         return this.numericalMeans;
      }

      @Generated
      public List getNumericalStds() {
         return this.numericalStds;
      }

      @Generated
      public List getCategoricalFeatures() {
         return this.categoricalFeatures;
      }

      @Generated
      public List getCategoricalCategories() {
         return this.categoricalCategories;
      }

      @Generated
      public List getFeatureNames() {
         return this.featureNames;
      }

      @Generated
      public Map getFeatureStatistics() {
         return this.featureStatistics;
      }

      public void setNumericalFeatures(List numericalFeatures) {
         this.numericalFeatures = numericalFeatures;
      }

      public void setNumericalMeans(List numericalMeans) {
         this.numericalMeans = numericalMeans;
      }

      public void setNumericalStds(List numericalStds) {
         this.numericalStds = numericalStds;
      }

      public void setCategoricalFeatures(List categoricalFeatures) {
         this.categoricalFeatures = categoricalFeatures;
      }

      public void setCategoricalCategories(List categoricalCategories) {
         this.categoricalCategories = categoricalCategories;
      }

      public void setFeatureNames(List featureNames) {
         this.featureNames = featureNames;
      }

      public void setFeatureStatistics(Map featureStatistics) {
         this.featureStatistics = featureStatistics;
      }
   }

   private static class MinedOreRecord {
      final long timestamp;
      final Material material;
      final Location location;

      @Generated
      public long getTimestamp() {
         return this.timestamp;
      }

      @Generated
      public Material getMaterial() {
         return this.material;
      }

      @Generated
      public Location getLocation() {
         return this.location;
      }

      @Generated
      public MinedOreRecord(long timestamp, Material material, Location location) {
         this.timestamp = timestamp;
         this.material = material;
         this.location = location;
      }
   }

   private static class PlayerMiningState {
      Location lastOreLocation = null;

      @Generated
      public Location getLastOreLocation() {
         return this.lastOreLocation;
      }

      public void setLastOreLocation(Location lastOreLocation) {
         this.lastOreLocation = lastOreLocation;
      }
   }
}
