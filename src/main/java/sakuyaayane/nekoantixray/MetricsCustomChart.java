package sakuyaayane.nekoantixray;

import java.util.function.BiConsumer;

public abstract class MetricsCustomChart {
   private final String chartId;

   protected abstract JsonBuilder.JsonObject getChartData() throws Exception;

   public JsonBuilder.JsonObject getRequestJsonObject(BiConsumer errorLogger, boolean logErrors) {
      JsonBuilder jsonBuilder = new JsonBuilder();
      jsonBuilder.appendField("chartId", this.chartId);

      try {
         JsonBuilder.JsonObject chartData = this.getChartData();
         if (chartData == null) {
            return null;
         }

         jsonBuilder.appendField("data", chartData);
      } catch (Throwable ex) {
         if (logErrors) {
            errorLogger.accept("Failed to get data for custom chart with id " + this.chartId, ex);
         }

         return null;
      }

      return jsonBuilder.build();
   }

   protected MetricsCustomChart(String chartId) {
      if (chartId == null) {
         throw new IllegalArgumentException("chartId must not be null");
      }

      this.chartId = chartId;
   }
}
