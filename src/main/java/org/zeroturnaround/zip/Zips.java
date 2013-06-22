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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Fluent api for zip handling.
 *
 * @author shelajev
 *
 */
public class Zips {

  private File src;
  private File dest;

  private Charset charset;
  private boolean preserveTimestamps;

  private List<ZipEntrySource> changedEntries = new ArrayList<ZipEntrySource>();
  private List<ZipEntrySource> removedEntries = new ArrayList<ZipEntrySource>();

  private Zips(File src) {
    this.src = src;
  }

  /**
   * Static factory method to obtain an instance of Zips.
   * Source file is mandatory
   *
   * @param src zip file to process
   * @return instance of Zips
   */
  public static Zips process(File src) {
    return new Zips(src);
  }

  /**
   * Specifies an entry to add or change to the output when this Zips executes.
   * Adding takes precedence over removal of entries.
   *
   * @param entry entry to add
   * @return this Zips for fluent api
   */
  public Zips addEntry(ZipEntrySource entry) {
    this.changedEntries.add(entry);
    return this;
  }

  /**
   * Specifies entries to add or change to the output when this Zips executes.
   * Adding takes precedence over removal of entries.
   *
   * @param entries entries to add
   * @return this Zips for fluent api
   */
  public Zips addEntries(ZipEntrySource[] entries) {
    this.changedEntries.addAll(Arrays.asList(entries));
    return this;
  }

  /**
   * Specifies an entry to remove to the output when this Zips executes.
   *
   * @param entry entry to remove
   * @return this Zips for fluent api
   */
  public Zips removeEntry(ZipEntrySource entry) {
    this.removedEntries.add(entry);
    return this;
  }

  /**
   * Specifies entries to remove to the output when this Zips executes.
   *
   * @param entries entries to remove
   * @return this Zips for fluent api
   */
  public Zips removeEntries(ZipEntrySource[] entries) {
    this.removedEntries.addAll(Arrays.asList(entries));
    return this;
  }

  /**
   * Enables timestamp preserving for this Zips execution
   *
   * @return this Zips for fluent api
   */
  public Zips preserveTimestamps() {
    this.preserveTimestamps = true;
    return this;
  }

  /**
   * Specifies timestamp preserving for this Zips execution
   *
   * @param preserve flag to preserve timestamps
   * @return this Zips for fluent api
   */
  public Zips setPreserveTimestamps(boolean preserve) {
    this.preserveTimestamps = true;
    return this;
  }

  /**
   * Specifies charset for this Zips execution
   *
   * @param charset charset to use
   * @return this Zips for fluent api
   */
  public Zips charset(Charset charset) {
    this.charset = charset;
    return this;
  }

  /**
   * Specifies destination file for this Zips execution,
   * if destination is null (default value), then source file will be overwritten.
   * Temporary file will be used as destination and then written over the source to
   * create an illusion if inplace action.
   *
   * @param destination charset to use
   * @return this Zips for fluent api
   */
  public Zips destination(File destination) {
    this.dest = destination;
    return this;
  }

  /**
   * @return true if destination is not specified.
   */
  private boolean isInPlace() {
    return dest == null;
  }

  /**
   * Iterates through source Zip entries removing or changing them according to
   * set parameters.
   */
  public void execute() {
    final Map entryByPath = ZipUtil.byPath(changedEntries.toArray(new ZipEntrySource[changedEntries.size()]));
    final Set dirNames = ZipUtil.filterDirEntries(src, removedEntries);
    File destinationZip = null;
    try {
      destinationZip = isInPlace() ? File.createTempFile("zips", ".zip") : dest;
      final ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destinationZip)));
      try {
        // Copy and replace entries
        final Set names = new HashSet();
        iterate(new ZipEntryCallback() {
          public void process(InputStream in, ZipEntry zipEntry) throws IOException {
            String entryName = zipEntry.getName();
            if (names.add(entryName)) { // duplicate entries are ignored
              if (removedEntries.contains(entryName) || isEntryInDir(dirNames, entryName)) {
                // this entry should be removed.
                return;
              }

              ZipEntrySource entry = (ZipEntrySource) entryByPath.remove(entryName);
              if (entry != null) {
                // change entry
                ZipUtil.addEntry(entry, out);
              }
              else {
                // unchanged entry
                copyEntry(zipEntry, in, out);
              }
            }
          }
        });
        // Add new entries
        for (Iterator it = entryByPath.values().iterator(); it.hasNext();) {
          ZipUtil.addEntry((ZipEntrySource) it.next(), out);
        }

        if (isInPlace()) {
          // we operate in-place
          FileUtils.forceDelete(src);
          FileUtils.moveFile(destinationZip, src);
        }
      }
      finally {
        IOUtils.closeQuietly(out);
      }
    }
    catch (IOException e) {
      throw ZipUtil.rethrow(e);
    }
    finally {
      if (isInPlace()) {
        // destinationZip is a temporary file
        FileUtils.deleteQuietly(destinationZip);
      }
    }
  }

  /**
   * Checks if entry given by name resides inside of one of the dirs.
   *
   * @param dirNames dirs
   * @param entryName entryPath
   */
  private boolean isEntryInDir(Set dirNames, String entryName) {
    // this should be done with a trie, put dirNames in a trie and check if entryName leads to
    // some node or not.
    Iterator iter = dirNames.iterator();
    while (iter.hasNext()) {
      String dirName = (String) iter.next();
      if (entryName.startsWith(dirName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Iterates through entries, running callback on them.
   * Uses getZipFile(), so is charset aware.
   *
   * @param zipEntryCallback
   */
  private void iterate(ZipEntryCallback zipEntryCallback) {
    ZipFile zf = null;
    try {
      zf = getZipFile();

      Enumeration en = zf.entries();
      while (en.hasMoreElements()) {
        ZipEntry e = (ZipEntry) en.nextElement();

        InputStream is = zf.getInputStream(e);
        try {
          zipEntryCallback.process(is, e);
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
      throw ZipUtil.rethrow(e);
    }
    finally {
      ZipUtil.closeQuietly(zf);
    }
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
  private void copyEntry(ZipEntry zipEntry, InputStream in, ZipOutputStream out) throws IOException {
    ZipEntry copy = new ZipEntry(zipEntry.getName());
    copy.setTime(preserveTimestamps ? zipEntry.getTime() : System.currentTimeMillis());
    ZipUtil.addEntry(copy, new BufferedInputStream(in), out);
  }

  /**
   * Creates a ZipFile from src and charset of this object. If a constructor with charset is
   * not available, throws an exception.
   *
   * @return ZipFile
   * @throws IOException if ZipFile cannot be constructed
   * @throws IllegalArgumentException if accessing constructor ZipFile(File, Charset)
   *
   */
  private ZipFile getZipFile() throws IOException {
    if (charset == null) {
      return new ZipFile(src);
    }

    try {
      Constructor constructor = ZipFile.class.getConstructor(new Class[] { File.class, Charset.class });
      return (ZipFile) constructor.newInstance(new Object[] { src, charset });
    }
    catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("Using constructor ZipFile(File, Charset) has failed", e);
    }
    catch (InstantiationException e) {
      throw new IllegalArgumentException("Using constructor ZipFile(File, Charset) has failed", e);
    }
    catch (IllegalAccessException e) {
      throw new IllegalArgumentException("Using constructor ZipFile(File, Charset) has failed", e);
    }
    catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Using constructor ZipFile(File, Charset) has failed", e);
    }
    catch (InvocationTargetException e) {
      throw new IllegalArgumentException("Using constructor ZipFile(File, Charset) has failed", e);
    }
  }

}
