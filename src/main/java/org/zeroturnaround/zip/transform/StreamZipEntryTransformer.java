package org.zeroturnaround.zip.transform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public abstract class StreamZipEntryTransformer implements ZipEntryTransformer {
  
  /**
   * Copies and transforms the given input stream into the output stream.
   */
  protected abstract void transform(ZipEntry zipEntry, InputStream in, OutputStream out) throws IOException;

  public void transform(InputStream in, ZipEntry zipEntry, ZipOutputStream out) throws IOException {
    ZipEntry entry = new ZipEntry(zipEntry.getName());
    entry.setTime(System.currentTimeMillis());
    out.putNextEntry(entry);
    transform(zipEntry, in, out);
    out.closeEntry();
  }

}
