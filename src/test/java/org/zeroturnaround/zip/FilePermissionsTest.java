package org.zeroturnaround.zip;

import java.io.File;
import java.io.IOException;

import org.zeroturnaround.zip.commons.FileUtils;

import junit.framework.TestCase;

public class FilePermissionsTest extends TestCase {
  private final File testFile = new File(getClass().getClassLoader().getResource("TestFile.txt").getPath());
  
  public void testPreserveExecuteFlag() throws IOException {
    File tmpDir = File.createTempFile("FilePermissionsTest-", null);
    tmpDir.delete();
    tmpDir.mkdir();
    
    File file = new File(tmpDir, "TestFile.txt");
    FileUtils.copyFile(testFile, file);
    
    assertTrue(file.exists());
    file.setExecutable(true);
    
    File tmpZip = File.createTempFile("FilePermissionsTest-", ".zip");
    ZipUtil.pack(tmpDir, tmpZip);
    FileUtils.deleteDirectory(tmpDir);
    ZipUtil.unpack(tmpZip, tmpDir);
    
    assertTrue(file.exists());
    assertTrue(file.canExecute());
  }
  
}
