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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a class that has been made significantly smaller (deleted a bunch of methods) and originally
 * is from the Apache Commons IO 2.2 package (the latest version that supports Java 1.5). All license and other documentation is intact.
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
 * @version $Id: FileUtils.java 1304052 2012-03-22 20:55:29Z ggregory $
 */
public class FileUtilsV2_2 {

  /**
   * Instances should NOT be constructed in standard programming.
   */
  public FileUtilsV2_2() {
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
   * The file copy buffer size (30 MB)
   */
  private static final long FILE_COPY_BUFFER_SIZE = ONE_MB * 30;

  /**
   * The number of bytes in a gigabyte.
   */
  public static final long ONE_GB = ONE_KB * ONE_MB;

  /**
   * The number of bytes in a terabyte.
   */
  public static final long ONE_TB = ONE_KB * ONE_GB;

  /**
   * The number of bytes in a petabyte.
   */
  public static final long ONE_PB = ONE_KB * ONE_TB;

  /**
   * The number of bytes in an exabyte.
   */
  public static final long ONE_EB = ONE_KB * ONE_PB;

  /**
   * The number of bytes in a zettabyte.
   */
  public static final BigInteger ONE_ZB = BigInteger.valueOf(ONE_KB).multiply(BigInteger.valueOf(ONE_EB));

  /**
   * The number of bytes in a yottabyte.
   */
  public static final BigInteger ONE_YB = ONE_ZB.multiply(BigInteger.valueOf(ONE_EB));

  /**
   * An empty array of type <code>File</code>.
   */
  public static final File[] EMPTY_FILE_ARRAY = new File[0];

  //-----------------------------------------------------------------------
  /**
   * Opens a {@link FileInputStream} for the specified file, providing better
   * error messages than simply calling <code>new FileInputStream(file)</code>.
   * <p>
   * At the end of the method either the stream will be successfully opened,
   * or an exception will have been thrown.
   * <p>
   * An exception is thrown if the file does not exist.
   * An exception is thrown if the file object exists but is a directory.
   * An exception is thrown if the file exists but cannot be read.
   * 
   * @param file  the file to open for input, must not be <code>null</code>
   * @return a new {@link FileInputStream} for the specified file
   * @throws FileNotFoundException if the file does not exist
   * @throws IOException if the file object is a directory
   * @throws IOException if the file cannot be read
   * @since 1.3
   */
  public static FileInputStream openInputStream(File file) throws IOException {
    if (file.exists()) {
      if (file.isDirectory()) {
        throw new IOException("File '" + file + "' exists but is a directory");
      }
      if (file.canRead() == false) {
        throw new IOException("File '" + file + "' cannot be read");
      }
    } else {
      throw new FileNotFoundException("File '" + file + "' does not exist");
    }
    return new FileInputStream(file);
  }

  //-----------------------------------------------------------------------
  /**
   * Opens a {@link FileOutputStream} for the specified file, checking and
   * creating the parent directory if it does not exist.
   * <p>
   * At the end of the method either the stream will be successfully opened,
   * or an exception will have been thrown.
   * <p>
   * The parent directory will be created if it does not exist.
   * The file will be created if it does not exist.
   * An exception is thrown if the file object exists but is a directory.
   * An exception is thrown if the file exists but cannot be written to.
   * An exception is thrown if the parent directory cannot be created.
   * 
   * @param file  the file to open for output, must not be <code>null</code>
   * @return a new {@link FileOutputStream} for the specified file
   * @throws IOException if the file object is a directory
   * @throws IOException if the file cannot be written to
   * @throws IOException if a parent directory needs creating but that fails
   * @since 1.3
   */
  public static FileOutputStream openOutputStream(File file) throws IOException {
    return openOutputStream(file, false);
  }

  /**
   * Opens a {@link FileOutputStream} for the specified file, checking and
   * creating the parent directory if it does not exist.
   * <p>
   * At the end of the method either the stream will be successfully opened,
   * or an exception will have been thrown.
   * <p>
   * The parent directory will be created if it does not exist.
   * The file will be created if it does not exist.
   * An exception is thrown if the file object exists but is a directory.
   * An exception is thrown if the file exists but cannot be written to.
   * An exception is thrown if the parent directory cannot be created.
   * 
   * @param file  the file to open for output, must not be <code>null</code>
   * @param append if <code>true</code>, then bytes will be added to the
   * end of the file rather than overwriting
   * @return a new {@link FileOutputStream} for the specified file
   * @throws IOException if the file object is a directory
   * @throws IOException if the file cannot be written to
   * @throws IOException if a parent directory needs creating but that fails
   * @since 2.1
   */
  public static FileOutputStream openOutputStream(File file, boolean append) throws IOException {
    if (file.exists()) {
      if (file.isDirectory()) {
        throw new IOException("File '" + file + "' exists but is a directory");
      }
      if (file.canWrite() == false) {
        throw new IOException("File '" + file + "' cannot be written to");
      }
    } else {
      File parent = file.getParentFile();
      if (parent != null) {
        if (!parent.mkdirs() && !parent.isDirectory()) {
          throw new IOException("Directory '" + parent + "' could not be created");
        }
      }
    }
    return new FileOutputStream(file, append);
  }

  //-----------------------------------------------------------------------
  /**
   * Compares the contents of two files to determine if they are equal or not.
   * <p>
   * This method checks to see if the two files are different lengths
   * or if they point to the same file, before resorting to byte-by-byte
   * comparison of the contents.
   * <p>
   * Code origin: Avalon
   *
   * @param file1  the first file
   * @param file2  the second file
   * @return true if the content of the files are equal or they both don't
   * exist, false otherwise
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

    } finally {
      IOUtils.closeQuietly(input1);
      IOUtils.closeQuietly(input2);
    }
  }

  //-----------------------------------------------------------------------
  /**
   * Copies a file to a directory preserving the file date.
   * <p>
   * This method copies the contents of the specified source file
   * to a file of the same name in the specified destination directory.
   * The destination directory is created if it does not exist.
   * If the destination file exists, then this method will overwrite it.
   * <p>
   * <strong>Note:</strong> This method tries to preserve the file's last
   * modified date/times using {@link File#setLastModified(long)}, however
   * it is not guaranteed that the operation will succeed.
   * If the modification operation fails, no indication is provided.
   *
   * @param srcFile  an existing file to copy, must not be <code>null</code>
   * @param destDir  the directory to place the copy in, must not be <code>null</code>
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
   * This method copies the contents of the specified source file
   * to a file of the same name in the specified destination directory.
   * The destination directory is created if it does not exist.
   * If the destination file exists, then this method will overwrite it.
   * <p>
   * <strong>Note:</strong> Setting <code>preserveFileDate</code> to
   * <code>true</code> tries to preserve the file's last modified
   * date/times using {@link File#setLastModified(long)}, however it is
   * not guaranteed that the operation will succeed.
   * If the modification operation fails, no indication is provided.
   *
   * @param srcFile  an existing file to copy, must not be <code>null</code>
   * @param destDir  the directory to place the copy in, must not be <code>null</code>
   * @param preserveFileDate  true if the file date of the copy
   *  should be the same as the original
   *
   * @throws NullPointerException if source or destination is <code>null</code>
   * @throws IOException if source or destination is invalid
   * @throws IOException if an IO error occurs during copying
   * @see #copyFile(File, File, boolean)
   * @since 1.3
   */
  public static void copyFileToDirectory(File srcFile, File destDir, boolean preserveFileDate) throws IOException {
    if (destDir == null) {
      throw new NullPointerException("Destination must not be null");
    }
    if (destDir.exists() && destDir.isDirectory() == false) {
      throw new IllegalArgumentException("Destination '" + destDir + "' is not a directory");
    }
    File destFile = new File(destDir, srcFile.getName());
    copyFile(srcFile, destFile, preserveFileDate);
  }

  /**
   * Copies a file to a new location preserving the file date.
   * <p>
   * This method copies the contents of the specified source file to the
   * specified destination file. The directory holding the destination file is
   * created if it does not exist. If the destination file exists, then this
   * method will overwrite it.
   * <p>
   * <strong>Note:</strong> This method tries to preserve the file's last
   * modified date/times using {@link File#setLastModified(long)}, however
   * it is not guaranteed that the operation will succeed.
   * If the modification operation fails, no indication is provided.
   * 
   * @param srcFile  an existing file to copy, must not be <code>null</code>
   * @param destFile  the new file, must not be <code>null</code>
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
   * This method copies the contents of the specified source file
   * to the specified destination file.
   * The directory holding the destination file is created if it does not exist.
   * If the destination file exists, then this method will overwrite it.
   * <p>
   * <strong>Note:</strong> Setting <code>preserveFileDate</code> to
   * <code>true</code> tries to preserve the file's last modified
   * date/times using {@link File#setLastModified(long)}, however it is
   * not guaranteed that the operation will succeed.
   * If the modification operation fails, no indication is provided.
   *
   * @param srcFile  an existing file to copy, must not be <code>null</code>
   * @param destFile  the new file, must not be <code>null</code>
   * @param preserveFileDate  true if the file date of the copy
   *  should be the same as the original
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
    File parentFile = destFile.getParentFile();
    if (parentFile != null) {
      if (!parentFile.mkdirs() && !parentFile.isDirectory()) {
        throw new IOException("Destination '" + parentFile + "' directory cannot be created");
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
   * @param srcFile  the validated source file, must not be <code>null</code>
   * @param destFile  the validated destination file, must not be <code>null</code>
   * @param preserveFileDate  whether to preserve the file date
   * @throws IOException if an error occurs
   */
  private static void doCopyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
    if (destFile.exists() && destFile.isDirectory()) {
      throw new IOException("Destination '" + destFile + "' exists but is a directory");
    }

    FileInputStream fis = null;
    FileOutputStream fos = null;
    FileChannel input = null;
    FileChannel output = null;
    try {
      fis = new FileInputStream(srcFile);
      fos = new FileOutputStream(destFile);
      input  = fis.getChannel();
      output = fos.getChannel();
      long size = input.size();
      long pos = 0;
      long count = 0;
      while (pos < size) {
        count = size - pos > FILE_COPY_BUFFER_SIZE ? FILE_COPY_BUFFER_SIZE : size - pos;
        pos += output.transferFrom(input, pos, count);
      }
    } finally {
      IOUtils.closeQuietly(output);
      IOUtils.closeQuietly(fos);
      IOUtils.closeQuietly(input);
      IOUtils.closeQuietly(fis);
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
   * This method copies the specified directory and all its child
   * directories and files to the specified destination.
   * The destination is the new location and name of the directory.
   * <p>
   * The destination directory is created if it does not exist.
   * If the destination directory did exist, then this method merges
   * the source with the destination, with the source taking precedence.
   * <p>
   * <strong>Note:</strong> This method tries to preserve the files' last
   * modified date/times using {@link File#setLastModified(long)}, however
   * it is not guaranteed that those operations will succeed.
   * If the modification operation fails, no indication is provided.
   *
   * @param srcDir  an existing directory to copy, must not be <code>null</code>
   * @param destDir  the new directory, must not be <code>null</code>
   *
   * @throws NullPointerException if source or destination is <code>null</code>
   * @throws IOException if source or destination is invalid
   * @throws IOException if an IO error occurs during copying
   * @since 1.1
   */
  public static void copyDirectory(File srcDir, File destDir) throws IOException {
    copyDirectory(srcDir, destDir, true);
  }

  /**
   * Copies a whole directory to a new location.
   * <p>
   * This method copies the contents of the specified source directory
   * to within the specified destination directory.
   * <p>
   * The destination directory is created if it does not exist.
   * If the destination directory did exist, then this method merges
   * the source with the destination, with the source taking precedence.
   * <p>
   * <strong>Note:</strong> Setting <code>preserveFileDate</code> to
   * <code>true</code> tries to preserve the files' last modified
   * date/times using {@link File#setLastModified(long)}, however it is
   * not guaranteed that those operations will succeed.
   * If the modification operation fails, no indication is provided.
   *
   * @param srcDir  an existing directory to copy, must not be <code>null</code>
   * @param destDir  the new directory, must not be <code>null</code>
   * @param preserveFileDate  true if the file date of the copy
   *  should be the same as the original
   *
   * @throws NullPointerException if source or destination is <code>null</code>
   * @throws IOException if source or destination is invalid
   * @throws IOException if an IO error occurs during copying
   * @since 1.1
   */
  public static void copyDirectory(File srcDir, File destDir,
      boolean preserveFileDate) throws IOException {
    copyDirectory(srcDir, destDir, null, preserveFileDate);
  }

  /**
   * Copies a filtered directory to a new location.
   * <p>
   * This method copies the contents of the specified source directory
   * to within the specified destination directory.
   * <p>
   * The destination directory is created if it does not exist.
   * If the destination directory did exist, then this method merges
   * the source with the destination, with the source taking precedence.
   * <p>
   * <strong>Note:</strong> Setting <code>preserveFileDate</code> to
   * <code>true</code> tries to preserve the files' last modified
   * date/times using {@link File#setLastModified(long)}, however it is
   * not guaranteed that those operations will succeed.
   * If the modification operation fails, no indication is provided.
   *
   * <h3>Example: Copy directories only</h3> 
   *  <pre>
   *  // only copy the directory structure
   *  FileUtils.copyDirectory(srcDir, destDir, DirectoryFileFilter.DIRECTORY, false);
   *  </pre>
   *
   * <h4>Example: Copy directories and txt files</h4>
   *  <pre>
   *  // Create a filter for ".txt" files
   *  IOFileFilter txtSuffixFilter = FileFilterUtils.suffixFileFilter(".txt");
   *  IOFileFilter txtFiles = FileFilterUtils.andFileFilter(FileFileFilter.FILE, txtSuffixFilter);
   *
   *  // Create a filter for either directories or ".txt" files
   *  FileFilter filter = FileFilterUtils.orFileFilter(DirectoryFileFilter.DIRECTORY, txtFiles);
   *
   *  // Copy using the filter
   *  FileUtils.copyDirectory(srcDir, destDir, filter, false);
   *  </pre>
   * 
   * @param srcDir  an existing directory to copy, must not be <code>null</code>
   * @param destDir  the new directory, must not be <code>null</code>
   * @param filter  the filter to apply, null means copy all directories and files
   * @param preserveFileDate  true if the file date of the copy
   *  should be the same as the original
   *
   * @throws NullPointerException if source or destination is <code>null</code>
   * @throws IOException if source or destination is invalid
   * @throws IOException if an IO error occurs during copying
   * @since 1.4
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
        for (File srcFile : srcFiles) {
          File copiedFile = new File(destDir, srcFile.getName());
          exclusionList.add(copiedFile.getCanonicalPath());
        }
      }
    }
    doCopyDirectory(srcDir, destDir, filter, preserveFileDate, exclusionList);
  }

  /**
   * Internal copy directory method.
   * 
   * @param srcDir  the validated source directory, must not be <code>null</code>
   * @param destDir  the validated destination directory, must not be <code>null</code>
   * @param filter  the filter to apply, null means copy all directories and files
   * @param preserveFileDate  whether to preserve the file date
   * @param exclusionList  List of files and directories to exclude from the copy, may be null
   * @throws IOException if an error occurs
   * @since 1.1
   */
  private static void doCopyDirectory(File srcDir, File destDir, FileFilter filter,
      boolean preserveFileDate, List<String> exclusionList) throws IOException {
    // recurse
    File[] srcFiles = filter == null ? srcDir.listFiles() : srcDir.listFiles(filter);
    if (srcFiles == null) {  // null if abstract pathname does not denote a directory, or if an I/O error occurs
      throw new IOException("Failed to list contents of " + srcDir);
    }
    if (destDir.exists()) {
      if (destDir.isDirectory() == false) {
        throw new IOException("Destination '" + destDir + "' exists but is not a directory");
      }
    } else {
      if (!destDir.mkdirs() && !destDir.isDirectory()) {
        throw new IOException("Destination '" + destDir + "' directory cannot be created");
      }
    }
    if (destDir.canWrite() == false) {
      throw new IOException("Destination '" + destDir + "' cannot be written to");
    }
    for (File srcFile : srcFiles) {
      File dstFile = new File(destDir, srcFile.getName());
      if (exclusionList == null || !exclusionList.contains(srcFile.getCanonicalPath())) {
        if (srcFile.isDirectory()) {
          doCopyDirectory(srcFile, dstFile, filter, preserveFileDate, exclusionList);
        } else {
          doCopyFile(srcFile, dstFile, preserveFileDate);
        }
      }
    }

    // Do this last, as the above has probably affected directory metadata
    if (preserveFileDate) {
      destDir.setLastModified(srcDir.lastModified());
    }
  }

  //-----------------------------------------------------------------------
  /**
   * Deletes a directory recursively. 
   *
   * @param directory  directory to delete
   * @throws IOException in case deletion is unsuccessful
   */
  public static void deleteDirectory(File directory) throws IOException {
    if (!directory.exists()) {
      return;
    }

    if (!isSymlink(directory)) {
      cleanDirectory(directory);
    }

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
   * @param file  file or directory to delete, can be <code>null</code>
   * @return <code>true</code> if the file or directory was deleted, otherwise
   * <code>false</code>
   *
   * @since 1.4
   */
  public static boolean deleteQuietly(File file) {
    if (file == null) {
      return false;
    }
    try {
      if (file.isDirectory()) {
        cleanDirectory(file);
      }
    } catch (Exception ignored) {
    }

    try {
      return file.delete();
    } catch (Exception ignored) {
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
    if (files == null) {  // null if security restricted
      throw new IOException("Failed to list contents of " + directory);
    }

    IOException exception = null;
    for (File file : files) {
      try {
        forceDelete(file);
      } catch (IOException ioe) {
        exception = ioe;
      }
    }

    if (null != exception) {
      throw exception;
    }
  }

  //-----------------------------------------------------------------------
  /**
   * Reads the contents of a file into a String.
   * The file is always closed.
   *
   * @param file  the file to read, must not be <code>null</code>
   * @param encoding  the encoding to use, <code>null</code> means platform default
   * @return the file contents, never <code>null</code>
   * @throws IOException in case of an I/O error
   * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
   */
  public static String readFileToString(File file, String encoding) throws IOException {
    InputStream in = null;
    try {
      in = openInputStream(file);
      return IOUtils.toString(in, encoding);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }


  /**
   * Reads the contents of a file into a String using the default encoding for the VM. 
   * The file is always closed.
   *
   * @param file  the file to read, must not be <code>null</code>
   * @return the file contents, never <code>null</code>
   * @throws IOException in case of an I/O error
   * @since 1.3.1
   */
  public static String readFileToString(File file) throws IOException {
    return readFileToString(file, null);
  }

  //-----------------------------------------------------------------------
  /**
   * Deletes a file. If file is a directory, delete it and all sub-directories.
   * <p>
   * The difference between File.delete() and this method are:
   * <ul>
   * <li>A directory to be deleted does not have to be empty.</li>
   * <li>You get exceptions when a file or directory cannot be deleted.
   *      (java.io.File methods returns a boolean)</li>
   * </ul>
   *
   * @param file  file or directory to delete, must not be <code>null</code>
   * @throws NullPointerException if the directory is <code>null</code>
   * @throws FileNotFoundException if the file was not found
   * @throws IOException in case deletion is unsuccessful
   */
  public static void forceDelete(File file) throws IOException {
    if (file.isDirectory()) {
      deleteDirectory(file);
    } else {
      boolean filePresent = file.exists();
      if (!file.delete()) {
        if (!filePresent){
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
   * @param file  file or directory to delete, must not be <code>null</code>
   * @throws NullPointerException if the file is <code>null</code>
   * @throws IOException in case deletion is unsuccessful
   */
  public static void forceDeleteOnExit(File file) throws IOException {
    if (file.isDirectory()) {
      deleteDirectoryOnExit(file);
    } else {
      file.deleteOnExit();
    }
  }

  /**
   * Schedules a directory recursively for deletion on JVM exit.
   *
   * @param directory  directory to delete, must not be <code>null</code>
   * @throws NullPointerException if the directory is <code>null</code>
   * @throws IOException in case deletion is unsuccessful
   */
  private static void deleteDirectoryOnExit(File directory) throws IOException {
    if (!directory.exists()) {
      return;
    }

    directory.deleteOnExit();
    if (!isSymlink(directory)) {
      cleanDirectoryOnExit(directory);
    }
  }

  /**
   * Cleans a directory without deleting it.
   *
   * @param directory  directory to clean, must not be <code>null</code>
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
    if (files == null) {  // null if security restricted
      throw new IOException("Failed to list contents of " + directory);
    }

    IOException exception = null;
    for (File file : files) {
      try {
        forceDeleteOnExit(file);
      } catch (IOException ioe) {
        exception = ioe;
      }
    }

    if (null != exception) {
      throw exception;
    }
  }

  /**
   * Makes a directory, including any necessary but nonexistent parent
   * directories. If a file already exists with specified name but it is
   * not a directory then an IOException is thrown.
   * If the directory cannot be created (or does not already exist)
   * then an IOException is thrown.
   *
   * @param directory  directory to create, must not be <code>null</code>
   * @throws NullPointerException if the directory is <code>null</code>
   * @throws IOException if the directory cannot be created or the file already exists but is not a directory
   */
  public static void forceMkdir(File directory) throws IOException {
    if (directory.exists()) {
      if (!directory.isDirectory()) {
        String message =
            "File "
                + directory
                + " exists and is "
                + "not a directory. Unable to create directory.";
        throw new IOException(message);
      }
    } else {
      if (!directory.mkdirs()) {
        // Double-check that some other thread or process hasn't made
        // the directory in the background
        if (!directory.isDirectory())
        {
          String message =
              "Unable to create directory " + directory;
          throw new IOException(message);
        }
      }
    }
  }

  //-----------------------------------------------------------------------
  /**
   * Returns the size of the specified file or directory. If the provided 
   * {@link File} is a regular file, then the file's length is returned.
   * If the argument is a directory, then the size of the directory is
   * calculated recursively. If a directory or subdirectory is security 
   * restricted, its size will not be included.
   * 
   * @param file the regular file or directory to return the size 
   *        of (must not be <code>null</code>).
   * 
   * @return the length of the file, or recursive size of the directory, 
   *         provided (in bytes).
   * 
   * @throws NullPointerException if the file is <code>null</code>
   * @throws IllegalArgumentException if the file does not exist.
   *         
   * @since 2.0
   */
  public static long sizeOf(File file) {

    if (!file.exists()) {
      String message = file + " does not exist";
      throw new IllegalArgumentException(message);
    }

    if (file.isDirectory()) {
      return sizeOfDirectory(file);
    } else {
      return file.length();
    }

  }

  /**
   * Counts the size of a directory recursively (sum of the length of all files).
   *
   * @param directory  directory to inspect, must not be <code>null</code>
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
    if (files == null) {  // null if security restricted
      return 0L;
    }
    for (File file : files) {
      size += sizeOf(file);
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
   * @throws FileExistsException if the destination directory exists
   * @throws IOException if source or destination is invalid
   * @throws IOException if an IO error occurs moving the file
   * @since 1.4
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
      throw new FileExistsException("Destination '" + destDir + "' already exists");
    }
    boolean rename = srcDir.renameTo(destDir);
    if (!rename) {
      if (destDir.getCanonicalPath().startsWith(srcDir.getCanonicalPath())) {
        throw new IOException("Cannot move directory: "+srcDir+" to a subdirectory of itself: "+destDir);
      }
      copyDirectory( srcDir, destDir );
      deleteDirectory( srcDir );
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
   * @throws FileExistsException if the destination file exists
   * @throws IOException if source or destination is invalid
   * @throws IOException if an IO error occurs moving the file
   * @since 1.4
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
      throw new FileExistsException("Destination '" + destFile + "' already exists");
    }
    if (destFile.isDirectory()) {
      throw new IOException("Destination '" + destFile + "' is a directory");
    }
    boolean rename = srcFile.renameTo(destFile);
    if (!rename) {
      copyFile( srcFile, destFile );
      if (!srcFile.delete()) {
        FileUtils.deleteQuietly(destFile);
        throw new IOException("Failed to delete original file '" + srcFile +
            "' after copy to '" + destFile + "'");
      }
    }
  }

  /**
   * Determines whether the specified file is a Symbolic Link rather than an actual file.
   * <p>
   * Will not return true if there is a Symbolic Link anywhere in the path,
   * only if the specific file is.
   * <p>
   * <b>Note:</b> the current implementation always returns {@code false} if the system
   * is detected as Windows using {@link FilenameUtils#isSystemWindows()}
   * 
   * @param file the file to check
   * @return true if the file is a Symbolic Link
   * @throws IOException if an IO error occurs while checking the file
   * @since 2.0
   */
  public static boolean isSymlink(File file) throws IOException {
    if (file == null) {
      throw new NullPointerException("File must not be null");
    }
    if (FilenameUtils.isSystemWindows()) {
      return false;
    }
    File fileInCanonicalDir = null;
    if (file.getParent() == null) {
      fileInCanonicalDir = file;
    } else {
      File canonicalDir = file.getParentFile().getCanonicalFile();
      fileInCanonicalDir = new File(canonicalDir, file.getName());
    }

    if (fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile())) {
      return false;
    } else {
      return true;
    }
  }
}
