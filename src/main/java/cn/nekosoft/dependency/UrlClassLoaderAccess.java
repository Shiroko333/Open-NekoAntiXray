package cn.nekosoft.dependency;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import sun.misc.Unsafe;

public abstract class UrlClassLoaderAccess {
   private final URLClassLoader classLoader;
   private static final String UNSUPPORTED_ERROR_MESSAGE = "不支持该操作";

   private UrlClassLoaderAccess(URLClassLoader classLoader) {
      this.classLoader = classLoader;
   }

   public abstract void addURL(URL url);

   public static UrlClassLoaderAccess create(DependencyManager dependencyManager, URLClassLoader classLoader) {
      UrlClassLoaderAccess.ReflectionAccess reflectionAccess = new UrlClassLoaderAccess.ReflectionAccess(dependencyManager, classLoader);
      if (reflectionAccess.isSupported()) {
         return reflectionAccess;
      } else if (UrlClassLoaderAccess.UnsafeAccess.isSupported()) {
         return new UrlClassLoaderAccess.UnsafeAccess(classLoader);
      } else {
         throw new RuntimeException("不支持该操作");
      }
   }

   private static class ReflectionAccess extends UrlClassLoaderAccess {
      private Method method;
      private static final String ADD_URL_METHOD_NAME = "addURL";

      private ReflectionAccess(DependencyManager dependencyManager, URLClassLoader classLoader) {
         super(classLoader);

         try {
            this.method = dependencyManager.getReflectionHelper().getDeclaredMethod(URLClassLoader.class, "addURL", true, new Class[]{URL.class});
         } catch (Exception ex) {
            this.method = null;
         }
      }

      @Override
      public void addURL(URL url) {
         this.appendUrl(url);
      }

      public boolean isSupported() {
         return this.method != null;
      }

      @lombok.SneakyThrows
   public void appendUrl(URL url) {
         this.method.invoke(super.classLoader, url);
      }
   }

   private static class UnsafeAccess extends UrlClassLoaderAccess {
      private static final Unsafe UNSAFE;
      private final Collection<URL> unopenedUrls;
      private final Collection<URL> pathUrls;

      UnsafeAccess(URLClassLoader classLoader) {
         super(classLoader);

         Collection unopened;
         Collection path;
         try {
            Object urlClassPath = fetchField(URLClassLoader.class, classLoader, "ucp");
            unopened = (Collection)fetchField(urlClassPath.getClass(), urlClassPath, "unopenedUrls");
            path = (Collection)fetchField(urlClassPath.getClass(), urlClassPath, "path");
         } catch (Throwable ex) {
            unopened = null;
            path = null;
         }

         this.unopenedUrls = unopened;
         this.pathUrls = path;
      }

      @Override
      public void addURL(URL url) {
         this.appendUrl(url);
      }

      private static boolean isSupported() {
         return UNSAFE != null;
      }

      private static Object fetchField(Class clazz, Object target, String fieldName) throws NoSuchFieldException {
         Field field = clazz.getDeclaredField(fieldName);
         long offset = UNSAFE.objectFieldOffset(field);
         return UNSAFE.getObject(target, offset);
      }

      public void appendUrl(URL url) {
         if (this.unopenedUrls != null && this.pathUrls != null) {
            synchronized (this.unopenedUrls) {
               this.unopenedUrls.add(url);
               this.pathUrls.add(url);
            }
         } else {
            throw new RuntimeException("载入的地址不存在");
         }
      }

      static {
         Unsafe unsafe;
         try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe)field.get(null);
         } catch (Throwable ex) {
            unsafe = null;
         }

         UNSAFE = unsafe;
      }
   }
}
