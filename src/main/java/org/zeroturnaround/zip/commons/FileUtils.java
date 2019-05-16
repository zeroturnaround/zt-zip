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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;

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

    if (!Files.isSymbolicLink(directory.toPath())) {
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
    if (!Files.isSymbolicLink(directory.toPath())) {
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
}
