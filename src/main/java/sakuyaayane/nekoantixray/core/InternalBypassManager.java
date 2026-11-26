package sakuyaayane.nekoantixray.core;

import java.time.chrono.ChronoLocalDateTime;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import sakuyaayane.nekoantixray.config.ConfigManager;
import sakuyaayane.nekoantixray.lib.folialib.FoliaLib;
import sakuyaayane.nekoantixray.NekoAntiXray;

public class InternalBypassManager {
   private final NekoAntiXray plugin;
   private final FoliaLib foliaLib;
   private final ConfigManager configManager;
   private final Map<UUID, LocalDateTime> bypassExpirationTimes = new ConcurrentHashMap<>();
   private final Map<UUID, String> bypassReasons = new ConcurrentHashMap<>();
   private static final long DEFAULT_BYPASS_DURATION_SECONDS = 180L;

   public void grantBypass(Player player, long durationSeconds, String reason) {
      if (player != null && player.isOnline()) {
         UUID playerId = player.getUniqueId();
         LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(durationSeconds);
         this.bypassExpirationTimes.put(playerId, expirationTime);
         this.bypassReasons.put(playerId, reason);
         String debugMessage = String.format(
            "InternalBypassManager: 给予玩家 %s 内部检测豁免 - 持续时间: %d秒, 原因: %s, 截止时间: %s", player.getName(), durationSeconds, reason, expirationTime
         );
         this.plugin.logDebug(debugMessage);
         if (this.configManager.isCheckXrayMassExcavationNotificationEnabled()) {
            String notificationMessage = this.configManager
               .getCheckXrayMassExcavationNotificationMessage()
               .replace("%duration%", String.valueOf(durationSeconds / 60L))
               .replace("%reason%", reason);
            player.sendMessage(notificationMessage);
         }

         this.scheduleBypassRemoval(playerId, durationSeconds);
      } else {
         this.plugin.logDebug("InternalBypassManager: 无法给予豁免 - 玩家为null或不在线");
      }
   }

   public void grantMassExcavationBypass(Player player, String reason) {
      long durationSeconds = this.configManager.getCheckXrayMassExcavationBypassDuration();
      this.grantBypass(player, durationSeconds, reason);
   }

   public boolean hasBypass(Player player) {
      return player == null ? false : this.hasBypass(player.getUniqueId());
   }

   public boolean hasBypass(UUID playerId) {
      LocalDateTime expirationTime = this.bypassExpirationTimes.get(playerId);
      if (expirationTime == null) {
         return false;
      }

      LocalDateTime now = LocalDateTime.now();
      boolean isActive = now.isBefore(expirationTime);
      if (!isActive) {
         this.removeBypass(playerId);
      }

      return isActive;
   }

   public void removeBypass(Player player) {
      if (player != null) {
         this.removeBypass(player.getUniqueId());
      }
   }

   public void removeBypass(UUID playerId) {
      LocalDateTime removedExpiration = this.bypassExpirationTimes.remove(playerId);
      String removedReason = this.bypassReasons.remove(playerId);
      if (removedExpiration != null) {
         String debugMessage = String.format(
            "InternalBypassManager: 移除玩家 %s 的内部检测豁免 - 原因: %s",
            this.plugin.getServer().getOfflinePlayer(playerId).getName(),
            removedReason != null ? removedReason : "未知"
         );
         this.plugin.logDebug(debugMessage);
      }
   }

   public long getRemainingBypassSeconds(Player player) {
      if (player == null) {
         return 0L;
      }

      LocalDateTime expirationTime = this.bypassExpirationTimes.get(player.getUniqueId());
      if (expirationTime == null) {
         return 0L;
      }

      LocalDateTime now = LocalDateTime.now();
      return now.isBefore(expirationTime) ? ChronoUnit.SECONDS.between(now, expirationTime) : 0L;
   }

   public String getBypassReason(Player player) {
      return player == null ? null : this.bypassReasons.get(player.getUniqueId());
   }

   public boolean extendBypass(Player player, long extraSeconds) {
      if (player == null) {
         return false;
      } else {
         UUID playerId = player.getUniqueId();
         LocalDateTime currentExpiration = this.bypassExpirationTimes.get(playerId);
         if (currentExpiration == null) {
            this.plugin.logDebug("InternalBypassManager: 无法延长豁免 - 玩家当前没有有效的豁免");
            return false;
         } else {
            LocalDateTime newExpiration = currentExpiration.plusSeconds(extraSeconds);
            this.bypassExpirationTimes.put(playerId, newExpiration);
            this.plugin.logDebug(String.format("InternalBypassManager: 延长玩家 %s 的豁免时间 %d秒 - 新截止时间: %s", player.getName(), extraSeconds, newExpiration));
            return true;
         }
      }
   }

   public int getActiveBypassCount() {
      this.cleanupExpiredBypasses();
      return this.bypassExpirationTimes.size();
   }

   public void clearAllBypasses() {
      int clearedCount = this.bypassExpirationTimes.size();
      this.bypassExpirationTimes.clear();
      this.bypassReasons.clear();
      this.plugin.logDebug(String.format("InternalBypassManager: 已清理所有豁免记录，共 %d 个", clearedCount));
   }

   public void clearPlayerData(UUID playerId) {
      this.removeBypass(playerId);
   }

   private void scheduleBypassRemoval(UUID playerId, long durationSeconds) {
      this.foliaLib.getImpl().runTimer(wrappedTask -> {
         LocalDateTime expirationTime = this.bypassExpirationTimes.get(playerId);
         if (expirationTime != null && LocalDateTime.now().isAfter(expirationTime)) {
            this.removeBypass(playerId);
            Player player = this.plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
               this.plugin.logDebug(String.format("InternalBypassManager: 玩家 %s 的检测豁免已自动过期", player.getName()));
            }
         }
      }, durationSeconds * 20L, durationSeconds * 20L);
   }

   private void setupCleanupTask() {
      long cleanupIntervalTicks = 6000L;
      this.foliaLib.getImpl().runTimer(wrappedTask -> this.cleanupExpiredBypasses(), cleanupIntervalTicks, cleanupIntervalTicks);
   }

   private void cleanupExpiredBypasses() {
      LocalDateTime now = LocalDateTime.now();
      Iterator<Entry<UUID, LocalDateTime>> iterator = this.bypassExpirationTimes.entrySet().iterator();
      int cleanedCount = 0;

      while (iterator.hasNext()) {
         Entry<UUID, LocalDateTime> entry = iterator.next();
         if (now.isAfter(entry.getValue())) {
            UUID playerId = entry.getKey();
            iterator.remove();
            this.bypassReasons.remove(playerId);
            cleanedCount++;
         }
      }

      if (cleanedCount > 0) {
         this.plugin.logDebug(String.format("InternalBypassManager: 定期清理完成，清理了 %d 个过期的豁免记录", cleanedCount));
      }
   }

   public Map getActiveBypassSummaries() {
      Map<String, String> summaries = new HashMap<>();
      LocalDateTime now = LocalDateTime.now();

      for (Entry<UUID, LocalDateTime> entry : this.bypassExpirationTimes.entrySet()) {
         UUID playerId = entry.getKey();
         LocalDateTime expirationTime = entry.getValue();
         if (now.isBefore(expirationTime)) {
            String playerName = this.plugin.getServer().getOfflinePlayer(playerId).getName();
            String reason = this.bypassReasons.getOrDefault(playerId, "未知原因");
            long remainingSeconds = ChronoUnit.SECONDS.between(now, expirationTime);
            summaries.put(playerName != null ? playerName : playerId.toString(), String.format("原因: %s, 剩余: %d秒", reason, remainingSeconds));
         }
      }

      return summaries;
   }

   public InternalBypassManager(NekoAntiXray plugin, FoliaLib foliaLib) {
      this.plugin = plugin;
      this.foliaLib = foliaLib;
      this.configManager = plugin.getConfigManager();
      this.setupCleanupTask();
   }
}
