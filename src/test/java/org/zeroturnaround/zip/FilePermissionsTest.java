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
