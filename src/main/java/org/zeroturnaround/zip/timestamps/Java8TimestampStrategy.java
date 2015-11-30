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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.zip.ZipEntry;

/**
 * This strategy will cache and call the lastModifiedTime, creationTime and
 * lastAccessTime methods through reflection. Don't use this class unless
 * you are running JDK8. It will through exceptions in that case for a 
 * fail fast approach.
 */
public class Java8TimestampStrategy implements TimestampStrategy {
  private final Class<?> fileTimeClass;
  private final Method setLastModifiedTimeMethod;
  private final Method getLastModifiedTimeMethod;
  private final Method setCreationTime;
  private final Method getCreationTime;
  private final Method setLastAccessTime;
  private final Method getLastAccessTime;

  public Java8TimestampStrategy() {
    try {
      fileTimeClass = Class.forName("java.nio.file.attribute.FileTime");

      setLastModifiedTimeMethod = ZipEntry.class.getMethod("setLastModifiedTime", fileTimeClass);
      getLastModifiedTimeMethod = ZipEntry.class.getMethod("getLastModifiedTime");

      setCreationTime = ZipEntry.class.getMethod("setCreationTime", fileTimeClass);
      getCreationTime = ZipEntry.class.getMethod("getCreationTime");

      setLastAccessTime = ZipEntry.class.getMethod("setLastAccessTime", fileTimeClass);
      getLastAccessTime = ZipEntry.class.getMethod("getLastAccessTime");
    }
    catch (ClassNotFoundException e) {
      throw new RuntimeException("You shouldn't instanciate this class when not running JDK 8", e);
    }
    catch (NoSuchMethodException e) {
      throw new RuntimeException("You shouldn't instanciate this class when not running JDK 8", e);
    }
    catch (SecurityException e) {
      throw new RuntimeException("You shouldn't instanciate this class when not running JDK 8", e);
    }
  }

  public void preserveCreationTime(ZipEntry newInstance, ZipEntry oldInstance) {
    invokeMethod(getCreationTime, setCreationTime, newInstance, oldInstance);
  }

  public void preserveLastModifiedTime(ZipEntry newInstance, ZipEntry oldInstance) {
    invokeMethod(getLastModifiedTimeMethod, setLastModifiedTimeMethod, newInstance, oldInstance);
  }

  public void preserveLastAccessedTime(ZipEntry newInstance, ZipEntry oldInstance) {
    invokeMethod(getLastAccessTime, setLastAccessTime, newInstance, oldInstance);
  }

  private void invokeMethod(Method getMethod, Method setMethod, ZipEntry newInstance, ZipEntry oldInstance) {
    try {
      Object prevValue = getMethod.invoke(oldInstance);
      setMethod.invoke(newInstance, prevValue);
    }
    catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    }
    catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}
