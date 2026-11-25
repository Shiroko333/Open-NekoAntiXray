package cn.nekosoft.dependency;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DependencyRelocator {
   private final DependencyManager instance;
   private Constructor<?> relocatorConstructor;
   private Method runMethod;

   public DependencyRelocator(DependencyManager dependencyManager) {
      this.instance = dependencyManager;
   }

   public DependencyManager getInstance() {
      return this.instance;
   }

   @lombok.SneakyThrows
   public void initRelocator() {
      Class<?> relocatorClass = Class.forName("me.lucko.jarrelocator.JarRelocator");
      this.relocatorConstructor = relocatorClass.getDeclaredConstructor(File.class, File.class, Map.class);
      this.relocatorConstructor.setAccessible(true);
      this.runMethod = relocatorClass.getDeclaredMethod("run");
      this.runMethod.setAccessible(true);
   }

   private HashMap buildRelocationRules() {
      HashMap<String, String> relocationRules = new HashMap<>();
      List<Dependency> dependencies = this.getInstance().getDependencies();

      for (Dependency dependency : dependencies) {
         if (dependency.isEnable()) {
            RelocationConfig relocationConfig = dependency.getRelocationConfig();
            if (relocationConfig.isEnable()) {
               String relocationPackage = this.getInstance().getRelocationPackage();
               if (relocationConfig.isRelocateGroup()) {
                  relocationRules.put(dependency.getGroup(), relocationPackage + "." + dependency.getGroup());
               }

               for (String packagePrefix : relocationConfig.getRelocator()) {
                  relocationRules.put(packagePrefix, relocationPackage + "." + packagePrefix);
               }
            }
         }
      }

      return relocationRules;
   }

   private File getRelocatedFile(File jarFile) {
      return new File(jarFile.getParentFile(), "relocated-" + jarFile.getName());
   }

   public File resolveDependencyFile(Dependency dependency) {
      File jarFile = new File(this.getInstance().getLibraryDirectory(), dependency.getJarFileName());
      RelocationConfig relocationConfig = dependency.getRelocationConfig();
      return !relocationConfig.isEnable() ? jarFile : this.relocateJar(jarFile);
   }

   @lombok.SneakyThrows
   public File relocateJar(File jarFile) {
      if (!jarFile.exists()) {
         throw new NullPointerException("找不到依赖: " + jarFile.getName());
      }

      File relocatedFile = this.getRelocatedFile(jarFile);
      if (relocatedFile.exists()) {
         return relocatedFile;
      }

      if (this.relocatorConstructor == null || this.runMethod == null) {
         this.initRelocator();
      }

      this.runMethod.invoke(this.relocatorConstructor.newInstance(jarFile, relocatedFile, this.buildRelocationRules()));
      return relocatedFile;
   }
}