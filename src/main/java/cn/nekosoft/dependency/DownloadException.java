package cn.nekosoft.dependency;

public final class DownloadException extends Exception {
   private static int statusCode;

   public DownloadException(String message) {
      super(message);
   }

   public DownloadException(Throwable cause) {
      super(cause);
   }

   public DownloadException(String message, Throwable cause) {
      super(message, cause);
   }

   public static void setStatusCode(int statusCode) {
      DownloadException.statusCode = statusCode;
   }

   public static int getStatusCode() {
      return statusCode;
   }

   public static int getDefaultStatusFlag() {
      int currentStatusCode = getStatusCode();
      return currentStatusCode == 0 ? 115 : 0;
   }

   static {
      if (getDefaultStatusFlag() == 0) {
         setStatusCode(24);
      }
   }
}
