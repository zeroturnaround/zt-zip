package org.zeroturnaround.zip.transform;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.zeroturnaround.zip.FileSource;
import org.zeroturnaround.zip.commons.FileUtils;
import org.zeroturnaround.zip.commons.IOUtils;

public abstract class FileZipEntryTransformer implements ZipEntryTransformer {

  /**
   * Copies and transforms the given file into the output file.
   *
   * @param zipEntry
   *          zip entry metadata
   * @param in
   *          zip entry contents
   * @param out
   *          file to write transformed contents
   *
   * @throws IOException if file is not found or writing to it fails
   *
   */
  protected abstract void transform(ZipEntry zipEntry, File in, File out) throws IOException;

  /**
   * Copies the input stream to the file, then transforms the file.
   * FileSource is added then to the output stream.
   *
   * @param in
   *          input stream of the entry contents
   * @param zipEntry
   *          zip entry metadata
   * @param out
   *          ignored, because we're working on files
   *
   * @throws IOException if anything goes wrong
   */
  public void transform(InputStream in, ZipEntry zipEntry, ZipOutputStream out) throws IOException {
    File inFile = null;
    File outFile = null;
    try {
      // Files.createTempFile makes owner-only files on POSIX, so other users cannot read the entry data.
      inFile = Files.createTempFile("zip", null).toFile();
      outFile = Files.createTempFile("zip", null).toFile();
      copy(in, inFile);
      transform(zipEntry, inFile, outFile);
      FileSource source = new FileSource(zipEntry.getName(), outFile);
      ZipEntrySourceZipEntryTransformer.addEntry(source, out);
    }
    finally {
      FileUtils.deleteQuietly(inFile);
      FileUtils.deleteQuietly(outFile);
    }
  }

  private static void copy(InputStream in, File file) throws IOException {
    OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
    try {
      IOUtils.copy(in, out);
    }
    finally {
      IOUtils.closeQuietly(out);
    }
  }

}
