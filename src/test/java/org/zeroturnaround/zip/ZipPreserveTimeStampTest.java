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
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.zeroturnaround.zip.commons.FileUtils;

/**
 * These tests shouldn't be run with JDK8 as we preserve
 * lastModified, create, lastAccessed times for JDK8 and
 * the tests here that target the older API would fail.
 */
public class ZipPreserveTimeStampTest {
  private File srcZipFile;
  private File destZipFile;
  private ZipFile zf;

  @ClassRule
  public final static SkipIfZipEntryFileTimeNotAvailableRule skipRule = new SkipIfZipEntryFileTimeNotAvailableRule();

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
  public void testPreservingTimestamps() {
    // this construct doesn't add any entries but will trigger a re-pack with
    // the same files and preserve the time stamps
    Zips.get(srcZipFile).addEntries(new ZipEntrySource[0]).preserveTimestamps().destination(destZipFile).process();
    validateTimeStamps();
  }

  @Test
  public void testPreservingTimestampsSetter() {
    // this construct doesn't add any entries but will trigger a re-pack with
    // the same files and preserve the time stamps
    Zips.get(srcZipFile).addEntries(new ZipEntrySource[0]).setPreserveTimestamps(true).destination(destZipFile).process();
    validateTimeStamps();
  }
  
  private void validateTimeStamps() {
    Zips.get(destZipFile).iterate(new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        String name = zipEntry.getName();
        Assert.assertEquals("Timestamps differ at entry " + name, zf.getEntry(name).getTime(), zipEntry.getTime());
      }
    });
  }
}
