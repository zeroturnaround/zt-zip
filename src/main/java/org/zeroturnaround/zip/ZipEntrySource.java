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
import java.io.InputStream;
import java.util.zip.ZipEntry;


/**
 * ZIP entry with its contents.
 * 
 * @author Rein Raudjärv
 */
public interface ZipEntrySource {
  
  /**
   * @return path of the given entry (not <code>null</code>).
   */
  String getPath();

  /**
   * @return meta-data of the given entry (not <code>null</code>).
   */
  ZipEntry getEntry();
  
  /**
   * @return an input stream of the given entry 
   *    or <code>null</code> if this entry is a directory.
   */
  InputStream getInputStream() throws IOException;
  
}
