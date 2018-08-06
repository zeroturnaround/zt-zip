package org.zeroturnaround.zip.path;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.zeroturnaround.zip.ZipPathUtil;
import org.zeroturnaround.zip.commons.IOUtils;
import org.zeroturnaround.zip.transform.ByteArrayZipEntryTransformer;
import org.zeroturnaround.zip.transform.PathZipEntryTransformer;
import org.zeroturnaround.zip.transform.StreamZipEntryTransformer;
import org.zeroturnaround.zip.transform.StringZipEntryTransformer;

import junit.framework.TestCase;

public class ZipPathUtilTransformTest extends TestCase {

  public void testZipTransformNotInPlaceButSameLocation() throws IOException {
    // Create dummy file and transformer
    Path file = Files.createTempFile("temp", null);
    StreamZipEntryTransformer arrayZipEntryTransformer = new StreamZipEntryTransformer() {
      protected void transform(ZipEntry zipEntry, InputStream in, OutputStream out) throws IOException {
      }
    };
    try {
      ZipPathUtil.transformEntry(file, "", arrayZipEntryTransformer, file);
      // This line should not be reached.
      assertTrue(false);
    }
    catch (IllegalArgumentException e) {
      // Do nothing, test passed.
    }
  }

  public void testByteArrayTransformer() throws IOException {
    final String name = "foo";
    final byte[] contents = "bar".getBytes();

    Path file1 = Files.createTempFile("temp", null);
    Path file2 = Files.createTempFile("temp", null);
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
      }, file2);

      // Test the ZipPathUtil
      byte[] actual = ZipPathUtil.unpackEntry(file2, name);
      assertNotNull(actual);
      assertEquals(new String(contents).toUpperCase(), new String(actual));
    }
    finally {
      Files.deleteIfExists(file1);
      Files.deleteIfExists(file2);
    }
  }

  public void testStreamTransformerIdentity() throws IOException {
    final String name = "foo";
    final byte[] contents = "bar".getBytes();

    Path file1 = Files.createTempFile("temp", null);
    Path file2 = Files.createTempFile("temp", null);
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
      ZipPathUtil.transformEntry(file1, name, new StreamZipEntryTransformer() {
        protected void transform(ZipEntry zipEntry, InputStream in, OutputStream out) throws IOException {
          IOUtils.copy(in, out);
        }
      }, file2);

      // Test the ZipPathUtil
      byte[] actual = ZipPathUtil.unpackEntry(file2, name);
      assertNotNull(actual);
      assertEquals(new String(contents), new String(actual));
    }
    finally {
      Files.deleteIfExists(file1);
      Files.deleteIfExists(file2);
    }
  }

  public void testStreamTransformer() throws IOException {
    final String name = "foo";
    final byte[] contents = "bar".getBytes();
    final byte[] transformed = "cbs".getBytes();

    Path file1 = Files.createTempFile("temp", null);
    Path file2 = Files.createTempFile("temp", null);
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
      ZipPathUtil.transformEntry(file1, name, new StreamZipEntryTransformer() {
        protected void transform(ZipEntry zipEntry, InputStream in, OutputStream out) throws IOException {
          int b;
          while ((b = in.read()) != -1)
            out.write(b + 1);
        }
      }, file2);

      // Test the ZipPathUtil
      byte[] actual = ZipPathUtil.unpackEntry(file2, name);
      assertNotNull(actual);
      assertEquals(new String(transformed), new String(actual));
    }
    finally {
      Files.deleteIfExists(file1);
      Files.deleteIfExists(file2);
    }
  }

  public void testByteArrayTransformerInStream() throws IOException {
    final String name = "foo";
    final byte[] contents = "bar".getBytes();

    Path file1 = Files.createTempFile("temp", null);
    Path file2 = Files.createTempFile("temp", null);
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
      try (InputStream in = Files.newInputStream(file1);
          OutputStream out = Files.newOutputStream(file2);) {

        ZipPathUtil.transformEntry(in, name, new ByteArrayZipEntryTransformer() {
          protected byte[] transform(ZipEntry zipEntry, byte[] input) throws IOException {
            String s = new String(input);
            assertEquals(new String(contents), s);
            return s.toUpperCase().getBytes();
          }
        }, out);
      }

      // Test the ZipPathUtil
      byte[] actual = ZipPathUtil.unpackEntry(file2, name);
      assertNotNull(actual);
      assertEquals(new String(contents).toUpperCase(), new String(actual));
    }
    finally {
      Files.deleteIfExists(file1);
      Files.deleteIfExists(file2);
    }
  }

  public void testFileZipEntryTransformerInStream() throws IOException {
    final String name = "foo";
    final byte[] contents = "bar".getBytes();

    Path file1 = Files.createTempFile("temp", null);
    Path file2 = Files.createTempFile("temp", null);
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

      try (InputStream in = Files.newInputStream(file1);
          OutputStream out = Files.newOutputStream(file2);) {

        ZipPathUtil.transformEntry(in, name, new PathZipEntryTransformer() {
          protected void transform(ZipEntry zipEntry, Path in, Path out) throws IOException {
            Files.write(out, "CAFEBABE".getBytes());
          }
        }, out);
      }

      // Test the ZipPathUtil
      byte[] actual = ZipPathUtil.unpackEntry(file2, name);
      assertNotNull(actual);
      assertEquals("CAFEBABE", new String(actual));
    }
    finally {
      Files.deleteIfExists(file1);
      Files.deleteIfExists(file2);
    }
  }

  public void testPathZipEntryTransformerInStream() throws IOException {
    final String name = "foo";
    final byte[] contents = "bar".getBytes();

    Path file1 = Files.createTempFile("temp", null);
    Path file2 = Files.createTempFile("temp", null);
    try {
      // Create the ZIP file
      try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(file1))) {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(contents);
        zos.closeEntry();
      }

      // Transform the ZIP file
      try (InputStream in = Files.newInputStream(file1);
          OutputStream out = Files.newOutputStream(file2)) {

        ZipPathUtil.transformEntry(in, name, new PathZipEntryTransformer() {
          protected void transform(ZipEntry zipEntry, Path in, Path out) throws IOException {
            Files.write(out, "CAFEBABE".getBytes());
          }
        }, out);
      }

      // Test the ZipPathUtil
      byte[] actual = ZipPathUtil.unpackEntry(file2, name);
      assertNotNull(actual);
      assertEquals("CAFEBABE", new String(actual));
    }
    finally {
      Files.deleteIfExists(file1);
      Files.deleteIfExists(file2);
    }
  }

  public void testStringZipEntryTransformerInStream() throws IOException {
    final String name = "foo";
    String FILE_CONTENTS = "bar";
    final byte[] contents = FILE_CONTENTS.getBytes();

    Path file1 = Files.createTempFile("temp", null);
    Path file2 = Files.createTempFile("temp", null);
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
      try (InputStream in = Files.newInputStream(file1);
          OutputStream out = Files.newOutputStream(file2);) {

        ZipPathUtil.transformEntry(in, name, new StringZipEntryTransformer("UTF-8") {
          protected String transform(ZipEntry zipEntry, String input) throws IOException {
            return input.toUpperCase();
          }
        }, out);
      }

      // Test the ZipPathUtil
      byte[] actual = ZipPathUtil.unpackEntry(file2, name);
      assertEquals(FILE_CONTENTS.toUpperCase(), new String(actual));
    }
    finally {
      Files.deleteIfExists(file1);
      Files.deleteIfExists(file2);
    }
  }
}
