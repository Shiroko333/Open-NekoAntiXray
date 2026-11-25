package sakuyaayane.nekoantixray;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class CrossServerMessageListener implements PluginMessageListener {
   private static final String BUNGEE_CHANNEL = "nekoantixray:bungee";

   @Override
   public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
      if (channel.equals("nekoantixray:bungee")) {
         ByteArrayDataInput dataInput = ByteStreams.newDataInput(message);
         String playerName = dataInput.readUTF();
         String locationText = dataInput.readUTF();
         String worldName = dataInput.readUTF();
         double violationLevel = dataInput.readDouble();
         CrossServerNotificationManager.broadcastToLocalOps(playerName, locationText, worldName, violationLevel);
      }
   }
}