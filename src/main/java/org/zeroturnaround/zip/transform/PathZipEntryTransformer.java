package org.zeroturnaround.zip.transform;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.zeroturnaround.zip.PathSource;
import org.zeroturnaround.zip.commons.IOUtils;

public abstract class PathZipEntryTransformer implements ZipEntryTransformer {

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
  protected abstract void transform(ZipEntry zipEntry, Path in, Path out) throws IOException;

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
    Path inFile = null;
    Path outFile = null;
    try {
      inFile = Files.createTempFile("zip", null);
      outFile = Files.createTempFile("zip", null);

      copy(in, inFile);
      transform(zipEntry, inFile, outFile);
      PathSource source = new PathSource(zipEntry.getName(), outFile);
      ZipEntrySourceZipEntryTransformer.addEntry(source, out);
    }
    finally {
      Files.deleteIfExists(inFile);
      Files.deleteIfExists(outFile);
    }
  }

  private static void copy(InputStream in, Path file) throws IOException {
    OutputStream out = new BufferedOutputStream(Files.newOutputStream(file));
    try {
      IOUtils.copy(in, out);
    }
    finally {
      IOUtils.closeQuietly(out);
    }
  }

}
