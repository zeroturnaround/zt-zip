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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;

public final class ZTFileUtil {
  private ZTFileUtil() {
  }

  public static Collection<File> listFiles(File dir) {
    return listFiles(dir, null);
  }
  
  public static Collection<File> listFiles(File dir, FileFilter filter) {
    Collection<File> accumulator = new ArrayList<File>();

    if (dir.isFile()) {
      return accumulator;
    }
    
    if (filter == null) {
      // Set default filter to accept any file
      filter = new FileFilter() {
        public boolean accept(File pathname) {
          return true;
        }
      };
    }

    innerListFiles(dir, accumulator, filter);
    return accumulator;
  }

  private static void innerListFiles(File dir, Collection<File> accumulator, FileFilter filter) {

    String[] filenames = dir.list();

    if (filenames != null) {
      for (int i = 0; i < filenames.length; i++) {
        File file = new File(dir, filenames[i]);
        if (file.isDirectory()) {
          innerListFiles(file, accumulator, filter);
        }
        else {
          if (filter != null && filter.accept(file)) {
            accumulator.add(file);
          }
        }
      }
    }
  }
}
