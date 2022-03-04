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
import java.nio.file.Files;

import junit.framework.TestCase;

public class BrokenJarTest extends TestCase {
  private static final File file = new File("src/test/resources/scalactic_2.13-3.2.5.jar");

  public void testZipFileWithBrokenPermissions() throws Exception {    
    File tmpDir = Files.createTempDirectory("zt-zip-tests").toFile();
    ZipUtil.unpack(file, tmpDir);
  }
}
