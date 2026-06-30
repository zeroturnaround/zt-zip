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
import java.util.zip.CRC32;
import java.util.zip.ZipException;

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
      assertTrue(true);
    }
  }

  public void testParseRejectsNegativeLinkLength() throws Exception {
    byte[] data = localFileData(0, 0xFFFFFFFFL, new byte[0]); // reads as a negative int

    try {
      new AsiExtraField().parseFromLocalFileData(data, 0, data.length);
      fail();
    }
    catch (ZipException e) {
      assertTrue(true);
    }
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
