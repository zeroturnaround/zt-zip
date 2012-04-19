package org.zeroturnaround.zip;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import junit.framework.TestCase;

public class FileUtilTest extends TestCase {
  public void testGetTempFileFor() throws Exception {
    File tmpFile = File.createTempFile("prefix", "suffix");
    File file = FileUtil.getTempFileFor(tmpFile);
    assertNotNull(file);
  }

  public void testCopy() throws Exception {
    File outFile = File.createTempFile("prefix", "suffix");
    File inFile = new File(MainExamplesTest.DEMO_ZIP);
    OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
    FileUtil.copy(inFile, out);
    out.close();
    assertEquals(inFile.length(), outFile.length());
  }
}
