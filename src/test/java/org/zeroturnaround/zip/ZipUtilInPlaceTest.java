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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import junit.framework.TestCase;

import org.zeroturnaround.zip.commons.FileUtils;
import org.zeroturnaround.zip.commons.IOUtils;
import org.zeroturnaround.zip.transform.ByteArrayZipEntryTransformer;

public class ZipUtilInPlaceTest extends TestCase {

  /** @noinspection ConstantConditions*/
  private File file(String name) {
    return new File(getClass().getClassLoader().getResource(name).getPath());
  }

  public void testAddEntry() throws IOException {
    File src = file("demo.zip");
    File dest = File.createTempFile("temp.zip", null);
    try {
      FileUtils.copyFile(src, dest);

      final String fileName = "TestFile.txt";
      assertFalse(ZipUtil.containsEntry(dest, fileName));
      File newEntry = file(fileName);

      ZipUtil.addEntry(dest, fileName, newEntry);
      assertTrue(ZipUtil.containsEntry(dest, fileName));
    }
    finally {
      FileUtils.deleteQuietly(dest);
    }
  }

  public void testRemoveEntry() throws IOException {
    File src = file("demo.zip");
    File dest = File.createTempFile("temp", null);
    try {
      FileUtils.copyFile(src, dest);
      assertTrue(ZipUtil.containsEntry(dest, "bar.txt"));
      ZipUtil.removeEntry(dest, "bar.txt");

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
    File src = file("demo-dirs.zip");

    File dest = File.createTempFile("temp", null);
    try {
      FileUtils.copyFile(src, dest);

      ZipUtil.removeEntries(dest, new String[] { "bar.txt", "a/b" });

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

  public void testByteArrayTransformer() throws IOException {
    final String name = "foo";
    final byte[] contents = "bar".getBytes();

    File file1 = File.createTempFile("temp", null);
    try {
      // Create the ZIP file
      ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file1));
      try {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(contents);
        zos.closeEntry();
      }
      finally {
        IOUtils.closeQuietly(zos);
      }

      // Transform the ZIP file
      ZipUtil.transformEntry(file1, name, new ByteArrayZipEntryTransformer() {
        protected byte[] transform(ZipEntry zipEntry, byte[] input) throws IOException {
          String s = new String(input);
          assertEquals(new String(contents), s);
          return s.toUpperCase().getBytes();
        }
      });

      // Test the ZipUtil
      byte[] actual = ZipUtil.unpackEntry(file1, name);
      assertNotNull(actual);
      assertEquals(new String(contents).toUpperCase(), new String(actual));
    }
    finally {
      FileUtils.deleteQuietly(file1);
    }
  }
}
