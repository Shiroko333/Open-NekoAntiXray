package sakuyaayane.nekoantixray.replay;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Generated;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import sakuyaayane.nekoantixray.lib.folialib.FoliaLib;
import sakuyaayane.nekoantixray.lib.folialib.wrapper.task.WrappedTask;
import sakuyaayane.nekoantixray.NekoAntiXray;
import sakuyaayane.nekoantixray.stats.OreParticleRenderer;

public class ReplaySession {
   private final NekoAntiXray plugin;
   private final FoliaLib foliaLib;
   private volatile WrappedTask task;
   private final ArmorStand playerEntity;
   private final ArmorStand nameEntity;
   private final Location originalLocation;
   private final GameMode originalGameMode;
   private final List<ReplayFrame> frames;
   private int currentIndex;
   private boolean isPaused;
   private final Map<Material, Integer> oreCount;
   private final Player viewer;
   private final Map<Location, Material> brokenOres = new ConcurrentHashMap<>();
   private final Set<Location> pathLocations = Collections.newSetFromMap(new ConcurrentHashMap<>());
   private static final long PARTICLE_DISPLAY_INTERVAL = 2L;
   private long lastParticleDisplay = 0L;
   private static final long BLOCK_RESTORE_DELAY = 5000L;
   private final Map<Location, ReplaySession.BlockStateInfo> blockStates = new ConcurrentHashMap<>();
   private final Map<Location, Material> blocksToRestore = new ConcurrentHashMap<>();
   private boolean isInitialized = false;
   private final Set<Location> brokenBlocksInReplay = Collections.newSetFromMap(new ConcurrentHashMap<>());

   public ReplaySession(
      WrappedTask task,
      ArmorStand playerEntity,
      ArmorStand nameEntity,
      Location originalLocation,
      GameMode originalGameMode,
      List<ReplayFrame> frames,
      Player viewer
   ) {
      this.plugin = NekoAntiXray.getInstance();
      this.foliaLib = this.plugin.getFoliaScheduler();
      this.task = task;
      this.playerEntity = playerEntity;
      this.nameEntity = nameEntity;
      this.originalLocation = originalLocation;
      this.originalGameMode = originalGameMode;
      this.frames = frames;
      this.viewer = viewer;
      this.currentIndex = 0;
      this.isPaused = false;
      this.oreCount = new HashMap<>();
      this.collectBlocksToRestore();
   }

   private void collectBlocksToRestore() {
      for (ReplayFrame replayFrame : this.frames) {
         if ("BREAK".equals(replayFrame.action) && replayFrame.blockType != null) {
            Location correctLocation = replayFrame.getCorrectLocation();
            if (correctLocation != null) {
               this.blocksToRestore.put(correctLocation, Material.valueOf(replayFrame.blockType));
            }
         }
      }
   }

   public void initializeBlocks() {
      if (!this.isInitialized) {
         this.blocksToRestore.forEach((location, material) -> this.viewer.sendBlockChange(location, material.createBlockData()));
         this.isInitialized = true;
         this.plugin.logDebug("已恢复所有被破坏的方块");
      }
   }

   private void handlePlayerMove(ReplaySession replaySession, Location targetLocation) {
      Location standLocation = targetLocation.clone().add(0.0, -0.2, 0.0);
      ArmorStand sessionPlayerEntity = replaySession.getPlayerEntity();
      ArmorStand sessionNameEntity = replaySession.getNameEntity();
      if (sessionPlayerEntity != null) {
         this.foliaLib.getImpl().teleportAsync(sessionPlayerEntity, standLocation);
      }

      if (sessionNameEntity != null) {
         this.foliaLib.getImpl().teleportAsync(sessionNameEntity, standLocation.clone().add(0.0, 1.7, 0.0));
      }

      Location blockLocation = targetLocation.getBlock().getLocation();
      if (this.blocksToRestore.containsKey(blockLocation)) {
         Material restoredMaterial = this.blocksToRestore.get(blockLocation);
         this.viewer.sendBlockChange(blockLocation, Material.AIR.createBlockData());
         this.blocksToRestore.remove(blockLocation);
         if (OreParticleRenderer.isSupportedOre(restoredMaterial)) {
            OreParticleRenderer.renderOreOutline(this.viewer, blockLocation, restoredMaterial);
         }
      }
   }

   public boolean isInitialized() {
      return this.isInitialized;
   }

   public void addPathLocation(Location location) {
      this.pathLocations.add(location.getBlock().getLocation());
   }

   public void addBrokenOre(Location location, Material material) {
      if (OreParticleRenderer.isSupportedOre(material)) {
         this.brokenOres.put(location, material);
         this.incrementOreCount(material);
      }
   }

   public void displayParticles(long currentTick) {
      if (currentTick - this.lastParticleDisplay >= 2L) {
         this.displayPathParticles();
         this.displayOreParticles();
         this.lastParticleDisplay = currentTick;
      }
   }

   private void displayPathParticles() {
      Location viewerLocation = this.viewer.getLocation();
      this.pathLocations
         .stream()
         .filter(pathLocation -> pathLocation.getWorld().equals(viewerLocation.getWorld()) && pathLocation.distance(viewerLocation) <= 32.0)
         .forEach(pathLocation -> OreParticleRenderer.spawnTrailParticle(this.viewer, pathLocation));
   }

   private void displayOreParticles() {
      Location viewerLocation = this.viewer.getLocation();
      this.brokenOres
         .entrySet()
         .stream()
         .filter(entry -> entry.getKey().getWorld().equals(viewerLocation.getWorld()) && entry.getKey().distance(viewerLocation) <= 32.0)
         .forEach(entry -> {
            Location oreLocation = entry.getKey();
            Material oreMaterial = entry.getValue();
            OreParticleRenderer.renderOreOutline(this.viewer, oreLocation, oreMaterial);
         });
   }

   public void addBrokenBlock(Location location, long breakTime, Material material) {
      this.blockStates.put(location, new ReplaySession.BlockStateInfo(material, breakTime, false));
      if (OreParticleRenderer.isSupportedOre(material)) {
         this.addBrokenOre(location, material);
      }

      this.plugin.logDebug("记录破坏方块: " + location + ", 材质: " + material);
   }

   public void checkAndRestoreBlocks(Location centerLocation, long currentTime) {
      this.blockStates
         .entrySet()
         .stream()
         .filter(entry -> entry.getKey().getWorld().equals(centerLocation.getWorld()) && entry.getKey().distance(centerLocation) <= 16.0)
         .filter(entry -> this.shouldRestoreBlock(entry.getKey(), currentTime))
         .forEach(entry -> {
            Location blockLocation = entry.getKey();
            ReplaySession.BlockStateInfo stateInfo = entry.getValue();
            this.viewer.sendBlockChange(blockLocation, stateInfo.material.createBlockData());
            this.blockStates.put(blockLocation, new ReplaySession.BlockStateInfo(stateInfo.material, stateInfo.breakTime, true));
         });
   }

   public boolean shouldRestoreBlock(Location location, long currentTime) {
      ReplaySession.BlockStateInfo blockStateInfo = this.blockStates.get(location);
      return blockStateInfo != null && !blockStateInfo.restored && currentTime < blockStateInfo.breakTime - 5000L;
   }

   public WrappedTask getTask() {
      return this.task;
   }

   public void setTask(WrappedTask task) {
      this.task = task;
   }

   public ArmorStand getPlayerEntity() {
      return this.playerEntity;
   }

   public ArmorStand getNameEntity() {
      return this.nameEntity;
   }

   public Location getOriginalLocation() {
      return this.originalLocation;
   }

   public GameMode getOriginalGameMode() {
      return this.originalGameMode;
   }

   public List<ReplayFrame> getFrames() {
      return this.frames;
   }

   public Player getViewer() {
      return this.viewer;
   }

   public int getCurrentIndex() {
      return this.currentIndex;
   }

   public void setCurrentIndex(int currentIndex) {
      this.currentIndex = currentIndex;
   }

   public boolean isPaused() {
      return this.isPaused;
   }

   public void setPaused(boolean paused) {
      this.isPaused = paused;
   }

   public Map<Material, Integer> getOreCount() {
      return this.oreCount;
   }

   public void pause() {
      this.isPaused = true;
   }

   public void resume() {
      this.isPaused = false;
   }

   public void forward() {
      this.currentIndex = Math.min(this.currentIndex + 100, this.frames.size() - 1);
   }

   public void backward() {
      this.currentIndex = Math.max(this.currentIndex - 100, 0);
   }

   public void incrementCurrentIndex() {
      this.currentIndex++;
   }

   void incrementOreCount(Material material) {
      this.oreCount.merge(material, 1, Integer::sum);
   }

   public World getInitialWorld() {
      return this.frames.isEmpty() ? null : Bukkit.getWorld(this.frames.get(0).worldName);
   }

   public boolean isBlockBroken(Location location) {
      return this.blocksToRestore.containsKey(location);
   }

   public void markBlockBrokenInReplay(Location location) {
      this.brokenBlocksInReplay.add(location);
   }

   public boolean isBlockBrokenInReplay(Location location) {
      return this.brokenBlocksInReplay.contains(location);
   }

   public static class BlockStateInfo {
      public final Material material;
      public final long breakTime;
      public final boolean restored;
      public final boolean isOre;
      private static final String ORE_MATERIAL_SUFFIX = "_ORE";

      public BlockStateInfo(Material material, long breakTime, boolean restored) {
         this.material = material;
         this.breakTime = breakTime;
         this.restored = restored;
         this.isOre = material.name().endsWith("_ORE") || material == Material.ANCIENT_DEBRIS;
      }

      @Generated
      public Material getMaterial() {
         return this.material;
      }

      @Generated
      public long getBreakTime() {
         return this.breakTime;
      }

      @Generated
      public boolean isRestored() {
         return this.restored;
      }

      @Generated
      public boolean isOre() {
         return this.isOre;
      }

      @Generated
      public BlockStateInfo(Material material, long breakTime, boolean restored, boolean isOre) {
         this.material = material;
         this.breakTime = breakTime;
         this.restored = restored;
         this.isOre = isOre;
      }
   }
}
