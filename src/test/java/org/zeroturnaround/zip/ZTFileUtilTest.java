package org.zeroturnaround.zip;
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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collection;

import org.zeroturnaround.zip.commons.FileUtils;

import junit.framework.TestCase;

public class ZTFileUtilTest extends TestCase {
  public void testGetTempFileFor() throws Exception {
    File tmpFile = File.createTempFile("prefix", "suffix");
    File file = FileUtils.getTempFileFor(tmpFile);
    assertNotNull(file);
  }

  public void testCopy() throws Exception {
    File outFile = File.createTempFile("prefix", "suffix");
    File inFile = new File(MainExamplesTest.DEMO_ZIP);
    OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
    FileUtils.copy(inFile, out);
    out.close();
    assertEquals(inFile.length(), outFile.length());
  }

  public void testListFiles() {
    Collection<File> files = ZTFileUtil.listFiles(new File("."), new FileFilter() {

      public boolean accept(File pathname) {
        if (pathname.toString().endsWith("." + File.separator + "pom.xml")) {
          return true;
        }
        else {
          return false;
        }
      }
    });

    assertEquals(1, files.size());
  }

  public void testListFilesFromFile() {
    Collection files = ZTFileUtil.listFiles(new File("pom.xml"), null);
    assertEquals(files.size(), 0);
  }

  public void testListFilesFromNonExistent() {
    Collection files = ZTFileUtil.listFiles(new File("don'tExist"), null);
    assertEquals(files.size(), 0);
  }
}
