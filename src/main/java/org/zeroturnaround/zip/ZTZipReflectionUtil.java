package org.zeroturnaround.zip;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ZTZipReflectionUtil {
  private ZTZipReflectionUtil() {
  }

  static <T> Class<? extends T> getClassForName(String name, Class<T> clazz) {
    try {
      return Class.forName(name).asSubclass(clazz);
    }
    catch (ClassNotFoundException e) {
      throw new ZipException(e);
    }
    catch (ClassCastException e) {
      throw new ZipException(e);
    }
  }

  static Method getDeclaredMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
    try {
      return clazz.getDeclaredMethod(methodName, parameterTypes);
    }
    catch (NoSuchMethodException e) {
      throw new ZipException(e);
    }
  }

  static Object invoke(Method method, Object obj, Object... args) throws ZipException {
    try {
      return method.invoke(obj, args);
    }
    catch (IllegalAccessException e) {
      throw new ZipException(e);
    }
    catch (InvocationTargetException e) {
      throw new ZipException(e);
    }
    catch (IllegalArgumentException e) {
      throw new ZipException(e);
    }
  }
  
  public static boolean isJdk8() {
    try {
      Class<?> streamClass = Class.forName("java.util.stream.Stream");
      return true;
    }
    catch (ClassNotFoundException e) {
      // Ignore, we are not running Java 8
    }
    catch (SecurityException e) {
      // Ignore, we are not running Java 8
    }
    return false;
  }
}
