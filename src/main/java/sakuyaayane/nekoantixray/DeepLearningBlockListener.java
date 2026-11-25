package sakuyaayane.nekoantixray;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class DeepLearningBlockListener implements Listener {
   private final XrayDetector xrayDetector;
   private final MiningBehaviorDetector miningBehaviorDetector;
   private final ConcurrentHashMap<UUID, CompletableFuture<Void>> playerInferenceFutures = new ConcurrentHashMap<>();

   public DeepLearningBlockListener(NekoAntiXray plugin) {
      this.xrayDetector = plugin.getXrayDetector();
      this.miningBehaviorDetector = plugin.getMiningBehaviorDetector();
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onBlockBreak(BlockBreakEvent event) {
      long nowNanos = System.nanoTime();

      try {
         Player player = event.getPlayer();
         if (player != null) {
            this.xrayDetector.processPendingDecoyPlacement(player);
         }

         Block block = event.getBlock();
         if (!OreMaterialMatcher.isOreMaterial(block.getType())) {
            return;
         }

         NekoAntiXray plugin = NekoAntiXray.getInstance();
         plugin.logDebug("BlockBreakEvent 触发，玩家: " + player.getName() + ", 方块: " + block.getType());

         try {
            boolean hasBypassPermission = player.hasPermission("nekoantixray.bypass");
            boolean hasInternalBypass = plugin.getInternalBypassManager() != null && plugin.getInternalBypassManager().hasBypass(player);
            if (hasBypassPermission || hasInternalBypass) {
               if (this.xrayDetector.isFakeOre(block)) {
                  this.xrayDetector.processFakeOreBreak(player, block);
               }

               return;
            }
         } catch (Throwable ex) {
         }

         if (this.xrayDetector.isFakeOre(block)) {
            this.xrayDetector.processFakeOreBreak(player, block);
            return;
         }

         this.xrayDetector.checkXRay(player, block);
         this.miningBehaviorDetector.handleBlockBreak(player, block);
         boolean deepLearnDebug = plugin.getConfig().getBoolean("deeplearn.debug", false);
         boolean deepLearnEnabled = plugin.getConfig().getBoolean("deeplearn.enable", false);
         if (!deepLearnEnabled) {
            if (deepLearnDebug) {
               plugin.logDebug("[DL-Debug] 深度学习模块已在配置中禁用，跳过AI检测");
            }

            return;
         }

         OnnxInferenceEngine onnxInferenceEngine = plugin.getOnnxInferenceEngine();
         if (onnxInferenceEngine != null) {
            this.runDeepLearnAsync(plugin, onnxInferenceEngine, player, block, deepLearnDebug);
            return;
         }

         if (deepLearnDebug) {
            plugin.getLogger().warning("[DL-Debug] InferenceManager为null，AI检测模块可能初始化失败");
         }
      } finally {
         ModuleTimingStatistics.record("主线程-挖矿检测", System.nanoTime() - nowNanos);
      }
   }

   @EventHandler
   public void onBlockPlace(BlockPlaceEvent event) {
      Block block = event.getBlock();
      if (OreMaterialMatcher.isOreMaterial(block.getType())) {
         this.xrayDetector.addPlayerPlacedOre(block.getLocation());
         FakeOreProtectionListener.trackOreLocation(block.getLocation());
         NekoAntiXray plugin = NekoAntiXray.getInstance();
         String playerName = event.getPlayer().getName();
         String blockType = String.valueOf(block.getType());
         String blockLocation = String.valueOf(block.getLocation());
         plugin.logDebug("玩家 " + playerName + " 放置了矿石: " + blockType + " 在位置: " + blockLocation);
         this.miningBehaviorDetector.handleBlockPlace(event.getPlayer(), block);
      }
   }

   @EventHandler
   public void onBlockExplode(BlockExplodeEvent event) {
      for (Block block : event.blockList()) {
         if (this.xrayDetector.isFakeOre(block)) {
            block.setType(Material.STONE);
         }
      }

      this.miningBehaviorDetector.handleExplosion(event.blockList(), null);
   }

   @EventHandler
   public void onEntityExplode(EntityExplodeEvent event) {
      ArrayList<Block> explodedBlocks = new ArrayList<>(event.blockList());
      this.miningBehaviorDetector.handleExplosion(explodedBlocks, event.getEntity());
   }

   @EventHandler
   public void onPistonExtend(BlockPistonExtendEvent event) {
      for (Block block : event.getBlocks()) {
         if (this.xrayDetector.isFakeOre(block)) {
            event.setCancelled(true);
            return;
         }
      }
   }

   @EventHandler
   public void onPistonRetract(BlockPistonRetractEvent event) {
      for (Block block : event.getBlocks()) {
         if (this.xrayDetector.isFakeOre(block)) {
            event.setCancelled(true);
            return;
         }
      }
   }

   private void removeCompletedInferenceFuture(UUID uuid, CompletableFuture future) {
      this.playerInferenceFutures.remove(uuid, future);
   }

   private static CompletionStage dispatchAsyncInference(NekoAntiXray plugin, OnnxInferenceEngine onnxInferenceEngine, Player player, Block block, boolean debugEnabled) {
      CompletableFuture<Void> future = new CompletableFuture<>();
      plugin.getFoliaScheduler().getImpl().runAsync(wrappedTask -> {
         try {
            onnxInferenceEngine.onPlayerBlockBreak(player, block);
         } catch (Exception ex) {
            plugin.logDebug("深度学习检测调用异常: " + ex.getMessage());
            if (debugEnabled) {
               plugin.logDebug("[DL-Debug] 异常堆栈:");
               ex.printStackTrace();
            }
         } finally {
            future.complete(null);
         }
      });
      return future;
   }

   private static Object swallowPreviousInferenceError() {
      return null;
   }

   private void runDeepLearnAsync(NekoAntiXray plugin, OnnxInferenceEngine onnxInferenceEngine, Player player, Block block, boolean debugEnabled) {
      UUID playerId = player.getUniqueId();
      this.playerInferenceFutures
         .compute(
            playerId,
            (playerKey, existingFuture) -> {
               CompletableFuture previousFuture = existingFuture == null ? CompletableFuture.completedFuture(null) : existingFuture;
               CompletableFuture nextFuture = previousFuture.<Object>handle((ignoredResult, previousError) -> swallowPreviousInferenceError())
                  .thenCompose(ignored -> dispatchAsyncInference(plugin, onnxInferenceEngine, player, block, debugEnabled));
               nextFuture.whenComplete((completedResult, completionError) -> this.removeCompletedInferenceFuture(playerKey, nextFuture));
               return nextFuture;
            }
         );
   }
}
