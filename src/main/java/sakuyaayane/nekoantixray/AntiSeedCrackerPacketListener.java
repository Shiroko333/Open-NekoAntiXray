package sakuyaayane.nekoantixray;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.DataPalette;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import java.util.LinkedHashSet;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;

public class AntiSeedCrackerPacketListener extends PacketListenerAbstract {
   private static final int BIOME_GRID_SIZE = 4;
   private final NekoAntiXray plugin;
   private final BiomePacketObfuscator biomePacketObfuscator;
   private final boolean debug;

   public AntiSeedCrackerPacketListener(NekoAntiXray plugin) {
      super(PacketListenerPriority.HIGHEST);
      this.plugin = plugin;
      this.biomePacketObfuscator = new BiomePacketObfuscator(plugin);
      this.debug = plugin.getConfig().getBoolean("debug", false);
   }

   @Override
   public void onPacketSend(PacketSendEvent event) {
      if (!event.isCancelled() && event.getPacketType() == Server.CHUNK_DATA) {
         Player player = event.getPlayer();
         if (player != null) {
            if (this.biomePacketObfuscator.isOverworldOnly()) {
               World world = player.getWorld();
               if (world == null || world.getEnvironment() != Environment.NORMAL) {
                  return;
               }
            }

            long startNanos = System.nanoTime();

            try {
               WrapperPlayServerChunkData chunkDataPacket = new WrapperPlayServerChunkData(event);
               Column column = chunkDataPacket.getColumn();
               if (column == null || column.getChunks() == null) {
                  return;
               }

               int chunkX = column.getX();
               int chunkZ = column.getZ();
               if (this.biomePacketObfuscator.shouldObfuscateChunk(player, chunkX, chunkZ)) {
                  int changedCount = this.obfuscateChunkColumnBiomes(player, column, chunkX, chunkZ);
                  if (changedCount <= 0) {
                     return;
                  }

                  event.markForReEncode(true);
                  if (this.debug) {
                     this.plugin
                        .logDebug(
                           "[AntiSeedCracker] Biome packet obfuscated for "
                              + player.getName()
                              + " chunk=("
                              + chunkX
                              + ","
                              + chunkZ
                              + ") changed="
                              + changedCount
                        );
                  }

                  return;
               }
            } catch (Throwable ex) {
               if (this.debug) {
                  this.plugin.logDebug("[AntiSeedCracker] Biome packet obfuscation failed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
               }

               return;
            } finally {
               ModuleTimingStatistics.record("AntiSeedCracker-生物群系", System.nanoTime() - startNanos);
            }

            return;
         }
      }
   }

   private int[] collectUniqueBiomes(DataPalette dataPalette) {
      LinkedHashSet<Integer> uniqueBiomes = new LinkedHashSet<>();

      for (int y = 0; y < 4; y++) {
         for (int z = 0; z < 4; z++) {
            for (int x = 0; x < 4; x++) {
               uniqueBiomes.add(dataPalette.get(x, y, z));
            }
         }
      }

      int[] biomeIds = new int[uniqueBiomes.size()];
      int index = 0;

      for (int biomeId : uniqueBiomes) {
         biomeIds[index++] = biomeId;
      }

      return biomeIds;
   }

   private int obfuscateChunkColumnBiomes(Player player, Column column, int chunkX, int chunkZ) {
      BaseChunk[] chunks = column.getChunks();
      int changedCount = 0;

      for (int sectionIndex = 0; sectionIndex < chunks.length; sectionIndex++) {
         if (chunks[sectionIndex] instanceof Chunk_v1_18 chunkSection) {
            DataPalette biomeData = chunkSection.getBiomeData();
            if (biomeData != null) {
               int[] uniqueBiomes = this.collectUniqueBiomes(biomeData);
               if (uniqueBiomes.length >= 2) {
                  boolean[] obfuscatedColumns = this.biomePacketObfuscator.selectObfuscatedColumns(player, chunkX, chunkZ, sectionIndex);

                  for (int z = 0; z < 4; z++) {
                     for (int x = 0; x < 4; x++) {
                        if (obfuscatedColumns[z << 2 | x]) {
                           for (int y = 0; y < 4; y++) {
                              int originalBiome = biomeData.get(x, y, z);
                              int localIndex = y << 4 | z << 2 | x;
                              int obfuscatedBiome = this.biomePacketObfuscator
                                 .obfuscateBiomeId(player, originalBiome, uniqueBiomes, chunkX, chunkZ, sectionIndex, localIndex);
                              if (obfuscatedBiome != originalBiome) {
                                 biomeData.set(x, y, z, obfuscatedBiome);
                                 changedCount++;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return changedCount;
   }
}
