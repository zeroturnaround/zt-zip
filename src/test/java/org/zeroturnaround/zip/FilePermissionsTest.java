package org.zeroturnaround.zip;

import java.io.File;

import org.zeroturnaround.zip.commons.FileUtils;

import junit.framework.TestCase;

public class FilePermissionsTest extends TestCase {
  private final File testFile = new File(getClass().getClassLoader().getResource("TestFile.txt").getPath());
  
  public void testPreserveExecuteFlag() throws Exception {
    File tmpDir = File.createTempFile("FilePermissionsTest-", null);
    tmpDir.delete();
    tmpDir.mkdir();
    
    File fileA = new File(tmpDir, "fileA.txt");
    File fileB = new File(tmpDir, "fileB.txt");
    FileUtils.copyFile(testFile, fileA);
    FileUtils.copyFile(testFile, fileB);
    
    assertTrue(fileA.exists());
    setExecutable(fileA, true);
    
    File tmpZip = File.createTempFile("FilePermissionsTest-", ".zip");
    ZipUtil.pack(tmpDir, tmpZip);
    FileUtils.deleteDirectory(tmpDir);
    ZipUtil.unpack(tmpZip, tmpDir);
    
    assertTrue(fileA.exists());
    assertTrue(canExecute(fileA));
    assertTrue(fileB.exists());
    assertFalse(canExecute(fileB));
  }
  
  private boolean canExecute(File file) throws Exception {
    return (Boolean) File.class.getDeclaredMethod("canExecute").invoke(file);
  }
  
  private boolean setExecutable(File file, boolean executable) throws Exception {
    return (Boolean) File.class.getDeclaredMethod("setExecutable", boolean.class).invoke(file, executable);
  }
  
}
