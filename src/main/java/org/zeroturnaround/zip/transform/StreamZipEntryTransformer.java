package org.zeroturnaround.zip.transform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public abstract class StreamZipEntryTransformer implements ZipEntryTransformer {

  /**
   * Copies and transforms the given input stream into the output stream.
   *
   * @param zipEntry
   *         zip entry metadata
   * @param in
   *         zip entry contents
   * @param out
   *         output stream to write the transformed entry
   *
   * @throws IOException if writing transformed entry fails
   *
   */
  protected abstract void transform(ZipEntry zipEntry, InputStream in, OutputStream out) throws IOException;

  /**
   * Transforms the input stream entry, writes that to output stream, closes entry in the output stream.
   *
   * @param in input stream of the entry contents
   * @param zipEntry zip entry metadata
   * @param out output stream to write transformed entry (if necessary)
   *
   * @throws IOException if anything goes wrong
   */
  public void transform(InputStream in, ZipEntry zipEntry, ZipOutputStream out) throws IOException {
    ZipEntry entry = new ZipEntry(zipEntry.getName());
    entry.setTime(System.currentTimeMillis());
    out.putNextEntry(entry);
    transform(zipEntry, in, out);
    out.closeEntry();
  }

}
