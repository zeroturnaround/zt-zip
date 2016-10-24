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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.zeroturnaround.zip.commons.FileUtils;
import org.zeroturnaround.zip.commons.IOUtils;

import junit.framework.TestCase;


/** @noinspection ResultOfMethodCallIgnored*/
public class ZipUtilTest extends TestCase {

  /** @noinspection ConstantConditions*/
  public static File file(String name) {
    return new File(ZipUtilTest.class.getClassLoader().getResource(name).getPath());
  }

  public void testPackEntryStream() {
    File src = file("TestFile.txt");
    byte[] bytes = ZipUtil.packEntry(src);
    boolean processed = ZipUtil.handle(new ByteArrayInputStream(bytes), "TestFile.txt", new ZipEntryCallback() {

      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
      }
    });
    assertTrue(processed);
  }

  public void testPackEntryFile() throws Exception {
    File fileToPack = file("TestFile.txt");
    File dest = File.createTempFile("temp", null);
    ZipUtil.packEntry(fileToPack, dest);
    assertTrue(dest.exists());

    ZipUtil.explode(dest);
    assertTrue((new File(dest, "TestFile.txt")).exists());
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(108, (new File(dest, "TestFile.txt")).length());
  }

  public void testPackEntryFileWithNameParameter() throws Exception {
    File fileToPack = file("TestFile.txt");
    File dest = File.createTempFile("temp", null);
    ZipUtil.packEntry(fileToPack, dest, "TestFile-II.txt");
    assertTrue(dest.exists());

    ZipUtil.explode(dest);
    assertTrue((new File(dest, "TestFile-II.txt")).exists());
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(108, (new File(dest, "TestFile-II.txt")).length());
  }

  public void testPackEntryFileWithNameMapper() throws Exception {
    File fileToPack = file("TestFile.txt");
    File dest = File.createTempFile("temp", null);
    ZipUtil.packEntry(fileToPack, dest, new NameMapper() {
      public String map(String name) {
        return "TestFile-II.txt";
      }
    });
    assertTrue(dest.exists());

    ZipUtil.explode(dest);
    assertTrue((new File(dest, "TestFile-II.txt")).exists());
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(108, (new File(dest, "TestFile-II.txt")).length());
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
    File src = file("duplicate.zip");

    File dest = File.createTempFile("temp", null);
    try {
      ZipUtil.addEntries(src, new ZipEntrySource[0], dest);
    }
    finally {
      FileUtils.deleteQuietly(dest);
    }
  }

  public void testDuplicateEntryAtReplace() throws IOException {
    File src = file("duplicate.zip");

    File dest = File.createTempFile("temp", null);
    try {
      ZipUtil.replaceEntries(src, new ZipEntrySource[0], dest);
    }
    finally {
      FileUtils.deleteQuietly(dest);
    }
  }

  public void testDuplicateEntryAtAddOrReplace() throws IOException {
    File src = file("duplicate.zip");

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

  public void testPackEntriesWithCompressionLevel() throws Exception {
    long filesizeBestCompression = 0;
    long filesizeNoCompression = 0;

    ZipFile zf = null;
    File dest = null;

    try {
        dest = File.createTempFile("temp-stor", null);
        ZipUtil.packEntries(new File[]{file("TestFile.txt"), file("TestFile-II.txt")}, dest, Deflater.BEST_COMPRESSION);
        zf = new ZipFile(dest);
        filesizeBestCompression = zf.getEntry("TestFile.txt").getCompressedSize();
    }finally {
        zf.close();
    }

    try {
        dest = File.createTempFile("temp-stor", null);
        ZipUtil.packEntries(new File[]{file("TestFile.txt"), file("TestFile-II.txt")}, dest, Deflater.NO_COMPRESSION);
        zf = new ZipFile(dest);
        filesizeNoCompression = zf.getEntry("TestFile.txt").getCompressedSize();
    }finally {
        zf.close();
    }

    assertTrue(filesizeNoCompression > 0);
    assertTrue(filesizeBestCompression > 0);
    assertTrue(filesizeNoCompression > filesizeBestCompression);
  }

  public void testPackEntries() throws Exception {
    File fileToPack = file("TestFile.txt");
    File fileToPackII = file("TestFile-II.txt");
    File dest = File.createTempFile("temp", null);
    ZipUtil.packEntries(new File[]{fileToPack, fileToPackII}, dest);
    assertTrue(dest.exists());

    ZipUtil.explode(dest);
    assertTrue((new File(dest, "TestFile.txt")).exists());
    assertTrue((new File(dest, "TestFile-II.txt")).exists());
    // if fails then maybe somebody changed the file contents and did not update the test
    assertEquals(108, (new File(dest, "TestFile.txt")).length());
    assertEquals(103, (new File(dest, "TestFile-II.txt")).length());
  }

  public void testPackEntriesToStream() throws Exception {
    String encoding = "UTF-8";
    //list of entries, each entry consists of entry name and entry contents
    List<String[]> entryDescriptions = Arrays.asList(new String[][] {
      new String[] {"foo.txt", "foo"},
      new String[] {"bar.txt", "bar"}
      });
    ByteArrayOutputStream out = null;
    out = new ByteArrayOutputStream();
    ZipUtil.pack(convertToEntries(entryDescriptions, encoding), out);
    
    byte[] zipBytes = out.toByteArray();
    assertEquals(244, zipBytes.length);
    assertEntries(entryDescriptions, zipBytes, encoding);
  }

  public void testAddEntriesToStream() throws Exception {
    String encoding = "UTF-8";
    //list of entries, each entry consists of entry name and entry contents
    List<String[]> entryDescriptions = Arrays.asList(new String[][] {
      new String[] {"foo.txt", "foo"},
      new String[] {"bar.txt", "bar"}
      });
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ZipUtil.pack(convertToEntries(entryDescriptions, encoding), out);
    byte[] zipBytes = out.toByteArray();
    List<String[]> entryDescriptions2 = Arrays.asList(new String[][] {
      new String[] {"foo2.txt", "foo2"},
      new String[] {"bar2.txt", "bar2"}
      });
    ByteArrayOutputStream out2 = new ByteArrayOutputStream();
    ZipUtil.addEntries(new ByteArrayInputStream(zipBytes), convertToEntries(entryDescriptions2, encoding), out2);
    byte[] zipBytes2 = out2.toByteArray();
    ArrayList<String[]> allEntryDescriptions = new ArrayList<String[]>(entryDescriptions.size() + entryDescriptions2.size());
    allEntryDescriptions.addAll(entryDescriptions);
    allEntryDescriptions.addAll(entryDescriptions2);
    
    assertEntries(allEntryDescriptions, zipBytes2, encoding);
  }

  private ZipEntrySource[] convertToEntries(List<String[]> entryDescriptions, String encoding) throws UnsupportedEncodingException {
    ZipEntrySource[] entries = new ZipEntrySource[entryDescriptions.size()];
    for (int i = 0; i < entries.length; i++) {
      String[] entryDescription = entryDescriptions.get(i);
      entries[i] = new ByteSource(entryDescription[0], entryDescription[1].getBytes(encoding));
    }
    return entries;
  }

  private void assertEntries(final List<String[]> entryDescriptions, byte[] zipBytes, final String encoding) {
    final ArrayList<String> actualContents = new ArrayList<String>(entryDescriptions.size());
    ZipUtil.iterate(new ByteArrayInputStream(zipBytes), new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        String content = IOUtils.toString(in, encoding);
        actualContents.add(content);
        for (int i = 0; i < entryDescriptions.size(); i++) {
          String[] entryDescription = entryDescriptions.get(i);
          if (zipEntry.getName().equals(entryDescription[0])) {
            assertEquals(entryDescription[1], content);
          }
        }
      }
    });
    assertEquals(entryDescriptions.size(), actualContents.size());
  }

  public void testPackEntriesWithNameMapper() throws Exception {
    File fileToPack = file("TestFile.txt");
    File fileToPackII = file("TestFile-II.txt");
    File dest = File.createTempFile("temp", null);
    ZipUtil.packEntries(new File[] { fileToPack, fileToPackII }, dest, new NameMapper() {
      public String map(String name) {
        return "Changed-" + name;
      }
    });
    assertTrue(dest.exists());

    ZipUtil.explode(dest);
    assertTrue((new File(dest, "Changed-TestFile.txt")).exists());
    assertTrue((new File(dest, "Changed-TestFile-II.txt")).exists());
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(108, (new File(dest, "Changed-TestFile.txt")).length());
    assertEquals(103, (new File(dest, "Changed-TestFile-II.txt")).length());
  }

  public void testZipException() {
    boolean exceptionThrown = false;
    File target = new File("weeheha");
    try {
      ZipUtil.pack(new File("nonExistent"), target);
    }
    catch (ZipException e) {
      exceptionThrown = true;
    }
    assertFalse("Target file is created when source does not exist", target.exists());
    assertTrue(exceptionThrown);
  }

  public void testPackEntriesWithNamesList() throws Exception {
    File fileToPack = file("TestFile.txt");
    File fileToPackII = file("TestFile-II.txt");
    File dest = File.createTempFile("temp", null);

    ZipUtil.pack(
      FileSource.pair(
          new File[]{fileToPack, fileToPackII},
          new String[]{"Changed-TestFile.txt", "Changed-TestFile-II.txt"}),
      dest
    );

    assertTrue(dest.exists());

    ZipUtil.explode(dest);
    assertTrue((new File(dest, "Changed-TestFile.txt")).exists());
    assertTrue((new File(dest, "Changed-TestFile-II.txt")).exists());
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(108, (new File(dest, "Changed-TestFile.txt")).length());
    assertEquals(103, (new File(dest, "Changed-TestFile-II.txt")).length());
  }

  public void testPreserveRoot() throws Exception {
    File dest = File.createTempFile("temp", null);
    File parent = file("TestFile.txt").getParentFile();
    ZipUtil.pack(parent, dest, true);
    ZipUtil.explode(dest);
    assertTrue((new File(dest, parent.getName())).exists());
  }

  private void unexplodeWithException(File file, String message) {
    try {
      ZipUtil.unexplode(file);
    }
    catch (Exception e) {
      return;
    }
    fail(message);
  }

  public void testArchiveEquals() {
    File src = file("demo.zip");
    // byte-by-byte copy
    File src2 = file("demo-copy.zip");
    assertTrue(ZipUtil.archiveEquals(src, src2));

    // entry by entry copy
    File src3 = file("demo-copy-II.zip");
    assertTrue(ZipUtil.archiveEquals(src, src3));
  }

  public void testRepackArchive() throws IOException {
    File src = file("demo.zip");
    File dest = File.createTempFile("temp", null);
    ZipUtil.repack(src, dest, 1);
    assertTrue(ZipUtil.archiveEquals(src, dest));
  }

  public void testContainsAnyEntry() throws IOException {
    File src = file("demo.zip");
    boolean exists = ZipUtil.containsAnyEntry(src, new String[] { "foo.txt", "bar.txt" });
    assertTrue(exists);

    exists = ZipUtil.containsAnyEntry(src, new String[] { "foo.txt", "does-not-exist.txt" });
    assertTrue(exists);

    exists = ZipUtil.containsAnyEntry(src, new String[] { "does-not-exist-I.txt", "does-not-exist-II.txt" });
    assertFalse(exists);
  }

  public void testAddEntry() throws IOException {
    File initialSrc = file("demo.zip");

    File src = File.createTempFile("ztr", ".zip");
    FileUtils.copyFile(initialSrc, src);

    final String fileName = "TestFile.txt";
    if(ZipUtil.containsEntry(src, fileName)) {
      ZipUtil.removeEntry(src, fileName);
    }
    assertFalse(ZipUtil.containsEntry(src, fileName));
    File newEntry = file(fileName);
    File dest = File.createTempFile("temp.zip", null);

    ZipUtil.addEntry(src, fileName, newEntry, dest);
    assertTrue(ZipUtil.containsEntry(dest, fileName));
    FileUtils.forceDelete(src);
  }
  
  public void testKeepEntriesState() throws IOException {
    File src = file("demo-keep-entries-state.zip");
    final String existingEntryName = "TestFile.txt";
    final String fileNameToAdd = "TestFile-II.txt";
    assertFalse(ZipUtil.containsEntry(src, fileNameToAdd));
    File newEntry = file(fileNameToAdd);
    File dest = File.createTempFile("temp.zip", null);
    ZipUtil.addEntry(src, fileNameToAdd, newEntry, dest);
    
    ZipEntry srcEntry = new ZipFile(src).getEntry(existingEntryName);
    ZipEntry destEntry = new ZipFile(dest).getEntry(existingEntryName);
    assertTrue(srcEntry.getCompressedSize() == destEntry.getCompressedSize());
  }

  public void testRemoveEntry() throws IOException {
    File src = file("demo.zip");

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

  public void testRemoveMissingEntry() throws IOException {
    File src = file("demo.zip");
    assertFalse("Source zip contains entry 'missing.txt'", ZipUtil.containsEntry(src, "missing.txt"));

    File dest = File.createTempFile("temp", null);
    try {
      ZipUtil.removeEntry(src, "missing.txt", dest);
    }
    finally {
      FileUtils.deleteQuietly(dest);
    }
  }

  public void testRemoveDirs() throws IOException {
    File src = file("demo-dirs.zip");

    File dest = File.createTempFile("temp", null);
    try {
      ZipUtil.removeEntries(src, new String[] { "bar.txt", "a/b" }, dest);

      assertFalse("Result zip still contains 'bar.txt'", ZipUtil.containsEntry(dest, "bar.txt"));
      assertFalse("Result zip still contains dir 'a/b'", ZipUtil.containsEntry(dest, "a/b"));
      assertTrue("Result doesn't contain 'attic'", ZipUtil.containsEntry(dest, "attic/treasure.txt"));
      assertTrue("Entry whose prefix is dir name is removed too: 'b.txt'", ZipUtil.containsEntry(dest, "a/b.txt"));
      assertFalse("Entry in a removed dir is still there: 'a/b/c.txt'", ZipUtil.containsEntry(dest, "a/b/c.txt"));

    }
    finally {
      FileUtils.deleteQuietly(dest);
    }
  }

  public void testHandle() {
    File src = file("demo.zip");

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
    File src = file("demo.zip");
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

  public void testIterateGivenEntriesZipInfoCallback() {
    File src = file("demo.zip");
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
    assertTrue("Wrong entry hasn't been iterated", files.contains("bar.txt"));
  }
  
  public void testIterateGivenEntriesZipEntryCallback() {
    File src = file("demo.zip");
    final Set files = new HashSet();
    files.add("foo.txt");
    files.add("bar.txt");
    files.add("foo1.txt");
    files.add("foo2.txt");

    ZipUtil.iterate(src, new String[] { "foo.txt", "foo1.txt", "foo2.txt" }, new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        files.remove(zipEntry.getName());
      }
    });
    assertEquals(1, files.size());
    assertTrue("Wrong entry hasn't been iterated", files.contains("bar.txt"));
  }

  public void testIterateGivenEntriesFromStream() throws IOException {
    File src = file("demo.zip");
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
      assertTrue("Wrong entry hasn't been iterated", files.contains("bar.txt"));
    }
    finally {
      if (inputStream != null) {
        inputStream.close();
      }
    }
  }

  public void testIterateAndBreak() {
    File src = file("demo.zip");
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
      File parent = file(child).getParentFile();
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
      File parent = file(child).getParentFile();
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
    File src = file("demo.zip");
    File destDir = File.createTempFile("tempDir", null);
    try {
      destDir.delete();
      destDir.mkdir();
      ZipUtil.unwrap(src, destDir);
      fail("expected a ZipException, unwrapping with multiple roots is not supported");
    }
    catch (ZipException e) {
      // this is normal outcome
    }
    finally {
      FileUtils.forceDelete(destDir);
    }
  }

  public void testUnwrapMultipleRoots() throws Exception {
    File src = file("demo-dirs-only.zip");
    File destDir = File.createTempFile("tempDir", null);
    try {
      destDir.delete();
      destDir.mkdir();
      ZipUtil.unwrap(src, destDir);
      fail("expected a ZipException, unwrapping with multiple roots is not supported");
    }
    catch (ZipException e) {
      // this is normal outcome
    }
    finally {
      FileUtils.forceDelete(destDir);
    }
  }

  public void testUnwrapSingleRootWithStructure() throws Exception {
    File src = file("demo-single-root-dir.zip");
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
    File src = file("demo-single-empty-root-dir.zip");
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

  public void testUnpackEntryDir() throws Exception {
    File src = file("demo-dirs.zip");
    File dest = File.createTempFile("unpackEntryDir", null);
    try {
      ZipUtil.unpackEntry(src, "a", dest);
      assertTrue("Couldn't unpackEntry of a directory entry from a zip!", dest.exists());
      assertTrue("UnpackedEntry of a directory is not a dir!", dest.isDirectory());
    }
    finally {
      FileUtils.forceDelete(dest);
    }

  }


  public void testAddEntryWithCompressionMethodAndDestFile() throws IOException {
      int compressionMethod = ZipEntry.STORED;
      doTestAddEntryWithCompressionMethodAndDestFile(compressionMethod);

      compressionMethod = ZipEntry.DEFLATED;
      doTestAddEntryWithCompressionMethodAndDestFile(compressionMethod);
  }

  private void doTestAddEntryWithCompressionMethodAndDestFile(int compressionMethod) throws IOException {
      File src = file("demo.zip");
      final String fileName = "TestFile.txt";
      if(ZipUtil.containsEntry(src, fileName)) {
        ZipUtil.removeEntry(src, fileName);
      }
      assertFalse(ZipUtil.containsEntry(src, fileName));
      InputStream is = null;
      try {
          is = new FileInputStream(file(fileName));
          byte[] newEntry = IOUtils.toByteArray(is);
          File dest = File.createTempFile("temp.zip", null);
          ZipUtil.addEntry(src, fileName, newEntry, dest, compressionMethod);
          assertTrue(ZipUtil.containsEntry(dest, fileName));

          assertEquals(compressionMethod, ZipUtil.getCompressionMethodOfEntry(dest, fileName));
      } finally {
          IOUtils.closeQuietly(is);
      }
  }

  public void testAddEntryWithCompressionMethodStoredInPlace() throws IOException {
      int compressionMethod = ZipEntry.STORED;
      File src = file("demo.zip");
      File srcCopy = File.createTempFile("ztr", ".zip");
      FileUtils.copyFile(src, srcCopy);
      doTestAddEntryWithCompressionMethodInPlace(srcCopy, compressionMethod);
      FileUtils.forceDelete(srcCopy);
  }

  public void testAddEntryWithCompressionMethodDeflatedInPlace() throws IOException {
      int compressionMethod = ZipEntry.DEFLATED;
      File src = file("demo.zip");
      File srcCopy = File.createTempFile("ztr", ".zip");
      FileUtils.copyFile(src, srcCopy);
      doTestAddEntryWithCompressionMethodInPlace(srcCopy, compressionMethod);
      FileUtils.forceDelete(srcCopy);
  }

  private void doTestAddEntryWithCompressionMethodInPlace(File src, int compressionMethod) throws IOException {
      final String fileName = "TestFile.txt";
      if(ZipUtil.containsEntry(src, fileName)) {
        ZipUtil.removeEntry(src, fileName);
      }
      assertFalse(ZipUtil.containsEntry(src, fileName));
      InputStream is = null;
      try {
          is = new FileInputStream(file(fileName));
          byte[] newEntry = IOUtils.toByteArray(is);
          ZipUtil.addEntry(src, fileName, newEntry, compressionMethod);
          assertTrue(ZipUtil.containsEntry(src, fileName));

          assertEquals(compressionMethod, ZipUtil.getCompressionMethodOfEntry(src, fileName));
      } finally {
          IOUtils.closeQuietly(is);
      }
  }

  public void testReplaceEntryWithCompressionMethod() throws IOException {
    File initialSrc = file("demo.zip");
    File src = File.createTempFile("ztr", ".zip");
    FileUtils.copyFile(initialSrc, src);
    final String fileName = "foo.txt";
    assertTrue(ZipUtil.containsEntry(src, fileName));
    assertEquals(ZipEntry.STORED, ZipUtil.getCompressionMethodOfEntry(src, fileName));
    byte[] content = "testReplaceEntryWithCompressionMethod".getBytes("UTF-8");
    ZipUtil.replaceEntry(src, fileName, content, ZipEntry.DEFLATED);
    assertEquals(ZipEntry.DEFLATED, ZipUtil.getCompressionMethodOfEntry(src, fileName));
    FileUtils.forceDelete(src);
  }

  public void testUnpackBackslashes() throws IOException {
    File initialSrc = file("backSlashTest.zip");

    // lets create a temporary file and then use it as a dir
    File dest = File.createTempFile("unpackEntryDir", null);

    if(!(dest.delete())) {
        throw new IOException("Could not delete temp file: " + dest.getAbsolutePath());
    }

    if(!(dest.mkdir())){
        throw new IOException("Could not create temp directory: " + dest.getAbsolutePath());
    }

    // unpack the archive that has the backslashes
    // and double check that the file structure is preserved
    ZipUtil.iterate(initialSrc, new ZipUtil.BackslashUnpacker(dest));

    File parentDir = new File(dest, "testDirectory");
    assertTrue("Sub directory 'testDirectory' wasn't created", parentDir.isDirectory());

    File file = new File(parentDir, "testfileInTestDirectory.txt");
    assertTrue("Can't find file 'testfileInTestDirectory.txt' in testDirectory", file.isFile());

    file = new File(parentDir, "testSubdirectory");
    assertTrue("The sub sub directory 'testSubdirectory' isn't a directory", file.isDirectory());

    file = new File(file, "testFileInTestSubdirectory.txt");
    assertTrue("The 'testFileInTestSubdirectory.txt' is not a file", file.isFile());
  }
}
