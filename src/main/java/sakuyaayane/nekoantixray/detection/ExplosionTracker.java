package sakuyaayane.nekoantixray.detection;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import java.util.UUID;
import lombok.Generated;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World;
import sakuyaayane.nekoantixray.NekoAntiXray;

public class ExplosionTracker {
   private static final double DEFAULT_EXPLOSION_RADIUS = 8.0;
   private static final int RECORD_EXPIRATION_SECONDS = 300;
   private final Map<UUID, List<ExplosionTracker.ExplosionRecord>> playerExplosionRecords = new ConcurrentHashMap<>();
   private static final Logger logger = Logger.getLogger("NekoAntiXray");

   private void cleanupExpiredRecords(UUID uuid) {
      try {
         List<ExplosionTracker.ExplosionRecord> records = this.playerExplosionRecords.get(uuid);
         if (records != null) {
            records.removeIf(explosionRecord -> isExpiredRecord(explosionRecord));
            if (records.isEmpty()) {
               this.playerExplosionRecords.remove(uuid);
            }
         }
      } catch (Exception ex) {
         logger.warning("清理过期爆炸记录时发生错误: " + ex.getMessage());
      }
   }

   private static boolean isExpiredRecord(ExplosionTracker.ExplosionRecord explosionRecord) {
      return explosionRecord == null || !explosionRecord.isValid();
   }

   private static boolean matchesExplosionLocation(Location targetLocation, ExplosionTracker.ExplosionRecord explosionRecord) {
      return explosionRecord != null && explosionRecord.isWithinRadius(targetLocation);
   }

   public void recordExplosion(Player player, Location location) {
      try {
         if (player == null || location == null || location.getWorld() == null) {
            logger.warning("尝试记录爆炸时收到无效参数");
            return;
         }

         UUID uuid = player.getUniqueId();
         ExplosionTracker.ExplosionRecord explosionRecord = new ExplosionTracker.ExplosionRecord(location, 8.0);
         this.playerExplosionRecords.computeIfAbsent(uuid, playerId -> new CopyOnWriteArrayList<>()).add(explosionRecord);
         this.cleanupExpiredRecords(uuid);
      } catch (Exception ex) {
         logger.severe("记录爆炸事件时发生错误: " + ex.getMessage());
      }
   }

   public boolean isNearExplosion(Player player, Location location) {
      try {
         if (player != null && location != null && location.getWorld() != null) {
            UUID uuid = player.getUniqueId();
            List<ExplosionTracker.ExplosionRecord> records = this.playerExplosionRecords.get(uuid);
            if (records != null && !records.isEmpty()) {
               this.cleanupExpiredRecords(uuid);
               return records.stream().anyMatch(explosionRecord -> matchesExplosionLocation(location, explosionRecord));
            } else {
               return false;
            }
         } else {
            return false;
         }
      } catch (Exception ex) {
         logger.severe("检查爆炸范围时发生错误: " + ex.getMessage());
         return false;
      }
   }

   public void cleanupAllExpiredRecords() {
      try {
         this.playerExplosionRecords.keySet().forEach(uuid -> this.cleanupExpiredRecords(uuid));
      } catch (Exception ex) {
         logger.severe("清理所有过期爆炸记录时发生错误: " + ex.getMessage());
      }
   }

   private static class ExplosionRecord {
      private final Location location;
      private final LocalDateTime time;
      private final double radius;
      private static final long EXPIRATION_SECONDS = 300L;

      @Generated
      public Location getLocation() {
         return this.location;
      }

      @Generated
      public LocalDateTime getTime() {
         return this.time;
      }

      @Generated
      public double getRadius() {
         return this.radius;
      }

      public boolean isValid() {
         try {
            return ChronoUnit.SECONDS.between(this.time, LocalDateTime.now()) <= 300L;
         } catch (Exception ex) {
            ExplosionTracker.logger.warning("检查爆炸记录有效性时发生错误: " + ex.getMessage());
            return false;
         }
      }

      public boolean isWithinRadius(Location targetLocation) {
         try {
            if (this.location != null && targetLocation != null) {
               World explosionWorld = this.location.getWorld();
               World targetWorld = targetLocation.getWorld();
               return explosionWorld != null && targetWorld != null && explosionWorld.equals(targetWorld)
                  ? this.location.distanceSquared(targetLocation) <= this.radius * this.radius
                  : false;
            } else {
               return false;
            }
         } catch (Exception ex) {
            ExplosionTracker.logger.warning("检查爆炸范围时发生错误: " + ex.getMessage());
            return false;
         }
      }

      public ExplosionRecord(Location location, double radius) {
         this.location = location != null ? location.clone() : null;
         this.time = LocalDateTime.now();
         this.radius = radius;
      }
   }
}
