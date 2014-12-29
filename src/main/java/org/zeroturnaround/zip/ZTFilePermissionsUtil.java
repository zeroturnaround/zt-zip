package org.zeroturnaround.zip;

import java.io.File;

/**
 * Utilities to manipulate {@link ZTFilePermissions}.
 * 
 * @author Viktor Karabut
 */
class ZTFilePermissionsUtil {
  
  private ZTFilePermissionsUtil() {
  }
  
  private static final int OWNER_READ_FLAG =     0400;
  private static final int OWNER_WRITE_FLAG =    0200;
  private static final int OWNER_EXECUTE_FLAG =  0100;
  
  private static final int GROUP_READ_FLAG =     0040;
  private static final int GROUP_WRITE_FLAG =    0020;
  private static final int GROUP_EXECUTE_FLAG =  0010;
  
  private static final int OTHERS_READ_FLAG =    0004;
  private static final int OTHERS_WRITE_FLAG =   0002;
  private static final int OTHERS_EXECUTE_FLAG = 0001;
  
  /**
   * Get most appropriate {@link ZTFilePermissionsStrategy} based on Java version and OS.
   * 
   * @return
   */
  static ZTFilePermissionsStrategy getDefaultStategy() {
    return DEFAULT_STRATEGY;
  }
  
  /**
   * Convert {@link ZTFilePermissions} to POSIX file permission bit array.
   * 
   * 
   * @param permissions permissions
   * @return Posix mode
   */
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
  
  /**
   * Convert Posix mode to {@link ZTFilePermissions}
   * 
   * @param mode
   * @return
   */
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
  
  /**
   * Empty {@link ZTFilePermissionsStrategy} implementation.
   */
  private static final ZTFilePermissionsStrategy NOP_STRATEGY = new ZTFilePermissionsStrategy() {
    public void setPermissions(File file, ZTFilePermissions permissions) {
      // do nothing
    }
    
    public ZTFilePermissions getPermissions(File file) {
      // do nothing
      return null;
    }
  };
  
  private static final ZTFilePermissionsStrategy DEFAULT_STRATEGY = fetchDefaultStrategy();
  
  private static ZTFilePermissionsStrategy fetchDefaultStrategy() {
    ZTFilePermissionsStrategy strategy = tryInstantiateStrategy(Java7Nio2ApiPermissionsStrategy.class);
    
    if (strategy == null) {
      strategy = tryInstantiateStrategy(Java6FileApiPermissionsStrategy.class);
    }
    
    if (strategy == null) {
      strategy = NOP_STRATEGY;
    }
    
    return strategy;
  }
  
  private static ZTFilePermissionsStrategy tryInstantiateStrategy(Class<? extends ZTFilePermissionsStrategy> clazz) {
    try {
      return clazz.newInstance();
    }
    catch (Exception e) {
      // failed to instantiate strategy by some reason
      return null;
    }
  }
  
}
