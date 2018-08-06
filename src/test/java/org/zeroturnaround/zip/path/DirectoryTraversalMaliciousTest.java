package org.zeroturnaround.zip.path;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.zeroturnaround.zip.MaliciousZipException;
import org.zeroturnaround.zip.ZipPathUtil;

import junit.framework.TestCase;

public class DirectoryTraversalMaliciousTest extends TestCase {
  /*
   * This is the contents of the file. There is one evil file that tries to get out of the
   * target.
   *
   * $ unzip -t zip-malicious-traversal.zip
   * Archive: zip-malicious-traversal.zip
   * testing: good.txt OK
   * testing: ../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../tmp/evil.txt OK
   * No errors detected in compressed data of zip-malicious-traversal.zip.
   */
  private static final Path badFile = Paths.get("src/test/resources/zip-malicious-traversal.zip");

  /*
   * This is the contents of the file. There is one evil file that tries to get out of the
   * target.
   *
   * $ unzip -t zip-malicious-traversal-root.zip
   * Archive: zip-malicious-traversal-root.zip
   * testing: someroot/good.txt OK
   * testing: someroot/../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../home/evil.txt OK
   * No errors detected in compressed data of zip-malicious-traversal-root.zip.
   */
  private static final Path badFileWithRoot = Paths.get("src/test/resources/zip-malicious-traversal-root.zip");

  /*
   * This is the contents of the file. There is one evil file that tries to get out of the
   * target.
   *
   * $ unzip -t zip-malicious-traversal-backslashes.zip
   * Archive: zip-malicious-traversal-backslashes.zip
   * testing: someroot/good.txt OK
   * testing: ..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\home\evil.txt OK
   * No errors detected in compressed data of zip-malicious-traversal-backslashes.zip.
   */
  private static final Path badFileBackslashes = Paths.get("src/test/resources/zip-malicious-traversal-backslashes.zip");

  public void testUnpackDoesntLeaveTarget() throws Exception {
    Path tmpDir = Files.createTempDirectory("temp");

    try {
      ZipPathUtil.unpack(badFile, tmpDir);
      fail();
    }
    catch (MaliciousZipException e) {
      assertTrue(true);
    }
  }

  public void testUnwrapDoesntLeaveTarget() throws Exception {
    Path tmpDir = Files.createTempDirectory("temp");

    try {
      ZipPathUtil.unwrap(badFileWithRoot, tmpDir);
      fail();
    }
    catch (MaliciousZipException e) {
      assertTrue(true);
    }
  }

  public void testBackslashUnpackerDoesntLeaveTarget() throws Exception {
    Path tmpDir = Files.createTempDirectory("temp");

    try {
      ZipPathUtil.iterate(badFileBackslashes, new ZipPathUtil.BackslashUnpacker(tmpDir));
      fail();
    }
    catch (MaliciousZipException e) {
      assertTrue(true);
    }
  }
}
