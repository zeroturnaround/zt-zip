package org.zeroturnaround.zip;

import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.zeroturnaround.zip.timestamps.TimestampStrategyFactory;

public class SkipIfZipEntryFileTimeNotAvailableRule implements TestRule {

  public Statement apply(Statement statement, Description desc) {
    if (TimestampStrategyFactory.HAS_ZIP_ENTRY_FILE_TIME_METHODS) {
      return statement;
    }
    return new IgnoreStatement();
  }

  private class IgnoreStatement extends Statement {

    @Override
    public void evaluate() throws Throwable {
      Assume.assumeTrue("Ignoring, as ZipEntry FileTime API is not available", false);
    }

  }

}
