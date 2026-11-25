package sakuyaayane.nekoantixray;

public interface StatusTextProvider {
   boolean onCommand();

   String getDescription();

   String getUsage();
}
