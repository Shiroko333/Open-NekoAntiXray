package sakuyaayane.nekoantixray.compat;

import java.lang.reflect.Field;
import org.bukkit.Bukkit;
import sakuyaayane.nekoantixray.NekoAntiXray;

public final class LeafCompatibilitySupport {
   private LeafCompatibilitySupport() {
   }

   public static Boolean isAsyncChunkSendEnabled() {
      try {
         Class<?> asyncChunkSendClass = Class.forName("org.dreeam.leaf.config.modules.async.AsyncChunkSend");
         Field enabledField = asyncChunkSendClass.getDeclaredField("enabled");
         enabledField.setAccessible(true);
         return enabledField.getBoolean(null);
      } catch (Throwable ex) {
         return null;
      }
   }

   public static boolean isLeafServer() {
      try {
         Class.forName("org.dreeam.leaf.Leaf");
         return true;
      } catch (Throwable ex) {
         return false;
      }
   }

   public static void logCompatibilityDiagnostics(NekoAntiXray plugin) {
      try {
         if (!isLeafServer()) {
            return;
         }

         Boolean asyncChunkSendEnabled = isAsyncChunkSendEnabled();
         String version = Bukkit.getVersion();
         String bukkitVersion = Bukkit.getBukkitVersion();
         plugin.getLogger().info("[兼容性] 检测到 Leaf 核心 (" + version + " / " + bukkitVersion + ")");
         if (Boolean.TRUE.equals(asyncChunkSendEnabled)) {
            plugin.getLogger().info("[兼容性] async-chunk-send 已开启，插件将使用 leaf$modifyBlocks 兼容路径处理区块混淆。");
         } else {
            plugin.getLogger().info("[兼容性] async-chunk-send 未开启，插件将使用标准 modifyBlocks 路径处理区块混淆。");
         }

         String combinedVersion = version + " " + bukkitVersion;
         if (combinedVersion.contains("26.1.2")) {
            plugin.getLogger()
               .warning("[兼容性] 检测到 Leaf 26.1.2：该版本线已被 Leaf 官方标记为 unsupported（仅发布 Build 2/3），并被 26.2 取代。若反X-Ray异常请优先升级到 26.2 Build 34+（含官方反X-Ray插件兼容修复）。");
         } else if (combinedVersion.contains("1.21.11")) {
            plugin.getLogger()
               .warning(
                  "[兼容性] 检测到 Leaf 1.21.11 线：async chunk sender 的已知缺陷（追踪顺序/NPE/size计数）在 Build 93~149 期间存在，建议升级到 Build 150+；反X-Ray插件兼容修复(#823)尚未移植到该线，插件会依赖 leaf$modifyBlocks 兼容路径。"
               );
         }
      } catch (Throwable ex) {
         plugin.logDebug("Leaf 兼容性探测失败: " + ex);
      }
   }
}
