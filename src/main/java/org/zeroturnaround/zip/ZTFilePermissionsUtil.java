package org.zeroturnaround.zip;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

/**
 * Utilities to manipulate {@link ZTFilePermissions}.
 *
 * @author Viktor Karabut
 */
class ZTFilePermissionsUtil {

  private ZTFilePermissionsUtil() {
  }

  private static final int OWNER_READ_FLAG = 0400;
  private static final int OWNER_WRITE_FLAG = 0200;
  private static final int OWNER_EXECUTE_FLAG = 0100;

  private static final int GROUP_READ_FLAG = 0040;
  private static final int GROUP_WRITE_FLAG = 0020;
  private static final int GROUP_EXECUTE_FLAG = 0010;

  private static final int OTHERS_READ_FLAG = 0004;
  private static final int OTHERS_WRITE_FLAG = 0002;
  private static final int OTHERS_EXECUTE_FLAG = 0001;

  /**
   * Convert {@link ZTFilePermissions} to POSIX file permission bit array.
   *
   *
   * @param permissions permissions
   * @return Posix mode
   */
  static int toPosixFileMode(ZTFilePermissions permissions) {
    int mode = 0;

    mode |= addFlag(permissions.isOwnerCanExecute(), OWNER_EXECUTE_FLAG);
    mode |= addFlag(permissions.isGroupCanExecute(), GROUP_EXECUTE_FLAG);
    mode |= addFlag(permissions.isOthersCanExecute(), OTHERS_EXECUTE_FLAG);

    mode |= addFlag(permissions.isOwnerCanWrite(), OWNER_WRITE_FLAG);
    mode |= addFlag(permissions.isGroupCanWrite(), GROUP_WRITE_FLAG);
    mode |= addFlag(permissions.isOthersCanWrite(), OTHERS_WRITE_FLAG);

    mode |= addFlag(permissions.isOwnerCanRead(), OWNER_READ_FLAG);
    mode |= addFlag(permissions.isGroupCanRead(), GROUP_READ_FLAG);
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

    permissions.setOwnerCanExecute((mode & OWNER_EXECUTE_FLAG) > 0);
    permissions.setGroupCanExecute((mode & GROUP_EXECUTE_FLAG) > 0);
    permissions.setOthersCanExecute((mode & OTHERS_EXECUTE_FLAG) > 0);

    permissions.setOwnerCanWrite((mode & OWNER_WRITE_FLAG) > 0);
    permissions.setGroupCanWrite((mode & GROUP_WRITE_FLAG) > 0);
    permissions.setOthersCanWrite((mode & OTHERS_WRITE_FLAG) > 0);

    permissions.setOwnerCanRead((mode & OWNER_READ_FLAG) > 0);
    permissions.setGroupCanRead((mode & GROUP_READ_FLAG) > 0);
    permissions.setOthersCanRead((mode & OTHERS_READ_FLAG) > 0);

    return permissions;
  }

  public static ZTFilePermissions getPermissions(Path file) {
    if (!isPosix()) {
      // Windows is not posix
      ZTFilePermissions permissions = new ZTFilePermissions();

      permissions.setDirectory(Files.isDirectory(file));

      if (Files.isExecutable(file)) {
        // set execute flag only for owner
        permissions.setOwnerCanExecute(true);
      }

      if (Files.isWritable(file)) {
        // 0644 for files and 0666 for directories
        // this is a quite common choice for shared installations
        permissions.setOwnerCanWrite(true);
        if (Files.isDirectory(file)) {
          permissions.setGroupCanWrite(true);
          permissions.setOthersCanWrite(true);
        }
      }

      if (Files.isReadable(file)) {
        permissions.setOwnerCanRead(true);
        permissions.setGroupCanRead(true);
        permissions.setOthersCanRead(true);
      }

      return permissions;
    }

    ZTFilePermissions permissions = new ZTFilePermissions();
    permissions.setDirectory(Files.isDirectory(file));

    Set<?> posixFilePermissions = null;

    try {
      posixFilePermissions = Files.getPosixFilePermissions(file, new LinkOption[] { LinkOption.NOFOLLOW_LINKS });
    }
    catch (IOException e) {
      throw new ZipException(e);
    }

    permissions.setOwnerCanRead(posixFilePermissions.contains(PosixFilePermission.OWNER_READ));
    permissions.setOwnerCanWrite(posixFilePermissions.contains(PosixFilePermission.OWNER_WRITE));
    permissions.setOwnerCanExecute(posixFilePermissions.contains(PosixFilePermission.OWNER_EXECUTE));

    permissions.setGroupCanRead(posixFilePermissions.contains(PosixFilePermission.GROUP_READ));
    permissions.setGroupCanWrite(posixFilePermissions.contains(PosixFilePermission.GROUP_WRITE));
    permissions.setGroupCanExecute(posixFilePermissions.contains(PosixFilePermission.GROUP_EXECUTE));

    permissions.setOthersCanRead(posixFilePermissions.contains(PosixFilePermission.OTHERS_READ));
    permissions.setOthersCanWrite(posixFilePermissions.contains(PosixFilePermission.OTHERS_WRITE));
    permissions.setOthersCanExecute(posixFilePermissions.contains(PosixFilePermission.OTHERS_EXECUTE));

    return permissions;
  }

  public static void setPermissions(Path file, ZTFilePermissions permissions) {
    if (!isPosix()) {
      // Windows is not posix
      file.toFile().setExecutable(permissions.isOwnerCanExecute(), !permissions.isGroupCanExecute() && !permissions.isOthersCanExecute());
      file.toFile().setWritable(permissions.isOwnerCanWrite(), !permissions.isGroupCanWrite() && !permissions.isOthersCanWrite());
      file.toFile().setReadable(permissions.isOwnerCanRead(), !permissions.isGroupCanRead() && !permissions.isOthersCanRead());
      return;
    }

    Set<PosixFilePermission> set = new HashSet<PosixFilePermission>();
    addIf(permissions.isOwnerCanRead(), set, PosixFilePermission.OWNER_READ);
    addIf(permissions.isOwnerCanRead(), set, PosixFilePermission.OWNER_READ);
    addIf(permissions.isOwnerCanWrite(), set, PosixFilePermission.OWNER_WRITE);
    addIf(permissions.isOwnerCanExecute(), set, PosixFilePermission.OWNER_EXECUTE);

    addIf(permissions.isGroupCanRead(), set, PosixFilePermission.GROUP_READ);
    addIf(permissions.isGroupCanWrite(), set, PosixFilePermission.GROUP_WRITE);
    addIf(permissions.isGroupCanExecute(), set, PosixFilePermission.GROUP_EXECUTE);

    addIf(permissions.isOthersCanRead(), set, PosixFilePermission.OTHERS_READ);
    addIf(permissions.isOthersCanWrite(), set, PosixFilePermission.OTHERS_WRITE);
    addIf(permissions.isOthersCanExecute(), set, PosixFilePermission.OTHERS_EXECUTE);

    try {
      Files.setPosixFilePermissions(file, set);
    }
    catch (IOException e) {
      throw new ZipException(e);
    }
  }

  private static void addIf(boolean condition, Set<PosixFilePermission> set, PosixFilePermission el) {
    if (condition) {
      set.add(el);
    }
  }

  private static boolean isPosix() {
    return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
  }
}
