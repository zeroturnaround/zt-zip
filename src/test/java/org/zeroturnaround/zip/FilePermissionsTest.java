package org.zeroturnaround.zip;

import java.io.File;

import org.zeroturnaround.zip.commons.FileUtils;

import junit.framework.TestCase;

public class FilePermissionsTest extends TestCase {
  private final File testFile = new File(getClass().getClassLoader().getResource("TestFile.txt").getPath());
  
  public void beforeMethod() {
    System.out.println("Before method called!");
  }
  
  public void testPreserveExecuteFlag() throws Exception {
    File tmpDir = File.createTempFile("FilePermissionsTest-", null);
    tmpDir.delete();
    tmpDir.mkdir();
    
    File file = new File(tmpDir, "TestFile.txt");
    FileUtils.copyFile(testFile, file);
    
    assertTrue(file.exists());
    setExecutable(file, true);
    
    File tmpZip = File.createTempFile("FilePermissionsTest-", ".zip");
    ZipUtil.pack(tmpDir, tmpZip);
    FileUtils.deleteDirectory(tmpDir);
    ZipUtil.unpack(tmpZip, tmpDir);
    
    assertTrue(file.exists());
    assertTrue(canExecute(file));
  }
  
  private boolean canExecute(File file) throws Exception {
    return (Boolean) File.class.getDeclaredMethod("canExecute").invoke(file);
  }
  
  private boolean setExecutable(File file, boolean executable) throws Exception {
    return (Boolean) File.class.getDeclaredMethod("setExecutable", boolean.class).invoke(file, executable);
  }
  
}
