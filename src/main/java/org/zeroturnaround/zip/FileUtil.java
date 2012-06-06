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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;

/**
 * Some file manipulating utilities missing from <code>Commons IO</code>.
 * 
 * @author Rein Raudjärv
 */
final class FileUtil {
  
  private FileUtil() {}
  
  /**
   * Copies the given file into an output stream.
   * 
   * @param file input file (must exist).
   * @param out output stream.
   */
  public static void copy(File file, OutputStream out) throws IOException {
    FileInputStream in = new FileInputStream(file);
    try {
      IOUtils.copy(new BufferedInputStream(in), out);
    }
    finally {
      IOUtils.closeQuietly(in);
    }
  }
  
  /**
   * Copies the given input stream into a file.
   * <p>
   * The target file must not be a directory and its parent must exist.
   * 
   * @param in source stream.
   * @param file output file to be created or overwritten.
   */
  public static void copy(InputStream in, File file) throws IOException {
    OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
    try {
      IOUtils.copy(in, out);
    }
    finally {
      IOUtils.closeQuietly(out);
    }
  }
  
  /**
   * Find a non-existing file in the same directory using the same name as prefix.
   * 
   * @param file file used for the name and location (it is not read or written).
   * @return a non-existing file in the same directory using the same name as prefix.
   */
  public static File getTempFileFor(File file) {
    File parent = file.getParentFile();
    String name = file.getName();
    File result;
    int index = 0;
    do {
     result = new File(parent, name + "_" + index++); 
    }
    while (result.exists());
    return result;
  }
  
}
