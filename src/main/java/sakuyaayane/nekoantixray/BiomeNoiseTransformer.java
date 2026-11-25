package sakuyaayane.nekoantixray;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public class BiomeNoiseTransformer implements ClassFileTransformer, Opcodes {
   private static final String TARGET_CLASS_NAME = "net/minecraft/world/level/biome/MultiNoiseBiomeSource";
   private final int xXorMask;
   private final int zXorMask;

   @Override
   public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
      if (!"net/minecraft/world/level/biome/MultiNoiseBiomeSource".equals(className)) {
         return null;
      }

      try {
         ClassReader classReader = new ClassReader(classfileBuffer);
         ClassWriter classWriter = new ClassWriter(classReader, 3);
         final boolean[] transformed = new boolean[]{false};
         ClassVisitor classVisitor = new ClassVisitor(589824, classWriter) {
            @Override
            public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature, String[] exceptions) {
               MethodVisitor methodVisitor = super.visitMethod(access, methodName, descriptor, signature, exceptions);
               if ("getNoiseBiome".equals(methodName)
                  && descriptor.startsWith("(III")
                  && descriptor.contains("Climate$Sampler")
                  && descriptor.endsWith("Lnet/minecraft/core/Holder;")) {
                  transformed[0] = true;
                  return new AdviceAdapter(589824, methodVisitor, access, methodName, descriptor) {
                     @Override
                     protected void onMethodEnter() {
                        this.visitVarInsn(21, 1);
                        this.visitLdcInsn(BiomeNoiseTransformer.this.xXorMask);
                        this.visitInsn(130);
                        this.visitVarInsn(54, 1);
                        this.visitVarInsn(21, 3);
                        this.visitLdcInsn(BiomeNoiseTransformer.this.zXorMask);
                        this.visitInsn(130);
                        this.visitVarInsn(54, 3);
                     }
                  };
               } else {
                  return methodVisitor;
               }
            }
         };
         classReader.accept(classVisitor, 0);
         return transformed[0] ? classWriter.toByteArray() : null;
      } catch (Throwable ex) {
         return null;
      }
   }

   private static int nonZeroOrDefault(int candidate, int defaultValue) {
      return candidate == 0 ? defaultValue : candidate;
   }

   public BiomeNoiseTransformer(long seed) {
      this.xXorMask = nonZeroOrDefault((int)seed, 1831565813);
      this.zXorMask = nonZeroOrDefault((int)(seed >>> 32), 461845907);
   }
}
