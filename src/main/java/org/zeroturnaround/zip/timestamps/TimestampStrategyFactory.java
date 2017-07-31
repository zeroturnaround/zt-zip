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
 * The getInstance() of this method will return a JDK8 implementation when
 * running on JVM 8 and a no operation instance when running on older JVM.
 * 
 * @since 1.9
 */
public class TimestampStrategyFactory {

  public static boolean HAS_ZIP_ENTRY_FILE_TIME_METHODS = hasZipEntryFileTimeMethods();

  private static TimestampStrategy INSTANCE = getStrategy();

  private TimestampStrategyFactory() {
  }

  private static boolean hasZipEntryFileTimeMethods() {
    try {
      ZipEntry.class.getDeclaredMethod("getCreationTime");
      return true;
    }
    catch (Exception e) {
      return false;
    }
  }

  private static TimestampStrategy getStrategy() {
    if (HAS_ZIP_ENTRY_FILE_TIME_METHODS) {
      return new Java8TimestampStrategy();
    }
    else {
      return new PreJava8TimestampStrategy();
    }
  }
  
  public static TimestampStrategy getInstance() {
    return INSTANCE;
  }
}
