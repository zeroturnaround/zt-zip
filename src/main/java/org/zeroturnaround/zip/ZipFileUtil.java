package org.zeroturnaround.zip;

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
 * because Java 6 doesn't have constructors with Charsets that were
 * introduced in Java 7.
 */
class ZipFileUtil {
  private static final String MISSING_METHOD_PLEASE_UPGRADE = "Your JRE doesn't support the ZipFile Charset constructor. Please upgrade JRE to 1.7 use this feature. Tried constructor ZipFile(File, Charset).";
  private static final String CONSTRUCTOR_MESSAGE_FOR_ZIPFILE = "Using constructor ZipFile(File, Charset) has failed: ";
  private static final String CONSTRUCTOR_MESSAGE_FOR_OUTPUT = "Using constructor ZipOutputStream(OutputStream, Charset) has failed: ";
  private static final String CONSTRUCTOR_MESSAGE_FOR_INPUT = "Using constructor ZipInputStream(InputStream, Charset) has failed: ";

  // Private constructor for the utility class
  private ZipFileUtil() {
  }

  /**
   * Returns a ZipInputStream opened with a given charset.
   */
  static ZipInputStream createZipInputStream(InputStream inStream, Charset charset) {
    if (charset == null)
      return new ZipInputStream(inStream);

    try {
      Constructor<ZipInputStream> constructor = ZipInputStream.class.getConstructor(new Class[] { InputStream.class, Charset.class });
      return (ZipInputStream) constructor.newInstance(new Object[] { inStream, charset });
    }
    catch (NoSuchMethodException e) {
      throw new IllegalStateException(MISSING_METHOD_PLEASE_UPGRADE, e);
    }
    catch (InstantiationException e) {
      throw new IllegalStateException(CONSTRUCTOR_MESSAGE_FOR_INPUT + e.getMessage(), e);
    }
    catch (IllegalAccessException e) {
      throw new IllegalStateException(CONSTRUCTOR_MESSAGE_FOR_INPUT + e.getMessage(), e);
    }
    catch (IllegalArgumentException e) {
      throw new IllegalStateException(CONSTRUCTOR_MESSAGE_FOR_INPUT + e.getMessage(), e);
    }
    catch (InvocationTargetException e) {
      throw new IllegalStateException(CONSTRUCTOR_MESSAGE_FOR_INPUT + e.getMessage(), e);
    }
  }


  /**
   * Returns a ZipOutputStream opened with a given charset.
   */
  static ZipOutputStream createZipOutputStream(BufferedOutputStream outStream, Charset charset) {
    if (charset == null)
      return new ZipOutputStream(outStream);

    try {
      Constructor<ZipOutputStream> constructor = ZipOutputStream.class.getConstructor(new Class[] { OutputStream.class, Charset.class });
      return (ZipOutputStream) constructor.newInstance(new Object[] { outStream, charset });
    }
    catch (NoSuchMethodException e) {
      throw new IllegalStateException(MISSING_METHOD_PLEASE_UPGRADE, e);
    }
    catch (InstantiationException e) {
      throw new IllegalStateException(CONSTRUCTOR_MESSAGE_FOR_OUTPUT + e.getMessage(), e);
    }
    catch (IllegalAccessException e) {
      throw new IllegalStateException(CONSTRUCTOR_MESSAGE_FOR_OUTPUT + e.getMessage(), e);
    }
    catch (IllegalArgumentException e) {
      throw new IllegalStateException(CONSTRUCTOR_MESSAGE_FOR_OUTPUT + e.getMessage(), e);
    }
    catch (InvocationTargetException e) {
      throw new IllegalStateException(CONSTRUCTOR_MESSAGE_FOR_OUTPUT + e.getMessage(), e);
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
      Constructor<ZipFile> constructor = ZipFile.class.getConstructor(new Class[] { File.class, Charset.class });
      return (ZipFile) constructor.newInstance(new Object[] { src, charset });
    }
    catch (NoSuchMethodException e) {
      throw new IllegalStateException(MISSING_METHOD_PLEASE_UPGRADE, e);
    }
    catch (InstantiationException e) {
      throw new IllegalStateException(CONSTRUCTOR_MESSAGE_FOR_ZIPFILE + e.getMessage(), e);
    }
    catch (IllegalAccessException e) {
      throw new IllegalStateException(CONSTRUCTOR_MESSAGE_FOR_ZIPFILE + e.getMessage(), e);
    }
    catch (IllegalArgumentException e) {
      throw new IllegalStateException(CONSTRUCTOR_MESSAGE_FOR_ZIPFILE + e.getMessage(), e);
    }
    catch (InvocationTargetException e) {
      throw new IllegalStateException(CONSTRUCTOR_MESSAGE_FOR_ZIPFILE + e.getMessage(), e);
    }
  }

  /**
   * Returns <code>true</code> if charsets are supported in this JRE.
   */
  static boolean isCharsetSupported() throws IOException {
    try {
      ZipFile.class.getConstructor(new Class[] { File.class, Charset.class });
      return true;
    }
    catch (NoSuchMethodException e) {
      return false;
    }
  }

}
