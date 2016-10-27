package org.zeroturnaround.zip.commons;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class adds some convenience methods on top of Apache CommonsIO FileUtils.
 * It exists so that the class it extends can contain code only from Apache Commons IO, which simplifies upgrades.
 */
public class FileUtils extends FileUtilsV2_2 {

  /**
   * Instances should NOT be constructed in standard programming.
   */
  public FileUtils() {
    super();
  }

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
}
