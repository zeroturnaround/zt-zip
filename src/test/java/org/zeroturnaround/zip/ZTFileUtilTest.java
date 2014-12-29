package org.zeroturnaround.zip;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collection;

import junit.framework.TestCase;

import org.zeroturnaround.zip.commons.FileUtils;

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
        if (pathname.toString().endsWith("./pom.xml")) {
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
