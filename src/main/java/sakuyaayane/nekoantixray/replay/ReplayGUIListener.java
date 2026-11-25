package sakuyaayane.nekoantixray.replay;

import java.io.File;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import sakuyaayane.nekoantixray.NekoAntiXray;

public class ReplayGUIListener implements Listener {
   private final NekoAntiXray plugin;
   private final ReplayCommand replayCommand;
   private final ReplaySessionManager sessionManager;

   public ReplayGUIListener(NekoAntiXray plugin, ReplayCommand replayCommand, ReplaySessionManager sessionManager) {
      this.plugin = plugin;
      this.replayCommand = replayCommand;
      this.sessionManager = sessionManager;
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getView().getTitle().startsWith("回放管理")) {
         event.setCancelled(true);
         if (!(event.getWhoClicked() instanceof Player)) {
            return;
         }

         Player player = (Player)event.getWhoClicked();
         ItemStack currentItem = event.getCurrentItem();
         if (currentItem == null || currentItem.getType() == Material.AIR) {
            return;
         }

         this.plugin.getFoliaScheduler().getImpl().runAtEntity(player, wrappedTask -> {
            if (currentItem.getType() == Material.BOOK) {
               String replayName = ChatColor.stripColor(currentItem.getItemMeta().getDisplayName());
               if (event.isLeftClick()) {
                  player.closeInventory();
                  this.replayCommand.startReplay(player, replayName);
               } else if (event.isRightClick()) {
                  this.replayCommand.deleteReplay(player, replayName);
                  this.replayCommand.openReplayGUI(player);
               }
            } else if (currentItem.getType() == Material.BARRIER) {
               player.closeInventory();
            } else if (currentItem.getType() == Material.PAPER) {
               int currentPage = Integer.parseInt(event.getView().getTitle().split(" ")[3]);
               if (currentItem.getItemMeta().getDisplayName().equals("§e上一页")) {
                  if (currentPage > 1) {
                     this.replayCommand.openReplayGUI(player, currentPage - 1);
                  }
               } else if (currentItem.getItemMeta().getDisplayName().equals("§e下一页")) {
                  File replaysDir = new File(this.plugin.getDataFolder(), "replays");
                  File[] replayFiles = replaysDir.listFiles((dir, fileName) -> fileName.endsWith(".replay"));
                  if (replayFiles != null && replayFiles.length > currentPage * 45) {
                     this.replayCommand.openReplayGUI(player, currentPage + 1);
                  }
               }
            } else if (currentItem.getType() == Material.HOPPER) {
               this.replayCommand.openReplayGUI(player);
            }
         });
      }
   }
}
