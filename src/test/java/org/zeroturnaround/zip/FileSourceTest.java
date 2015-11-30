package org.zeroturnaround.zip;
/**
 *    Copyright (C) 2012 ZeroTurnaround LLC <support@zeroturnaround.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
import junit.framework.TestCase;

import java.io.File;

/**
 * @author Innokenty Shuvalov
 */
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
