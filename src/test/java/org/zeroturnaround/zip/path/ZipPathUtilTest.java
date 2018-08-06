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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.NameMapper;
import org.zeroturnaround.zip.PathSource;
import org.zeroturnaround.zip.ZipBreakException;
import org.zeroturnaround.zip.ZipEntryCallback;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipException;
import org.zeroturnaround.zip.ZipInfoCallback;
import org.zeroturnaround.zip.ZipPathUtil;
import org.zeroturnaround.zip.commons.IOUtils;
import org.zeroturnaround.zip.commons.PathUtils;

import junit.framework.TestCase;

/** @noinspection ResultOfMethodCallIgnored */
public class ZipPathUtilTest extends TestCase {

  /** @noinspection ConstantConditions */
  public static Path file(String name) {
    return Paths.get("src/test/resources/" + name);
  }

  public void testPackEntryStream() {
    Path src = file("TestFile.txt");
    byte[] bytes = ZipPathUtil.packEntry(src);
    boolean processed = ZipPathUtil.handle(new ByteArrayInputStream(bytes), "TestFile.txt", new ZipEntryCallback() {

      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
      }
    });
    assertTrue(processed);
  }

  public void testPackEntryFile() throws Exception {
    Path fileToPack = file("TestFile.txt");
    Path dest = Files.createTempFile("temp", null);
    ZipPathUtil.packEntry(fileToPack, dest);
    assertTrue(Files.exists(dest));

    ZipPathUtil.explode(dest);
    assertTrue(Files.exists(dest.resolve("TestFile.txt")));
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(108, Files.size(dest.resolve("TestFile.txt")));
  }

  public void testPackEntryFileWithNameParameter() throws Exception {
    Path fileToPack = file("TestFile.txt");
    Path dest = Files.createTempFile("temp", null);
    ZipPathUtil.packEntry(fileToPack, dest, "TestFile-II.txt");
    assertTrue(Files.exists(dest));

    ZipPathUtil.explode(dest);
    assertTrue(Files.exists(dest.resolve("TestFile-II.txt")));
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(108, Files.size(dest.resolve("TestFile-II.txt")));
  }

  public void testPackEntryFileWithNameMapper() throws Exception {
    Path fileToPack = file("TestFile.txt");
    Path dest = Files.createTempFile("temp", null);
    ZipPathUtil.packEntry(fileToPack, dest, new NameMapper() {
      public String map(String name) {
        return "TestFile-II.txt";
      }
    });
    assertTrue(Files.exists(dest));

    ZipPathUtil.explode(dest);
    assertTrue(Files.exists(dest.resolve("TestFile-II.txt")));
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(108, Files.size(dest.resolve("TestFile-II.txt")));
  }

  public void testUnpackEntryFromFile() throws IOException {
    final String name = "foo";
    final byte[] contents = "bar".getBytes();

    Path file = Files.createTempFile("temp", null);
    try {
      // Create the ZIP file
      ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(file));
      try {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(contents);
        zos.closeEntry();
      }
      finally {
        IOUtils.closeQuietly(zos);
      }

      // Test the ZipPathUtil
      byte[] actual = ZipPathUtil.unpackEntry(file, name);
      assertNotNull(actual);
      assertEquals(new String(contents), new String(actual));
    }
    finally {
      Files.deleteIfExists(file);
    }
  }

  public void testUnpackEntryFromStreamToFile() throws IOException {
    final String name = "foo";
    final byte[] contents = "bar".getBytes();

    Path file = Files.createTempFile("temp", null);
    try {
      // Create the ZIP file
      ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(file));
      try {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(contents);
        zos.closeEntry();
      }
      finally {
        IOUtils.closeQuietly(zos);
      }

      InputStream fis = Files.newInputStream(file);

      Path outputFile = Files.createTempFile("temp-output", null);

      boolean result = ZipPathUtil.unpackEntry(fis, name, outputFile);
      assertTrue(result);

      BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(outputFile));
      byte[] actual = new byte[1024];
      int read = bis.read(actual);
      bis.close();

      assertEquals(new String(contents), new String(actual, 0, read));
    }
    finally {
      Files.deleteIfExists(file);
    }
  }

  public void testUnpackEntryFromStream() throws IOException {
    final String name = "foo";
    final byte[] contents = "bar".getBytes();

    Path file = Files.createTempFile("temp", null);
    try {
      // Create the ZIP file
      ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(file));
      try {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(contents);
        zos.closeEntry();
      }
      finally {
        IOUtils.closeQuietly(zos);
      }

      InputStream fis = Files.newInputStream(file);
      // Test the ZipPathUtil
      byte[] actual = ZipPathUtil.unpackEntry(fis, name);
      assertNotNull(actual);
      assertEquals(new String(contents), new String(actual));
    }
    finally {
      Files.deleteIfExists(file);
    }
  }

  public void testDuplicateEntryAtAdd() throws IOException {
    Path src = file("duplicate.zip");

    Path dest = Files.createTempFile("temp", null);
    try {
      ZipPathUtil.addEntries(src, new ZipEntrySource[0], dest);
    }
    finally {
      Files.deleteIfExists(dest);
    }
  }

  public void testDuplicateEntryAtReplace() throws IOException {
    Path src = file("duplicate.zip");

    Path dest = Files.createTempFile("temp", null);
    try {
      ZipPathUtil.replaceEntries(src, new ZipEntrySource[0], dest);
    }
    finally {
      Files.deleteIfExists(dest);
    }
  }

  public void testDuplicateEntryAtAddOrReplace() throws IOException {
    Path src = file("duplicate.zip");

    Path dest = Files.createTempFile("temp", null);
    try {
      ZipPathUtil.addOrReplaceEntries(src, new ZipEntrySource[0], dest);
    }
    finally {
      Files.deleteIfExists(dest);
    }
  }

  public void testUnexplode() throws IOException {
    Path tmpDir = Files.createTempDirectory("tmpDir");
    Path file = tmpDir.resolve("not.a.dir");
    Files.write(file, "data".getBytes());

    unexplodeWithException(file, "shouldn't be able to unexplode file that is not a directory");
    Files.delete(file);
    assertTrue("Should be able to delete tmp file", !Files.exists(file));
    unexplodeWithException(file, "shouldn't be able to unexplode file that doesn't exist");

    // create empty tmp dir with the same name as deleted file
    Path dir = tmpDir.resolve("emptyDir");
    Files.createDirectory(dir);
    assertTrue("Should be able to create directory", Files.isDirectory(dir));

    unexplodeWithException(dir, "shouldn't be able to unexplode dir that doesn't contain any files");

    // unexplode should succeed with at least one file in directory
    Files.write(dir.resolve("random.file"), "data".getBytes());
    ZipPathUtil.unexplode(dir);

    assertTrue("zip file should exist with the same name as the directory that was unexploded", Files.exists(dir));
    assertTrue("unexploding input directory should have produced zip file with the same name", !Files.isDirectory(dir));
    PathUtils.deleteDir(tmpDir);
    assertTrue("Should be able to delete zip that was created from directory", !Files.exists(tmpDir));
  }

  public void testPackEntriesWithCompressionLevel() throws Exception {
    long filesizeBestCompression = 0;
    long filesizeNoCompression = 0;

    ZipFile zf = null;
    Path dest = null;

    try {
      dest = Files.createTempFile("temp-stor", null);
      ZipPathUtil.packEntries(new Path[] { file("TestFile.txt"), file("TestFile-II.txt") }, dest, Deflater.BEST_COMPRESSION);
      zf = new ZipFile(dest.toFile());
      filesizeBestCompression = zf.getEntry("TestFile.txt").getCompressedSize();
    }
    finally {
      zf.close();
    }

    try {
      dest = Files.createTempFile("temp-stor", null);
      ZipPathUtil.packEntries(new Path[] { file("TestFile.txt"), file("TestFile-II.txt") }, dest, Deflater.NO_COMPRESSION);
      zf = new ZipFile(dest.toFile());
      filesizeNoCompression = zf.getEntry("TestFile.txt").getCompressedSize();
    }
    finally {
      zf.close();
    }

    assertTrue(filesizeNoCompression > 0);
    assertTrue(filesizeBestCompression > 0);
    assertTrue(filesizeNoCompression > filesizeBestCompression);
  }

  public void testPackEntries() throws Exception {
    Path fileToPack = file("TestFile.txt");
    Path fileToPackII = file("TestFile-II.txt");
    Path dest = Files.createTempFile("temp", null);
    ZipPathUtil.packEntries(new Path[] { fileToPack, fileToPackII }, dest);
    assertTrue(Files.exists(dest));

    ZipPathUtil.explode(dest);
    assertTrue(Files.exists(dest.resolve("TestFile.txt")));
    assertTrue(Files.exists(dest.resolve("TestFile-II.txt")));
    // if fails then maybe somebody changed the file contents and did not update the test
    assertEquals(108, Files.size(dest.resolve("TestFile.txt")));
    assertEquals(103, Files.size(dest.resolve("TestFile-II.txt")));
  }

  public void testPackEntriesToStream() throws Exception {
    String encoding = "UTF-8";
    // list of entries, each entry consists of entry name and entry contents
    List<String[]> entryDescriptions = Arrays.asList(new String[][] {
        new String[] { "foo.txt", "foo" },
        new String[] { "bar.txt", "bar" }
    });
    ByteArrayOutputStream out = null;
    out = new ByteArrayOutputStream();
    ZipPathUtil.pack(convertToEntries(entryDescriptions, encoding), out);

    byte[] zipBytes = out.toByteArray();
    assertEquals(244, zipBytes.length);
    assertEntries(entryDescriptions, zipBytes, encoding);
  }

  public void testAddEntriesToStream() throws Exception {
    String encoding = "UTF-8";
    // list of entries, each entry consists of entry name and entry contents
    List<String[]> entryDescriptions = Arrays.asList(new String[][] {
        new String[] { "foo.txt", "foo" },
        new String[] { "bar.txt", "bar" }
    });
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ZipPathUtil.pack(convertToEntries(entryDescriptions, encoding), out);
    byte[] zipBytes = out.toByteArray();
    List<String[]> entryDescriptions2 = Arrays.asList(new String[][] {
        new String[] { "foo2.txt", "foo2" },
        new String[] { "bar2.txt", "bar2" }
    });
    ByteArrayOutputStream out2 = new ByteArrayOutputStream();
    ZipPathUtil.addEntries(new ByteArrayInputStream(zipBytes), convertToEntries(entryDescriptions2, encoding), out2);
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
    ZipPathUtil.iterate(new ByteArrayInputStream(zipBytes), new ZipEntryCallback() {
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
    Path fileToPack = file("TestFile.txt");
    Path fileToPackII = file("TestFile-II.txt");
    Path dest = Files.createTempFile("temp", null);
    ZipPathUtil.packEntries(new Path[] { fileToPack, fileToPackII }, dest, new NameMapper() {
      public String map(String name) {
        return "Changed-" + name;
      }
    });
    assertTrue(Files.exists(dest));

    ZipPathUtil.explode(dest);
    assertTrue(Files.exists(dest.resolve("Changed-TestFile.txt")));
    assertTrue(Files.exists(dest.resolve("Changed-TestFile-II.txt")));
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(108, Files.size(dest.resolve("Changed-TestFile.txt")));
    assertEquals(103, Files.size(dest.resolve("Changed-TestFile-II.txt")));
  }

  public void testZipException() {
    boolean exceptionThrown = false;
    Path target = Paths.get("weeheha");
    try {
      ZipPathUtil.pack(Paths.get("nonExistent"), target);
    }
    catch (ZipException e) {
      exceptionThrown = true;
    }
    assertFalse("Target file is created when source does not exist", Files.exists(target));
    assertTrue(exceptionThrown);
  }

  public void testPackEntriesWithNamesList() throws Exception {
    Path fileToPack = file("TestFile.txt");
    Path fileToPackII = file("TestFile-II.txt");
    Path dest = Files.createTempFile("temp", null);

    ZipPathUtil.pack(
        PathSource.pair(
            new Path[] { fileToPack, fileToPackII },
            new String[] { "Changed-TestFile.txt", "Changed-TestFile-II.txt" }),
        dest);

    assertTrue(Files.exists(dest));

    ZipPathUtil.explode(dest);
    assertTrue(Files.exists((dest.resolve("Changed-TestFile.txt"))));
    assertTrue(Files.exists((dest.resolve("Changed-TestFile-II.txt"))));
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(108, Files.size(dest.resolve("Changed-TestFile.txt")));
    assertEquals(103, Files.size(dest.resolve("Changed-TestFile-II.txt")));
  }

  public void testPreserveRoot() throws Exception {
    Path dest = Files.createTempFile("temp", null);
    Path parent = file("TestFile.txt").getParent();
    ZipPathUtil.pack(parent, dest, true);
    ZipPathUtil.explode(dest);
    assertTrue(Files.exists(dest.resolve(parent.getFileName())));
  }

  private void unexplodeWithException(Path file, String message) {
    try {
      ZipPathUtil.unexplode(file);
    }
    catch (Exception e) {
      return;
    }
    fail(message);
  }

  public void testArchiveEquals() {
    Path src = file("demo.zip");
    // byte-by-byte copy
    Path src2 = file("demo-copy.zip");
    assertTrue(ZipPathUtil.archiveEquals(src, src2));

    // entry by entry copy
    Path src3 = file("demo-copy-II.zip");
    assertTrue(ZipPathUtil.archiveEquals(src, src3));
  }

  public void testRepackArchive() throws IOException {
    Path src = file("demo.zip");
    Path dest = Files.createTempFile("temp", null);
    ZipPathUtil.repack(src, dest, 1);
    assertTrue(ZipPathUtil.archiveEquals(src, dest));
  }

  public void testContainsAnyEntry() throws IOException {
    Path src = file("demo.zip");
    boolean exists = ZipPathUtil.containsAnyEntry(src, new String[] { "foo.txt", "bar.txt" });
    assertTrue(exists);

    exists = ZipPathUtil.containsAnyEntry(src, new String[] { "foo.txt", "does-not-exist.txt" });
    assertTrue(exists);

    exists = ZipPathUtil.containsAnyEntry(src, new String[] { "does-not-exist-I.txt", "does-not-exist-II.txt" });
    assertFalse(exists);
  }

  public void testAddEntry() throws IOException {
    Path initialSrc = file("demo.zip");

    Path src = Files.createTempFile("ztr", ".zip");
    Files.copy(initialSrc, src, StandardCopyOption.REPLACE_EXISTING);

    final String fileName = "TestFile.txt";
    if (ZipPathUtil.containsEntry(src, fileName)) {
      ZipPathUtil.removeEntry(src, fileName);
    }
    assertFalse(ZipPathUtil.containsEntry(src, fileName));
    Path newEntry = file(fileName);
    Path dest = Files.createTempFile("temp.zip", null);

    ZipPathUtil.addEntry(src, fileName, newEntry, dest);
    assertTrue(ZipPathUtil.containsEntry(dest, fileName));
    Files.deleteIfExists(src);
  }

  public void testKeepEntriesState() throws IOException {
    Path src = file("demo-keep-entries-state.zip");
    final String existingEntryName = "TestFile.txt";
    final String fileNameToAdd = "TestFile-II.txt";
    assertFalse(ZipPathUtil.containsEntry(src, fileNameToAdd));
    Path newEntry = file(fileNameToAdd);
    Path dest = Files.createTempFile("temp.zip", null);
    ZipPathUtil.addEntry(src, fileNameToAdd, newEntry, dest);

    ZipEntry srcEntry = new ZipFile(src.toFile()).getEntry(existingEntryName);
    ZipEntry destEntry = new ZipFile(dest.toFile()).getEntry(existingEntryName);
    assertTrue(srcEntry.getCompressedSize() == destEntry.getCompressedSize());
  }

  public void testRemoveEntry() throws IOException {
    Path src = file("demo.zip");

    Path dest = Files.createTempFile("temp", null);
    try {
      ZipPathUtil.removeEntry(src, "bar.txt", dest);
      assertTrue("Result zip misses entry 'foo.txt'", ZipPathUtil.containsEntry(dest, "foo.txt"));
      assertTrue("Result zip misses entry 'foo1.txt'", ZipPathUtil.containsEntry(dest, "foo1.txt"));
      assertTrue("Result zip misses entry 'foo2.txt'", ZipPathUtil.containsEntry(dest, "foo2.txt"));
      assertFalse("Result zip still contains 'bar.txt'", ZipPathUtil.containsEntry(dest, "bar.txt"));
    }
    finally {
      Files.deleteIfExists(dest);
    }
  }

  public void testRemoveMissingEntry() throws IOException {
    Path src = file("demo.zip");
    assertFalse("Source zip contains entry 'missing.txt'", ZipPathUtil.containsEntry(src, "missing.txt"));

    Path dest = Files.createTempFile("temp", null);
    try {
      ZipPathUtil.removeEntry(src, "missing.txt", dest);
    }
    finally {
      Files.deleteIfExists(dest);
    }
  }

  public void testRemoveDirs() throws IOException {
    Path src = file("demo-dirs.zip");

    Path dest = Files.createTempFile("temp", null);
    try {
      ZipPathUtil.removeEntries(src, new String[] { "bar.txt", "a/b" }, dest);

      assertFalse("Result zip still contains 'bar.txt'", ZipPathUtil.containsEntry(dest, "bar.txt"));
      assertFalse("Result zip still contains dir 'a/b'", ZipPathUtil.containsEntry(dest, "a/b"));
      assertTrue("Result doesn't contain 'attic'", ZipPathUtil.containsEntry(dest, "attic/treasure.txt"));
      assertTrue("Entry whose prefix is dir name is removed too: 'b.txt'", ZipPathUtil.containsEntry(dest, "a/b.txt"));
      assertFalse("Entry in a removed dir is still there: 'a/b/c.txt'", ZipPathUtil.containsEntry(dest, "a/b/c.txt"));

    }
    finally {
      Files.deleteIfExists(dest);
    }
  }

  public void testRemoveDirsOutputStream() throws IOException {
    Path src = file("demo-dirs.zip");

    Path dest = Files.createTempFile("temp", null);
    try (OutputStream out = Files.newOutputStream(dest);) {
      ZipPathUtil.removeEntries(src, new String[] { "bar.txt", "a/b" }, out);

      assertFalse("Result zip still contains 'bar.txt'", ZipPathUtil.containsEntry(dest, "bar.txt"));
      assertFalse("Result zip still contains dir 'a/b'", ZipPathUtil.containsEntry(dest, "a/b"));
      assertTrue("Result doesn't contain 'attic'", ZipPathUtil.containsEntry(dest, "attic/treasure.txt"));
      assertTrue("Entry whose prefix is dir name is removed too: 'b.txt'", ZipPathUtil.containsEntry(dest, "a/b.txt"));
      assertFalse("Entry in a removed dir is still there: 'a/b/c.txt'", ZipPathUtil.containsEntry(dest, "a/b/c.txt"));

    }
    finally {
      Files.deleteIfExists(dest);
    }
  }

  public void testHandle() {
    Path src = file("demo.zip");

    boolean entryFound = ZipPathUtil.handle(src, "foo.txt", new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        assertEquals("foo.txt", zipEntry.getName());
      }
    });
    assertTrue(entryFound);

    entryFound = ZipPathUtil.handle(src, "non-existent-file.txt", new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        throw new RuntimeException("This should not happen!");
      }
    });
    assertFalse(entryFound);
  }

  public void testIterate() {
    Path src = file("demo.zip");
    final Set<String> files = new HashSet<>();
    files.add("foo.txt");
    files.add("bar.txt");
    files.add("foo1.txt");
    files.add("foo2.txt");

    ZipPathUtil.iterate(src, new ZipInfoCallback() {

      public void process(ZipEntry zipEntry) throws IOException {
        files.remove(zipEntry.getName());
      }
    });
    assertEquals(0, files.size());
  }

  public void testIterateGivenEntriesZipInfoCallback() {
    Path src = file("demo.zip");
    final Set<String> files = new HashSet<>();
    files.add("foo.txt");
    files.add("bar.txt");
    files.add("foo1.txt");
    files.add("foo2.txt");

    ZipPathUtil.iterate(src, new String[] { "foo.txt", "foo1.txt", "foo2.txt" }, new ZipInfoCallback() {

      public void process(ZipEntry zipEntry) throws IOException {
        files.remove(zipEntry.getName());
      }
    });
    assertEquals(1, files.size());
    assertTrue("Wrong entry hasn't been iterated", files.contains("bar.txt"));
  }

  public void testIterateGivenEntriesZipEntryCallback() {
    Path src = file("demo.zip");
    final Set<String> files = new HashSet<>();
    files.add("foo.txt");
    files.add("bar.txt");
    files.add("foo1.txt");
    files.add("foo2.txt");

    ZipPathUtil.iterate(src, new String[] { "foo.txt", "foo1.txt", "foo2.txt" }, new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        files.remove(zipEntry.getName());
      }
    });
    assertEquals(1, files.size());
    assertTrue("Wrong entry hasn't been iterated", files.contains("bar.txt"));
  }

  public void testIterateGivenEntriesFromStream() throws IOException {
    Path src = file("demo.zip");
    final Set<String> files = new HashSet<>();
    files.add("foo.txt");
    files.add("bar.txt");
    files.add("foo1.txt");
    files.add("foo2.txt");

    try (InputStream inputStream = Files.newInputStream(src);) {
      ZipPathUtil.iterate(inputStream, new String[] { "foo.txt", "foo1.txt", "foo2.txt" }, new ZipEntryCallback() {
        public void process(InputStream in, ZipEntry zipEntry) throws IOException {
          files.remove(zipEntry.getName());
        }
      });
      assertEquals(1, files.size());
      assertTrue("Wrong entry hasn't been iterated", files.contains("bar.txt"));
    }
  }

  public void testIterateAndBreak() {
    Path src = file("demo.zip");
    final Set<String> files = new HashSet<>();
    files.add("foo.txt");
    files.add("bar.txt");
    files.add("foo1.txt");
    files.add("foo2.txt");

    ZipPathUtil.iterate(src, new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        files.remove(zipEntry.getName());
        throw new ZipBreakException();
      }
    });
    assertEquals(3, files.size());
  }

  public void testUnwrapFile() throws Exception {
    Path dest = Files.createTempFile("temp", null);
    Path destDir = Files.createTempDirectory("tempDir");
    try {
      String child = "TestFile.txt";
      Path parent = file(child).getParent();
      ZipPathUtil.pack(parent, dest, true);
      ZipPathUtil.unwrap(dest, destDir);
      assertTrue(Files.exists(destDir.resolve(child)));
    }
    finally {
      PathUtils.deleteDir(destDir);
    }
  }

  public void testUnwrapStream() throws Exception {
    Path dest = Files.createTempFile("temp", null);
    Path destDir = Files.createTempDirectory("tempDir");
    String child = "TestFile.txt";
    Path parent = file(child).getParent();
    InputStream is = null;
    try {
      ZipPathUtil.pack(parent, dest, true);
      is = Files.newInputStream(dest);
      ZipPathUtil.unwrap(is, destDir);
      assertTrue(Files.exists(destDir.resolve(child)));
    }
    finally {
      IOUtils.closeQuietly(is);
      PathUtils.deleteDir(destDir);
    }
  }

  public void testUnwrapEntriesInRoot() throws Exception {
    Path src = file("demo.zip");
    Path destDir = Files.createTempDirectory("tempDir");
    try {
      ZipPathUtil.unwrap(src, destDir);
      fail("expected a ZipException, unwrapping with multiple roots is not supported");
    }
    catch (ZipException e) {
      // this is normal outcome
    }
    finally {
      PathUtils.deleteDir(destDir);
    }
  }

  public void testUnwrapMultipleRoots() throws Exception {
    Path src = file("demo-dirs-only.zip");
    Path destDir = Files.createTempDirectory("tempDir");
    try {
      ZipPathUtil.unwrap(src, destDir);
      fail("expected a ZipException, unwrapping with multiple roots is not supported");
    }
    catch (ZipException e) {
      // this is normal outcome
    }
    finally {
      PathUtils.deleteDir(destDir);
    }
  }

  public void testUnwrapSingleRootWithStructure() throws Exception {
    Path src = file("demo-single-root-dir.zip");
    Path destDir = Files.createTempDirectory("tempDir");
    try {
      ZipPathUtil.unwrap(src, destDir);
      assertTrue(Files.exists(destDir.resolve("b.txt")));
      assertTrue(Files.exists(destDir.resolve("bad.txt")));
      assertTrue(Files.exists(destDir.resolve("b")));
      assertTrue(Files.exists(destDir.resolve("b").resolve("c.txt")));
    }
    finally {
      PathUtils.deleteDir(destDir);
    }
  }

  public void testUnwrapEmptyRootDir() throws Exception {
    Path src = file("demo-single-empty-root-dir.zip");
    Path destDir = Files.createTempDirectory("tempDir");
    try {
      ZipPathUtil.unwrap(src, destDir);
      assertTrue("Dest dir should be empty, root dir was shaved", Files.list(destDir).count() == 0);
    }
    finally {
      PathUtils.deleteDir(destDir);
    }
  }

  public void testUnpackEntryDir() throws Exception {
    Path src = file("demo-dirs.zip");
    Path dest = Files.createTempFile("unpackEntryDir", null);
    try {
      ZipPathUtil.unpackEntry(src, "a", dest);
      assertTrue("Couldn't unpackEntry of a directory entry from a zip!", Files.exists(dest));
      assertTrue("UnpackedEntry of a directory is not a dir!", Files.isDirectory(dest));
    }
    finally {
      PathUtils.deleteDir(dest);
    }
  }

  public void testAddEntryWithCompressionMethodAndDestFile() throws IOException {
    int compressionMethod = ZipEntry.STORED;
    doTestAddEntryWithCompressionMethodAndDestFile(compressionMethod);

    compressionMethod = ZipEntry.DEFLATED;
    doTestAddEntryWithCompressionMethodAndDestFile(compressionMethod);
  }

  private void doTestAddEntryWithCompressionMethodAndDestFile(int compressionMethod) throws IOException {
    Path src = file("demo.zip");
    final String fileName = "TestFile.txt";
    if (ZipPathUtil.containsEntry(src, fileName)) {
      ZipPathUtil.removeEntry(src, fileName);
    }
    assertFalse(ZipPathUtil.containsEntry(src, fileName));
    InputStream is = null;
    try {
      is = Files.newInputStream(file(fileName));
      byte[] newEntry = IOUtils.toByteArray(is);
      Path dest = Files.createTempFile("temp.zip", null);
      ZipPathUtil.addEntry(src, fileName, newEntry, dest, compressionMethod);
      assertTrue(ZipPathUtil.containsEntry(dest, fileName));

      assertEquals(compressionMethod, ZipPathUtil.getCompressionMethodOfEntry(dest, fileName));
    }
    finally {
      IOUtils.closeQuietly(is);
    }
  }

  public void testAddEntryWithCompressionMethodStoredInPlace() throws IOException {
    int compressionMethod = ZipEntry.STORED;
    Path src = file("demo.zip");
    Path srcCopy = Files.createTempFile("ztr", ".zip");
    Files.copy(src, srcCopy, StandardCopyOption.REPLACE_EXISTING);
    doTestAddEntryWithCompressionMethodInPlace(srcCopy, compressionMethod);
    Files.deleteIfExists(srcCopy);
  }

  public void testAddEntryWithCompressionMethodDeflatedInPlace() throws IOException {
    int compressionMethod = ZipEntry.DEFLATED;
    Path src = file("demo.zip");
    Path srcCopy = Files.createTempFile("ztr", ".zip");
    Files.copy(src, srcCopy, StandardCopyOption.REPLACE_EXISTING);
    doTestAddEntryWithCompressionMethodInPlace(srcCopy, compressionMethod);
    Files.deleteIfExists(srcCopy);
  }

  private void doTestAddEntryWithCompressionMethodInPlace(Path src, int compressionMethod) throws IOException {
    final String fileName = "TestFile.txt";
    if (ZipPathUtil.containsEntry(src, fileName)) {
      ZipPathUtil.removeEntry(src, fileName);
    }
    assertFalse(ZipPathUtil.containsEntry(src, fileName));
    try (InputStream is = Files.newInputStream(file(fileName));) {
      byte[] newEntry = IOUtils.toByteArray(is);
      ZipPathUtil.addEntry(src, fileName, newEntry, compressionMethod);
      assertTrue(ZipPathUtil.containsEntry(src, fileName));

      assertEquals(compressionMethod, ZipPathUtil.getCompressionMethodOfEntry(src, fileName));
    }
  }

  public void testReplaceEntryWithCompressionMethod() throws IOException {
    Path initialSrc = file("demo.zip");
    Path src = Files.createTempFile("ztr", ".zip");
    Files.copy(initialSrc, src, StandardCopyOption.REPLACE_EXISTING);
    final String fileName = "foo.txt";
    assertTrue(ZipPathUtil.containsEntry(src, fileName));
    assertEquals(ZipEntry.STORED, ZipPathUtil.getCompressionMethodOfEntry(src, fileName));
    byte[] content = "testReplaceEntryWithCompressionMethod".getBytes("UTF-8");
    ZipPathUtil.replaceEntry(src, fileName, content, ZipEntry.DEFLATED);
    assertEquals(ZipEntry.DEFLATED, ZipPathUtil.getCompressionMethodOfEntry(src, fileName));
    Files.deleteIfExists(src);
  }

  public void testUnpackBackslashes() throws IOException {
    Path initialSrc = file("backSlashTest.zip");

    // lets create a temporary file and then use it as a dir
    Path dest = Files.createTempDirectory("unpackEntryDir");

    // unpack the archive that has the backslashes
    // and double check that the file structure is preserved
    ZipPathUtil.iterate(initialSrc, new ZipPathUtil.BackslashUnpacker(dest));

    Path parentDir = dest.resolve("testDirectory");
    assertTrue("Sub directory 'testDirectory' wasn't created", Files.isDirectory(parentDir));

    Path file = parentDir.resolve("testfileInTestDirectory.txt");
    assertTrue("Can't find file 'testfileInTestDirectory.txt' in testDirectory", Files.isRegularFile(file));

    file = parentDir.resolve("testSubdirectory");
    assertTrue("The sub sub directory 'testSubdirectory' isn't a directory", Files.isDirectory(file));

    file = file.resolve("testFileInTestSubdirectory.txt");
    assertTrue("The 'testFileInTestSubdirectory.txt' is not a file", Files.isRegularFile(file));
  }
}
