package sakuyaayane.nekoantixray;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class UnexposedMiningListener implements Listener {
   private static final BlockFace[] CARDINAL_FACES = new BlockFace[]{
      BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
   };
   private final Map<UUID, UnexposedMiningListener.MiningSampleWindow> windows = new HashMap<>();
   private final Set<Material> hostBlockMaterials = EnumSet.of(Material.STONE, Material.DEEPSLATE, Material.NETHERRACK, Material.END_STONE);
   private final NekoAntiXray plugin;

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onBlockBreak(BlockBreakEvent event) {
      if (this.plugin.getConfig().getBoolean("check.unexposed-mining.enabled", false)) {
         Player player = event.getPlayer();
         if (!player.hasPermission("nekoantixray.bypass") && !this.isBlockExposed(event.getBlock())) {
            Material blockType = event.getBlock().getType();
            boolean isOre = OreMaterialMatcher.isOreMaterial(blockType);
            boolean isHostBlock = this.hostBlockMaterials.contains(blockType);
            if (isOre || isHostBlock) {
               UnexposedMiningListener.MiningSampleWindow miningSampleWindow = this.windows.get(player.getUniqueId());
               if (miningSampleWindow == null) {
                  miningSampleWindow = new UnexposedMiningListener.MiningSampleWindow();
                  this.windows.put(player.getUniqueId(), miningSampleWindow);
               }

               int windowSize = Math.max(1, this.plugin.getConfig().getInt("check.unexposed-mining.window-size", 200));
               miningSampleWindow.recordSample(isOre, windowSize);
               this.evaluateAndAlert(player, miningSampleWindow);
            }
         }
      }
   }

   private boolean isBlockExposed(Block block) {
      for (BlockFace face : CARDINAL_FACES) {
         Material neighborType = block.getRelative(face).getType();
         if (neighborType.isAir() || neighborType == Material.WATER || neighborType == Material.LAVA) {
            return true;
         }
      }

      return false;
   }

   private void evaluateAndAlert(Player player, UnexposedMiningListener.MiningSampleWindow miningSampleWindow) {
      int minSamples = Math.max(1, this.plugin.getConfig().getInt("check.unexposed-mining.minimum-samples", 100));
      int minOres = Math.max(1, this.plugin.getConfig().getInt("check.unexposed-mining.minimum-ores", 8));
      int minHostBlocks = Math.max(0, this.plugin.getConfig().getInt("check.unexposed-mining.minimum-host-blocks", 60));
      double maxOreRatio = this.plugin.getConfig().getDouble("check.unexposed-mining.maximum-ore-ratio", 0.08);
      if (miningSampleWindow.getSampleCount() >= minSamples && miningSampleWindow.ores >= minOres && miningSampleWindow.hostBlocks >= minHostBlocks) {
         double oreRatio = (double)miningSampleWindow.ores / miningSampleWindow.getSampleCount();
         if (!(oreRatio < maxOreRatio) && !miningSampleWindow.alertTriggered) {
            miningSampleWindow.alertTriggered = true;
            String alertMessage = String.format(
               "[UnexposedMining] %s: hidden ores=%d, host blocks=%d, ratio=%.2f%%, samples=%d",
               player.getName(),
               miningSampleWindow.ores,
               miningSampleWindow.hostBlocks,
               oreRatio * 100.0,
               miningSampleWindow.getSampleCount()
            );
            this.plugin.getLogger().warning(alertMessage);

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
               if (onlinePlayer.hasPermission("nekoantixray.alert")) {
                  onlinePlayer.sendMessage("§c[NekoAntiXray] §f" + alertMessage);
               }
            }
         }
      }
   }

   public void cleanupOfflinePlayers() {
      Iterator<UUID> iterator = this.windows.keySet().iterator();

      while (iterator.hasNext()) {
         if (Bukkit.getPlayer(iterator.next()) == null) {
            iterator.remove();
         }
      }
   }

   public UnexposedMiningListener(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   private static final class MiningSampleWindow {
      private final ArrayDeque<Boolean> samples = new ArrayDeque<>();
      private int ores;
      private int hostBlocks;
      private boolean alertTriggered;

      private int getSampleCount() {
         return this.samples.size();
      }

      public void recordSample(boolean isOre, int maxWindowSize) {
         this.samples.addLast(isOre);
         if (isOre) {
            this.ores++;
         } else {
            this.hostBlocks++;
         }

         while (this.samples.size() > maxWindowSize) {
            if (this.samples.removeFirst()) {
               this.ores--;
            } else {
               this.hostBlocks--;
            }
         }

         if (this.ores == 0 || (double)this.ores / this.samples.size() < 0.04) {
            this.alertTriggered = false;
         }
      }
   }
}