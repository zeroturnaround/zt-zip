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

import org.zeroturnaround.zip.ZTZipReflectionUtil;

/**
 * The getInstance() of this method will return a JDK8 implementation when
 * running on JVM 8 and a no operation instance when running on older JVM.
 * 
 * @since 1.9
 */
public class TimestampStrategyFactory {
  private static TimestampStrategy INSTANCE = new TimestampStrategyFactory().getStrategy();

  private TimestampStrategyFactory() {
  }

  private TimestampStrategy getStrategy() {
    if (ZTZipReflectionUtil.isClassAvailable(ZTZipReflectionUtil.JAVA8_STREAM_API)) {
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
