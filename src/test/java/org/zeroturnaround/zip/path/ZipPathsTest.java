package org.zeroturnaround.zip.path;
/**
 *    Copyright (C) 2018 ZeroTurnaround LLC <support@zeroturnaround.com>
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

import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.zeroturnaround.zip.MainExamplesTest;
import org.zeroturnaround.zip.NameMapper;
import org.zeroturnaround.zip.PathSource;
import org.zeroturnaround.zip.ZipBreakException;
import org.zeroturnaround.zip.ZipEntryCallback;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipInfoCallback;
import org.zeroturnaround.zip.ZipPathUtil;
import org.zeroturnaround.zip.ZipPaths;
import org.zeroturnaround.zip.commons.PathUtils;
import org.zeroturnaround.zip.transform.ByteArrayZipEntryTransformer;
import org.zeroturnaround.zip.transform.ZipEntryTransformer;

import junit.framework.TestCase;

public class ZipPathsTest extends TestCase {

  public void testDuplicateEntryAtAdd() throws IOException {
    Path src = Paths.get("src/test/resources/duplicate.zip");

    Path dest = Files.createTempFile("temp", ".zip");
    try {
      ZipPaths.get(src).addEntries(new ZipEntrySource[0]).destination(dest).process();
    }
    finally {
      Files.deleteIfExists(dest);
    }
  }

  public void testAddEntry() throws IOException {
    Path src = Paths.get(MainExamplesTest.DEMO_ZIP);
    final String fileName = "TestFile.txt";
    assertFalse(ZipPathUtil.containsEntry(src, fileName));

    Path newEntry = Paths.get("src/test/resources/" + fileName);
    Path dest = Files.createTempFile("temp.zip", ".zip");

    ZipPaths.get(src).addEntry(new PathSource(fileName, newEntry)).destination(dest).process();
    assertTrue(ZipPathUtil.containsEntry(dest, fileName));
  }

  public void testRemoveEntry() throws IOException {
    Path src = Paths.get(MainExamplesTest.DEMO_ZIP);

    Path dest = Files.createTempFile("temp", ".zip");
    try {
      ZipPaths.get(src).removeEntry("bar.txt").destination(dest).process();
      assertTrue("Result zip misses entry 'foo.txt'", ZipPathUtil.containsEntry(dest, "foo.txt"));
      assertTrue("Result zip misses entry 'foo1.txt'", ZipPathUtil.containsEntry(dest, "foo1.txt"));
      assertTrue("Result zip misses entry 'foo2.txt'", ZipPathUtil.containsEntry(dest, "foo2.txt"));
      assertFalse("Result zip still contains 'bar.txt'", ZipPathUtil.containsEntry(dest, "bar.txt"));
    }
    finally {
      Files.deleteIfExists(dest);
    }
  }

  public void testRemoveDirs() throws IOException {
    Path src = Paths.get("src/test/resources/demo-dirs.zip");

    Path dest = Files.createTempFile("temp", ".zip");
    try {
      ZipPaths.get(src).removeEntries(new String[] { "bar.txt", "a/b" }).destination(dest).process();

      assertFalse("Result zip still contains 'bar.txt'", ZipPathUtil.containsEntry(dest, "bar.txt"));
      assertFalse("Result zip still contains dir 'a/b'", ZipPathUtil.containsEntry(dest, "a/b"));
      assertTrue("Result doesn't containt 'attic'", ZipPathUtil.containsEntry(dest, "attic/treasure.txt"));
      assertTrue("Entry whose prefix is dir name is removed too: 'b.txt'", ZipPathUtil.containsEntry(dest, "a/b.txt"));
      assertFalse("Entry in a removed dir is still there: 'a/b/c.txt'", ZipPathUtil.containsEntry(dest, "a/b/c.txt"));

    }
    finally {
      Files.deleteIfExists(dest);
    }
  }

  public void testAddRemoveEntries() throws IOException {
    final String fileName = "TestFile.txt";
    Path newEntry = Paths.get("src/test/resources/" + fileName);

    Path src = Paths.get("src/test/resources/demo-dirs.zip");
    Path dest = Files.createTempFile("temp", ".zip");
    try {
      ZipPaths.get(src).addEntry(new PathSource(fileName, newEntry)).removeEntries(new String[] { "bar.txt", "a/b" }).destination(dest).process();

      assertFalse("Result zip still contains 'bar.txt'", ZipPathUtil.containsEntry(dest, "bar.txt"));
      assertFalse("Result zip still contains dir 'a/b'", ZipPathUtil.containsEntry(dest, "a/b"));
      assertTrue("Result doesn't containt 'attic'", ZipPathUtil.containsEntry(dest, "attic/treasure.txt"));
      assertTrue("Entry whose prefix is dir name is removed too: 'b.txt'", ZipPathUtil.containsEntry(dest, "a/b.txt"));
      assertFalse("Entry in a removed dir is still there: 'a/b/c.txt'", ZipPathUtil.containsEntry(dest, "a/b/c.txt"));
      assertTrue("Result doesn't contain added entry", ZipPathUtil.containsEntry(dest, fileName));

    }
    finally {
      Files.deleteIfExists(dest);
    }
  }

  public void testInPlaceAddEntry() throws IOException {
    Path src = Paths.get(MainExamplesTest.DEMO_ZIP);
    Path dest = Files.createTempFile("temp.zip", ".zip");
    try {
      Files.deleteIfExists(dest);
      Files.copy(src, dest);

      final String fileName = "TestFile.txt";
      assertFalse(ZipPathUtil.containsEntry(dest, fileName));
      Path newEntry = Paths.get("src/test/resources/" + fileName);

      ZipPaths.get(dest).addEntry(new PathSource(fileName, newEntry)).process();
      assertTrue(ZipPathUtil.containsEntry(dest, fileName));
    }
    finally {
      Files.deleteIfExists(dest);
    }
  }

  public void testInPlaceAddRemoveEntries() throws IOException {
    final String fileName = "TestFile.txt";
    Path newEntry = Paths.get("src/test/resources/" + fileName);

    Path original = Paths.get("src/test/resources/demo-dirs.zip");
    Path workFile = Files.createTempFile("temp", ".zip");
    try {
      Files.copy(original, workFile, StandardCopyOption.REPLACE_EXISTING);
      ZipPaths.get(workFile).addEntry(new PathSource(fileName, newEntry)).removeEntries(new String[] { "bar.txt", "a/b" }).process();
      assertFalse("Result zip still contains 'bar.txt'", ZipPathUtil.containsEntry(workFile, "bar.txt"));
      assertFalse("Result zip still contains dir 'a/b'", ZipPathUtil.containsEntry(workFile, "a/b"));
      assertTrue("Result doesn't containt 'attic'", ZipPathUtil.containsEntry(workFile, "attic/treasure.txt"));
      assertTrue("Entry whose prefix is dir name is removed too: 'b.txt'", ZipPathUtil.containsEntry(workFile, "a/b.txt"));
      assertFalse("Entry in a removed dir is still there: 'a/b/c.txt'", ZipPathUtil.containsEntry(workFile, "a/b/c.txt"));
      assertTrue("Result doesn't contain added entry", ZipPathUtil.containsEntry(workFile, fileName));

    }
    finally {
      Files.deleteIfExists(workFile);
    }
  }

  public void testOverwritingTimestamps() throws IOException {
    Path src = Paths.get(MainExamplesTest.DEMO_ZIP);

    Path dest = Files.createTempFile("temp", ".zip");
    final ZipFile zf = new ZipFile(src.toFile());
    try {
      ZipPaths.get(src).addEntries(new ZipEntrySource[0]).destination(dest).process();
      ZipPaths.get(dest).iterate(new ZipEntryCallback() {
        public void process(InputStream in, ZipEntry zipEntry) throws IOException {
          String name = zipEntry.getName();
          // original timestamp is believed to be earlier than test execution time.
          assertTrue("Timestamps were carried over for entry " + name, zf.getEntry(name).getTime() < zipEntry.getTime());
        }
      });
    }
    finally {
      ZipPathUtil.closeQuietly(zf);
      Files.deleteIfExists(dest);
    }
  }

  public void testAddRemovePriorities() throws IOException {
    Path src = Paths.get(MainExamplesTest.DEMO_ZIP);
    String filename = "bar.txt";
    Path newEntry = Paths.get("src/test/resources/TestFile.txt");

    Path dest = Files.createTempFile("temp", ".zip");
    try {
      ZipPaths.get(src).addEntry(new PathSource(filename, newEntry)).removeEntry(filename).destination(dest).process();
      assertTrue("Result zip misses entry 'foo.txt'", ZipPathUtil.containsEntry(dest, "foo.txt"));
      assertTrue("Result zip misses entry 'foo1.txt'", ZipPathUtil.containsEntry(dest, "foo1.txt"));
      assertTrue("Result zip misses entry 'foo2.txt'", ZipPathUtil.containsEntry(dest, "foo2.txt"));
      assertTrue("Result doesn't contain " + filename, ZipPathUtil.containsEntry(dest, filename));
      assertFalse(filename + " entry did not change", ZipPathUtil.entryEquals(src, dest, filename));
    }
    finally {
      Files.deleteIfExists(dest);
    }
  }

  public void testCharsetEntry() throws IOException {
    Path src = Paths.get(MainExamplesTest.DEMO_ZIP);
    final String fileName = "TestFile.txt";
    assertFalse(ZipPathUtil.containsEntry(src, fileName));

    Path newEntry = Paths.get("src/test/resources/TestFile.txt");
    Path dest = Files.createTempFile("temp.zip", ".zip");

    Charset charset = Charset.forName("UTF-8");
    String entryName = "中文.txt";
    try {
      ZipPaths.get(src).charset(charset).addEntry(new PathSource(entryName, newEntry)).destination(dest).process();
    }
    catch (IllegalStateException e) {
      if (e.getMessage().startsWith("Using constructor ZipFile(File, Charset) has failed") ||
          e.getMessage().startsWith("Using constructor ZipOutputStream(OutputStream, Charset) has failed")) {
        // this is acceptable when old java doesn't have charset constructor
        return;
      }
      else {
        System.out.println("'" + e.getMessage() + "'");
      }
    }

    ZipFile zf = null;
    try {
      zf = new ZipFile(dest.toFile(), charset);
      assertNotNull("Entry '" + entryName + "' was not added", zf.getEntry(entryName));
    }
    finally {
      ZipPathUtil.closeQuietly(zf);
    }
  }

  public void testIterateAndBreak() {
    Path src = Paths.get("src/test/resources/demo.zip");
    final Set<String> files = new HashSet<String>();
    files.add("foo.txt");
    files.add("bar.txt");
    files.add("foo1.txt");
    files.add("foo2.txt");

    ZipPaths.get(src).iterate(new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        files.remove(zipEntry.getName());
        throw new ZipBreakException();
      }
    });
    assertEquals(3, files.size());
  }

  public void testAddEntryFile() throws Exception {
    Path fileToPack = Paths.get("src/test/resources/TestFile.txt");
    Path dest = Files.createTempFile("temp", ".zip");
    ZipPaths.create().destination(dest).addFile(fileToPack).process();
    assertTrue(Files.exists(dest));
    ZipPathUtil.explode(dest);
    assertTrue(Files.exists(dest.resolve("TestFile.txt")));
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(108, Files.size(dest.resolve("TestFile.txt")));
  }

  public void testAddEntryFilter() throws Exception {
    Path fileToPack = Paths.get("src/test/resources");
    Path dest = Files.createTempFile("temp", ".zip");
    FileFilter filter = new FileFilter() {
      public boolean accept(java.io.File pathname) {
        return pathname.getName().startsWith("TestFile");
      }
    };

    ZipPaths.create().destination(dest).addFile(fileToPack, filter).process();
    assertTrue(Files.exists(dest));
    assertTrue(ZipPathUtil.containsEntry(dest, "TestFile.txt"));
    assertTrue(ZipPathUtil.containsEntry(dest, "TestFile-II.txt"));
    assertFalse(ZipPathUtil.containsEntry(dest, "log4j.properties"));
  }

  public void testPackEntries() throws Exception {
    Path fileToPack = Paths.get("src/test/resources/TestFile.txt");
    Path fileToPackII = Paths.get("src/test/resources/TestFile-II.txt");
    Path dest = Files.createTempFile("temp", ".zip");
    ZipPaths.create().destination(dest).addFile(fileToPack).addFile(fileToPackII).process();

    assertTrue(Files.exists(dest));

    ZipPathUtil.explode(dest);
    assertTrue(Files.exists(dest.resolve("TestFile.txt")));
    assertTrue(Files.exists(dest.resolve("TestFile-II.txt")));
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(108, Files.size(dest.resolve("TestFile.txt")));
    assertEquals(103, Files.size(dest.resolve("TestFile-II.txt")));
  }

  public void testPreserveRoot() throws Exception {
    Path dest = Files.createTempFile("temp", ".zip");
    Path parent = Paths.get("src/test/resources");
    // System.out.println("Parent file is " + parent);
    ZipPaths.create().destination(dest).addFile(parent, true).process();
    ZipPathUtil.explode(dest);
    Path parentDir = dest.resolve(parent.getFileName());
    assertTrue("Root dir is not preserved", Files.exists(parentDir));
    assertTrue("File from the parent directory was not added", Files.exists(parentDir.resolve("TestFile.txt")));
  }

  public void testPreserveRootWithSubdirectories() throws Exception {
    Path dest = Files.createTempFile("temp", ".zip");
    Path parent = Paths.get("src/test/resources/testDirectory");
    ZipPaths.create().destination(dest).addFile(parent, true).process();
    String entryName = "testDirectory/testSubdirectory/testFileInTestSubdirectory.txt";
    assertContainsEntryWithSeparatorrs(dest, entryName, "/"); // this failed on windows
  }

  private void assertContainsEntryWithSeparatorrs(Path zip, String entryPath, String expectedSeparator) throws IOException {
    char expectedSeparatorChar = expectedSeparator.charAt(0);
    String osSpecificEntryPath = entryPath.replace('\\', expectedSeparatorChar).replace('/', expectedSeparatorChar);
    ZipFile zf = new ZipFile(zip.toFile());
    try {
      if (zf.getEntry(osSpecificEntryPath) == null) {
        char unexpectedSeparatorChar = expectedSeparatorChar == '/' ? '\\' : '/';
        String nonOsSpecificEntryPath = entryPath.replace('\\', unexpectedSeparatorChar).replace('/', unexpectedSeparatorChar);
        if (zf.getEntry(nonOsSpecificEntryPath) != null) {
          fail(zip.toAbsolutePath() + " is not packed using directory separator '" + expectedSeparatorChar + "', found entry '" + nonOsSpecificEntryPath + "', but not '" + osSpecificEntryPath + "'"); // used to fail with this message on windows
        }
        StringBuilder sb = new StringBuilder();
        Enumeration<? extends ZipEntry> entries = zf.entries();
        while (entries.hasMoreElements()) {
          ZipEntry ze = (ZipEntry) entries.nextElement();
          sb.append(ze.getName()).append(",");
        }
        fail(zip.toAbsolutePath() + " doesn't contain entry '" + entryPath + "', but found following entries: " + sb.toString());
      }
    }
    finally {
      zf.close();
    }
    assertTrue("Didn't find entry '" + entryPath + "' from " + zip.toAbsolutePath(), ZipPathUtil.containsEntry(zip, entryPath));
  }

  public void testIgnoringRoot() throws Exception {
    Path dest = Files.createTempFile("temp", ".zip");
    Path parent = Paths.get("src/test/resources/TestFile.txt").getParent();
    // System.out.println("Parent file is " + parent);
    ZipPaths.create().destination(dest).addFile(parent).process();
    ZipPathUtil.explode(dest);
    assertFalse("Root dir is preserved", Files.exists(dest.resolve(parent)));
    assertTrue("Child file is missing", Files.exists(dest.resolve("TestFile.txt")));
  }

  public void testByteArrayTransformer() throws IOException {
    final String name = "foo";
    final byte[] contents = "bar".getBytes();

    Path source = Files.createTempFile("temp", ".zip");
    Path destination = Files.createTempFile("temp", ".zip");
    try {
      // Create the ZIP file
      try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(source));) {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(contents);
        zos.closeEntry();
      }

      // Transform the ZIP file
      ZipEntryTransformer transformer = new ByteArrayZipEntryTransformer() {
        protected byte[] transform(ZipEntry zipEntry, byte[] input) throws IOException {
          String s = new String(input);
          assertEquals(new String(contents), s);
          return s.toUpperCase().getBytes();
        }
      };
      ZipPaths.get(source).destination(destination).addTransformer(name, transformer).process();

      // Test the ZipPathUtil
      byte[] actual = ZipPathUtil.unpackEntry(destination, name);
      assertNotNull(actual);
      assertEquals(new String(contents).toUpperCase(), new String(actual));
    }
    finally {
      Files.deleteIfExists(source);
      Files.deleteIfExists(destination);
    }
  }

  public void testTransformationPreservesTimestamps() throws IOException {
    final String name = "foo";
    final byte[] contents = "bar".getBytes();

    Path source = Files.createTempFile("temp", ".zip");
    Path destination = Files.createTempFile("temp", ".zip");
    try {
      // Create the ZIP file
      try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(source));){
        for (int i = 0; i < 2; i++) {
          // we need many entries, some are transformed, some just copied.
          ZipEntry e = new ZipEntry(name + (i == 0 ? "" : "" + i));
          // 5 seconds ago.
          e.setTime(System.currentTimeMillis() - 5000);
          zos.putNextEntry(e);
          zos.write(contents);
          zos.closeEntry();
        }
      }
      // Transform the ZIP file
      ZipEntryTransformer transformer = new ByteArrayZipEntryTransformer() {
        protected byte[] transform(ZipEntry zipEntry, byte[] input) throws IOException {
          String s = new String(input);
          assertEquals(new String(contents), s);
          return s.toUpperCase().getBytes();
        }

        protected boolean preserveTimestamps() {
          // transformed entries preserve timestamps thanks to this.
          return true;
        }
      };
      ZipPaths.get(source).destination(destination).preserveTimestamps().addTransformer(name, transformer).process();

      final ZipFile zf = new ZipFile(source.toFile());
      try {
        ZipPaths.get(destination).iterate(new ZipEntryCallback() {
          public void process(InputStream in, ZipEntry zipEntry) throws IOException {
            String name = zipEntry.getName();
            assertEquals("Timestamps differ at entry " + name, zf.getEntry(name).getTime(), zipEntry.getTime());
          }
        });
      }
      finally {
        ZipPathUtil.closeQuietly(zf);
      }
      // Test the ZipPathUtil
      byte[] actual = ZipPathUtil.unpackEntry(destination, name);
      assertNotNull(actual);
      assertEquals(new String(contents).toUpperCase(), new String(actual));
    }
    finally {
      Files.deleteIfExists(source);
      Files.deleteIfExists(destination);
    }

  }

  public void testTransformAddedEntries() throws IOException {
    final String fileName = "TestFile.txt";
    Path newEntry = Paths.get("src/test/resources/" + fileName);

    Path src = Paths.get("src/test/resources/demo-dirs.zip");
    Path dest = Files.createTempFile("temp", ".zip");
    try {
      ZipEntryTransformer transformer = new ByteArrayZipEntryTransformer() {
        protected byte[] transform(ZipEntry zipEntry, byte[] input) throws IOException {
          String s = new String(input);
          return s.toUpperCase().getBytes();
        }

        protected boolean preserveTimestamps() {
          // transformed entries preserve timestamps thanks to this.
          return true;
        }
      };

      ZipPaths.get(src).addEntry(new PathSource(fileName, newEntry)).addTransformer(fileName, transformer).destination(dest).process();

      assertTrue("Result doesn't containt 'attic'", ZipPathUtil.containsEntry(dest, "attic/treasure.txt"));
      assertTrue("Entry whose prefix is dir name is removed too: 'b.txt'", ZipPathUtil.containsEntry(dest, "a/b.txt"));
      assertTrue("Result doesn't contain added entry", ZipPathUtil.containsEntry(dest, fileName));

      boolean contentIsUpper = new String(ZipPathUtil.unpackEntry(dest, fileName)).startsWith("I'M A TEST FILE");
      assertTrue("Added entry is not transformed!", contentIsUpper);

    }
    finally {
      Files.deleteIfExists(dest);
    }
  }

  public void testPackWithPrefixNameMapper() throws IOException {
    Path fileToPack = Paths.get("src/test/resources/TestFile.txt");
    Path fileToPackII = Paths.get("src/test/resources/TestFile-II.txt");
    Path dest = Files.createTempFile("temp", ".zip");
    ZipPaths.create().destination(dest).nameMapper(new NameMapper() {
      public String map(String name) {
        return "doc/" + name;
      }
    }).addFile(fileToPack).addFile(fileToPackII).process();

    assertTrue(Files.exists(dest));

    ZipPathUtil.explode(dest);
    assertTrue(Files.exists(dest.resolve("doc").resolve("TestFile.txt")));
    assertTrue(Files.exists(dest.resolve("doc").resolve("TestFile-II.txt")));
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(108, Files.size(dest.resolve("doc").resolve("TestFile.txt")));
    assertEquals(103, Files.size(dest.resolve("doc").resolve("TestFile-II.txt")));
  }

  public void testPackWithSuffixOnlyNameMapper() throws IOException {
    Path fileToPack = Paths.get("src/test/resources/TestFile.txt");
    Path fileToPackII = Paths.get("src/test/resources/TestFile-II.txt");
    Path dest = Files.createTempFile("temp", ".zip");
    ZipPaths.create().destination(dest).nameMapper(new NameMapper() {
      public String map(String name) {
        return name.endsWith("I.txt") ? name : null;
      }
    }).addFile(fileToPack).addFile(fileToPackII).process();

    assertTrue(Files.exists(dest));

    ZipPathUtil.explode(dest);
    assertFalse(Files.exists(dest.resolve("TestFile.txt")));
    assertTrue(Files.exists(dest.resolve("TestFile-II.txt")));
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(103, Files.size(dest.resolve("TestFile-II.txt")));
  }

  public void testUnpack() throws IOException {
    Path src = Paths.get(MainExamplesTest.DEMO_ZIP);

    final Path dest = Files.createTempFile("temp", null);

    ZipPaths.get(src).unpack().destination(dest).process();
    assertTrue(Files.exists(dest));
    ZipPathUtil.iterate(src, new ZipInfoCallback() {
      public void process(ZipEntry zipEntry) throws IOException {
        assertTrue(Files.exists(dest.resolve(zipEntry.getName())));
      }
    });
  }

  public void testUnpackInPlace() throws IOException {
    Path original = Paths.get(MainExamplesTest.DEMO_ZIP);
    final Path src = Files.createTempFile("temp", null);
    Files.delete(src);
    Files.copy(original, src);
    ZipPaths.get(src).unpack().process();
    assertTrue(Files.isDirectory(src));
    ZipPathUtil.iterate(original, new ZipInfoCallback() {
      public void process(ZipEntry zipEntry) throws IOException {
        assertTrue(Files.exists(src.resolve(zipEntry.getName())));
      }
    });
  }

  public void testUnpackImplicit() throws IOException {
    Path original = Paths.get(MainExamplesTest.DEMO_ZIP);
    final Path dest = Files.createTempDirectory("temp");
    ZipPaths.get(original).destination(dest).process();
    assertTrue(Files.isDirectory(dest));
    ZipPathUtil.iterate(original, new ZipInfoCallback() {
      public void process(ZipEntry zipEntry) throws IOException {
        assertTrue(Files.exists(dest.resolve(zipEntry.getName())));
      }
    });
  }

  public void testUnpackWithTransofrmer() throws IOException {
    final String fileName = "TestFile.txt";
    Path newEntry = Paths.get("src/test/resources/" + fileName);

    Path src = Paths.get("src/test/resources/demo-dirs.zip");
    Path dest = Files.createTempFile("temp", ".zip");
    try {
      ZipEntryTransformer transformer = new ByteArrayZipEntryTransformer() {
        protected byte[] transform(ZipEntry zipEntry, byte[] input) throws IOException {
          String s = new String(input);
          byte[] result = s.toUpperCase().getBytes();
          return result;
        }

        protected boolean preserveTimestamps() {
          // transformed entries preserve timestamps thanks to this.
          return true;
        }
      };

      ZipPaths.get(src).unpack().addEntry(new PathSource(fileName, newEntry)).addTransformer(fileName, transformer).destination(dest).process();
      assertTrue(Files.isDirectory(dest));
      assertTrue("Result doesn't containt 'attic'", Files.exists(dest.resolve("attic/treasure.txt")));
      assertTrue("Result doesn't contain added entry", Files.exists(dest.resolve(fileName)));

      boolean contentIsUpper = Files.readAllLines(dest.resolve(fileName)).get(0).startsWith("I'M A TEST FILE");
      assertTrue("Added entry is not transformed!", contentIsUpper);
    }
    finally {
      PathUtils.deleteDir(dest);
    }
  }
}
