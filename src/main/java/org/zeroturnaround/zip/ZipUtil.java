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
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ZIP file manipulation utilities.
 * 
 * @author Rein Raudjärv
 * 
 * @see #containsEntry(File, String)
 * @see #unpackEntry(File, String)
 * @see #unpack(File, File)
 * @see #pack(File, File)
 */
public final class ZipUtil {
  
  private static final Logger log = LoggerFactory.getLogger(ZipUtil.class);

  private ZipUtil() {}
  
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
      throw rethrow(e);
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
   * @return <code>true</code> if the ZIP file contains any of the given entries.
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
      throw rethrow(e);
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
    catch (IOException e) {
      throw rethrow(e);
    }
    finally {
      closeQuietly(zf);
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
    if (log.isTraceEnabled()) {
      log.trace("Extracting '" + zip + "' entry '" + name + "' into '" + file + "'.");
    }

    ZipFile zf = null;
    try {
      zf = new ZipFile(zip);
      ZipEntry ze = zf.getEntry(name);
      if (ze == null) {
        return false; // entry not found
      }

      InputStream in = new BufferedInputStream(zf.getInputStream(ze));
      try {
        FileUtil.copy(in, file);
      }
      finally {
        IOUtils.closeQuietly(in);
      }
      return true;
    }
    catch (IOException e) {
      throw rethrow(e);
    }
    finally {
      closeQuietly(zf);
    }
  }

  /* Traversing ZIP files */

  /**
   * Reads the given ZIP file and executes the given action for each entry.
   * <p>
   * For each entry the corresponding input stream is also passed to the action.
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
    ZipFile zf = null;
    try {
      zf = new ZipFile(zip);

      Enumeration en = zf.entries();
      while (en.hasMoreElements()) {
        ZipEntry e = (ZipEntry) en.nextElement();

        InputStream is = zf.getInputStream(e);
        try {
          action.process(is, e);
        }
        finally {
          IOUtils.closeQuietly(is);
        }
      }
    }
    catch (IOException e) {
      throw rethrow(e);
    }
    finally {
      closeQuietly(zf);
    }
  }

  /**
   * Scans the given ZIP file and executes the given action for each entry.
   * <p>
   * Only the meta-data without the actual data is read.
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

      Enumeration en = zf.entries();
      while (en.hasMoreElements()) {
        ZipEntry e = (ZipEntry) en.nextElement();
        action.process(e);
      }
    }
    catch (IOException e) {
      throw rethrow(e);
    }
    finally {
      closeQuietly(zf);
    }
  }

  /**
   * Reads the given ZIP stream and executes the given action for each entry.
   * <p>
   * For each entry the corresponding input stream is also passed to the action.
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
    try {
      ZipInputStream in = new ZipInputStream(new BufferedInputStream(is));
      ZipEntry entry;
      while ((entry = in.getNextEntry()) != null) {
        action.process(in, entry);
      }
    }
    catch (IOException e) {
      throw rethrow(e);
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
   * Unpacks a ZIP file to the given directory.
   * <p>
   * The output directory must not be a file.
   * 
   * @param zip
   *          input ZIP file.
   * @param outputDir
   *          output directory (created automatically if not found).
   */
  public static void unpack(File zip, File outputDir, NameMapper mapper) {
    log.debug("Extracting '{}' into '{}'.", zip, outputDir);
    iterate(zip, new Unpacker(outputDir, mapper));
  }
  
  /**
   * Unpacks a ZIP stream to the given directory.
   * <p>
   * The output directory must not be a file.
   * 
   * @param zip
   *          input ZIP file.
   * @param outputDir
   *          output directory (created automatically if not found).
   */
  public static void unpack(InputStream is, File outputDir) {
    unpack(is, outputDir, IdentityNameMapper.INSTANCE);
  }
  
  /**
   * Unpacks a ZIP stream to the given directory.
   * <p>
   * The output directory must not be a file.
   * 
   * @param zip
   *          input ZIP file.
   * @param outputDir
   *          output directory (created automatically if not found).
   */
  public static void unpack(InputStream is, File outputDir, NameMapper mapper) {
    log.debug("Extracting {} into '{}'.", is, outputDir);
    iterate(is, new Unpacker(outputDir, mapper));
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
        if (zipEntry.isDirectory()) {
          FileUtils.forceMkdir(file);
        }
        else {
          FileUtils.forceMkdir(file.getParentFile());

          if (log.isDebugEnabled() && file.exists()) {
            log.debug("Overwriting file '{}'.", zipEntry.getName());
          }

          FileUtil.copy(in, file);
        }
      }
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
      File tempFile = FileUtil.getTempFileFor(zip);

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
      throw rethrow(e);
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
      ZipEntry entry = new ZipEntry(file.getName());
      entry.setTime(file.lastModified());
      InputStream in = new BufferedInputStream(new FileInputStream(file));
      try {
        addEntry(entry, in, out);
      }
      finally {
        IOUtils.closeQuietly(in);
      }
      out.close();
    }
    catch (IOException e) {
      throw rethrow(e);
    }
    return result.toByteArray();
  }

  /* Compressing ZIP files. */

  /**
   * Compresses the given directory and all its sub-directories into a ZIP file.
   * <p>
   * The ZIP file must not be a directory and its parent directory must exist.
   * 
   * @param File
   *          root directory.
   * @param zip
   *          ZIP file that will be created or overwritten.
   */
  public static void pack(File rootDir, File zip) {
    pack(rootDir, zip, IdentityNameMapper.INSTANCE);
  }

  /**
   * Compresses the given directory and all its sub-directories into a ZIP file.
   * <p>
   * The ZIP file must not be a directory and its parent directory must exist.
   * 
   * @param File
   *          root directory.
   * @param zip
   *          ZIP file that will be created or overwritten.
   */
  public static void pack(File rootDir, File zip, NameMapper mapper) {
    log.debug("Compressing '{}' into '{}'.", rootDir, zip);

    ZipOutputStream out = null;
    try {
      out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zip)));
      pack(rootDir, out, mapper, "");
    }
    catch (IOException e) {
      throw rethrow(e);
    }
    finally {
      IOUtils.closeQuietly(out);
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
   */
  private static void pack(File dir, ZipOutputStream out, NameMapper mapper, String pathPrefix) throws IOException {
    File[] files = dir.listFiles();
    if (files == null) {
      return; // Not a directory
    }

    for (int i = 0; i < files.length; i++) {
      File file = files[i];
      boolean isDir = file.isDirectory();
      String path = pathPrefix + file.getName();
      if (isDir) {
        path += "/";
      }

      // Create a ZIP entry
      String name = mapper.map(path);
      if (name != null) {
        ZipEntry zipEntry = new ZipEntry(name);
        if (!isDir) {
          zipEntry.setSize(file.length());
          zipEntry.setTime(file.lastModified());
        }
        
        out.putNextEntry(zipEntry);
        
        // Copy the file content
        if (!isDir) {
          FileUtil.copy(file, out);
        }
        
        out.closeEntry();
      }

      // Traverse the directory
      if (isDir) {
        pack(file, out, mapper, path);
      }
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
    try {
      // Find a new unique name is the same directory
      File zip = FileUtil.getTempFileFor(dir);

      // Pack it
      pack(dir, zip);

      // Delete the directory
      FileUtils.deleteDirectory(dir);

      // Rename the archive
      FileUtils.moveFile(zip, dir);
    }
    catch (IOException e) {
      throw rethrow(e);
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
    log.debug("Creating '{}' from {}.", zip, Arrays.asList(entries));

    ZipOutputStream out = null;
    try {
      out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zip)));
      for (int i = 0; i < entries.length; i++) {
        addEntry(entries[i], out);
      }
    }
    catch (IOException e) {
      throw rethrow(e);
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
   * @param entry
   *          new ZIP entry appended.
   * @param destZip
   *          new ZIP file created.
   */
  public static void addEntry(File zip, ZipEntrySource entry, File destZip) {
    addEntries(zip, new ZipEntrySource[] { entry }, destZip);
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

    ZipOutputStream out = null;
    try {
      out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destZip)));
      copyEntries(zip, out);
      for (int i = 0; i < entries.length; i++) {
        addEntry(entries[i], out);
      }
    }
    catch (IOException e) {
      throw rethrow(e);
    }
    finally {
      IOUtils.closeQuietly(out);
    }
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
    final Set names = new HashSet();
    iterate(zip, new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        if (names.add(zipEntry.getName())) {
          copyEntry(zipEntry, in, out);
        }
        else if (log.isDebugEnabled()) {
          log.debug("Duplicate entry: {}", zipEntry.getName());
        }
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

    final Map entryByPath = byPath(entries);
    final int entryCount = entryByPath.size();
    try {
      final ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destZip)));
      try {
        final Set names = new HashSet();
        iterate(zip, new ZipEntryCallback() {
          public void process(InputStream in, ZipEntry zipEntry) throws IOException {
            if (names.add(zipEntry.getName())) {
              ZipEntrySource entry = (ZipEntrySource) entryByPath.remove(zipEntry.getName());
              if (entry != null) {
                addEntry(entry, out);
              }
              else {
                copyEntry(zipEntry, in, out);
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
      throw rethrow(e);
    }
    return entryByPath.size() < entryCount;
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
      log.debug("Copying '" + zip + "' to '" + destZip + "' and adding/replacing entries " + Arrays.asList(entries) + ".");
    }

    final Map entryByPath = byPath(entries);
    try {
      final ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destZip)));
      try {
        // Copy and replace entries
        final Set names = new HashSet();
        iterate(zip, new ZipEntryCallback() {
          public void process(InputStream in, ZipEntry zipEntry) throws IOException {
            if (names.add(zipEntry.getName())) {
              ZipEntrySource entry = (ZipEntrySource) entryByPath.remove(zipEntry.getName());
              if (entry != null) {
                addEntry(entry, out);
              }
              else {
                copyEntry(zipEntry, in, out);
              }
            }
            else if (log.isDebugEnabled()) {
              log.debug("Duplicate entry: {}", zipEntry.getName());
            }
          }
        });
        
        // Add new entries
        for (Iterator it = entryByPath.values().iterator(); it.hasNext();) {
          addEntry((ZipEntrySource) it.next(), out);
        }
      }
      finally {
        IOUtils.closeQuietly(out);
      }
    }
    catch (IOException e) {
      throw rethrow(e);
    }
  }
  
  /**
   * @return given entries indexed by path.
   */
  private static Map byPath(ZipEntrySource[] entries) {
    Map result = new HashMap();
    for (int i = 0; i < entries.length; i++) {
      ZipEntrySource source = entries[i];
      result.put(source.getPath(), source);
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
  private static void addEntry(ZipEntry zipEntry, InputStream in, ZipOutputStream out) throws IOException {
    out.putNextEntry(zipEntry);
    if (in != null) {
      IOUtils.copy(in, out);
    }
    out.closeEntry();
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
  private static void copyEntry(ZipEntry zipEntry, InputStream in, ZipOutputStream out) throws IOException {
    ZipEntry copy = new ZipEntry(zipEntry.getName());
    copy.setTime(zipEntry.getTime());
    addEntry(copy, new BufferedInputStream(in), out);
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
      Enumeration en = zf1.entries();
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

    InputStream is1 = null;
    InputStream is2 = null;
    try {
      zf1 = new ZipFile(f1);
      zf2 = new ZipFile(f2);

      ZipEntry e1 = zf1.getEntry(path1);
      ZipEntry e2 = zf2.getEntry(path2);
      
      if (e1 == null && e2 == null){
        return true;
      }
      
      if (e1 == null || e2 == null){
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
    catch (IOException e) {
      throw rethrow(e);
    }
    finally {
      IOUtils.closeQuietly(is1);
      IOUtils.closeQuietly(is2);

      closeQuietly(zf1);
      closeQuietly(zf2);
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
   * Rethrow the given exception as a runtime exception. 
   */
  private static RuntimeException rethrow(IOException e) {
    throw new RuntimeException(e);
  }

}
