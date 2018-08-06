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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.zeroturnaround.zip.ZipPathUtil;
import org.zeroturnaround.zip.commons.IOUtils;
import org.zeroturnaround.zip.transform.ByteArrayZipEntryTransformer;

import junit.framework.TestCase;

public class ZipPathUtilInPlaceTest extends TestCase {

  /** @noinspection ConstantConditions*/
  private Path file(String name) {
    return Paths.get("src/test/resources/" + name);
  }

  public void testAddEntry() throws IOException {
    Path src = file("demo.zip");
    Path dest = Files.createTempFile("temp.zip", null);
    try {
      Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);

      final String fileName = "TestFile.txt";
      assertFalse(ZipPathUtil.containsEntry(dest, fileName));
      Path newEntry = file(fileName);

      ZipPathUtil.addEntry(dest, fileName, newEntry);
      assertTrue(ZipPathUtil.containsEntry(dest, fileName));
    }
    finally {
      Files.deleteIfExists(dest);
    }
  }

  public void testRemoveEntry() throws IOException {
    Path src = file("demo.zip");
    Path dest = Files.createTempFile("temp", null);
    try {
      Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
      assertTrue(ZipPathUtil.containsEntry(dest, "bar.txt"));
      ZipPathUtil.removeEntry(dest, "bar.txt");

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
    Path src = file("demo-dirs.zip");

    Path dest = Files.createTempFile("temp", null);
    try {
      Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);

      ZipPathUtil.removeEntries(dest, new String[] { "bar.txt", "a/b" });

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

  public void testByteArrayTransformer() throws IOException {
    final String name = "foo";
    final byte[] contents = "bar".getBytes();

    Path file1 = Files.createTempFile("temp", null);
    try {
      // Create the ZIP file
      ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(file1));
      try {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(contents);
        zos.closeEntry();
      }
      finally {
        IOUtils.closeQuietly(zos);
      }

      // Transform the ZIP file
      ZipPathUtil.transformEntry(file1, name, new ByteArrayZipEntryTransformer() {
        protected byte[] transform(ZipEntry zipEntry, byte[] input) throws IOException {
          String s = new String(input);
          assertEquals(new String(contents), s);
          return s.toUpperCase().getBytes();
        }
      });

      // Test the ZipPathUtil
      byte[] actual = ZipPathUtil.unpackEntry(file1, name);
      assertNotNull(actual);
      assertEquals(new String(contents).toUpperCase(), new String(actual));
    }
    finally {
      Files.deleteIfExists(file1);
    }
  }
}
