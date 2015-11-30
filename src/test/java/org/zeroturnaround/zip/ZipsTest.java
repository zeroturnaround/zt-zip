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
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.zeroturnaround.zip.commons.FileUtils;
import org.zeroturnaround.zip.commons.IOUtils;
import org.zeroturnaround.zip.transform.ByteArrayZipEntryTransformer;
import org.zeroturnaround.zip.transform.ZipEntryTransformer;

import junit.framework.TestCase;

public class ZipsTest extends TestCase {

  public void testDuplicateEntryAtAdd() throws IOException {
    File src = new File("src/test/resources/duplicate.zip");

    File dest = File.createTempFile("temp", ".zip");
    try {
      Zips.get(src).addEntries(new ZipEntrySource[0]).destination(dest).process();
    }
    finally {
      FileUtils.deleteQuietly(dest);
    }
  }

  public void testAddEntry() throws IOException {
    File src = new File(MainExamplesTest.DEMO_ZIP);
    final String fileName = "TestFile.txt";
    assertFalse(ZipUtil.containsEntry(src, fileName));

    File newEntry = new File("src/test/resources/" + fileName);
    File dest = File.createTempFile("temp.zip", ".zip");

    Zips.get(src).addEntry(new FileSource(fileName, newEntry)).destination(dest).process();
    assertTrue(ZipUtil.containsEntry(dest, fileName));
  }

  public void testRemoveEntry() throws IOException {
    File src = new File(MainExamplesTest.DEMO_ZIP);

    File dest = File.createTempFile("temp", ".zip");
    try {
      Zips.get(src).removeEntry("bar.txt").destination(dest).process();
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
    File src = new File("src/test/resources/demo-dirs.zip");

    File dest = File.createTempFile("temp", ".zip");
    try {
      Zips.get(src).removeEntries(new String[] { "bar.txt", "a/b" }).destination(dest).process();

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

  public void testAddRemoveEntries() throws IOException {
    final String fileName = "TestFile.txt";
    File newEntry = new File("src/test/resources/" + fileName);

    File src = new File("src/test/resources/demo-dirs.zip");
    File dest = File.createTempFile("temp", ".zip");
    try {
      Zips.get(src).addEntry(new FileSource(fileName, newEntry)).removeEntries(new String[] { "bar.txt", "a/b" }).destination(dest).process();

      assertFalse("Result zip still contains 'bar.txt'", ZipUtil.containsEntry(dest, "bar.txt"));
      assertFalse("Result zip still contains dir 'a/b'", ZipUtil.containsEntry(dest, "a/b"));
      assertTrue("Result doesn't containt 'attic'", ZipUtil.containsEntry(dest, "attic/treasure.txt"));
      assertTrue("Entry whose prefix is dir name is removed too: 'b.txt'", ZipUtil.containsEntry(dest, "a/b.txt"));
      assertFalse("Entry in a removed dir is still there: 'a/b/c.txt'", ZipUtil.containsEntry(dest, "a/b/c.txt"));
      assertTrue("Result doesn't contain added entry", ZipUtil.containsEntry(dest, fileName));

    }
    finally {
      FileUtils.deleteQuietly(dest);
    }
  }

  public void testInPlaceAddEntry() throws IOException {
    File src = new File(MainExamplesTest.DEMO_ZIP);
    File dest = File.createTempFile("temp.zip", ".zip");
    try {
      FileUtils.copyFile(src, dest);

      final String fileName = "TestFile.txt";
      assertFalse(ZipUtil.containsEntry(dest, fileName));
      File newEntry = new File("src/test/resources/" + fileName);

      Zips.get(dest).addEntry(new FileSource(fileName, newEntry)).process();
      assertTrue(ZipUtil.containsEntry(dest, fileName));
    }
    finally {
      FileUtils.deleteQuietly(dest);
    }
  }

  public void testInPlaceAddRemoveEntries() throws IOException {
    final String fileName = "TestFile.txt";
    File newEntry = new File("src/test/resources/" + fileName);

    File original = new File("src/test/resources/demo-dirs.zip");
    File workFile = File.createTempFile("temp", ".zip");
    try {
      FileUtils.copyFile(original, workFile);
      Zips.get(workFile).addEntry(new FileSource(fileName, newEntry)).removeEntries(new String[] { "bar.txt", "a/b" }).process();
      assertFalse("Result zip still contains 'bar.txt'", ZipUtil.containsEntry(workFile, "bar.txt"));
      assertFalse("Result zip still contains dir 'a/b'", ZipUtil.containsEntry(workFile, "a/b"));
      assertTrue("Result doesn't containt 'attic'", ZipUtil.containsEntry(workFile, "attic/treasure.txt"));
      assertTrue("Entry whose prefix is dir name is removed too: 'b.txt'", ZipUtil.containsEntry(workFile, "a/b.txt"));
      assertFalse("Entry in a removed dir is still there: 'a/b/c.txt'", ZipUtil.containsEntry(workFile, "a/b/c.txt"));
      assertTrue("Result doesn't contain added entry", ZipUtil.containsEntry(workFile, fileName));

    }
    finally {
      FileUtils.deleteQuietly(workFile);
    }
  }

  public void testOverwritingTimestamps() throws IOException {
    File src = new File(MainExamplesTest.DEMO_ZIP);

    File dest = File.createTempFile("temp", ".zip");
    final ZipFile zf = new ZipFile(src);
    try {
      Zips.get(src).addEntries(new ZipEntrySource[0]).destination(dest).process();
      Zips.get(dest).iterate(new ZipEntryCallback() {
        public void process(InputStream in, ZipEntry zipEntry) throws IOException {
          String name = zipEntry.getName();
          // original timestamp is believed to be earlier than test execution time.
          assertTrue("Timestamps were carried over for entry " + name, zf.getEntry(name).getTime() < zipEntry.getTime());
        }
      });
    }
    finally {
      ZipUtil.closeQuietly(zf);
      FileUtils.deleteQuietly(dest);
    }
  }

  public void testAddRemovePriorities() throws IOException {
    File src = new File(MainExamplesTest.DEMO_ZIP);
    String filename = "bar.txt";
    File newEntry = new File("src/test/resources/TestFile.txt");

    File dest = File.createTempFile("temp", ".zip");
    try {
      Zips.get(src).addEntry(new FileSource(filename, newEntry)).removeEntry(filename).destination(dest).process();
      assertTrue("Result zip misses entry 'foo.txt'", ZipUtil.containsEntry(dest, "foo.txt"));
      assertTrue("Result zip misses entry 'foo1.txt'", ZipUtil.containsEntry(dest, "foo1.txt"));
      assertTrue("Result zip misses entry 'foo2.txt'", ZipUtil.containsEntry(dest, "foo2.txt"));
      assertTrue("Result doesn't contain " + filename, ZipUtil.containsEntry(dest, filename));
      assertFalse(filename + " entry did not change", ZipUtil.entryEquals(src, dest, filename));
    }
    finally {
      FileUtils.deleteQuietly(dest);
    }
  }

  public void testCharsetEntry() throws IOException {
    if (!ZipFileUtil.isCharsetSupported()) {
      return; // skip
    }

    File src = new File(MainExamplesTest.DEMO_ZIP);
    final String fileName = "TestFile.txt";
    assertFalse(ZipUtil.containsEntry(src, fileName));

    File newEntry = new File("src/test/resources/TestFile.txt");
    File dest = File.createTempFile("temp.zip", ".zip");

    Charset charset = Charset.forName("UTF-8");
    String entryName = "中文.txt";
    try {
      Zips.get(src).charset(charset).addEntry(new FileSource(entryName, newEntry)).destination(dest).process();
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
      zf = ZipFileUtil.getZipFile(dest, charset);
      assertNotNull("Entry '" + entryName + "' was not added", zf.getEntry(entryName));
    }
    finally {
      ZipUtil.closeQuietly(zf);
    }
  }

  public void testIterateAndBreak() {
    File src = new File("src/test/resources/demo.zip");
    final Set files = new HashSet();
    files.add("foo.txt");
    files.add("bar.txt");
    files.add("foo1.txt");
    files.add("foo2.txt");

    Zips.get(src).iterate(new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        files.remove(zipEntry.getName());
        throw new ZipBreakException();
      }
    });
    assertEquals(3, files.size());
  }

  public void testAddEntryFile() throws Exception {
    File fileToPack = new File("src/test/resources/TestFile.txt");
    File dest = File.createTempFile("temp", ".zip");
    Zips.create().destination(dest).addFile(fileToPack).process();
    assertTrue(dest.exists());
    ZipUtil.explode(dest);
    assertTrue((new File(dest, "TestFile.txt")).exists());
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(108, (new File(dest, "TestFile.txt")).length());
  }

  public void testAddEntryFilter() throws Exception {
    File fileToPack = new File("src/test/resources");
    File dest = File.createTempFile("temp", ".zip");
    FileFilter filter = new FileFilter() {
      public boolean accept(File pathname) {
        return pathname.getName().startsWith("TestFile");
      }
    };

    Zips.create().destination(dest).addFile(fileToPack, filter).process();
    assertTrue(dest.exists());
    assertTrue(ZipUtil.containsEntry(dest, "TestFile.txt"));
    assertTrue(ZipUtil.containsEntry(dest, "TestFile-II.txt"));
    assertFalse(ZipUtil.containsEntry(dest, "log4j.properties"));
  }

  public void testPackEntries() throws Exception {
    File fileToPack = new File("src/test/resources/TestFile.txt");
    File fileToPackII = new File("src/test/resources/TestFile-II.txt");
    File dest = File.createTempFile("temp", ".zip");
    Zips.create().destination(dest).addFile(fileToPack).addFile(fileToPackII).process();

    assertTrue(dest.exists());

    ZipUtil.explode(dest);
    assertTrue((new File(dest, "TestFile.txt")).exists());
    assertTrue((new File(dest, "TestFile-II.txt")).exists());
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(108, (new File(dest, "TestFile.txt")).length());
    assertEquals(103, (new File(dest, "TestFile-II.txt")).length());
  }

  public void testPreserveRoot() throws Exception {
    File dest = File.createTempFile("temp", ".zip");
    File parent = new File("src/test/resources");
    // System.out.println("Parent file is " + parent);
    Zips.create().destination(dest).addFile(parent, true).process();
    ZipUtil.explode(dest);
    File parentDir = new File(dest, parent.getName());
    assertTrue("Root dir is not preserved", parentDir.exists());
    assertTrue("File from the parent directory was not added", new File(parentDir, "TestFile.txt").exists());
  }

  public void testPreserveRootWithSubdirectories() throws Exception {
	    File dest = File.createTempFile("temp", ".zip");
	    File parent = new File("src/test/resources/testDirectory");
    Zips.create().destination(dest).addFile(parent, true).process();
    String entryName = "testDirectory/testSubdirectory/testFileInTestSubdirectory.txt";
    assertContainsEntryWithSeparatorrs(dest, entryName, "/"); // this failed on windows
  }

  private void assertContainsEntryWithSeparatorrs(File zip, String entryPath, String expectedSeparator) throws IOException {
    char expectedSeparatorChar = expectedSeparator.charAt(0);
    String osSpecificEntryPath = entryPath.replace('\\', expectedSeparatorChar).replace('/', expectedSeparatorChar);
    ZipFile zf = new ZipFile(zip);
    try {
      if (zf.getEntry(osSpecificEntryPath) == null) {
        char unexpectedSeparatorChar = expectedSeparatorChar == '/' ? '\\' : '/';
        String nonOsSpecificEntryPath = entryPath.replace('\\', unexpectedSeparatorChar).replace('/', unexpectedSeparatorChar);
        if (zf.getEntry(nonOsSpecificEntryPath) != null) {
          fail(zip.getAbsolutePath() + " is not packed using directory separator '" + expectedSeparatorChar + "', found entry '" + nonOsSpecificEntryPath
              + "', but not '" + osSpecificEntryPath + "'"); // used to fail with this message on windows
        }
        StringBuilder sb = new StringBuilder();
        Enumeration entries = zf.entries();
        while (entries.hasMoreElements()) {
          ZipEntry ze = (ZipEntry) entries.nextElement();
          sb.append(ze.getName()).append(",");
        }
        fail(zip.getAbsolutePath() + " doesn't contain entry '" + entryPath + "', but found following entries: " + sb.toString());
      }
    }
    finally {
      zf.close();
    }
    assertTrue("Didn't find entry '" + entryPath + "' from " + zip.getAbsolutePath(), ZipUtil.containsEntry(zip, entryPath));
  }

  public void testIgnoringRoot() throws Exception {
    File dest = File.createTempFile("temp", ".zip");
    File parent = new File("src/test/resources/TestFile.txt").getParentFile();
    // System.out.println("Parent file is " + parent);
    Zips.create().destination(dest).addFile(parent).process();
    ZipUtil.explode(dest);
    assertFalse("Root dir is preserved", (new File(dest, parent.getName())).exists());
    assertTrue("Child file is missing", (new File(dest, "TestFile.txt")).exists());
  }

  public void testByteArrayTransformer() throws IOException {
    final String name = "foo";
    final byte[] contents = "bar".getBytes();

    File source = File.createTempFile("temp", ".zip");
    File destination = File.createTempFile("temp", ".zip");
    try {
      // Create the ZIP file
      ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(source));
      try {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(contents);
        zos.closeEntry();
      }
      finally {
        IOUtils.closeQuietly(zos);
      }

      // Transform the ZIP file
      ZipEntryTransformer transformer = new ByteArrayZipEntryTransformer() {
        protected byte[] transform(ZipEntry zipEntry, byte[] input) throws IOException {
          String s = new String(input);
          assertEquals(new String(contents), s);
          return s.toUpperCase().getBytes();
        }
      };
      Zips.get(source).destination(destination).addTransformer(name, transformer).process();

      // Test the ZipUtil
      byte[] actual = ZipUtil.unpackEntry(destination, name);
      assertNotNull(actual);
      assertEquals(new String(contents).toUpperCase(), new String(actual));
    }
    finally {
      FileUtils.deleteQuietly(source);
      FileUtils.deleteQuietly(destination);
    }
  }

  public void testTransformationPreservesTimestamps() throws IOException {
    final String name = "foo";
    final byte[] contents = "bar".getBytes();

    File source = File.createTempFile("temp", ".zip");
    File destination = File.createTempFile("temp", ".zip");
    try {
      // Create the ZIP file
      ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(source));
      try {
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
      finally {
        IOUtils.closeQuietly(zos);
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
      Zips.get(source).destination(destination).preserveTimestamps().addTransformer(name, transformer).process();

      final ZipFile zf = new ZipFile(source);
      try {
        Zips.get(destination).iterate(new ZipEntryCallback() {
          public void process(InputStream in, ZipEntry zipEntry) throws IOException {
            String name = zipEntry.getName();
            assertEquals("Timestamps differ at entry " + name, zf.getEntry(name).getTime(), zipEntry.getTime());
          }
        });
      }
      finally {
        ZipUtil.closeQuietly(zf);
      }
      // Test the ZipUtil
      byte[] actual = ZipUtil.unpackEntry(destination, name);
      assertNotNull(actual);
      assertEquals(new String(contents).toUpperCase(), new String(actual));
    }
    finally {
      FileUtils.deleteQuietly(source);
      FileUtils.deleteQuietly(destination);
    }

  }

  public void testTransformAddedEntries() throws IOException {
    final String fileName = "TestFile.txt";
    File newEntry = new File("src/test/resources/" + fileName);

    File src = new File("src/test/resources/demo-dirs.zip");
    File dest = File.createTempFile("temp", ".zip");
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

      Zips.get(src).addEntry(new FileSource(fileName, newEntry)).addTransformer(fileName, transformer).destination(dest).process();

      assertTrue("Result doesn't containt 'attic'", ZipUtil.containsEntry(dest, "attic/treasure.txt"));
      assertTrue("Entry whose prefix is dir name is removed too: 'b.txt'", ZipUtil.containsEntry(dest, "a/b.txt"));
      assertTrue("Result doesn't contain added entry", ZipUtil.containsEntry(dest, fileName));

      boolean contentIsUpper = new String(ZipUtil.unpackEntry(dest, fileName)).startsWith("I'M A TEST FILE");
      assertTrue("Added entry is not transformed!", contentIsUpper);

    }
    finally {
      FileUtils.deleteQuietly(dest);
    }
  }

  public void testPackWithPrefixNameMapper() throws IOException {
    File fileToPack = new File("src/test/resources/TestFile.txt");
    File fileToPackII = new File("src/test/resources/TestFile-II.txt");
    File dest = File.createTempFile("temp", ".zip");
    Zips.create().destination(dest).nameMapper(new NameMapper() {
      public String map(String name) {
        return "doc/" + name;
      }
    }).addFile(fileToPack).addFile(fileToPackII).process();

    assertTrue(dest.exists());

    ZipUtil.explode(dest);
    assertTrue((new File(new File(dest, "doc"), "TestFile.txt")).exists());
    assertTrue((new File(new File(dest, "doc"), "TestFile-II.txt")).exists());
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(108, (new File(new File(dest, "doc"), "TestFile.txt")).length());
    assertEquals(103, (new File(new File(dest, "doc"), "TestFile-II.txt")).length());
  }

  public void testPackWithSuffixOnlyNameMapper() throws IOException {
    File fileToPack = new File("src/test/resources/TestFile.txt");
    File fileToPackII = new File("src/test/resources/TestFile-II.txt");
    File dest = File.createTempFile("temp", ".zip");
    Zips.create().destination(dest).nameMapper(new NameMapper() {
      public String map(String name) {
        return name.endsWith("I.txt") ? name : null;
      }
    }).addFile(fileToPack).addFile(fileToPackII).process();

    assertTrue(dest.exists());

    ZipUtil.explode(dest);
    assertFalse((new File(dest, "TestFile.txt")).exists());
    assertTrue((new File(dest, "TestFile-II.txt")).exists());
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(103, (new File(dest, "TestFile-II.txt")).length());
  }

  public void testUnpack() throws IOException {
    File src = new File(MainExamplesTest.DEMO_ZIP);

    final File dest = File.createTempFile("temp", null);

    Zips.get(src).unpack().destination(dest).process();
    assertTrue(dest.exists());
    ZipUtil.iterate(src, new ZipInfoCallback() {
      public void process(ZipEntry zipEntry) throws IOException {
        assertTrue(new File(dest, zipEntry.getName()).exists());
      }
    });
  }

  public void testUnpackInPlace() throws IOException {
    File original = new File(MainExamplesTest.DEMO_ZIP);
    final File src = File.createTempFile("temp", null);
    FileUtils.copyFile(original, src);
    Zips.get(src).unpack().process();
    assertTrue(src.isDirectory());
    ZipUtil.iterate(original, new ZipInfoCallback() {
      public void process(ZipEntry zipEntry) throws IOException {
        assertTrue(new File(src, zipEntry.getName()).exists());
      }
    });
  }

  public void testUnpackImplicit() throws IOException {
    File original = new File(MainExamplesTest.DEMO_ZIP);
    final File dest = File.createTempFile("temp", null);
    FileUtils.deleteQuietly(dest);
    dest.mkdirs();
    Zips.get(original).destination(dest).process();
    assertTrue(dest.isDirectory());
    ZipUtil.iterate(original, new ZipInfoCallback() {
      public void process(ZipEntry zipEntry) throws IOException {
        assertTrue(new File(dest, zipEntry.getName()).exists());
      }
    });
  }

  public void testUnpackWithTransofrmer() throws IOException {
    final String fileName = "TestFile.txt";
    File newEntry = new File("src/test/resources/" + fileName);

    File src = new File("src/test/resources/demo-dirs.zip");
    File dest = File.createTempFile("temp", ".zip");
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

      Zips.get(src).unpack().addEntry(new FileSource(fileName, newEntry)).addTransformer(fileName, transformer).destination(dest).process();
      assertTrue(dest.isDirectory());
      assertTrue("Result doesn't containt 'attic'", new File(dest, "attic/treasure.txt").exists());
      assertTrue("Result doesn't contain added entry", new File(dest, fileName).exists());

      boolean contentIsUpper = new String(FileUtils.readFileToString(new File(dest, fileName))).startsWith("I'M A TEST FILE");
      assertTrue("Added entry is not transformed!", contentIsUpper);
    }
    finally {
      FileUtils.deleteQuietly(dest);
    }
  }
}
