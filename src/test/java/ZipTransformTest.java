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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.transform.ByteArrayZipEntryTransformer;
import org.zeroturnaround.zip.transform.StreamZipEntryTransformer;

public class ZipTransformTest extends TestCase {

  public void testByteArrayTransformer() throws IOException {
    final String name = "foo";
    final byte[] contents = "bar".getBytes();

    File file1 = File.createTempFile("temp", null);
    File file2 = File.createTempFile("temp", null);
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
      }, file2);

      // Test the ZipUtil
      byte[] actual = ZipUtil.unpackEntry(file2, name);
      assertNotNull(actual);
      assertEquals(new String(contents).toUpperCase(), new String(actual));
    }
    finally {
      FileUtils.deleteQuietly(file1);
      FileUtils.deleteQuietly(file2);
    }
  }

  public void testStreamTransformerIdentity() throws IOException {
    final String name = "foo";
    final byte[] contents = "bar".getBytes();

    File file1 = File.createTempFile("temp", null);
    File file2 = File.createTempFile("temp", null);
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
      ZipUtil.transformEntry(file1, name, new StreamZipEntryTransformer() {
        protected void transform(ZipEntry zipEntry, InputStream in, OutputStream out) throws IOException {
          IOUtils.copy(in, out);
        }
      }, file2);

      // Test the ZipUtil
      byte[] actual = ZipUtil.unpackEntry(file2, name);
      assertNotNull(actual);
      assertEquals(new String(contents), new String(actual));
    }
    finally {
      FileUtils.deleteQuietly(file1);
      FileUtils.deleteQuietly(file2);
    }
  }

  public void testStreamTransformer() throws IOException {
    final String name = "foo";
    final byte[] contents = "bar".getBytes();
    final byte[] transformed = "cbs".getBytes();

    File file1 = File.createTempFile("temp", null);
    File file2 = File.createTempFile("temp", null);
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
      ZipUtil.transformEntry(file1, name, new StreamZipEntryTransformer() {
        protected void transform(ZipEntry zipEntry, InputStream in, OutputStream out) throws IOException {
          int b;
          while ((b = in.read()) != -1)
            out.write(b + 1);
        }
      }, file2);

      // Test the ZipUtil
      byte[] actual = ZipUtil.unpackEntry(file2, name);
      assertNotNull(actual);
      assertEquals(new String(transformed), new String(actual));
    }
    finally {
      FileUtils.deleteQuietly(file1);
      FileUtils.deleteQuietly(file2);
    }
  }

  public void testByteArrayTransformerInStream() throws IOException {
    final String name = "foo";
    final byte[] contents = "bar".getBytes();

    File file1 = File.createTempFile("temp", null);
    File file2 = File.createTempFile("temp", null);
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
      FileInputStream in = null;
      FileOutputStream out = null;
      try {
        in = new FileInputStream(file1);
        out = new FileOutputStream(file2);
        
        ZipUtil.transformEntry(in, name, new ByteArrayZipEntryTransformer() {
          protected byte[] transform(ZipEntry zipEntry, byte[] input) throws IOException {
            String s = new String(input);
            assertEquals(new String(contents), s);
            return s.toUpperCase().getBytes();
          }
        }, out);
      }
      finally {
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
      }

      // Test the ZipUtil
      byte[] actual = ZipUtil.unpackEntry(file2, name);
      assertNotNull(actual);
      assertEquals(new String(contents).toUpperCase(), new String(actual));
    }
    finally {
      FileUtils.deleteQuietly(file1);
      FileUtils.deleteQuietly(file2);
    }
  }
  
}
