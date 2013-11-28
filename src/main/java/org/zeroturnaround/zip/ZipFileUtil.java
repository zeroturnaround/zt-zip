package org.zeroturnaround.zip;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

class ZipFileUtil {

  /**
   * Returns a ZipOutputStream opened with a given charset.
   */
  static ZipOutputStream createZipOutputStream(BufferedOutputStream outStream, Charset charset) {
    if (charset == null)
      return new ZipOutputStream(outStream);

    try {
      Constructor constructor = ZipOutputStream.class.getConstructor(new Class[] { OutputStream.class, Charset.class });
      return (ZipOutputStream) constructor.newInstance(new Object[] { outStream, charset });
    }
    catch (NoSuchMethodException e) {
      throw new IllegalStateException("Using constructor ZipOutputStream(OutputStream, Charset) has failed: " + e.getMessage());
    }
    catch (InstantiationException e) {
      throw new IllegalStateException("Using constructor ZipOutputStream(OutputStream, Charset) has failed: " + e.getMessage());
    }
    catch (IllegalAccessException e) {
      throw new IllegalStateException("Using constructor ZipOutputStream(OutputStream, Charset) has failed: " + e.getMessage());
    }
    catch (IllegalArgumentException e) {
      throw new IllegalStateException("Using constructor ZipOutputStream(OutputStream, Charset) has failed: " + e.getMessage());
    }
    catch (InvocationTargetException e) {
      throw new IllegalStateException("Using constructor ZipOutputStream(OutputStream, Charset) has failed: " + e.getMessage());
    }
  }

  /**
   * Returns a zipFile opened with a given charset
   */
  static ZipFile getZipFile(File src, Charset charset) throws IOException {
    if (charset == null) {
      return new ZipFile(src);
    }

    try {
      Constructor constructor = ZipFile.class.getConstructor(new Class[] { File.class, Charset.class });
      return (ZipFile) constructor.newInstance(new Object[] { src, charset });
    }
    catch (NoSuchMethodException e) {
      throw new IllegalStateException("Using constructor ZipFile(File, Charset) has failed: " + e.getMessage());
    }
    catch (InstantiationException e) {
      throw new IllegalStateException("Using constructor ZipFile(File, Charset) has failed: " + e.getMessage());
    }
    catch (IllegalAccessException e) {
      throw new IllegalStateException("Using constructor ZipFile(File, Charset) has failed: " + e.getMessage());
    }
    catch (IllegalArgumentException e) {
      throw new IllegalStateException("Using constructor ZipFile(File, Charset) has failed: " + e.getMessage());
    }
    catch (InvocationTargetException e) {
      throw new IllegalStateException("Using constructor ZipFile(File, Charset) has failed: " + e.getMessage());
    }
  }

}
