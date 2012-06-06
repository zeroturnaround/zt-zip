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
   * Copies and transforms the given entry into the ZIP output stream.
   */
  void transform(InputStream in, ZipEntry zipEntry, ZipOutputStream out) throws IOException;

}
