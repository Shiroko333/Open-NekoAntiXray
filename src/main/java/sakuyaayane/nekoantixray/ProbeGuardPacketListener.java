package sakuyaayane.nekoantixray;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ProbeGuardPacketListener extends PacketListenerAbstract implements Listener {
   private final NekoAntiXray plugin;
   private final ProbeGuardChecker guard;
   private final AtomicLong checkedPacketCount = new AtomicLong();
   private final AtomicLong cancelledPacketCount = new AtomicLong();
   private final ConcurrentMap<String, AtomicLong> cancelReasonCounts = new ConcurrentHashMap<>();
   private final ConcurrentMap<String, AtomicLong> gatedReasonCounts = new ConcurrentHashMap<>();
   private volatile long lastCancelTimeMs;

   @Override
   public void onPacketReceive(PacketReceiveEvent event) {
      try {
         PacketTypeCommon packetType = event.getPacketType();
         if (WrapperPlayClientPlayerFlying.isFlying(packetType)) {
            this.handleFlyingPacket(event);
            return;
         }

         if (packetType != Client.PLAYER_DIGGING || !this.checkActive()) {
            return;
         }

         Player player = event.getPlayer();
         if (player == null) {
            return;
         }

         WrapperPlayClientPlayerDigging wrapperPlayClientPlayerDigging = new WrapperPlayClientPlayerDigging(event);
         DiggingAction action = wrapperPlayClientPlayerDigging.getAction();
         Vector3i blockPosition = wrapperPlayClientPlayerDigging.getBlockPosition();
         if (action == null || blockPosition == null) {
            return;
         }

         this.checkedPacketCount.incrementAndGet();
         ProbeGuardChecker.CheckResult checkResult = this.guard.check(player, action, blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(), player.getWorld().getName());
         if (!checkResult.isCancel()) {
            String reason = checkResult.getReason();
            if (reason != null && reason.startsWith("gated:")) {
               this.gatedReasonCounts.computeIfAbsent(reason.substring(6), key -> new AtomicLong()).incrementAndGet();
            }

            return;
         }

         event.setCancelled(true);
         this.cancelledPacketCount.incrementAndGet();
         this.lastCancelTimeMs = System.currentTimeMillis();
         this.cancelReasonCounts.computeIfAbsent(checkResult.getReason(), key -> new AtomicLong()).incrementAndGet();
         this.handleCancelledProbe(player, checkResult.getReason(), action, blockPosition);
      } catch (Throwable ex) {
         if (this.plugin.getConfig().getBoolean("debug", false)) {
            this.plugin.logDebug("[Probe] 收包校验异常: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
         }
      }
   }

   public long getCheckedPacketCount() {
      return this.checkedPacketCount.get();
   }

   public long getCancelledPacketCount() {
      return this.cancelledPacketCount.get();
   }

   public Map getGatedReasons() {
      LinkedHashMap<String, Long> gatedReasons = new LinkedHashMap<>();
      this.gatedReasonCounts.forEach((reason, count) -> copyGatedReasonEntry(gatedReasons, reason, count));
      return gatedReasons;
   }

   public Map getCancelReasons() {
      LinkedHashMap<String, Long> cancelReasons = new LinkedHashMap<>();
      this.cancelReasonCounts.forEach((reason, count) -> copyCancelReasonEntry(cancelReasons, reason, count));
      return cancelReasons;
   }

   public long getLastCancelTimeMs() {
      return this.lastCancelTimeMs;
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      this.guard.removePlayerState(event.getPlayer().getUniqueId());
   }

   private static void copyCancelReasonEntry(Map targetMap, String reason, AtomicLong count) {
      targetMap.put(reason, count.get());
   }

   private static void copyGatedReasonEntry(Map targetMap, String reason, AtomicLong count) {
      targetMap.put(reason, count.get());
   }

   private void handleFlyingPacket(PacketReceiveEvent event) {
      if (this.checkActive()) {
         Player player = event.getPlayer();
         if (player != null) {
            WrapperPlayClientPlayerFlying wrapperPlayClientPlayerFlying = new WrapperPlayClientPlayerFlying(event);
            if (wrapperPlayClientPlayerFlying.hasPositionChanged() || wrapperPlayClientPlayerFlying.hasRotationChanged()) {
               Location location = wrapperPlayClientPlayerFlying.getLocation();
               boolean positionChanged = wrapperPlayClientPlayerFlying.hasPositionChanged();
               double posX = location.getX();
               double posY = location.getY();
               double posZ = location.getZ();
               boolean rotationChanged = wrapperPlayClientPlayerFlying.hasRotationChanged();
               float yaw = location.getYaw();
               float pitch = location.getPitch();
               String worldName = player.getWorld().getName();
               this.guard.onMovement(player, positionChanged, posX, posY, posZ, rotationChanged, yaw, pitch, worldName);
            }
         }
      }
   }

   private boolean checkActive() {
      ConfigManager configManager = this.plugin.getConfigManager();
      return !configManager.isCheckXrayProbeGuardEnabled() ? false : !configManager.isCheckXrayProbeGuardRequireHeavyMode() || configManager.isHeavyModeEnabled();
   }

   public boolean isActive() {
      return this.checkActive();
   }

   public String getPlayerDebugStatus(Player player) {
      return this.guard.formatPlayerDebugStatus(player);
   }

   private void handleCancelledProbe(Player player, String reason, DiggingAction diggingAction, Vector3i blockPosition) {
      ConfigManager configManager = this.plugin.getConfigManager();
      double vlPerCancel = configManager.getCheckXrayProbeGuardVlPerCancel();
      if (vlPerCancel > 0.0) {
         XrayDetector xrayDetector = this.plugin.getXrayDetector();
         xrayDetector.addPlayerViolation(player, vlPerCancel);
      }

      double playerViolationLevel = this.plugin.getXrayDetector().getPlayerViolationLevel(player);
      String actionName = diggingAction.name();
      int blockX = blockPosition.getX();
      int blockY = blockPosition.getY();
      int blockZ = blockPosition.getZ();
      String detailMessage = "鬼手探测(" + reason + ") " + actionName + " @ " + blockX + "," + blockY + "," + blockZ;

      try {
         DetectionNotificationManager detectionNotificationManager = this.plugin.getDetectionNotificationManager();
         double notifyVl = playerViolationLevel > 0.0 ? playerViolationLevel : vlPerCancel;
         detectionNotificationManager.notifyDetection(player, "xray", "Probe", notifyVl, detailMessage);
      } catch (Throwable ex) {
      }

      if (configManager.isCheckXrayProbeGuardDebugLog()) {
         String playerName = player.getName();
         String formattedVl = String.format("%.1f", playerViolationLevel);
         this.plugin.logDebug("[Probe] 已拦截 " + playerName + " 的挖掘包: " + detailMessage + ", 当前VL=" + formattedVl);
      }
   }

   public ProbeGuardPacketListener(NekoAntiXray plugin) {
      super(PacketListenerPriority.HIGHEST);
      this.plugin = plugin;
      this.guard = new ProbeGuardChecker(plugin);
   }
}
