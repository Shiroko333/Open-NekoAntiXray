package sakuyaayane.nekoantixray.util;

import java.util.Arrays;

public final class LongBooleanHashMap {
   private static final byte EMPTY = 0;
   private static final byte OCCUPIED = 1;
   private static final byte REMOVED = 2;
   private static final float LOAD_FACTOR = 0.7F;
   private long[] keys;
   private boolean[] values;
   private byte[] states;
   private int size;
   private int usedSlots;
   private int mask;
   private int threshold;

   private LongBooleanHashMap(LongBooleanHashMap other) {
      this.keys = Arrays.copyOf(other.keys, other.keys.length);
      this.values = Arrays.copyOf(other.values, other.values.length);
      this.states = Arrays.copyOf(other.states, other.states.length);
      this.size = other.size;
      this.usedSlots = other.usedSlots;
      this.mask = other.mask;
      this.threshold = other.threshold;
   }

   public LongBooleanHashMap isEmpty() {
      return new LongBooleanHashMap(this);
   }

   public int size() {
      return this.size;
   }

   public void clear() {
      Arrays.fill(this.states, (byte)0);
      this.size = 0;
      this.usedSlots = 0;
   }

   public LongBooleanHashMap.EntryCursor entryCursor() {
      return new LongBooleanHashMap.EntryCursor();
   }

   private void allocateTable(int capacity) {
      this.keys = new long[capacity];
      this.values = new boolean[capacity];
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

   public boolean isMapEmpty() {
      return this.size == 0;
   }

   public void put(long key, boolean entryValue) {
      int slot = this.findInsertionSlot(key);
      if (this.states[slot] == 1 && this.keys[slot] == key) {
         this.values[slot] = entryValue;
      } else {
         this.keys[slot] = key;
         this.values[slot] = entryValue;
         if (this.states[slot] == 0) {
            this.usedSlots++;
         }

         this.states[slot] = 1;
         this.size++;
         if (this.usedSlots >= this.threshold) {
            this.rehash(this.keys.length << 1);
         }
      }
   }

   private void rehash(int newCapacity) {
      long[] oldKeys = this.keys;
      this.allocateTable(newCapacity);

      for (int i = 0; i < oldKeys.length; i++) {
         if (this.states[i] == 1) {
            this.put(oldKeys[i], this.values[i]);
         }
      }
   }

   public LongBooleanHashMap(int expectedSize) {
      byte capacity = 16;

      while (capacity < expectedSize * 2) {
         capacity = (byte)(capacity << 1);
      }

      this.allocateTable(capacity);
   }

   private int slotIndex(long key) {
      return mixHash(key) & this.mask;
   }

   public LongBooleanHashMap() {
      this(16);
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

   public boolean containsKey(long key) {
      return this.findSlot(key) >= 0;
   }

   public boolean get(long key) {
      int slot = this.findSlot(key);
      return slot >= 0 && this.values[slot];
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

   public final class EntryCursor {
      private int index = -1;

      public long key() {
         return LongBooleanHashMap.this.keys[this.index];
      }

      public boolean value() {
         return LongBooleanHashMap.this.values[this.index];
      }

      public boolean advance() {
         for (int i = this.index + 1; i < LongBooleanHashMap.this.states.length; i++) {
            byte state = LongBooleanHashMap.this.states[i];
            if (state == 1) {
               this.index = i;
               return true;
            }
         }

         this.index = LongBooleanHashMap.this.states.length;
         return false;
      }

      public void setValue(boolean entryValue) {
         LongBooleanHashMap.this.values[this.index] = entryValue;
      }

      public void remove() {
         if (this.index >= 0) {
            if (this.index >= LongBooleanHashMap.this.states.length) {
               return;
            }

            if (LongBooleanHashMap.this.states[this.index] != 1) {
               return;
            }

            LongBooleanHashMap.this.states[this.index] = 2;
            LongBooleanHashMap.this.size--;
         }
      }
   }
}
