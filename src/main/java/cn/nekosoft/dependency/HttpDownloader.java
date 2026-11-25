package cn.nekosoft.dependency;

import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public final class HttpDownloader {
   private Proxy proxy = Proxy.NO_PROXY;
   private int timeoutMillis = (int)TimeUnit.SECONDS.toMillis(20L);
   private static final String EMPTY_FILE_ERROR_MESSAGE = "无可下载文件";
   private static final long DEFAULT_TIMEOUT_SECONDS = 20L;

   public Proxy getProxy() {
      return this.proxy;
   }

   public int getTimeoutMillis() {
      return this.timeoutMillis;
   }

   @lombok.SneakyThrows
   public URLConnection openConnection(String url) {
      URLConnection connection = new URL(url).openConnection(this.getProxy());
      connection.setConnectTimeout(this.timeoutMillis);
      connection.setReadTimeout(this.timeoutMillis);
      return connection;
   }

   public byte[] readAllBytes(URLConnection connection) throws DownloadException {
      try (InputStream inputStream = connection.getInputStream()) {
         byte[] bytes = inputStream.readAllBytes();
         if (bytes.length == 0) {
            throw new DownloadException("无可下载文件");
         } else {
            return bytes;
         }
      } catch (Exception ex) {
         throw new DownloadException(ex);
      }
   }

   public void downloadToFile(String url, Path targetPath) throws DownloadException {
      URLConnection connection = this.openConnection(url);
      this.saveConnectionToFile(connection, targetPath);
   }

   public void setProxy(Proxy proxy) {
      this.proxy = proxy;
   }

   public void setTimeoutMillis(int timeoutMillis) {
      this.timeoutMillis = timeoutMillis;
   }

   public byte[] downloadBytes(String url) throws DownloadException {
      return this.readAllBytes(this.openConnection(url));
   }

   public void saveConnectionToFile(URLConnection connection, Path targetPath) throws DownloadException {
      try {
         Files.write(targetPath, this.readAllBytes(connection));
      } catch (IOException ex) {
         if (targetPath.toFile().exists()) {
            targetPath.toFile().delete();
         }

         throw new DownloadException(ex);
      }
   }
}
