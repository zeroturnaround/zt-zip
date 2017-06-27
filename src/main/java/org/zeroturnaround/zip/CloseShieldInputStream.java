package org.zeroturnaround.zip;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

/**
 * Filter stream that prevents the underlying input stream from being closed.
 * <p>
 *   It's a simpler version than <code>org.apache.commons.io.input.CloseShieldInputStream</code> as it doesn't replace the underlying stream.
 *   It's only meant for internal use where we can close a {@link ZipInputStream} without closing the underlying stream itself.
 * </p>
 */
class CloseShieldInputStream extends FilterInputStream {

  /**
   * Creates a <code>FilterInputStream</code>
   * by assigning the  argument <code>in</code>
   * to the field <code>this.in</code> so as
   * to remember it for later use.
   *
   * @param in the underlying input stream, or <code>null</code> if
   *           this instance is to be created without an underlying stream.
   */
  public CloseShieldInputStream(InputStream in) {
    super(in);
  }

  @Override
  public void close() throws IOException {
    // do nothing
  }

}
