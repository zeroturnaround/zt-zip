package org.zeroturnaround.zip.timestamps;
import java.nio.file.attribute.FileTime;
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

/**
 * This strategy will call lastModifiedTime, creationTime and
 * lastAccessTime methods (added in Java 8). Don't use this class unless
 * you are running Java 8.
 * 
 * @since 1.9
 */
public class Java8TimestampStrategy implements TimestampStrategy {

  public void setTime(ZipEntry newInstance, ZipEntry oldInstance) {
    {
      FileTime time = oldInstance.getCreationTime();
      if (time != null) {
        newInstance.setCreationTime(time);
      }
    }
    {
      FileTime time = oldInstance.getLastModifiedTime();
      if (time != null) {
        newInstance.setLastModifiedTime(time);
      }
    }
    {
      FileTime time = oldInstance.getLastAccessTime();
      if (time != null) {
        newInstance.setLastAccessTime(time);
      }
    }
  }

}
