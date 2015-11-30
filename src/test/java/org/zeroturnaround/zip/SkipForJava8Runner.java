package org.zeroturnaround.zip;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

/**
 * JUnit runner that can be used to skip tests for JDK8. This is needed
 * for example some time stamp preserving tests that should have custom
 * JDK 8 implementations and custom pre JDK8 tests.
 */
public class SkipForJava8Runner extends BlockJUnit4ClassRunner {

  public SkipForJava8Runner(Class<?> testClass) throws InitializationError {
    super(testClass);
  }

  @Override
  protected boolean isIgnored(FrameworkMethod child) {
    return ZTZipReflectionUtil.isJdk8();
  }
}
