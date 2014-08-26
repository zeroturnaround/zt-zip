package org.zeroturnaround.zip;

import java.io.File;

class Java5FileApiFilePermissionsStrategy implements ZTFilePermissionsStrategy {

  public ZTFilePermissions getPermissions(File file) {
    ZTFilePermissions permissions = new ZTFilePermissions();
    
    permissions.setDirectory(file.isDirectory());
    
    if (file.canExecute()) {
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
    file.setExecutable(permissions.isOwnerCanExecute(), !permissions.isGroupCanExecute() && !permissions.isOthersCanExecute());
    file.setWritable(permissions.isOwnerCanWrite(), !permissions.isGroupCanWrite() && !permissions.isOthersCanWrite());
    file.setReadable(permissions.isOwnerCanRead(), !permissions.isGroupCanRead() && !permissions.isOthersCanRead());
  }
}
