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
import java.util.zip.ZipEntry;

import junit.framework.TestCase;


public class ZipEntryUtilTest extends TestCase {
  
  public void testSetUnixFileMode() {
    ZipEntry entry = new ZipEntry("test1");
    
    ZipEntryUtil.setZTFilePermissions(entry, ZTFilePermissionsUtil.fromPosixFileMode(0654));
    int modeFromExtra = ZTFilePermissionsUtil.toPosixFileMode(ZipEntryUtil.getZTFilePermissions(entry));
    assertEquals(0654, modeFromExtra);    
  }
  
  public void testGetUnixModeOnEntryWithoutExtra() {
    ZipEntry entry = new ZipEntry("test2");
    
    assertNull(ZipEntryUtil.getZTFilePermissions(entry));
  }
}
