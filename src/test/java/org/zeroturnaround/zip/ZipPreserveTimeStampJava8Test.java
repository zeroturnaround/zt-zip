package org.zeroturnaround.zip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zeroturnaround.zip.commons.FileUtils;

public class ZipPreserveTimeStampJava8Test {
  private File srcZipFile;
  private File destZipFile;
  private ZipFile zf;

  @Rule
  public final SkipIfClassNotAvailableRule skipRule = new SkipIfClassNotAvailableRule(ZTZipReflectionUtil.JAVA8_STREAM_API);

  @Before
  public void setUp() throws IOException {
    srcZipFile = new File(MainExamplesTest.DEMO_ZIP);
    destZipFile = File.createTempFile("temp", ".zip");
    zf = new ZipFile(srcZipFile);
  }

  @After
  public void tearDown() throws Exception {
    ZipUtil.closeQuietly(zf);
    FileUtils.deleteQuietly(destZipFile);
  }

  @Test
  public void testDontPreserveTime() {
    // this construct doesn't add any entries but will trigger a re-pack with
    // the same files and preserve the time stamps
    Zips.get(srcZipFile).addEntries(new ZipEntrySource[0]).destination(destZipFile).process();
    Zips.get(destZipFile).iterate(new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        String name = zipEntry.getName();
        Assert.assertNotEquals(zf.getEntry(name).getLastModifiedTime(), zipEntry.getLastModifiedTime());
      }
    });
  }
  
  @Test
  public void testPreserveTime() {
    // this construct doesn't add any entries but will trigger a re-pack with
    // the same files and preserve the time stamps
    Zips.get(srcZipFile).addEntries(new ZipEntrySource[0]).preserveTimestamps().destination(destZipFile).process();
    validateTimeStampEquality();
  }
  
  @Test
  public void testPreserveTimeWithSetter() {
    // this construct doesn't add any entries but will trigger a re-pack with
    // the same files and preserve the time stamps
    Zips.get(srcZipFile).addEntries(new ZipEntrySource[0]).setPreserveTimestamps(true).destination(destZipFile).process();
    validateTimeStampEquality();
  }
  
  private void validateTimeStampEquality() {
    Zips.get(destZipFile).iterate(new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        String name = zipEntry.getName();
        Assert.assertEquals(zf.getEntry(name).getLastModifiedTime(), zipEntry.getLastModifiedTime());
        Assert.assertEquals(zf.getEntry(name).getLastAccessTime(), zipEntry.getLastAccessTime());
        Assert.assertEquals(zf.getEntry(name).getCreationTime(), zipEntry.getCreationTime());
      }
    });
  }
}
