package sakuyaayane.nekoantixray.util;

import java.util.Arrays;

public final class LongHashSet {
   private static final byte EMPTY = 0;
   private static final byte OCCUPIED = 1;
   private static final byte REMOVED = 2;
   private static final float LOAD_FACTOR = 0.7F;
   private long[] keys;
   private byte[] states;
   private int size;
   private int usedSlots;
   private int mask;
   private int threshold;

   public LongHashSet() {
      this(16);
   }

   public LongHashSet(int expectedSize) {
      byte capacity = 16;

      while (capacity < expectedSize * 2) {
         capacity = (byte)(capacity << 1);
      }

      this.allocateTable(capacity);
   }

   public int size() {
      return this.size;
   }

   public void clear() {
      Arrays.fill(this.states, (byte)0);
      this.size = 0;
      this.usedSlots = 0;
   }

   private void allocateTable(int capacity) {
      this.keys = new long[capacity];
      this.states = new byte[capacity];
      this.mask = capacity - 1;
      this.threshold = (int)(capacity * 0.7F);
      this.size = 0;
      this.usedSlots = 0;
   }

   private static int mixHash(long key) {
      long mixed = key;
      mixed ^= mixed >>> 33;
      mixed *= -49064778989728563L;
      mixed ^= mixed >>> 33;
      mixed *= -4265267296055464877L;
      mixed ^= mixed >>> 33;
      return (int)mixed;
   }

   public boolean isEmpty() {
      return this.size == 0;
   }

   public boolean add(long key) {
      int slot = this.findInsertionSlot(key);
      if (this.states[slot] == 1 && this.keys[slot] == key) {
         return false;
      }

      this.keys[slot] = key;
      if (this.states[slot] == 0) {
         this.usedSlots++;
      }

      this.states[slot] = 1;
      this.size++;
      if (this.usedSlots >= this.threshold) {
         this.rehash(this.keys.length << 1);
      }

      return true;
   }

   private void rehash(int newCapacity) {
      long[] oldKeys = this.keys;
      this.allocateTable(newCapacity);

      for (int i = 0; i < oldKeys.length; i++) {
         if (this.states[i] == 1) {
            this.add(oldKeys[i]);
         }
      }
   }

   private int slotIndex(long key) {
      return mixHash(key) & this.mask;
   }

   private int findSlot(long key) {
      int slot = this.slotIndex(key);

      while (true) {
         byte state = this.states[slot];
         if (state == 0) {
            return -1;
         }

         if (state == 1 && this.keys[slot] == key) {
            return slot;
         }

         slot = slot + 1 & this.mask;
      }
   }

   private int findInsertionSlot(long key) {
      int slot = this.slotIndex(key);
      int firstRemovedSlot = -1;

      while (true) {
         byte state = this.states[slot];
         if (state == 0) {
            return firstRemovedSlot >= 0 ? firstRemovedSlot : slot;
         }

         if (state == 1 && this.keys[slot] == key) {
            return slot;
         }

         if (state == 2 && firstRemovedSlot < 0) {
            firstRemovedSlot = slot;
         }

         slot = slot + 1 & this.mask;
      }
   }

   public boolean contains(long key) {
      return this.findSlot(key) >= 0;
   }

   public boolean remove(long key) {
      int slot = this.findSlot(key);
      if (slot < 0) {
         return false;
      }

      this.states[slot] = 2;
      this.size--;
      return true;
   }
}
