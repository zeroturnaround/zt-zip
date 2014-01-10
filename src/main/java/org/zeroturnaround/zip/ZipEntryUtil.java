package org.zeroturnaround.zip;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

/**
 * Util class for static methods shared between ZipUtil and Zips.
 *
 * @author shelajev
 *
 */
class ZipEntryUtil {

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
   * @param zipEntry
   *          a ZIP entry from existing ZIP file.
   * @param in
   *          contents of the ZIP entry.
   * @param out
   *          target ZIP stream.
   */
  static void copyEntry(ZipEntry zipEntry, InputStream in, ZipOutputStream out, boolean preserveTimestamps) throws IOException {
    ZipEntry copy = copy(zipEntry);
    copy.setTime(preserveTimestamps ? zipEntry.getTime() : System.currentTimeMillis());
    addEntry(copy, new BufferedInputStream(in), out);
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

}
