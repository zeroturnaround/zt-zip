package org.zeroturnaround.zip.transform;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.commons.IOUtils;

public abstract class StringZipEntryTransformer implements ZipEntryTransformer {

  /**
   * The encoding to use, null means platform default.
   */
  private final String encoding;

  public StringZipEntryTransformer() {
    this(null);
  }

  public StringZipEntryTransformer(String encoding) {
    this.encoding = encoding;
  }

  /**
   * Transforms the given String into a new one.
   *
   * @param zipEntry
   *          zip entry metadata
   * @param input
   *          zip entry contents
   *          
   * @return String - transformed entry contents
   *
   * @throws IOException if transformation cannot be completed succesfully
   */
  protected abstract String transform(ZipEntry zipEntry, String input) throws IOException;

  public void transform(InputStream in, ZipEntry zipEntry, ZipOutputStream out) throws IOException {
    String data = IOUtils.toString(in, encoding);
    data = transform(zipEntry, data);
    byte[] bytes = encoding == null ? data.getBytes() : data.getBytes(encoding);
    ByteSource source = new ByteSource(zipEntry.getName(), bytes);
    ZipEntrySourceZipEntryTransformer.addEntry(source, out);
  }

}
