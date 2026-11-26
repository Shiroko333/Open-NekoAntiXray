package sakuyaayane.nekoantixray.util;

public interface StatusTextProvider {
   boolean onCommand();

   String getDescription();

   String getUsage();
}
