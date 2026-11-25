package cn.nekosoft.dependency;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

public final class DependencyLoader {
   private final DependencyManager instance;
   private final ClassPathAppender classPathAppender;
   private final CopyOnWriteArrayList<Dependency> loadedDependencies = new CopyOnWriteArrayList<>();

   public DependencyLoader(DependencyManager dependencyManager, ClassPathAppender classPathAppender) {
      this.instance = dependencyManager;
      this.classPathAppender = classPathAppender;
   }

   public DependencyManager getInstance() {
      return this.instance;
   }

   public ClassPathAppender getClassPathAppender() {
      return this.classPathAppender;
   }

   public CopyOnWriteArrayList getLoadedDependencies() {
      return this.loadedDependencies;
   }

   @lombok.SneakyThrows
   private void downloadDependency(Dependency dependency) {
      File jarFile = new File(this.getInstance().getLibraryDirectory(), dependency.getJarFileName());
      if (!jarFile.exists()) {
         MavenRepository repository = dependency.getRepository();
         String artifactUrl = repository.buildArtifactUrl(dependency);
         this.getInstance().getLogger().log("正在下载依赖 " + dependency.getJarFileName() + "(" + artifactUrl + ")");
         this.getInstance().getDownloader().downloadToFile(artifactUrl, jarFile.toPath());
      }
   }

   public void downloadAll(Collection dependencies) {
      CountDownLatch latch = new CountDownLatch(dependencies.size());

      for (Object item : dependencies) {
         Dependency dependency = (Dependency)item;
         new Thread(() -> {
            if (!dependency.isEnable()) {
               latch.countDown();
            } else if (this.getLoadedDependencies().contains(dependency)) {
               latch.countDown();
            } else {
               try {
                  this.downloadDependency(dependency);
               } catch (Throwable cause) {
                  throw new RuntimeException("无法下载依赖 " + dependency.getJarFileName(), cause);
               } finally {
                  latch.countDown();
               }
            }
         }).start();
      }

      try {
         latch.await();
      } catch (InterruptedException ex) {
         Thread.currentThread().interrupt();
      }
   }

   private void loadDependency(Dependency dependency) {
      if (!this.getLoadedDependencies().contains(dependency)) {
         this.getInstance().getLogger().log("正在加载依赖 " + dependency.getJarFileName());
         this.classPathAppender.appendFile(this.getInstance().getRelocator().resolveDependencyFile(dependency));
         this.getLoadedDependencies().add(dependency);
         this.getInstance().getLogger().log("依赖 " + dependency.getJarFileName() + " 加载完成!");
      }
   }

   public void loadAll(Collection dependencies) {
      CountDownLatch latch = new CountDownLatch(dependencies.size());

      for (Object item : dependencies) {
         Dependency dependency = (Dependency)item;
         new Thread(() -> {
            if (!dependency.isEnable()) {
               latch.countDown();
            } else if (this.getLoadedDependencies().contains(dependency)) {
               latch.countDown();
            } else {
               try {
                  this.loadDependency(dependency);
               } catch (Throwable cause) {
                  throw new RuntimeException("无法下载依赖 " + dependency.getJarFileName(), cause);
               } finally {
                  latch.countDown();
               }
            }
         }).start();
      }

      try {
         latch.await();
      } catch (InterruptedException ex) {
         Thread.currentThread().interrupt();
      }
   }
}
