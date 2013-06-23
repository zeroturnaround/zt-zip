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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.zeroturnaround.zip.FileSource;
import org.zeroturnaround.zip.ZipEntryCallback;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.Zips;

public class ZipsTest extends TestCase {

  public void testDuplicateEntryAtAdd() throws IOException {
    File src = new File("src/test/resources/duplicate.zip");

    File dest = File.createTempFile("temp", ".zip");
    try {
      Zips.process(src).addEntries(new ZipEntrySource[0]).destination(dest).execute();
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

    Zips.process(src).addEntry(new FileSource(fileName, newEntry)).destination(dest).execute();
    assertTrue(ZipUtil.containsEntry(dest, fileName));
  }

  public void testRemoveEntry() throws IOException {
    File src = new File(MainExamplesTest.DEMO_ZIP);

    File dest = File.createTempFile("temp", ".zip");
    try {
      Zips.process(src).removeEntry("bar.txt").destination(dest).execute();
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

      Zips.process(src).removeEntries(new String[] { "bar.txt", "a/b" }).destination(dest).execute();

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
      Zips.process(src).addEntry(new FileSource(fileName, newEntry)).removeEntries(new String[] { "bar.txt", "a/b" }).destination(dest).execute();

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

      Zips.process(dest).addEntry(new FileSource(fileName, newEntry)).execute();
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
      Zips.process(workFile).addEntry(new FileSource(fileName, newEntry)).removeEntries(new String[] { "bar.txt", "a/b" }).execute();
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
      Zips.process(src).addEntries(new ZipEntrySource[0]).preserveTimestamps().destination(dest).execute();
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
      Zips.process(src).addEntries(new ZipEntrySource[0]).destination(dest).execute();
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
      Zips.process(src).addEntry(new FileSource(filename, newEntry)).removeEntry(filename).destination(dest).execute();
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
      Zips.process(src).charset(charset).addEntry(new FileSource(entryName, newEntry)).destination(dest).execute();
    }
    catch (IllegalArgumentException e) {
      if (e.getMessage().equals("Using constructor ZipFile(File, Charset) has failed")) {
        // this is acceptable if java doesn't have charset constructor
        return;
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
    String path = "src/test/resources/TestFile.txt";
    File fileToPack = new File(path);
    File dest = File.createTempFile("temp", null);
    Zips.get().destination(dest).addFile(fileToPack).execute();
    assertTrue(dest.exists());
    ZipUtil.explode(dest);
    assertTrue((new File(dest, path)).exists());
    // if fails then maybe somebody changed the file contents and did not update
    // the test
    assertEquals(108, (new File(dest, path)).length());
  }

  public void testPreserveRoot() throws Exception {
    File dest = File.createTempFile("temp", null);
    File parent = new File("src/test/resources/TestFile.txt").getParentFile();
    // System.out.println("Parent file is " + parent);
    Zips.get().destination(dest).addFile(parent, true).execute();
    ZipUtil.explode(dest);
    assertTrue("Root dir is not preserved", (new File(dest, parent.getName())).exists());
  }

  public void testIgnoringRoot() throws Exception {
    File dest = File.createTempFile("temp", null);
    File parent = new File("src/test/resources/TestFile.txt").getParentFile();
    // System.out.println("Parent file is " + parent);
    Zips.get().destination(dest).addFile(parent).execute();
    ZipUtil.explode(dest);
    assertFalse("Root dir is preserved", (new File(dest, parent.getName())).exists());
    assertTrue("Child file is missing", (new File(dest, "TestFile.txt")).exists());
  }

}
