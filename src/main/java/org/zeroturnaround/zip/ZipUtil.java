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
package org.zeroturnaround.zip;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.commons.FileUtils;
import org.zeroturnaround.zip.commons.FilenameUtils;
import org.zeroturnaround.zip.commons.IOUtils;
import org.zeroturnaround.zip.transform.ZipEntryTransformer;
import org.zeroturnaround.zip.transform.ZipEntryTransformerEntry;

/**
 * ZIP file manipulation utilities.
 *
 * @author Rein Raudjärv
 * @author Innokenty Shuvalov
 *
 * @see #containsEntry(File, String)
 * @see #unpackEntry(File, String)
 * @see #unpack(File, File)
 * @see #pack(File, File)
 */
public final class ZipUtil {

  private static final String PATH_SEPARATOR = "/";

  /** Default compression level */
  public static final int DEFAULT_COMPRESSION_LEVEL = Deflater.DEFAULT_COMPRESSION;

  // Use / instead of . to work around an issue with Maven Shade Plugin
  private static final Logger log = LoggerFactory.getLogger("org/zeroturnaround/zip/ZipUtil".replace('/', '.')); // NOSONAR

  private ZipUtil() {
  }

  /* Extracting single entries from ZIP files. */

  /**
   * Checks if the ZIP file contains the given entry.
   *
   * @param zip
   *          ZIP file.
   * @param name
   *          entry name.
   * @return <code>true</code> if the ZIP file contains the given entry.
   */
  public static boolean containsEntry(File zip, String name) {
    ZipFile zf = null;
    try {
      zf = new ZipFile(zip);
      return zf.getEntry(name) != null;
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
    finally {
      closeQuietly(zf);
    }
  }

  /**
   * Returns the compression method of a given entry of the ZIP file.
   *
   * @param zip
   *          ZIP file.
   * @param name
   *          entry name.
   * @return Returns <code>ZipEntry.STORED</code>, <code>ZipEntry.DEFLATED</code> or -1 if
   *         the ZIP file does not contain the given entry.
   * @deprecated The compression level cannot be retrieved. This method exists only to ensure backwards compatibility with ZipUtil version 1.9, which returned the compression
   *             method, not the level.
   */
  @Deprecated
  public static int getCompressionLevelOfEntry(File zip, String name) {
    return getCompressionMethodOfEntry(zip, name);
  }

  /**
   * Returns the compression method of a given entry of the ZIP file.
   *
   * @param zip
   *          ZIP file.
   * @param name
   *          entry name.
   * @return Returns <code>ZipEntry.STORED</code>, <code>ZipEntry.DEFLATED</code> or -1 if
   *         the ZIP file does not contain the given entry.
   */
  public static int getCompressionMethodOfEntry(File zip, String name) {
    ZipFile zf = null;
    try {
      zf = new ZipFile(zip);
      ZipEntry zipEntry = zf.getEntry(name);
      if (zipEntry == null) {
        return -1;
      }
      return zipEntry.getMethod();
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
    finally {
      closeQuietly(zf);
    }
  }

  /**
   * Checks if the ZIP file contains any of the given entries.
   *
   * @param zip
   *          ZIP file.
   * @param names
   *          entry names.
   * @return <code>true</code> if the ZIP file contains any of the given
   *         entries.
   */
  public static boolean containsAnyEntry(File zip, String[] names) {
    ZipFile zf = null;
    try {
      zf = new ZipFile(zip);
      for (int i = 0; i < names.length; i++) {
        if (zf.getEntry(names[i]) != null) {
          return true;
        }
      }
      return false;
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
    finally {
      closeQuietly(zf);
    }
  }

  /**
   * Unpacks a single entry from a ZIP file.
   *
   * @param zip
   *          ZIP file.
   * @param name
   *          entry name.
   * @return contents of the entry or <code>null</code> if it was not found.
   */
  public static byte[] unpackEntry(File zip, String name) {
    ZipFile zf = null;
    try {
      zf = new ZipFile(zip);
      return doUnpackEntry(zf, name);
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
    finally {
      closeQuietly(zf);
    }
  }

  /**
   * Unpacks a single entry from a ZIP file.
   *
   * @param zip
   *          ZIP file.
   * @param name
   *          entry name.
   *
   * @param charset
   *          charset to be used to process the zip
   *
   * @return contents of the entry or <code>null</code> if it was not found.
   */
  public static byte[] unpackEntry(File zip, String name, Charset charset) {
    ZipFile zf = null;
    try {
      if (charset != null) {
        zf = new ZipFile(zip, charset);
      }
      else {
        zf = new ZipFile(zip);
      }
      return doUnpackEntry(zf, name);
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
    finally {
      closeQuietly(zf);
    }
  }

  /**
   * Unpacks a single entry from a ZIP file.
   *
   * @param zf
   *          ZIP file.
   * @param name
   *          entry name.
   * @return contents of the entry or <code>null</code> if it was not found.
   */
  public static byte[] unpackEntry(ZipFile zf, String name) {
    try {
      return doUnpackEntry(zf, name);
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
  }

  /**
   * Unpacks a single entry from a ZIP file.
   *
   * @param zf
   *          ZIP file.
   * @param name
   *          entry name.
   * @return contents of the entry or <code>null</code> if it was not found.
   */
  private static byte[] doUnpackEntry(ZipFile zf, String name) throws IOException {
    ZipEntry ze = zf.getEntry(name);
    if (ze == null) {
      return null; // entry not found
    }

    InputStream is = zf.getInputStream(ze);
    try {
      return IOUtils.toByteArray(is);
    }
    finally {
      IOUtils.closeQuietly(is);
    }
  }

  /**
   * Unpacks a single entry from a ZIP stream.
   *
   * @param is
   *          ZIP stream.
   * @param name
   *          entry name.
   * @return contents of the entry or <code>null</code> if it was not found.
   */
  public static byte[] unpackEntry(InputStream is, String name) {
    ByteArrayUnpacker action = new ByteArrayUnpacker();
    if (!handle(is, name, action))
      return null; // entry not found
    return action.getBytes();
  }

  /**
   * Copies an entry into a byte array.
   *
   * @author Rein Raudjärv
   */
  private static class ByteArrayUnpacker implements ZipEntryCallback {

    private byte[] bytes;

    public void process(InputStream in, ZipEntry zipEntry) throws IOException {
      bytes = IOUtils.toByteArray(in);
    }

    public byte[] getBytes() {
      return bytes;
    }

  }

  /**
   * Unpacks a single file from a ZIP archive to a file.
   *
   * @param zip
   *          ZIP file.
   * @param name
   *          entry name.
   * @param file
   *          target file to be created or overwritten.
   * @return <code>true</code> if the entry was found and unpacked,
   *         <code>false</code> if the entry was not found.
   */
  public static boolean unpackEntry(File zip, String name, File file) {
    return unpackEntry(zip, name, file, null);
  }

  /**
   * Unpacks a single file from a ZIP archive to a file.
   *
   * @param zip
   *          ZIP file.
   * @param name
   *          entry name.
   * @param file
   *          target file to be created or overwritten.
   * @param charset
   *          charset to be used processing the zip
   *
   * @return <code>true</code> if the entry was found and unpacked,
   *         <code>false</code> if the entry was not found.
   */
  public static boolean unpackEntry(File zip, String name, File file, Charset charset) {
    ZipFile zf = null;
    try {
      if (charset != null) {
        zf = new ZipFile(zip, charset);
      }
      else {
        zf = new ZipFile(zip);
      }
      return doUnpackEntry(zf, name, file);
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
    finally {
      closeQuietly(zf);
    }
  }

  /**
   * Unpacks a single file from a ZIP archive to a file.
   *
   * @param zf
   *          ZIP file.
   * @param name
   *          entry name.
   * @param file
   *          target file to be created or overwritten.
   * @return <code>true</code> if the entry was found and unpacked,
   *         <code>false</code> if the entry was not found.
   */
  public static boolean unpackEntry(ZipFile zf, String name, File file) {
    try {
      return doUnpackEntry(zf, name, file);
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
  }

  /**
   * Unpacks a single file from a ZIP archive to a file.
   *
   * @param zf
   *          ZIP file.
   * @param name
   *          entry name.
   * @param file
   *          target file to be created or overwritten.
   * @return <code>true</code> if the entry was found and unpacked,
   *         <code>false</code> if the entry was not found.
   */
  private static boolean doUnpackEntry(ZipFile zf, String name, File file) throws IOException {
    if (log.isTraceEnabled()) {
      log.trace("Extracting '" + zf.getName() + "' entry '" + name + "' into '" + file + "'.");
    }

    ZipEntry ze = zf.getEntry(name);
    if (ze == null) {
      return false; // entry not found
    }

    if (ze.isDirectory() || zf.getInputStream(ze) == null) {
      if (file.isDirectory()) {
        return true;
      }
      if (file.exists()) {
        FileUtils.forceDelete(file);
      }
      return file.mkdirs();
    }

    InputStream in = new BufferedInputStream(zf.getInputStream(ze));
    try {
      FileUtils.copy(in, file);
    }
    finally {
      IOUtils.closeQuietly(in);
    }
    return true;
  }

  /**
   * Unpacks a single file from a ZIP stream to a file.
   *
   * @param is
   *          ZIP stream.
   * @param name
   *          entry name.
   * @param file
   *          target file to be created or overwritten.
   * @return <code>true</code> if the entry was found and unpacked,
   *         <code>false</code> if the entry was not found.
   * @throws java.io.IOException if file is not found or writing to it fails
   */
  public static boolean unpackEntry(InputStream is, String name, File file) throws IOException {
    return handle(is, name, new FileUnpacker(file));
  }

  /**
   * Copies an entry into a File.
   *
   * @author Rein Raudjärv
   */
  private static class FileUnpacker implements ZipEntryCallback {

    private final File file;

    public FileUnpacker(File file) {
      this.file = file;
    }

    public void process(InputStream in, ZipEntry zipEntry) throws IOException {
      FileUtils.copy(in, file);
    }

  }

  /* Traversing ZIP files */

  /**
   * Reads the given ZIP file and executes the given action for each entry.
   * <p>
   * For each entry the corresponding input stream is also passed to the action. If you want to stop the loop
   * then throw a ZipBreakException.
   *
   * @param zip
   *          input ZIP file.
   * @param action
   *          action to be called for each entry.
   *
   * @see ZipEntryCallback
   * @see #iterate(File, ZipInfoCallback)
   */
  public static void iterate(File zip, ZipEntryCallback action) {
    iterate(zip, action, null);
  }

  /**
   * Reads the given ZIP file and executes the given action for each entry.
   * <p>
   * For each entry the corresponding input stream is also passed to the action. If you want to stop the loop
   * then throw a ZipBreakException.
   *
   * @param zip
   *          input ZIP file.
   * @param action
   *          action to be called for each entry.
   *
   * @param charset
   *          Charset used to processed the ZipFile with
   *
   * @see ZipEntryCallback
   * @see #iterate(File, ZipInfoCallback)
   */
  public static void iterate(File zip, ZipEntryCallback action, Charset charset) {
    ZipFile zf = null;
    try {
      if (charset == null) {
        zf = new ZipFile(zip);
      }
      else {
        zf = new ZipFile(zip, charset);
      }

      Enumeration<? extends ZipEntry> en = zf.entries();
      while (en.hasMoreElements()) {
        ZipEntry e = (ZipEntry) en.nextElement();

        InputStream is = zf.getInputStream(e);
        try {
          action.process(is, e);
        }
        catch (IOException ze) {
          throw new ZipException("Failed to process zip entry '" + e.getName() + "' with action " + action, ze);
        }
        catch (ZipBreakException ex) {
          break;
        }
        finally {
          IOUtils.closeQuietly(is);
        }
      }
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
    finally {
      closeQuietly(zf);
    }
  }

  /**
   * Reads the given ZIP file and executes the given action for each given entry.
   * <p>
   * For each given entry the corresponding input stream is also passed to the action. If you want to stop the loop then throw a ZipBreakException.
   *
   * @param zip
   *          input ZIP file.
   * @param entryNames
   *          names of entries to iterate
   * @param action
   *          action to be called for each entry.
   *
   * @see ZipEntryCallback
   * @see #iterate(File, String[], ZipInfoCallback)
   */
  public static void iterate(File zip, String[] entryNames, ZipEntryCallback action) {
    iterate(zip, entryNames, action, null);
  }

  /**
   * Reads the given ZIP file and executes the given action for each given entry.
   * <p>
   * For each given entry the corresponding input stream is also passed to the action. If you want to stop the loop then throw a ZipBreakException.
   *
   * @param zip
   *          input ZIP file.
   * @param entryNames
   *          names of entries to iterate
   * @param action
   *          action to be called for each entry.
   * @param charset
   *          charset used to process the zip file
   *
   * @see ZipEntryCallback
   * @see #iterate(File, String[], ZipInfoCallback)
   */
  public static void iterate(File zip, String[] entryNames, ZipEntryCallback action, Charset charset) {
    ZipFile zf = null;
    try {
      if (charset == null) {
        zf = new ZipFile(zip);
      }
      else {
        zf = new ZipFile(zip, charset);
      }

      for (int i = 0; i < entryNames.length; i++) {
        ZipEntry e = zf.getEntry(entryNames[i]);
        if (e == null) {
          continue;
        }
        InputStream is = zf.getInputStream(e);
        try {
          action.process(is, e);
        }
        catch (IOException ze) {
          throw new ZipException("Failed to process zip entry '" + e.getName() + " with action " + action, ze);
        }
        catch (ZipBreakException ex) {
          break;
        }
        finally {
          IOUtils.closeQuietly(is);
        }
      }
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
    finally {
      closeQuietly(zf);
    }
  }

  /**
   * Scans the given ZIP file and executes the given action for each entry.
   * <p>
   * Only the meta-data without the actual data is read. If you want to stop the loop
   * then throw a ZipBreakException.
   *
   * @param zip
   *          input ZIP file.
   * @param action
   *          action to be called for each entry.
   *
   * @see ZipInfoCallback
   * @see #iterate(File, ZipEntryCallback)
   */
  public static void iterate(File zip, ZipInfoCallback action) {
    ZipFile zf = null;
    try {
      zf = new ZipFile(zip);

      Enumeration<? extends ZipEntry> en = zf.entries();
      while (en.hasMoreElements()) {
        ZipEntry e = (ZipEntry) en.nextElement();
        try {
          action.process(e);
        }
        catch (IOException ze) {
          throw new ZipException("Failed to process zip entry '" + e.getName() + " with action " + action, ze);
        }
        catch (ZipBreakException ex) {
          break;
        }
      }
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
    finally {
      closeQuietly(zf);
    }
  }

  /**
   * Scans the given ZIP file and executes the given action for each given entry.
   * <p>
   * Only the meta-data without the actual data is read. If you want to stop the loop then throw a ZipBreakException.
   *
   * @param zip
   *          input ZIP file.
   * @param entryNames
   *          names of entries to iterate
   * @param action
   *          action to be called for each entry.
   *
   * @see ZipInfoCallback
   * @see #iterate(File, String[], ZipEntryCallback)
   */
  public static void iterate(File zip, String[] entryNames, ZipInfoCallback action) {
    ZipFile zf = null;
    try {
      zf = new ZipFile(zip);

      for (int i = 0; i < entryNames.length; i++) {
        ZipEntry e = zf.getEntry(entryNames[i]);
        if (e == null) {
          continue;
        }
        try {
          action.process(e);
        }
        catch (IOException ze) {
          throw new ZipException("Failed to process zip entry '" + e.getName() + " with action " + action, ze);
        }
        catch (ZipBreakException ex) {
          break;
        }
      }
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
    finally {
      closeQuietly(zf);
    }
  }

  /**
   * Reads the given ZIP stream and executes the given action for each entry.
   * <p>
   * For each entry the corresponding input stream is also passed to the action. If you want to stop the loop
   * then throw a ZipBreakException.
   *
   * @param is
   *          input ZIP stream (it will not be closed automatically).
   * @param action
   *          action to be called for each entry.
   * @param charset
   *          charset to process entries in
   *
   * @see ZipEntryCallback
   * @see #iterate(File, ZipEntryCallback)
   */
  public static void iterate(InputStream is, ZipEntryCallback action, Charset charset) {
    try {
      ZipInputStream in = null;
      try {
        in = newCloseShieldZipInputStream(is, charset);
        ZipEntry entry;
        while ((entry = in.getNextEntry()) != null) {
          try {
            action.process(in, entry);
          }
          catch (IOException ze) {
            throw new ZipException("Failed to process zip entry '" + entry.getName() + " with action " + action, ze);
          }
          catch (ZipBreakException ex) {
            break;
          }
        }
      }
      finally {
        if (in != null) {
          in.close();
        }
      }
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
  }

  /**
   * See {@link #iterate(InputStream, ZipEntryCallback, Charset)}. This method
   * is a shorthand for a version where no Charset is specified.
   *
   * @param is
   *          input ZIP stream (it will not be closed automatically).
   * @param action
   *          action to be called for each entry.
   *
   * @see ZipEntryCallback
   * @see #iterate(File, ZipEntryCallback)
   */
  public static void iterate(InputStream is, ZipEntryCallback action) {
    iterate(is, action, null);
  }

  /**
   * Reads the given ZIP stream and executes the given action for each given entry.
   * <p>
   * For each given entry the corresponding input stream is also passed to the action. If you want to stop the loop then throw a ZipBreakException.
   *
   * @param is
   *          input ZIP stream (it will not be closed automatically).
   * @param entryNames
   *          names of entries to iterate
   * @param action
   *          action to be called for each entry.
   * @param charset
   *          charset to process entries in
   *
   * @see ZipEntryCallback
   * @see #iterate(File, String[], ZipEntryCallback)
   */
  public static void iterate(InputStream is, String[] entryNames, ZipEntryCallback action, Charset charset) {
    Set<String> namesSet = new HashSet<String>();
    for (int i = 0; i < entryNames.length; i++) {
      namesSet.add(entryNames[i]);
    }
    try {
      ZipInputStream in = null;
      try {
        in = newCloseShieldZipInputStream(is, charset);
        ZipEntry entry;
        while ((entry = in.getNextEntry()) != null) {
          if (!namesSet.contains(entry.getName())) {
            // skip the unnecessary entry
            continue;
          }
          try {
            action.process(in, entry);
          }
          catch (IOException ze) {
            throw new ZipException("Failed to process zip entry '" + entry.getName() + " with action " + action, ze);
          }
          catch (ZipBreakException ex) {
            break;
          }
        }
      }
      finally {
        if (in != null) {
          in.close();
        }
      }
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
  }

  /**
   * See @link{ {@link #iterate(InputStream, ZipEntryCallback, Charset)}. It is a
   * shorthand where no Charset is specified.
   *
   * @param is
   *          input ZIP stream (it will not be closed automatically).
   * @param entryNames
   *          names of entries to iterate
   * @param action
   *          action to be called for each entry.
   *
   * @see ZipEntryCallback
   * @see #iterate(File, String[], ZipEntryCallback)
   */
  public static void iterate(InputStream is, String[] entryNames, ZipEntryCallback action) {
    iterate(is, entryNames, action, null);
  }

  /**
   * Creates a new {@link ZipInputStream} based on the given {@link InputStream}. It will be buffered and close-shielded.
   * Closing the result stream flushes the buffers and frees up resources of the {@link ZipInputStream}. However the source stream itself remains open.
   */
  private static ZipInputStream newCloseShieldZipInputStream(final InputStream is, Charset charset) {
    InputStream in = new BufferedInputStream(new CloseShieldInputStream(is));
    if (charset == null) {
      return new ZipInputStream(in);
    }
    return ZipFileUtil.createZipInputStream(in, charset);
  }

  /**
   * Reads the given ZIP file and executes the given action for a single entry.
   *
   * @param zip
   *          input ZIP file.
   * @param name
   *          entry name.
   * @param action
   *          action to be called for this entry.
   * @return <code>true</code> if the entry was found, <code>false</code> if the
   *         entry was not found.
   *
   * @see ZipEntryCallback
   */
  public static boolean handle(File zip, String name, ZipEntryCallback action) {
    ZipFile zf = null;
    try {
      zf = new ZipFile(zip);

      ZipEntry ze = zf.getEntry(name);
      if (ze == null) {
        return false; // entry not found
      }

      InputStream in = new BufferedInputStream(zf.getInputStream(ze));
      try {
        action.process(in, ze);
      }
      finally {
        IOUtils.closeQuietly(in);
      }
      return true;
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
    finally {
      closeQuietly(zf);
    }
  }

  /**
   * Reads the given ZIP stream and executes the given action for a single
   * entry.
   *
   * @param is
   *          input ZIP stream (it will not be closed automatically).
   * @param name
   *          entry name.
   * @param action
   *          action to be called for this entry.
   * @return <code>true</code> if the entry was found, <code>false</code> if the
   *         entry was not found.
   *
   * @see ZipEntryCallback
   */
  public static boolean handle(InputStream is, String name, ZipEntryCallback action) {
    SingleZipEntryCallback helper = new SingleZipEntryCallback(name, action);
    iterate(is, helper);
    return helper.found();
  }

  /**
   * ZipEntryCallback which is only applied to single entry.
   *
   * @author Rein Raudjärv
   */
  private static class SingleZipEntryCallback implements ZipEntryCallback {

    private final String name;

    private final ZipEntryCallback action;

    private boolean found;

    public SingleZipEntryCallback(String name, ZipEntryCallback action) {
      this.name = name;
      this.action = action;
    }

    public void process(InputStream in, ZipEntry zipEntry) throws IOException {
      if (name.equals(zipEntry.getName())) {
        found = true;
        action.process(in, zipEntry);
      }
    }

    public boolean found() {
      return found;
    }

  }

  /* Extracting whole ZIP files. */

  /**
   * Unpacks a ZIP file to the given directory.
   * <p>
   * The output directory must not be a file.
   *
   * @param zip
   *          input ZIP file.
   * @param outputDir
   *          output directory (created automatically if not found).
   */
  public static void unpack(File zip, final File outputDir) {
    unpack(zip, outputDir, IdentityNameMapper.INSTANCE);
  }

  /**
   * Unpacks a ZIP file to the given directory using a specific Charset
   * for the input file.
   *
   * <p>
   * The output directory must not be a file.
   *
   * @param zip
   *          input ZIP file.
   * @param outputDir
   *          output directory (created automatically if not found).
   *
   * @param charset
   *          charset used to unpack the zip file
   */
  public static void unpack(File zip, final File outputDir, Charset charset) {
    unpack(zip, outputDir, IdentityNameMapper.INSTANCE, charset);
  }

  /**
   * Unpacks a ZIP file to the given directory.
   * <p>
   * The output directory must not be a file.
   *
   * @param zip
   *          input ZIP file.
   * @param outputDir
   *          output directory (created automatically if not found).
   * @param mapper
   *          call-back for renaming the entries.
   * @param charset
   *          charset used to process the zip file
   */
  public static void unpack(File zip, File outputDir, NameMapper mapper, Charset charset) {
    log.debug("Extracting '{}' into '{}'.", zip, outputDir);
    iterate(zip, new Unpacker(outputDir, mapper), charset);
  }

  /**
   * Unpacks a ZIP file to the given directory using a specific Charset
   * for the input file.
   *
   * <p>
   * The output directory must not be a file.
   *
   * @param zip
   *          input ZIP file.
   * @param outputDir
   *          output directory (created automatically if not found).
   * @param mapper
   *          call-back for renaming the entries.
   */
  public static void unpack(File zip, File outputDir, NameMapper mapper) {
    log.debug("Extracting '{}' into '{}'.", zip, outputDir);
    iterate(zip, new Unpacker(outputDir, mapper));
  }

  /**
   * Unwraps a ZIP file to the given directory shaving of root dir.
   * If there are multiple root dirs or entries in the root of zip,
   * ZipException is thrown.
   * <p>
   * The output directory must not be a file.
   *
   * @param zip
   *          input ZIP file.
   * @param outputDir
   *          output directory (created automatically if not found).
   */
  public static void unwrap(File zip, final File outputDir) {
    unwrap(zip, outputDir, IdentityNameMapper.INSTANCE);
  }

  /**
   * Unwraps a ZIP file to the given directory shaving of root dir.
   * If there are multiple root dirs or entries in the root of zip,
   * ZipException is thrown.
   * <p>
   * The output directory must not be a file.
   *
   * @param zip
   *          input ZIP file.
   * @param outputDir
   *          output directory (created automatically if not found).
   * @param mapper
   *          call-back for renaming the entries.
   */
  public static void unwrap(File zip, File outputDir, NameMapper mapper) {
    log.debug("Unwrapping '{}' into '{}'.", zip, outputDir);
    iterate(zip, new Unwraper(outputDir, mapper));
  }

  /**
   * Unpacks a ZIP stream to the given directory.
   * <p>
   * The output directory must not be a file.
   *
   * @param is
   *          inputstream for ZIP file.
   * @param outputDir
   *          output directory (created automatically if not found).
   */
  public static void unpack(InputStream is, File outputDir) {
    unpack(is, outputDir, IdentityNameMapper.INSTANCE, null);
  }

  /**
   * Unpacks a ZIP stream to the given directory.
   * <p>
   * The output directory must not be a file.
   *
   * @param is
   *          inputstream for ZIP file.
   * @param outputDir
   *          output directory (created automatically if not found).
   * @param charset
   *          charset used to process the zip stream
   */
  public static void unpack(InputStream is, File outputDir, Charset charset) {
    unpack(is, outputDir, IdentityNameMapper.INSTANCE, charset);
  }

  /**
   * Unpacks a ZIP stream to the given directory.
   * <p>
   * The output directory must not be a file.
   *
   * @param is
   *          inputstream for ZIP file.
   * @param outputDir
   *          output directory (created automatically if not found).
   * @param mapper
   *          call-back for renaming the entries.
   */
  public static void unpack(InputStream is, File outputDir, NameMapper mapper) {
    unpack(is, outputDir, mapper, null);
  }

  /**
   * Unpacks a ZIP stream to the given directory.
   * <p>
   * The output directory must not be a file.
   *
   * @param is
   *          inputstream for ZIP file.
   * @param outputDir
   *          output directory (created automatically if not found).
   * @param mapper
   *          call-back for renaming the entries.
   * @param charset
   *          charset to use when unpacking the stream
   */
  public static void unpack(InputStream is, File outputDir, NameMapper mapper, Charset charset) {
    log.debug("Extracting {} into '{}'.", is, outputDir);
    iterate(is, new Unpacker(outputDir, mapper), charset);
  }

  /**
   * Unwraps a ZIP file to the given directory shaving of root dir.
   * If there are multiple root dirs or entries in the root of zip,
   * ZipException is thrown.
   * <p>
   * The output directory must not be a file.
   *
   * @param is
   *          inputstream for ZIP file.
   * @param outputDir
   *          output directory (created automatically if not found).
   */
  public static void unwrap(InputStream is, File outputDir) {
    unwrap(is, outputDir, IdentityNameMapper.INSTANCE);
  }

  /**
   * Unwraps a ZIP file to the given directory shaving of root dir.
   * If there are multiple root dirs or entries in the root of zip,
   * ZipException is thrown.
   * <p>
   * The output directory must not be a file.
   *
   * @param is
   *          inputstream for ZIP file.
   * @param outputDir
   *          output directory (created automatically if not found).
   * @param mapper
   *          call-back for renaming the entries.
   */
  public static void unwrap(InputStream is, File outputDir, NameMapper mapper) {
    log.debug("Unwrapping {} into '{}'.", is, outputDir);
    iterate(is, new Unwraper(outputDir, mapper));
  }

  /**
   * Unpacks each ZIP entry.
   *
   * @author Rein Raudjärv
   */
  private static class Unpacker implements ZipEntryCallback {

    private final File outputDir;
    private final NameMapper mapper;

    public Unpacker(File outputDir, NameMapper mapper) {
      this.outputDir = outputDir;
      this.mapper = mapper;
    }

    public void process(InputStream in, ZipEntry zipEntry) throws IOException {
      String name = mapper.map(zipEntry.getName());
      if (name != null) {
        File file = new File(outputDir, name);

        /* If we see the relative traversal string of ".." we need to make sure
         * that the outputdir + name doesn't leave the outputdir. See
         * DirectoryTraversalMaliciousTest for details.
         */
        if (name.indexOf("..") != -1 && !file.getCanonicalPath().startsWith(outputDir.getCanonicalPath())) {
          throw new ZipException("The file "+name+" is trying to leave the target output directory of "+outputDir+". Ignoring this file.");
        }

        if (zipEntry.isDirectory()) {
          FileUtils.forceMkdir(file);
        }
        else {
          FileUtils.forceMkdir(file.getParentFile());

          if (log.isDebugEnabled() && file.exists()) {
            log.debug("Overwriting file '{}'.", zipEntry.getName());
          }

          FileUtils.copy(in, file);
        }

        ZTFilePermissions permissions = ZipEntryUtil.getZTFilePermissions(zipEntry);
        if (permissions != null) {
          ZTFilePermissionsUtil.getDefaultStategy().setPermissions(file, permissions);
        }
      }
    }
  }

  /**
   * Unpacks each ZIP entries. Presumes they are packed with the backslash separator.
   * Some archives can have this problem if they are created with some software
   * that is not following the ZIP specification.
   *
   * @since zt-zip 1.9
   */
  public static class BackslashUnpacker implements ZipEntryCallback {

    private final File outputDir;
    private final NameMapper mapper;

    public BackslashUnpacker(File outputDir, NameMapper mapper) {
      this.outputDir = outputDir;
      this.mapper = mapper;
    }

    public BackslashUnpacker(File outputDir) {
      this(outputDir, IdentityNameMapper.INSTANCE);
    }

    public void process(InputStream in, ZipEntry zipEntry) throws IOException {
      String name = mapper.map(zipEntry.getName());
      if (name != null) {
        /**
         * We assume that EVERY backslash will denote a directory
         * separator. Also such broken archives don't have entries that
         * are just directories. Everything is a file. See the example
         *
         * Archive: backSlashTest.zip
         * testing: testDirectory\testfileInTestDirectory.txt OK
         * testing: testDirectory\testSubdirectory\testFileInTestSubdirectory.txt OK
         * No errors detected in compressed data of backSlashTest.zip.
         */
        if (name.indexOf('\\') != -1) {
          File parentDirectory = outputDir;
          String[] dirs = name.split("\\\\");

          // lets create all the directories and the last entry is the file as EVERY entry is a file
          for (int i = 0; i < dirs.length - 1; i++) {
            File file = new File(parentDirectory, dirs[i]);
            if (!file.exists()) {
              FileUtils.forceMkdir(file);
            }
            parentDirectory = file;
          }
          File destFile = new File(parentDirectory, dirs[dirs.length - 1]);

          /* If we see the relative traversal string of ".." we need to make sure
           * that the outputdir + name doesn't leave the outputdir. See
           * DirectoryTraversalMaliciousTest for details.
           */
          if (name.indexOf("..") != -1 && !destFile.getCanonicalPath().startsWith(outputDir.getCanonicalPath())) {
            throw new ZipException("The file "+name+" is trying to leave the target output directory of "+outputDir+". Ignoring this file.");
          }

          FileUtils.copy(in, destFile);
        }
        // it could be that there are just top level files that the unpacker is used for
        else {
          File destFile = new File(outputDir, name);

          /* If we see the relative traversal string of ".." we need to make sure
           * that the outputdir + name doesn't leave the outputdir. See
           * DirectoryTraversalMaliciousTest for details.
           */
          if (name.indexOf("..") != -1 && !destFile.getCanonicalPath().startsWith(outputDir.getCanonicalPath())) {
            throw new ZipException("The file "+name+" is trying to leave the target output directory of "+outputDir+". Ignoring this file.");
          }

          FileUtils.copy(in, destFile);
        }
      }
    }
  }

  /**
   * Unwraps entries excluding a single parent dir. If there are multiple roots
   * ZipException is thrown.
   *
   * @author Oleg Shelajev
   */
  private static class Unwraper implements ZipEntryCallback {

    private final File outputDir;
    private final NameMapper mapper;
    private String rootDir;

    public Unwraper(File outputDir, NameMapper mapper) {
      this.outputDir = outputDir;
      this.mapper = mapper;
    }

    public void process(InputStream in, ZipEntry zipEntry) throws IOException {
      String root = getRootName(zipEntry.getName());
      if (rootDir == null) {
        rootDir = root;
      }
      else if (!rootDir.equals(root)) {
        throw new ZipException("Unwrapping with multiple roots is not supported, roots: " + rootDir + ", " + root);
      }

      String name = mapper.map(getUnrootedName(root, zipEntry.getName()));
      if (name != null) {
        File file = new File(outputDir, name);

        /* If we see the relative traversal string of ".." we need to make sure
         * that the outputdir + name doesn't leave the outputdir. See
         * DirectoryTraversalMaliciousTest for details.
         */
        if (name.indexOf("..") != -1 && !file.getCanonicalPath().startsWith(outputDir.getCanonicalPath())) {
          throw new ZipException("The file "+name+" is trying to leave the target output directory of "+outputDir+". Ignoring this file.");
        }

        if (zipEntry.isDirectory()) {
          FileUtils.forceMkdir(file);
        }
        else {
          FileUtils.forceMkdir(file.getParentFile());

          if (log.isDebugEnabled() && file.exists()) {
            log.debug("Overwriting file '{}'.", zipEntry.getName());
          }

          FileUtils.copy(in, file);
        }
      }
    }

    private String getUnrootedName(String root, String name) {
      return name.substring(root.length());
    }

    private String getRootName(final String name) {
      String newName = name.substring(FilenameUtils.getPrefixLength(name));
      int idx = newName.indexOf(PATH_SEPARATOR);
      if (idx < 0) {
        throw new ZipException("Entry " + newName + " from the root of the zip is not supported");
      }
      return newName.substring(0, newName.indexOf(PATH_SEPARATOR));
    }
  }

  /**
   * Unpacks a ZIP file to its own location.
   * <p>
   * The ZIP file will be first renamed (using a temporary name). After the
   * extraction it will be deleted.
   *
   * @param zip
   *          input ZIP file as well as the target directory.
   *
   * @see #unpack(File, File)
   */
  public static void explode(File zip) {
    try {
      // Find a new unique name is the same directory
      File tempFile = FileUtils.getTempFileFor(zip);

      // Rename the archive
      FileUtils.moveFile(zip, tempFile);

      // Unpack it
      unpack(tempFile, zip);

      // Delete the archive
      if (!tempFile.delete()) {
        throw new IOException("Unable to delete file: " + tempFile);
      }
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
  }

  /* Compressing single entries to ZIP files. */

  /**
   * Compresses the given file into a ZIP file with single entry.
   *
   * @param file file to be compressed.
   * @return ZIP file created.
   */
  public static byte[] packEntry(File file) {
    log.trace("Compressing '{}' into a ZIP file with single entry.", file);

    ByteArrayOutputStream result = new ByteArrayOutputStream();
    try {
      ZipOutputStream out = new ZipOutputStream(result);
      ZipEntry entry = ZipEntryUtil.fromFile(file.getName(), file);
      InputStream in = new BufferedInputStream(new FileInputStream(file));
      try {
        ZipEntryUtil.addEntry(entry, in, out);
      }
      finally {
        IOUtils.closeQuietly(in);
      }
      out.close();
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
    return result.toByteArray();
  }

  /* Compressing ZIP files. */

  /**
   * Compresses the given directory and all its sub-directories into a ZIP file.
   * <p>
   * The ZIP file must not be a directory and its parent directory must exist.
   * Will not include the root directory name in the archive.
   *
   * @param rootDir
   *          root directory.
   * @param zip
   *          ZIP file that will be created or overwritten.
   */
  public static void pack(File rootDir, File zip) {
    pack(rootDir, zip, DEFAULT_COMPRESSION_LEVEL);
  }

  /**
   * Compresses the given directory and all its sub-directories into a ZIP file.
   * <p>
   * The ZIP file must not be a directory and its parent directory must exist.
   * Will not include the root directory name in the archive.
   *
   * @param rootDir
   *          root directory.
   * @param zip
   *          ZIP file that will be created or overwritten.
   * @param compressionLevel
   *          compression level
   */
  public static void pack(File rootDir, File zip, int compressionLevel) {
    pack(rootDir, zip, IdentityNameMapper.INSTANCE, compressionLevel);
  }

  /**
   * Compresses the given directory and all its sub-directories into a ZIP file.
   * <p>
   * The ZIP file must not be a directory and its parent directory must exist.
   * Will not include the root directory name in the archive.
   *
   * @param sourceDir
   *          root directory.
   * @param targetZipFile
   *          ZIP file that will be created or overwritten.
   * @param preserveRoot
   *          true if the resulted archive should have the top directory entry
   */
  public static void pack(final File sourceDir, final File targetZipFile, final boolean preserveRoot) {
    if (preserveRoot) {
      final String parentName = sourceDir.getName();
      pack(sourceDir, targetZipFile, new NameMapper() {
        public String map(String name) {
          return parentName + PATH_SEPARATOR + name;
        }
      });
    }
    else {
      pack(sourceDir, targetZipFile);
    }
  }

  /**
   * Compresses the given file into a ZIP file.
   * <p>
   * The ZIP file must not be a directory and its parent directory must exist.
   *
   * @param fileToPack
   *          file that needs to be zipped.
   * @param destZipFile
   *          ZIP file that will be created or overwritten.
   */
  public static void packEntry(File fileToPack, File destZipFile) {
    packEntry(fileToPack, destZipFile, IdentityNameMapper.INSTANCE);
  }

  /**
   * Compresses the given file into a ZIP file.
   * <p>
   * The ZIP file must not be a directory and its parent directory must exist.
   *
   * @param fileToPack
   *          file that needs to be zipped.
   * @param destZipFile
   *          ZIP file that will be created or overwritten.
   * @param fileName
   *          the name for the file inside the archive
   */
  public static void packEntry(File fileToPack, File destZipFile, final String fileName) {
    packEntry(fileToPack, destZipFile, new NameMapper() {
      public String map(String name) {
        return fileName;
      }
    });
  }

  /**
   * Compresses the given file into a ZIP file.
   * <p>
   * The ZIP file must not be a directory and its parent directory must exist.
   *
   * @param fileToPack
   *          file that needs to be zipped.
   * @param destZipFile
   *          ZIP file that will be created or overwritten.
   * @param mapper
   *          call-back for renaming the entries.
   */
  public static void packEntry(File fileToPack, File destZipFile, NameMapper mapper) {
    packEntries(new File[] { fileToPack }, destZipFile, mapper);
  }

  /**
   * Compresses the given files into a ZIP file.
   * <p>
   * The ZIP file must not be a directory and its parent directory must exist.
   *
   * @param filesToPack
   *          files that needs to be zipped.
   * @param destZipFile
   *          ZIP file that will be created or overwritten.
   */
  public static void packEntries(File[] filesToPack, File destZipFile) {
    packEntries(filesToPack, destZipFile, IdentityNameMapper.INSTANCE);
  }

  /**
   * Compresses the given files into a ZIP file.
   * <p>
   * The ZIP file must not be a directory and its parent directory must exist.
   *
   * @param filesToPack
   *          files that needs to be zipped.
   * @param destZipFile
   *          ZIP file that will be created or overwritten.
   * @param mapper
   *          call-back for renaming the entries.
   */
  public static void packEntries(File[] filesToPack, File destZipFile, NameMapper mapper) {
    packEntries(filesToPack, destZipFile, mapper, DEFAULT_COMPRESSION_LEVEL);
  }

  /**
   * Compresses the given files into a ZIP file.
   * <p>
   * The ZIP file must not be a directory and its parent directory must exist.
   *
   * @param filesToPack
   *          files that needs to be zipped.
   * @param destZipFile
   *          ZIP file that will be created or overwritten.
   * @param compressionLevel
   *          ZIP file compression level (speed versus filesize), e.g. <code>Deflater.NO_COMPRESSION</code>, <code>Deflater.BEST_SPEED</code>, or
   *          <code>Deflater.BEST_COMPRESSION</code>
   */
  public static void packEntries(File[] filesToPack, File destZipFile, int compressionLevel) {
    packEntries(filesToPack, destZipFile, IdentityNameMapper.INSTANCE, compressionLevel);
  }

  /**
   * Compresses the given files into a ZIP file.
   * <p>
   * The ZIP file must not be a directory and its parent directory must exist.
   *
   * @param filesToPack
   *          files that needs to be zipped.
   * @param destZipFile
   *          ZIP file that will be created or overwritten.
   * @param mapper
   *          call-back for renaming the entries.
   * @param compressionLevel
   *          ZIP file compression level (speed versus filesize), e.g. <code>Deflater.NO_COMPRESSION</code>, <code>Deflater.BEST_SPEED</code>, or
   *          <code>Deflater.BEST_COMPRESSION</code>
   */
  public static void packEntries(File[] filesToPack, File destZipFile, NameMapper mapper, int compressionLevel) {
    log.debug("Compressing '{}' into '{}'.", filesToPack, destZipFile);

    ZipOutputStream out = null;
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(destZipFile);
      out = new ZipOutputStream(new BufferedOutputStream(fos));
      out.setLevel(compressionLevel);

      for (int i = 0; i < filesToPack.length; i++) {
        File fileToPack = filesToPack[i];

        ZipEntry zipEntry = ZipEntryUtil.fromFile(mapper.map(fileToPack.getName()), fileToPack);
        out.putNextEntry(zipEntry);
        FileUtils.copy(fileToPack, out);
        out.closeEntry();
      }
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
    finally {
      IOUtils.closeQuietly(out);
      IOUtils.closeQuietly(fos);
    }
  }

  /**
   * Compresses the given directory and all its sub-directories into a ZIP file.
   * <p>
   * The ZIP file must not be a directory and its parent directory must exist.
   *
   * @param sourceDir
   *          root directory.
   * @param targetZip
   *          ZIP file that will be created or overwritten.
   * @param mapper
   *          call-back for renaming the entries.
   */
  public static void pack(File sourceDir, File targetZip, NameMapper mapper) {
    pack(sourceDir, targetZip, mapper, DEFAULT_COMPRESSION_LEVEL);
  }

  /**
   * Compresses the given directory and all its sub-directories into a ZIP file.
   * <p>
   * The ZIP file must not be a directory and its parent directory must exist.
   *
   * @param sourceDir
   *          root directory.
   * @param targetZip
   *          ZIP file that will be created or overwritten.
   * @param mapper
   *          call-back for renaming the entries.
   * @param compressionLevel
   *          compression level
   */
  public static void pack(File sourceDir, File targetZip, NameMapper mapper, int compressionLevel) {
    log.debug("Compressing '{}' into '{}'.", sourceDir, targetZip);
    if (!sourceDir.exists()) {
      throw new ZipException("Given file '" + sourceDir + "' doesn't exist!");
    }
    ZipOutputStream out = null;
    try {
      out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(targetZip)));
      out.setLevel(compressionLevel);
      pack(sourceDir, out, mapper, "", true);
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
    finally {
      IOUtils.closeQuietly(out);
    }
  }

  /**
   * Compresses the given directory and all of its sub-directories into the passed in
   * stream. It is the responsibility of the caller to close the passed in
   * stream properly.
   *
   * @param sourceDir
   *          root directory.
   * @param os
   *          output stream (will be buffered in this method).
   *
   * @since 1.10
   */
  public static void pack(File sourceDir, OutputStream os) {
    pack(sourceDir, os, IdentityNameMapper.INSTANCE, DEFAULT_COMPRESSION_LEVEL);
  }

  /**
   * Compresses the given directory and all of its sub-directories into the passed in
   * stream. It is the responsibility of the caller to close the passed in
   * stream properly.
   *
   * @param sourceDir
   *          root directory.
   * @param os
   *          output stream (will be buffered in this method).
   * @param compressionLevel
   *          compression level
   *
   * @since 1.10
   */
  public static void pack(File sourceDir, OutputStream os, int compressionLevel) {
    pack(sourceDir, os, IdentityNameMapper.INSTANCE, compressionLevel);
  }

  /**
   * Compresses the given directory and all of its sub-directories into the passed in
   * stream. It is the responsibility of the caller to close the passed in
   * stream properly.
   *
   * @param sourceDir
   *          root directory.
   * @param os
   *          output stream (will be buffered in this method).
   * @param mapper
   *          call-back for renaming the entries.
   *
   * @since 1.10
   */
  public static void pack(File sourceDir, OutputStream os, NameMapper mapper) {
    pack(sourceDir, os, mapper, DEFAULT_COMPRESSION_LEVEL);
  }

  /**
   * Compresses the given directory and all of its sub-directories into the passed in
   * stream. It is the responsibility of the caller to close the passed in
   * stream properly.
   *
   * @param sourceDir
   *          root directory.
   * @param os
   *          output stream (will be buffered in this method).
   * @param mapper
   *          call-back for renaming the entries.
   * @param compressionLevel
   *          compression level
   *
   * @since 1.10
   */
  public static void pack(File sourceDir, OutputStream os, NameMapper mapper, int compressionLevel) {
    log.debug("Compressing '{}' into a stream.", sourceDir);
    if (!sourceDir.exists()) {
      throw new ZipException("Given file '" + sourceDir + "' doesn't exist!");
    }
    ZipOutputStream out = null;
    IOException error = null;
    try {
      out = new ZipOutputStream(new BufferedOutputStream(os));
      out.setLevel(compressionLevel);
      pack(sourceDir, out, mapper, "", true);
    }
    catch (IOException e) {
      error = e;
    }
    finally {
      if (out != null && error == null) {
        try {
          out.finish();
          out.flush();
        }
        catch (IOException e) {
          error = e;
        }
      }
    }
    if (error != null) {
      throw ZipExceptionUtil.rethrow(error);
    }
  }

  /**
   * Compresses the given directory and all its sub-directories into a ZIP file.
   *
   * @param dir
   *          root directory.
   * @param out
   *          ZIP output stream.
   * @param mapper
   *          call-back for renaming the entries.
   * @param pathPrefix
   *          prefix to be used for the entries.
   * @param mustHaveChildren
   *          if true, but directory to pack doesn't have any files, throw an exception.
   */
  private static void pack(File dir, ZipOutputStream out, NameMapper mapper, String pathPrefix, boolean mustHaveChildren) throws IOException {
    String[] filenames = dir.list();
    if (filenames == null) {
      if (!dir.exists()) {
        throw new ZipException("Given file '" + dir + "' doesn't exist!");
      }
      throw new IOException("Given file is not a directory '" + dir + "'");
    }

    if (mustHaveChildren && filenames.length == 0) {
      throw new ZipException("Given directory '" + dir + "' doesn't contain any files!");
    }

    for (int i = 0; i < filenames.length; i++) {
      String filename = filenames[i];
      File file = new File(dir, filename);
      boolean isDir = file.isDirectory();
      String path = pathPrefix + file.getName(); // NOSONAR
      if (isDir) {
        path += PATH_SEPARATOR; // NOSONAR
      }

      // Create a ZIP entry
      String name = mapper.map(path);
      if (name != null) {
        ZipEntry zipEntry = ZipEntryUtil.fromFile(name, file);

        out.putNextEntry(zipEntry);

        // Copy the file content
        if (!isDir) {
          FileUtils.copy(file, out);
        }

        out.closeEntry();
      }

      // Traverse the directory
      if (isDir) {
        pack(file, out, mapper, path, false);
      }
    }
  }

  /**
   * Repacks a provided ZIP file into a new ZIP with a given compression level.
   * <p>
   *
   * @param srcZip
   *          source ZIP file.
   * @param dstZip
   *          destination ZIP file.
   * @param compressionLevel
   *          compression level.
   */
  public static void repack(File srcZip, File dstZip, int compressionLevel) {

    log.debug("Repacking '{}' into '{}'.", srcZip, dstZip);

    RepackZipEntryCallback callback = new RepackZipEntryCallback(dstZip, compressionLevel);

    try {
      iterate(srcZip, callback);
    }
    finally {
      callback.closeStream();
    }
  }

  /**
   * Repacks a provided ZIP input stream into a ZIP file with a given compression level.
   * <p>
   *
   * @param is
   *          ZIP input stream.
   * @param dstZip
   *          destination ZIP file.
   * @param compressionLevel
   *          compression level.
   */
  public static void repack(InputStream is, File dstZip, int compressionLevel) {

    log.debug("Repacking from input stream into '{}'.", dstZip);

    RepackZipEntryCallback callback = new RepackZipEntryCallback(dstZip, compressionLevel);

    try {
      iterate(is, callback);
    }
    finally {
      callback.closeStream();
    }
  }

  /**
   * Repacks a provided ZIP file and replaces old file with the new one.
   * <p>
   *
   * @param zip
   *          source ZIP file to be repacked and replaced.
   * @param compressionLevel
   *          compression level.
   */
  public static void repack(File zip, int compressionLevel) {
    try {
      File tmpZip = FileUtils.getTempFileFor(zip);

      repack(zip, tmpZip, compressionLevel);

      // Delete original zip
      if (!zip.delete()) {
        throw new IOException("Unable to delete the file: " + zip);
      }

      // Rename the archive
      FileUtils.moveFile(tmpZip, zip);
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
  }

  /**
   * RepackZipEntryCallback used in repacking methods.
   *
   * @author Pavel Grigorenko
   */
  private static final class RepackZipEntryCallback implements ZipEntryCallback {

    private ZipOutputStream out;

    private RepackZipEntryCallback(File dstZip, int compressionLevel) {
      try {
        this.out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(dstZip)));
        this.out.setLevel(compressionLevel);
      }
      catch (IOException e) {
        ZipExceptionUtil.rethrow(e);
      }
    }

    public void process(InputStream in, ZipEntry zipEntry) throws IOException {
      ZipEntryUtil.copyEntry(zipEntry, in, out);
    }

    private void closeStream() {
      IOUtils.closeQuietly(out);
    }
  }

  /**
   * Compresses a given directory in its own location.
   * <p>
   * A ZIP file will be first created with a temporary name. After the
   * compressing the directory will be deleted and the ZIP file will be renamed
   * as the original directory.
   *
   * @param dir
   *          input directory as well as the target ZIP file.
   *
   * @see #pack(File, File)
   */
  public static void unexplode(File dir) {
    unexplode(dir, DEFAULT_COMPRESSION_LEVEL);
  }

  /**
   * Compresses a given directory in its own location.
   * <p>
   * A ZIP file will be first created with a temporary name. After the
   * compressing the directory will be deleted and the ZIP file will be renamed
   * as the original directory.
   *
   * @param dir
   *          input directory as well as the target ZIP file.
   * @param compressionLevel
   *          compression level
   *
   * @see #pack(File, File)
   */
  public static void unexplode(File dir, int compressionLevel) {
    try {
      // Find a new unique name is the same directory
      File zip = FileUtils.getTempFileFor(dir);

      // Pack it
      pack(dir, zip, compressionLevel);

      // Delete the directory
      FileUtils.deleteDirectory(dir);

      // Rename the archive
      FileUtils.moveFile(zip, dir);
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
  }

  /**
   * Compresses the given entries into an output stream.
   *
   * @param entries
   *          ZIP entries added.
   * @param os
   *          output stream for the new ZIP (does not have to be buffered)
   *
   * @since 1.9
   */
  public static void pack(ZipEntrySource[] entries, OutputStream os) {
    if (log.isDebugEnabled()) {
      log.debug("Creating stream from {}.", Arrays.asList(entries));
    }
    pack(entries, os, false);
  }

  private static void pack(ZipEntrySource[] entries, OutputStream os, boolean closeStream) {
    try {
      ZipOutputStream out = new ZipOutputStream(os);
      for (int i = 0; i < entries.length; i++) {
        addEntry(entries[i], out);
      }
      out.flush();
      out.finish();
      if (closeStream) {
        out.close();
      }
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
  }

  /**
   * Compresses the given entries into a new ZIP file.
   *
   * @param entries
   *          ZIP entries added.
   * @param zip
   *          new ZIP file created.
   */
  public static void pack(ZipEntrySource[] entries, File zip) {
    if (log.isDebugEnabled()) {
      log.debug("Creating '{}' from {}.", zip, Arrays.asList(entries));
    }

    OutputStream out = null;
    try {
      out = new BufferedOutputStream(new FileOutputStream(zip));
      pack(entries, out, true);
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
    finally {
      IOUtils.closeQuietly(out);
    }
  }

  /**
   * Copies an existing ZIP file and appends it with one new entry.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param path
   *          new ZIP entry path.
   * @param file
   *          new entry to be added.
   * @param destZip
   *          new ZIP file created.
   */
  public static void addEntry(File zip, String path, File file, File destZip) {
    addEntry(zip, new FileSource(path, file), destZip);
  }

  /**
   * Changes a zip file, adds one new entry in-place.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param path
   *          new ZIP entry path.
   * @param file
   *          new entry to be added.
   */
  public static void addEntry(final File zip, final String path, final File file) {
    operateInPlace(zip, new InPlaceAction() {
      public boolean act(File tmpFile) {
        addEntry(zip, path, file, tmpFile);
        return true;
      }
    });
  }

  /**
   * Copies an existing ZIP file and appends it with one new entry.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param path
   *          new ZIP entry path.
   * @param bytes
   *          new entry bytes (or <code>null</code> if directory).
   * @param destZip
   *          new ZIP file created.
   */
  public static void addEntry(File zip, String path, byte[] bytes, File destZip) {
    addEntry(zip, new ByteSource(path, bytes), destZip);
  }

  /**
   * Copies an existing ZIP file and appends it with one new entry.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param path
   *          new ZIP entry path.
   * @param bytes
   *          new entry bytes (or <code>null</code> if directory).
   * @param destZip
   *          new ZIP file created.
   * @param compressionMethod
   *          the new compression method (<code>ZipEntry.STORED</code> or <code>ZipEntry.DEFLATED</code>).
   */
  public static void addEntry(File zip, String path, byte[] bytes, File destZip, final int compressionMethod) {
    addEntry(zip, new ByteSource(path, bytes, compressionMethod), destZip);
  }

  /**
   * Changes a zip file, adds one new entry in-place.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param path
   *          new ZIP entry path.
   * @param bytes
   *          new entry bytes (or <code>null</code> if directory).
   */
  public static void addEntry(final File zip, final String path, final byte[] bytes) {
    operateInPlace(zip, new InPlaceAction() {
      public boolean act(File tmpFile) {
        addEntry(zip, path, bytes, tmpFile);
        return true;
      }
    });
  }

  /**
   * Changes a zip file, adds one new entry in-place.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param path
   *          new ZIP entry path.
   * @param bytes
   *          new entry bytes (or <code>null</code> if directory).
   * @param compressionMethod
   *          the new compression method (<code>ZipEntry.STORED</code> or <code>ZipEntry.DEFLATED</code>).
   */
  public static void addEntry(final File zip, final String path, final byte[] bytes, final int compressionMethod) {
    operateInPlace(zip, new InPlaceAction() {
      public boolean act(File tmpFile) {
        addEntry(zip, path, bytes, tmpFile, compressionMethod);
        return true;
      }
    });
  }

  /**
   * Copies an existing ZIP file and appends it with one new entry.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param entry
   *          new ZIP entry appended.
   * @param destZip
   *          new ZIP file created.
   */
  public static void addEntry(File zip, ZipEntrySource entry, File destZip) {
    addEntries(zip, new ZipEntrySource[] { entry }, destZip);
  }

  /**
   * Changes a zip file, adds one new entry in-place.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param entry
   *          new ZIP entry appended.
   */
  public static void addEntry(final File zip, final ZipEntrySource entry) {
    operateInPlace(zip, new InPlaceAction() {
      public boolean act(File tmpFile) {
        addEntry(zip, entry, tmpFile);
        return true;
      }
    });
  }

  /**
   * Copies an existing ZIP file and appends it with new entries.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param entries
   *          new ZIP entries appended.
   * @param destZip
   *          new ZIP file created.
   */
  public static void addEntries(File zip, ZipEntrySource[] entries, File destZip) {
    if (log.isDebugEnabled()) {
      log.debug("Copying '" + zip + "' to '" + destZip + "' and adding " + Arrays.asList(entries) + ".");
    }

    OutputStream destOut = null;
    try {
      destOut = new BufferedOutputStream(new FileOutputStream(destZip));
      addEntries(zip, entries, destOut);
    }
    catch (IOException e) {
      ZipExceptionUtil.rethrow(e);
    }
    finally {
      IOUtils.closeQuietly(destOut);
    }
  }

  /**
   * Copies an existing ZIP file and appends it with new entries.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param entries
   *          new ZIP entries appended.
   * @param destOut
   *          new ZIP destination output stream
   */
  public static void addEntries(File zip, ZipEntrySource[] entries, OutputStream destOut) {
    if (log.isDebugEnabled()) {
      log.debug("Copying '" + zip + "' to a stream and adding " + Arrays.asList(entries) + ".");
    }

    ZipOutputStream out = null;
    try {
      out = new ZipOutputStream(destOut);
      copyEntries(zip, out);
      for (int i = 0; i < entries.length; i++) {
        addEntry(entries[i], out);
      }
      out.finish();
    }
    catch (IOException e) {
      ZipExceptionUtil.rethrow(e);
    }
  }

  /**
   * Copies an existing ZIP file and appends it with new entries.
   *
   * @param is
   *          an existing ZIP input stream.
   * @param entries
   *          new ZIP entries appended.
   * @param destOut
   *          new ZIP destination output stream
   *
   * @since 1.9
   */
  public static void addEntries(InputStream is, ZipEntrySource[] entries, OutputStream destOut) {
    if (log.isDebugEnabled()) {
      log.debug("Copying input stream to an output stream and adding " + Arrays.asList(entries) + ".");
    }

    ZipOutputStream out = null;
    try {
      out = new ZipOutputStream(destOut);
      copyEntries(is, out);
      for (int i = 0; i < entries.length; i++) {
        addEntry(entries[i], out);
      }
      out.finish();
    }
    catch (IOException e) {
      ZipExceptionUtil.rethrow(e);
    }
  }

  /**
   * Changes a zip file it with with new entries. in-place.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param entries
   *          new ZIP entries appended.
   */
  public static void addEntries(final File zip, final ZipEntrySource[] entries) {
    operateInPlace(zip, new InPlaceAction() {
      public boolean act(File tmpFile) {
        addEntries(zip, entries, tmpFile);
        return true;
      }
    });
  }

  /**
   * Copies an existing ZIP file and removes entry with a given path.
   *
   * @param zip
   *          an existing ZIP file (only read)
   * @param path
   *          path of the entry to remove
   * @param destZip
   *          new ZIP file created.
   * @since 1.7
   */
  public static void removeEntry(File zip, String path, File destZip) {
    removeEntries(zip, new String[] { path }, destZip);
  }

  /**
   * Changes an existing ZIP file: removes entry with a given path.
   *
   * @param zip
   *          an existing ZIP file
   * @param path
   *          path of the entry to remove
   * @since 1.7
   */
  public static void removeEntry(final File zip, final String path) {
    operateInPlace(zip, new InPlaceAction() {
      public boolean act(File tmpFile) {
        removeEntry(zip, path, tmpFile);
        return true;
      }
    });
  }

  /**
   * Copies an existing ZIP file and removes entries with given paths.
   *
   * @param zip
   *          an existing ZIP file (only read)
   * @param paths
   *          paths of the entries to remove
   * @param destZip
   *          new ZIP file created.
   * @since 1.7
   */
  public static void removeEntries(File zip, String[] paths, File destZip) {
    if (log.isDebugEnabled()) {
      log.debug("Copying '" + zip + "' to '" + destZip + "' and removing paths " + Arrays.asList(paths) + ".");
    }

    ZipOutputStream out = null;
    try {
      out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destZip)));
      copyEntries(zip, out, new HashSet<String>(Arrays.asList(paths)));
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
    finally {
      IOUtils.closeQuietly(out);
    }
  }

  /**
   * Changes an existing ZIP file: removes entries with given paths.
   *
   * @param zip
   *          an existing ZIP file
   * @param paths
   *          paths of the entries to remove
   * @since 1.7
   */
  public static void removeEntries(final File zip, final String[] paths) {
    operateInPlace(zip, new InPlaceAction() {
      public boolean act(File tmpFile) {
        removeEntries(zip, paths, tmpFile);
        return true;
      }
    });
  }

  /**
   * Copies all entries from one ZIP file to another.
   *
   * @param zip
   *          source ZIP file.
   * @param out
   *          target ZIP stream.
   */
  private static void copyEntries(File zip, final ZipOutputStream out) {
    // this one doesn't call copyEntries with ignoredEntries, because that has poorer performance
    final Set<String> names = new HashSet<String>();
    iterate(zip, new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        String entryName = zipEntry.getName();
        if (names.add(entryName)) {
          ZipEntryUtil.copyEntry(zipEntry, in, out);
        }
        else if (log.isDebugEnabled()) {
          log.debug("Duplicate entry: {}", entryName);
        }
      }
    });
  }

  /**
   * Copies all entries from one ZIP stream to another.
   *
   * @param is
   *          source stream (contains ZIP file).
   * @param out
   *          target ZIP stream.
   */
  private static void copyEntries(InputStream is, final ZipOutputStream out) {
    // this one doesn't call copyEntries with ignoredEntries, because that has poorer performance
    final Set<String> names = new HashSet<String>();
    iterate(is, new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        String entryName = zipEntry.getName();
        if (names.add(entryName)) {
          ZipEntryUtil.copyEntry(zipEntry, in, out);
        }
        else if (log.isDebugEnabled()) {
          log.debug("Duplicate entry: {}", entryName);
        }
      }
    });
  }

  /**
   * Copies all entries from one ZIP file to another, ignoring entries with path in ignoredEntries
   *
   * @param zip
   *          source ZIP file.
   * @param out
   *          target ZIP stream.
   * @param ignoredEntries
   *          paths of entries not to copy
   */
  private static void copyEntries(File zip, final ZipOutputStream out, final Set<String> ignoredEntries) {
    final Set<String> names = new HashSet<String>();
    final Set<String> dirNames = filterDirEntries(zip, ignoredEntries);
    iterate(zip, new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        String entryName = zipEntry.getName();
        if (ignoredEntries.contains(entryName)) {
          return;
        }

        for (String dirName : dirNames) {
          if (entryName.startsWith(dirName)) {
            return;
          }
        }

        if (names.add(entryName)) {
          ZipEntryUtil.copyEntry(zipEntry, in, out);
        }
        else if (log.isDebugEnabled()) {
          log.debug("Duplicate entry: {}", entryName);
        }
      }
    });
  }

  /**
   *
   * @param zip
   *          zip file to traverse
   * @param names
   *          names of entries to filter dirs from
   * @return Set<String> names of entries that are dirs.
   *
   */
  static Set<String> filterDirEntries(File zip, Collection<String> names) {
    Set<String> dirs = new HashSet<String>();
    if (zip == null) {
      return dirs;
    }
    ZipFile zf = null;
    try {
      zf = new ZipFile(zip);
      for (String entryName : names) {
        ZipEntry entry = zf.getEntry(entryName);
        if (entry != null) {
          if (entry.isDirectory()) {
            dirs.add(entry.getName());
          }
          else if (zf.getInputStream(entry) == null) {
            // no input stream means that this is a dir.
            dirs.add(entry.getName() + PATH_SEPARATOR);
          }
        }
      }

    }
    catch (IOException e) {
      ZipExceptionUtil.rethrow(e);
    }
    finally {
      closeQuietly(zf);
    }
    return dirs;
  }

  /**
   * Copies an existing ZIP file and replaces a given entry in it.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param path
   *          new ZIP entry path.
   * @param file
   *          new entry.
   * @param destZip
   *          new ZIP file created.
   * @return <code>true</code> if the entry was replaced.
   */
  public static boolean replaceEntry(File zip, String path, File file, File destZip) {
    return replaceEntry(zip, new FileSource(path, file), destZip);
  }

  /**
   * Changes an existing ZIP file: replaces a given entry in it.
   *
   * @param zip
   *          an existing ZIP file.
   * @param path
   *          new ZIP entry path.
   * @param file
   *          new entry.
   * @return <code>true</code> if the entry was replaced.
   */
  public static boolean replaceEntry(final File zip, final String path, final File file) {
    return operateInPlace(zip, new InPlaceAction() {
      public boolean act(File tmpFile) {
        return replaceEntry(zip, new FileSource(path, file), tmpFile);
      }
    });
  }

  /**
   * Copies an existing ZIP file and replaces a given entry in it.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param path
   *          new ZIP entry path.
   * @param bytes
   *          new entry bytes (or <code>null</code> if directory).
   * @param destZip
   *          new ZIP file created.
   * @return <code>true</code> if the entry was replaced.
   */
  public static boolean replaceEntry(File zip, String path, byte[] bytes, File destZip) {
    return replaceEntry(zip, new ByteSource(path, bytes), destZip);
  }

  /**
   * Changes an existing ZIP file: replaces a given entry in it.
   *
   * @param zip
   *          an existing ZIP file.
   * @param path
   *          new ZIP entry path.
   * @param bytes
   *          new entry bytes (or <code>null</code> if directory).
   * @return <code>true</code> if the entry was replaced.
   */
  public static boolean replaceEntry(final File zip, final String path, final byte[] bytes) {
    return operateInPlace(zip, new InPlaceAction() {
      public boolean act(File tmpFile) {
        return replaceEntry(zip, new ByteSource(path, bytes), tmpFile);
      }
    });
  }

  /**
   * Changes an existing ZIP file: replaces a given entry in it.
   *
   * @param zip
   *          an existing ZIP file.
   * @param path
   *          new ZIP entry path.
   * @param bytes
   *          new entry bytes (or <code>null</code> if directory).
   * @param compressionMethod
   *          the new compression method (<code>ZipEntry.STORED</code> or <code>ZipEntry.DEFLATED</code>).
   * @return <code>true</code> if the entry was replaced.
   */
  public static boolean replaceEntry(final File zip, final String path, final byte[] bytes,
      final int compressionMethod) {
    return operateInPlace(zip, new InPlaceAction() {
      public boolean act(File tmpFile) {
        return replaceEntry(zip, new ByteSource(path, bytes, compressionMethod), tmpFile);
      }
    });
  }

  /**
   * Copies an existing ZIP file and replaces a given entry in it.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param entry
   *          new ZIP entry.
   * @param destZip
   *          new ZIP file created.
   * @return <code>true</code> if the entry was replaced.
   */
  public static boolean replaceEntry(File zip, ZipEntrySource entry, File destZip) {
    return replaceEntries(zip, new ZipEntrySource[] { entry }, destZip);
  }

  /**
   * Changes an existing ZIP file: replaces a given entry in it.
   *
   * @param zip
   *          an existing ZIP file.
   * @param entry
   *          new ZIP entry.
   * @return <code>true</code> if the entry was replaced.
   */
  public static boolean replaceEntry(final File zip, final ZipEntrySource entry) {
    return operateInPlace(zip, new InPlaceAction() {
      public boolean act(File tmpFile) {
        return replaceEntry(zip, entry, tmpFile);
      }
    });
  }

  /**
   * Copies an existing ZIP file and replaces the given entries in it.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param entries
   *          new ZIP entries to be replaced with.
   * @param destZip
   *          new ZIP file created.
   * @return <code>true</code> if at least one entry was replaced.
   */
  public static boolean replaceEntries(File zip, ZipEntrySource[] entries, File destZip) {
    if (log.isDebugEnabled()) {
      log.debug("Copying '" + zip + "' to '" + destZip + "' and replacing entries " + Arrays.asList(entries) + ".");
    }

    final Map<String, ZipEntrySource> entryByPath = entriesByPath(entries);
    final int entryCount = entryByPath.size();
    try {
      final ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destZip)));
      try {
        final Set<String> names = new HashSet<String>();
        iterate(zip, new ZipEntryCallback() {
          public void process(InputStream in, ZipEntry zipEntry) throws IOException {
            if (names.add(zipEntry.getName())) {
              ZipEntrySource entry = (ZipEntrySource) entryByPath.remove(zipEntry.getName());
              if (entry != null) {
                addEntry(entry, out);
              }
              else {
                ZipEntryUtil.copyEntry(zipEntry, in, out);
              }
            }
            else if (log.isDebugEnabled()) {
              log.debug("Duplicate entry: {}", zipEntry.getName());
            }
          }
        });
      }
      finally {
        IOUtils.closeQuietly(out);
      }
    }
    catch (IOException e) {
      ZipExceptionUtil.rethrow(e);
    }
    return entryByPath.size() < entryCount;
  }

  /**
   * Changes an existing ZIP file: replaces a given entry in it.
   *
   * @param zip
   *          an existing ZIP file.
   * @param entries
   *          new ZIP entries to be replaced with.
   * @return <code>true</code> if at least one entry was replaced.
   */
  public static boolean replaceEntries(final File zip, final ZipEntrySource[] entries) {
    return operateInPlace(zip, new InPlaceAction() {
      public boolean act(File tmpFile) {
        return replaceEntries(zip, entries, tmpFile);
      }
    });
  }

  /**
   * Copies an existing ZIP file and adds/replaces the given entries in it.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param entries
   *          ZIP entries to be replaced or added.
   * @param destZip
   *          new ZIP file created.
   */
  public static void addOrReplaceEntries(File zip, ZipEntrySource[] entries, File destZip) {
    if (log.isDebugEnabled()) {
      log.debug("Copying '" + zip + "' to '" + destZip + "' and adding/replacing entries " + Arrays.asList(entries)
          + ".");
    }

    final Map<String, ZipEntrySource> entryByPath = entriesByPath(entries);
    try {
      final ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destZip)));
      try {
        // Copy and replace entries
        final Set<String> names = new HashSet<String>();
        iterate(zip, new ZipEntryCallback() {
          public void process(InputStream in, ZipEntry zipEntry) throws IOException {
            if (names.add(zipEntry.getName())) {
              ZipEntrySource entry = (ZipEntrySource) entryByPath.remove(zipEntry.getName());
              if (entry != null) {
                addEntry(entry, out);
              }
              else {
                ZipEntryUtil.copyEntry(zipEntry, in, out);
              }
            }
            else if (log.isDebugEnabled()) {
              log.debug("Duplicate entry: {}", zipEntry.getName());
            }
          }
        });

        // Add new entries
        for (ZipEntrySource zipEntrySource : entryByPath.values()) {
          addEntry(zipEntrySource, out);
        }
      }
      finally {
        IOUtils.closeQuietly(out);
      }
    }
    catch (IOException e) {
      ZipExceptionUtil.rethrow(e);
    }
  }

  /**
   * Changes a ZIP file: adds/replaces the given entries in it.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param entries
   *          ZIP entries to be replaced or added.
   */
  public static void addOrReplaceEntries(final File zip, final ZipEntrySource[] entries) {
    operateInPlace(zip, new InPlaceAction() {
      public boolean act(File tmpFile) {
        addOrReplaceEntries(zip, entries, tmpFile);
        return true;
      }
    });
  }

  /**
   * @return given entries indexed by path.
   */
  static Map<String, ZipEntrySource> entriesByPath(ZipEntrySource... entries) {
    Map<String, ZipEntrySource> result = new HashMap<String, ZipEntrySource>();
    for (int i = 0; i < entries.length; i++) {
      ZipEntrySource source = entries[i];
      result.put(source.getPath(), source);
    }
    return result;
  }

  /**
   * Copies an existing ZIP file and transforms a given entry in it.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param path
   *          new ZIP entry path.
   * @param transformer
   *          transformer for the given ZIP entry.
   * @param destZip
   *          new ZIP file created.
   * @throws IllegalArgumentException if the destination is the same as the location
   * @return <code>true</code> if the entry was replaced.
   */
  public static boolean transformEntry(File zip, String path, ZipEntryTransformer transformer, File destZip) {
    if(zip.equals(destZip)){throw new IllegalArgumentException("Input (" +zip.getAbsolutePath()+ ") is the same as the destination!" +
            "Please use the transformEntry method without destination for in-place transformation." );}
    return transformEntry(zip, new ZipEntryTransformerEntry(path, transformer), destZip);
  }

  /**
   * Changes an existing ZIP file: transforms a given entry in it.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param path
   *          new ZIP entry path.
   * @param transformer
   *          transformer for the given ZIP entry.
   * @return <code>true</code> if the entry was replaced.
   */
  public static boolean transformEntry(final File zip, final String path, final ZipEntryTransformer transformer) {
    return operateInPlace(zip, new InPlaceAction() {
      public boolean act(File tmpFile) {
        return transformEntry(zip, path, transformer, tmpFile);
      }
    });
  }

  /**
   * Copies an existing ZIP file and transforms a given entry in it.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param entry
   *          transformer for a ZIP entry.
   * @param destZip
   *          new ZIP file created.
   * @return <code>true</code> if the entry was replaced.
   */
  public static boolean transformEntry(File zip, ZipEntryTransformerEntry entry, File destZip) {
    return transformEntries(zip, new ZipEntryTransformerEntry[] { entry }, destZip);
  }

  /**
   * Changes an existing ZIP file: transforms a given entry in it.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param entry
   *          transformer for a ZIP entry.
   * @return <code>true</code> if the entry was replaced.
   */
  public static boolean transformEntry(final File zip, final ZipEntryTransformerEntry entry) {
    return operateInPlace(zip, new InPlaceAction() {
      public boolean act(File tmpFile) {
        return transformEntry(zip, entry, tmpFile);
      }
    });
  }

  /**
   * Copies an existing ZIP file and transforms the given entries in it.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param entries
   *          ZIP entry transformers.
   * @param destZip
   *          new ZIP file created.
   * @return <code>true</code> if at least one entry was replaced.
   */
  public static boolean transformEntries(File zip, ZipEntryTransformerEntry[] entries, File destZip) {
    if (log.isDebugEnabled())
      log.debug("Copying '" + zip + "' to '" + destZip + "' and transforming entries " + Arrays.asList(entries) + ".");

    try {
      ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destZip)));
      try {
        TransformerZipEntryCallback action = new TransformerZipEntryCallback(Arrays.asList(entries), out);
        iterate(zip, action);
        return action.found();
      }
      finally {
        IOUtils.closeQuietly(out);
      }
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
  }

  /**
   * Changes an existing ZIP file: transforms a given entries in it.
   *
   * @param zip
   *          an existing ZIP file (only read).
   * @param entries
   *          ZIP entry transformers.
   * @return <code>true</code> if the entry was replaced.
   */
  public static boolean transformEntries(final File zip, final ZipEntryTransformerEntry[] entries) {
    return operateInPlace(zip, new InPlaceAction() {
      public boolean act(File tmpFile) {
        return transformEntries(zip, entries, tmpFile);
      }
    });
  }

  /**
   * Copies an existing ZIP file and transforms a given entry in it.
   *
   * @param is
   *          a ZIP input stream.
   * @param path
   *          new ZIP entry path.
   * @param transformer
   *          transformer for the given ZIP entry.
   * @param os
   *          a ZIP output stream.
   * @return <code>true</code> if the entry was replaced.
   */
  public static boolean transformEntry(InputStream is, String path, ZipEntryTransformer transformer, OutputStream os) {
    return transformEntry(is, new ZipEntryTransformerEntry(path, transformer), os);
  }

  /**
   * Copies an existing ZIP file and transforms a given entry in it.
   *
   * @param is
   *          a ZIP input stream.
   * @param entry
   *          transformer for a ZIP entry.
   * @param os
   *          a ZIP output stream.
   * @return <code>true</code> if the entry was replaced.
   */
  public static boolean transformEntry(InputStream is, ZipEntryTransformerEntry entry, OutputStream os) {
    return transformEntries(is, new ZipEntryTransformerEntry[] { entry }, os);
  }

  /**
   * Copies an existing ZIP file and transforms the given entries in it.
   *
   * @param is
   *          a ZIP input stream.
   * @param entries
   *          ZIP entry transformers.
   * @param os
   *          a ZIP output stream.
   * @return <code>true</code> if at least one entry was replaced.
   */
  public static boolean transformEntries(InputStream is, ZipEntryTransformerEntry[] entries, OutputStream os) {
    if (log.isDebugEnabled())
      log.debug("Copying '" + is + "' to '" + os + "' and transforming entries " + Arrays.asList(entries) + ".");

    try {
      ZipOutputStream out = new ZipOutputStream(os);
      TransformerZipEntryCallback action = new TransformerZipEntryCallback(Arrays.asList(entries), out);
      iterate(is, action);
      // Finishes writing the contents of the ZIP output stream without closing
      // the underlying stream.
      out.finish();
      return action.found();
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
  }

  private static class TransformerZipEntryCallback implements ZipEntryCallback {

    private final Map<String, ZipEntryTransformer> entryByPath;
    private final int entryCount;
    private final ZipOutputStream out;
    private final Set<String> names = new HashSet<String>();

    public TransformerZipEntryCallback(List<ZipEntryTransformerEntry> entries, ZipOutputStream out) {
      entryByPath = transformersByPath(entries);
      entryCount = entryByPath.size();
      this.out = out;
    }

    public void process(InputStream in, ZipEntry zipEntry) throws IOException {
      if (names.add(zipEntry.getName())) {
        ZipEntryTransformer entry = (ZipEntryTransformer) entryByPath.remove(zipEntry.getName());
        if (entry != null) {
          entry.transform(in, zipEntry, out);
        }
        else {
          ZipEntryUtil.copyEntry(zipEntry, in, out);
        }
      }
      else if (log.isDebugEnabled()) {
        log.debug("Duplicate entry: {}", zipEntry.getName());
      }
    }

    /**
     * @return <code>true</code> if at least one entry was replaced.
     */
    public boolean found() {
      return entryByPath.size() < entryCount;
    }

  }

  /**
   * @return transformers by path.
   */
  static Map<String, ZipEntryTransformer> transformersByPath(List<ZipEntryTransformerEntry> entries) {
    Map<String, ZipEntryTransformer> result = new HashMap<String, ZipEntryTransformer>();
    for (ZipEntryTransformerEntry entry : entries) {
      result.put(entry.getPath(), entry.getTransformer());
    }
    return result;
  }

  /**
   * Adds a given ZIP entry to a ZIP file.
   *
   * @param entry
   *          new ZIP entry.
   * @param out
   *          target ZIP stream.
   */
  private static void addEntry(ZipEntrySource entry, ZipOutputStream out) throws IOException {
    out.putNextEntry(entry.getEntry());
    InputStream in = entry.getInputStream();
    if (in != null) {
      try {
        IOUtils.copy(in, out);
      }
      finally {
        IOUtils.closeQuietly(in);
      }
    }
    out.closeEntry();
  }

  /* Comparing two ZIP files. */

  /**
   * Compares two ZIP files and returns <code>true</code> if they contain same
   * entries.
   * <p>
   * First the two files are compared byte-by-byte. If a difference is found the
   * corresponding entries of both ZIP files are compared. Thus if same contents
   * is packed differently the two archives may still be the same.
   * </p>
   * <p>
   * Two archives are considered the same if
   * <ol>
   * <li>they contain same number of entries,</li>
   * <li>for each entry in the first archive there exists an entry with the same
   * in the second archive</li>
   * <li>for each entry in the first archive and the entry with the same name in
   * the second archive
   * <ol>
   * <li>both are either directories or files,</li>
   * <li>both have the same size,</li>
   * <li>both have the same CRC,</li>
   * <li>both have the same contents (compared byte-by-byte).</li>
   * </ol>
   * </li>
   * </ol>
   *
   * @param f1
   *          first ZIP file.
   * @param f2
   *          second ZIP file.
   * @return <code>true</code> if the two ZIP files contain same entries,
   *         <code>false</code> if a difference was found or an error occurred
   *         during the comparison.
   */
  public static boolean archiveEquals(File f1, File f2) {
    try {
      // Check the files byte-by-byte
      if (FileUtils.contentEquals(f1, f2)) {
        return true;
      }

      log.debug("Comparing archives '{}' and '{}'...", f1, f2);

      long start = System.currentTimeMillis();
      boolean result = archiveEqualsInternal(f1, f2);
      long time = System.currentTimeMillis() - start;
      if (time > 0) {
        log.debug("Archives compared in " + time + " ms.");
      }
      return result;
    }
    catch (Exception e) {
      log.debug("Could not compare '" + f1 + "' and '" + f2 + "':", e);
      return false;
    }
  }

  private static boolean archiveEqualsInternal(File f1, File f2) throws IOException {
    ZipFile zf1 = null;
    ZipFile zf2 = null;
    try {
      zf1 = new ZipFile(f1);
      zf2 = new ZipFile(f2);

      // Check the number of entries
      if (zf1.size() != zf2.size()) {
        log.debug("Number of entries changed (" + zf1.size() + " vs " + zf2.size() + ").");
        return false;
      }
      /*
       * As there are same number of entries in both archives we can traverse
       * all entries of one of the archives and get the corresponding entries
       * from the other archive.
       *
       * If a corresponding entry is missing from the second archive the
       * archives are different and we finish the comparison.
       *
       * We guarantee that no entry of the second archive is skipped as there
       * are same number of unique entries in both archives.
       */
      Enumeration<? extends ZipEntry> en = zf1.entries();
      while (en.hasMoreElements()) {
        ZipEntry e1 = (ZipEntry) en.nextElement();
        String path = e1.getName();
        ZipEntry e2 = zf2.getEntry(path);

        // Check meta data
        if (!metaDataEquals(path, e1, e2)) {
          return false;
        }

        // Check the content
        InputStream is1 = null;
        InputStream is2 = null;
        try {
          is1 = zf1.getInputStream(e1);
          is2 = zf2.getInputStream(e2);

          if (!IOUtils.contentEquals(is1, is2)) {
            log.debug("Entry '{}' content changed.", path);
            return false;
          }
        }
        finally {
          IOUtils.closeQuietly(is1);
          IOUtils.closeQuietly(is2);
        }
      }
    }
    finally {
      closeQuietly(zf1);
      closeQuietly(zf2);
    }

    log.debug("Archives are the same.");

    return true;
  }

  /**
   * Compares meta-data of two ZIP entries.
   * <p>
   * Two entries are considered the same if
   * <ol>
   * <li>both entries exist,</li>
   * <li>both entries are either directories or files,</li>
   * <li>both entries have the same size,</li>
   * <li>both entries have the same CRC.</li>
   * </ol>
   *
   * @param path
   *          name of the entries.
   * @param e1
   *          first entry (required).
   * @param e2
   *          second entry (may be <code>null</code>).
   * @return <code>true</code> if no difference was found.
   */
  private static boolean metaDataEquals(String path, ZipEntry e1, ZipEntry e2) throws IOException {
    // Check if the same entry exists in the second archive
    if (e2 == null) {
      log.debug("Entry '{}' removed.", path);
      return false;
    }

    // Check the directory flag
    if (e1.isDirectory()) {
      if (e2.isDirectory()) {
        return true; // Let's skip the directory as there is nothing to compare
      }
      else {
        log.debug("Entry '{}' not a directory any more.", path);
        return false;
      }
    }
    else if (e2.isDirectory()) {
      log.debug("Entry '{}' now a directory.", path);
      return false;
    }

    // Check the size
    long size1 = e1.getSize();
    long size2 = e2.getSize();
    if (size1 != -1 && size2 != -1 && size1 != size2) {
      log.debug("Entry '" + path + "' size changed (" + size1 + " vs " + size2 + ").");
      return false;
    }

    // Check the CRC
    long crc1 = e1.getCrc();
    long crc2 = e2.getCrc();
    if (crc1 != -1 && crc2 != -1 && crc1 != crc2) {
      log.debug("Entry '" + path + "' CRC changed (" + crc1 + " vs " + crc2 + ").");
      return false;
    }

    // Check the time (ignored, logging only)
    if (log.isTraceEnabled()) {
      long time1 = e1.getTime();
      long time2 = e2.getTime();
      if (time1 != -1 && time2 != -1 && time1 != time2) {
        log.trace("Entry '" + path + "' time changed (" + new Date(time1) + " vs " + new Date(time2) + ").");
      }
    }

    return true;
  }

  /**
   * Compares same entry in two ZIP files (byte-by-byte).
   *
   * @param f1
   *          first ZIP file.
   * @param f2
   *          second ZIP file.
   * @param path
   *          name of the entry.
   * @return <code>true</code> if the contents of the entry was same in both ZIP
   *         files.
   */
  public static boolean entryEquals(File f1, File f2, String path) {
    return entryEquals(f1, f2, path, path);
  }

  /**
   * Compares two ZIP entries (byte-by-byte). .
   *
   * @param f1
   *          first ZIP file.
   * @param f2
   *          second ZIP file.
   * @param path1
   *          name of the first entry.
   * @param path2
   *          name of the second entry.
   * @return <code>true</code> if the contents of the entries were same.
   */
  public static boolean entryEquals(File f1, File f2, String path1, String path2) {
    ZipFile zf1 = null;
    ZipFile zf2 = null;

    try {
      zf1 = new ZipFile(f1);
      zf2 = new ZipFile(f2);

      return doEntryEquals(zf1, zf2, path1, path2);
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
    finally {
      closeQuietly(zf1);
      closeQuietly(zf2);
    }
  }

  /**
   * Compares two ZIP entries (byte-by-byte). .
   *
   * @param zf1
   *          first ZIP file.
   * @param zf2
   *          second ZIP file.
   * @param path1
   *          name of the first entry.
   * @param path2
   *          name of the second entry.
   * @return <code>true</code> if the contents of the entries were same.
   */
  public static boolean entryEquals(ZipFile zf1, ZipFile zf2, String path1, String path2) {
    try {
      return doEntryEquals(zf1, zf2, path1, path2);
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
  }

  /**
   * Compares two ZIP entries (byte-by-byte). .
   *
   * @param zf1
   *          first ZIP file.
   * @param zf2
   *          second ZIP file.
   * @param path1
   *          name of the first entry.
   * @param path2
   *          name of the second entry.
   * @return <code>true</code> if the contents of the entries were same.
   */
  private static boolean doEntryEquals(ZipFile zf1, ZipFile zf2, String path1, String path2) throws IOException {
    InputStream is1 = null;
    InputStream is2 = null;
    try {
      ZipEntry e1 = zf1.getEntry(path1);
      ZipEntry e2 = zf2.getEntry(path2);

      if (e1 == null && e2 == null) {
        return true;
      }

      if (e1 == null || e2 == null) {
        return false;
      }

      is1 = zf1.getInputStream(e1);
      is2 = zf2.getInputStream(e2);
      if (is1 == null && is2 == null) {
        return true;
      }
      if (is1 == null || is2 == null) {
        return false;
      }

      return IOUtils.contentEquals(is1, is2);
    }
    finally {
      IOUtils.closeQuietly(is1);
      IOUtils.closeQuietly(is2);
    }
  }

  /**
   * Closes the ZIP file while ignoring any errors.
   *
   * @param zf
   *          ZIP file to be closed.
   */
  public static void closeQuietly(ZipFile zf) {
    try {
      if (zf != null) {
        zf.close();
      }
    }
    catch (IOException e) {
    }
  }

  /**
   * Simple helper to make inplace operation easier
   *
   * @author shelajev
   */
  private abstract static class InPlaceAction {

    /**
     * @return true if something has been changed during the action.
     */
    abstract boolean act(File tmpFile);
  }

  /**
   *
   * This method provides a general infrastructure for in-place operations.
   * It creates temp file as a destination, then invokes the action on source and destination.
   * Then it copies the result back into src file.
   *
   * @param src - source zip file we want to modify
   * @param action - action which actually modifies the archives
   *
   * @return result of the action
   */
  private static boolean operateInPlace(File src, InPlaceAction action) {
    File tmp = null;
    try {
      tmp = File.createTempFile("zt-zip-tmp", ".zip");
      boolean result = action.act(tmp);
      if (result) { // else nothing changes
        FileUtils.forceDelete(src);
        FileUtils.moveFile(tmp, src);
      }
      return result;
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
    finally {
      FileUtils.deleteQuietly(tmp);
    }
  }

}
