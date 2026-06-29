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
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
  private static final File badFile = new File("src/test/resources/zip-malicious-traversal.zip");

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
  private static final File badFileWithRoot = new File("src/test/resources/zip-malicious-traversal-root.zip");

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
  private static final File badFileBackslashes = new File("src/test/resources/zip-malicious-traversal-backslashes.zip");

  public void testUnpackDoesntLeaveTarget() throws Exception {
    File file = File.createTempFile("temp", null);
    File tmpDir = file.getParentFile();

    try {
      ZipUtil.unpack(badFile, tmpDir);
      fail();
    }
    catch (MaliciousZipException e) {
      assertTrue(true);
    }
  }

  public void testUnwrapDoesntLeaveTarget() throws Exception {
    File file = File.createTempFile("temp", null);
    File tmpDir = file.getParentFile();

    try {
      ZipUtil.unwrap(badFileWithRoot, tmpDir);
      fail();
    }
    catch (MaliciousZipException e) {
      assertTrue(true);
    }
  }

  public void testBackslashUnpackerDoesntLeaveTarget() throws Exception {
    File file = File.createTempFile("temp", null);
    File tmpDir = file.getParentFile();

    try {
      ZipUtil.iterate(badFileBackslashes, new ZipUtil.BackslashUnpacker(tmpDir));
      fail();
    }
    catch (MaliciousZipException e) {
      assertTrue(true);
    }
  }

  /*
   * An entry can leave the target without escaping its parent: a name like
   * "../targetX" resolves to a sibling directory whose path shares a prefix with
   * the target. A plain string prefix check treats that as inside the target; the
   * traversal guard must compare path components instead.
   */
  public void testUnpackDoesntLeaveTargetForSiblingWithSharedPrefix() throws Exception {
    File parent = Files.createTempDirectory("zt-zip-traversal").toFile();
    File target = new File(parent, "target");
    target.mkdir();

    File zip = File.createTempFile("sibling-prefix-traversal", ".zip");
    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
    try {
      out.putNextEntry(new ZipEntry("good.txt"));
      out.closeEntry();
      out.putNextEntry(new ZipEntry("../targetX/evil.txt"));
      out.closeEntry();
    }
    finally {
      out.close();
    }

    try {
      ZipUtil.unpack(zip, target);
      fail();
    }
    catch (MaliciousZipException e) {
      assertFalse(new File(parent, "targetX/evil.txt").exists());
    }
  }
}
