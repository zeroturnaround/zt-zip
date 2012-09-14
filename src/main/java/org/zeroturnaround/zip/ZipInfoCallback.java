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
package org.zeroturnaround.zip;

import java.io.IOException;
import java.util.zip.ZipEntry;

/**
 * Call-back for traversing ZIP entries without their contents.
 *  
 * @author Rein Raudjärv
 * 
 * @see ZipEntryCallback
 */
public interface ZipInfoCallback {
  
  /**
   * Invoked for each entry in a ZIP file.
   * 
   * @param zipEntry ZIP entry.
   */
  void process(ZipEntry zipEntry) throws IOException;
  
}