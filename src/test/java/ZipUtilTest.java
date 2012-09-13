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
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipException;
import org.zeroturnaround.zip.ZipUtil;

public class ZipUtilTest extends TestCase {

  public void testUnpackEntry() throws IOException {
    final String name = "foo";
    final byte[] contents = "bar".getBytes();

    File file = File.createTempFile("temp", null);
    try {
      // Create the ZIP file
      ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));
      try {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(contents);
        zos.closeEntry();
      }
      finally {
        IOUtils.closeQuietly(zos);
      }

      // Test the ZipUtil
      byte[] actual = ZipUtil.unpackEntry(file, name);
      assertNotNull(actual);
      assertEquals(new String(contents), new String(actual));
    }
    finally {
      FileUtils.deleteQuietly(file);
    }
  }

  public void testDuplicateEntryAtAdd() throws IOException {
    File src = new File(getClass().getResource("duplicate.zip").getPath());

    File dest = File.createTempFile("temp", null);
    try {
      ZipUtil.addEntries(src, new ZipEntrySource[0], dest);
    }
    finally {
      FileUtils.deleteQuietly(dest);
    }
  }

  public void testDuplicateEntryAtReplace() throws IOException {
    File src = new File(getClass().getResource("duplicate.zip").getPath());

    File dest = File.createTempFile("temp", null);
    try {
      ZipUtil.replaceEntries(src, new ZipEntrySource[0], dest);
    }
    finally {
      FileUtils.deleteQuietly(dest);
    }
  }

  public void testDuplicateEntryAtAddOrReplace() throws IOException {
    File src = new File(getClass().getResource("duplicate.zip").getPath());

    File dest = File.createTempFile("temp", null);
    try {
      ZipUtil.addOrReplaceEntries(src, new ZipEntrySource[0], dest);
    }
    finally {
      FileUtils.deleteQuietly(dest);
    }
  }

  public void testUnexplode() throws IOException {
    File file = File.createTempFile("tempFile", null);
    File tmpDir = file.getParentFile();

    unexplodeWithException(file, "shouldn't be able to unexplode file that is not a directory");
    assertTrue("Should be able to delete tmp file", file.delete());
    unexplodeWithException(file, "shouldn't be able to unexplode file that doesn't exist");

    // create empty tmp dir with the same name as deleted file
    File dir = new File(tmpDir, file.getName());
    dir.deleteOnExit();
    assertTrue("Should be able to create directory with the same name as there was tmp file", dir.mkdir());

    unexplodeWithException(dir, "shouldn't be able to unexplode dir that doesn't contain any files");

    // unexplode should succeed with at least one file in directory
    File.createTempFile("temp", null, dir);
    ZipUtil.unexplode(dir);

    assertTrue("zip file should exist with the same name as the directory that was unexploded", dir.exists());
    assertTrue("unexploding input directory should have produced zip file with the same name", !dir.isDirectory());
    assertTrue("Should be able to delete zip that was created from directory", dir.delete());
  }

  public void testPackEntry() throws Exception {
    File fileToPack = new File(getClass().getResource("TestFile.txt").getPath());
    File dest = File.createTempFile("temp", null);
    ZipUtil.packEntry(fileToPack, dest);
    assertTrue(dest.exists());

    ZipUtil.explode(dest);
    assertTrue((new File(dest, "TestFile.txt")).exists());
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(108, (new File(dest, "TestFile.txt")).length());
  }

  public void testPackEntries() throws Exception {
    File fileToPack = new File(getClass().getResource("TestFile.txt").getPath());
    File fileToPackII = new File(getClass().getResource("TestFile-II.txt").getPath());
    File dest = File.createTempFile("temp", null);
    ZipUtil.packEntries(new File[] { fileToPack, fileToPackII }, dest);
    assertTrue(dest.exists());

    ZipUtil.explode(dest);
    assertTrue((new File(dest, "TestFile.txt")).exists());
    assertTrue((new File(dest, "TestFile-II.txt")).exists());
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(108, (new File(dest, "TestFile.txt")).length());
    assertEquals(103, (new File(dest, "TestFile-II.txt")).length());
  }

  public void testZipException() {
    boolean exceptionThrown = false;
    try {
      ZipUtil.pack(new File("nonExistent"), new File("weeheha"));
    }
    catch (ZipException e) {
      exceptionThrown = true;
    }
    assertTrue(exceptionThrown);
  }

  public void testPreserveRoot() throws Exception {
    File dest = File.createTempFile("temp", null);
    File parent = new File(getClass().getResource("TestFile.txt").getPath()).getParentFile();
    ZipUtil.pack(parent, dest, true);
    ZipUtil.explode(dest);
    assertTrue((new File(dest, parent.getName())).exists());
  }

  private void unexplodeWithException(File file, String message) {
    boolean ok = false;
    try {
      ZipUtil.unexplode(file);
    }
    catch (Exception e) {
      ok = true;
    }
    assertTrue(message, ok);
  }
}
