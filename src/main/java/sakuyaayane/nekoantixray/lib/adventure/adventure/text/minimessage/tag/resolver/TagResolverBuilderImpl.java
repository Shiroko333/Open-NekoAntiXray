package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import org.jetbrains.annotations.NotNull;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.TagInternals;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;

final class TagResolverBuilderImpl implements TagResolver.Builder {
   static final Collector<TagResolver, TagResolver.Builder, TagResolver> COLLECTOR = Collector.of(
      () -> TagResolver.builder(),
      (var0, var1) -> var0.resolver(var1),
      (var0, var1) -> restoredLambda$static$0((TagResolver.Builder)var0, (TagResolver.Builder)var1),
      var0 -> var0.build()
   );
   private final Map<String, Tag> replacements = new HashMap<>();
   private final List<TagResolver> resolvers = new ArrayList<>();

   @Override
   public TagResolver.@NotNull Builder tag(@NotNull String var1, @NotNull Tag var2) {
      TagInternals.assertValidTagName(Objects.requireNonNull(var1, "name"));
      this.replacements.put(var1, Objects.requireNonNull(var2, "tag"));
      return this;
   }

   @Override
   public TagResolver.@NotNull Builder resolver(@NotNull TagResolver var1) {
      if (var1 instanceof SequentialTagResolver) {
         this.resolvers(((SequentialTagResolver)var1).resolvers, false);
      } else if (!this.consumePotentialMappable(var1)) {
         this.popMap();
         this.resolvers.add(Objects.requireNonNull(var1, "resolver"));
      }

      return this;
   }

   @Override
   public TagResolver.@NotNull Builder resolvers(@NotNull TagResolver... var1) {
      return this.resolvers(var1, true);
   }

   private TagResolver.@NotNull Builder resolvers(@NotNull TagResolver[] var1, boolean var2) {
      boolean var3 = false;
      Objects.requireNonNull(var1, "resolvers");
      if (var2) {
         for (TagResolver var7 : var1) {
            var3 = this.single(var7, var3);
         }
      } else {
         for (int var8 = var1.length - 1; var8 >= 0; var8--) {
            var3 = this.single(var1[var8], var3);
         }
      }

      return this;
   }

   @Override
   public TagResolver.@NotNull Builder resolvers(@NotNull Iterable<? extends TagResolver> var1) {
      boolean var2 = false;

      for (TagResolver var4 : Objects.requireNonNull(var1, "resolvers")) {
         var2 = this.single(var4, var2);
      }

      return this;
   }

   private boolean single(TagResolver var1, boolean var2) {
      if (var1 instanceof SequentialTagResolver) {
         this.resolvers(((SequentialTagResolver)var1).resolvers, false);
      } else if (!this.consumePotentialMappable(var1)) {
         if (!var2) {
            this.popMap();
         }

         this.resolvers.add(Objects.requireNonNull(var1, "resolvers[?]"));
         return true;
      }

      return false;
   }

   private void popMap() {
      if (!this.replacements.isEmpty()) {
         this.resolvers.add(new MapTagResolver(new HashMap<>(this.replacements)));
         this.replacements.clear();
      }
   }

   private boolean consumePotentialMappable(TagResolver var1) {
      return var1 instanceof MappableResolver ? ((MappableResolver)var1).contributeToMap(this.replacements) : false;
   }

   @NotNull
   @Override
   public TagResolver build() {
      this.popMap();
      if (this.resolvers.size() == 0) {
         return EmptyTagResolver.INSTANCE;
      }

      if (this.resolvers.size() == 1) {
         return this.resolvers.get(0);
      }

      TagResolver[] var1 = this.resolvers.toArray(new TagResolver[0]);
      Collections.reverse(Arrays.asList(var1));
      return new SequentialTagResolver(var1);
   }

   // $VF: synthetic method
   private static TagResolver.Builder restoredLambda$static$0(TagResolver.Builder var0, TagResolver.Builder var1) {
      return TagResolver.builder().resolvers(var0.build(), var1.build());
   }
}
