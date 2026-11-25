package sakuyaayane.nekoantixray;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import java.util.UUID;
import org.bukkit.entity.Player;

public class DecoratorPacketListener extends PacketListenerAbstract {
   private final NekoAntiXray plugin;
   private final DecoratorSeedObfuscator decoratorSeedObfuscator;
   private final boolean debug;

   public DecoratorPacketListener(NekoAntiXray plugin, ConfigManager configManager, PlayerBlockStateCache playerBlockStateCache) {
      super(PacketListenerPriority.HIGHEST);
      this.plugin = plugin;
      this.decoratorSeedObfuscator = new DecoratorSeedObfuscator(configManager, playerBlockStateCache);
      this.debug = plugin.getConfig().getBoolean("debug", false);
   }

   @Override
   public void onPacketSend(PacketSendEvent event) {
      if (!event.isCancelled() && event.getPacketType() == Server.CHUNK_DATA) {
         Player player = event.getPlayer();
         if (player != null) {
            try {
               WrapperPlayServerChunkData chunkDataPacket = new WrapperPlayServerChunkData(event);
               User user = event.getUser();
               UUID playerUuid = player.getUniqueId();
               String worldName = player.getWorld().getName();
               DecoratorSeedObfuscator.PacketObfuscationResult packetObfuscationResult = this.decoratorSeedObfuscator.obfuscateChunkPacket(chunkDataPacket, user, playerUuid, worldName);
               if (!packetObfuscationResult.hasChanges()) {
                  return;
               }

               event.markForReEncode(true);
               if (this.debug) {
                  String playerName = player.getName();
                  int chunkX = chunkDataPacket.getColumn().getX();
                  int chunkZ = chunkDataPacket.getColumn().getZ();
                  int modifiedBlocks = packetObfuscationResult.getModifiedBlocks();
                  int strippedBlockEntities = packetObfuscationResult.getStrippedBlockEntities();
                  String debugMessage = "[AntiSeedCracker] Decorator packet obfuscated for "
                     + playerName
                     + " chunk=("
                     + chunkX
                     + ","
                     + chunkZ
                     + ") blocks="
                     + modifiedBlocks
                     + " blockEntities="
                     + strippedBlockEntities;
                  this.plugin.logDebug(debugMessage);
               }
            } catch (Throwable ex) {
               if (this.debug) {
                  String errorMessage = "[AntiSeedCracker] Decorator packet obfuscation failed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
                  this.plugin.logDebug(errorMessage);
               }
            }
         }
      }
   }
}
