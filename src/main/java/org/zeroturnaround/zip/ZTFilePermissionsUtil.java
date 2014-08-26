package org.zeroturnaround.zip;

class ZTFilePermissionsUtil {
  
  private ZTFilePermissionsUtil() {
  }
  
  private static ZTFilePermissionsStrategy DEFAULT_STRATEGY = new Java5FileApiFilePermissionsStrategy();
  
  private static final int OWNER_READ_FLAG =     0400;
  private static final int OWNER_WRITE_FLAG =    0200;
  private static final int OWNER_EXECUTE_FLAG =  0100;
  
  private static final int GROUP_READ_FLAG =     0040;
  private static final int GROUP_WRITE_FLAG =    0020;
  private static final int GROUP_EXECUTE_FLAG =  0010;
  
  private static final int OTHERS_READ_FLAG =    0004;
  private static final int OTHERS_WRITE_FLAG =   0002;
  private static final int OTHERS_EXECUTE_FLAG = 0001;
  
  static ZTFilePermissionsStrategy getDefaultStategy() {
    return DEFAULT_STRATEGY;
  }
  
  static int toPosixFileMode(ZTFilePermissions permissions) {
    int mode = 0;
    
    mode |= addFlag(permissions.isOwnerCanExecute(),  OWNER_EXECUTE_FLAG);
    mode |= addFlag(permissions.isGroupCanExecute(),  GROUP_EXECUTE_FLAG);
    mode |= addFlag(permissions.isOthersCanExecute(), OTHERS_EXECUTE_FLAG);
    
    mode |= addFlag(permissions.isOwnerCanWrite(),  OWNER_WRITE_FLAG);
    mode |= addFlag(permissions.isGroupCanWrite(),  GROUP_WRITE_FLAG);
    mode |= addFlag(permissions.isOthersCanWrite(), OTHERS_WRITE_FLAG);
    
    mode |= addFlag(permissions.isOwnerCanRead(),  OWNER_READ_FLAG);
    mode |= addFlag(permissions.isGroupCanRead(),  GROUP_READ_FLAG);
    mode |= addFlag(permissions.isOthersCanRead(), OTHERS_READ_FLAG);
    
    return mode;
  }
  
  private static int addFlag(boolean condition, int flag) {
    return condition ? flag : 0;
  }
  
  static ZTFilePermissions fromPosixFileMode(int mode) {
    ZTFilePermissions permissions = new ZTFilePermissions();
    
    permissions.setOwnerCanExecute( (mode &  OWNER_EXECUTE_FLAG)  > 0 );
    permissions.setGroupCanExecute( (mode &  GROUP_EXECUTE_FLAG)  > 0 );
    permissions.setOthersCanExecute((mode &  OTHERS_EXECUTE_FLAG) > 0 );
    
    permissions.setOwnerCanWrite( (mode &  OWNER_WRITE_FLAG)  > 0 );
    permissions.setGroupCanWrite( (mode &  GROUP_WRITE_FLAG)  > 0 );
    permissions.setOthersCanWrite((mode &  OTHERS_WRITE_FLAG) > 0 );
    
    permissions.setOwnerCanRead( (mode &  OWNER_READ_FLAG)  > 0 );
    permissions.setGroupCanRead( (mode &  GROUP_READ_FLAG)  > 0 );
    permissions.setOthersCanRead((mode &  OTHERS_READ_FLAG) > 0 );
    
    return permissions;
  }
  
}
