/**
 *    Copyright (C) 2012 ZeroTurnaround LLC <support@zeroturnaround.com>
 *    Copyright (C) 2026 Neeme Praks
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
package org.zeroturnaround.zip;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ZTFileUtil {
  private static final Logger log = LoggerFactory.getLogger("org/zeroturnaround/zip/ZTFileUtil".replace('/', '.')); // NOSONAR

  private ZTFileUtil() {
  }

  /** A short fixed prefix, so the temporary name stays within file name limits whatever the target's name. */
  private static final String TEMP_PREFIX = "zt-zip_";
  private static final String REPLACEMENT_NAME = "replacement";

  private static final Set<PosixFilePermission> EXECUTE_PERMISSIONS = EnumSet.of(
      PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_EXECUTE);

  public static Collection<File> listFiles(File dir) {
    return listFiles(dir, null);
  }
  
  public static Collection<File> listFiles(File dir, FileFilter filter) {
    Collection<File> accumulator = new ArrayList<File>();

    if (dir.isFile()) {
      return accumulator;
    }
    
    if (filter == null) {
      // Set default filter to accept any file
      filter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
          return true;
        }
      };
    }

    innerListFiles(dir, accumulator, filter);
    return accumulator;
  }

  private static void innerListFiles(File dir, Collection<File> accumulator, FileFilter filter) {

    String[] filenames = dir.list();

    if (filenames != null) {
      for (String filename : filenames) {
        File file = new File(dir, filename);
        if (file.isDirectory()) {
          innerListFiles(file, accumulator, filter);
        }
        else {
          if (filter != null && filter.accept(file)) {
            accumulator.add(file);
          }
        }
      }
    }
  }

  /**
   * Creates an empty file to build a replacement for an archive in. The file is in a fresh owner-only directory under
   * {@code java.io.tmpdir}, so no other user can read the content or redirect the path while the content is written
   * and while the file gets the archive's permissions in {@link #moveOver(File, File)}. Delete it with
   * {@link #deleteTemp(File)}.
   */
  static File createTempFile() throws IOException {
    Path privateDir = Files.createTempDirectory(TEMP_PREFIX);
    return Files.createFile(privateDir.resolve(REPLACEMENT_NAME)).toFile();
  }

  /**
   * Creates an empty directory to build a replacement for the given file in, next to it, so that
   * {@link #moveOver(File, File)} only renames it. The directory gets its final POSIX permissions when it is created,
   * from {@link #directoryPermissionsFor}. Nothing changes its permissions later through a path, which another user
   * with write access to the target's directory could redirect. The umask can make the permissions stricter. The
   * directory is in a fresh owner-only directory, so its content is not reachable while it is written. Delete it with
   * {@link #deleteTemp(File)}.
   */
  static File createTempDirFor(File target) throws IOException {
    PosixFileAttributes targetAttributes = posixAttributes(target.toPath());
    Path privateDir = Files.createTempDirectory(parentOf(target), TEMP_PREFIX);
    try {
      Path dir = privateDir.resolve(REPLACEMENT_NAME);
      if (targetAttributes == null) {
        return Files.createDirectory(dir).toFile();
      }
      // The directory inside gets the same owner and group as the private directory.
      PosixFileAttributes created = posixAttributes(privateDir);
      boolean sameGroup = targetAttributes.group().equals(created.group());
      Set<PosixFilePermission> permissions = directoryPermissionsFor(targetAttributes.permissions(), sameGroup);
      Files.createDirectory(dir, PosixFilePermissions.asFileAttribute(permissions));
      if (!targetAttributes.owner().equals(created.owner()) && isTrustedPath(target.toPath(), targetAttributes.owner(), created.owner())) {
        // Only root can give the still empty directory to the archive's owner; NOFOLLOW_LINKS changes no link target.
        restoreOwner(Files.getFileAttributeView(dir, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS),
            targetAttributes.owner());
      }
      return dir.toFile();
    }
    catch (IOException e) {
      deleteTree(privateDir);
      throw e;
    }
  }

  /**
   * Deletes a file or directory created by {@link #createTempFile()} or {@link #createTempDirFor(File)}, together with
   * the private directory around it. Does not follow symbolic links inside the tree. Another user with write access
   * to the target's directory can still swap the private directory itself for a link while it is being deleted.
   */
  static void deleteTemp(File temp) {
    if (temp == null) {
      return;
    }
    File privateDir = temp.getAbsoluteFile().getParentFile();
    if (privateDir.getName().startsWith(TEMP_PREFIX)) {
      try {
        deleteTree(privateDir.toPath());
      }
      catch (IOException e) {
        log.debug("Could not delete {}", privateDir, e);
      }
    }
  }

  private static Path parentOf(File file) {
    return file.getAbsoluteFile().getParentFile().toPath();
  }

  /**
   * Moves {@code replacement} over {@code target}, replacing it. A replacement file from {@link #createTempFile()}
   * first gets the POSIX group and permissions of the target, in its private directory. A replacement directory from
   * {@link #createTempDirFor(File)} already has them. An in-place operation thus does not make the result more
   * accessible than the original. No permission is changed through a path in the target's directory, where another
   * user with write access could have put a symbolic link.
   * <p>
   * A file that replaces a file does so in one step. If the replacement is on another file system, the JDK first
   * copies it to a new file next to the target, which it creates exclusively (so not through a link) and gives the
   * permissions through the open file. Until the copy is complete, that file is in the group that new files get.
   * Otherwise the target is moved aside first and deleted only after the replacement is in place, so a failure never
   * loses both.
   * <p>
   * On file systems without POSIX permissions (Windows) a file result has the access rules of the user's temporary
   * directory, as in earlier versions, and a directory result those of a new directory next to the target.
   */
  static void moveOver(File replacement, File target) throws IOException {
    Path source = replacement.toPath();
    Path dest = target.toPath();
    boolean sourceIsDir = Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS);
    if (!sourceIsDir) {
      copyPosixAccess(dest, source);
    }
    if (sourceIsDir || Files.isDirectory(dest, LinkOption.NOFOLLOW_LINKS)) {
      swap(source, dest, target);
      return;
    }
    try {
      Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
    catch (AtomicMoveNotSupportedException e) {
      // Another file system: copy next to the target (the copy refuses to follow a symbolic link and sets the
      // permissions on the open file), then rename the copy over the target.
      Path staged = Files.createTempFile(parentOf(target), TEMP_PREFIX, ".zip");
      try {
        Files.copy(source, staged, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        replaceInOneStep(staged, dest);
      }
      catch (IOException copyFailure) {
        Files.deleteIfExists(staged);
        throw copyFailure;
      }
    }
  }

  private static void replaceInOneStep(Path source, Path dest) throws IOException {
    try {
      Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
    catch (AtomicMoveNotSupportedException e) {
      Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /** Replaces a directory with a file, or a file or directory with a directory. */
  private static void swap(Path source, Path dest, File target) throws IOException {
    Path backupDir = Files.createTempDirectory(parentOf(target), TEMP_PREFIX);
    Path backup = backupDir.resolve("original");
    try {
      Files.move(dest, backup);
    }
    catch (IOException e) {
      Files.deleteIfExists(backupDir);
      throw e;
    }
    try {
      Files.move(source, dest);
    }
    catch (IOException e) {
      try {
        Files.move(backup, dest);
        Files.deleteIfExists(backupDir);
      }
      catch (IOException restoreFailure) {
        e.addSuppressed(restoreFailure); // the original stays in backupDir
      }
      throw e;
    }
    try {
      deleteTree(backupDir);
    }
    catch (IOException e) {
      log.warn("Replaced {} but could not delete its previous version in {}", target, backupDir, e);
    }
  }

  /**
   * Deletes a file or directory tree without following symbolic links. A file that disappears during the walk is not
   * an error: on FAT and exFAT volumes macOS deletes the "._" metadata file together with the file it belongs to.
   */
  private static void deleteTree(Path root) throws IOException {
    Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.deleteIfExists(file);
        return FileVisitResult.CONTINUE;
      }

      public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
        if (e instanceof NoSuchFileException) {
          return FileVisitResult.CONTINUE;
        }
        throw e;
      }

      public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
        if (e != null) {
          throw e;
        }
        Files.deleteIfExists(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  /**
   * Gives the replacement file {@code to} the POSIX group and permissions of the target {@code from}, as far as
   * {@link #replacementPermissions} allows. The target is the user's own file if the user owns it, or if the process
   * can give the replacement the target's owner (only root can), and the archive path is not a symbolic link of a
   * third user (see {@link #isTrustedPath}). The group of another user's file is taken only if it is the group of the
   * target's directory, which any new file there can get too. Does nothing on file systems without POSIX permissions.
   */
  private static void copyPosixAccess(Path from, Path to) throws IOException {
    PosixFileAttributes original = posixAttributes(from);
    PosixFileAttributeView toView = Files.getFileAttributeView(to, PosixFileAttributeView.class);
    if (original == null || toView == null) {
      return;
    }
    PosixFileAttributes replacement = toView.readAttributes();
    boolean ownFile = isTrustedPath(from, original.owner(), replacement.owner())
        && (original.owner().equals(replacement.owner()) || restoreOwner(toView, original.owner()));
    boolean groupAllowed = ownFile || original.group().equals(posixAttributes(parentOf(from.toFile())).group());
    if (groupAllowed && !original.group().equals(replacement.group())) {
      try {
        toView.setGroup(original.group());
        replacement = toView.readAttributes();
      }
      catch (IOException e) {
        // Only a member of the original group can set it.
      }
    }
    toView.setPermissions(replacementPermissions(original.permissions(), original.isDirectory(),
        replacement.permissions(), ownFile, original.group().equals(replacement.group())));
  }

  /**
   * The permissions for a replacement file: those of the original, without execute permissions if the original is a
   * directory. If the replacement is not in the original's group, its group and others get only what both the
   * original's group and others had, because members of its group were one or the other. If the original is another
   * user's file (see {@link #copyPosixAccess}), the result is also limited to {@code defaults}, the permissions that
   * any new file gets. The result is thus never more accessible than the original.
   */
  static Set<PosixFilePermission> replacementPermissions(Set<PosixFilePermission> original, boolean originalIsDirectory,
      Set<PosixFilePermission> defaults, boolean ownFile, boolean sameGroup) {
    Set<PosixFilePermission> permissions = new HashSet<PosixFilePermission>(original);
    if (originalIsDirectory) {
      permissions.removeAll(EXECUTE_PERMISSIONS);
    }
    if (!sameGroup) {
      limitGroupToOthers(permissions);
    }
    if (!ownFile) {
      permissions.retainAll(defaults);
    }
    return permissions;
  }

  /**
   * The permissions for a directory that replaces a file: those of the file, with search permission wherever the file
   * is readable and full access for the owner, who writes the content. The group rule is the same as in
   * {@link #replacementPermissions}. The directory gets these when it is created, so the umask limits them, as it
   * limits any new directory.
   */
  static Set<PosixFilePermission> directoryPermissionsFor(Set<PosixFilePermission> filePermissions, boolean sameGroup) {
    Set<PosixFilePermission> permissions = new HashSet<PosixFilePermission>(filePermissions);
    permissions.addAll(EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
        PosixFilePermission.OWNER_EXECUTE));
    if (!sameGroup) {
      limitGroupToOthers(permissions);
    }
    if (permissions.contains(PosixFilePermission.GROUP_READ)) {
      permissions.add(PosixFilePermission.GROUP_EXECUTE);
    }
    if (permissions.contains(PosixFilePermission.OTHERS_READ)) {
      permissions.add(PosixFilePermission.OTHERS_EXECUTE);
    }
    return permissions;
  }

  /**
   * Whether the archive path can stand for the file it names: it is not a symbolic link, or the link belongs to the
   * process user or to the owner of the file it points to. A third user cannot create a link that belongs to either.
   */
  static boolean isTrustedPath(Path path, UserPrincipal fileOwner, UserPrincipal processUser) throws IOException {
    if (!Files.isSymbolicLink(path)) {
      return true;
    }
    UserPrincipal linkOwner = Files.getOwner(path, LinkOption.NOFOLLOW_LINKS);
    return linkOwner.equals(fileOwner) || linkOwner.equals(processUser);
  }

  /** Gives the replacement the original's owner, which only root can. Returns whether that worked. */
  private static boolean restoreOwner(PosixFileAttributeView view, UserPrincipal owner) {
    try {
      view.setOwner(owner);
      return true;
    }
    catch (IOException e) {
      return false;
    }
  }

  /**
   * For a result outside the original's group: its members were in the original's group or others, so group and
   * others both get only what both had.
   */
  private static void limitGroupToOthers(Set<PosixFilePermission> permissions) {
    limitToBoth(permissions, PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ);
    limitToBoth(permissions, PosixFilePermission.GROUP_WRITE, PosixFilePermission.OTHERS_WRITE);
    limitToBoth(permissions, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_EXECUTE);
  }

  private static void limitToBoth(Set<PosixFilePermission> permissions, PosixFilePermission group,
      PosixFilePermission others) {
    if (!permissions.contains(group) || !permissions.contains(others)) {
      permissions.remove(group);
      permissions.remove(others);
    }
  }

  /** The POSIX attributes of the given path, or {@code null} on file systems without POSIX permissions. */
  private static PosixFileAttributes posixAttributes(Path path) throws IOException {
    PosixFileAttributeView view = Files.getFileAttributeView(path, PosixFileAttributeView.class);
    return view == null ? null : view.readAttributes();
  }
}
