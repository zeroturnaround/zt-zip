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
import junit.framework.TestCase;

public class ByteSourceTest extends TestCase {

  public void testNullBytesDenotesDirectoryEntry() throws Exception {
    // The byte[] addEntry/replaceEntry overloads document null bytes as "directory"; the
    // constructor must accept that instead of throwing NullPointerException on bytes.clone().
    ByteSource source = new ByteSource("dir/", null);

    assertEquals("dir/", source.getPath());
    assertNull(source.getInputStream());
    assertEquals("dir/", source.getEntry().getName());
  }
}
