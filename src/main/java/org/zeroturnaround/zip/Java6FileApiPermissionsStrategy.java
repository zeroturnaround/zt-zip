package org.zeroturnaround.zip;

import java.io.File;
import java.lang.reflect.Method;

class Java6FileApiPermissionsStrategy implements ZTFilePermissionsStrategy {
  private final Method canExecuteMethod;
  private final Method setExecutableMethod;
  private final Method setWritableMethod;
  private final Method setReadableMethod;
  
  public Java6FileApiPermissionsStrategy() throws ZipException {
    canExecuteMethod = ZTZipReflectionUtil.getDeclaredMethod(File.class, "canExecute");
    setExecutableMethod = ZTZipReflectionUtil.getDeclaredMethod(File.class, "setExecutable", boolean.class, boolean.class);
    setReadableMethod = ZTZipReflectionUtil.getDeclaredMethod(File.class, "setReadable", boolean.class, boolean.class);
    setWritableMethod = ZTZipReflectionUtil.getDeclaredMethod(File.class, "setWritable", boolean.class, boolean.class);
  }

  public ZTFilePermissions getPermissions(File file) {
    ZTFilePermissions permissions = new ZTFilePermissions();
    
    permissions.setDirectory(file.isDirectory());
    
    if (canExecute(file)) {
      // set execute flag only for owner
      permissions.setOwnerCanExecute(true);
    }
    
    if (file.canWrite()) {
      // 0644 for files and 0666 for directories 
      // this is a quite common choice for shared installations
      permissions.setOwnerCanWrite(true); 
      if (file.isDirectory()) {
        permissions.setGroupCanWrite(true);
        permissions.setOthersCanWrite(true);
      }
    }
    
    if (file.canRead()) {
      permissions.setOwnerCanRead(true);
      permissions.setGroupCanRead(true);
      permissions.setOthersCanRead(true);
    }
    
    return permissions;
  }

  public void setPermissions(File file, ZTFilePermissions permissions) {
    setExecutable(file, permissions.isOwnerCanExecute(), !permissions.isGroupCanExecute() && !permissions.isOthersCanExecute());
    setWritable(file, permissions.isOwnerCanWrite(), !permissions.isGroupCanWrite() && !permissions.isOthersCanWrite());
    setReadable(file, permissions.isOwnerCanRead(), !permissions.isGroupCanRead() && !permissions.isOthersCanRead());
  }
  
  private boolean setExecutable(File file, boolean executable, boolean ownerOnly) {
    return (Boolean) ZTZipReflectionUtil.invoke(setExecutableMethod, file, executable, ownerOnly);
  }
  
  private boolean setWritable(File file, boolean executable, boolean ownerOnly) {
    return (Boolean) ZTZipReflectionUtil.invoke(setWritableMethod, file, executable, ownerOnly);
  }
  
  private boolean setReadable(File file, boolean executable, boolean ownerOnly) {
    return (Boolean) ZTZipReflectionUtil.invoke(setReadableMethod, file, executable, ownerOnly);
  }
  
  private boolean canExecute(File file) {
    return (Boolean) ZTZipReflectionUtil.invoke(canExecuteMethod, file);
  }
}
