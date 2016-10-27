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

import java.io.File;

/**
 * This is a class that has been made significantly smaller (deleted a bunch of methods) and originally
 * is from the Apache Commons IO 2.2 package (the latest version that supports Java 1.5). All license and other documentation is intact.
 * 
 * General filename and filepath manipulation utilities.
 * <p>
 * When dealing with filenames you can hit problems when moving from a Windows based development machine to a Unix based production machine. This class aims to help avoid those
 * problems.
 * <p>
 * <b>NOTE</b>: You may be able to avoid using this class entirely simply by using JDK {@link java.io.File File} objects and the two argument constructor
 * {@link java.io.File#File(java.io.File, java.lang.String) File(File,String)}.
 * <p>
 * Most methods on this class are designed to work the same on both Unix and Windows. Those that don't include 'System', 'Unix' or 'Windows' in their name.
 * <p>
 * Most methods recognise both separators (forward and back), and both sets of prefixes. See the javadoc of each method for details.
 * <p>
 * This class defines six components within a filename (example C:\dev\project\file.txt):
 * <ul>
 * <li>the prefix - C:\</li>
 * <li>the path - dev\project\</li>
 * <li>the full path - C:\dev\project\</li>
 * <li>the name - file.txt</li>
 * <li>the base name - file</li>
 * <li>the extension - txt</li>
 * </ul>
 * Note that this class works best if directory filenames end with a separator. If you omit the last separator, it is impossible to determine if the filename corresponds to a file
 * or a directory. As a result, we have chosen to say it corresponds to a file.
 * <p>
 * This class only supports Unix and Windows style names. Prefixes are matched as follows:
 * 
 * <pre>
 * Windows:
 * a\b\c.txt           --&gt; ""          --&gt; relative
 * \a\b\c.txt          --&gt; "\"         --&gt; current drive absolute
 * C:a\b\c.txt         --&gt; "C:"        --&gt; drive relative
 * C:\a\b\c.txt        --&gt; "C:\"       --&gt; absolute
 * \\server\a\b\c.txt  --&gt; "\\server\" --&gt; UNC
 * 
 * Unix:
 * a/b/c.txt           --&gt; ""          --&gt; relative
 * /a/b/c.txt          --&gt; "/"         --&gt; absolute
 * ~/a/b/c.txt         --&gt; "~/"        --&gt; current user
 * ~                   --&gt; "~/"        --&gt; current user (slash added)
 * ~user/a/b/c.txt     --&gt; "~user/"    --&gt; named user
 * ~user               --&gt; "~user/"    --&gt; named user (slash added)
 * </pre>
 * 
 * Both prefix styles are matched always, irrespective of the machine that you are currently running on.
 * <p>
 * Origin of code: Excalibur, Alexandria, Tomcat, Commons-Utils.
 *
 * @author <a href="mailto:burton@relativity.yi.org">Kevin A. Burton</A>
 * @author <a href="mailto:sanders@apache.org">Scott Sanders</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author <a href="mailto:Christoph.Reck@dlr.de">Christoph.Reck</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:jefft@apache.org">Jeff Turner</a>
 * @author Matthew Hawthorne
 * @author Martin Cooper
 * @author <a href="mailto:jeremias@apache.org">Jeremias Maerki</a>
 * @author Stephen Colebourne
 * @version $Id: FilenameUtils.java 609870 2008-01-08 04:46:26Z niallp $
 * @since Commons IO 1.1
 */
public class FilenameUtils {

  /**
   * The extension separator character.
   * 
   * @since Commons IO 1.4
   */
  public static final char EXTENSION_SEPARATOR = '.';

  /**
   * The extension separator String.
   * 
   * @since Commons IO 1.4
   */
  public static final String EXTENSION_SEPARATOR_STR = (Character.valueOf(EXTENSION_SEPARATOR)).toString();

  /**
   * The Unix separator character.
   */
  private static final char UNIX_SEPARATOR = '/';

  /**
   * The Windows separator character.
   */
  private static final char WINDOWS_SEPARATOR = '\\';

  /**
   * The system separator character.
   */
  private static final char SYSTEM_SEPARATOR = File.separatorChar;

  /**
   * Instances should NOT be constructed in standard programming.
   */
  public FilenameUtils() {
    super();
  }

  // -----------------------------------------------------------------------
  /**
   * Determines if Windows file system is in use.
   * 
   * @return true if the system is Windows
   */
  static boolean isSystemWindows() {
    return SYSTEM_SEPARATOR == WINDOWS_SEPARATOR;
  }

  // -----------------------------------------------------------------------
  /**
   * Checks if the character is a separator.
   * 
   * @param ch the character to check
   * @return true if it is a separator character
   */
  private static boolean isSeparator(char ch) {
    return (ch == UNIX_SEPARATOR) || (ch == WINDOWS_SEPARATOR);
  }

  // -----------------------------------------------------------------------
  /**
   * Returns the length of the filename prefix, such as <code>C:/</code> or <code>~/</code>.
   * <p>
   * This method will handle a file in either Unix or Windows format.
   * <p>
   * The prefix length includes the first slash in the full filename if applicable. Thus, it is possible that the length returned is greater than the length of the input string.
   * 
   * <pre>
   * Windows:
   * a\b\c.txt           --&gt; ""          --&gt; relative
   * \a\b\c.txt          --&gt; "\"         --&gt; current drive absolute
   * C:a\b\c.txt         --&gt; "C:"        --&gt; drive relative
   * C:\a\b\c.txt        --&gt; "C:\"       --&gt; absolute
   * \\server\a\b\c.txt  --&gt; "\\server\" --&gt; UNC
   * 
   * Unix:
   * a/b/c.txt           --&gt; ""          --&gt; relative
   * /a/b/c.txt          --&gt; "/"         --&gt; absolute
   * ~/a/b/c.txt         --&gt; "~/"        --&gt; current user
   * ~                   --&gt; "~/"        --&gt; current user (slash added)
   * ~user/a/b/c.txt     --&gt; "~user/"    --&gt; named user
   * ~user               --&gt; "~user/"    --&gt; named user (slash added)
   * </pre>
   * <p>
   * The output will be the same irrespective of the machine that the code is running on. ie. both Unix and Windows prefixes are matched regardless.
   *
   * @param filename the filename to find the prefix in, null returns -1
   * @return the length of the prefix, -1 if invalid or null
   */
  public static int getPrefixLength(String filename) {
    if (filename == null) {
      return -1;
    }
    int len = filename.length();
    if (len == 0) {
      return 0;
    }
    char ch0 = filename.charAt(0);
    if (ch0 == ':') {
      return -1;
    }
    if (len == 1) {
      if (ch0 == '~') {
        return 2; // return a length greater than the input
      }
      return (isSeparator(ch0) ? 1 : 0);
    }
    else {
      if (ch0 == '~') {
        int posUnix = filename.indexOf(UNIX_SEPARATOR, 1);
        int posWin = filename.indexOf(WINDOWS_SEPARATOR, 1);
        if (posUnix == -1 && posWin == -1) {
          return len + 1; // return a length greater than the input
        }
        posUnix = (posUnix == -1 ? posWin : posUnix);
        posWin = (posWin == -1 ? posUnix : posWin);
        return Math.min(posUnix, posWin) + 1;
      }
      char ch1 = filename.charAt(1);
      if (ch1 == ':') {
        ch0 = Character.toUpperCase(ch0);
        if (ch0 >= 'A' && ch0 <= 'Z') {
          if (len == 2 || isSeparator(filename.charAt(2)) == false) {
            return 2;
          }
          return 3;
        }
        return -1;

      }
      else if (isSeparator(ch0) && isSeparator(ch1)) {
        int posUnix = filename.indexOf(UNIX_SEPARATOR, 2);
        int posWin = filename.indexOf(WINDOWS_SEPARATOR, 2);
        if ((posUnix == -1 && posWin == -1) || posUnix == 2 || posWin == 2) {
          return -1;
        }
        posUnix = (posUnix == -1 ? posWin : posUnix);
        posWin = (posWin == -1 ? posUnix : posWin);
        return Math.min(posUnix, posWin) + 1;
      }
      else {
        return (isSeparator(ch0) ? 1 : 0);
      }
    }
  }
}
