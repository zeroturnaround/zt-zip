package org.zeroturnaround.zip;

import java.util.zip.ZipEntry;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

/**
 * JUnit runner that can be used to skip tests for JDK8. This is needed
 * for example some timestamp preserving tests that should have custom
 * JDK 8 implementations and custom pre JDK8 tests.
 */
public class SkipForJava8Runner extends BlockJUnit4ClassRunner {

  public SkipForJava8Runner(Class<?> testClass) throws InitializationError {
    super(testClass);
  }

  protected boolean isIgnored(FrameworkMethod child) {
    return isJdk8();
  }

  private boolean isJdk8() {
    try {
      Class<?> fileTimeClass = Class.forName("java.nio.file.attribute.FileTime");
      // the following method is only in Java 1.8
      ZipEntry.class.getMethod("setLastModifiedTime", fileTimeClass);
      return true;
    }
    catch (NoSuchMethodException e) {
      // Ignore, we are not running Java 8
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
