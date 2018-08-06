package org.zeroturnaround.zip.path;

import java.io.ByteArrayInputStream;
import org.zeroturnaround.zip.*;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.zeroturnaround.zip.commons.FileUtils;

import org.junit.Assert;

/**
 * Tests that need a temporary directory for the test.
 */
public class ZipPathUtilDirTest {

  @Rule
  public TemporaryFolder tempSrcDir = new TemporaryFolder();

  @Rule
  public TemporaryFolder tempVerificationDir = new TemporaryFolder();

  @Test
  public void testPackDirectoryToStream() throws Exception {
    // set up directory to be packed
    Path sourceDir = tempSrcDir.getRoot().toPath();
    Path file1 = ZipPathUtilTest.file("TestFile.txt");
    Path file2 = ZipPathUtilTest.file("TestFile-II.txt");
    Files.copy(file1, sourceDir.resolve(file1.getFileName()));
    Files.copy(file2, sourceDir.resolve(file2.getFileName()));
    ByteArrayOutputStream actualOs = new ByteArrayOutputStream(1024);

    // execute test
    ZipPathUtil.pack(sourceDir, actualOs);

    // verify
    Path verificationDir = tempVerificationDir.getRoot().toPath();
    // comparing bytes is not reliable across JDK versions, so we
    // unpack the archive and compare files instead
    ZipPathUtil.unpack(new ByteArrayInputStream(actualOs.toByteArray()), verificationDir);
    Assert.assertTrue(FileUtils.contentEquals(file1.toFile(), verificationDir.resolve(file1.getFileName()).toFile()));
    Assert.assertTrue(FileUtils.contentEquals(file2.toFile(), verificationDir.resolve(file2.getFileName()).toFile()));
  }

}
