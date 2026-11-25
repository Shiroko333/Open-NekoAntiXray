package cn.nekosoft.dependency;

public interface DependencyLogger {
   default void log(String message) {
      System.out.println(message);
   }
}
