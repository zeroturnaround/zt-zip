package org.zeroturnaround.zip;

import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * JUnit rule that can be used to skip tests in case some API is not
 * available. This is needed for some time stamp preserving tests
 * that should have custom JDK 8 implementations and custom pre-JDK8 tests.
 */
public class SkipIfClassNotAvailableRule implements TestRule {

  private final String classNameToCheckFor;

  public SkipIfClassNotAvailableRule(String classNameToCheckFor) {
    this.classNameToCheckFor = classNameToCheckFor;
  }

  public Statement apply(Statement statement, Description desc) {
    if (ZTZipReflectionUtil.isClassAvailable(classNameToCheckFor)) {
      return statement;
    } else {
      return new IgnoreStatement();
    }
  }

  private class IgnoreStatement extends Statement {

    @Override
    public void evaluate() throws Throwable {
      Assume.assumeTrue(String.format("Ignoring, as %s API is not available", classNameToCheckFor), false);
    }
    
  }

}
