package org.zeroturnaround.zip.transform;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Call-back for traversing ZIP entries with their contents and producing a new ZIP file as an output.
 * 
 * @author Rein Raudjärv
 */
public interface ZipEntryTransformer {

  /**
   * Transforms the zip entry given as an input stream and ZipEntry metadata.
   * The result is written to a ZipOutputStream
   *
   * @param in input stream of the entry contents
   * @param zipEntry zip entry metadata
   * @param out output stream to write transformed entry (if necessary)
   *
   * @throws IOException if anything goes wrong
   */
  void transform(InputStream in, ZipEntry zipEntry, ZipOutputStream out) throws IOException;

}
