package sakuyaayane.nekoantixray.replay;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import sakuyaayane.nekoantixray.NekoAntiXray;

public class ReplayManager implements Listener {
   private final NekoAntiXray plugin;
   private final Map<UUID, List<ReplayFrame>> replayData;
   private final int recordDuration;

   public ReplayManager(NekoAntiXray plugin) {
      this.plugin = plugin;
      this.replayData = new HashMap<>();
      this.recordDuration = plugin.getConfigManager().getReplayRecordDuration();
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   @EventHandler
   public void onPlayerMove(PlayerMoveEvent event) {
      this.recordFrame(event.getPlayer(), "MOVE", event.getTo(), null);
   }

   @EventHandler
   public void onBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      Block block = event.getBlock();
      Location location = block.getLocation();
      Material type = block.getType();
      this.recordFrame(player, "BREAK", location, type);
   }

   private void recordFrame(Player player, String actionType, Location location, Material material) {
      if (player != null && location != null && actionType != null) {
         UUID playerUuid = player.getUniqueId();
         if (playerUuid == null) {
            this.plugin.getLogger().warning("ReplayManager.recordFrame: 玩家UUID为空");
         } else {
            try {
               ReplayFrame replayFrame = new ReplayFrame(System.currentTimeMillis(), actionType, location, material);
               if (location.getWorld() != null) {
                  replayFrame.worldName = location.getWorld().getName();
               }

               List<ReplayFrame> frames = this.replayData.computeIfAbsent(playerUuid, ignoredUuid -> new ArrayList<>());
               frames.add(replayFrame);
               long cutoffTimeMs = System.currentTimeMillis() - this.recordDuration * 1000L;
               frames.removeIf(frame -> isExpiredFrame(cutoffTimeMs, frame));
            } catch (Exception ex) {
               this.plugin.getLogger().severe("ReplayManager.recordFrame: 记录回放帧时发生异常: " + ex.getMessage());
               ex.printStackTrace();
            }
         }
      } else {
         this.plugin.getLogger().warning("ReplayManager.recordFrame: 接收到空参数");
      }
   }

   public void saveReplay(Player player) {
      UUID playerUuid = player.getUniqueId();
      List<ReplayFrame> frames = this.replayData.get(playerUuid);
      if (frames != null && !frames.isEmpty()) {
         File replayDir = new File(this.plugin.getDataFolder(), "replays");
         if (!replayDir.exists()) {
            replayDir.mkdirs();
         }

         File replayFile = new File(replayDir, player.getName() + "_" + System.currentTimeMillis() + ".replay");

         try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(replayFile))) {
            objectOutputStream.writeObject(frames);
            this.plugin.getLogger().info("已保存玩家 " + player.getName() + " 的回放文件");
         } catch (IOException ex) {
            this.plugin.getLogger().severe("保存回放文件时出错: " + ex.getMessage());
         }

         this.replayData.remove(playerUuid);
      }
   }

   public void cleanupAllReplays() {
      try {
         long cutoffTimeMs = System.currentTimeMillis() - this.recordDuration * 1000L;
         this.replayData.entrySet().removeIf(entry -> {
            List<ReplayFrame> frames = entry.getValue();
            if (frames == null) {
               return true;
            }

            frames.removeIf(frame -> isExpiredFrameForCleanup(cutoffTimeMs, frame));
            return frames.isEmpty();
         });
         this.plugin.getLogger().info("ReplayManager: 已清理所有回放数据");
      } catch (Exception ex) {
         this.plugin.getLogger().severe("ReplayManager.cleanupAllReplays: 清理回放数据时发生异常: " + ex.getMessage());
         ex.printStackTrace();
      }
   }

   private static boolean isExpiredFrameForCleanup(long cutoffTimeMs, ReplayFrame replayFrame) {
      return replayFrame == null || replayFrame.timestamp < cutoffTimeMs;
   }

   private static boolean isExpiredFrame(long cutoffTimeMs, ReplayFrame replayFrame) {
      return replayFrame == null || replayFrame.timestamp < cutoffTimeMs;
   }
}