/**
 *    Copyright (C) 2012 ZeroTurnaround LLC <support@zeroturnaround.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.zeroturnaround.zip;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;

/**
 * @author Toomas Romer
 * @author Oleg Shelajev
 * @author Innokenty Shuvalov
 */
public class PathSource implements ZipEntrySource {

  private final String path;
  private final Path file;

  public PathSource(String path, Path file) {
    this.path = path;
    this.file = file;
  }

  public String getPath() {
    return path;
  }

  public ZipEntry getEntry() throws IOException {
    ZipEntry entry = ZipEntryUtil.fromFile(path, file);
    return entry;
  }

  public InputStream getInputStream() throws IOException {
    if (Files.isDirectory(file)) {
      return null;
    }
    else {
      return new BufferedInputStream(Files.newInputStream(file));
    }
  }

  public String toString() {
    return "PathSource[" + path + ", " + file + "]";
  }

  /**
   * Creates a sequence of PathSource objects via mapping
   * a sequence of files to the sequence of corresponding names
   * for the entries
   * @param files file array to form the data of the objects
   *              in the resulting array
   * @param names file array to form the names of the objects
   *              in the resulting array
   * @return array of PathSource objects created by mapping
   * given files array to the given names array one by one
   * @throws java.lang.IllegalArgumentException if the names array
   * contains less items than the files array
   */
  public static PathSource[] pair(Path[] files, String[] names) {
    if (files.length > names.length) {
      throw new IllegalArgumentException("names array must contain " +
          "at least the same amount of items as files array or more");
    }

    PathSource[] result = new PathSource[files.length];
    for (int i = 0; i < files.length; i++) {
      result[i] = new PathSource(names[i], files[i]);
    }
    return result;
  }
}