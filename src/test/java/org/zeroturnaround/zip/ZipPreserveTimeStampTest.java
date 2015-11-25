package org.zeroturnaround.zip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.zeroturnaround.zip.commons.FileUtils;

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
import junit.framework.TestCase;

/**
 * These tests shouldn't be run with JDK8 as we preserve
 * lastModified, create, lastAccessed times for JDK8 and
 * the tests here that target the older API would fail.
 */
@RunWith(SkipForJava8Runner.class)
public class ZipPreserveTimeStampTest extends TestCase {

  @Test
  public void testPreservingTimestamps() throws IOException {
    File src = new File(MainExamplesTest.DEMO_ZIP);

    File dest = File.createTempFile("temp", ".zip");
    final ZipFile zf = new ZipFile(src);
    try {
      Zips.get(src).addEntries(new ZipEntrySource[0]).preserveTimestamps().destination(dest).process();
      Zips.get(dest).iterate(new ZipEntryCallback() {
        public void process(InputStream in, ZipEntry zipEntry) throws IOException {
          String name = zipEntry.getName();
          assertEquals("Timestamps differ at entry " + name, zf.getEntry(name).getTime(), zipEntry.getTime());
        }
      });
    }
    finally {
      ZipUtil.closeQuietly(zf);
      FileUtils.deleteQuietly(dest);
    }
  }

  @Test
  public void testPreservingTimestampsSetter() throws IOException {
    File src = new File(MainExamplesTest.DEMO_ZIP);

    File dest = File.createTempFile("temp", ".zip");
    final ZipFile zf = new ZipFile(src);
    try {
      // this construct doesn't add any entries but will trigger a repack with
      // the same files and preserve the timestamps
      Zips.get(src).addEntries(new ZipEntrySource[0]).setPreserveTimestamps(true).destination(dest).process();
      Zips.get(dest).iterate(new ZipEntryCallback() {
        public void process(InputStream in, ZipEntry zipEntry) throws IOException {
          String name = zipEntry.getName();
          assertEquals("Timestamps differ at entry " + name, zf.getEntry(name).getTime(), zipEntry.getTime());
        }
      });
    }
    finally {
      ZipUtil.closeQuietly(zf);
      FileUtils.deleteQuietly(dest);
    }
  }
}
