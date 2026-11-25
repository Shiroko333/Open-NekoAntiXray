package sakuyaayane.nekoantixray;

public class Blake2bSeedHasher {
   private static final long[] BLAKE2B_IV = new long[]{
      7640891576956012808L,
      -4942790177534073029L,
      4354685564936845355L,
      -6534734903238641935L,
      5840696475078001361L,
      -7276294671716946913L,
      2270897969802886507L,
      6620516959819538809L
   };
   private static final byte[][] BLAKE2B_SIGMA = new byte[][]{
      {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
      {14, 10, 4, 8, 9, 15, 13, 6, 1, 12, 0, 2, 11, 7, 5, 3},
      {11, 8, 12, 0, 5, 2, 15, 13, 10, 14, 3, 6, 7, 1, 9, 4},
      {7, 9, 3, 1, 13, 12, 11, 14, 2, 6, 5, 10, 4, 0, 15, 8},
      {9, 0, 5, 7, 2, 4, 10, 15, 14, 1, 11, 12, 6, 8, 3, 13},
      {2, 12, 6, 10, 0, 11, 8, 3, 4, 13, 7, 5, 15, 14, 1, 9},
      {12, 5, 1, 15, 14, 13, 4, 10, 0, 7, 6, 3, 9, 2, 8, 11},
      {13, 11, 7, 14, 12, 1, 3, 9, 5, 0, 15, 4, 8, 6, 2, 10},
      {6, 15, 14, 9, 11, 3, 0, 8, 12, 2, 13, 7, 1, 4, 10, 5},
      {10, 2, 8, 4, 7, 6, 1, 5, 15, 11, 9, 14, 3, 12, 13, 0},
      {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
      {14, 10, 4, 8, 9, 15, 13, 6, 1, 12, 0, 2, 11, 7, 5, 3}
   };
   private static final int ROTATION_R1 = 32;
   private static final int ROTATION_R2 = 24;
   private static final int ROTATION_R3 = 16;
   private static final int ROTATION_R4 = 63;

   private static void mixQuarterRound(long firstWord, long secondWord, int indexA, int indexB, int indexC, int indexD, long[] state) {
      state[indexA] = state[indexA] + state[indexB] + firstWord;
      state[indexD] = Long.rotateRight(state[indexD] ^ state[indexA], 32);
      state[indexC] += state[indexD];
      state[indexB] = Long.rotateRight(state[indexB] ^ state[indexC], 24);
      state[indexA] = state[indexA] + state[indexB] + secondWord;
      state[indexD] = Long.rotateRight(state[indexD] ^ state[indexA], 16);
      state[indexC] += state[indexD];
      state[indexB] = Long.rotateRight(state[indexB] ^ state[indexC], 63);
   }

   public static void compressBlock(long[] messageBlock, long[] chainValue, long[] internalState, long bytesCompressed, boolean isLastBlock) {
      if (messageBlock.length != 16) {
         throw new IllegalArgumentException("Message block must be 16 longs (128 bytes)");
      }

      if (chainValue.length != 8) {
         throw new IllegalArgumentException("Chain value must be 8 longs (64 bytes)");
      }

      if (internalState.length != 16) {
         throw new IllegalArgumentException("Internal state must be 16 longs (128 bytes)");
      }

      System.arraycopy(chainValue, 0, internalState, 0, 8);
      System.arraycopy(BLAKE2B_IV, 0, internalState, 8, 4);
      internalState[12] = bytesCompressed ^ BLAKE2B_IV[4];
      internalState[13] = bytesCompressed >>> 32 ^ BLAKE2B_IV[5];
      if (isLastBlock) {
         internalState[14] = ~BLAKE2B_IV[6];
      } else {
         internalState[14] = BLAKE2B_IV[6];
      }

      internalState[15] = BLAKE2B_IV[7];

      for (int round = 0; round < 12; round++) {
         byte[] sigma = BLAKE2B_SIGMA[round % 12];
         mixQuarterRound(messageBlock[sigma[0]], messageBlock[sigma[1]], 0, 4, 8, 12, internalState);
         mixQuarterRound(messageBlock[sigma[2]], messageBlock[sigma[3]], 1, 5, 9, 13, internalState);
         mixQuarterRound(messageBlock[sigma[4]], messageBlock[sigma[5]], 2, 6, 10, 14, internalState);
         mixQuarterRound(messageBlock[sigma[6]], messageBlock[sigma[7]], 3, 7, 11, 15, internalState);
         mixQuarterRound(messageBlock[sigma[8]], messageBlock[sigma[9]], 0, 5, 10, 15, internalState);
         mixQuarterRound(messageBlock[sigma[10]], messageBlock[sigma[11]], 1, 6, 11, 12, internalState);
         mixQuarterRound(messageBlock[sigma[12]], messageBlock[sigma[13]], 2, 7, 8, 13, internalState);
         mixQuarterRound(messageBlock[sigma[14]], messageBlock[sigma[15]], 3, 4, 9, 14, internalState);
      }

      for (int i = 0; i < 8; i++) {
         chainValue[i] ^= internalState[i] ^ internalState[i + 8];
      }
   }

   public static long[] hashWorldSeed(long[] worldSeed) {
      if (worldSeed.length != 16) {
         throw new IllegalArgumentException("Invalid world seed length");
      }

      long[] chainValue = (long[])BLAKE2B_IV.clone();
      chainValue[0] ^= 16842816L;
      long[] internalState = new long[16];
      compressBlock(worldSeed, chainValue, internalState, 128L, true);
      return chainValue;
   }

   public static long[] hashBytes(byte[] input) {
      long[] chainValue = (long[])BLAKE2B_IV.clone();
      chainValue[0] ^= 16842816L;
      int paddedLength = (input.length + 127) / 128 * 128;
      byte[] paddedInput = new byte[paddedLength];
      System.arraycopy(input, 0, paddedInput, 0, input.length);
      long[] internalState = new long[16];
      long[] messageBlock = new long[16];

      for (short blockOffset = 0; blockOffset < paddedLength; blockOffset = (short)(blockOffset + 128)) {
         for (int wordIndex = 0; wordIndex < 16; wordIndex++) {
            long word = 0L;

            for (int byteIndex = 0; byteIndex < 8; byteIndex++) {
               int inputIndex = blockOffset + wordIndex * 8 + byteIndex;
               if (inputIndex < paddedInput.length) {
                  word |= (long)(paddedInput[inputIndex] & 0xFF) << byteIndex * 8;
               }
            }

            messageBlock[wordIndex] = word;
         }

         boolean isLastBlock = blockOffset + 128 >= paddedLength;
         long bytesCompressed = Math.min(blockOffset + 128, input.length);
         compressBlock(messageBlock, chainValue, internalState, bytesCompressed, isLastBlock);
      }

      return chainValue;
   }

   public static long[] hashFeatureSeed(int chunkX, int chunkZ, int dimensionId, WorldSeedResolver.SeedSalt seedSalt, long extraSalt) {
      byte[] seedBytes = new byte[32];
      seedBytes[0] = (byte)chunkX;
      seedBytes[1] = (byte)(chunkX >>> 8);
      seedBytes[2] = (byte)(chunkX >>> 16);
      seedBytes[3] = (byte)(chunkX >>> 24);
      seedBytes[4] = (byte)chunkZ;
      seedBytes[5] = (byte)(chunkZ >>> 8);
      seedBytes[6] = (byte)(chunkZ >>> 16);
      seedBytes[7] = (byte)(chunkZ >>> 24);
      seedBytes[8] = (byte)dimensionId;
      seedBytes[9] = (byte)(dimensionId >>> 8);
      seedBytes[10] = (byte)(dimensionId >>> 16);
      seedBytes[11] = (byte)(dimensionId >>> 24);
      int saltValue = seedSalt.getValue();
      seedBytes[12] = (byte)saltValue;
      seedBytes[13] = (byte)(saltValue >>> 8);
      seedBytes[14] = (byte)(saltValue >>> 16);
      seedBytes[15] = (byte)(saltValue >>> 24);
      seedBytes[16] = (byte)extraSalt;
      seedBytes[17] = (byte)(extraSalt >>> 8);
      seedBytes[18] = (byte)(extraSalt >>> 16);
      seedBytes[19] = (byte)(extraSalt >>> 24);
      seedBytes[20] = (byte)(extraSalt >>> 32);
      seedBytes[21] = (byte)(extraSalt >>> 40);
      seedBytes[22] = (byte)(extraSalt >>> 48);
      seedBytes[23] = (byte)(extraSalt >>> 56);
      return hashBytes(seedBytes);
   }
}