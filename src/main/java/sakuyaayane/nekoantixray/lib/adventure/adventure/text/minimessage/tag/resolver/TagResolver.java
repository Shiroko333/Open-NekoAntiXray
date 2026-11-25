package sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.resolver;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.Context;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.ParsingException;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.internal.TagInternals;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.Tag;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.TagPattern;
import sakuyaayane.nekoantixray.lib.adventure.adventure.text.minimessage.tag.standard.StandardTags;

public interface TagResolver {
   @NotNull
   static TagResolver.Builder builder() {
      return new TagResolverBuilderImpl();
   }

   @NotNull
   static TagResolver standard() {
      return StandardTags.defaults();
   }

   @NotNull
   static TagResolver empty() {
      return EmptyTagResolver.INSTANCE;
   }

   static TagResolver.@NotNull Single resolver(@TagPattern @NotNull String var0, @NotNull Tag var1) {
      TagInternals.assertValidTagName(var0);
      return new SingleResolver(var0, Objects.requireNonNull(var1, "tag"));
   }

   @NotNull
   static TagResolver resolver(@TagPattern @NotNull String var0, @NotNull BiFunction<ArgumentQueue, Context, Tag> var1) {
      return resolver(Collections.singleton(var0), var1);
   }

   @NotNull
   static TagResolver resolver(@NotNull final Set<String> var0, @NotNull final BiFunction<ArgumentQueue, Context, Tag> var1) {
      for (String var4 : (java.lang.Iterable<java.lang.String>)(java.lang.Iterable<?>)(new HashSet(var0))) {
         TagInternals.assertValidTagName(var4);
      }

      Objects.requireNonNull(var1, "handler");
      return new TagResolver() {
         @Nullable
         @Override
         public Tag resolve(@NotNull String var1x, @NotNull ArgumentQueue var2, @NotNull Context var3) throws ParsingException {
            return !var0.contains(var1x) ? null : (Tag)var1.apply(var2, var3);
         }

         @Override
         public boolean has(@NotNull String var1x) {
            return var0.contains(var1x);
         }
      };
   }

   @NotNull
   static TagResolver resolver(@NotNull TagResolver... var0) {
      return ((TagResolver[])Objects.requireNonNull(var0, "resolvers")).length == 1
         ? Objects.requireNonNull(var0[0], "resolvers must not contain null elements")
         : builder().resolvers(var0).build();
   }

   @NotNull
   static TagResolver resolver(@NotNull Iterable<? extends TagResolver> var0) {
      if (var0 instanceof Collection) {
         int var1 = ((Collection)var0).size();
         if (var1 == 0) {
            return empty();
         }

         if (var1 == 1) {
            return Objects.requireNonNull((TagResolver)var0.iterator().next(), "resolvers must not contain null elements");
         }
      }

      return builder().resolvers(var0).build();
   }

   @NotNull
   static TagResolver caching(TagResolver.@NotNull WithoutArguments var0) {
      return var0 instanceof CachingTagResolver ? var0 : new CachingTagResolver(Objects.requireNonNull(var0, "resolver"));
   }

   @NotNull
   static Collector<TagResolver, ?, TagResolver> toTagResolver() {
      return TagResolverBuilderImpl.COLLECTOR;
   }

   @Nullable
   Tag resolve(@TagPattern @NotNull String var1, @NotNull ArgumentQueue var2, @NotNull Context var3) throws ParsingException;

   boolean has(@NotNull String var1);

   interface Builder {
      @NotNull
      TagResolver.Builder tag(@TagPattern @NotNull String var1, @NotNull Tag var2);

      @NotNull
      default TagResolver.Builder tag(@TagPattern @NotNull String var1, @NotNull BiFunction<ArgumentQueue, Context, Tag> var2) {
         return this.tag(Collections.singleton(var1), var2);
      }

      @NotNull
      default TagResolver.Builder tag(@NotNull Set<String> var1, @NotNull BiFunction<ArgumentQueue, Context, Tag> var2) {
         return this.resolver(TagResolver.resolver(var1, var2));
      }

      @NotNull
      TagResolver.Builder resolver(@NotNull TagResolver var1);

      @NotNull
      TagResolver.Builder resolvers(@NotNull TagResolver... var1);

      @NotNull
      TagResolver.Builder resolvers(@NotNull Iterable<? extends TagResolver> var1);

      @NotNull
      default TagResolver.Builder caching(TagResolver.@NotNull WithoutArguments var1) {
         return this.resolver(TagResolver.caching(var1));
      }

      @NotNull
      TagResolver build();
   }

   @NonExtendable
   interface Single extends TagResolver.WithoutArguments {
      @NotNull
      String key();

      @NotNull
      Tag tag();

      @Nullable
      @Override
      default Tag resolve(@TagPattern @NotNull String var1) {
         return this.has(var1) ? this.tag() : null;
      }

      @Override
      default boolean has(@NotNull String var1) {
         return var1.equalsIgnoreCase(this.key());
      }
   }

   @FunctionalInterface
   interface WithoutArguments extends TagResolver {
      @Nullable
      Tag resolve(@TagPattern @NotNull String var1);

      @Override
      default boolean has(@NotNull String var1) {
         return this.resolve(var1) != null;
      }

      @Nullable
      @Override
      default Tag resolve(@TagPattern @NotNull String var1, @NotNull ArgumentQueue var2, @NotNull Context var3) throws ParsingException {
         Tag var4 = this.resolve(var1);
         if (var4 != null && var2.hasNext()) {
            throw var3.newException("Tag '<" + var1 + ">' does not accept any arguments");
         } else {
            return var4;
         }
      }
   }
}
