package org.zeroturnaround.zip;

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
