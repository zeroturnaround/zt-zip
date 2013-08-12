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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.zeroturnaround.zip.FileSource;
import org.zeroturnaround.zip.ZipEntryCallback;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.Zips;
import org.zeroturnaround.zip.transform.ByteArrayZipEntryTransformer;
import org.zeroturnaround.zip.transform.ZipEntryTransformer;

public class ZipsTest extends TestCase {

  public void testDuplicateEntryAtAdd() throws IOException {
    File src = new File("src/test/resources/duplicate.zip");

    File dest = File.createTempFile("temp", ".zip");
    try {
      Zips.process(src).addEntries(new ZipEntrySource[0]).destination(dest).process();
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

    Zips.process(src).addEntry(new FileSource(fileName, newEntry)).destination(dest).process();
    assertTrue(ZipUtil.containsEntry(dest, fileName));
  }

  public void testRemoveEntry() throws IOException {
    File src = new File(MainExamplesTest.DEMO_ZIP);

    File dest = File.createTempFile("temp", ".zip");
    try {
      Zips.process(src).removeEntry("bar.txt").destination(dest).process();
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

      Zips.process(src).removeEntries(new String[] { "bar.txt", "a/b" }).destination(dest).process();

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
      Zips.process(src).addEntry(new FileSource(fileName, newEntry)).removeEntries(new String[] { "bar.txt", "a/b" }).destination(dest).process();

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

      Zips.process(dest).addEntry(new FileSource(fileName, newEntry)).process();
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
      Zips.process(workFile).addEntry(new FileSource(fileName, newEntry)).removeEntries(new String[] { "bar.txt", "a/b" }).process();
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

  public void testPreservingTimestamps() throws IOException {
    File src = new File(MainExamplesTest.DEMO_ZIP);

    File dest = File.createTempFile("temp", ".zip");
    final ZipFile zf = new ZipFile(src);
    try {
      Zips.process(src).addEntries(new ZipEntrySource[0]).preserveTimestamps().destination(dest).process();
      Zips.process(dest).iterate(new ZipEntryCallback() {
        public void process(InputStream in, ZipEntry zipEntry) throws IOException {
          String name = zipEntry.getName();
          assertEquals("Timestapms differ at entry " + name, zf.getEntry(name).getTime(), zipEntry.getTime());
        }
      });
    }
    finally {
      ZipUtil.closeQuietly(zf);
      FileUtils.deleteQuietly(dest);
    }
  }

  public void testOverwritingTimestamps() throws IOException {
    File src = new File(MainExamplesTest.DEMO_ZIP);

    File dest = File.createTempFile("temp", ".zip");
    final ZipFile zf = new ZipFile(src);
    try {
      Zips.process(src).addEntries(new ZipEntrySource[0]).destination(dest).process();
      Zips.process(dest).iterate(new ZipEntryCallback() {
        public void process(InputStream in, ZipEntry zipEntry) throws IOException {
          String name = zipEntry.getName();
          // original timestamp is believed to be earlier than test execution time.
          assertTrue("Timestapms were carried over for entry " + name, zf.getEntry(name).getTime() < zipEntry.getTime());
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
      Zips.process(src).addEntry(new FileSource(filename, newEntry)).removeEntry(filename).destination(dest).process();
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
    File src = new File(MainExamplesTest.DEMO_ZIP);
    final String fileName = "TestFile.txt";
    assertFalse(ZipUtil.containsEntry(src, fileName));

    File newEntry = new File("src/test/resources/TestFile.txt");
    File dest = File.createTempFile("temp.zip", ".zip");

    Charset charset = Charset.forName("UTF-8");
    String entryName = "中文.txt";
    try {
      Zips.process(src).charset(charset).addEntry(new FileSource(entryName, newEntry)).destination(dest).process();
    }
    catch (IllegalArgumentException e) {
      if (e.getMessage().equals("Using constructor ZipFile(File, Charset) has failed") ||
          e.getMessage().equals("Using constructor ZipOutputStream(OutputStream, Charset) has failed")) {
        // this is acceptable when old java doesn't have charset constructor
        return;
      }
      else {
        System.out.println("'" + e.getMessage() + "'");
      }
    }

    ZipFile zf = null;
    try {
      zf = Zips.getZipFile(dest, charset);
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

    Zips.process(src).iterate(new ZipEntryCallback() {
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
    Zips.get().destination(dest).addFile(fileToPack).process();
    assertTrue(dest.exists());
    ZipUtil.explode(dest);
    assertTrue((new File(dest, "TestFile.txt")).exists());
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(108, (new File(dest, "TestFile.txt")).length());
  }

  public void testPackEntries() throws Exception {
    File fileToPack = new File("src/test/resources/TestFile.txt");
    File fileToPackII = new File("src/test/resources/TestFile-II.txt");
    File dest = File.createTempFile("temp", ".zip");
    Zips.get().destination(dest).addFile(fileToPack).addFile(fileToPackII).process();

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
    File parent = new File("src/test/resources/TestFile.txt").getParentFile();
    // System.out.println("Parent file is " + parent);
    Zips.get().destination(dest).addFile(parent, true).process();
    ZipUtil.explode(dest);
    assertTrue("Root dir is not preserved", (new File(dest, parent.getName())).exists());
  }
  
  public void testPreserveRootWithSubdirectories() throws Exception {
	    File dest = File.createTempFile("temp", ".zip");
	    File parent = new File("src/test/resources/testDirectory");
	    // System.out.println("Parent file is " + parent);
	    Zips.get().destination(dest).addFile(parent, true).process();
	    assertTrue("File in subdirectory at specified path not found.",ZipUtil.containsEntry(dest, "testDirectory/testSubdirectory/testFileInTestSubdirectory.txt"));
  }

  public void testIgnoringRoot() throws Exception {
    File dest = File.createTempFile("temp", ".zip");
    File parent = new File("src/test/resources/TestFile.txt").getParentFile();
    // System.out.println("Parent file is " + parent);
    Zips.get().destination(dest).addFile(parent).process();
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
      Zips.process(source).destination(destination).addTransformer(name, transformer).transform();

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
      Zips.process(source).destination(destination).preserveTimestamps().addTransformer(name, transformer).transform();

      final ZipFile zf = new ZipFile(source);
      try {
        Zips.process(destination).iterate(new ZipEntryCallback() {
          public void process(InputStream in, ZipEntry zipEntry) throws IOException {
            String name = zipEntry.getName();
            assertEquals("Timestapms differ at entry " + name, zf.getEntry(name).getTime(), zipEntry.getTime());
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
}
