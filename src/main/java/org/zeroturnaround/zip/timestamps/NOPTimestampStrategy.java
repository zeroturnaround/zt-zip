package org.zeroturnaround.zip.timestamps;
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
 * No operation implementation of the timestamp strategy. See {@link Java8TimestampStrategy}
 * for a implementation.
 */
public class NOPTimestampStrategy implements TimestampStrategy {

  public void preserveCreationTime(ZipEntry newInstance, ZipEntry oldInstance) {
  }

  public void preserveLastModifiedTime(ZipEntry newInstance, ZipEntry oldInstance) {
  }

  public void preserveLastAccessedTime(ZipEntry newInstance, ZipEntry oldInstance) {
  }

}
