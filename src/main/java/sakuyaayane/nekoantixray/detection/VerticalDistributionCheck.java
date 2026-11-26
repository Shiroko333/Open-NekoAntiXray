package sakuyaayane.nekoantixray.detection;

import org.bukkit.entity.Player;
import org.bukkit.Location;
import sakuyaayane.nekoantixray.lib.folialib.FoliaLib;
import sakuyaayane.nekoantixray.NekoAntiXray;

public class VerticalDistributionCheck {
   private final NekoAntiXray plugin;
   private final FoliaLib foliaLib;

   public VerticalDistributionCheck(NekoAntiXray plugin, FoliaLib foliaLib) {
      this.plugin = plugin;
      this.foliaLib = foliaLib;
   }

   public boolean isValidVerticalDistribution(Location location, Player player) {
      if (location != null && player != null && location.getWorld() != null) {
         try {
            int blockY = location.getBlockY();
            int playerY = player.getLocation().getBlockY();
            boolean isValid = Math.abs(blockY - playerY) >= 2;
            this.plugin.logDebug(String.format("垂直分布检查 - 方块Y: %d, 玩家Y: %d, 差值: %d, 是否有效: %s", blockY, playerY, Math.abs(blockY - playerY), isValid));
            return isValid;
         } catch (Exception ex) {
            this.plugin.getLogger().warning("检查垂直分布时发生错误: " + ex.getMessage());
            return false;
         }
      } else {
         this.plugin.logDebug("无效的位置或玩家参数");
         return false;
      }
   }
}
