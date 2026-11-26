package sakuyaayane.nekoantixray.detection.ai;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.Material;
import sakuyaayane.nekoantixray.NekoAntiXray;

public class TrainingDataRecorder implements Listener {
   private static final Map<String, String> OBFUSCATED_KEY_MAP = new HashMap<String, String>() {
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
   private final Map<UUID, String> recordingPlayerLabels = new ConcurrentHashMap<>();
   private final File outputDirectory;
   private Set<Material> valuableMaterials;

   @EventHandler
   public void onBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      Block block = event.getBlock();
      if (this.isRecordingPlayer(player)) {
         if (!this.isValuableBlock(block)) {
            if (this.plugin.getConfig().getBoolean("deeplearn.debug", false)) {
               this.plugin.getLogger().info("[Recording] Skipping non-valuable block: " + block.getType());
            }
         } else {
            if (this.plugin.getConfig().getBoolean("deeplearn.debug", false)) {
               this.plugin.getLogger().info("[Recording] Processing valuable block: " + block.getType() + " for player: " + player.getName());
            }

            OnnxInferenceEngine onnxInferenceEngine = this.plugin.getOnnxInferenceEngine();
            Map featureMap = null;
            if (onnxInferenceEngine != null) {
               featureMap = onnxInferenceEngine.onPlayerBlockBreak(player, block);
            }

            try {
               if (featureMap != null) {
                  if (this.plugin.getConfig().getBoolean("deeplearn.debug", false)) {
                     this.plugin.getLogger().info("[Recording] Using feature map from InferenceManager");
                  }

                  this.appendTrainingRecord(player, featureMap);
               } else if (this.plugin.getConfig().getBoolean("deeplearn.debug", false)) {
                  this.plugin.getLogger().info("[Recording] InferenceManager returned null, skipping data recording");
               }
            } catch (IOException ex) {
               this.plugin.getLogger().severe("无法为深度学习记录玩家数据: " + player.getName());
               this.plugin.getLogger().severe("错误详情: " + ex.toString());
            }
         }
      }
   }

   private Material parseValuableMaterial(String materialName) {
      try {
         Material material = Material.valueOf(materialName.toUpperCase());
         if (this.plugin.getConfig().getBoolean("deeplearn.debug", false)) {
            this.plugin.getLogger().info("[Recording] Successfully loaded material: " + material);
         }

         return material;
      } catch (IllegalArgumentException ex) {
         this.plugin.getLogger().warning("[Recording] 配置中的材料名称无效: " + materialName);
         return null;
      }
   }

   private String obfuscateKey(String key) {
      return OBFUSCATED_KEY_MAP.getOrDefault(key, key);
   }

   private void loadValuableMaterials() {
      List<String> configuredMaterials = this.plugin.getConfig().getStringList("deeplearn.valuable_materials");
      if (configuredMaterials != null && !configuredMaterials.isEmpty()) {
         if (this.plugin.getConfig().getBoolean("deeplearn.debug", false)) {
            this.plugin.getLogger().info("[Recording] Loading valuable materials from config: " + configuredMaterials);
         }

         this.valuableMaterials = configuredMaterials.stream()
            .map(this::parseValuableMaterial)
            .filter(Objects::nonNull)
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
         if (this.plugin.getConfig().getBoolean("deeplearn.debug", false)) {
            this.plugin.getLogger().info("[Recording] Using default valuable materials list");
         }
      }

      if (this.plugin.getConfig().getBoolean("deeplearn.debug", false)) {
         this.plugin.getLogger().info("[Recording] Loaded valuable materials: " + this.valuableMaterials);
      }
   }

   private boolean isValuableBlock(Block block) {
      return this.valuableMaterials != null && this.valuableMaterials.contains(block.getType());
   }

   public boolean isRecordingPlayer(Player player) {
      return this.recordingPlayerLabels.containsKey(player.getUniqueId());
   }

   public void startRecording(Player player, String label) {
      this.recordingPlayerLabels.put(player.getUniqueId(), label);
   }

   public void stopRecording(Player player) {
      this.recordingPlayerLabels.remove(player.getUniqueId());
   }

   private void appendTrainingRecord(Player player, Map featureMap) throws IOException {
      String label = this.recordingPlayerLabels.getOrDefault(player.getUniqueId(), "unknown");
      String obfuscatedLabel = this.obfuscateKey(label);
      File outputFile = new File(this.outputDirectory, "recording_" + player.getName() + "_" + label + ".csv");
      boolean fileExists = outputFile.exists();
      if (this.plugin.getConfig().getBoolean("deeplearn.debug", false)) {
         this.plugin.getLogger().info("[Recording] Logging data for " + player.getName() + " with features: " + featureMap);
         this.plugin.getLogger().info("[Recording] Original label: " + label + ", Encrypted label: " + obfuscatedLabel);
      }

      try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile, true))) {
         String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
         UUID playerId = player.getUniqueId();
         String playerName = player.getName();
         if (!fileExists) {
            String header = "timestamp,player_uuid,player_name," + String.join(",", featureMap.keySet()) + "," + this.obfuscateKey("label");
            writer.println(header);
            if (this.plugin.getConfig().getBoolean("deeplearn.debug", false)) {
               this.plugin.getLogger().info("[Recording] Created new file with header: " + header);
            }
         }

         String featureValues = (String)featureMap.values().stream().map(featureValue -> String.valueOf(featureValue)).collect(Collectors.joining(","));
         String csvLine = String.format("%s,%s,%s,%s,%s", timestamp, playerId.toString(), playerName, featureValues, obfuscatedLabel);
         writer.println(csvLine);
         if (this.plugin.getConfig().getBoolean("deeplearn.debug", false)) {
            this.plugin.getLogger().info("[Recording] Logged data line: " + csvLine);
         }
      }
   }

   public TrainingDataRecorder(NekoAntiXray plugin) {
      this.plugin = plugin;
      this.outputDirectory = new File(plugin.getDataFolder(), "training_data");
      if (!this.outputDirectory.exists()) {
         this.outputDirectory.mkdirs();
      }

      this.loadValuableMaterials();
   }
}
