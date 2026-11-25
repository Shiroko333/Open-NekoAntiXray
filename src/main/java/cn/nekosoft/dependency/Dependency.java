package cn.nekosoft.dependency;

public final class Dependency {
   private final String group;
   private final String artifactId;
   private final String version;
   private final MavenRepository repository;
   private final boolean enable;
   private final RelocationConfig relocationConfig;

   public Dependency(String group, String artifactId, String version, MavenRepository repository, boolean enable) {
      this(group, artifactId, version, repository, enable, new RelocationConfig(false));
   }

   public Dependency(String group, String artifactId, String version, MavenRepository repository) {
      this(group, artifactId, version, repository, true);
   }

   public String getGroup() {
      return this.group;
   }

   public String getArtifactId() {
      return this.artifactId;
   }

   public String getVersion() {
      return this.version;
   }

   public MavenRepository getRepository() {
      return this.repository;
   }

   public boolean isEnable() {
      return this.enable;
   }

   public RelocationConfig getRelocationConfig() {
      return this.relocationConfig;
   }

   public String getJarFileName() {
      return this.getArtifactId() + "-" + this.getVersion() + ".jar";
   }

   public Dependency(String group, String artifactId, String version, MavenRepository repository, boolean enable, RelocationConfig relocationConfig) {
      this.group = group;
      this.artifactId = artifactId;
      this.version = version;
      this.repository = repository;
      this.enable = enable;
      this.relocationConfig = relocationConfig;
   }

   public String getMavenPath() {
      return String.format("%s/%s/%s/%s", this.getGroup().replace(".", "/"), this.getArtifactId(), this.getVersion(), this.getJarFileName());
   }

   public Dependency(String group, String artifactId, String version, MavenRepository repository, RelocationConfig relocationConfig) {
      this(group, artifactId, version, repository, true, relocationConfig);
   }
}
