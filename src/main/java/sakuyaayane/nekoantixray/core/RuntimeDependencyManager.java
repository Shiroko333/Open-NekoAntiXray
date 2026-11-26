package sakuyaayane.nekoantixray.core;

import cn.nekosoft.dependency.Dependency;
import cn.nekosoft.dependency.DependencyManager;
import cn.nekosoft.dependency.MavenRepository;
import cn.nekosoft.dependency.RelocationConfig;
import java.io.File;
import org.bukkit.plugin.java.JavaPlugin;

public class RuntimeDependencyManager {
   private final JavaPlugin plugin;
   private final DependencyManager libraryLoader;

   public RuntimeDependencyManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.libraryLoader = new DependencyManager(plugin.getClass(), "sakuyaayane.nekoantixray.libs", new File(plugin.getDataFolder(), "libraries"));
   }

   public void loadDependencies() {
      this.plugin.getLogger().info("开始下载并加载依赖项...");
      String repositoryUrl = "https://repo.huaweicloud.com/repository/maven/";
      this.libraryLoader
         .addDependency(new Dependency("com.microsoft.onnxruntime", "onnxruntime", "1.18.0", new MavenRepository(repositoryUrl), new RelocationConfig(false)));
      this.libraryLoader.addDependency(new Dependency("com.google.code.gson", "gson", "2.10.1", new MavenRepository(repositoryUrl), new RelocationConfig(false)));
      this.libraryLoader.addDependency(new Dependency("net.bytebuddy", "byte-buddy", "1.14.15", new MavenRepository(repositoryUrl), new RelocationConfig(false)));
      this.libraryLoader.addDependency(new Dependency("net.bytebuddy", "byte-buddy-agent", "1.14.15", new MavenRepository(repositoryUrl), new RelocationConfig(false)));
      this.libraryLoader.addDependency(new Dependency("org.ow2.asm", "asm", "9.7", new MavenRepository(repositoryUrl), new RelocationConfig(false)));
      this.libraryLoader.addDependency(new Dependency("org.ow2.asm", "asm-commons", "9.7", new MavenRepository(repositoryUrl), new RelocationConfig(false)));

      try {
         this.libraryLoader.downloadDependencies();
         this.libraryLoader.loadDependencies();
         this.plugin.getLogger().info("所有依赖项已成功加载!");
      } catch (Exception ex) {
         this.plugin.getLogger().severe("加载依赖时发生严重错误，插件将无法正常工作！错误详情: " + ex.getMessage());
         this.plugin.getLogger().severe("完整错误信息请查看下方日志：");

         for (StackTraceElement stackTraceElement : ex.getStackTrace()) {
            this.plugin.getLogger().severe("    " + stackTraceElement.toString());
         }
      }
   }
}
