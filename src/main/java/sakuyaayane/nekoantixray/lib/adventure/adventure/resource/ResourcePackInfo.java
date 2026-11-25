package sakuyaayane.nekoantixray.lib.adventure.adventure.resource;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.builder.AbstractBuilder;
import sakuyaayane.nekoantixray.lib.adventure.examination.Examinable;

public interface ResourcePackInfo extends Examinable, ResourcePackInfoLike {
   @NotNull
   static ResourcePackInfo resourcePackInfo(@NotNull UUID var0, @NotNull URI var1, @NotNull String var2) {
      return new ResourcePackInfoImpl(var0, var1, var2);
   }

   @NotNull
   static ResourcePackInfo.Builder resourcePackInfo() {
      return new ResourcePackInfoImpl.BuilderImpl();
   }

   @NotNull
   UUID id();

   @NotNull
   URI uri();

   @NotNull
   String hash();

   @NotNull
   @Override
   default ResourcePackInfo asResourcePackInfo() {
      return this;
   }

   interface Builder extends AbstractBuilder<ResourcePackInfo>, ResourcePackInfoLike {
      @Contract("_ -> this")
      @NotNull
      ResourcePackInfo.Builder id(@NotNull UUID var1);

      @Contract("_ -> this")
      @NotNull
      ResourcePackInfo.Builder uri(@NotNull URI var1);

      @Contract("_ -> this")
      @NotNull
      ResourcePackInfo.Builder hash(@NotNull String var1);

      @NotNull
      ResourcePackInfo build();

      @NotNull
      default CompletableFuture<ResourcePackInfo> computeHashAndBuild() {
         return this.computeHashAndBuild(ForkJoinPool.commonPool());
      }

      @NotNull
      CompletableFuture<ResourcePackInfo> computeHashAndBuild(@NotNull Executor var1);

      @NotNull
      @Override
      default ResourcePackInfo asResourcePackInfo() {
         return this.build();
      }
   }
}
