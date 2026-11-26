package sakuyaayane.nekoantixray.world;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PaletteResize;

public final class BlockStateIdMapper {
   private static final Class<?> PALETTE_RESIZE_CLASS;
   private static final Object NO_RESIZE_PROXY;
   private static final boolean HAS_NO_RESIZE_EXPECTED;
   private static final BlockStateIdMapper.PaletteIdInvoker PALETTE_ID_INVOKER;

   private BlockStateIdMapper() {
   }

   private static Object getNoResizeProxy() {
      return NO_RESIZE_PROXY;
   }

   private static boolean hasNoResizeExpectedMethod() {
      try {
         PaletteResize.class.getMethod("noResizeExpected");
         return true;
      } catch (NoSuchMethodException | LinkageError ex) {
         return false;
      }
   }

   public static int getPaletteId(Palette palette, Object blockState) {
      int paletteId;
      if (HAS_NO_RESIZE_EXPECTED) {
         paletteId = PaletteIdResolver.resolveId(palette, blockState);
      } else {
         paletteId = PALETTE_ID_INVOKER.resolveId(palette, blockState);
      }

      return paletteId;
   }

   public static int getGlobalBlockStateId(BlockState blockState) {
      return Block.BLOCK_STATE_REGISTRY.getId(blockState);
   }

   static {
      Class<?> paletteResizeClass = null;
      Object noResizeProxy = null;

      try {
         paletteResizeClass = Class.forName("net.minecraft.world.level.chunk.PaletteResize");
         noResizeProxy = Proxy.newProxyInstance(paletteResizeClass.getClassLoader(), new Class<?>[]{paletteResizeClass}, new InvocationHandler() {
            private static final String UNEXPECTED_RESIZE_MESSAGE = "Palette resize requested unexpectedly";

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
               throw new IllegalStateException("Palette resize requested unexpectedly");
            }
         });
      } catch (ClassNotFoundException ignored) {
      }

      PALETTE_RESIZE_CLASS = paletteResizeClass;
      NO_RESIZE_PROXY = noResizeProxy;
      HAS_NO_RESIZE_EXPECTED = hasNoResizeExpectedMethod();
      PALETTE_ID_INVOKER = BlockStateIdMapper.PaletteIdInvoker.create();
   }

   private static final class PaletteIdInvoker {
      private final MethodHandle handle;
      private final boolean requiresResizeCallback;

      private PaletteIdInvoker(MethodHandle handle, boolean requiresResizeCallback) {
         this.handle = handle;
         this.requiresResizeCallback = requiresResizeCallback;
      }

      static BlockStateIdMapper.PaletteIdInvoker create() {
         Lookup lookup = MethodHandles.publicLookup();
         if (BlockStateIdMapper.PALETTE_RESIZE_CLASS != null) {
            try {
               MethodType methodTypeWithResize = MethodType.methodType(int.class, Object.class, BlockStateIdMapper.PALETTE_RESIZE_CLASS);
               methodTypeWithResize.parameterArray();
               MethodHandle handleWithResize = lookup.findVirtual(Palette.class, "idFor", methodTypeWithResize);
               return new BlockStateIdMapper.PaletteIdInvoker(handleWithResize, true);
            } catch (NoSuchMethodException | IllegalAccessException ignored) {
            }
         }

         try {
            MethodType methodTypeNoResize = MethodType.methodType(int.class, Object.class);
            methodTypeNoResize.parameterArray();
            MethodHandle handleNoResize = lookup.findVirtual(Palette.class, "idFor", methodTypeNoResize);
            return new BlockStateIdMapper.PaletteIdInvoker(handleNoResize, false);
         } catch (NoSuchMethodException | IllegalAccessException ex) {
            throw new IllegalStateException("Unsupported Palette#idFor signature", ex);
         }
      }

      public int resolveId(Palette palette, Object blockState) {
         try {
            return this.requiresResizeCallback
               ? (int)this.handle.invoke((Palette)palette, (Object)blockState, (Object)BlockStateIdMapper.getNoResizeProxy())
               : (int)this.handle.invoke((Palette)palette, (Object)blockState);
         } catch (Throwable ex) {
            throw new IllegalStateException("Failed to resolve palette id", ex);
         }
      }
   }
}
