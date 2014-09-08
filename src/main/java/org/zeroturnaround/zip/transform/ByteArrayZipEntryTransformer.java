package org.zeroturnaround.zip.transform;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.commons.IOUtils;

public abstract class ByteArrayZipEntryTransformer implements ZipEntryTransformer {

  /**
   * Transforms the given byte array into a new one.
   *
   * @param zipEntry
   *          entry to transform
   * @param input
   *          entry contents
   *
   * @return byte[]
   *           the transformed contents of the entry
   *
   * @throws IOException
   *           if anything goes wrong
   */
  protected abstract byte[] transform(ZipEntry zipEntry, byte[] input) throws IOException;

  /**
   * Transforms the zip entry given as an input stream and ZipEntry metadata.
   * The result is written to a ZipOutputStream
   *   * @param in input stream of the entry contents
   * @param zipEntry zip entry metadata
   * @param out output stream to write transformed entry
   *
   * @throws IOException if anything goes wrong

   */
  public void transform(InputStream in, ZipEntry zipEntry, ZipOutputStream out) throws IOException {
    byte[] bytes = IOUtils.toByteArray(in);
    bytes = transform(zipEntry, bytes);

    ByteSource source;

    if (preserveTimestamps()) {
      source = new ByteSource(zipEntry.getName(), bytes, zipEntry.getTime());
    }
    else {
      source = new ByteSource(zipEntry.getName(), bytes);
    }

    ZipEntrySourceZipEntryTransformer.addEntry(source, out);
  }

  /**
   * Override to return true if needed.
   *
   * @return true if this transformer should preserve timestamp of the entry it transforms, false otherwise
   */
  protected boolean preserveTimestamps() {
    return false;
  }

}
