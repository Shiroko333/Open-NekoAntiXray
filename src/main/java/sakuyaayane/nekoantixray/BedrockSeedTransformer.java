package sakuyaayane.nekoantixray;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public class BedrockSeedTransformer implements ClassFileTransformer, Opcodes {
   private static final String TARGET = "net/minecraft/world/level/levelgen/SurfaceRules$VerticalGradientConditionSource";
   private static final String RESOURCE_LOCATION_DESC = "Lnet/minecraft/resources/ResourceLocation;";
   private static final String BRIDGE_CLASS_NAME = "net.minecraft.resources.NaxBedrockRemapBridge";
   private static final String BRIDGE_RESOURCE_PATH = "net/minecraft/resources/NaxBedrockRemapBridge.class";
   private static final String BRIDGE_INTERNAL_NAME = "net/minecraft/resources/NaxBedrockRemapBridge";
   private static final String REMAP_METHOD_DESC = "(Ljava/lang/Object;J)Ljava/lang/Object;";
   private static final Object BRIDGE_DEFINE_LOCK = new Object();
   private final long xor;

   public BedrockSeedTransformer(long xor) {
      this.xor = xor;
   }

   @Override
   public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
      if (!"net/minecraft/world/level/levelgen/SurfaceRules$VerticalGradientConditionSource".equals(className)) {
         return null;
      }

      if (!ensureBridgeDefined(loader)) {
         return null;
      }

      try {
         ClassReader classReader = new ClassReader(classfileBuffer);
         ClassWriter classWriter = new ClassWriter(classReader, 3);
         ClassVisitor classVisitor = new ClassVisitor(589824, classWriter) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
               MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
               if ("<init>".equals(name) && descriptor.startsWith("(Lnet/minecraft/resources/ResourceLocation;")) {
                  return new AdviceAdapter(589824, methodVisitor, access, name, descriptor) {
                     @Override
                     protected void onMethodEnter() {
                        this.visitVarInsn(25, 1);
                        this.visitLdcInsn(BedrockSeedTransformer.this.xor);
                        this.visitMethodInsn(
                           184, "net/minecraft/resources/NaxBedrockRemapBridge", "remapRandomName", "(Ljava/lang/Object;J)Ljava/lang/Object;", false
                        );
                        this.visitTypeInsn(192, "net/minecraft/resources/ResourceLocation");
                        this.visitVarInsn(58, 1);
                     }
                  };
               } else {
                  return "randomName".equals(name) && "()Lnet/minecraft/resources/ResourceLocation;".equals(descriptor)
                     ? new AdviceAdapter(589824, methodVisitor, access, name, descriptor) {
                        @Override
                        protected void onMethodExit(int opcode) {
                           if (opcode == 176) {
                              this.visitLdcInsn(BedrockSeedTransformer.this.xor);
                              this.visitMethodInsn(
                                 184, "net/minecraft/resources/NaxBedrockRemapBridge", "remapRandomName", "(Ljava/lang/Object;J)Ljava/lang/Object;", false
                              );
                              this.visitTypeInsn(192, "net/minecraft/resources/ResourceLocation");
                           }
                        }
                     }
                     : methodVisitor;
               }
            }
         };
         classReader.accept(classVisitor, 0);
         return classWriter.toByteArray();
      } catch (Throwable ex) {
         return null;
      }
   }

   private static boolean ensureBridgeDefined(ClassLoader classLoader) {
      if (classLoader != null && !isBridgeLoaded(classLoader)) {
         synchronized (BRIDGE_DEFINE_LOCK) {
            if (isBridgeLoaded(classLoader)) {
               return true;
            }

            byte[] bridgeClassBytes = loadBridgeClassBytes();
            if (bridgeClassBytes != null && bridgeClassBytes.length != 0) {
               try {
                  Class<?> resourceLocationClass = Class.forName("net.minecraft.resources.ResourceLocation", false, classLoader);
                  Lookup lookup = MethodHandles.privateLookupIn(resourceLocationClass, MethodHandles.lookup());
                  lookup.defineClass(bridgeClassBytes);
                  return true;
               } catch (LinkageError linkageError) {
                  return isBridgeLoaded(classLoader);
               } catch (Throwable ex) {
                  return false;
               }
            } else {
               return false;
            }
         }
      } else {
         return classLoader != null;
      }
   }

   private static boolean isBridgeLoaded(ClassLoader classLoader) {
      try {
         Class.forName("net.minecraft.resources.NaxBedrockRemapBridge", false, classLoader);
         return true;
      } catch (ClassNotFoundException ex) {
         return false;
      }
   }

   private static byte[] loadBridgeClassBytes() {
      ClassLoader classLoader = BedrockSeedTransformer.class.getClassLoader();

      try (InputStream resourceStream = classLoader == null
            ? ClassLoader.getSystemResourceAsStream("net/minecraft/resources/NaxBedrockRemapBridge.class")
            : classLoader.getResourceAsStream("net/minecraft/resources/NaxBedrockRemapBridge.class")) {
         return resourceStream == null ? null : resourceStream.readAllBytes();
      } catch (IOException ex) {
         return null;
      }
   }
}
