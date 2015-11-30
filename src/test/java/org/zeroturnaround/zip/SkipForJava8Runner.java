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
import java.util.zip.ZipEntry;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

/**
 * JUnit runner that can be used to skip tests for JDK8. This is needed
 * for example some timestamp preserving tests that should have custom
 * JDK 8 implementations and custom pre JDK8 tests.
 */
public class SkipForJava8Runner extends BlockJUnit4ClassRunner {

  public SkipForJava8Runner(Class<?> testClass) throws InitializationError {
    super(testClass);
  }

  @Override
  protected boolean isIgnored(FrameworkMethod child) {
    return isJdk8();
  }

  private boolean isJdk8() {
    try {
      Class<?> fileTimeClass = Class.forName("java.nio.file.attribute.FileTime");
      // the following method is only in Java 1.8
      ZipEntry.class.getMethod("setLastModifiedTime", fileTimeClass);
      return true;
    }
    catch (NoSuchMethodException e) {
      // Ignore, we are not running Java 8
    }
    catch (ClassNotFoundException e) {
      // Ignore, we are not running Java 8
    }
    catch (SecurityException e) {
      // Ignore, we are not running Java 8
    }
    return false;
  }
}
