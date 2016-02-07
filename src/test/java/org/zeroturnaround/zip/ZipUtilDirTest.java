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
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Test
  public void testPackDirectoryToStream() throws Exception {
    // set up expected value
    ByteArrayOutputStream expectedOs = new ByteArrayOutputStream(1024);
    FileUtils.copy(ZipUtilTest.file("TestFile-and-TestFile-II.zip"), expectedOs);

    // set up directory to be packed
    File destDir = tempDir.getRoot();
    FileUtils.copyFileToDirectory(ZipUtilTest.file("TestFile.txt"), destDir);
    FileUtils.copyFileToDirectory(ZipUtilTest.file("TestFile-II.txt"), destDir);
    ByteArrayOutputStream actualOs = new ByteArrayOutputStream(1024);

    // execute test
    ZipUtil.pack(destDir, actualOs);

    // verify
    Assert.assertArrayEquals(expectedOs.toByteArray(), actualOs.toByteArray());
  }

}
