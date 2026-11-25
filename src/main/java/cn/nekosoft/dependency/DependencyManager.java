package cn.nekosoft.dependency;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DependencyManager {
   public static MavenRepository defaultRepository = new MavenRepository("https://repo.huaweicloud.com/repository/maven/", "huawei-maven");
   private final HttpDownloader downloader;
   private final ReflectionHelper reflectionHelper;
   private final DependencyLoader dependencyLoader;
   private final DependencyRelocator relocator;
   private final DependencyLogger logger;
   private final String relocationPackage;
   private final File libraryDirectory;
   private final List<Dependency> bootstrapDependencies = List.of(
      new Dependency("org.ow2.asm", "asm", "9.7.1", defaultRepository),
      new Dependency("org.ow2.asm", "asm-commons", "9.7.1", defaultRepository),
      new Dependency("me.lucko", "jar-relocator", "1.7", defaultRepository)
   );
   private final CopyOnWriteArrayList<Dependency> dependencies = new CopyOnWriteArrayList<>();

   public DependencyManager(Class ownerClass, DependencyLogger dependencyLogger, String relocationPackage, File libraryDirectory) {
      this.downloader = new HttpDownloader();
      this.reflectionHelper = new ReflectionHelper();
      this.dependencyLoader = new DependencyLoader(this, new UrlClassPathAppender(this, ownerClass.getClassLoader()));
      this.logger = dependencyLogger;
      this.relocationPackage = relocationPackage;
      this.libraryDirectory = libraryDirectory;
      if (!this.getLibraryDirectory().exists()) {
         this.getLibraryDirectory().mkdirs();
      }

      this.getDependencyLoader().downloadAll(this.getBootstrapDependencies());
      this.relocator = new DependencyRelocator(this);
      this.getDependencyLoader().loadAll(this.getBootstrapDependencies());
   }

   public DependencyManager(Class ownerClass, String relocationPackage, File libraryDirectory) {
      this(ownerClass, new DependencyLogger() {}, relocationPackage, libraryDirectory);
   }

   public HttpDownloader getDownloader() {
      return this.downloader;
   }

   public ReflectionHelper getReflectionHelper() {
      return this.reflectionHelper;
   }

   public DependencyLoader getDependencyLoader() {
      return this.dependencyLoader;
   }

   public DependencyRelocator getRelocator() {
      return this.relocator;
   }

   public DependencyLogger getLogger() {
      return this.logger;
   }

   public String getRelocationPackage() {
      return this.relocationPackage;
   }

   public File getLibraryDirectory() {
      return this.libraryDirectory;
   }

   public List getBootstrapDependencies() {
      return this.bootstrapDependencies;
   }

   public CopyOnWriteArrayList getDependencies() {
      return this.dependencies;
   }

   public void addDependency(Dependency dependency) {
      this.getDependencies().add(dependency);
   }

   public void downloadDependencies() {
      this.getDependencyLoader().downloadAll(this.getDependencies());
   }

   public void loadDependencies() {
      this.getDependencyLoader().loadAll(this.getDependencies());
   }
}
