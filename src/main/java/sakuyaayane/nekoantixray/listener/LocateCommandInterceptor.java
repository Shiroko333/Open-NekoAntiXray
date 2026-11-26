package sakuyaayane.nekoantixray.listener;

import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import sakuyaayane.nekoantixray.NekoAntiXray;

public class LocateCommandInterceptor implements Listener {
   private final NekoAntiXray plugin;

   public LocateCommandInterceptor(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
      String message = event.getMessage();
      if (message != null && !message.isEmpty() && this.isLocateCommandInvocation(message)) {
         Player player = event.getPlayer();
         if (!player.hasPermission("nekoantixray.locate.bypass")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "[NekoAntiXray] Locate commands are disabled to prevent seed cracking.");
            if (this.plugin != null) {
               this.plugin.logDebug("[LocateCommandInterceptor] Blocked locate command from " + player.getName() + ": " + message);
            }
         }
      }
   }

   private boolean isLocateCommandInvocation(String rawCommand) {
      String normalized = rawCommand.trim();
      if (normalized.isEmpty()) {
         return false;
      }

      if (normalized.charAt(0) == '/') {
         normalized = normalized.substring(1);
      }

      if (normalized.isEmpty()) {
         return false;
      }

      String[] tokens = normalized.toLowerCase(Locale.ROOT).split("\\s+");
      if (tokens.length == 0) {
         return false;
      }

      if (this.isLocateToken(tokens[0])) {
         return true;
      }

      if (!this.isExecuteToken(tokens[0])) {
         return false;
      }

      for (int i = 1; i < tokens.length; i++) {
         if ("run".equals(tokens[i])) {
            if (i + 1 < tokens.length && this.isLocateToken(tokens[i + 1])) {
               return true;
            }

            for (int j = i + 1; j < tokens.length; j++) {
               if (this.isLocateToken(tokens[j])) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean isLocateToken(String token) {
      return "locate".equals(token) || "minecraft:locate".equals(token);
   }

   private boolean isExecuteToken(String token) {
      return "execute".equals(token) || "minecraft:execute".equals(token);
   }
}
