package org.zeroturnaround.zip;

import junit.framework.TestCase;

public class FailingToBeDeletedTest extends TestCase {

  public void testSomethingAndFail() {
    fail("This is a failing test");
  }

}
