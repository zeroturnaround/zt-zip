package org.zeroturnaround.zip.extra;

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
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.zeroturnaround.zip.ZipUtil;

import junit.framework.TestCase;

public class AsiExtraFieldTest extends TestCase {

  /*
   * Local file data layout consumed by parseFromLocalFileData(data, offset, length):
   *   data[offset .. offset+3]   CRC32 of the remaining bytes (tmp)
   *   tmp[0..1]                  mode (ZipShort)
   *   tmp[2..5]                  symbolic-link length (ZipLong)
   *   tmp[6..7]                  uid (ZipShort)
   *   tmp[8..9]                  gid (ZipShort)
   *   tmp[10..]                  link target bytes
   */
  private static byte[] localFileData(int mode, long declaredLinkLength, byte[] linkBytes) {
    byte[] tmp = new byte[10 + linkBytes.length];
    System.arraycopy(ZipShort.getBytes(mode), 0, tmp, 0, 2);
    System.arraycopy(ZipLong.getBytes(declaredLinkLength), 0, tmp, 2, 4);
    System.arraycopy(linkBytes, 0, tmp, 10, linkBytes.length);

    CRC32 crc = new CRC32();
    crc.update(tmp);

    byte[] data = new byte[4 + tmp.length];
    System.arraycopy(ZipLong.getBytes(crc.getValue()), 0, data, 0, 4);
    System.arraycopy(tmp, 0, data, 4, tmp.length);
    return data;
  }

  public void testParseRejectsOversizedLinkLengthWithoutAllocating() throws Exception {
    // A forged link length far larger than the bytes actually present must be rejected before
    // it is used to size an array, otherwise it triggers a ~2 GB allocation per parsed entry.
    byte[] data = localFileData(0, Integer.MAX_VALUE, new byte[0]);

    try {
      new AsiExtraField().parseFromLocalFileData(data, 0, data.length);
      fail();
    }
    catch (ZipException e) {
      assertEquals("Invalid symbolic link length " + Integer.MAX_VALUE + " in ASI extra field", e.getMessage());
    }
  }

  public void testParseRejectsNegativeLinkLength() throws Exception {
    byte[] data = localFileData(0, 0xFFFFFFFFL, new byte[0]); // larger than the bytes present (read as an unsigned 4-byte value)

    try {
      new AsiExtraField().parseFromLocalFileData(data, 0, data.length);
      fail();
    }
    catch (ZipException e) {
      assertEquals("Invalid symbolic link length " + 0xFFFFFFFFL + " in ASI extra field", e.getMessage());
    }
  }

  public void testParseRejectsShortFieldWithoutCrashing() throws Exception {
    // A declared length below the fixed-field minimum (CRC + mode + link length + uid + gid = 14)
    // must be rejected with a ZipException, not an unchecked ArrayIndexOutOfBoundsException or
    // NegativeArraySizeException from reading/allocating past the supplied bytes.
    int[] shortLengths = { 0, 2, 4, 10, 13 };
    for (int length : shortLengths) {
      byte[] data = new byte[Math.max(length, 4)];
      try {
        new AsiExtraField().parseFromLocalFileData(data, 0, length);
        fail("expected ZipException for length " + length);
      }
      catch (ZipException e) {
        assertEquals("ASI extra field is too short: " + length + " bytes", e.getMessage());
      }
    }
  }

  public void testUnpackDoesNotCrashOnShortAsiExtraField() throws Exception {
    // An entry carrying a truncated ASI (0x756E) extra field must not abort unpack with an
    // unchecked exception; the bad permission field is ignored and unpacking completes.
    File zip = File.createTempFile("asi-short-extra", ".zip");
    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
    try {
      ZipEntry entry = new ZipEntry("good.txt");
      // [header id 0x756E, little-endian][data length 4, little-endian][4 bytes of data]
      entry.setExtra(new byte[] { 0x6E, 0x75, 0x04, 0x00, 0, 0, 0, 0 });
      out.putNextEntry(entry);
      out.write("hi".getBytes("UTF-8"));
      out.closeEntry();
    }
    finally {
      out.close();
    }

    File outDir = Files.createTempDirectory("asi-short-out").toFile();
    ZipUtil.unpack(zip, outDir);

    assertTrue(new File(outDir, "good.txt").exists());
  }

  public void testParseAcceptsValidLink() throws Exception {
    byte[] link = "x".getBytes("UTF-8");
    byte[] data = localFileData(0120000 /* LINK_FLAG */, link.length, link);

    AsiExtraField field = new AsiExtraField();
    field.parseFromLocalFileData(data, 0, data.length);

    assertTrue(field.isLink());
    assertEquals("x", field.getLinkedFile());
  }
}
