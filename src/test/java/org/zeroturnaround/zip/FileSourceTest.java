package org.zeroturnaround.zip;

import junit.framework.TestCase;

import java.io.File;

public class FileSourceTest extends TestCase {

  /** @noinspection ConstantConditions*/
  private final File testFile
      = new File(getClass().getClassLoader().getResource("TestFile.txt").getPath());

  /** @noinspection ConstantConditions*/
  private final File testFileII
      = new File(getClass().getClassLoader().getResource("TestFile-II.txt").getPath());

  private final String name1 = "Changed-TestFile.txt";
  private final String name2 = "Changed-TestFile-II.txt";

  public void testPair() throws Exception {
    FileSource[] pairs = FileSource.pair(
        new File[]{testFile, testFileII},
        new String[]{name1, name2}
    );

    assertEquals(pairs.length, 2);
    assertEquals(pairs[0].getEntry().getName(), name1);
    assertEquals(pairs[1].getEntry().getName(), name2);
  }

  public void testPairThrowsExceptionWhenNotEnoughNames() throws Exception {
    try {
      FileSource.pair(
          new File[]{testFile, testFileII},
          new String[]{name1}
      );
    } catch (IllegalArgumentException e) {
      return;
    }
    fail("FileSource.pair must throw an IllegalArgumentException " +
        "if the names array is less than files array");
  }

  public void testPairDoesNotThrowExceptionWhenTooManyNames() throws Exception {
    try {
      FileSource.pair(
          new File[]{testFile},
          new String[]{name1, name2}
      );
    } catch (IllegalArgumentException e) {
      fail("FileSource.pair must not throw an IllegalArgumentException " +
          "if the names array is bigger than files array");
    }
  }
}
