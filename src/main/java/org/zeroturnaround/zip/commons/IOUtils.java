/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zeroturnaround.zip.commons;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * This is a class that has been made significantly smaller (deleted a bunch of methods) and originally
 * is from the Apache commons-io package. All license and other documentation is intact.
 * 
 * General IO stream manipulation utilities.
 * <p>
 * This class provides static utility methods for input/output operations.
 * <ul>
 * <li>closeQuietly - these methods close a stream ignoring nulls and exceptions
 * <li>toXxx/read - these methods read data from a stream
 * <li>write - these methods write data to a stream
 * <li>copy - these methods copy all the data from one stream to another
 * <li>contentEquals - these methods compare the content of two streams
 * </ul>
 * <p>
 * The byte-to-char methods and char-to-byte methods involve a conversion step. Two methods are provided in each case, one that uses the platform default encoding and the other
 * which allows you to specify an encoding. You are encouraged to always specify an encoding because relying on the platform default can lead to unexpected results, for example
 * when moving from development to production.
 * <p>
 * All the methods in this class that read a stream are buffered internally. This means that there is no cause to use a <code>BufferedInputStream</code> or
 * <code>BufferedReader</code>. The default buffer size of 4K has been shown to be efficient in tests.
 * <p>
 * Wherever possible, the methods in this class do <em>not</em> flush or close the stream. This is to avoid making non-portable assumptions about the streams' origin and further
 * use. Thus the caller is still responsible for closing streams after use.
 * <p>
 * Origin of code: Excalibur.
 *
 * @author Peter Donald
 * @author Jeff Turner
 * @author Matthew Hawthorne
 * @author Stephen Colebourne
 * @author Gareth Davis
 * @author Ian Springer
 * @author Niall Pemberton
 * @author Sandy McArthur
 * @version $Id: IOUtils.java 481854 2006-12-03 18:30:07Z scolebourne $
 */
public class IOUtils {
  // NOTE: This class is focused on InputStream, OutputStream, Reader and
  // Writer. Each method should take at least one of these as a parameter,
  // or return one of them.

  /**
   * The Unix directory separator character.
   */
  public static final char DIR_SEPARATOR_UNIX = '/';
  /**
   * The Windows directory separator character.
   */
  public static final char DIR_SEPARATOR_WINDOWS = '\\';
  /**
   * The system directory separator character.
   */
  public static final char DIR_SEPARATOR = File.separatorChar;
  /**
   * The Unix line separator string.
   */
  public static final String LINE_SEPARATOR_UNIX = "\n";
  /**
   * The Windows line separator string.
   */
  public static final String LINE_SEPARATOR_WINDOWS = "\r\n";
  /**
   * The system line separator string.
   */
  public static final String LINE_SEPARATOR;
  static {
    // avoid security issues
    StringWriter buf = new StringWriter(4);
    PrintWriter out = new PrintWriter(buf);
    out.println();
    LINE_SEPARATOR = buf.toString();
  }

  /**
   * The default buffer size to use.
   */
  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

  /**
   * Instances should NOT be constructed in standard programming.
   */
  public IOUtils() {
    super();
  }

  /**
   * Unconditionally close an <code>InputStream</code>.
   * <p>
   * Equivalent to {@link InputStream#close()}, except any exceptions will be ignored. This is typically used in finally blocks.
   *
   * @param input the InputStream to close, may be null or already closed
   */
  public static void closeQuietly(InputStream input) {
    try {
      if (input != null) {
        input.close();
      }
    }
    catch (IOException ioe) {
      // ignore
    }
  }

  /**
   * Unconditionally close an <code>OutputStream</code>.
   * <p>
   * Equivalent to {@link OutputStream#close()}, except any exceptions will be ignored. This is typically used in finally blocks.
   *
   * @param output the OutputStream to close, may be null or already closed
   */
  public static void closeQuietly(OutputStream output) {
    try {
      if (output != null) {
        output.close();
      }
    }
    catch (IOException ioe) {
      // ignore
    }
  }

  // read toByteArray
  // -----------------------------------------------------------------------
  /**
   * Get the contents of an <code>InputStream</code> as a <code>byte[]</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedInputStream</code>.
   * 
   * @param input the <code>InputStream</code> to read from
   * @return the requested byte array
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs
   */
  public static byte[] toByteArray(InputStream input) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    copy(input, output);
    return output.toByteArray();
  }

  /**
   * Get the contents of an <code>InputStream</code> as a String
   * using the specified character encoding.
   * <p>
   * Character encoding names can be found at <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedInputStream</code>.
   * 
   * @param input the <code>InputStream</code> to read from
   * @param encoding the encoding to use, null means platform default
   * @return the requested String
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs
   */
  public static String toString(InputStream input, String encoding)
      throws IOException {
    StringWriter sw = new StringWriter();
    copy(input, sw, encoding);
    return sw.toString();
  }

  // copy from InputStream
  // -----------------------------------------------------------------------
  /**
   * Copy bytes from an <code>InputStream</code> to an <code>OutputStream</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedInputStream</code>.
   * <p>
   * Large streams (over 2GB) will return a bytes copied value of <code>-1</code> after the copy has completed since the correct number of bytes cannot be returned as an int. For
   * large streams use the <code>copyLarge(InputStream, OutputStream)</code> method.
   * 
   * @param input the <code>InputStream</code> to read from
   * @param output the <code>OutputStream</code> to write to
   * @return the number of bytes copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   * @throws ArithmeticException if the byte count is too large
   * @since Commons IO 1.1
   */
  public static int copy(InputStream input, OutputStream output) throws IOException {
    long count = copyLarge(input, output);
    if (count > Integer.MAX_VALUE) {
      return -1;
    }
    return (int) count;
  }

  /**
   * Copy bytes from a large (over 2GB) <code>InputStream</code> to an <code>OutputStream</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedInputStream</code>.
   * 
   * @param input the <code>InputStream</code> to read from
   * @param output the <code>OutputStream</code> to write to
   * @return the number of bytes copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   * @since Commons IO 1.3
   */
  public static long copyLarge(InputStream input, OutputStream output)
      throws IOException {
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    long count = 0;
    int n = 0;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  /**
   * Copy bytes from an <code>InputStream</code> to chars on a <code>Writer</code> using the default character encoding of the platform.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedInputStream</code>.
   * <p>
   * This method uses {@link InputStreamReader}.
   *
   * @param input the <code>InputStream</code> to read from
   * @param output the <code>Writer</code> to write to
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static void copy(InputStream input, Writer output)
      throws IOException {
    InputStreamReader in = new InputStreamReader(input);
    copy(in, output);
  }

  /**
   * Copy bytes from an <code>InputStream</code> to chars on a <code>Writer</code> using the specified character encoding.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedInputStream</code>.
   * <p>
   * Character encoding names can be found at <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * This method uses {@link InputStreamReader}.
   *
   * @param input the <code>InputStream</code> to read from
   * @param output the <code>Writer</code> to write to
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static void copy(InputStream input, Writer output, String encoding)
      throws IOException {
    if (encoding == null) {
      copy(input, output);
    }
    else {
      InputStreamReader in = new InputStreamReader(input, encoding);
      copy(in, output);
    }
  }

  // copy from Reader
  // -----------------------------------------------------------------------
  /**
   * Copy chars from a <code>Reader</code> to a <code>Writer</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedReader</code>.
   * <p>
   * Large streams (over 2GB) will return a chars copied value of <code>-1</code> after the copy has completed since the correct number of chars cannot be returned as an int. For
   * large streams use the <code>copyLarge(Reader, Writer)</code> method.
   *
   * @param input the <code>Reader</code> to read from
   * @param output the <code>Writer</code> to write to
   * @return the number of characters copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   * @throws ArithmeticException if the character count is too large
   * @since Commons IO 1.1
   */
  public static int copy(Reader input, Writer output) throws IOException {
    long count = copyLarge(input, output);
    if (count > Integer.MAX_VALUE) {
      return -1;
    }
    return (int) count;
  }

  /**
   * Copy chars from a large (over 2GB) <code>Reader</code> to a <code>Writer</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedReader</code>.
   *
   * @param input the <code>Reader</code> to read from
   * @param output the <code>Writer</code> to write to
   * @return the number of characters copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   * @since Commons IO 1.3
   */
  public static long copyLarge(Reader input, Writer output) throws IOException {
    char[] buffer = new char[DEFAULT_BUFFER_SIZE];
    long count = 0;
    int n = 0;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  // content equals
  // -----------------------------------------------------------------------
  /**
   * Compare the contents of two Streams to determine if they are equal or
   * not.
   * <p>
   * This method buffers the input internally using <code>BufferedInputStream</code> if they are not already buffered.
   *
   * @param input1 the first stream
   * @param input2 the second stream
   * @return true if the content of the streams are equal or they both don't
   *         exist, false otherwise
   * @throws NullPointerException if either input is null
   * @throws IOException if an I/O error occurs
   */
  public static boolean contentEquals(InputStream input1, InputStream input2)
      throws IOException {
    if (!(input1 instanceof BufferedInputStream)) {
      input1 = new BufferedInputStream(input1);
    }
    if (!(input2 instanceof BufferedInputStream)) {
      input2 = new BufferedInputStream(input2);
    }

    int ch = input1.read();
    while (-1 != ch) {
      int ch2 = input2.read();
      if (ch != ch2) {
        return false;
      }
      ch = input1.read();
    }

    int ch2 = input2.read();
    return (ch2 == -1);
  }
}
