/**
 *    Copyright (C) 2012 ZeroTurnaround LLC <support@zeroturnaround.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this Path except in compliance with the License.
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

import java.io.BufferedOutputStream;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.zeroturnaround.zip.commons.PathUtils;
import org.zeroturnaround.zip.transform.ZipEntryTransformer;
import org.zeroturnaround.zip.transform.ZipEntryTransformerEntry;

/**
 * Fluent API for Zip handling.
 *
 * @author Andres Luuk
 */
public class ZipPaths {

  /**
   * Source archive.
   */
  private final Path src;

  /**
   * Optional destination archive, if null, src will be overwritten
   */
  private Path dest;

  /**
   * Charset to use for entry names. Using the default from
   * java.util.zip.ZipOutputStream. Can be overridden.
   */
  private Charset charset = StandardCharsets.UTF_8;

  /**
   * Flag to carry timestamps of entries on.
   */
  private boolean preserveTimestamps;

  /**
   * List<ZipEntrySource>
   */
  private List<ZipEntrySource> changedEntries = new ArrayList<ZipEntrySource>();

  /**
   * Set<String>
   */
  private Set<String> removedEntries = new HashSet<String>();

  /**
   * List<ZipEntryTransformerEntry>
   */
  private List<ZipEntryTransformerEntry> transformers = new ArrayList<ZipEntryTransformerEntry>();

  /*
   * If you want many name mappers here, you can create some compound instance that knows if
   * it wants to stop after first successfull mapping or go through all transformations, if null means
   * stop and ignore entry or that name mapper didn't know how to transform, etc.
   */
  private NameMapper nameMapper;

  /**
   * Flag to show that we want the final result to be unpacked
   */
  private boolean unpackedResult;

  private ZipPaths(Path src) {
    this.src = src;
  }

  /**
   * Static factory method to obtain an instance of Zips.
   *
   * @param src zip Path to process
   * @return instance of Zips
   */
  public static ZipPaths get(Path src) {
    return new ZipPaths(src);
  }

  /**
   * Static factory method to obtain an instance of ZipPaths without source file.
   * See {@link #get(Path src)}.
   *
   * @return instance of Zips
   */
  public static ZipPaths create() {
    return new ZipPaths(null);
  }

  /**
   * Specifies an entry to add or change to the output when this ZipPaths executes.
   * Adding takes precedence over removal of entries.
   *
   * @param entry entry to add
   * @return this ZipPaths for fluent api
   */
  public ZipPaths addEntry(ZipEntrySource entry) {
    this.changedEntries.add(entry);
    return this;
  }

  /**
   * Specifies entries to add or change to the output when this ZipPaths executes.
   * Adding takes precedence over removal of entries.
   *
   * @param entries entries to add
   * @return this ZipPaths for fluent api
   */
  public ZipPaths addEntries(ZipEntrySource[] entries) {
    this.changedEntries.addAll(Arrays.asList(entries));
    return this;
  }

  /**
   * Adds a Path entry. If given Path is a dir, adds it and all subfiles recursively.
   * Adding takes precedence over removal of entries.
   *
   * @param path file to add.
   * @return this ZipPaths for fluent api
   */
  public ZipPaths addFile(Path path) {
    return addFile(path, false, null);
  }

  /**
   * Adds a Path entry. If given Path is a dir, adds it and all subfiles recursively.
   * Adding takes precedence over removal of entries.
   *
   * @param path file to add.
   * @param preserveRoot if Path is a directory, true indicates we want to preserve this dir in the zip.
   *          otherwise children of the Path are added directly under root.
   * @return this ZipPaths for fluent api
   */
  public ZipPaths addFile(Path path, boolean preserveRoot) {
    return addFile(path, preserveRoot, null);
  }

  /**
   * Adds a Path entry. If given Path is a dir, adds it and all subfiles recursively.
   * Adding takes precedence over removal of entries.
   *
   * @param path file to add.
   * @param filter a filter to accept files for adding, null means all files are accepted
   * @return this ZipPaths for fluent api
   */
  public ZipPaths addFile(Path path, FileFilter filter) {
    return this.addFile(path, false, filter);
  }

  /**
   * Adds a Path entry. If given Path is a dir, adds it and all subfiles recursively.
   * Adding takes precedence over removal of entries.
   *
   * @param path file to add.
   * @param preserveRoot if Path is a directory, true indicates we want to preserve this dir in the zip.
   *          otherwise children of the Path are added directly under root.
   * @param filter a filter to accept files for adding, null means all files are accepted
   * @return this ZipPaths for fluent api
   */
  public ZipPaths addFile(Path path, boolean preserveRoot, FileFilter filter) {
    if (!Files.isDirectory(path)) {
      this.changedEntries.add(new PathSource(path.getFileName().toString(), path));
      return this;
    }

    Path root = preserveRoot ? path.getParent() : path;
    try {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
          if (filter == null || filter.accept(dir.toFile())) {
            String entryPath = root.relativize(dir).toString();
            changedEntries.add(new PathSource(entryPath, dir));
          }
          return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          if (filter == null || filter.accept(file.toFile())) {
            String entryPath = root.relativize(file).toString();
            changedEntries.add(new PathSource(entryPath, file));
          }
          return super.visitFile(file, attrs);
        }
      });
    }
    catch (IOException e) {
      ZipExceptionUtil.rethrow(e);
    }
    return this;
  }

  /**
   * Specifies an entry to remove to the output when this ZipPaths executes.
   *
   * @param entry path of the entry to remove
   * @return this ZipPaths for fluent api
   */
  public ZipPaths removeEntry(String entry) {
    this.removedEntries.add(entry);
    return this;
  }

  /**
   * Specifies entries to remove to the output when this ZipPaths executes.
   *
   * @param entries paths of the entry to remove
   * @return this ZipPaths for fluent api
   */
  public ZipPaths removeEntries(String[] entries) {
    this.removedEntries.addAll(Arrays.asList(entries));
    return this;
  }

  /**
   * Enables timestamp preserving for this ZipPaths execution
   *
   * @return this ZipPaths for fluent api
   */
  public ZipPaths preserveTimestamps() {
    this.preserveTimestamps = true;
    return this;
  }

  /**
   * Specifies timestamp preserving for this ZipPaths execution
   *
   * @param preserve flag to preserve timestamps
   * @return this ZipPaths for fluent api
   */
  public ZipPaths setPreserveTimestamps(boolean preserve) {
    this.preserveTimestamps = preserve;
    return this;
  }

  /**
   * Specifies charset for this ZipPaths execution
   *
   * @param charset charset to use
   * @return this ZipPaths for fluent api
   */
  public ZipPaths charset(Charset charset) {
    this.charset = charset;
    return this;
  }

  /**
   * Specifies destination Path for this ZipPaths execution,
   * if destination is null (default value), then source Path will be overwritten.
   * Temporary Path will be used as destination and then written over the source to
   * create an illusion if inplace action.
   *
   * @param destination charset to use
   * @return this ZipPaths for fluent api
   */
  public ZipPaths destination(Path destination) {
    this.dest = destination;
    return this;
  }

  /**
   *
   * @param nameMapper to use when processing entries
   * @return this ZipPaths for fluent api
   */
  public ZipPaths nameMapper(NameMapper nameMapper) {
    this.nameMapper = nameMapper;
    return this;
  }

  public ZipPaths unpack() {
    this.unpackedResult = true;
    return this;
  }

  /**
   * @return true if destination is not specified.
   */
  private boolean isInPlace() {
    return dest == null;
  }

  /**
   * @return should the result of the processing be unpacked.
   */
  private boolean isUnpack() {
    return unpackedResult || (dest != null && Files.isDirectory(dest));
  }

  /**
   * Registers a transformer for a given entry.
   *
   * @param path entry to transform
   * @param transformer transformer for the entry
   * @return this ZipPaths for fluent api
   */
  public ZipPaths addTransformer(String path, ZipEntryTransformer transformer) {
    this.transformers.add(new ZipEntryTransformerEntry(path, transformer));
    return this;
  }

  /**
   * Iterates through source Zip entries removing or changing them according to
   * set parameters.
   */
  public void process() {
    if (src == null && dest == null) {
      throw new IllegalArgumentException("Source and destination shouldn't be null together");
    }

    Path destination = null;
    try {
      destination = getDestinationFile();
      ZipEntryOrInfoAdapter zipEntryAdapter = null;

      if (Files.isRegularFile(destination)) {
        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(destination)), charset)) {
          zipEntryAdapter = new ZipEntryOrInfoAdapter(new CopyingCallback(transformers, out, preserveTimestamps), null);
          processAllEntries(zipEntryAdapter);
        }
      }
      else { // directory
        zipEntryAdapter = new ZipEntryOrInfoAdapter(new UnpackingCallback(transformers, destination), null);
        Files.createDirectories(destination);
        processAllEntries(zipEntryAdapter);
      }
      handleInPlaceActions(destination);
    }
    catch (IOException e) {
      ZipExceptionUtil.rethrow(e);
    }
    finally {
      if (isInPlace()) {
        // destinationZip is a temporary file
        try {
          PathUtils.deleteDir(destination);
        }
        catch (IOException e) {
          ZipExceptionUtil.rethrow(e);
        }
      }
    }
  }

  private void processAllEntries(ZipEntryOrInfoAdapter zipEntryAdapter) {
    iterateChangedAndAdded(zipEntryAdapter);
    iterateExistingExceptRemoved(zipEntryAdapter);
  }

  private Path getDestinationFile() throws IOException {
    if (isUnpack()) {
      if (isInPlace()) {
        Path tempFile = Files.createTempDirectory("zips");
        PathUtils.deleteDir(tempFile);
        Files.createDirectories(tempFile); // temp dir created
        return tempFile;
      }
      else {
        if (!Files.isDirectory(dest)) {
          // destination is a directory, actually we shouldn't be here, because this should mean we want an unpacked result.
          PathUtils.deleteDir(dest);
          Path result = dest.toAbsolutePath();
          Files.createDirectories(result); // create a directory instead of dest file
          return result;
        }
        return dest;
      }
    }
    else {
      // we need a file
      if (isInPlace()) { // no destination specified, temp file
        return Files.createTempFile("zips", ".zip");
      }
      else {
        if (Files.isDirectory(dest)) {
          // destination is a directory, actually we shouldn't be here, because this should mean we want an unpacked result.
          PathUtils.deleteDir(dest);
          return dest.toAbsolutePath();
        }
        return dest;
      }
    }
  }

  /**
   * Reads the source ZIP Path and executes the given callback for each entry.
   * <p>
   * For each entry the corresponding input stream is also passed to the callback. If you want to stop the loop then throw a ZipBreakException.
   *
   * This method is charset aware and uses Zips.charset.
   *
   * @param zipEntryCallback
   *          callback to be called for each entry.
   *
   * @see ZipEntryCallback
   *
   */
  public void iterate(ZipEntryCallback zipEntryCallback) {
    ZipEntryOrInfoAdapter zipEntryAdapter = new ZipEntryOrInfoAdapter(zipEntryCallback, null);
    processAllEntries(zipEntryAdapter);
  }

  /**
   * Scans the source ZIP Path and executes the given callback for each entry.
   * <p>
   * Only the meta-data without the actual data is read. If you want to stop the loop then throw a ZipBreakException.
   *
   * This method is charset aware and uses Zips.charset.
   *
   * @param callback
   *          callback to be called for each entry.
   *
   * @see ZipInfoCallback
   * @see #iterate(ZipEntryCallback)
   */
  public void iterate(ZipInfoCallback callback) {
    ZipEntryOrInfoAdapter zipEntryAdapter = new ZipEntryOrInfoAdapter(null, callback);

    processAllEntries(zipEntryAdapter);
  }

  /**
   * Alias to ZipPathUtil.getEntry()
   *
   * @param name
   *          name of the entry to fetch bytes from
   * @return byte[]
   *         contents of the entry by given name
   */
  public byte[] getEntry(String name) {
    if (src == null) {
      throw new IllegalStateException("Source is not given");
    }
    return ZipPathUtil.unpackEntry(src, name);
  }

  /**
   * Alias to ZipPathUtil.containsEntry()
   *
   * @param name
   *          entry to check existence of
   * @return true if zip archive we're processing contains entry by given name, false otherwise
   */
  public boolean containsEntry(String name) {
    if (src == null) {
      throw new IllegalStateException("Source is not given");
    }
    return ZipPathUtil.containsEntry(src, name);
  }

  // ///////////// private api ///////////////

  /**
   * Iterate through source for not removed entries with a given callback
   *
   * @param zipEntryCallback callback to execute on entries or their info.
   */
  private void iterateExistingExceptRemoved(ZipEntryOrInfoAdapter zipEntryCallback) {
    if (src == null) {
      // if we don't have source specified, then we have nothing to iterate.
      return;
    }
    final Set<String> removedDirs = ZipPathUtil.filterDirEntries(src, removedEntries);

    try (ZipInputStream zf = new ZipInputStream(Files.newInputStream(src), charset)) {
      ZipEntry entry;
      while ((entry = zf.getNextEntry()) != null) {
        String entryName = entry.getName();
        if (removedEntries.contains(entryName) || isEntryInDir(removedDirs, entryName)) {
          // removed entries are
          continue;
        }

        if (nameMapper != null) {
          String mappedName = nameMapper.map(entry.getName());
          if (mappedName == null) {
            continue; // we should ignore this entry
          }
          else if (!mappedName.equals(entry.getName())) {
            // if name is different, do nothing
            entry = ZipEntryUtil.copy(entry, mappedName);
          }
        }
        try {
          zipEntryCallback.process(zf, entry);
        }
        catch (ZipBreakException ex) {
          break;
        }
      }
    }
    catch (IOException e) {
      ZipExceptionUtil.rethrow(e);
    }
  }

  /**
   * Iterate through ZipEntrySources for added or changed entries with a given callback
   *
   * @param zipEntryCallback callback to execute on entries or their info
   */
  private void iterateChangedAndAdded(ZipEntryOrInfoAdapter zipEntryCallback) {
    for (ZipEntrySource entrySource : changedEntries) {
      try (InputStream entrySourceStream = entrySource.getInputStream()) {
        ZipEntry entry = entrySource.getEntry();
        if (nameMapper != null) {
          String mappedName = nameMapper.map(entry.getName());
          if (mappedName == null) {
            continue; // we should ignore this entry
          }
          else if (!mappedName.equals(entry.getName())) {
            // if name is different, do nothing
            entry = ZipEntryUtil.copy(entry, mappedName);
          }
        }
        zipEntryCallback.process(entrySourceStream, entry);
      }
      catch (ZipBreakException ex) {
        break;
      }
      catch (IOException e) {
        ZipExceptionUtil.rethrow(e);
      }
    }
  }

  /**
   * if we are doing something in place, move result Path into src.
   *
   * @param result destination zip file
   */
  private void handleInPlaceActions(Path result) throws IOException {
    if (isInPlace()) {
      // we operate in-place
      PathUtils.deleteDir(src);
      Files.move(result, src);
    }
  }

  /**
   * Checks if entry given by name resides inside of one of the dirs.
   *
   * @param dirNames dirs
   * @param entryName entryPath
   */
  private boolean isEntryInDir(Set<String> dirNames, String entryName) {
    // this should be done with a trie, put dirNames in a trie and check if entryName leads to
    // some node or not.
    for (String dirName : dirNames) {
      if (entryName.startsWith(dirName)) {
        return true;
      }
    }
    return false;
  }

  private static class CopyingCallback implements ZipEntryCallback {

    private final Map<String, ZipEntryTransformer> entryByPath;
    private final ZipOutputStream out;
    private final Set<String> visitedNames;
    private final boolean preserveTimestapms;

    private CopyingCallback(List<ZipEntryTransformerEntry> transformerEntries, ZipOutputStream out, boolean preserveTimestapms) {
      this.out = out;
      this.preserveTimestapms = preserveTimestapms;
      entryByPath = ZipPathUtil.transformersByPath(transformerEntries);
      visitedNames = new HashSet<String>();
    }

    public void process(InputStream in, ZipEntry zipEntry) throws IOException {
      String entryName = zipEntry.getName();

      if (visitedNames.contains(entryName)) {
        return;
      }
      visitedNames.add(entryName);

      ZipEntryTransformer transformer = (ZipEntryTransformer) entryByPath.remove(entryName);
      if (transformer == null) { // no transformer
        ZipEntryUtil.copyEntry(zipEntry, in, out, preserveTimestapms);
      }
      else { // still transfom entry
        transformer.transform(in, zipEntry, out);
      }
    }
  }

  private static class UnpackingCallback implements ZipEntryCallback {

    private final Map<String, ZipEntryTransformer> entryByPath;
    private final Set<String> visitedNames;
    private final Path destination;

    private UnpackingCallback(List<ZipEntryTransformerEntry> entries, Path destination) {
      this.destination = destination;
      this.entryByPath = ZipPathUtil.transformersByPath(entries);
      visitedNames = new HashSet<String>();
    }

    public void process(InputStream in, ZipEntry zipEntry) throws IOException {
      String entryName = zipEntry.getName();

      if (visitedNames.contains(entryName)) {
        return;
      }
      visitedNames.add(entryName);

      Path file = destination.resolve(entryName);
      if (zipEntry.isDirectory()) {
        Files.createDirectories(file);
        return;
      }
      else {
        Files.createDirectories(file.getParent());
      }

      ZipEntryTransformer transformer = (ZipEntryTransformer) entryByPath.remove(entryName);
      if (transformer == null) { // no transformer
        Files.copy(in, file);
      }
      else { // still transform entry
        transformIntoFile(transformer, in, zipEntry, file);
      }
    }

    private void transformIntoFile(final ZipEntryTransformer transformer, final InputStream entryIn, final ZipEntry zipEntry, final Path destination) throws IOException {
      ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(1);
      try (
          PipedInputStream pipedIn = new PipedInputStream();
          PipedOutputStream pipedOut = new PipedOutputStream(pipedIn);
          ZipInputStream zipIn = new ZipInputStream(pipedIn);
          ZipOutputStream zipOut = new ZipOutputStream(pipedOut);) {
        newFixedThreadPool.execute(new Runnable() {
          public void run() {
            try {
              transformer.transform(entryIn, zipEntry, zipOut);
            }
            catch (IOException e) {
              ZipExceptionUtil.rethrow(e);
            }
          }
        });
        zipIn.getNextEntry();
        Files.copy(zipIn, destination);
      }
      finally {
        newFixedThreadPool.shutdown();
      }
    }
  }
}