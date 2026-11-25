package sakuyaayane.nekoantixray;

import java.util.Random;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class FakeSeedCommand implements CommandExecutor, Listener {
   private final NekoAntiXray plugin;
   private final Random random;
   private boolean fakeSeedEnableFakeSeed;
   private boolean fakeSeedShowRealSeedToOp;
   private String seedMessageFormat;
   private String noPermissionMessage;

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!this.fakeSeedEnableFakeSeed) {
         return false;
      }

      if (!this.fakeSeedShowRealSeedToOp || !sender.hasPermission("fakeseed.viewreal")) {
         SecureSeedRandom secureSeedRandom = new SecureSeedRandom(System.currentTimeMillis(), sender.hashCode(), WorldSeedResolver.SeedSalt.UNDEFINED, 0L);
         long fakeSeed = secureSeedRandom.nextLong();
         sender.sendMessage(this.seedMessageFormat.replace("%seed%", String.valueOf(fakeSeed)));
      } else if (SecureSeedStatusFormatter.isSecureSystemActive()) {
         String securityStatus = SecureSeedStatusFormatter.formatSecurityStatus();
         sender.sendMessage(securityStatus);
         String globalSeedString = SecureSeedStatusFormatter.getGlobalSeedString();
         String truncatedSeedHash = globalSeedString.length() > 16 ? globalSeedString.substring(0, 16) + "..." : globalSeedString;
         sender.sendMessage(ChatColor.GRAY + "Secure Seed Hash: " + ChatColor.WHITE + truncatedSeedHash);
      } else {
         long seed = this.plugin.getServer().getWorlds().get(0).getSeed();
         sender.sendMessage(this.seedMessageFormat.replace("%seed%", String.valueOf(seed)));
      }

      return true;
   }

   @EventHandler
   public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
      if (this.fakeSeedEnableFakeSeed) {
         String commandMessage = event.getMessage().toLowerCase();
         if (commandMessage.startsWith("/seed")) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            if (!this.fakeSeedShowRealSeedToOp || !player.hasPermission("fakeseed.viewreal")) {
               SecureSeedRandom secureSeedRandom = new SecureSeedRandom(System.currentTimeMillis(), player.hashCode(), WorldSeedResolver.SeedSalt.UNDEFINED, 0L);
               long fakeSeed = secureSeedRandom.nextLong();
               player.sendMessage(this.seedMessageFormat.replace("%seed%", String.valueOf(fakeSeed)));
            } else if (SecureSeedStatusFormatter.isSecureSystemActive()) {
               String securityStatus = SecureSeedStatusFormatter.formatSecurityStatus();
               player.sendMessage(securityStatus);
               String globalSeedString = SecureSeedStatusFormatter.getGlobalSeedString();
               String truncatedSeedHash = globalSeedString.length() > 16 ? globalSeedString.substring(0, 16) + "..." : globalSeedString;
               player.sendMessage(ChatColor.GRAY + "Secure Seed Hash: " + ChatColor.WHITE + truncatedSeedHash);
            } else {
               long seed = this.plugin.getServer().getWorlds().get(0).getSeed();
               player.sendMessage(this.seedMessageFormat.replace("%seed%", String.valueOf(seed)));
            }
         }
      }
   }

   public void reloadConfig() {
      this.fakeSeedEnableFakeSeed = this.plugin.getConfig().getBoolean("fake-seed.enable-fake-seed", true);
      this.fakeSeedShowRealSeedToOp = this.plugin.getConfig().getBoolean("fake-seed.show-real-seed-to-op", false);
      this.seedMessageFormat = ChatColor.translateAlternateColorCodes(
         '&', this.plugin.getConfig().getString("fake-seed.messages.seed-message", "&aSeed: [&f%seed%&a]")
      );
      this.noPermissionMessage = ChatColor.translateAlternateColorCodes(
         '&', this.plugin.getConfig().getString("fake-seed.messages.no-permission", "&cYou don't have permission to view the seed.")
      );
   }

   public FakeSeedCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
      this.random = new Random();
      this.reloadConfig();
   }
}
