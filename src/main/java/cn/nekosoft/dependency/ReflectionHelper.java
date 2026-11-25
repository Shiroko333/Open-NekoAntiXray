package cn.nekosoft.dependency;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ReflectionHelper {
   public Object newInstance(Class targetClass, Object[] constructorArgs) {
      try {
         return targetClass.getConstructor().newInstance(constructorArgs);
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
         throw new RuntimeException(ex);
      }
   }

   public Method getDeclaredMethod(Class targetClass, String methodName, boolean accessible, Class[] parameterTypes) {
      try {
         Method method = targetClass.getDeclaredMethod(methodName, parameterTypes);
         method.setAccessible(accessible);
         return method;
      } catch (NoSuchMethodException ex) {
         throw new RuntimeException(ex);
      }
   }

   public Field getDeclaredField(Class targetClass, String fieldName, boolean accessible) {
      try {
         Field field = targetClass.getDeclaredField(fieldName);
         field.setAccessible(accessible);
         return field;
      } catch (NoSuchFieldException ex) {
         throw new RuntimeException(ex);
      }
   }

   public Object invokeMethod(Method method, Object target, Object[] arguments) {
      try {
         return method.invoke(target, arguments);
      } catch (IllegalAccessException | InvocationTargetException ex) {
         throw new RuntimeException(ex);
      }
   }

   public Object getFieldValue(Field field, Object target) {
      try {
         return field.get(target);
      } catch (IllegalAccessException ex) {
         throw new RuntimeException(ex);
      }
   }

   public void setFieldValue(Field field, Object target, Object fieldValue) {
      try {
         field.set(target, fieldValue);
      } catch (IllegalAccessException ex) {
         throw new RuntimeException(ex);
      }
   }
}
