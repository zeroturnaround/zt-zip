package org.zeroturnaround.zip;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;

public final class ZTFileUtil {
  private ZTFileUtil() {
  }

  public static Collection listFiles(File dir) {
    return listFiles(dir, null);
  }
  
  public static Collection listFiles(File dir, FileFilter filter) {
    Collection rtrn = new ArrayList();

    if (dir.isFile()) {
      return rtrn;
    }
    
    if (filter == null) {
      // Set default filter to accept any file
      filter = new FileFilter() {
        public boolean accept(File pathname) {
          return true;
        }
      };
    }

    innerListFiles(dir, rtrn, filter);
    return rtrn;
  }

  private static void innerListFiles(File dir, Collection rtrn, FileFilter filter) {

    File[] files = dir.listFiles();

    if (files != null) {
      for (int i = 0; i < files.length; i++) {
        if (files[i].isDirectory()) {
          innerListFiles(files[i], rtrn, filter);
        }
        else {
          if (filter != null && filter.accept(files[i])) {
            rtrn.add(files[i]);
          }
        }
      }
    }
  }
}
