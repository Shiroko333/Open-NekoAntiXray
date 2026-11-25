package sakuyaayane.nekoantixray;

import java.util.Arrays;
import java.util.stream.Collectors;

public class JsonBuilder {
   private StringBuilder builder = new StringBuilder();
   private boolean hasAtLeastOneField = false;

   public JsonBuilder() {
      this.builder.append("{");
   }

   public JsonBuilder appendStringArray(String key, String[] elements) {
      return this.appendField(key, elements);
   }

   public JsonBuilder.JsonObject build() {
      if (this.builder == null) {
         throw new IllegalStateException("JSON has already been built");
      }

      JsonBuilder.JsonObject jsonObject = new JsonBuilder.JsonObject(this.builder.append("}").toString());
      this.builder = null;
      return jsonObject;
   }

   private static String escape(String input) {
      StringBuilder escaped = new StringBuilder();

      for (int i = 0; i < input.length(); i++) {
         char ch = input.charAt(i);
         if (ch == '"') {
            escaped.append("\\\"");
         } else if (ch == '\\') {
            escaped.append("\\\\");
         } else if (ch <= 15) {
            escaped.append("\\u000").append(Integer.toHexString(ch));
         } else if (ch <= 31) {
            escaped.append("\\u00").append(Integer.toHexString(ch));
         } else {
            escaped.append(ch);
         }
      }

      return escaped.toString();
   }

   private void appendFieldUnescaped(String key, String rawValue) {
      if (this.builder == null) {
         throw new IllegalStateException("JSON has already been built");
      }

      if (key == null) {
         throw new IllegalArgumentException("JSON key must not be null");
      }

      if (this.hasAtLeastOneField) {
         this.builder.append(",");
      }

      this.builder.append("\"").append(escape(key)).append("\":").append(rawValue);
      this.hasAtLeastOneField = true;
   }

   public JsonBuilder appendNull(String key) {
      this.appendFieldUnescaped(key, "null");
      return this;
   }

   public JsonBuilder appendField(String key, String stringValue) {
      if (stringValue == null) {
         throw new IllegalArgumentException("JSON value must not be null");
      }

      this.appendFieldUnescaped(key, "\"" + escape(stringValue) + "\"");
      return this;
   }

   public JsonBuilder appendField(String key, int intValue) {
      this.appendFieldUnescaped(key, String.valueOf(intValue));
      return this;
   }

   public JsonBuilder appendField(String key, JsonBuilder.JsonObject jsonObject) {
      if (jsonObject == null) {
         throw new IllegalArgumentException("JSON object must not be null");
      }

      this.appendFieldUnescaped(key, jsonObject.toString());
      return this;
   }

   public JsonBuilder appendField(String key, String[] elements) {
      if (elements == null) {
         throw new IllegalArgumentException("JSON values must not be null");
      }

      String joinedElements = Arrays.stream(elements).map(element -> "\"" + escape(element) + "\"").collect(Collectors.joining(","));
      this.appendFieldUnescaped(key, "[" + joinedElements + "]");
      return this;
   }

   public JsonBuilder appendField(String key, int[] numbers) {
      if (numbers == null) {
         throw new IllegalArgumentException("JSON values must not be null");
      }

      String joinedNumbers = Arrays.stream(numbers).mapToObj(number -> String.valueOf(number)).collect(Collectors.joining(","));
      this.appendFieldUnescaped(key, "[" + joinedNumbers + "]");
      return this;
   }

   public JsonBuilder appendField(String key, JsonBuilder.JsonObject[] jsonObjects) {
      if (jsonObjects == null) {
         throw new IllegalArgumentException("JSON values must not be null");
      }

      String joinedObjects = Arrays.stream(jsonObjects).map(jsonObject -> jsonObject.toString()).collect(Collectors.joining(","));
      this.appendFieldUnescaped(key, "[" + joinedObjects + "]");
      return this;
   }

   public static class JsonObject {
      private final String value;

      private JsonObject(String json) {
         this.value = json;
      }

      @Override
      public String toString() {
         return this.value;
      }
   }
}
