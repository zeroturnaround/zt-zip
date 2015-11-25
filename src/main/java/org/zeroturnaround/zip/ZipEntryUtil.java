package org.zeroturnaround.zip;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.zeroturnaround.zip.commons.IOUtils;
import org.zeroturnaround.zip.extra.AsiExtraField;
import org.zeroturnaround.zip.extra.ExtraFieldUtils;
import org.zeroturnaround.zip.extra.ZipExtraField;

/**
 * Util class for static methods shared between ZipUtil and Zips.
 *
 * @author shelajev
 *
 */
class ZipEntryUtil {

  private ZipEntryUtil() {
  }

  /**
   * Copy entry
   *
   * @param original - zipEntry to copy
   * @return copy of the original entry
   */
  static ZipEntry copy(ZipEntry original) {
    return copy(original, null);
  }

  /**
   * Copy entry with another name.
   *
   * @param original - zipEntry to copy
   * @param newName - new entry name, optional, if null, ogirinal's entry
   * @return copy of the original entry, but with the given name
   */
  static ZipEntry copy(ZipEntry original, String newName) {
    ZipEntry copy = new ZipEntry(newName == null ? original.getName() : newName);
    if (original.getCrc() != -1) {
      copy.setCrc(original.getCrc());
    }
    if (original.getMethod() != -1) {
      copy.setMethod(original.getMethod());
    }
    if (original.getSize() >= 0) {
      copy.setSize(original.getSize());
    }
    if (original.getExtra() != null) {
      copy.setExtra(original.getExtra());
    }

    copy.setComment(original.getComment());
    copy.setTime(original.getTime());
    return copy;
  }

  /**
   * Copies a given ZIP entry to a ZIP file.
   *
   * @param zipEntry
   *          a ZIP entry from existing ZIP file.
   * @param in
   *          contents of the ZIP entry.
   * @param out
   *          target ZIP stream.
   */
  static void copyEntry(ZipEntry zipEntry, InputStream in, ZipOutputStream out) throws IOException {
    copyEntry(zipEntry, in, out, true);
  }

  /**
   * Copies a given ZIP entry to a ZIP file. If this.preserveTimestamps is true, original timestamp
   * is carried over, otherwise uses current time.
   *
   * @param originalEntry
   *          a ZIP entry from existing ZIP file.
   * @param in
   *          contents of the ZIP entry.
   * @param out
   *          target ZIP stream.
   */
  static void copyEntry(ZipEntry originalEntry, InputStream in, ZipOutputStream out, boolean preserveTimestamps) throws IOException {
    ZipEntry copy = copy(originalEntry);

    if (preserveTimestamps) {
      /**
       * If we succeed to set the modified time (works only on JDK8)
       * then we shouldn't call the setTime method anymore as that
       * will nullify the modified time - see
       * http://hg.openjdk.java.net/jdk8u/jdk8u/jdk/file/f1f3f9eaf7fa/src/share/classes/java/util/zip/ZipEntry.java#l163
       */
      boolean success = tryToPreserveLatestJDKSupportedTimes(copy, originalEntry);
      if (!success) {
        copy.setTime(originalEntry.getTime());
      }
    }
    else {
      copy.setTime(System.currentTimeMillis());
    }
    addEntry(copy, new BufferedInputStream(in), out);
  }

  /**
   * The method tries to set the last modified time. The pre-requisite for this is that
   * the running JDK is at least 1.8 that has the necessary methods. As we need to preserve
   * compile time compatibility with JDK 5 then we use reflection for the lookup.
   * 
   * See https://github.com/zeroturnaround/zt-zip/issues/73
   * 
   * @param target the instance we want to set the last modified time to
   * @param original the instance we use to get the last modified time
   */
  private static boolean tryToPreserveLatestJDKSupportedTimes(ZipEntry target, ZipEntry original) {
    try {
      Class<?> fileTimeClass = Class.forName("java.nio.file.attribute.FileTime");
      
      // modified time handling
      Method setLastModifiedTimeMethod = ZipEntry.class.getMethod("setLastModifiedTime", fileTimeClass);
      Method getLastModifiedTimeMethod = ZipEntry.class.getMethod("getLastModifiedTime");

      Object lastModified = getLastModifiedTimeMethod.invoke(original);
      setLastModifiedTimeMethod.invoke(target, lastModified);
      
      // creation time handling
      Method setCreationTime = ZipEntry.class.getMethod("setCreationTime", fileTimeClass);
      Method getCreationTime = ZipEntry.class.getMethod("getCreationTime");

      Object createdTime = getCreationTime.invoke(original);
      setCreationTime.invoke(target, createdTime);
      
      // last access time
      Method setLastAccessTime = ZipEntry.class.getMethod("setLastAccessTime", fileTimeClass);
      Method getLastAccessTime = ZipEntry.class.getMethod("getLastAccessTime");

      Object lastAccessTime = getLastAccessTime.invoke(original);
      setLastAccessTime.invoke(target, lastAccessTime);
      
      return true;
    }
    catch (IllegalAccessException e) {
      // Ignore, we are not running Java 8
    }
    catch (InvocationTargetException e) {
      // Ignore, we are not running Java 8
    }
    catch (NoSuchMethodException e) {
      // Ignore, we are not running Java 8
    }
    catch (ClassNotFoundException e) {
      // Ignore, we are not running Java 8
    }
    catch (SecurityException e) {
      // Ignore, we are not running Java 8
    }
    return false;
  }

  /**
   * Adds a given ZIP entry to a ZIP file.
   *
   * @param zipEntry
   *          new ZIP entry.
   * @param in
   *          contents of the ZIP entry.
   * @param out
   *          target ZIP stream.
   */
  static void addEntry(ZipEntry zipEntry, InputStream in, ZipOutputStream out) throws IOException {
    out.putNextEntry(zipEntry);
    if (in != null) {
      IOUtils.copy(in, out);
    }
    out.closeEntry();
  }

  /**
   * Create new Zip entry and fill it with associated with file meta-info
   * 
   * @param name Zip entry name
   * @param file source File
   * @return newly created Zip entry
   */
  static ZipEntry fromFile(String name, File file) {
    ZipEntry zipEntry = new ZipEntry(name);
    if (!file.isDirectory()) {
      zipEntry.setSize(file.length());
    }
    zipEntry.setTime(file.lastModified());

    ZTFilePermissions permissions = ZTFilePermissionsUtil.getDefaultStategy().getPermissions(file);
    if (permissions != null) {
      ZipEntryUtil.setZTFilePermissions(zipEntry, permissions);
    }
    return zipEntry;
  }

  /**
   * Add file permissions info to ZIP entry.
   * Current implementation adds "ASi Unix" (tag 0x756e) extra block to entry.
   * 
   * @param zipEntry ZIP entry
   * @param permissions permissions to assign
   */
  static boolean setZTFilePermissions(ZipEntry zipEntry, ZTFilePermissions permissions) {
    try {
      List<ZipExtraField> fields = ExtraFieldUtils.parse(zipEntry.getExtra());
      AsiExtraField asiExtraField = getFirstAsiExtraField(fields);
      if (asiExtraField == null) {
        asiExtraField = new AsiExtraField();
        fields.add(asiExtraField);
      }

      asiExtraField.setDirectory(zipEntry.isDirectory());
      asiExtraField.setMode(ZTFilePermissionsUtil.toPosixFileMode(permissions));
      zipEntry.setExtra(ExtraFieldUtils.mergeLocalFileDataData(fields));
      return true;
    }
    catch (java.util.zip.ZipException ze) {
      return false;
    }
  }

  /**
   * Get assigned to ZIP entry file permissions info. Current implementation tries to read "ASi Unix" (tag 0x756e) extra tag.
   * "ASi Unix"
   * 
   * @param zipEntry
   * @return file permissions info or <code>null</code> if ZIP entry does not have "ASi Unix" extra field.
   */
  static ZTFilePermissions getZTFilePermissions(ZipEntry zipEntry) {
    try {
      ZTFilePermissions permissions = null;
      List<ZipExtraField> fields = ExtraFieldUtils.parse(zipEntry.getExtra());
      AsiExtraField asiExtraField = getFirstAsiExtraField(fields);
      if (asiExtraField != null) {
        int mode = asiExtraField.getMode() & 0777;
        permissions = ZTFilePermissionsUtil.fromPosixFileMode(mode);
      }
      return permissions;
    }
    catch (java.util.zip.ZipException ze) {
      throw new ZipException(ze);
    }
  }

  private static AsiExtraField getFirstAsiExtraField(List<ZipExtraField> fields) {
    AsiExtraField asiExtraField = null;
    for (ZipExtraField field : fields) {
      if (field instanceof AsiExtraField) {
        asiExtraField = (AsiExtraField) field;
      }
    }
    return asiExtraField;
  }

}
