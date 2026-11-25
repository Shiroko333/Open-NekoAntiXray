package sakuyaayane.nekoantixray;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicInteger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public class WorldgenRandomTransformer implements ClassFileTransformer, Opcodes {
   private static final String TARGET = "net/minecraft/world/level/levelgen/WorldgenRandom";
   private static final AtomicInteger VISIT_COUNT = new AtomicInteger();
   private static final AtomicInteger TRANSFORMED_COUNT = new AtomicInteger();
   private static final AtomicInteger SET_LARGE_FEATURE_WITH_SALT_HOOKS = new AtomicInteger();
   private static final AtomicInteger MISSING_SET_LARGE_FEATURE_WITH_SALT_COUNT = new AtomicInteger();
   private static final AtomicInteger FAILURE_COUNT = new AtomicInteger();
   private final long worldSeedXor;
   private final boolean structurePlacementXorEnabled;

   public WorldgenRandomTransformer(long worldSeedXor) {
      this(worldSeedXor, true);
   }

   public WorldgenRandomTransformer(long worldSeedXor, boolean structurePlacementXorEnabled) {
      this.worldSeedXor = worldSeedXor;
      this.structurePlacementXorEnabled = structurePlacementXorEnabled;
   }

   @Override
   public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
      boolean isTargetClass = "net/minecraft/world/level/levelgen/WorldgenRandom".equals(className);
      if (!isTargetClass) {
         return null;
      }

      VISIT_COUNT.incrementAndGet();

      try {
         ClassReader classReader = new ClassReader(classfileBuffer);
         ClassWriter classWriter = new ClassWriter(classReader, 3);
         final int[] hookCount = new int[]{0};
         ClassVisitor classVisitor = new ClassVisitor(589824, classWriter) {
            @Override
            public MethodVisitor visitMethod(int methodAccess, String methodName, String descriptor, String signature, String[] exceptions) {
               MethodVisitor methodVisitor = super.visitMethod(methodAccess, methodName, descriptor, signature, exceptions);
               if (!WorldgenRandomTransformer.this.structurePlacementXorEnabled) {
                  return methodVisitor;
               } else if (WorldgenRandomTransformer.isSetLargeFeatureWithSaltMethod(methodName, descriptor)) {
                  hookCount[0]++;
                  return new AdviceAdapter(589824, methodVisitor, methodAccess, methodName, descriptor) {
                     @Override
                     protected void onMethodEnter() {
                        this.visitVarInsn(22, 1);
                        this.visitLdcInsn(WorldgenRandomTransformer.this.worldSeedXor);
                        this.visitInsn(131);
                        this.visitVarInsn(55, 1);
                     }
                  };
               } else {
                  return methodVisitor;
               }
            }
         };
         classReader.accept(classVisitor, 0);
         int appliedHooks = hookCount[0];
         if (appliedHooks == 0) {
            if (this.structurePlacementXorEnabled) {
               MISSING_SET_LARGE_FEATURE_WITH_SALT_COUNT.incrementAndGet();
            }

            return null;
         } else {
            TRANSFORMED_COUNT.incrementAndGet();
            SET_LARGE_FEATURE_WITH_SALT_HOOKS.addAndGet(appliedHooks);
            if (!this.structurePlacementXorEnabled) {
               return classWriter.toByteArray();
            }

            if (hookCount[0] == 0) {
               MISSING_SET_LARGE_FEATURE_WITH_SALT_COUNT.incrementAndGet();
            }

            return classWriter.toByteArray();
         }
      } catch (Throwable ex) {
         FAILURE_COUNT.incrementAndGet();
         return null;
      }
   }

   public static String getHookStats() {
      int visits = VISIT_COUNT.get();
      int transformed = TRANSFORMED_COUNT.get();
      int setLargeFeatureWithSaltHooks = SET_LARGE_FEATURE_WITH_SALT_HOOKS.get();
      int missingSetLargeFeatureWithSalt = MISSING_SET_LARGE_FEATURE_WITH_SALT_COUNT.get();
      int failures = FAILURE_COUNT.get();
      return "visits="
         + visits
         + ", transformed="
         + transformed
         + ", setLargeFeatureWithSaltHooks="
         + setLargeFeatureWithSaltHooks
         + ", missingSetLargeFeatureWithSalt="
         + missingSetLargeFeatureWithSalt
         + ", failures="
         + failures;
   }

   private static boolean isSetLargeFeatureWithSaltMethod(String methodName, String descriptor) {
      return "setLargeFeatureWithSalt".equals(methodName) && ("(JIII)V".equals(descriptor) || "(JIII)J".equals(descriptor));
   }
}
