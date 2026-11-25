package cn.nekosoft.dependency;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.nio.file.Path;

public final class UrlClassPathAppender implements ClassPathAppender {
   private final UrlClassLoaderAccess classLoaderAccess;

   public UrlClassPathAppender(DependencyManager dependencyManager, ClassLoader classLoader) throws IllegalStateException {
      try {
         Class<?> paperPluginClassLoaderClass = Class.forName("io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader");
         if (paperPluginClassLoaderClass.isInstance(classLoader)) {
            ReflectionHelper reflectionHelper = dependencyManager.getReflectionHelper();
            Field libraryLoaderField = dependencyManager.getReflectionHelper().getDeclaredField(paperPluginClassLoaderClass, "libraryLoader", true);
            classLoader = (ClassLoader)reflectionHelper.getFieldValue(libraryLoaderField, classLoader);
         }
      } catch (ClassNotFoundException ignored) {
      }

      if (classLoader instanceof URLClassLoader) {
         this.classLoaderAccess = UrlClassLoaderAccess.create(dependencyManager, (URLClassLoader)classLoader);
      } else {
         throw new RuntimeException("classLoader 类型并不是 URLClassLoader");
      }
   }

   @lombok.SneakyThrows
   @Override
   public void appendPath(Path path) {
      this.classLoaderAccess.addURL(path.toUri().toURL());
   }

   @Override
   public void appendFile(File file) {
      this.appendPath(file.toPath());
   }
}
