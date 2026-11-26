package sakuyaayane.nekoantixray.chunk;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.World;
import sakuyaayane.nekoantixray.NekoAntiXray;

public final class BiomePacketObfuscator {
   public static final int MIN_DISTINCT_BIOMES = 2;
   private static final int BIOME_GRID_SIZE = 4;
   private final NekoAntiXray plugin;
   private final long baseSalt;
   private final boolean perPlayerSalt;
   private final boolean overworldOnly;
   private final int minDistanceChunks;
   private final int cellsPerSection;

   public BiomePacketObfuscator(NekoAntiXray plugin) {
      this.plugin = plugin;
      long configuredSalt = plugin.getConfig().getLong("antiSeedCracker.biomePacketObfuscation.salt", 0L);
      this.baseSalt = configuredSalt == 0L ? this.generateDefaultSalt() : configuredSalt;
      this.perPlayerSalt = plugin.getConfig().getBoolean("antiSeedCracker.biomePacketObfuscation.perPlayerSalt", true);
      this.overworldOnly = plugin.getConfig().getBoolean("antiSeedCracker.biomePacketObfuscation.overworldOnly", true);
      this.minDistanceChunks = Math.max(0, plugin.getConfig().getInt("antiSeedCracker.biomePacketObfuscation.minDistanceChunks", 0));
      this.cellsPerSection = getAntiSeedCrackerBiomePacketObfuscationCellsPerSection(plugin);
   }

   public boolean isOverworldOnly() {
      return this.overworldOnly;
   }

   private static int getAntiSeedCrackerBiomePacketObfuscationCellsPerSection(NekoAntiXray plugin) {
      String mode = plugin.getConfig().getString("antiSeedCracker.biomePacketObfuscation.mode", "moderate");
      mode = mode == null ? "moderate" : mode.trim().toLowerCase(Locale.ROOT);
      switch (mode) {
         case "surgical":
            return 1;
         case "aggressive":
            return 16;
         case "moderate":
         default:
            int configuredCells = plugin.getConfig().getInt("antiSeedCracker.biomePacketObfuscation.cellsPerSection", 3);
            return Math.max(1, Math.min(16, configuredCells));
      }
   }

   public boolean shouldObfuscateChunk(Player player, int chunkX, int chunkZ) {
      if (player == null || !player.isOnline()) {
         return false;
      }

      if (this.minDistanceChunks <= 0) {
         return true;
      }

      int playerChunkX = player.getLocation().getBlockX() >> 4;
      int playerChunkZ = player.getLocation().getBlockZ() >> 4;
      int deltaChunkX = Math.abs(playerChunkX - chunkX);
      int deltaChunkZ = Math.abs(playerChunkZ - chunkZ);
      return Math.max(deltaChunkX, deltaChunkZ) >= this.minDistanceChunks;
   }

   private static long mixHash(long hash, long input) {
      return hash ^ input + -7046029254386353131L + (hash << 6) + (hash >>> 2);
   }

   private static long finalizeHash(long hash) {
      hash ^= hash >>> 33;
      hash *= -49064778989728563L;
      hash ^= hash >>> 33;
      hash *= -4265267296055464877L;
      return hash ^ hash >>> 33;
   }

   private String getWorldIdentifier(World world) {
      try {
         return world.getKey().toString();
      } catch (Throwable ex) {
         return world.getEnvironment().name();
      }
   }

   private long generateDefaultSalt() {
      return System.nanoTime() ^ Runtime.getRuntime().freeMemory() ^ 3114645857374772L;
   }

   private long computeCellHash(Player player, int chunkX, int chunkZ, int sectionY, int cellIndex) {
      UUID playerUuid = player.getUniqueId();
      long hash = this.baseSalt ^ -7046029254386353131L;
      if (this.perPlayerSalt) {
         hash = mixHash(hash, playerUuid.getMostSignificantBits());
         hash = mixHash(hash, playerUuid.getLeastSignificantBits());
      }

      World world = player.getWorld();
      hash = mixHash(hash, world.getName().hashCode());
      hash = mixHash(hash, this.getWorldIdentifier(world).hashCode());
      hash = mixHash(hash, chunkX);
      hash = mixHash(hash, chunkZ);
      hash = mixHash(hash, sectionY);
      hash = mixHash(hash, cellIndex);
      return finalizeHash(hash);
   }

   public boolean[] selectObfuscatedColumns(Player player, int chunkX, int chunkZ, int sectionY) {
      boolean[] selectedColumns = new boolean[16];
      if (this.cellsPerSection >= 16) {
         Arrays.fill(selectedColumns, true);
         return selectedColumns;
      }

      byte centerColumn = 10;
      selectedColumns[centerColumn] = true;
      int additionalCells = this.cellsPerSection - 1;
      if (additionalCells <= 0) {
         return selectedColumns;
      }

      int[] candidateColumns = new int[15];
      long[] candidateHashes = new long[15];
      int candidateCount = 0;

      for (int gridX = 0; gridX < 4; gridX++) {
         for (int gridZ = 0; gridZ < 4; gridZ++) {
            int columnIndex = gridX << 2 | gridZ;
            if (columnIndex != centerColumn) {
               candidateColumns[candidateCount] = columnIndex;
               candidateHashes[candidateCount] = this.computeCellHash(player, chunkX, chunkZ, sectionY, columnIndex);
               candidateCount++;
            }
         }
      }

      for (int selectedIdx = 0; selectedIdx < additionalCells && selectedIdx < candidateCount; selectedIdx++) {
         int minHashIdx = selectedIdx;

         for (int scanIdx = selectedIdx + 1; scanIdx < candidateCount; scanIdx++) {
            if (candidateHashes[scanIdx] < candidateHashes[minHashIdx]) {
               minHashIdx = scanIdx;
            }
         }

         long swapHash = candidateHashes[selectedIdx];
         candidateHashes[selectedIdx] = candidateHashes[minHashIdx];
         candidateHashes[minHashIdx] = swapHash;
         int swapColumn = candidateColumns[selectedIdx];
         candidateColumns[selectedIdx] = candidateColumns[minHashIdx];
         candidateColumns[minHashIdx] = swapColumn;
         selectedColumns[candidateColumns[selectedIdx]] = true;
      }

      return selectedColumns;
   }

   private int pickAlternativeBiome(Player player, int[] candidateBiomes, int originalBiomeId, int chunkX, int chunkZ, int sectionY, int cellIndex) {
      if (candidateBiomes != null && candidateBiomes.length != 0) {
         long cellHash = this.computeCellHash(player, chunkX, chunkZ, sectionY, cellIndex);
         int startIndex = Math.floorMod((int)cellHash, candidateBiomes.length);
         int candidateBiomeId = candidateBiomes[startIndex];
         if (candidateBiomeId != originalBiomeId) {
            return candidateBiomeId;
         }

         for (int offset = 1; offset < candidateBiomes.length; offset++) {
            candidateBiomeId = candidateBiomes[(startIndex + offset) % candidateBiomes.length];
            if (candidateBiomeId != originalBiomeId) {
               return candidateBiomeId;
            }
         }

         return originalBiomeId;
      } else {
         return originalBiomeId;
      }
   }

   public int obfuscateBiomeId(Player player, int originalBiomeId, int[] candidateBiomes, int chunkX, int chunkZ, int sectionY, int cellIndex) {
      return candidateBiomes != null && candidateBiomes.length >= 2
         ? this.pickAlternativeBiome(player, candidateBiomes, originalBiomeId, chunkX, chunkZ, sectionY, cellIndex)
         : originalBiomeId;
   }
}