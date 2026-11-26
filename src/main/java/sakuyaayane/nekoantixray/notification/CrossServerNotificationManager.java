package sakuyaayane.nekoantixray.notification;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import sakuyaayane.nekoantixray.NekoAntiXray;

public class CrossServerNotificationManager {
   public static void sendCrossServerAlert(String playerName, String locationInfo, String worldName, double violationLevel) {
      Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
      if (!onlinePlayers.isEmpty()) {
         ByteArrayDataOutput bungeePayload = ByteStreams.newDataOutput();
         bungeePayload.writeUTF("Forward");
         bungeePayload.writeUTF("ALL");
         bungeePayload.writeUTF("nekoantixray:bungee");
         ByteArrayOutputStream alertBuffer = new ByteArrayOutputStream();
         DataOutputStream alertStream = new DataOutputStream(alertBuffer);

         try {
            alertStream.writeUTF(playerName);
            alertStream.writeUTF(locationInfo);
            alertStream.writeUTF(worldName);
            alertStream.writeDouble(violationLevel);
         } catch (IOException ex) {
            ex.printStackTrace();
         }

         bungeePayload.writeShort(alertBuffer.toByteArray().length);
         bungeePayload.write(alertBuffer.toByteArray());
         Player carrierPlayer = Iterables.getFirst(onlinePlayers, null);
         if (carrierPlayer != null) {
            carrierPlayer.sendPluginMessage(NekoAntiXray.getInstance(), "bungeecord:main", bungeePayload.toByteArray());
         }
      }
   }

   public static void sendPlayerMessage(Player player, String message) {
      player.sendMessage(message);
   }

   public static void broadcastToLocalOps(String playerName, String locationInfo, String worldName, double violationLevel) {
      String alertMessage = String.format(
         "§c警告：玩家 §e%s§c 的违规值正在持续上升！\n§7位置: §f%s\n§7世界: §f%s\n§7当前违规值: §f%.2f\n§a[点击此处传送到玩家位置]",
         playerName,
         locationInfo,
         worldName,
         violationLevel
      );
      TextComponent alertComponent = new TextComponent(alertMessage);
      alertComponent.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/tp " + playerName));
      alertComponent.setHoverEvent(
         new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new Text("点击传送到 " + playerName + " 的位置"))
      );
      Bukkit.getOnlinePlayers()
         .stream()
         .filter(recipient -> recipient.isOp() || recipient.hasPermission("nekoantixray.alerts"))
         .forEach(recipient -> recipient.spigot().sendMessage(alertComponent));
   }
}
