package cn.nekosoft.dependency;

public final class MavenRepository {
   private final String url;
   private final String id;

   public MavenRepository(String url, String id) {
      this.url = url;
      this.id = id;
   }

   public MavenRepository(String url) {
      this(url, url);
   }

   public String getUrl() {
      return this.url;
   }

   public String getId() {
      return this.id;
   }

   public String buildArtifactUrl(Dependency dependency) {
      return this.getUrl() + dependency.getMavenPath();
   }
}
