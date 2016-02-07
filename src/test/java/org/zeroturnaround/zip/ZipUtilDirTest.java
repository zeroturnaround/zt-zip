package org.zeroturnaround.zip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.zeroturnaround.zip.commons.FileUtils;

import org.junit.Assert;

/**
 * Tests that need a temporary directory for the test.
 */
public class ZipUtilDirTest {

  @Rule
  public TemporaryFolder tempSrcDir = new TemporaryFolder();

  @Rule
  public TemporaryFolder tempVerificationDir = new TemporaryFolder();

  @Test
  public void testPackDirectoryToStream() throws Exception {
    // set up directory to be packed
    File sourceDir = tempSrcDir.getRoot();
    File file1 = ZipUtilTest.file("TestFile.txt");
    File file2 = ZipUtilTest.file("TestFile-II.txt");
    FileUtils.copyFileToDirectory(file1, sourceDir);
    FileUtils.copyFileToDirectory(file2, sourceDir);
    ByteArrayOutputStream actualOs = new ByteArrayOutputStream(1024);

    // execute test
    ZipUtil.pack(sourceDir, actualOs);

    // verify
    File verificationDir = tempVerificationDir.getRoot();
    // comparing bytes is not reliable across JDK versions, so we
    // unpack the archive and compare files instead
    ZipUtil.unpack(new ByteArrayInputStream(actualOs.toByteArray()), verificationDir);
    Assert.assertTrue(FileUtils.contentEquals(file1, new File(verificationDir, file1.getName())));
    Assert.assertTrue(FileUtils.contentEquals(file2, new File(verificationDir, file2.getName())));
  }

}
