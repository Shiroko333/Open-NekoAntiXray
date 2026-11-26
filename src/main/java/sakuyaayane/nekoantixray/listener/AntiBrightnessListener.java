package sakuyaayane.nekoantixray.listener;

import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import sakuyaayane.nekoantixray.config.ConfigManager;
import sakuyaayane.nekoantixray.NekoAntiXray;

public class AntiBrightnessListener implements Listener {
   private final NekoAntiXray plugin;
   private final ConfigManager configManager;

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void onBlockBreak(BlockBreakEvent event) {
      if (this.configManager.isAntiBrightnessEnabled()) {
         Player player = event.getPlayer();
         Block block = event.getBlock();
         String worldName = player.getWorld().getName();
         List enabledWorlds = this.configManager.getAntiBrightnessEnabledWorlds();
         List blacklistWorlds = this.configManager.getBlacklistWorlds();
         if (!enabledWorlds.isEmpty()) {
            if (!enabledWorlds.contains(worldName)) {
               return;
            }
         } else if (blacklistWorlds != null && blacklistWorlds.contains(worldName)) {
            return;
         }

         List targetOres = this.configManager.getAntiBrightnessTargetOres();
         if (!targetOres.isEmpty() && targetOres.contains(block.getType())) {
            byte lightLevel = block.getLightLevel();
            int lightThreshold = this.configManager.getAntiBrightnessLightThreshold();
            if (lightLevel < lightThreshold) {
               event.setCancelled(true);
               player.sendMessage(this.configManager.getAntiBrightnessMessage());
               String playerName = player.getName();
               String blockTypeName = block.getType().name();
               String blockLocation = block.getLocation().toString();
               String debugMessage = "AntiBrightness: Prevented "
                  + playerName
                  + " from breaking "
                  + blockTypeName
                  + " at "
                  + blockLocation
                  + " due to low light level ("
                  + lightLevel
                  + " < "
                  + lightThreshold
                  + ").";
               this.plugin.logDebug(debugMessage);
            }
         }
      }
   }

   public AntiBrightnessListener(NekoAntiXray plugin, ConfigManager configManager) {
      this.plugin = plugin;
      this.configManager = configManager;
   }
}
