package org.zeroturnaround.zip;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * ZTFilePermissionsStrategy which uses Java 7 posix file permissions
 * 
 * @author VIktor Karabut
 */
class Java7Nio2ApiPermissionsStrategy implements ZTFilePermissionsStrategy {
  private final Class<? extends Enum<?>> posixFilePermissionClass;
  private final Class<?> filesClass;
  private final Class<?> pathClass;
  private final Class<? extends Enum<?>> linkOptionClass;
  private final Enum<?>[] linkOptionsArray;
  
  private final Method toPathMethod;
  private final Method setPosixFilePermissionsMethod;
  private final Method getPosixFilePermissionsMethod;
  
  private final Object OWNER_READ;
  private final Object OWNER_WRITE;
  private final Object OWNER_EXECUTE;
  private final Object GROUP_READ;
  private final Object GROUP_WRITE;
  private final Object GROUP_EXECUTE;
  private final Object OTHERS_READ;
  private final Object OTHERS_WRITE;
  private final Object OTHERS_EXECUTE;
  
  
  @SuppressWarnings("unchecked")
  public Java7Nio2ApiPermissionsStrategy() {
    if (!isPosix()) {
      throw new ZipException("File system does not support POSIX file attributes");
    }
    
    posixFilePermissionClass = 
        (Class<? extends Enum<?>>) ZTZipReflectionUtil.getClassForName("java.nio.file.attribute.PosixFilePermission", Enum.class);
    Enum<?>[] constants = posixFilePermissionClass.getEnumConstants();
    OWNER_READ = constants[0];
    OWNER_WRITE = constants[1];
    OWNER_EXECUTE = constants[2];
    GROUP_READ = constants[3];
    GROUP_WRITE = constants[4];
    GROUP_EXECUTE = constants[5];
    OTHERS_READ = constants[6];
    OTHERS_WRITE = constants[7];
    OTHERS_EXECUTE = constants[8];
    
    linkOptionClass = 
        (Class<? extends Enum<?>>) ZTZipReflectionUtil.getClassForName("java.nio.file.LinkOption", Enum.class);
    linkOptionsArray = (Enum<?>[]) Array.newInstance(linkOptionClass, 1);
    linkOptionsArray[0] = (Enum<?>) linkOptionClass.getEnumConstants()[0]; // LinkOption.NOFOLLOW_LINKS;
    
    filesClass = ZTZipReflectionUtil.getClassForName("java.nio.file.Files", Object.class);
    pathClass = ZTZipReflectionUtil.getClassForName("java.nio.file.Path", Object.class);
    
    toPathMethod = ZTZipReflectionUtil.getDeclaredMethod(File.class, "toPath");
    setPosixFilePermissionsMethod = ZTZipReflectionUtil.getDeclaredMethod(filesClass, "setPosixFilePermissions", pathClass, Set.class);
    getPosixFilePermissionsMethod = ZTZipReflectionUtil.getDeclaredMethod(filesClass, "getPosixFilePermissions", pathClass, linkOptionsArray.getClass());
  }

  public ZTFilePermissions getPermissions(File file) {
    ZTFilePermissions permissions = new ZTFilePermissions();
    permissions.setDirectory(file.isDirectory());
    
    Set<?> posixFilePermissions = getPosixFilePermissions(file);
    
    permissions.setOwnerCanRead(   posixFilePermissions.contains(OWNER_READ));
    permissions.setOwnerCanWrite(  posixFilePermissions.contains(OWNER_WRITE));
    permissions.setOwnerCanExecute(posixFilePermissions.contains(OWNER_EXECUTE));
    
    permissions.setGroupCanRead(   posixFilePermissions.contains(GROUP_READ));
    permissions.setGroupCanWrite(  posixFilePermissions.contains(GROUP_WRITE));
    permissions.setGroupCanExecute(posixFilePermissions.contains(GROUP_EXECUTE));
    
    permissions.setOthersCanRead(   posixFilePermissions.contains(OTHERS_READ));
    permissions.setOthersCanWrite(  posixFilePermissions.contains(OTHERS_WRITE));
    permissions.setOthersCanExecute(posixFilePermissions.contains(OTHERS_EXECUTE));
    
    return permissions;
  }

  public void setPermissions(File file, ZTFilePermissions permissions) {
    Set<Object> set = new HashSet<Object>();
    addIf(permissions.isOwnerCanRead(), set, OWNER_READ);
    addIf(permissions.isOwnerCanRead(),   set,OWNER_READ);
    addIf(permissions.isOwnerCanWrite(),  set,OWNER_WRITE);
    addIf(permissions.isOwnerCanExecute(),set,OWNER_EXECUTE);
    
    addIf(permissions.isGroupCanRead(),   set,GROUP_READ);
    addIf(permissions.isGroupCanWrite(),  set,GROUP_WRITE);
    addIf(permissions.isGroupCanExecute(),set,GROUP_EXECUTE);
    
    addIf(permissions.isOthersCanRead(),   set,OTHERS_READ);
    addIf(permissions.isOthersCanWrite(),  set,OTHERS_WRITE);
    addIf(permissions.isOthersCanExecute(),set,OTHERS_EXECUTE);
    
    setPosixFilePermissions(file, set);
  }
  
  private <E> void addIf(boolean condition, Set<E> set, E el) {
    if (condition) {
      set.add(el);
    }
  }
  
  /**
   * Construct java.nio.file.Path object from abstract path.
   * Invokes JDK7 <code>file.toPath()</code> method through reflection.
   * 
   * @param file
   * @return instance of java.nio.file.Path object
   */
  private Object toPath(File file) {
    return ZTZipReflectionUtil.invoke(toPathMethod, file);
  }
  
  // Files.setPosixFilePermissions(file.toPath(), set);
  private void setPosixFilePermissions(File file, Set<?> set) {
    ZTZipReflectionUtil.invoke(setPosixFilePermissionsMethod, null, toPath(file), set);
  }
  
  // Files.getPosixFilePermissions(file.toPath(), new LinkOption[]{ LinkOption.NOFOLLOW_LINKS });
  private Set<?> getPosixFilePermissions(File file) {
    return (Set<?>) ZTZipReflectionUtil.invoke(getPosixFilePermissionsMethod, null, toPath(file), linkOptionsArray);
  }
  
  // FileSystems.getDefault().supportedFileAttrubuteViews().contains("posix");
  private static boolean isPosix() {
    Method getDefaultMethod = ZTZipReflectionUtil.getDeclaredMethod(
        ZTZipReflectionUtil.getClassForName("java.nio.file.FileSystems", Object.class), "getDefault");
    Method supportedFileAttributeViewsMethod = ZTZipReflectionUtil.getDeclaredMethod(
        ZTZipReflectionUtil.getClassForName("java.nio.file.FileSystem", Object.class), "supportedFileAttributeViews");
    
    Object fileSystem = ZTZipReflectionUtil.invoke(getDefaultMethod, null);
    @SuppressWarnings("unchecked")
    Set<String> views = (Set<String>) ZTZipReflectionUtil.invoke(supportedFileAttributeViewsMethod, fileSystem);
    
    return views.contains("posix");
  }
}
