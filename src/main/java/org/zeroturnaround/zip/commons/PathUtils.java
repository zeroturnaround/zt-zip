package org.zeroturnaround.zip.commons;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * This class adds some convenience methods on top of Paths.
 */
public class PathUtils {

  /**
   * Instances should NOT be constructed in standard programming.
   */
  private PathUtils() {
  }

  /**
   * Delete a directory and the subdirectories of a Path
   * 
   * @param directory - the subdirectorie to be deleted
   * @throws IOException if an I/O error is thrown by a file visitor method
   */
  public static void deleteDir(Path directory) throws IOException {
    if (!Files.exists(directory)) {
      return;
    }
    Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  /**
   * Find a non-existing file in the same directory using the same name as prefix.
   * 
   * @param file Path used for the name and location (it is not read or written).
   * @return a non-existing file in the same directory using the same name as prefix.
   */
  public static Path getTempFileFor(Path file) {
    Path parent = file.getParent();
    String name = file.getFileName().toString();
    Path result;
    int index = 0;
    do {
      result = parent.resolve(name + "_" + index++);
    }
    while (Files.exists(result));
    return result;
  }

  /**
   * Compares the contents of two files to determine if they are equal or not.
   * <p>
   * This method checks to see if the two files are different lengths
   * or if they point to the same file, before resorting to byte-by-byte
   * comparison of the contents.
   * <p>
   * Code origin: Avalon
   *
   * @param file1 the first file
   * @param file2 the second file
   * @return true if the content of the files are equal or they both don't
   *         exist, false otherwise
   * @throws IOException in case of an I/O error
   */
  public static boolean contentEquals(Path file1, Path file2) throws IOException {
    boolean file1Exists = Files.exists(file1);
    if (file1Exists != Files.exists(file2)) {
      return false;
    }

    if (!file1Exists) {
      // two not existing files are equal
      return true;
    }

    if (Files.isDirectory(file1) || Files.isDirectory(file2)) {
      // don't want to compare directory contents
      throw new IOException("Can't compare directories, only files");
    }
    if (Files.size(file1) != Files.size(file2)) {
      // lengths differ, cannot be equal
      return false;
    }

    if (Files.isSameFile(file1, file2)) {
      // same file
      return true;
    }
    try (InputStream input1 = Files.newInputStream(file1);
        InputStream input2 = Files.newInputStream(file2);) {
      return IOUtils.contentEquals(input1, input2);
    }
  }
}