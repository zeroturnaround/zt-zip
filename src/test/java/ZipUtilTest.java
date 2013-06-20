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
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.zeroturnaround.zip.ZipBreakException;
import org.zeroturnaround.zip.ZipEntryCallback;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipException;
import org.zeroturnaround.zip.ZipInfoCallback;
import org.zeroturnaround.zip.ZipUtil;

public class ZipUtilTest extends TestCase {

  public void testPackEntryStream() {
    File src = new File(getClass().getResource("TestFile.txt").getPath());
    byte[] bytes = ZipUtil.packEntry(src);
    boolean processed = ZipUtil.handle(new ByteArrayInputStream(bytes), "TestFile.txt", new ZipEntryCallback() {

      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
      }
    });
    assertTrue(processed);
  }

  public void testPackEntryFile() throws Exception {
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

  public void testUnpackEntryFromFile() throws IOException {
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

  public void testUnpackEntryFromStreamToFile() throws IOException {
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

      FileInputStream fis = new FileInputStream(file);

      File outputFile = File.createTempFile("temp-output", null);

      boolean result = ZipUtil.unpackEntry(fis, name, outputFile);
      assertTrue(result);

      BufferedInputStream bis = new BufferedInputStream(new FileInputStream(outputFile));
      byte[] actual = new byte[1024];
      int read = bis.read(actual);
      bis.close();

      assertEquals(new String(contents), new String(actual, 0, read));
    }
    finally {
      FileUtils.deleteQuietly(file);
    }
  }

  public void testUnpackEntryFromStream() throws IOException {
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

      FileInputStream fis = new FileInputStream(file);
      // Test the ZipUtil
      byte[] actual = ZipUtil.unpackEntry(fis, name);
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

  public void testArchiveEquals() {
    File src = new File(getClass().getResource("demo.zip").getPath());
    // byte-by-byte copy
    File src2 = new File(getClass().getResource("demo-copy.zip").getPath());
    assertTrue(ZipUtil.archiveEquals(src, src2));

    // entry by entry copy
    File src3 = new File(getClass().getResource("demo-copy-II.zip").getPath());
    assertTrue(ZipUtil.archiveEquals(src, src3));
  }

  public void testRepackArchive() throws IOException {
    File src = new File(getClass().getResource("demo.zip").getPath());
    File dest = File.createTempFile("temp", null);

    ZipUtil.repack(src, dest, 1);

    assertTrue(ZipUtil.archiveEquals(src, dest));
  }

  public void testContainsAnyEntry() throws IOException {
    File src = new File(getClass().getResource("demo.zip").getPath());
    boolean exists = ZipUtil.containsAnyEntry(src, new String[] { "foo.txt", "bar.txt" });
    assertTrue(exists);

    exists = ZipUtil.containsAnyEntry(src, new String[] { "foo.txt", "does-not-exist.txt" });
    assertTrue(exists);

    exists = ZipUtil.containsAnyEntry(src, new String[] { "does-not-exist-I.txt", "does-not-exist-II.txt" });
    assertFalse(exists);
  }

  public void testAddEntry() throws IOException {
    File src = new File(getClass().getResource("demo.zip").getPath());
    final String fileName = "TestFile.txt";
    assertFalse(ZipUtil.containsEntry(src, fileName));
    File newEntry = new File(getClass().getResource(fileName).getPath());
    File dest = File.createTempFile("temp.zip", null);

    ZipUtil.addEntry(src, fileName, newEntry, dest);
    assertTrue(ZipUtil.containsEntry(dest, fileName));
  }

  public void testRemoveEntry() throws IOException {
    File src = new File(getClass().getResource("demo.zip").getPath());

    File dest = File.createTempFile("temp", null);
    try {
      ZipUtil.removeEntry(src, "bar.txt", dest);
      assertTrue("Result zip misses entry 'foo.txt'", ZipUtil.containsEntry(dest, "foo.txt"));
      assertTrue("Result zip misses entry 'foo1.txt'", ZipUtil.containsEntry(dest, "foo1.txt"));
      assertTrue("Result zip misses entry 'foo2.txt'", ZipUtil.containsEntry(dest, "foo2.txt"));
      assertFalse("Result zip still contains 'bar.txt'", ZipUtil.containsEntry(dest, "bar.txt"));
    }
    finally {
      FileUtils.deleteQuietly(dest);
    }
  }

  public void testRemoveDirs() throws IOException {
    File src = new File(getClass().getResource("demo-dirs.zip").getPath());

    File dest = File.createTempFile("temp", null);
    try {
      ZipUtil.removeEntries(src, new String[] { "bar.txt", "a/b" }, dest);

      assertFalse("Result zip still contains 'bar.txt'", ZipUtil.containsEntry(dest, "bar.txt"));
      assertFalse("Result zip still contains dir 'a/b'", ZipUtil.containsEntry(dest, "a/b"));
      assertTrue("Result doesn't containt 'attic'", ZipUtil.containsEntry(dest, "attic/treasure.txt"));
      assertTrue("Entry whose prefix is dir name is removed too: 'b.txt'", ZipUtil.containsEntry(dest, "a/b.txt"));
      assertFalse("Entry in a removed dir is still there: 'a/b/c.txt'", ZipUtil.containsEntry(dest, "a/b/c.txt"));

    }
    finally {
      FileUtils.deleteQuietly(dest);
    }
  }

  public void testHandle() {
    File src = new File(getClass().getResource("demo.zip").getPath());

    boolean entryFound = ZipUtil.handle(src, "foo.txt", new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        assertEquals("foo.txt", zipEntry.getName());
      }
    });
    assertTrue(entryFound);

    entryFound = ZipUtil.handle(src, "non-existent-file.txt", new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        throw new RuntimeException("This should not happen!");
      }
    });
    assertFalse(entryFound);
  }

  public void testIterate() {
    File src = new File(getClass().getResource("demo.zip").getPath());
    final Set files = new HashSet();
    files.add("foo.txt");
    files.add("bar.txt");
    files.add("foo1.txt");
    files.add("foo2.txt");

    ZipUtil.iterate(src, new ZipInfoCallback() {

      public void process(ZipEntry zipEntry) throws IOException {
        files.remove(zipEntry.getName());
      }
    });
    assertEquals(0, files.size());
  }

  public void testIterateGivenEntries() {
    File src = new File(getClass().getResource("demo.zip").getPath());
    final Set files = new HashSet();
    files.add("foo.txt");
    files.add("bar.txt");
    files.add("foo1.txt");
    files.add("foo2.txt");

    ZipUtil.iterate(src, new String[] { "foo.txt", "foo1.txt", "foo2.txt" }, new ZipInfoCallback() {

      public void process(ZipEntry zipEntry) throws IOException {
        files.remove(zipEntry.getName());
      }
    });
    assertEquals(1, files.size());
    assertTrue("Wrong entry hasn't beed iterated", files.contains("bar.txt"));
  }

  public void testIterateGivenEntriesFromStream() throws IOException {
    File src = new File(getClass().getResource("demo.zip").getPath());
    final Set files = new HashSet();
    files.add("foo.txt");
    files.add("bar.txt");
    files.add("foo1.txt");
    files.add("foo2.txt");

    FileInputStream inputStream = null;
    try {
      inputStream = new FileInputStream(src);
      ZipUtil.iterate(inputStream, new String[] { "foo.txt", "foo1.txt", "foo2.txt" }, new ZipEntryCallback() {
        public void process(InputStream in, ZipEntry zipEntry) throws IOException {
          files.remove(zipEntry.getName());
        }
      });
      assertEquals(1, files.size());
      assertTrue("Wrong entry hasn't beed iterated", files.contains("bar.txt"));
    }
    finally {
      inputStream.close();
    }
  }

  public void testIterateAndBreak() {
    File src = new File(getClass().getResource("demo.zip").getPath());
    final Set files = new HashSet();
    files.add("foo.txt");
    files.add("bar.txt");
    files.add("foo1.txt");
    files.add("foo2.txt");

    ZipUtil.iterate(src, new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        files.remove(zipEntry.getName());
        throw new ZipBreakException();
      }
    });
    assertEquals(3, files.size());
  }

  public void testUnwrapFile() throws Exception {
    File dest = File.createTempFile("temp", null);
    File destDir = File.createTempFile("tempDir", null);
    try {
      destDir.delete();
      destDir.mkdir();
      String child = "TestFile.txt";
      File parent = new File(getClass().getResource(child).getPath()).getParentFile();
      ZipUtil.pack(parent, dest, true);
      ZipUtil.unwrap(dest, destDir);
      assertTrue((new File(destDir, child)).exists());
    }
    finally {
      FileUtils.forceDelete(destDir);
    }
  }

  public void testUnwrapStream() throws Exception {
    File dest = File.createTempFile("temp", null);
    File destDir = File.createTempFile("tempDir", null);
    InputStream is = null;
    try {
      destDir.delete();
      destDir.mkdir();
      String child = "TestFile.txt";
      File parent = new File(getClass().getResource(child).getPath()).getParentFile();
      ZipUtil.pack(parent, dest, true);
      is = new FileInputStream(dest);
      ZipUtil.unwrap(is, destDir);
      assertTrue((new File(destDir, child)).exists());
    }
    finally {
      IOUtils.closeQuietly(is);
      FileUtils.forceDelete(destDir);
    }
  }

  public void testUnwrapEntriesInRoot() throws Exception {
    File src = new File(getClass().getResource("demo.zip").getPath());
    File destDir = File.createTempFile("tempDir", null);
    try {
      destDir.delete();
      destDir.mkdir();
      ZipUtil.unwrap(src, destDir);
      fail("expected a ZipException, unwraping with multiple roots is not supproted");
    }
    catch (ZipException e) {
      // this is normal outcome
    }
    finally {
      FileUtils.forceDelete(destDir);
    }
  }

  public void testUnwrapMultipleRoots() throws Exception {
    File src = new File(getClass().getResource("demo-dirs-only.zip").getPath());
    File destDir = File.createTempFile("tempDir", null);
    try {
      destDir.delete();
      destDir.mkdir();
      ZipUtil.unwrap(src, destDir);
      fail("expected a ZipException, unwraping with multiple roots is not supproted");
    }
    catch (ZipException e) {
      // this is normal outcome
    }
    finally {
      FileUtils.forceDelete(destDir);
    }
  }

  public void testUnwrapSingleRootWithStructure() throws Exception {
    File src = new File(getClass().getResource("demo-single-root-dir.zip").getPath());
    File destDir = File.createTempFile("tempDir", null);
    try {
      destDir.delete();
      destDir.mkdir();
      ZipUtil.unwrap(src, destDir);
      assertTrue((new File(destDir, "b.txt")).exists());
      assertTrue((new File(destDir, "bad.txt")).exists());
      assertTrue((new File(destDir, "b")).exists());
      assertTrue((new File(new File(destDir, "b"), "c.txt")).exists());
    }
    finally {
      FileUtils.forceDelete(destDir);
    }
  }

  public void testUnwrapEmptyRootDir() throws Exception {
    File src = new File(getClass().getResource("demo-single-empty-root-dir.zip").getPath());
    File destDir = File.createTempFile("tempDir", null);
    try {
      destDir.delete();
      destDir.mkdir();
      ZipUtil.unwrap(src, destDir);
      assertTrue("Dest dir should be empty, root dir was shaved", destDir.list().length == 0);
    }
    finally {
      FileUtils.forceDelete(destDir);
    }
  }

}
