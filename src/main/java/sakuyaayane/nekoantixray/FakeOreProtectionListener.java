package sakuyaayane.nekoantixray;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class FakeOreProtectionListener implements Listener {
   private static final double COLLINEARITY_TOLERANCE = 0.1;
   private static final double COLLINEARITY_TOLERANCE_SQUARED = 0.010000000000000002;
   private static final ConcurrentHashMap<Location, Long> TRACKED_ORE_LOCATIONS = new ConcurrentHashMap<>();
   private static final long ORE_EXPIRATION_MS = TimeUnit.HOURS.toMillis(1L);
   private static final ThreadLocal<Random> threadLocalRandom = ThreadLocal.withInitial(() -> new Random());
   private static final EnumSet<Material> ORE_MATERIALS = collectOreMaterials();

   @EventHandler
   public void onBlockPlace(BlockPlaceEvent event) {
      Block block = event.getBlock();
      if (isOreMaterial(block.getType())) {
         trackOreLocation(block.getLocation());
         NekoAntiXray.getInstance().logDebug("玩家放置了矿石: " + block.getType() + " 在位置: " + block.getLocation());
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onBlockExplode(BlockExplodeEvent event) {
      this.handleExplosion(event.blockList());
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onEntityExplode(EntityExplodeEvent event) {
      this.handleExplosion(event.blockList());
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onPistonExtend(BlockPistonExtendEvent event) {
      if (event.getBlocks().stream().anyMatch(block -> isTrackedOre(block.getLocation()))) {
         event.setCancelled(true);
         NekoAntiXray.getInstance().logDebug("活塞推动假矿石被阻止");
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onPistonRetract(BlockPistonRetractEvent event) {
      if (event.getBlocks().stream().anyMatch(block -> isTrackedOre(block.getLocation()))) {
         event.setCancelled(true);
         NekoAntiXray.getInstance().logDebug("活塞收回假矿石被阻止");
      }
   }

   public static void clearTrackedOres() {
      TRACKED_ORE_LOCATIONS.clear();
   }

   public static void cleanupExpiredOres() {
      long nowMs = System.currentTimeMillis();
      TRACKED_ORE_LOCATIONS.entrySet().removeIf(entry -> nowMs - entry.getValue() > ORE_EXPIRATION_MS);
   }

   private static boolean handleExplodedFakeOre(Block block) {
      Location location = block.getLocation();
      boolean isFakeOre = isTrackedOre(location) || NekoAntiXray.getInstance().getXrayDetector().isFakeOre(block);
      if (isFakeOre) {
         Material replacementMaterial = block.getWorld().getEnvironment() == Environment.NETHER ? Material.NETHERRACK : Material.STONE;
         block.setType(replacementMaterial);
         untrackOreLocation(location);
         NekoAntiXray.getInstance().getXrayDetector().unregisterFakeOre(location);
         NekoAntiXray.getInstance().logDebug("爆炸中的假矿石被移除: " + location);
      }

      return isFakeOre;
   }

   private static EnumSet collectOreMaterials() {
      EnumSet<Material> oreMaterials = EnumSet.noneOf(Material.class);

      for (Material material : Material.values()) {
         if (material.name().endsWith("_ORE")) {
            oreMaterials.add(material);
         }
      }

      return oreMaterials;
   }

   public static double calculateDistanceSquared(Location origin, Location target) {
      return origin.distanceSquared(target);
   }

   public static Location generateRandomOffsetLocation(Location origin, int minOffsetX, int maxOffsetX, int minOffsetY, int maxOffsetY, int minOffsetZ, int maxOffsetZ) {
      NekoAntiXray plugin = NekoAntiXray.getInstance();
      Random random = threadLocalRandom.get();
      int targetX = random.nextInt(maxOffsetX - minOffsetX + 1) + minOffsetX + origin.getBlockX();
      int targetZ = random.nextInt(maxOffsetZ - minOffsetZ + 1) + minOffsetZ + origin.getBlockZ();
      int targetY = random.nextInt(maxOffsetY - minOffsetY + 1) + minOffsetY + origin.getBlockY();
      World world = origin.getWorld();
      Location randomLocation = new Location(world, targetX, targetY, targetZ);
      plugin.logDebug("生成的随机位置: " + randomLocation);
      return randomLocation;
   }

   public static boolean areLocationsCollinear(List locations) {
      int count = locations.size();
      if (count < 3) {
         return true;
      }

      double[] coordsX = new double[count];
      double[] coordsY = new double[count];
      double[] coordsZ = new double[count];

      for (int i = 0; i < count; i++) {
         Location location = (Location)locations.get(i);
         coordsX[i] = location.getX();
         coordsY[i] = location.getY();
         coordsZ[i] = location.getZ();
      }

      double startX = coordsX[0];
      double startY = coordsY[0];
      double startZ = coordsZ[0];
      double endX = coordsX[count - 1];
      double endY = coordsY[count - 1];
      double endZ = coordsZ[count - 1];
      double lineDx = endX - startX;
      double lineDy = endY - startY;
      double lineDz = endZ - startZ;
      double lineLengthSq = lineDx * lineDx + lineDy * lineDy + lineDz * lineDz;
      if (lineLengthSq == 0.0) {
         return true;
      }

      double invLineLength = 1.0 / Math.sqrt(lineLengthSq);
      double dirX = lineDx * invLineLength;
      double dirY = lineDy * invLineLength;
      double dirZ = lineDz * invLineLength;

      for (int j = 1; j < count - 1; j++) {
         double relX = coordsX[j] - startX;
         double relY = coordsY[j] - startY;
         double relZ = coordsZ[j] - startZ;
         double projection = relX * dirX + relY * dirY + relZ * dirZ;
         double perpX = relX - projection * dirX;
         double perpY = relY - projection * dirY;
         double perpZ = relZ - projection * dirZ;
         double distSq = perpX * perpX + perpY * perpY + perpZ * perpZ;
         if (distSq > 0.010000000000000002) {
            return false;
         }
      }

      return true;
   }

   public static boolean isOreMaterial(Material material) {
      return ORE_MATERIALS.contains(material);
   }

   private void handleExplosion(List blocks) {
      blocks.removeIf(block -> handleExplodedFakeOre((Block)block));
   }

   public static boolean isTrackedOre(Location location) {
      Long trackedTimestamp = TRACKED_ORE_LOCATIONS.get(location);
      return trackedTimestamp == null ? false : System.currentTimeMillis() - trackedTimestamp <= ORE_EXPIRATION_MS;
   }

   public static void untrackOreLocation(Location location) {
      TRACKED_ORE_LOCATIONS.remove(location);
   }

   public static void trackOreLocation(Location location) {
      TRACKED_ORE_LOCATIONS.put(location, System.currentTimeMillis());
   }
}
