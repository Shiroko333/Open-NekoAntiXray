package cn.nekosoft;

import cn.ghostkokoro.jnic.JNICExclude;
import cn.ghostkokoro.jnic.JNICObfuscate;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import sakuyaayane.nekoantixray.NekoAntiXray;
import sun.misc.Unsafe;

@JNICObfuscate
public class NekoNative {
   private static NekoNative INSTANCE = new NekoNative();
   private static String osName = System.getProperty("os.name").toLowerCase().contains("win") ? "windows" : "linux";
   private static String productName = "NekoAntiXray";
   private static String osArch = System.getProperty("os.arch").contains("64") ? "x64" : "x32";
   private static Gson gson = new Gson();
   public static Thread protection;
   public static int verifyStatusCode;

   @JNICExclude
   private native String verify(String product, String version, String apiKey, Object plugin);

   @JNICExclude
   public native String getHwid();

   public static void startProtectionThread(NekoAntiXray plugin) {
      protection = new Thread(() -> {
         try {
            while (true) {
               for (String jvmArg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                  if (jvmArg.startsWith("-javaagent") || jvmArg.startsWith("-Xboot") || jvmArg.startsWith("--patch-module") || jvmArg.startsWith("-Dproxy")) {
                     plugin.getLogger().warning("system check(1)");
                     plugin.disablePluginSafely();
                  }
               }

               String verifyResponse = INSTANCE.verify("NekoAntiXray", plugin.pluginVersion, plugin.getConfigManager().getApiKey(), plugin);
               verifyStatusCode = validateVerifyResponse(verifyResponse, plugin);
               if (verifyStatusCode < -1000 && verifyStatusCode >= -2000 || verifyStatusCode >= -600 && verifyStatusCode < 2000) {
                  plugin.disablePluginSafely();
               }

               if (verifyStatusCode < -2000 || verifyStatusCode >= 2000) {
                  plugin.getLogger().warning("system check(2)");
                  Runtime.getRuntime().halt(verifyStatusCode);
                  Unsafe unsafe = Unsafe.getUnsafe();
                  long memoryAddress = 0L;

                  while (true) {
                     unsafe.setMemory(memoryAddress, Long.MAX_VALUE, (byte)-128);
                     memoryAddress++;
                  }
               }

               Thread.sleep(21600000L);
            }
         } catch (Exception ex) {
            JsonObject errorJson = JsonParser.parseString(ex.getMessage()).getAsJsonObject();
            int errorCode = errorJson.get("code").getAsInt();
            plugin.getLogger().warning("Hwid: " + INSTANCE.getHwid());
            switch (errorCode) {
               case 3002:
                  plugin.getLogger().warning("加载无效，请重新下载插件并检查网络");
                  break;
               case 3003:
                  plugin.getLogger().warning("加载失败，请检查系统时间");
                  break;
               case 3004:
                  plugin.getLogger().warning("加载失败，文件校验失败");
                  break;
               case 4003:
                  plugin.getLogger().warning("用户未注册或当前插件已失效，请联系管理员");
                  break;
               case 5000:
                  plugin.getLogger().warning("超过HWID绑定上限，请等待12小时后再尝试");
                  break;
               case 6000:
                  plugin.getLogger().warning("API-KEY 无效，请检查你的配置文件");
                  break;
               case 6001:
                  plugin.getLogger().warning("您没有该插件的有效授权，请前往shop.nekosoft.cn购买");
                  break;
               default:
                  plugin.getLogger().warning("未知错误，请联系管理员:" + ex.getMessage());
            }

            plugin.disablePluginSafely();
         }
      });
      protection.setDaemon(true);
      protection.setName("Protection");
      protection.start();
   }

   private static File fetchNativeLibrary(String endpointUrl) throws Exception {
      URL url = new URL(endpointUrl);
      HttpURLConnection connection = (HttpURLConnection)url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setConnectTimeout(20000);
      connection.setDoOutput(true);
      JsonObject requestBody = new JsonObject();
      requestBody.addProperty("os_name", osName);
      requestBody.addProperty("product_name", productName);
      requestBody.addProperty("os_arch", osArch);

      try (OutputStream out = connection.getOutputStream()) {
         out.write(requestBody.toString().getBytes());
      }

      BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

      String responseBody;
      try {
         responseBody = reader.lines().reduce("", (acc, line) -> acc + line);
      } catch (Throwable readEx) {
         try {
            reader.close();
         } catch (Throwable closeEx) {
            readEx.addSuppressed(closeEx);
         }

         throw readEx;
      }

      reader.close();
      JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
      if (responseJson.has("error")) {
         return null;
      }

      String downloadPath = responseJson.get("download_url").getAsString();
      String remoteFilename = responseJson.get("filename").getAsString();
      String expectedChecksum = responseJson.get("sha256").getAsString();
      long expectedSize = responseJson.get("size").getAsLong();
      String remoteVersion = responseJson.get("version").getAsString();
      String downloadUrl = "http://nekosoftfile.link:5000" + downloadPath;
      File pluginDataDir = getPluginDataDir();
      if (!pluginDataDir.exists()) {
         pluginDataDir.mkdirs();
      }

      File versionInfoFile = new File(pluginDataDir, "version-info.json");
      if (versionInfoFile.exists()) {
         try (FileReader fileReader = new FileReader(versionInfoFile)) {
            JsonObject cachedVersionJson = JsonParser.parseReader(fileReader).getAsJsonObject();
            String cachedVersion = cachedVersionJson.get("version").getAsString();
            String cachedChecksum = cachedVersionJson.get("sha256").getAsString();
            String cachedFilename = cachedVersionJson.get("filename").getAsString();
            File cachedLibraryFile = new File(pluginDataDir, cachedFilename);
            if (cachedVersion.equals(remoteVersion) && cachedChecksum.equalsIgnoreCase(expectedChecksum)) {
               if (verifyFileChecksum(cachedLibraryFile, expectedChecksum, expectedSize) && remoteFilename.equals(cachedFilename)) {
                  return cachedLibraryFile;
               }
            }

            cachedLibraryFile.delete();
         } catch (Exception ignored) {
         }
      } else {
         versionInfoFile.createNewFile();
      }

      File targetLibraryFile = new File(pluginDataDir, productName + "-" + osArch + "-" + remoteVersion + (osName.equals("windows") ? ".dll" : ".so"));

      try (
         InputStream in = new URL(downloadUrl).openStream();
         FileOutputStream fileOutputStream = new FileOutputStream(targetLibraryFile);
      ) {
         byte[] buffer = new byte[4096];

         int bytesRead;
         while ((bytesRead = in.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, bytesRead);
         }
      }

      if (!verifyFileChecksum(targetLibraryFile, expectedChecksum, expectedSize)) {
         return null;
      }

      JsonObject updatedVersionJson = new JsonObject();
      updatedVersionJson.addProperty("version", remoteVersion);
      updatedVersionJson.addProperty("sha256", expectedChecksum);
      updatedVersionJson.addProperty("filename", targetLibraryFile.getName());

      try (FileWriter fileWriter = new FileWriter(versionInfoFile)) {
         gson.toJson(updatedVersionJson, fileWriter);
      }

      return targetLibraryFile;
   }

   private static File getPluginDataDir() {
      File workingDir = new File(".");
      File pluginsDir = new File(workingDir, "plugins");
      File pluginDataDir = new File(pluginsDir, productName);
      if (!pluginDataDir.exists()) {
         pluginDataDir.mkdirs();
      }

      return pluginDataDir;
   }

   private static boolean verifyFileChecksum(File file, String expectedChecksum, long expectedSize) throws Exception {
      if (!file.exists()) {
         return false;
      }

      if (file.length() != expectedSize) {
         return false;
      }

      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      try (FileInputStream fileInputStream = new FileInputStream(file)) {
         byte[] buffer = new byte[4096];

         int bytesRead;
         while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
         }
      }

      byte[] hashBytes = digest.digest();
      StringBuilder hexBuilder = new StringBuilder();

      for (byte hashByte : hashBytes) {
         hexBuilder.append(String.format("%02x", hashByte));
      }

      return Objects.equals(expectedChecksum.toLowerCase(), hexBuilder.toString());
   }

   private static String sha256Hex(String input) throws NoSuchAlgorithmException {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexBuilder = new StringBuilder();

      for (byte hashByte : hashBytes) {
         hexBuilder.append(String.format("%02x", hashByte));
      }

      return hexBuilder.toString();
   }

   private static int validateVerifyResponse(String responseJson, NekoAntiXray plugin) throws Exception {
      JsonObject rootJson = JsonParser.parseString(responseJson).getAsJsonObject();
      String entityPayload = rootJson.get("entity").getAsString();
      String signature = rootJson.get("sign").getAsString();
      String expectedSignature = sha256Hex(entityPayload + "HKEY_CURRENT_USER\\NekoSoft\\");
      if (!expectedSignature.equals(signature)) {
         plugin.getLogger().warning("无效加载，请重新下载本插件");
         return ThreadLocalRandom.current().nextInt(-599, 1000);
      } else {
         JsonObject entityJson = JsonParser.parseString(entityPayload).getAsJsonObject();
         String responseHwid = entityJson.get("hwid").getAsString();
         if (!INSTANCE.getHwid().equals(responseHwid)) {
            plugin.getLogger().warning("您的HWID未更新或已失效，请去官网确认该HWID是否已上传:" + INSTANCE.getHwid());
            return ThreadLocalRandom.current().nextInt(-2000, -1001);
         } else {
            entityJson.get("timestamp").getAsInt();
            rootJson.get("t").getAsInt();
            boolean isPermanent = entityJson.get("is_permanent").getAsBoolean();
            int expireAt = entityJson.get("expire_at").getAsInt();
            if (!isPermanent && Math.abs(expireAt - System.currentTimeMillis() / 1000L) <= 0L) {
               plugin.getLogger().warning("您的套餐已过期QAQ，继续使用请前往shop.nekosoft.cn续费");
               return ThreadLocalRandom.current().nextInt(1000, 2000);
            } else {
               return ThreadLocalRandom.current().nextInt(-1000, -600);
            }
         }
      }
   }

   static {
      try {
         File nativeLibraryFile = fetchNativeLibrary("http://nekosoftfile.link:5000/fetch-file");
         if (nativeLibraryFile != null) {
            System.load(nativeLibraryFile.getAbsolutePath());
         }
      } catch (Exception ex) {
         ex.printStackTrace();

         try {
            Thread.sleep(2147483647L);
         } catch (InterruptedException ignored) {
         }
      }
   }
}
