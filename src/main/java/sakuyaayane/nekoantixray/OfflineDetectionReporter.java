package sakuyaayane.nekoantixray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class OfflineDetectionReporter {
   private static final String EVENTS_LOG_FILE_NAME = "offline-report-events.log";
   private static final String LAST_SEEN_FILE_NAME = "offline-report-lastseen.yml";
   private static final String FIELD_DELIMITER = "|";
   private final NekoAntiXray plugin;
   private final List<String> events = new ArrayList<>();
   private final Map<String, Long> lastSeenTimestamps = new ConcurrentHashMap<>();
   private File eventsLogFile;
   private File lastSeenFile;
   private int maxEntries;

   public OfflineDetectionReporter(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   private static int[] createDetectionCounter() {
      return new int[1];
   }

   private static int[] createBanCounter() {
      return new int[1];
   }

   public void initialize() {
      this.maxEntries = Math.max(100, this.plugin.getConfigManager().getNotificationSystemOfflineReportMaxEntries());
      File logsDir = new File(this.plugin.getDataFolder(), "logs");
      if (!logsDir.exists()) {
         logsDir.mkdirs();
      }

      this.eventsLogFile = new File(logsDir, "offline-report-events.log");
      this.lastSeenFile = new File(logsDir, "offline-report-lastseen.yml");
      if (this.eventsLogFile.exists()) {
         String eventLine;
         try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.eventsLogFile), StandardCharsets.UTF_8))) {
            while ((eventLine = reader.readLine()) != null) {
               if (!eventLine.isBlank()) {
                  this.events.add(eventLine);
               }
            }
         } catch (IOException ignored) {
         }

         this.trimEvents();
      }

      if (this.lastSeenFile.exists()) {
         YamlConfiguration lastSeenConfig = YamlConfiguration.loadConfiguration(this.lastSeenFile);

         for (String playerName : lastSeenConfig.getKeys(false)) {
            this.lastSeenTimestamps.put(playerName, lastSeenConfig.getLong(playerName, 0L));
         }
      }
   }

   public void recordDetection(Player player, String reason, double violationLevel) {
      if (player != null && this.isEnabled()) {
         long nowMs = System.currentTimeMillis();
         String sanitizedPlayer = sanitizeField(player.getName());
         String sanitizedReason = sanitizeField(reason);
         String formattedVl = String.format(Locale.ROOT, "%.1f", violationLevel);
         this.appendEventLine(nowMs + "|" + sanitizedPlayer + "|" + sanitizedReason + "|" + formattedVl + "|DETECT");
      }
   }

   public void recordBan(Player player, String category, String reason) {
      if (player != null && this.isEnabled()) {
         long nowMs = System.currentTimeMillis();
         String sanitizedPlayer = sanitizeField(player.getName());
         String sanitizedCategory = sanitizeField(category);
         String sanitizedReason = sanitizeField(reason);
         this.appendEventLine(nowMs + "|" + sanitizedPlayer + "|" + sanitizedCategory + "|" + sanitizedReason + "|BAN");
      }
   }

   private void trimEvents() {
      if (this.events.size() > this.maxEntries) {
         this.events.subList(0, this.events.size() - this.maxEntries).clear();
      }
   }

   private void saveLastSeenTimestamps() {
      try {
         YamlConfiguration lastSeenConfig = new YamlConfiguration();

         for (Entry<String, Long> entry : this.lastSeenTimestamps.entrySet()) {
            lastSeenConfig.set(entry.getKey(), entry.getValue());
         }

         lastSeenConfig.save(this.lastSeenFile);
      } catch (IOException ex) {
         this.plugin.getLogger().warning("保存最后在线时间失败: " + ex.getMessage());
      }
   }

   public void sendOfflineReportOnJoin(Player player) {
      if (player != null && this.isEnabled()) {
         boolean canViewReport = player.hasPermission("nekoantixray.notify") || player.hasPermission("nekoantixray.alerts") || player.hasPermission("nekoantixray.admin");
         if (canViewReport) {
            Long lastSeenMs = this.lastSeenTimestamps.get(player.getName());
            if (lastSeenMs == null) {
               this.markPlayerLastSeen(player);
            } else {
               long nowMs = System.currentTimeMillis();
               this.showCompactReport(player, nowMs - lastSeenMs, "你离线期间(§7" + formatOfflineDuration(nowMs - lastSeenMs) + "§f)检测概况", "离线期间");
               this.markPlayerLastSeen(player);
            }
         }
      }
   }

   public void showRecentReport(Player player, long windowMillis) {
      if (player != null && windowMillis > 0L) {
         this.showCompactReport(player, windowMillis, "最近 " + formatDurationShort(windowMillis) + " 的检测概况", "最近 " + formatDurationShort(windowMillis) + " 内");
      }
   }

   private void showCompactReport(Player player, long windowMillis, String headerText, String emptyPeriodText) {
      long nowMs = System.currentTimeMillis();
      Map<String, int[]> detectionCounts = new LinkedHashMap<>();
      Map<String, double[]> latestViolationLevels = new LinkedHashMap<>();
      Map<String, int[]> banCounts = new LinkedHashMap<>();
      Map<String, String> latestBanReasons = new LinkedHashMap<>();
      LinkedHashSet<String> reportedPlayers = new LinkedHashSet<>();

      for (String eventLine : this.events) {
         String[] parts = eventLine.split("\\|", -1);
         if (parts.length >= 5) {
            long eventTimestamp = parseEventTimestamp(eventLine);
            if (eventTimestamp >= 0L && (windowMillis <= 0L || eventTimestamp >= nowMs - windowMillis && eventTimestamp <= nowMs)) {
               String targetPlayerName = parts[1];
               reportedPlayers.add(targetPlayerName);
               if (eventLine.endsWith("|BAN")) {
                  banCounts.computeIfAbsent(targetPlayerName, ignoredKey -> createBanCounter())[0]++;
                  latestBanReasons.put(targetPlayerName, parts.length > 3 ? parts[3] : "?");
               } else {
                  detectionCounts.computeIfAbsent(targetPlayerName, ignoredKey -> createDetectionCounter())[0]++;
                  latestViolationLevels.put(targetPlayerName, new double[]{parseDoubleSafe(parts[3])});
               }
            }
         }
      }

      if (reportedPlayers.isEmpty()) {
         player.sendMessage("§7[NekoAntiXray] §f" + emptyPeriodText + "没有检测/封禁记录。");
      } else {
         player.sendMessage("§6§l[NekoAntiXray] §f" + headerText + "(共 §e" + reportedPlayers.size() + " §f名玩家):");

         for (String targetPlayerName : reportedPlayers) {
            int detectionCount = detectionCounts.containsKey(targetPlayerName) ? detectionCounts.get(targetPlayerName)[0] : 0;
            double lastViolationLevel = latestViolationLevels.containsKey(targetPlayerName) ? latestViolationLevels.get(targetPlayerName)[0] : 0.0;
            String formattedVl = formatViolationLevel(lastViolationLevel);
            StringBuilder message = new StringBuilder(" §e- " + targetPlayerName + " §7检测 §f" + detectionCount + "§7 次 | 最后VL §e" + formattedVl);
            int[] banCounter = banCounts.get(targetPlayerName);
            if (banCounter != null) {
               message.append(" §7| 封禁: §c是§8(").append(banCounter[0]).append(" 次");
               String banReason = latestBanReasons.get(targetPlayerName);
               if (banReason != null) {
                  message.append(", ").append(banReason);
               }

               message.append(")");
            } else {
               message.append(" §7| 封禁: §a否");
            }

            player.sendMessage(message.toString());
         }
      }
   }

   public void showPlayerHistory(Player player, String targetPlayerName) {
      if (player != null && targetPlayerName != null) {
         System.currentTimeMillis();
         List<String> matchingEvents = new ArrayList<>();

         for (String eventLine : this.events) {
            String[] parts = eventLine.split("\\|", -1);
            if (parts.length >= 5 && parts[1].equalsIgnoreCase(targetPlayerName)) {
               parseEventTimestamp(eventLine);
               matchingEvents.add(eventLine);
            }
         }

         boolean isSelf = targetPlayerName.equalsIgnoreCase(player.getName());
         String subjectLabel = isSelf ? "你" : "玩家 " + targetPlayerName;
         if (matchingEvents.isEmpty()) {
            player.sendMessage("§7[NekoAntiXray] §f" + subjectLabel + " §7暂无检测记录。");
         } else {
            int displayCount = Math.min(20, matchingEvents.size());
            String countSummary = "共 " + matchingEvents.size() + " 条, 显示最近 " + displayCount + " 条):";
            player.sendMessage("§6§l[NekoAntiXray] §f" + subjectLabel + " §7" + "全部" + "检测记录(" + countSummary);

            for (int i = matchingEvents.size() - 1; i >= matchingEvents.size() - displayCount; i--) {
               String eventLine = matchingEvents.get(i);
               String[] eventParts = eventLine.split("\\|", -1);
               String timeText = formatTimeOfDay(parseEventTimestamp(eventLine));
               if (eventLine.endsWith("|BAN")) {
                  player.sendMessage(" §c[" + timeText + "] §7被封禁 §8(原因: " + (eventParts.length > 3 ? eventParts[3] : "?") + ")");
               } else {
                  String detectionDetail = eventParts[2];
                  String formattedVl = formatViolationLevel(parseDoubleSafe(eventParts[3]));
                  player.sendMessage(" §e[" + timeText + "] §f" + detectionDetail + " §8VL: " + formattedVl);
               }
            }
         }
      }
   }

   public static long parseDurationMillis(String durationText) {
      if (durationText == null) {
         return -1L;
      }

      String normalized = durationText.trim().toLowerCase(Locale.ROOT);
      if (!normalized.isEmpty() && !normalized.equals("all")) {
         long unitMillis = 3600000L;
         char unitChar = normalized.charAt(normalized.length() - 1);
         if (unitChar == 'm') {
            unitMillis = 60000L;
            normalized = normalized.substring(0, normalized.length() - 1);
         } else if (unitChar == 'h') {
            unitMillis = 3600000L;
            normalized = normalized.substring(0, normalized.length() - 1);
         } else if (unitChar == 'd') {
            unitMillis = 86400000L;
            normalized = normalized.substring(0, normalized.length() - 1);
         }

         try {
            long amount = Long.parseLong(normalized);
            if (amount <= 0L) {
               return 0L;
            }

            long totalMillis = amount * unitMillis;
            return totalMillis > 0L ? totalMillis : -1L;
         } catch (NumberFormatException ex) {
            return -1L;
         }
      } else {
         return 0L;
      }
   }

   private static String formatDurationShort(long durationMillis) {
      long totalMinutes = durationMillis / 60000L;
      long days = totalMinutes / 1440L;
      long hours = totalMinutes % 1440L / 60L;
      long minutes = totalMinutes % 60L;
      StringBuilder result = new StringBuilder();
      if (days > 0L) {
         result.append(days).append(" 天 ");
      }

      if (hours > 0L || days > 0L) {
         result.append(hours).append(" 小时 ");
      }

      if (minutes > 0L && days == 0L) {
         result.append(minutes).append(" 分钟");
      }

      return result.toString().trim();
   }

   private static String formatTimeOfDay(long timestampMillis) {
      if (timestampMillis <= 0L) {
         return "??:??:??";
      }

      SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.ROOT);
      return timeFormat.format(new Date(timestampMillis));
   }

   private boolean isEnabled() {
      return this.plugin.getConfigManager().isNotificationSystemOfflineReportEnabled();
   }

   private static long parseEventTimestamp(String eventLine) {
      int delimiterIndex = eventLine.indexOf("|");
      if (delimiterIndex <= 0) {
         return -1L;
      }

      try {
         return Long.parseLong(eventLine.substring(0, delimiterIndex));
      } catch (NumberFormatException ex) {
         return -1L;
      }
   }

   private static double parseDoubleSafe(String rawNumber) {
      try {
         return Double.parseDouble(rawNumber);
      } catch (NumberFormatException ex) {
         return 0.0;
      }
   }

   private static String sanitizeField(String rawText) {
      return rawText == null ? "" : rawText.replace("|", " ").replace('\n', ' ').replace('\r', ' ');
   }

   private static String formatViolationLevel(double violationLevel) {
      return violationLevel == Math.floor(violationLevel) && !Double.isInfinite(violationLevel)
         ? String.valueOf((long)violationLevel)
         : String.format(Locale.ROOT, "%.1f", violationLevel);
   }

   private static String formatOfflineDuration(long durationMillis) {
      long totalMinutes = durationMillis / 60000L;
      if (totalMinutes < 1L) {
         return "不到 1 分钟";
      }

      long days = totalMinutes / 1440L;
      long hours = totalMinutes % 1440L / 60L;
      long minutes = totalMinutes % 60L;
      StringBuilder result = new StringBuilder();
      if (days > 0L) {
         result.append(days).append(" 天 ");
      }

      if (hours > 0L || days > 0L) {
         result.append(hours).append(" 小时 ");
      }

      result.append(minutes).append(" 分钟");
      return result.toString();
   }

   private synchronized void appendEventLine(String eventLine) {
      this.events.add(eventLine);
      this.trimEvents();

      try {
         if (!this.eventsLogFile.exists()) {
            this.eventsLogFile.createNewFile();
         }

         try (FileWriter fileWriter = new FileWriter(this.eventsLogFile, true)) {
            fileWriter.write(eventLine);
            fileWriter.write(System.lineSeparator());
         }
      } catch (IOException ex) {
         this.plugin.getLogger().warning("写入离线报告事件日志失败: " + ex.getMessage());
      }
   }

   public void markPlayerLastSeen(Player player) {
      if (player != null) {
         this.lastSeenTimestamps.put(player.getName(), System.currentTimeMillis());
         this.saveLastSeenTimestamps();
      }
   }

   public void saveAllOnlineLastSeen() {
      for (Player player : Bukkit.getOnlinePlayers()) {
         this.lastSeenTimestamps.put(player.getName(), System.currentTimeMillis());
      }

      this.saveLastSeenTimestamps();
   }
}
