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
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;

/**
 * This is a class that has been made significantly smaller (deleted a bunch of methods) and originally
 * is from the Apache Commons IO 2.2 package (the latest version that supports Java 1.5). All license and other documentation is intact.
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
 * The byte-to-char methods and char-to-byte methods involve a conversion step.
 * Two methods are provided in each case, one that uses the platform default
 * encoding and the other which allows you to specify an encoding. You are
 * encouraged to always specify an encoding because relying on the platform
 * default can lead to unexpected results, for example when moving from
 * development to production.
 * <p>
 * All the methods in this class that read a stream are buffered internally.
 * This means that there is no cause to use a <code>BufferedInputStream</code>
 * or <code>BufferedReader</code>. The default buffer size of 4K has been shown
 * to be efficient in tests.
 * <p>
 * Wherever possible, the methods in this class do <em>not</em> flush or close
 * the stream. This is to avoid making non-portable assumptions about the
 * streams' origin and further use. Thus the caller is still responsible for
 * closing streams after use.
 * <p>
 * Origin of code: Excalibur.
 *
 * @version $Id: IOUtils.java 1304177 2012-03-23 03:36:44Z ggregory $
 */
public class IOUtils {
  // NOTE: This class is focussed on InputStream, OutputStream, Reader and
  // Writer. Each method should take at least one of these as a parameter,
  // or return one of them.

  private static final int EOF = -1;
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
    StringBuilderWriter buf = new StringBuilderWriter(4);
    PrintWriter out = new PrintWriter(buf);
    out.println();
    LINE_SEPARATOR = buf.toString();
    out.close();
  }

  /**
   * The default buffer size ({@value}) to use for 
   * {@link #copyLarge(InputStream, OutputStream)}
   * and
   * {@link #copyLarge(Reader, Writer)}
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
   * Equivalent to {@link InputStream#close()}, except any exceptions will be ignored.
   * This is typically used in finally blocks.
   * <p>
   * Example code:
   * <pre>
   *   byte[] data = new byte[1024];
   *   InputStream in = null;
   *   try {
   *       in = new FileInputStream("foo.txt");
   *       in.read(data);
   *       in.close(); //close errors are handled
   *   } catch (Exception e) {
   *       // error handling
   *   } finally {
   *       IOUtils.closeQuietly(in);
   *   }
   * </pre>
   *
   * @param input  the InputStream to close, may be null or already closed
   */
  public static void closeQuietly(InputStream input) {
    closeQuietly((Closeable)input);
  }

  /**
   * Unconditionally close an <code>OutputStream</code>.
   * <p>
   * Equivalent to {@link OutputStream#close()}, except any exceptions will be ignored.
   * This is typically used in finally blocks.
   * <p>
   * Example code:
   * <pre>
   * byte[] data = "Hello, World".getBytes();
   *
   * OutputStream out = null;
   * try {
   *     out = new FileOutputStream("foo.txt");
   *     out.write(data);
   *     out.close(); //close errors are handled
   * } catch (IOException e) {
   *     // error handling
   * } finally {
   *     IOUtils.closeQuietly(out);
   * }
   * </pre>
   *
   * @param output  the OutputStream to close, may be null or already closed
   */
  public static void closeQuietly(OutputStream output) {
    closeQuietly((Closeable)output);
  }

  /**
   * Unconditionally close a <code>Closeable</code>.
   * <p>
   * Equivalent to {@link Closeable#close()}, except any exceptions will be ignored.
   * This is typically used in finally blocks.
   * <p>
   * Example code:
   * <pre>
   *   Closeable closeable = null;
   *   try {
   *       closeable = new FileReader("foo.txt");
   *       // process closeable
   *       closeable.close();
   *   } catch (Exception e) {
   *       // error handling
   *   } finally {
   *       IOUtils.closeQuietly(closeable);
   *   }
   * </pre>
   *
   * @param closeable the object to close, may be null or already closed
   * @since 2.0
   */
  public static void closeQuietly(Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (IOException ioe) {
      // ignore
    }
  }

  // read toByteArray
  //-----------------------------------------------------------------------
  /**
   * Get the contents of an <code>InputStream</code> as a <code>byte[]</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * 
   * @param input  the <code>InputStream</code> to read from
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
   * Character encoding names can be found at
   * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * 
   * @param input  the <code>InputStream</code> to read from
   * @param encoding  the encoding to use, null means platform default
   * @return the requested String
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs
   */
  public static String toString(InputStream input, String encoding)
      throws IOException {
    StringBuilderWriter sw = new StringBuilderWriter();
    copy(input, sw, encoding);
    return sw.toString();
  }

  // copy from InputStream
  //-----------------------------------------------------------------------
  /**
   * Copy bytes from an <code>InputStream</code> to an
   * <code>OutputStream</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * <p>
   * Large streams (over 2GB) will return a bytes copied value of
   * <code>-1</code> after the copy has completed since the correct
   * number of bytes cannot be returned as an int. For large streams
   * use the <code>copyLarge(InputStream, OutputStream)</code> method.
   * 
   * @param input  the <code>InputStream</code> to read from
   * @param output  the <code>OutputStream</code> to write to
   * @return the number of bytes copied, or -1 if &gt; Integer.MAX_VALUE
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   * @since 1.1
   */
  public static int copy(InputStream input, OutputStream output) throws IOException {
    long count = copyLarge(input, output);
    if (count > Integer.MAX_VALUE) {
      return -1;
    }
    return (int) count;
  }

  /**
   * Copy bytes from a large (over 2GB) <code>InputStream</code> to an
   * <code>OutputStream</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * <p>
   * The buffer size is given by {@link #DEFAULT_BUFFER_SIZE}.
   * 
   * @param input  the <code>InputStream</code> to read from
   * @param output  the <code>OutputStream</code> to write to
   * @return the number of bytes copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   * @since 1.3
   */
  public static long copyLarge(InputStream input, OutputStream output)
      throws IOException {
    return copyLarge(input, output, new byte[DEFAULT_BUFFER_SIZE]);
  }

  /**
   * Copy bytes from a large (over 2GB) <code>InputStream</code> to an
   * <code>OutputStream</code>.
   * <p>
   * This method uses the provided buffer, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * <p>
   * 
   * @param input  the <code>InputStream</code> to read from
   * @param output  the <code>OutputStream</code> to write to
   * @param buffer the buffer to use for the copy
   * @return the number of bytes copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   * @since 2.2
   */
  public static long copyLarge(InputStream input, OutputStream output, byte[] buffer)
      throws IOException {
    long count = 0;
    int n = 0;
    while (EOF != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  /**
   * Copy bytes from an <code>InputStream</code> to chars on a
   * <code>Writer</code> using the default character encoding of the platform.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * <p>
   * This method uses {@link InputStreamReader}.
   *
   * @param input  the <code>InputStream</code> to read from
   * @param output  the <code>Writer</code> to write to
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   * @since 1.1
   */
  public static void copy(InputStream input, Writer output)
      throws IOException {
    InputStreamReader in = new InputStreamReader(input);
    copy(in, output);
  }

  /**
   * Copy bytes from an <code>InputStream</code> to chars on a
   * <code>Writer</code> using the specified character encoding.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * <p>
   * Character encoding names can be found at
   * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * This method uses {@link InputStreamReader}.
   *
   * @param input  the <code>InputStream</code> to read from
   * @param output  the <code>Writer</code> to write to
   * @param encoding  the encoding to use, null means platform default
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   * @since 1.1
   */
  public static void copy(InputStream input, Writer output, String encoding)
      throws IOException {
    if (encoding == null) {
      copy(input, output);
    } else {
      InputStreamReader in = new InputStreamReader(input, encoding);
      copy(in, output);
    }
  }

  // copy from Reader
  //-----------------------------------------------------------------------
  /**
   * Copy chars from a <code>Reader</code> to a <code>Writer</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedReader</code>.
   * <p>
   * Large streams (over 2GB) will return a chars copied value of
   * <code>-1</code> after the copy has completed since the correct
   * number of chars cannot be returned as an int. For large streams
   * use the <code>copyLarge(Reader, Writer)</code> method.
   *
   * @param input  the <code>Reader</code> to read from
   * @param output  the <code>Writer</code> to write to
   * @return the number of characters copied, or -1 if &gt; Integer.MAX_VALUE
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   * @since 1.1
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
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedReader</code>.
   * <p>
   * The buffer size is given by {@link #DEFAULT_BUFFER_SIZE}.
   *
   * @param input  the <code>Reader</code> to read from
   * @param output  the <code>Writer</code> to write to
   * @return the number of characters copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   * @since 1.3
   */
  public static long copyLarge(Reader input, Writer output) throws IOException {
    return copyLarge(input, output, new char[DEFAULT_BUFFER_SIZE]);
  }

  /**
   * Copy chars from a large (over 2GB) <code>Reader</code> to a <code>Writer</code>.
   * <p>
   * This method uses the provided buffer, so there is no need to use a
   * <code>BufferedReader</code>.
   * <p>
   *
   * @param input  the <code>Reader</code> to read from
   * @param output  the <code>Writer</code> to write to
   * @param buffer the buffer to be used for the copy
   * @return the number of characters copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   * @since 2.2
   */
  public static long copyLarge(Reader input, Writer output, char [] buffer) throws IOException {
    long count = 0;
    int n = 0;
    while (EOF != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  // content equals
  //-----------------------------------------------------------------------
  /**
   * Compare the contents of two Streams to determine if they are equal or
   * not.
   * <p>
   * This method buffers the input internally using
   * <code>BufferedInputStream</code> if they are not already buffered.
   *
   * @param input1  the first stream
   * @param input2  the second stream
   * @return true if the content of the streams are equal or they both don't
   * exist, false otherwise
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
    while (EOF != ch) {
      int ch2 = input2.read();
      if (ch != ch2) {
        return false;
      }
      ch = input1.read();
    }

    int ch2 = input2.read();
    return ch2 == EOF;
  }
}