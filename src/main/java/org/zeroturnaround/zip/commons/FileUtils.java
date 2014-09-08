/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.zeroturnaround.zip.commons;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a class that has been made significantly smaller (deleted a bunch of methods) and originally
 * is from the Apache commons-io package. All license and other documentation is intact.
 * 
 * General file manipulation utilities.
 * <p>
 * Facilities are provided in the following areas:
 * <ul>
 * <li>writing to a file
 * <li>reading from a file
 * <li>make a directory including parent directories
 * <li>copying files and directories
 * <li>deleting files and directories
 * <li>converting to and from a URL
 * <li>listing files and directories by filter and extension
 * <li>comparing file content
 * <li>file last changed date
 * <li>calculating a checksum
 * </ul>
 * <p>
 * Origin of code: Excalibur, Alexandria, Commons-Utils
 *
 * @author <a href="mailto:burton@relativity.yi.org">Kevin A. Burton</A>
 * @author <a href="mailto:sanders@apache.org">Scott Sanders</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author <a href="mailto:Christoph.Reck@dlr.de">Christoph.Reck</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:jefft@apache.org">Jeff Turner</a>
 * @author Matthew Hawthorne
 * @author <a href="mailto:jeremias@apache.org">Jeremias Maerki</a>
 * @author Stephen Colebourne
 * @author Ian Springer
 * @author Chris Eldredge
 * @author Jim Harrington
 * @author Niall Pemberton
 * @author Sandy McArthur
 * @version $Id: FileUtils.java 610810 2008-01-10 15:04:49Z niallp $
 */
public class FileUtils {

  /**
   * Instances should NOT be constructed in standard programming.
   */
  public FileUtils() {
    super();
  }

  /**
   * The number of bytes in a kilobyte.
   */
  public static final long ONE_KB = 1024;

  /**
   * The number of bytes in a megabyte.
   */
  public static final long ONE_MB = ONE_KB * ONE_KB;

  /**
   * The number of bytes in a gigabyte.
   */
  public static final long ONE_GB = ONE_KB * ONE_MB;

  /**
   * An empty array of type <code>File</code>.
   */
  public static final File[] EMPTY_FILE_ARRAY = new File[0];

  /**
   * Copies the given file into an output stream.
   * 
   * @param file input file (must exist).
   * @param out output stream.
   *
   * @throws java.io.IOException if file is not found or copying fails
   */
  public static void copy(File file, OutputStream out) throws IOException {
    FileInputStream in = new FileInputStream(file);
    try {
      IOUtils.copy(new BufferedInputStream(in), out);
    }
    finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Copies the given input stream into a file.
   * <p>
   * The target file must not be a directory and its parent must exist.
   * 
   * @param in source stream.
   * @param file output file to be created or overwritten.
   *
   * @throws java.io.IOException if file is not found or copying fails
   */
  public static void copy(InputStream in, File file) throws IOException {
    OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
    try {
      IOUtils.copy(in, out);
    }
    finally {
      IOUtils.closeQuietly(out);
    }
  }

  /**
   * Find a non-existing file in the same directory using the same name as prefix.
   * 
   * @param file file used for the name and location (it is not read or written).
   * @return a non-existing file in the same directory using the same name as prefix.
   */
  public static File getTempFileFor(File file) {
    File parent = file.getParentFile();
    String name = file.getName();
    File result;
    int index = 0;
    do {
      result = new File(parent, name + "_" + index++);
    }
    while (result.exists());
    return result;
  }

  // -----------------------------------------------------------------------
  /**
   * Opens a {@link FileInputStream} for the specified file, providing better
   * error messages than simply calling <code>new FileInputStream(file)</code>.
   * <p>
   * At the end of the method either the stream will be successfully opened, or an exception will have been thrown.
   * <p>
   * An exception is thrown if the file does not exist. An exception is thrown if the file object exists but is a directory. An exception is thrown if the file exists but cannot be
   * read.
   * 
   * @param file the file to open for input, must not be <code>null</code>
   * @return a new {@link FileInputStream} for the specified file
   * @throws FileNotFoundException if the file does not exist
   * @throws IOException if the file object is a directory
   * @throws IOException if the file cannot be read
   * @since Commons IO 1.3
   */
  public static FileInputStream openInputStream(File file) throws IOException {
    if (file.exists()) {
      if (file.isDirectory()) {
        throw new IOException("File '" + file + "' exists but is a directory");
      }
      if (file.canRead() == false) {
        throw new IOException("File '" + file + "' cannot be read");
      }
    }
    else {
      throw new FileNotFoundException("File '" + file + "' does not exist");
    }
    return new FileInputStream(file);
  }

  // -----------------------------------------------------------------------
  /**
   * Opens a {@link FileOutputStream} for the specified file, checking and
   * creating the parent directory if it does not exist.
   * <p>
   * At the end of the method either the stream will be successfully opened, or an exception will have been thrown.
   * <p>
   * The parent directory will be created if it does not exist. The file will be created if it does not exist. An exception is thrown if the file object exists but is a directory.
   * An exception is thrown if the file exists but cannot be written to. An exception is thrown if the parent directory cannot be created.
   * 
   * @param file the file to open for output, must not be <code>null</code>
   * @return a new {@link FileOutputStream} for the specified file
   * @throws IOException if the file object is a directory
   * @throws IOException if the file cannot be written to
   * @throws IOException if a parent directory needs creating but that fails
   * @since Commons IO 1.3
   */
  public static FileOutputStream openOutputStream(File file) throws IOException {
    if (file.exists()) {
      if (file.isDirectory()) {
        throw new IOException("File '" + file + "' exists but is a directory");
      }
      if (file.canWrite() == false) {
        throw new IOException("File '" + file + "' cannot be written to");
      }
    }
    else {
      File parent = file.getParentFile();
      if (parent != null && parent.exists() == false) {
        if (parent.mkdirs() == false) {
          throw new IOException("File '" + file + "' could not be created");
        }
      }
    }
    return new FileOutputStream(file);
  }

  // -----------------------------------------------------------------------
  /**
   * Compares the contents of two files to determine if they are equal or not.
   * <p>
   * This method checks to see if the two files are different lengths or if they point to the same file, before resorting to byte-by-byte comparison of the contents.
   * <p>
   * Code origin: Avalon
   *
   * @param file1 the first file
   * @param file2 the second file
   * @return true if the content of the files are equal or they both don't
   *         exist, false otherwise
   * @throws IOException in case of an I/O error
   */
  public static boolean contentEquals(File file1, File file2) throws IOException {
    boolean file1Exists = file1.exists();
    if (file1Exists != file2.exists()) {
      return false;
    }

    if (!file1Exists) {
      // two not existing files are equal
      return true;
    }

    if (file1.isDirectory() || file2.isDirectory()) {
      // don't want to compare directory contents
      throw new IOException("Can't compare directories, only files");
    }

    if (file1.length() != file2.length()) {
      // lengths differ, cannot be equal
      return false;
    }

    if (file1.getCanonicalFile().equals(file2.getCanonicalFile())) {
      // same file
      return true;
    }

    InputStream input1 = null;
    InputStream input2 = null;
    try {
      input1 = new FileInputStream(file1);
      input2 = new FileInputStream(file2);
      return IOUtils.contentEquals(input1, input2);

    }
    finally {
      IOUtils.closeQuietly(input1);
      IOUtils.closeQuietly(input2);
    }
  }

  // -----------------------------------------------------------------------
  /**
   * Copies a file to a directory preserving the file date.
   * <p>
   * This method copies the contents of the specified source file to a file of the same name in the specified destination directory. The destination directory is created if it does
   * not exist. If the destination file exists, then this method will overwrite it.
   *
   * @param srcFile an existing file to copy, must not be <code>null</code>
   * @param destDir the directory to place the copy in, must not be <code>null</code>
   *
   * @throws NullPointerException if source or destination is null
   * @throws IOException if source or destination is invalid
   * @throws IOException if an IO error occurs during copying
   * @see #copyFile(File, File, boolean)
   */
  public static void copyFileToDirectory(File srcFile, File destDir) throws IOException {
    copyFileToDirectory(srcFile, destDir, true);
  }

  /**
   * Copies a file to a directory optionally preserving the file date.
   * <p>
   * This method copies the contents of the specified source file to a file of the same name in the specified destination directory. The destination directory is created if it does
   * not exist. If the destination file exists, then this method will overwrite it.
   *
   * @param srcFile an existing file to copy, must not be <code>null</code>
   * @param destDir the directory to place the copy in, must not be <code>null</code>
   * @param preserveFileDate true if the file date of the copy
   *          should be the same as the original
   *
   * @throws NullPointerException if source or destination is <code>null</code>
   * @throws IOException if source or destination is invalid
   * @throws IOException if an IO error occurs during copying
   * @see #copyFile(File, File, boolean)
   * @since Commons IO 1.3
   */
  public static void copyFileToDirectory(File srcFile, File destDir, boolean preserveFileDate) throws IOException {
    if (destDir == null) {
      throw new NullPointerException("Destination must not be null");
    }
    if (destDir.exists() && destDir.isDirectory() == false) {
      throw new IllegalArgumentException("Destination '" + destDir + "' is not a directory");
    }
    copyFile(srcFile, new File(destDir, srcFile.getName()), preserveFileDate);
  }

  /**
   * Copies a file to a new location preserving the file date.
   * <p>
   * This method copies the contents of the specified source file to the specified destination file. The directory holding the destination file is created if it does not exist. If
   * the destination file exists, then this method will overwrite it.
   * 
   * @param srcFile an existing file to copy, must not be <code>null</code>
   * @param destFile the new file, must not be <code>null</code>
   * 
   * @throws NullPointerException if source or destination is <code>null</code>
   * @throws IOException if source or destination is invalid
   * @throws IOException if an IO error occurs during copying
   * @see #copyFileToDirectory(File, File)
   */
  public static void copyFile(File srcFile, File destFile) throws IOException {
    copyFile(srcFile, destFile, true);
  }

  /**
   * Copies a file to a new location.
   * <p>
   * This method copies the contents of the specified source file to the specified destination file. The directory holding the destination file is created if it does not exist. If
   * the destination file exists, then this method will overwrite it.
   *
   * @param srcFile an existing file to copy, must not be <code>null</code>
   * @param destFile the new file, must not be <code>null</code>
   * @param preserveFileDate true if the file date of the copy
   *          should be the same as the original
   *
   * @throws NullPointerException if source or destination is <code>null</code>
   * @throws IOException if source or destination is invalid
   * @throws IOException if an IO error occurs during copying
   * @see #copyFileToDirectory(File, File, boolean)
   */
  public static void copyFile(File srcFile, File destFile,
      boolean preserveFileDate) throws IOException {
    if (srcFile == null) {
      throw new NullPointerException("Source must not be null");
    }
    if (destFile == null) {
      throw new NullPointerException("Destination must not be null");
    }
    if (srcFile.exists() == false) {
      throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
    }
    if (srcFile.isDirectory()) {
      throw new IOException("Source '" + srcFile + "' exists but is a directory");
    }
    if (srcFile.getCanonicalPath().equals(destFile.getCanonicalPath())) {
      throw new IOException("Source '" + srcFile + "' and destination '" + destFile + "' are the same");
    }
    if (destFile.getParentFile() != null && destFile.getParentFile().exists() == false) {
      if (destFile.getParentFile().mkdirs() == false) {
        throw new IOException("Destination '" + destFile + "' directory cannot be created");
      }
    }
    if (destFile.exists() && destFile.canWrite() == false) {
      throw new IOException("Destination '" + destFile + "' exists but is read-only");
    }
    doCopyFile(srcFile, destFile, preserveFileDate);
  }

  /**
   * Internal copy file method.
   * 
   * @param srcFile the validated source file, must not be <code>null</code>
   * @param destFile the validated destination file, must not be <code>null</code>
   * @param preserveFileDate whether to preserve the file date
   * @throws IOException if an error occurs
   */
  private static void doCopyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
    if (destFile.exists() && destFile.isDirectory()) {
      throw new IOException("Destination '" + destFile + "' exists but is a directory");
    }

    FileInputStream input = new FileInputStream(srcFile);
    try {
      FileOutputStream output = new FileOutputStream(destFile);
      try {
        IOUtils.copy(input, output);
      }
      finally {
        IOUtils.closeQuietly(output);
      }
    }
    finally {
      IOUtils.closeQuietly(input);
    }

    if (srcFile.length() != destFile.length()) {
      throw new IOException("Failed to copy full contents from '" +
          srcFile + "' to '" + destFile + "'");
    }
    if (preserveFileDate) {
      destFile.setLastModified(srcFile.lastModified());
    }
  }

  /**
   * Copies a whole directory to a new location preserving the file dates.
   * <p>
   * This method copies the specified directory and all its child directories and files to the specified destination. The destination is the new location and name of the directory.
   * <p>
   * The destination directory is created if it does not exist. If the destination directory did exist, then this method merges the source with the destination, with the source
   * taking precedence.
   *
   * @param srcDir an existing directory to copy, must not be <code>null</code>
   * @param destDir the new directory, must not be <code>null</code>
   *
   * @throws NullPointerException if source or destination is <code>null</code>
   * @throws IOException if source or destination is invalid
   * @throws IOException if an IO error occurs during copying
   * @since Commons IO 1.1
   */
  public static void copyDirectory(File srcDir, File destDir) throws IOException {
    copyDirectory(srcDir, destDir, true);
  }

  /**
   * Copies a whole directory to a new location.
   * <p>
   * This method copies the contents of the specified source directory to within the specified destination directory.
   * <p>
   * The destination directory is created if it does not exist. If the destination directory did exist, then this method merges the source with the destination, with the source
   * taking precedence.
   *
   * @param srcDir an existing directory to copy, must not be <code>null</code>
   * @param destDir the new directory, must not be <code>null</code>
   * @param preserveFileDate true if the file date of the copy
   *          should be the same as the original
   *
   * @throws NullPointerException if source or destination is <code>null</code>
   * @throws IOException if source or destination is invalid
   * @throws IOException if an IO error occurs during copying
   * @since Commons IO 1.1
   */
  public static void copyDirectory(File srcDir, File destDir,
      boolean preserveFileDate) throws IOException {
    copyDirectory(srcDir, destDir, null, preserveFileDate);
  }

  /**
   * Copies a filtered directory to a new location.
   * <p>
   * This method copies the contents of the specified source directory to within the specified destination directory.
   * <p>
   * The destination directory is created if it does not exist. If the destination directory did exist, then this method merges the source with the destination, with the source
   * taking precedence.
   *
   * <h3>Example: Copy directories only</h3>
   * 
   * <pre>
   * // only copy the directory structure
   * FileUtils.copyDirectory(srcDir, destDir, DirectoryFileFilter.DIRECTORY, false);
   * </pre>
   *
   * <h3>Example: Copy directories and txt files</h3>
   * 
   * <pre>
   * // Create a filter for &quot;.txt&quot; files
   * IOFileFilter txtSuffixFilter = FileFilterUtils.suffixFileFilter(&quot;.txt&quot;);
   * IOFileFilter txtFiles = FileFilterUtils.andFileFilter(FileFileFilter.FILE, txtSuffixFilter);
   * 
   * // Create a filter for either directories or &quot;.txt&quot; files
   * FileFilter filter = FileFilterUtils.orFileFilter(DirectoryFileFilter.DIRECTORY, txtFiles);
   * 
   * // Copy using the filter
   * FileUtils.copyDirectory(srcDir, destDir, filter, false);
   * </pre>
   * 
   * @param srcDir an existing directory to copy, must not be <code>null</code>
   * @param destDir the new directory, must not be <code>null</code>
   * @param filter the filter to apply, null means copy all directories and files
   * @param preserveFileDate true if the file date of the copy
   *          should be the same as the original
   *
   * @throws NullPointerException if source or destination is <code>null</code>
   * @throws IOException if source or destination is invalid
   * @throws IOException if an IO error occurs during copying
   * @since Commons IO 1.4
   */
  public static void copyDirectory(File srcDir, File destDir,
      FileFilter filter, boolean preserveFileDate) throws IOException {
    if (srcDir == null) {
      throw new NullPointerException("Source must not be null");
    }
    if (destDir == null) {
      throw new NullPointerException("Destination must not be null");
    }
    if (srcDir.exists() == false) {
      throw new FileNotFoundException("Source '" + srcDir + "' does not exist");
    }
    if (srcDir.isDirectory() == false) {
      throw new IOException("Source '" + srcDir + "' exists but is not a directory");
    }
    if (srcDir.getCanonicalPath().equals(destDir.getCanonicalPath())) {
      throw new IOException("Source '" + srcDir + "' and destination '" + destDir + "' are the same");
    }

    // Cater for destination being directory within the source directory (see IO-141)
    List<String> exclusionList = null;
    if (destDir.getCanonicalPath().startsWith(srcDir.getCanonicalPath())) {
      File[] srcFiles = filter == null ? srcDir.listFiles() : srcDir.listFiles(filter);
      if (srcFiles != null && srcFiles.length > 0) {
        exclusionList = new ArrayList<String>(srcFiles.length);
        for (int i = 0; i < srcFiles.length; i++) {
          File copiedFile = new File(destDir, srcFiles[i].getName());
          exclusionList.add(copiedFile.getCanonicalPath());
        }
      }
    }
    doCopyDirectory(srcDir, destDir, filter, preserveFileDate, exclusionList);
  }

  /**
   * Internal copy directory method.
   * 
   * @param srcDir the validated source directory, must not be <code>null</code>
   * @param destDir the validated destination directory, must not be <code>null</code>
   * @param filter the filter to apply, null means copy all directories and files
   * @param preserveFileDate whether to preserve the file date
   * @param exclusionList List of files and directories to exclude from the copy, may be null
   * @throws IOException if an error occurs
   * @since Commons IO 1.1
   */
  private static void doCopyDirectory(File srcDir, File destDir, FileFilter filter,
      boolean preserveFileDate, List<String> exclusionList) throws IOException {
    if (destDir.exists()) {
      if (destDir.isDirectory() == false) {
        throw new IOException("Destination '" + destDir + "' exists but is not a directory");
      }
    }
    else {
      if (destDir.mkdirs() == false) {
        throw new IOException("Destination '" + destDir + "' directory cannot be created");
      }
      if (preserveFileDate) {
        destDir.setLastModified(srcDir.lastModified());
      }
    }
    if (destDir.canWrite() == false) {
      throw new IOException("Destination '" + destDir + "' cannot be written to");
    }
    // recurse
    File[] files = filter == null ? srcDir.listFiles() : srcDir.listFiles(filter);
    if (files == null) { // null if security restricted
      throw new IOException("Failed to list contents of " + srcDir);
    }
    for (int i = 0; i < files.length; i++) {
      File copiedFile = new File(destDir, files[i].getName());
      if (exclusionList == null || !exclusionList.contains(files[i].getCanonicalPath())) {
        if (files[i].isDirectory()) {
          doCopyDirectory(files[i], copiedFile, filter, preserveFileDate, exclusionList);
        }
        else {
          doCopyFile(files[i], copiedFile, preserveFileDate);
        }
      }
    }
  }

  // -----------------------------------------------------------------------
  /**
   * Deletes a directory recursively.
   *
   * @param directory directory to delete
   * @throws IOException in case deletion is unsuccessful
   */
  public static void deleteDirectory(File directory) throws IOException {
    if (!directory.exists()) {
      return;
    }

    cleanDirectory(directory);
    if (!directory.delete()) {
      String message =
          "Unable to delete directory " + directory + ".";
      throw new IOException(message);
    }
  }

  /**
   * Deletes a file, never throwing an exception. If file is a directory, delete it and all sub-directories.
   * <p>
   * The difference between File.delete() and this method are:
   * <ul>
   * <li>A directory to be deleted does not have to be empty.</li>
   * <li>No exceptions are thrown when a file or directory cannot be deleted.</li>
   * </ul>
   *
   * @param file file or directory to delete, can be <code>null</code>
   * @return <code>true</code> if the file or directory was deleted, otherwise <code>false</code>
   *
   * @since Commons IO 1.4
   */
  public static boolean deleteQuietly(File file) {
    if (file == null) {
      return false;
    }
    try {
      if (file.isDirectory()) {
        cleanDirectory(file);
      }
    }
    catch (Exception e) {
    }

    try {
      return file.delete();
    }
    catch (Exception e) {
      return false;
    }
  }

  /**
   * Cleans a directory without deleting it.
   *
   * @param directory directory to clean
   * @throws IOException in case cleaning is unsuccessful
   */
  public static void cleanDirectory(File directory) throws IOException {
    if (!directory.exists()) {
      String message = directory + " does not exist";
      throw new IllegalArgumentException(message);
    }

    if (!directory.isDirectory()) {
      String message = directory + " is not a directory";
      throw new IllegalArgumentException(message);
    }

    File[] files = directory.listFiles();
    if (files == null) { // null if security restricted
      throw new IOException("Failed to list contents of " + directory);
    }

    IOException exception = null;
    for (int i = 0; i < files.length; i++) {
      File file = files[i];
      try {
        forceDelete(file);
      }
      catch (IOException ioe) {
        exception = ioe;
      }
    }

    if (null != exception) {
      throw exception;
    }
  }

  // -----------------------------------------------------------------------
  /**
   * Reads the contents of a file into a String.
   * The file is always closed.
   *
   * @param file the file to read, must not be <code>null</code>
   * @param encoding the encoding to use, <code>null</code> means platform default
   * @return the file contents, never <code>null</code>
   * @throws IOException in case of an I/O error
   * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
   */
  public static String readFileToString(File file, String encoding) throws IOException {
    InputStream in = null;
    try {
      in = openInputStream(file);
      return IOUtils.toString(in, encoding);
    }
    finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Reads the contents of a file into a String using the default encoding for the VM.
   * The file is always closed.
   *
   * @param file the file to read, must not be <code>null</code>
   * @return the file contents, never <code>null</code>
   * @throws IOException in case of an I/O error
   * @since Commons IO 1.3.1
   */
  public static String readFileToString(File file) throws IOException {
    return readFileToString(file, null);
  }

  // -----------------------------------------------------------------------
  /**
   * Deletes a file. If file is a directory, delete it and all sub-directories.
   * <p>
   * The difference between File.delete() and this method are:
   * <ul>
   * <li>A directory to be deleted does not have to be empty.</li>
   * <li>You get exceptions when a file or directory cannot be deleted. (java.io.File methods returns a boolean)</li>
   * </ul>
   *
   * @param file file or directory to delete, must not be <code>null</code>
   * @throws NullPointerException if the directory is <code>null</code>
   * @throws FileNotFoundException if the file was not found
   * @throws IOException in case deletion is unsuccessful
   */
  public static void forceDelete(File file) throws IOException {
    if (file.isDirectory()) {
      deleteDirectory(file);
    }
    else {
      boolean filePresent = file.exists();
      if (!file.delete()) {
        if (!filePresent) {
          throw new FileNotFoundException("File does not exist: " + file);
        }
        String message =
            "Unable to delete file: " + file;
        throw new IOException(message);
      }
    }
  }

  /**
   * Schedules a file to be deleted when JVM exits.
   * If file is directory delete it and all sub-directories.
   *
   * @param file file or directory to delete, must not be <code>null</code>
   * @throws NullPointerException if the file is <code>null</code>
   * @throws IOException in case deletion is unsuccessful
   */
  public static void forceDeleteOnExit(File file) throws IOException {
    if (file.isDirectory()) {
      deleteDirectoryOnExit(file);
    }
    else {
      file.deleteOnExit();
    }
  }

  /**
   * Schedules a directory recursively for deletion on JVM exit.
   *
   * @param directory directory to delete, must not be <code>null</code>
   * @throws NullPointerException if the directory is <code>null</code>
   * @throws IOException in case deletion is unsuccessful
   */
  private static void deleteDirectoryOnExit(File directory) throws IOException {
    if (!directory.exists()) {
      return;
    }

    cleanDirectoryOnExit(directory);
    directory.deleteOnExit();
  }

  /**
   * Cleans a directory without deleting it.
   *
   * @param directory directory to clean, must not be <code>null</code>
   * @throws NullPointerException if the directory is <code>null</code>
   * @throws IOException in case cleaning is unsuccessful
   */
  private static void cleanDirectoryOnExit(File directory) throws IOException {
    if (!directory.exists()) {
      String message = directory + " does not exist";
      throw new IllegalArgumentException(message);
    }

    if (!directory.isDirectory()) {
      String message = directory + " is not a directory";
      throw new IllegalArgumentException(message);
    }

    File[] files = directory.listFiles();
    if (files == null) { // null if security restricted
      throw new IOException("Failed to list contents of " + directory);
    }

    IOException exception = null;
    for (int i = 0; i < files.length; i++) {
      File file = files[i];
      try {
        forceDeleteOnExit(file);
      }
      catch (IOException ioe) {
        exception = ioe;
      }
    }

    if (null != exception) {
      throw exception;
    }
  }

  /**
   * Makes a directory, including any necessary but nonexistent parent
   * directories. If there already exists a file with specified name or
   * the directory cannot be created then an exception is thrown.
   *
   * @param directory directory to create, must not be <code>null</code>
   * @throws NullPointerException if the directory is <code>null</code>
   * @throws IOException if the directory cannot be created
   */
  public static void forceMkdir(File directory) throws IOException {
    if (directory.exists()) {
      if (directory.isFile()) {
        String message =
            "File "
                + directory
                + " exists and is "
                + "not a directory. Unable to create directory.";
        throw new IOException(message);
      }
    }
    else {
      if (!directory.mkdirs()) {
        String message =
            "Unable to create directory " + directory;
        throw new IOException(message);
      }
    }
  }

  // -----------------------------------------------------------------------
  /**
   * Counts the size of a directory recursively (sum of the length of all files).
   *
   * @param directory directory to inspect, must not be <code>null</code>
   * @return size of directory in bytes, 0 if directory is security restricted
   * @throws NullPointerException if the directory is <code>null</code>
   */
  public static long sizeOfDirectory(File directory) {
    if (!directory.exists()) {
      String message = directory + " does not exist";
      throw new IllegalArgumentException(message);
    }

    if (!directory.isDirectory()) {
      String message = directory + " is not a directory";
      throw new IllegalArgumentException(message);
    }

    long size = 0;

    File[] files = directory.listFiles();
    if (files == null) { // null if security restricted
      return 0L;
    }
    for (int i = 0; i < files.length; i++) {
      File file = files[i];

      if (file.isDirectory()) {
        size += sizeOfDirectory(file);
      }
      else {
        size += file.length();
      }
    }

    return size;
  }

  /**
   * Moves a directory.
   * <p>
   * When the destination directory is on another file system, do a "copy and delete".
   *
   * @param srcDir the directory to be moved
   * @param destDir the destination directory
   * @throws NullPointerException if source or destination is <code>null</code>
   * @throws IOException if source or destination is invalid
   * @throws IOException if an IO error occurs moving the file
   * @since Commons IO 1.4
   */
  public static void moveDirectory(File srcDir, File destDir) throws IOException {
    if (srcDir == null) {
      throw new NullPointerException("Source must not be null");
    }
    if (destDir == null) {
      throw new NullPointerException("Destination must not be null");
    }
    if (!srcDir.exists()) {
      throw new FileNotFoundException("Source '" + srcDir + "' does not exist");
    }
    if (!srcDir.isDirectory()) {
      throw new IOException("Source '" + srcDir + "' is not a directory");
    }
    if (destDir.exists()) {
      throw new IOException("Destination '" + destDir + "' already exists");
    }
    boolean rename = srcDir.renameTo(destDir);
    if (!rename) {
      copyDirectory(srcDir, destDir);
      deleteDirectory(srcDir);
      if (srcDir.exists()) {
        throw new IOException("Failed to delete original directory '" + srcDir +
            "' after copy to '" + destDir + "'");
      }
    }
  }

  /**
   * Moves a file.
   * <p>
   * When the destination file is on another file system, do a "copy and delete".
   *
   * @param srcFile the file to be moved
   * @param destFile the destination file
   * @throws NullPointerException if source or destination is <code>null</code>
   * @throws IOException if source or destination is invalid
   * @throws IOException if an IO error occurs moving the file
   * @since Commons IO 1.4
   */
  public static void moveFile(File srcFile, File destFile) throws IOException {
    if (srcFile == null) {
      throw new NullPointerException("Source must not be null");
    }
    if (destFile == null) {
      throw new NullPointerException("Destination must not be null");
    }
    if (!srcFile.exists()) {
      throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
    }
    if (srcFile.isDirectory()) {
      throw new IOException("Source '" + srcFile + "' is a directory");
    }
    if (destFile.exists()) {
      throw new IOException("Destination '" + destFile + "' already exists");
    }
    if (destFile.isDirectory()) {
      throw new IOException("Destination '" + destFile + "' is a directory");
    }
    boolean rename = srcFile.renameTo(destFile);
    if (!rename) {
      copyFile(srcFile, destFile);
      if (!srcFile.delete()) {
        FileUtils.deleteQuietly(destFile);
        throw new IOException("Failed to delete original file '" + srcFile +
            "' after copy to '" + destFile + "'");
      }
    }
  }

}
