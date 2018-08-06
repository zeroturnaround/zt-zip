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
import java.io.IOException;

import org.junit.Assume;
import org.zeroturnaround.zip.commons.FileUtils;

import org.junit.Test;
import static org.junit.Assert.*;


public class FilePermissionsTest {
  private final File testFile = new File(getClass().getClassLoader().getResource("TestFile.txt").getPath());

  @Test
  public void testPreserveExecuteFlag() throws Exception {
    String dirName = "FilePermissionsTest-e";

    File tmpDir = File.createTempFile(dirName, null);
    tmpDir.delete();
    tmpDir.mkdir();
    File fileA = new File(tmpDir, "fileA.txt");
    File fileB = new File(tmpDir, "fileB.txt");
    FileUtils.copyFile(testFile, fileA);
    FileUtils.copyFile(testFile, fileB);

    Assume.assumeTrue(fileA.setExecutable(false));
    //Avoids failing test on Windows: File.setExecutable(): "If executable is false and the underlying file system does not implement an execute permission, then the operation will fail."

    fileA.setExecutable(true);
    fileB.setExecutable(false);

    //TESTS BEFORE ZIP
    assertTrue(fileA.exists() && fileB.exists());
    assertTrue(fileA.canExecute());
    assertFalse(fileB.canExecute());

    assertTrue(doZipAndUnpack(dirName, ".zip", tmpDir));

    //SAME TESTS AFTER ZIP & UNZIP
    assertTrue(fileA.exists() && fileB.exists());
    assertTrue(fileA.canExecute());
    assertFalse(fileB.canExecute());
  }

  @Test
  public void testPreserveReadFlag() throws Exception {
    String dirName = "FilePermissionsTest-r";

    File tmpDir = File.createTempFile(dirName, null);
    tmpDir.delete();
    tmpDir.mkdir();
    File fileA = new File(tmpDir, "fileA.txt");
    File fileB = new File(tmpDir, "fileB.txt");
    FileUtils.copyFile(testFile, fileA);
    FileUtils.copyFile(testFile, fileB);

    Assume.assumeTrue(fileA.setReadable(false));
    //Avoids failing test on Windows: File.setReadable(): "If readable is false and the underlying file system does not implement a read permission, then the operation will fail."

    fileA.setReadable(true);
    fileB.setReadable(false);
    fileB.setReadable(true);//if we set read permission to false, then we can't zip the file, causing the test to fail with a permission exception

    //TESTS BEFORE ZIP
    assertTrue(fileA.exists() && fileB.exists());
    assertTrue(fileA.canRead());
    assertTrue(fileB.canRead());

    assertTrue(doZipAndUnpack(dirName, ".zip", tmpDir));

    //SAME TESTS AFTER ZIP & UNZIP
    assertTrue(fileA.exists() && fileB.exists());
    assertTrue(fileA.canRead());
    assertTrue(fileB.canRead());
  }

  /** This is the only test that can be run on Windows to test that permissions are kept after zip and unzip. */
  @Test
  public void testPreserveWriteFlag() throws Exception {
    String dirName = "FilePermissionsTest-w";

    File tmpDir = File.createTempFile(dirName, null);
    tmpDir.delete();
    tmpDir.mkdir();
    File fileA = new File(tmpDir, "fileA.txt");
    File fileB = new File(tmpDir, "fileB.txt");
    FileUtils.copyFile(testFile, fileA, true);
    FileUtils.copyFile(testFile, fileB, true);

    //Assume.assumeTrue(setWritable(fileA, false));
    //this is commented because there is no OS-specific logic for returning false in File.setWritable(), only "The operation will fail if the user does not have permission to change the access permissions of this abstract pathname."

    fileA.setWritable(true);
    fileB.setWritable(false);

    //TESTS BEFORE ZIP
    assertTrue(fileA.exists() && fileB.exists());
    assertTrue(fileA.canWrite());
    assertFalse(fileB.canWrite());

    assertTrue(doZipAndUnpack(dirName, ".zip", tmpDir));

    //SAME TESTS AFTER ZIP & UNZIP
    assertTrue(fileA.exists() && fileB.exists());
    assertTrue(fileA.canWrite());
    assertFalse(fileB.canWrite());
  }

  private boolean doZipAndUnpack(String prefix, String suffix, File rootDir) throws IOException {
    File tmpZip = File.createTempFile(prefix, suffix);
    ZipUtil.pack(rootDir, tmpZip);
    FileUtils.deleteDirectory(rootDir);
    if(directoryHasFiles(rootDir)) {return false;}//if the directory isn't empty after the delete, we can't (easily) test to see if the unzip created files in that directory
    ZipUtil.unpack(tmpZip, rootDir);
    return directoryHasFiles(rootDir);//if the directory has files after they were all previously deleted, then the unzip worked 
  }

  private static boolean directoryHasFiles(File directory) {
    if(directory==null || !directory.exists() || directory.listFiles()==null) {return false;}
    return directory.listFiles().length > 0;
  }
}
