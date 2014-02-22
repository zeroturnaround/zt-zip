package org.zeroturnaround.zip;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Mainly methods to lookup Zip* class constructors. This is needed
 * becauses Java 6 doesn't have constructors with Charsets. 
 */
class ZipFileUtil {
  /**
   * Returns a ZipInputStream opened with a given charset.
   */
  static ZipInputStream createZipInputStream(InputStream inStream, Charset charset) {
    if (charset == null)
      return new ZipInputStream(inStream);

    try {
      Constructor constructor = ZipInputStream.class.getConstructor(new Class[] { InputStream.class, Charset.class });
      return (ZipInputStream) constructor.newInstance(new Object[] { inStream, charset });
    }
    catch (NoSuchMethodException e) {
      throw new IllegalStateException("Using constructor ZipInputStream(InputStream, Charset) has failed: " + e.getMessage());
    }
    catch (InstantiationException e) {
      throw new IllegalStateException("Using constructor ZipInputStream(InputStream, Charset) has failed: " + e.getMessage());
    }
    catch (IllegalAccessException e) {
      throw new IllegalStateException("Using constructor ZipInputStream(InputStream, Charset) has failed: " + e.getMessage());
    }
    catch (IllegalArgumentException e) {
      throw new IllegalStateException("Using constructor ZipInputStream(InputStream, Charset) has failed: " + e.getMessage());
    }
    catch (InvocationTargetException e) {
      throw new IllegalStateException("Using constructor ZipInputStream(InputStream, Charset) has failed: " + e.getMessage());
    }
  }
  
  
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
      throw new IllegalStateException("Your JRE doesn't support the ZipFile Charset constructor. Please upgrade JRE to 1.7 use this feature. Tried constructor ZipFile(File, Charset).", e);
    }
    catch (InstantiationException e) {
      throw new IllegalStateException("Using constructor ZipFile(File, Charset) has failed: " + e.getMessage(), e);
    }
    catch (IllegalAccessException e) {
      throw new IllegalStateException("Using constructor ZipFile(File, Charset) has failed: " + e.getMessage(), e);
    }
    catch (IllegalArgumentException e) {
      throw new IllegalStateException("Using constructor ZipFile(File, Charset) has failed: " + e.getMessage(), e);
    }
    catch (InvocationTargetException e) {
      throw new IllegalStateException("Using constructor ZipFile(File, Charset) has failed: " + e.getMessage(), e);
    }
  }

}
