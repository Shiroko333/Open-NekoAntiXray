package sakuyaayane.nekoantixray.lib.folialib.type;

public class Ref<T> {
   private T value;

   public Ref() {
      this.value = null;
   }

   public Ref(T var1) {
      this.value = (T)var1;
   }

   public T get() {
      return this.value;
   }

   public void set(T var1) {
      this.value = (T)var1;
   }
}
