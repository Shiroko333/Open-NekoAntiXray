package sakuyaayane.nekoantixray.metrics;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import javax.net.ssl.HttpsURLConnection;
import sakuyaayane.nekoantixray.util.JsonBuilder;

public class MetricsReporter {
   public static final String METRICS_VERSION = "3.0.2";
   private static final String REPORT_URL = "https://bStats.org/api/v2/data/%s";
   private final ScheduledExecutorService scheduler;
   private final String platform;
   private final String serverUuid;
   private final int serviceId;
   private final Consumer<JsonBuilder> appendPlatformDataConsumer;
   private final Consumer<JsonBuilder> appendServiceDataConsumer;
   private final Consumer<Runnable> submitTaskConsumer;
   private final Supplier<Boolean> checkServiceEnabledSupplier;
   private final BiConsumer<String, Throwable> errorLogger;
   private final Consumer<String> infoLogger;
   private final boolean logErrors;
   private final boolean logSentData;
   private final boolean logResponseStatusText;
   private final Set<MetricsCustomChart> customCharts = new HashSet<>();
   private final boolean enabled;
   private static final long SUBMIT_INTERVAL_MS = 1800000L;

   public void shutdown() {
      this.scheduler.shutdown();
   }

   private void submitData() {
      JsonBuilder payloadBuilder = new JsonBuilder();
      this.appendPlatformDataConsumer.accept(payloadBuilder);
      JsonBuilder serviceBuilder = new JsonBuilder();
      this.appendServiceDataConsumer.accept(serviceBuilder);
      JsonBuilder.JsonObject[] chartObjects = this.customCharts.stream()
         .map(metricsCustomChart -> metricsCustomChart.getRequestJsonObject(this.errorLogger, this.logErrors))
         .filter(jsonObject -> Objects.nonNull(jsonObject))
         .toArray(JsonBuilder.JsonObject[]::new);
      serviceBuilder.appendField("id", this.serviceId);
      serviceBuilder.appendField("customCharts", chartObjects);
      payloadBuilder.appendField("service", serviceBuilder.build());
      payloadBuilder.appendField("serverUUID", this.serverUuid);
      payloadBuilder.appendField("metricsVersion", "3.0.2");
      JsonBuilder.JsonObject payload = payloadBuilder.build();
      this.scheduler.execute(() -> {
         try {
            this.sendData(payload);
         } catch (Exception ex) {
            if (this.logErrors) {
               this.errorLogger.accept("Could not submit bStats metrics data", ex);
            }
         }
      });
   }

   public void addCustomChart(MetricsCustomChart metricsCustomChart) {
      this.customCharts.add(metricsCustomChart);
   }

   private void startSubmitting() {
      Runnable task = () -> {
         if (!this.enabled || !this.checkServiceEnabledSupplier.get()) {
            this.scheduler.shutdown();
         } else if (this.submitTaskConsumer != null) {
            this.submitTaskConsumer.accept(() -> this.submitData());
         } else {
            this.submitData();
         }
      };
      long initialDelayMs = (long)(60000.0 * (3.0 + Math.random() * 3.0));
      long secondDelayMs = (long)(60000.0 * (Math.random() * 30.0));
      this.scheduler.schedule(task, initialDelayMs, TimeUnit.MILLISECONDS);
      this.scheduler.scheduleAtFixedRate(task, initialDelayMs + secondDelayMs, 1800000L, TimeUnit.MILLISECONDS);
   }

   private void checkRelocation() {
      if (System.getProperty("bstats.relocatecheck") == null || !System.getProperty("bstats.relocatecheck").equals("false")) {
         String defaultPackage = new String(new byte[]{111, 114, 103, 46, 98, 115, 116, 97, 116, 115});
         String examplePackage = new String(new byte[]{121, 111, 117, 114, 46, 112, 97, 99, 107, 97, 103, 101});
         if (MetricsReporter.class.getPackage().getName().startsWith(defaultPackage) || MetricsReporter.class.getPackage().getName().startsWith(examplePackage)) {
            throw new IllegalStateException("bStats Metrics class has not been relocated correctly!");
         }
      }
   }

   private static byte[] compress(String input) throws IOException {
      if (input == null) {
         return null;
      }

      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

      try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
         gzipOutputStream.write(input.getBytes(StandardCharsets.UTF_8));
      }

      return byteArrayOutputStream.toByteArray();
   }

   public MetricsReporter(
      String platform,
      String serverUuid,
      int serviceId,
      boolean enabled,
      Consumer appendPlatformDataConsumer,
      Consumer appendServiceDataConsumer,
      Consumer submitTaskConsumer,
      Supplier checkServiceEnabledSupplier,
      BiConsumer errorLogger,
      Consumer infoLogger,
      boolean logErrors,
      boolean logSentData,
      boolean logResponseStatusText
   ) {
      ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1, task -> new Thread(task, "bStats-Metrics"));
      scheduledThreadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
      this.scheduler = scheduledThreadPoolExecutor;
      this.platform = platform;
      this.serverUuid = serverUuid;
      this.serviceId = serviceId;
      this.enabled = enabled;
      this.appendPlatformDataConsumer = appendPlatformDataConsumer;
      this.appendServiceDataConsumer = appendServiceDataConsumer;
      this.submitTaskConsumer = submitTaskConsumer;
      this.checkServiceEnabledSupplier = checkServiceEnabledSupplier;
      this.errorLogger = errorLogger;
      this.infoLogger = infoLogger;
      this.logErrors = logErrors;
      this.logSentData = logSentData;
      this.logResponseStatusText = logResponseStatusText;
      this.checkRelocation();
      if (enabled) {
         this.startSubmitting();
      }
   }

   private void sendData(JsonBuilder.JsonObject data) throws Exception {
      if (this.logSentData) {
         this.infoLogger.accept("Sent bStats metrics data: " + data.toString());
      }

      String endpointUrl = String.format("https://bStats.org/api/v2/data/%s", this.platform);
      HttpsURLConnection connection = (HttpsURLConnection)new URL(endpointUrl).openConnection();
      byte[] compressedData = compress(data.toString());
      connection.setRequestMethod("POST");
      connection.addRequestProperty("Accept", "application/json");
      connection.addRequestProperty("Connection", "close");
      connection.addRequestProperty("Content-Encoding", "gzip");
      connection.addRequestProperty("Content-Length", String.valueOf(compressedData.length));
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("User-Agent", "Metrics-Service/1");
      connection.setDoOutput(true);

      try (DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream())) {
         dataOutputStream.write(compressedData);
      }

      StringBuilder responseBuilder = new StringBuilder();
      BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

      String line;
      try {
         while ((line = reader.readLine()) != null) {
            responseBuilder.append(line);
         }
      } catch (Throwable readEx) {
         try {
            reader.close();
         } catch (Throwable closeEx) {
            readEx.addSuppressed(closeEx);
         }

         throw readEx;
      }

      reader.close();
      if (this.logResponseStatusText) {
         this.infoLogger.accept("Sent data to bStats and received response: " + responseBuilder);
      }
   }
}
