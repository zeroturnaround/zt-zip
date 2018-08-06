/**
 *    Copyright (C) 2018 ZeroTurnaround LLC <support@zeroturnaround.com>
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
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
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
import org.zeroturnaround.zip.commons.FilenameUtils;
import org.zeroturnaround.zip.commons.IOUtils;
import org.zeroturnaround.zip.commons.PathUtils;
import org.zeroturnaround.zip.transform.ZipEntryTransformer;
import org.zeroturnaround.zip.transform.ZipEntryTransformerEntry;

/**
 * ZIP file manipulation utilities.
 *
 * @author Andres Luuk
 *
 * @see #containsEntry(Path, String)
 * @see #unpackEntry(Path, String)
 * @see #unpack(Path, Path)
 * @see #pack(Path, Path)
 */
public class ZipPathUtil {

  private static final String PATH_SEPARATOR = "/";

  /** Default compression level */
  public static final int DEFAULT_COMPRESSION_LEVEL = Deflater.DEFAULT_COMPRESSION;

  private static final Logger log = LoggerFactory.getLogger("org/zeroturnaround/zip/ZipUtil".replace('/', '.')); // NOSONAR

  ZipPathUtil() {
  }

  /**
   * Returns a zip file system
   * 
   * @param zipFilename to construct the file system from
   * @param create true if the zip file should be created
   * @return a zip file system
   * @throws IOException
   */
  private static FileSystem createZipFileSystem(Path zip, Charset charset) throws IOException {
    // TODO remove when done
    // http://fahdshariff.blogspot.com/2011/08/java-7-working-with-zip-files.html
    // https://stackoverflow.com/questions/14733496/is-it-possible-to-create-a-new-zip-file-using-the-java-filesystem

    // convert the filename to a URI
    final URI uri = URI.create("jar:file:" + zip.toUri().getPath());

    final Map<String, String> env = new HashMap<>();
    env.put("create", String.valueOf(Files.notExists(zip)));
    if (charset != null) {
      env.put("encoding", charset.toString());
    }
    return FileSystems.newFileSystem(uri, env);
  }

  /**
   * Checks if the ZIP file contains the given entry.
   *
   * @param zip
   *          ZIP file.
   * @param name
   *          entry name.
   * @return <code>true</code> if the ZIP file contains the given entry.
   */
  public static boolean containsEntry(Path zip, String name) {
    try (FileSystem zipfs = createZipFileSystem(zip, null)) {
      Path item = zipfs.getPath(name);
      return Files.exists(item);
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
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
   */
  public static int getCompressionMethodOfEntry(Path zip, String name) {
    // TODO check this:
    // https://stackoverflow.com/questions/28239621/java-nio-zip-filesystem-equivalent-of-setmethod-in-java-util-zip-zipentry
    try (ZipFile zf = new ZipFile(zip.toFile())) {
      ZipEntry zipEntry = zf.getEntry(name);
      if (zipEntry == null) {
        return -1;
      }
      return zipEntry.getMethod();
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
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
  public static boolean containsAnyEntry(Path zip, String[] names) {
    try (FileSystem zipfs = createZipFileSystem(zip, null)) {
      for (int i = 0; i < names.length; i++) {
        if (Files.exists(zipfs.getPath(names[i]))) {
          return true;
        }
      }
      return false;
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
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
  public static byte[] unpackEntry(Path zip, String name) {
    return unpackEntry(zip, name, (Charset) null);
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
  public static byte[] unpackEntry(Path zip, String name, Charset charset) {
    try (FileSystem zipfs = createZipFileSystem(zip, charset)) {
      return doUnpackEntry(zipfs, name);
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
  private static byte[] doUnpackEntry(FileSystem zipfs, String name) throws IOException {
    Path file = zipfs.getPath(name);
    if (!Files.exists(file)) {
      return null; // entry not found
    }

    return Files.readAllBytes(file);
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
  public static boolean unpackEntry(Path zip, String name, Path file) {
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
  public static boolean unpackEntry(Path zip, String name, Path file, Charset charset) {
    try (FileSystem zipfs = createZipFileSystem(zip, charset)) {
      return doUnpackEntry(zipfs, name, file);
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
  private static boolean doUnpackEntry(FileSystem zipfs, String name, Path file) throws IOException {
    if (log.isTraceEnabled()) {
      log.trace("Extracting '" + zipfs + "' entry '" + name + "' into '" + file + "'.");
    }

    Path ze = zipfs.getPath(name);
    if (!Files.exists(ze)) {
      return false; // entry not found
    }

    if (Files.isDirectory(ze)) {
      if (Files.isDirectory(file)) {
        return true;
      }
      if (Files.exists(file)) {
        Files.delete(file);
      }
      Files.createDirectories(file);
      return true;
    }
    Files.copy(ze, file);
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
  public static boolean unpackEntry(InputStream is, String name, Path file) throws IOException {
    Files.deleteIfExists(file);
    try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(file))) {
      return handle(is, name, new OutputStreamUnpacker(os));
    }
  }

  /**
   * Copies an entry into a OutputStream.
   */
  private static class OutputStreamUnpacker implements ZipEntryCallback {

    private final OutputStream os;

    public OutputStreamUnpacker(OutputStream os) {
      this.os = os;
    }

    public void process(InputStream in, ZipEntry zipEntry) throws IOException {
      IOUtils.copy(in, os);
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
   * @see #iterate(Path, ZipInfoCallback)
   */
  public static void iterate(Path zip, ZipEntryCallback action) {
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
   * @see #iterate(Path, ZipInfoCallback)
   */
  public static void iterate(Path zip, ZipEntryCallback action, Charset charset) {
    try (InputStream is = Files.newInputStream(zip)) {
      iterate(is, action, charset);
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
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
   * @see #iterate(Path, String[], ZipInfoCallback)
   */
  public static void iterate(Path zip, String[] entryNames, ZipEntryCallback action) {
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
   * @see #iterate(Path, String[], ZipInfoCallback)
   */
  public static void iterate(Path zip, String[] entryNames, ZipEntryCallback action, Charset charset) {
    try (InputStream is = Files.newInputStream(zip)) {
      iterate(is, entryNames, action, charset);
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
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
   * @see #iterate(Path, ZipEntryCallback)
   */
  public static void iterate(Path zip, ZipInfoCallback action) {
    try (ZipInputStream zf = new ZipInputStream(Files.newInputStream(zip))) {
      ZipEntry entry;
      while ((entry = zf.getNextEntry()) != null) {
        try {
          action.process(entry);
        }
        catch (IOException ze) {
          throw new ZipException("Failed to process zip entry '" + entry.getName() + " with action " + action, ze);
        }
        catch (ZipBreakException ex) {
          break;
        }
      }
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
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
   * @see #iterate(Path, String[], ZipEntryCallback)
   */
  public static void iterate(Path zip, String[] entryNames, ZipInfoCallback action) {
    Set<String> entries = new HashSet<>();
    for (String s : entryNames) {
      entries.add(s);
    }
    try (ZipInputStream zf = new ZipInputStream(Files.newInputStream(zip))) {
      ZipEntry entry;
      while ((entry = zf.getNextEntry()) != null) {
        if (!entries.contains(entry.getName())) {
          continue;
        }
        try {
          action.process(entry);
        }
        catch (IOException ze) {
          throw new ZipException("Failed to process zip entry '" + entry.getName() + " with action " + action, ze);
        }
        catch (ZipBreakException ex) {
          break;
        }
      }
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
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
   * @see #iterate(Path, ZipEntryCallback)
   */
  public static void iterate(InputStream is, ZipEntryCallback action, Charset charset) {
    try (ZipInputStream in = newCloseShieldZipInputStream(is, charset)) {
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
   * @see #iterate(Path, ZipEntryCallback)
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
   * @see #iterate(Path, String[], ZipEntryCallback)
   */
  public static void iterate(InputStream is, String[] entryNames, ZipEntryCallback action, Charset charset) {
    Set<String> namesSet = new HashSet<String>();
    for (int i = 0; i < entryNames.length; i++) {
      namesSet.add(entryNames[i]);
    }
    try (ZipInputStream in = newCloseShieldZipInputStream(is, charset)) {
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
   * @see #iterate(Path, String[], ZipEntryCallback)
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
    return new ZipInputStream(in, charset != null ? charset : StandardCharsets.UTF_8);
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
  public static boolean handle(Path zip, String name, ZipEntryCallback action) {
    try (InputStream is = Files.newInputStream(zip)) {
      return handle(is, name, action);
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
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
  public static void unpack(Path zip, final Path outputDir) {
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
  public static void unpack(Path zip, final Path outputDir, Charset charset) {
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
  public static void unpack(Path zip, Path outputDir, NameMapper mapper, Charset charset) {
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
  public static void unpack(Path zip, Path outputDir, NameMapper mapper) {
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
  public static void unwrap(Path zip, final Path outputDir) {
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
  public static void unwrap(Path zip, Path outputDir, NameMapper mapper) {
    log.debug("Unwrapping '{}' into '{}'.", zip, outputDir);
    iterate(zip, new Unwrapper(outputDir, mapper));
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
  public static void unpack(InputStream is, Path outputDir) {
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
  public static void unpack(InputStream is, Path outputDir, Charset charset) {
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
  public static void unpack(InputStream is, Path outputDir, NameMapper mapper) {
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
  public static void unpack(InputStream is, Path outputDir, NameMapper mapper, Charset charset) {
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
  public static void unwrap(InputStream is, Path outputDir) {
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
  public static void unwrap(InputStream is, Path outputDir, NameMapper mapper) {
    log.debug("Unwrapping {} into '{}'.", is, outputDir);
    iterate(is, new Unwrapper(outputDir, mapper));
  }

  private static Path makeDestinationFile(Path outputDir, String name) throws IOException {
    return checkDestinationFileForTraversal(outputDir, name, outputDir.resolve(name.startsWith("/") ? name.substring(1) : name));
  }

  private static Path checkDestinationFileForTraversal(Path outputDir, String name, Path destFile) throws IOException {
    /*
     * If we see the relative traversal string of ".." we need to make sure
     * that the outputdir + name doesn't leave the outputdir. See
     * DirectoryTraversalMaliciousTest for details.
     */
    if (!destFile.normalize().startsWith(outputDir.normalize())) {
      throw new MaliciousZipException(outputDir, name);
    }
    return destFile;
  }

  /**
   * Unpacks each ZIP entry.
   *
   * @author Rein Raudjärv
   */
  private static class Unpacker implements ZipEntryCallback {

    private final Path outputDir;
    private final NameMapper mapper;

    public Unpacker(Path outputDir, NameMapper mapper) {
      this.outputDir = outputDir;
      this.mapper = mapper;
    }

    public void process(InputStream in, ZipEntry zipEntry) throws IOException {
      String name = mapper.map(zipEntry.getName());
      if (name != null) {
        Path file = makeDestinationFile(outputDir, name);

        if (zipEntry.isDirectory()) {
          Files.createDirectories(file);
        }
        else {
          Files.createDirectories(file.getParent());

          if (log.isDebugEnabled() && Files.exists(file)) {
            log.debug("Overwriting file '{}'.", zipEntry.getName());
          }

          Files.copy(in, file);
        }

        ZTFilePermissions permissions = ZipEntryUtil.getZTFilePermissions(zipEntry);
        if (permissions != null) {
          ZTFilePermissionsUtil.setPermissions(file, permissions);
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

    private final Path outputDir;
    private final NameMapper mapper;

    public BackslashUnpacker(Path outputDir, NameMapper mapper) {
      this.outputDir = outputDir;
      this.mapper = mapper;
    }

    public BackslashUnpacker(Path outputDir) {
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
          Path parentDirectory = outputDir;
          String[] dirs = name.split("\\\\");

          // lets create all the directories and the last entry is the file as EVERY entry is a file
          for (int i = 0; i < dirs.length - 1; i++) {
            Path file = parentDirectory.resolve(dirs[i]);
            if (!Files.exists(file)) {
              Files.createDirectories(file);
            }
            parentDirectory = file;
          }
          Path dest = checkDestinationFileForTraversal(outputDir, name,
              parentDirectory.resolve(dirs[dirs.length - 1]));

          Files.copy(in, dest);
        }
        // it could be that there are just top level files that the unpacker is used for
        else {
          Path dest = makeDestinationFile(outputDir, name);

          Files.copy(in, dest);
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
  private static class Unwrapper implements ZipEntryCallback {

    private final Path outputDir;
    private final NameMapper mapper;
    private String rootDir;

    public Unwrapper(Path outputDir, NameMapper mapper) {
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
        Path file = makeDestinationFile(outputDir, name);

        if (zipEntry.isDirectory()) {
          Files.createDirectories(file);
        }
        else {
          Files.createDirectories(file.getParent());

          if (log.isDebugEnabled() && Files.exists(file)) {
            log.debug("Overwriting file '{}'.", zipEntry.getName());
          }
          Files.copy(in, file);
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
   * @see #unpack(Path, Path)
   */
  public static void explode(Path zip) {
    if (Files.isDirectory(zip)) {
      return;
    }
    try {
      // Find a new unique name is the same directory
      Path temp = PathUtils.getTempFileFor(zip);

      // Rename the archive
      Files.move(zip, temp);

      // Unpack it
      unpack(temp, zip);

      // Delete the archive
      if (!Files.deleteIfExists(temp)) {
        throw new IOException("Unable to delete file: " + temp);
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
  public static byte[] packEntry(Path file) {
    log.trace("Compressing '{}' into a ZIP file with single entry.", file);

    ByteArrayOutputStream result = new ByteArrayOutputStream();
    try (ZipOutputStream out = new ZipOutputStream(result)) {
      ZipEntry entry = ZipEntryUtil.fromFile(file.getFileName().toString(), file);
      try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
        ZipEntryUtil.addEntry(entry, in, out);
      }
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
  public static void pack(Path rootDir, Path zip) {
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
  public static void pack(Path rootDir, Path zip, int compressionLevel) {
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
  public static void pack(final Path sourceDir, final Path targetZipFile, final boolean preserveRoot) {
    if (preserveRoot) {
      final String parentName = sourceDir.getFileName().toString();
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
  public static void packEntry(Path fileToPack, Path destZipFile) {
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
  public static void packEntry(Path fileToPack, Path destZipFile, final String fileName) {
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
  public static void packEntry(Path fileToPack, Path destZipFile, NameMapper mapper) {
    packEntries(new Path[] { fileToPack }, destZipFile, mapper);
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
  public static void packEntries(Path[] filesToPack, Path destZipFile) {
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
  public static void packEntries(Path[] filesToPack, Path destZipFile, NameMapper mapper) {
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
  public static void packEntries(Path[] filesToPack, Path destZipFile, int compressionLevel) {
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
  public static void packEntries(Path[] filesToPack, Path destZipFile, NameMapper mapper, int compressionLevel) {
    log.debug("Compressing '{}' into '{}'.", filesToPack, destZipFile);
    try {
      Files.deleteIfExists(destZipFile);

      try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(destZipFile)))) {

        out.setLevel(compressionLevel);

        for (int i = 0; i < filesToPack.length; i++) {
          Path fileToPack = filesToPack[i];

          ZipEntry zipEntry = ZipEntryUtil.fromFile(mapper.map(fileToPack.getFileName().toString()), fileToPack);
          out.putNextEntry(zipEntry);
          Files.copy(fileToPack, out);
          out.closeEntry();
        }
      }
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
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
  public static void pack(Path sourceDir, Path targetZip, NameMapper mapper) {
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
  public static void pack(Path sourceDir, Path targetZip, NameMapper mapper, int compressionLevel) {
    log.debug("Compressing '{}' into '{}'.", sourceDir, targetZip);
    if (!Files.exists(sourceDir)) {
      throw new ZipException("Given file '" + sourceDir + "' doesn't exist!");
    }
    try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(targetZip)))) {
      out.setLevel(compressionLevel);
      pack(sourceDir, out, mapper, "", true);
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
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
  public static void pack(Path sourceDir, OutputStream os) {
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
  public static void pack(Path sourceDir, OutputStream os, int compressionLevel) {
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
  public static void pack(Path sourceDir, OutputStream os, NameMapper mapper) {
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
  public static void pack(Path sourceDir, OutputStream os, NameMapper mapper, int compressionLevel) {
    log.debug("Compressing '{}' into a stream.", sourceDir);
    if (!Files.exists(sourceDir)) {
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
  private static void pack(Path dir, ZipOutputStream out, NameMapper mapper, String pathPrefix, boolean mustHaveChildren) throws IOException {
    if (!Files.exists(dir)) {
      throw new ZipException("Given file '" + dir + "' doesn't exist!");
    }
    if (!Files.isDirectory(dir)) {
      throw new IOException("Given file is not a directory '" + dir + "'");
    }

    boolean hasAny = false;
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir);) {
      for (Path file : stream) {
        hasAny = true;
        boolean isDir = Files.isDirectory(file);
        String path = pathPrefix + file.getFileName();
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
            Files.copy(file, out);
          }

          out.closeEntry();
        }

        // Traverse the directory
        if (isDir) {
          pack(file, out, mapper, path, false);
        }
      }
    }

    if (mustHaveChildren && !hasAny) {
      throw new ZipException("Given directory '" + dir + "' doesn't contain any files!");
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
  public static void repack(Path srcZip, Path dstZip, int compressionLevel) {
    log.debug("Repacking '{}' into '{}'.", srcZip, dstZip);

    try (RepackZipEntryCallback callback = new RepackZipEntryCallback(dstZip, compressionLevel)) {
      iterate(srcZip, callback);
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
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
  public static void repack(InputStream is, Path dstZip, int compressionLevel) {

    log.debug("Repacking from input stream into '{}'.", dstZip);

    try (RepackZipEntryCallback callback = new RepackZipEntryCallback(dstZip, compressionLevel)) {
      iterate(is, callback);
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
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
  public static void repack(Path zip, int compressionLevel) {
    try {
      Path tmpZip = PathUtils.getTempFileFor(zip);

      repack(zip, tmpZip, compressionLevel);

      // Delete original zip
      if (!Files.deleteIfExists(zip)) {
        throw new IOException("Unable to delete the file: " + zip);
      }

      // Rename the archive
      Files.move(tmpZip, zip);
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
  private static final class RepackZipEntryCallback implements ZipEntryCallback, Closeable {

    private ZipOutputStream out;

    private RepackZipEntryCallback(Path dstZip, int compressionLevel) throws IOException {
      this.out = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(dstZip)));
      this.out.setLevel(compressionLevel);
    }

    public void process(InputStream in, ZipEntry zipEntry) throws IOException {
      ZipEntryUtil.copyEntry(zipEntry, in, out);
    }

    public void close() throws IOException {
      out.close();
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
   * @see #pack(Path, Path)
   */
  public static void unexplode(Path dir) {
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
   * @see #pack(Path, Path)
   */
  public static void unexplode(Path dir, int compressionLevel) {
    try {
      // Find a new unique name is the same directory
      Path zip = PathUtils.getTempFileFor(dir);

      // Pack it
      pack(dir, zip, compressionLevel);

      // Delete the directory
      PathUtils.deleteDir(dir);

      // Rename the archive
      Files.move(zip, dir, StandardCopyOption.REPLACE_EXISTING);
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
  public static void pack(ZipEntrySource[] entries, Path zip) {
    if (log.isDebugEnabled()) {
      log.debug("Creating '{}' from {}.", zip, Arrays.asList(entries));
    }

    try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(zip))) {
      pack(entries, out, true);
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
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
  public static void addEntry(Path zip, String path, Path file, Path destZip) {
    addEntry(zip, new PathSource(path, file), destZip);
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
  public static void addEntry(final Path zip, final String path, final Path file) {
    operateInPlace(zip, new InPlaceAction() {
      public boolean act(Path tmpFile) {
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
  public static void addEntry(Path zip, String path, byte[] bytes, Path destZip) {
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
  public static void addEntry(Path zip, String path, byte[] bytes, Path destZip, final int compressionMethod) {
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
  public static void addEntry(final Path zip, final String path, final byte[] bytes) {
    operateInPlace(zip, new InPlaceAction() {
      public boolean act(Path tmpFile) {
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
  public static void addEntry(final Path zip, final String path, final byte[] bytes, final int compressionMethod) {
    operateInPlace(zip, new InPlaceAction() {
      public boolean act(Path tmpFile) {
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
  public static void addEntry(Path zip, ZipEntrySource entry, Path destZip) {
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
  public static void addEntry(final Path zip, final ZipEntrySource entry) {
    operateInPlace(zip, new InPlaceAction() {
      public boolean act(Path tmpFile) {
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
  public static void addEntries(Path zip, ZipEntrySource[] entries, Path destZip) {
    if (log.isDebugEnabled()) {
      log.debug("Copying '" + zip + "' to '" + destZip + "' and adding " + Arrays.asList(entries) + ".");
    }

    try (OutputStream destOut = new BufferedOutputStream(Files.newOutputStream(destZip))) {
      addEntries(zip, entries, destOut);
    }
    catch (IOException e) {
      ZipExceptionUtil.rethrow(e);
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
  public static void addEntries(Path zip, ZipEntrySource[] entries, OutputStream destOut) {
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
  public static void addEntries(final Path zip, final ZipEntrySource[] entries) {
    operateInPlace(zip, new InPlaceAction() {
      public boolean act(Path tmpFile) {
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
  public static void removeEntry(Path zip, String path, Path destZip) {
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
  public static void removeEntry(final Path zip, final String path) {
    operateInPlace(zip, new InPlaceAction() {
      public boolean act(Path tmpFile) {
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
  public static void removeEntries(Path zip, String[] paths, Path destZip) {
    if (log.isDebugEnabled()) {
      log.debug("Copying '" + zip + "' to '" + destZip + "' and removing paths " + Arrays.asList(paths) + ".");
    }

    try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(destZip)))) {
      copyEntries(zip, out, new HashSet<String>(Arrays.asList(paths)));
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
  }

  /**
   * Copies an existing ZIP file and removes entries with given paths.
   *
   * @param zip
   *          an existing ZIP file (only read)
   * @param paths
   *          paths of the entries to remove
   * @param destOut
   *          new ZIP destination output stream
   * @since 1.14
   */
  public static void removeEntries(Path zip, String[] paths, OutputStream destOut) {
    if (log.isDebugEnabled()) {
      log.debug("Copying '" + zip + "' to an output stream and removing paths " + Arrays.asList(paths) + ".");
    }

    ZipOutputStream out = null;
    try {
      out = new ZipOutputStream(destOut);
      copyEntries(zip, out, new HashSet<String>(Arrays.asList(paths)));
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
  public static void removeEntries(final Path zip, final String[] paths) {
    operateInPlace(zip, new InPlaceAction() {
      public boolean act(Path tmpFile) {
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
  private static void copyEntries(Path zip, final ZipOutputStream out) {
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
  private static void copyEntries(Path zip, final ZipOutputStream out, final Set<String> ignoredEntries) {
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
  static Set<String> filterDirEntries(Path zip, Collection<String> names) {
    Set<String> dirs = new HashSet<String>();
    if (zip == null) {
      return dirs;
    }

    Set<String> entries = new HashSet<>();
    for (String s : names) {
      entries.add(s);
    }
    try (ZipInputStream zf = new ZipInputStream(Files.newInputStream(zip))) {
      ZipEntry entry;
      while ((entry = zf.getNextEntry()) != null) {
        if (entry.isDirectory() && (entries.contains(entry.getName()) ||
            entry.getName().endsWith("/") && entries.contains(entry.getName().substring(0, entry.getName().length() - 1)))) {
          dirs.add(entry.getName());
        }
      }
    }
    catch (IOException e) {
      ZipExceptionUtil.rethrow(e);
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
  public static boolean replaceEntry(Path zip, String path, Path file, Path destZip) {
    return replaceEntry(zip, new PathSource(path, file), destZip);
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
  public static boolean replaceEntry(final Path zip, final String path, final Path file) {
    return operateInPlace(zip, new InPlaceAction() {
      public boolean act(Path tmpFile) {
        return replaceEntry(zip, new PathSource(path, file), tmpFile);
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
  public static boolean replaceEntry(Path zip, String path, byte[] bytes, Path destZip) {
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
  public static boolean replaceEntry(final Path zip, final String path, final byte[] bytes) {
    return operateInPlace(zip, new InPlaceAction() {
      public boolean act(Path tmpFile) {
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
  public static boolean replaceEntry(final Path zip, final String path, final byte[] bytes,
      final int compressionMethod) {
    return operateInPlace(zip, new InPlaceAction() {
      public boolean act(Path tmpFile) {
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
  public static boolean replaceEntry(Path zip, ZipEntrySource entry, Path destZip) {
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
  public static boolean replaceEntry(final Path zip, final ZipEntrySource entry) {
    return operateInPlace(zip, new InPlaceAction() {
      public boolean act(Path tmpFile) {
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
  public static boolean replaceEntries(Path zip, ZipEntrySource[] entries, Path destZip) {
    if (log.isDebugEnabled()) {
      log.debug("Copying '" + zip + "' to '" + destZip + "' and replacing entries " + Arrays.asList(entries) + ".");
    }

    final Map<String, ZipEntrySource> entryByPath = entriesByPath(entries);
    final int entryCount = entryByPath.size();
    try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(destZip)))) {
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
  public static boolean replaceEntries(final Path zip, final ZipEntrySource[] entries) {
    return operateInPlace(zip, new InPlaceAction() {
      public boolean act(Path tmpFile) {
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
  public static void addOrReplaceEntries(Path zip, ZipEntrySource[] entries, Path destZip) {
    if (log.isDebugEnabled()) {
      log.debug("Copying '" + zip + "' to '" + destZip + "' and adding/replacing entries " + Arrays.asList(entries) + ".");
    }

    final Map<String, ZipEntrySource> entryByPath = entriesByPath(entries);
    try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(destZip)))) {
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
  public static void addOrReplaceEntries(final Path zip, final ZipEntrySource[] entries) {
    operateInPlace(zip, new InPlaceAction() {
      public boolean act(Path tmpFile) {
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
  public static boolean transformEntry(Path zip, String path, ZipEntryTransformer transformer, Path destZip) {
    try {
      if (Files.isSameFile(zip, destZip)) {
        throw new IllegalArgumentException("Input (" + zip.normalize() + ") is the same as the destination!" +
            "Please use the transformEntry method without destination for in-place transformation.");
      }
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
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
  public static boolean transformEntry(final Path zip, final String path, final ZipEntryTransformer transformer) {
    return operateInPlace(zip, new InPlaceAction() {
      public boolean act(Path tmpFile) {
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
  public static boolean transformEntry(Path zip, ZipEntryTransformerEntry entry, Path destZip) {
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
  public static boolean transformEntry(final Path zip, final ZipEntryTransformerEntry entry) {
    return operateInPlace(zip, new InPlaceAction() {
      public boolean act(Path tmpFile) {
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
  public static boolean transformEntries(Path zip, ZipEntryTransformerEntry[] entries, Path destZip) {
    if (log.isDebugEnabled())
      log.debug("Copying '" + zip + "' to '" + destZip + "' and transforming entries " + Arrays.asList(entries) + ".");

    try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(destZip)))) {
      TransformerZipEntryCallback action = new TransformerZipEntryCallback(Arrays.asList(entries), out);
      iterate(zip, action);
      return action.found();
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
  public static boolean transformEntries(final Path zip, final ZipEntryTransformerEntry[] entries) {
    return operateInPlace(zip, new InPlaceAction() {
      public boolean act(Path tmpFile) {
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
    try (InputStream in = entry.getInputStream()) {
      IOUtils.copy(in, out);
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
  public static boolean archiveEquals(Path f1, Path f2) {
    try {
      // Check the files byte-by-byte
      if (PathUtils.contentEquals(f1, f2)) {
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

  private static boolean archiveEqualsInternal(Path f1, Path f2) throws IOException {
    try (FileSystem zipfs1 = createZipFileSystem(f1, null);
        FileSystem zipfs2 = createZipFileSystem(f2, null)) {
      Path root1 = zipfs1.getPath("/");
      Path root2 = zipfs2.getPath("/");
      Files.walkFileTree(root1, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
          FileVisitResult result = super.preVisitDirectory(dir, attrs);
          Path relativize = root1.relativize(dir);
          Path otherDir = root2.resolve(relativize);
          Set<String> names = new HashSet<>();
          Set<String> otherNames = new HashSet<>();
          try (DirectoryStream<Path> children = Files.newDirectoryStream(dir)) {
            children.forEach((e) -> names.add(e.getFileName().toString()));
          }
          try (DirectoryStream<Path> children = Files.newDirectoryStream(otherDir)) {
            children.forEach((e) -> otherNames.add(e.getFileName().toString()));
          }
          if (!names.equals(otherNames)) {
            throw new ZipsDiffer("Directory  '" + dir + "' content changed.");
          }
          return result;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          FileVisitResult result = super.visitFile(file, attrs);

          // get the relative file name from path "one"
          Path relativize = root1.relativize(file);
          // construct the path for the counterpart file in "other"
          Path fileInOther = root2.resolve(relativize);

          if (!PathUtils.contentEquals(file, fileInOther)) {
            throw new ZipsDiffer("Entry '" + relativize + "' content changed.");
          }
          return result;
        }
      });
    }
    catch (ZipsDiffer e) {
      log.debug(f1 + " and " + f2 + " are different in :" + e.toString());
      return false;
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
    log.debug("Archives are the same.");

    return true;
  }

  private static class ZipsDiffer extends RuntimeException {

    private static final long serialVersionUID = 1L;

    ZipsDiffer(String msg) {
      super(msg);
    }
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
  public static boolean entryEquals(Path f1, Path f2, String path) {
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
  public static boolean entryEquals(Path f1, Path f2, String path1, String path2) {
    try (FileSystem zipfs1 = createZipFileSystem(f1, null);
        FileSystem zipfs2 = createZipFileSystem(f2, null)) {
      Path p1 = zipfs1.getPath(path1);
      Path p2 = zipfs2.getPath(path1);
      return PathUtils.contentEquals(p1, p2);
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
    ZipEntry e1 = zf1.getEntry(path1);
    ZipEntry e2 = zf2.getEntry(path2);

    if (e1 == null && e2 == null) {
      return true;
    }

    if (e1 == null || e2 == null) {
      return false;
    }

    try (InputStream is1 = zf1.getInputStream(e1);
        InputStream is2 = zf2.getInputStream(e2)) {
      if (is1 == null && is2 == null) {
        return true;
      }
      if (is1 == null || is2 == null) {
        return false;
      }

      return IOUtils.contentEquals(is1, is2);
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
    abstract boolean act(Path tmpFile);
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
  private static boolean operateInPlace(Path src, InPlaceAction action) {
    Path tmp = null;
    try {
      tmp = Files.createTempFile("zt-zip-tmp", ".zip");
      boolean result = action.act(tmp);
      if (result) { // else nothing changes
        PathUtils.deleteDir(src);
        Files.move(tmp, src);
      }
      return result;
    }
    catch (IOException e) {
      throw ZipExceptionUtil.rethrow(e);
    }
    finally {
      try {
        Files.deleteIfExists(tmp);
      }
      catch (IOException e) {
        throw ZipExceptionUtil.rethrow(e);
      }
    }
  }
}