package sakuyaayane.nekoantixray;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Server;

public class ServerVersionAccess {
   private static final Pattern MC_VERSION_PATTERN = Pattern.compile("\\(MC: (\\d+\\.\\d+(\\.\\d+)?)\\)");

   public static String getMinecraftVersion(Server server) {
      try {
         Class clazz = server.getClass();
         Class[] clazzs = new Class[0];
         Method method = clazz.getMethod("getMinecraftVersion", clazzs);
         return (String)method.invoke(server);
      } catch (Exception e) {
         String bukkitVersion = server.getBukkitVersion();
         Matcher matcher = MC_VERSION_PATTERN.matcher(bukkitVersion);
         if (matcher.find()) {
            return matcher.group(1);
         }

         String[] strings = bukkitVersion.split("-");
         return strings.length > 0 ? strings[0] : "unknown";
      }
   }
}
