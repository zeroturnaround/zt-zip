package org.zeroturnaround.zip;
/**
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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;

import org.zeroturnaround.zip.commons.FileUtils;
import org.zeroturnaround.zip.transform.ByteArrayZipEntryTransformer;
import org.zeroturnaround.zip.transform.FileZipEntryTransformer;

import junit.framework.TestCase;

/**
 * Operations that write a temporary copy of an archive, or replace an archive in place, must not make its content
 * readable to other users. These tests use permissions that differ from what a usual umask (022 or 002) gives, so
 * they fail if a file falls back to umask-derived permissions.
 */
public class TempFilePermissionsTest extends TestCase {

  private static final Set<PosixFilePermission> OWNER_ONLY_FILE = PosixFilePermissions.fromString("rw-------");
  private static final Set<PosixFilePermission> OWNER_ONLY_DIR = PosixFilePermissions.fromString("rwx------");

  private File workDir;

  protected void setUp() throws Exception {
    workDir = Files.createTempDirectory("zt-zip-perms").toFile();
  }

  protected void tearDown() throws Exception {
    // Some tests leave read-only directories behind; make them writable so that they can be deleted.
    Files.walkFileTree(workDir.toPath(), new SimpleFileVisitor<Path>() {
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (posixSupported()) {
          Files.setPosixFilePermissions(dir, OWNER_ONLY_DIR);
        }
        return FileVisitResult.CONTINUE;
      }
    });
    FileUtils.deleteQuietly(workDir);
  }

  public void testInPlaceAddEntryKeepsOwnerOnlyArchive() throws Exception {
    if (!posixSupported()) {
      return; // POSIX permissions not supported on this platform; nothing to verify
    }
    File zip = createZip("rw-------");

    ZipUtil.addEntry(zip, "added.txt", "added".getBytes("UTF-8"));

    assertTrue(ZipUtil.containsEntry(zip, "added.txt"));
    assertEquals(OWNER_ONLY_FILE, permissionsOf(zip));
  }

  /*
   * Keeping the original permissions also means not narrowing them: a readable archive stays readable.
   */
  public void testInPlaceAddEntryKeepsReadableArchive() throws Exception {
    if (!posixSupported()) {
      return;
    }
    File zip = createZip("rw-r-----");

    ZipUtil.addEntry(zip, "added.txt", "added".getBytes("UTF-8"));

    assertEquals(PosixFilePermissions.fromString("rw-r-----"), permissionsOf(zip));
  }

  /*
   * Another user with write access to the archive's directory could rename a temporary file there, or put a symbolic
   * link in its place. So in-place operations build the archive elsewhere.
   */
  public void testInPlaceWritesNothingNextToArchive() throws Exception {
    final File zip = createZip("rw-------");
    final List<String> namesDuringOperation = new ArrayList<String>();

    ZipUtil.transformEntry(zip, "secret.txt", new ByteArrayZipEntryTransformer() {
      protected byte[] transform(ZipEntry zipEntry, byte[] input) throws IOException {
        namesDuringOperation.addAll(Arrays.asList(workDir.list()));
        return input;
      }
    });

    assertEquals(Arrays.asList("archive.zip"), namesDuringOperation);
  }

  public void testTempFileIsInOwnerOnlyDirectory() throws Exception {
    File temp = ZTFileUtil.createTempFile();
    File privateDir = temp.getParentFile();
    try {
      if (posixSupported()) {
        assertEquals(OWNER_ONLY_DIR, permissionsOf(privateDir));
      }
    }
    finally {
      ZTFileUtil.deleteTemp(temp);
    }
    assertFalse(privateDir.exists());
  }

  public void testZipsInPlaceKeepsOwnerOnlyArchive() throws Exception {
    if (!posixSupported()) {
      return;
    }
    File zip = createZip("rw-------");

    Zips.get(zip).addEntry(new ByteSource("added.txt", "added".getBytes("UTF-8"))).process();

    assertTrue(ZipUtil.containsEntry(zip, "added.txt"));
    assertEquals(OWNER_ONLY_FILE, permissionsOf(zip));
  }

  /*
   * The directory gets search permission exactly where the archive had read permission. "rwxr-x---" is not what
   * Files.createTempDirectory gives, so this fails if the archive's permissions are not carried over.
   */
  public void testZipsInPlaceUnpackMapsReadAccessToSearchAccess() throws Exception {
    if (!posixSupported()) {
      return;
    }
    File zip = createZip("rw-r-----");

    Zips.get(zip).unpack().process();

    assertTrue(new File(zip, "secret.txt").isFile());
    assertEquals(allowedByUmask("rwxr-x---"), permissionsOf(zip));
  }

  public void testRepackKeepsOwnerOnlyArchive() throws Exception {
    if (!posixSupported()) {
      return;
    }
    File zip = createZip("rw-------");

    ZipUtil.repack(zip, 9);

    assertTrue(ZipUtil.containsEntry(zip, "secret.txt"));
    assertEquals(OWNER_ONLY_FILE, permissionsOf(zip));
  }

  public void testExplodeMapsReadAccessToSearchAccess() throws Exception {
    if (!posixSupported()) {
      return;
    }
    File zip = createZip("rw-r-----");

    ZipUtil.explode(zip);

    assertTrue(new File(zip, "secret.txt").isFile());
    assertEquals(allowedByUmask("rwxr-x---"), permissionsOf(zip));
  }

  /*
   * The extracted directory gives its owner full access even when the archive is read-only: the owner writes its
   * content, and JDK 21 and later refuse to move a directory without owner write permission. Only a JDK 21+ run
   * catches a regression here; on older JDKs a read-only directory still moves.
   */
  public void testExplodeReadOnlyArchive() throws Exception {
    if (!posixSupported()) {
      return;
    }
    File zip = createZip("r--r--r--");

    ZipUtil.explode(zip);

    assertTrue(new File(zip, "secret.txt").isFile());
    assertEquals(allowedByUmask("rwxr-xr-x"), permissionsOf(zip));
    assertEquals(new HashSet<String>(Arrays.asList("archive.zip")), new HashSet<String>(Arrays.asList(workDir.list())));
  }

  /*
   * Moving a directory to another parent directory needs write permission on the directory itself (Linux and macOS,
   * any JDK). If the original cannot be moved aside, unexplode must fail and leave nothing behind.
   */
  public void testUnexplodeOfDirectoryThatCannotBeMovedAside() throws Exception {
    if (!posixSupported() || isRoot()) {
      return; // root can move any directory
    }
    File dir = new File(workDir, "dir");
    assertTrue(dir.mkdir());
    Files.write(new File(dir, "file.txt").toPath(), "content".getBytes("UTF-8"));
    Files.setPosixFilePermissions(dir.toPath(), PosixFilePermissions.fromString("r-x------"));

    try {
      ZipUtil.unexplode(dir);
      fail("Moving a read-only directory aside must fail");
    }
    catch (ZipException expected) {
      // the original must be unchanged
    }

    assertTrue(new File(dir, "file.txt").isFile());
    assertEquals(Arrays.asList("dir"), Arrays.asList(workDir.list()));
  }

  /*
   * Another user with write access to the archive's directory can rename the private directory that holds the
   * extracted content and put a symbolic link to one of the victim's directories in its place. The operation must not
   * change the permissions of, or delete anything in, the directory that the link points to.
   */
  public void testInPlaceUnpackDoesNotFollowSwappedPrivateDirectory() throws Exception {
    if (!posixSupported()) {
      return;
    }
    final File victimDir = new File(workDir, "victim");
    assertTrue(victimDir.mkdir());
    final File victimFile = new File(victimDir, "private.txt");
    Files.write(victimFile.toPath(), "private".getBytes("UTF-8"));
    Files.setPosixFilePermissions(victimDir.toPath(), OWNER_ONLY_DIR);
    final File zip = createZip("rw-r--r--");

    try {
      Zips.get(zip).unpack().addTransformer("secret.txt", new ByteArrayZipEntryTransformer() {
        protected byte[] transform(ZipEntry zipEntry, byte[] input) throws IOException {
          for (File file : workDir.listFiles()) {
            if (file.getName().startsWith("zt-zip_")) {
              Files.move(file.toPath(), new File(workDir, "moved-" + file.getName()).toPath());
              Files.createSymbolicLink(file.toPath(), victimDir.toPath());
            }
          }
          return input;
        }
      }).process();
    }
    catch (ZipException e) {
      // failing is fine, touching the victim's directory is not
    }

    assertEquals(OWNER_ONLY_DIR, permissionsOf(victimDir));
    assertTrue(victimFile.isFile());
  }

  /*
   * The result can keep the original's group only where the process is a member of that group. Otherwise the
   * result is in another group, which must not get the original's group permissions. On macOS a file created in
   * /tmp is in group "wheel", which a normal user is not a member of, so this covers the second case there.
   */
  public void testInPlaceGrantsGroupAccessOnlyToOriginalGroup() throws Exception {
    File sharedTmp = new File("/tmp");
    if (!posixSupported() || !sharedTmp.isDirectory()) {
      return;
    }
    File created = Files.createTempFile(sharedTmp.toPath(), "zt-zip-group", ".zip").toFile();
    created.deleteOnExit();
    ZipUtil.pack(new ZipEntrySource[] { new ByteSource("secret.txt", "secret".getBytes("UTF-8")) }, created);
    File zip = new File(workDir, "archive.zip");
    Files.move(created.toPath(), zip.toPath());
    Files.setPosixFilePermissions(zip.toPath(), PosixFilePermissions.fromString("rw-r-----"));
    GroupPrincipal originalGroup = groupOf(zip);

    ZipUtil.addEntry(zip, "added.txt", "added".getBytes("UTF-8"));

    if (groupOf(zip).equals(originalGroup)) {
      assertEquals(PosixFilePermissions.fromString("rw-r-----"), permissionsOf(zip));
    }
    else {
      assertEquals(OWNER_ONLY_FILE, permissionsOf(zip));
    }
  }

  public void testMoveOverRestoresTargetWhenReplacementCannotBeMoved() throws Exception {
    if (!posixSupported() || isRoot()) {
      return; // root can move out of a read-only directory
    }
    File zip = createZip("rw-------");
    File lockedDir = new File(workDir, "locked");
    File replacement = new File(lockedDir, "replacement");
    assertTrue(replacement.mkdirs());
    Files.setPosixFilePermissions(lockedDir.toPath(), PosixFilePermissions.fromString("r-x------"));

    try {
      ZTFileUtil.moveOver(replacement, zip);
      fail("Moving the replacement out of a read-only directory must fail");
    }
    catch (IOException expected) {
      // the original must be back in place
    }
    finally {
      Files.setPosixFilePermissions(lockedDir.toPath(), OWNER_ONLY_DIR);
    }

    assertTrue(ZipUtil.containsEntry(zip, "secret.txt"));
    assertEquals(new HashSet<String>(Arrays.asList("archive.zip", "locked")),
        new HashSet<String>(Arrays.asList(workDir.list())));
  }

  /*
   * Once the archive is in place, failing to delete the old tree must not fail the operation.
   */
  public void testUnexplodeSucceedsWhenOldTreeCannotBeDeleted() throws Exception {
    if (!posixSupported() || isRoot()) {
      return; // root can delete from a read-only directory
    }
    File dir = new File(workDir, "dir");
    File readOnlySubdir = new File(dir, "sub");
    assertTrue(readOnlySubdir.mkdirs());
    Files.write(new File(readOnlySubdir, "file.txt").toPath(), "content".getBytes("UTF-8"));
    Files.setPosixFilePermissions(readOnlySubdir.toPath(), PosixFilePermissions.fromString("r-x------"));

    ZipUtil.unexplode(dir);

    assertTrue(dir.isFile());
    assertTrue(ZipUtil.containsEntry(dir, "sub/file.txt"));
  }

  public void testUnexplodeKeepsOwnerOnlyAccess() throws Exception {
    if (!posixSupported()) {
      return;
    }
    File dir = new File(workDir, "dir");
    assertTrue(dir.mkdir());
    Files.write(new File(dir, "secret.txt").toPath(), "secret".getBytes("UTF-8"));
    Files.setPosixFilePermissions(dir.toPath(), OWNER_ONLY_DIR);

    ZipUtil.unexplode(dir);

    assertTrue(ZipUtil.containsEntry(dir, "secret.txt"));
    assertEquals(OWNER_ONLY_FILE, permissionsOf(dir));
  }

  public void testUnexplodeKeepsGroupReadAccess() throws Exception {
    if (!posixSupported()) {
      return;
    }
    File dir = new File(workDir, "dir");
    assertTrue(dir.mkdir());
    Files.write(new File(dir, "file.txt").toPath(), "content".getBytes("UTF-8"));
    Files.setPosixFilePermissions(dir.toPath(), PosixFilePermissions.fromString("rwxr-x---"));

    ZipUtil.unexplode(dir);

    assertEquals(PosixFilePermissions.fromString("rw-r-----"), permissionsOf(dir));
  }

  public void testInPlaceAddEntryOnLongArchiveName() throws Exception {
    // 240 characters fit in the usual 255-byte file name limit, but not with a temporary suffix appended.
    StringBuilder name = new StringBuilder();
    while (name.length() < 236) {
      name.append("long");
    }
    File zip = new File(workDir, name + ".zip");
    ZipUtil.pack(new ZipEntrySource[] { new ByteSource("secret.txt", "secret".getBytes("UTF-8")) }, zip);

    ZipUtil.addEntry(zip, "added.txt", "added".getBytes("UTF-8"));

    assertTrue(ZipUtil.containsEntry(zip, "added.txt"));
  }

  /*
   * An archive that belongs to another user, for example a link that someone put in place of the archive, must not
   * pass on more access than any new file gets.
   */
  public void testReplacementOfOtherUsersFileGetsNoMoreThanDefaults() {
    assertEquals(perms("rw-r--r--"), ZTFileUtil.replacementPermissions(
        perms("rw-rw-rw-"), false, perms("rw-r--r--"), false, false));
  }

  public void testReplacementOfOwnFileKeepsPermissions() {
    assertEquals(perms("rw-rw-r--"), ZTFileUtil.replacementPermissions(
        perms("rw-rw-r--"), false, perms("rw-------"), true, true));
  }

  /*
   * Members of another group were either in the original's group or others, so they get what both had.
   */
  public void testReplacementInAnotherGroupGetsOnlyWhatOthersGet() {
    assertEquals(perms("rw-r--r--"), ZTFileUtil.replacementPermissions(
        perms("rw-rw-r--"), false, perms("rw-r--r--"), true, false));
    assertEquals(perms("rw-------"), ZTFileUtil.replacementPermissions(
        perms("rw-r-----"), false, perms("rw-r--r--"), true, false));
  }

  /*
   * Members of the original's group were denied what others got; outside that group they must not get it as others.
   */
  public void testReplacementInAnotherGroupOfGroupDeniedArchive() {
    assertEquals(perms("rw-------"), ZTFileUtil.replacementPermissions(
        perms("rw----r--"), false, perms("rw-r--r--"), true, false));
    assertEquals(OWNER_ONLY_DIR, ZTFileUtil.directoryPermissionsFor(perms("rw----r--"), false));
  }

  public void testReplacementOfDirectoryGetsNoExecutePermissions() {
    assertEquals(perms("rw-r-----"), ZTFileUtil.replacementPermissions(
        perms("rwxr-x---"), true, perms("rw-r--r--"), true, true));
  }

  public void testDirectoryForOwnFileGetsSearchWhereReadable() {
    assertEquals(perms("rwxr-x---"), ZTFileUtil.directoryPermissionsFor(perms("rw-r-----"), true));
    assertEquals(perms("rwxr-xr-x"), ZTFileUtil.directoryPermissionsFor(perms("r--r--r--"), true));
  }

  public void testDirectoryInAnotherGroupGetsOnlyWhatOthersGet() {
    assertEquals(perms("rwxr-xr-x"), ZTFileUtil.directoryPermissionsFor(perms("rw-rw-r--"), false));
  }

  /*
   * A link that the user owns, to a file of the user (for example current.zip -> release-1.2.zip), stands for that
   * file: the result keeps its permissions and group.
   */
  public void testOwnLinkToOwnArchiveKeepsPermissionsAndGroup() throws Exception {
    if (!posixSupported()) {
      return;
    }
    File release = new File(workDir, "release-1.2.zip");
    ZipUtil.pack(new ZipEntrySource[] { new ByteSource("secret.txt", "secret".getBytes("UTF-8")) }, release);
    Files.setPosixFilePermissions(release.toPath(), perms("rw-r-----"));
    GroupPrincipal group = findOtherOwnGroup(release);
    if (group != null) {
      Files.getFileAttributeView(release.toPath(), PosixFileAttributeView.class).setGroup(group);
    }
    GroupPrincipal expectedGroup = groupOf(release);
    File link = new File(workDir, "current.zip");
    Files.createSymbolicLink(link.toPath(), release.toPath());

    ZipUtil.addEntry(link, "added.txt", "added".getBytes("UTF-8"));

    assertFalse(Files.isSymbolicLink(link.toPath()));
    assertEquals(perms("rw-r-----"), permissionsOf(link));
    assertEquals(expectedGroup, groupOf(link));
  }

  /*
   * A link of a third user does not stand for the file it points to; root owns /tmp on macOS, which is a link to
   * /private/tmp.
   */
  public void testOtherUsersLinkIsNotTrusted() throws Exception {
    Path rootLink = new File("/tmp").toPath();
    if (!posixSupported() || !Files.isSymbolicLink(rootLink) || isRoot()) {
      return;
    }
    UserPrincipal user = Files.getOwner(workDir.toPath());
    File ownLink = new File(workDir, "link");
    Files.createSymbolicLink(ownLink.toPath(), workDir.toPath());

    assertFalse(ZTFileUtil.isTrustedPath(rootLink, user, user));
    assertTrue(ZTFileUtil.isTrustedPath(ownLink.toPath(), user, user));
    assertTrue(ZTFileUtil.isTrustedPath(workDir.toPath(), user, user));
  }

  /*
   * An own archive in another of the user's groups: the extracted directory is in the default group, so that group
   * must not get the archive's group permissions.
   */
  public void testExplodeOfArchiveInOtherGroupGivesItsGroupNothing() throws Exception {
    if (!posixSupported()) {
      return;
    }
    File zip = createZip("rw-r-----");
    GroupPrincipal otherGroup = findOtherOwnGroup(zip);
    if (otherGroup == null) {
      return; // the user is in one group only
    }
    Files.getFileAttributeView(zip.toPath(), PosixFileAttributeView.class).setGroup(otherGroup);

    ZipUtil.explode(zip);

    assertTrue(new File(zip, "secret.txt").isFile());
    if (!groupOf(zip).equals(otherGroup)) {
      assertEquals(allowedByUmask("rwx------"), permissionsOf(zip));
    }
  }

  /*
   * Another user's file passes on its group only if it is the group of the archive's directory. A link to a file in
   * one of the user's other groups must not move the result into that group.
   */
  public void testLinkToOtherUsersFileDoesNotPassOnItsGroup() throws Exception {
    if (!posixSupported()) {
      return;
    }
    Path foreign = findOtherUsersFileInOwnGroup();
    if (foreign == null) {
      return; // no such file on this system
    }
    File link = new File(workDir, "archive.zip");
    Files.createSymbolicLink(link.toPath(), foreign);
    File replacement = ZTFileUtil.createTempFile();
    GroupPrincipal defaultGroup = groupOf(replacement);

    try {
      ZTFileUtil.moveOver(replacement, link);
    }
    finally {
      ZTFileUtil.deleteTemp(replacement);
    }

    assertEquals(defaultGroup, groupOf(link));
  }

  /*
   * An archive of the user's own in one of the user's other groups keeps that group and its group permissions.
   */
  public void testInPlaceKeepsOwnArchivesOtherGroup() throws Exception {
    if (!posixSupported()) {
      return;
    }
    File zip = createZip("rw-r-----");
    GroupPrincipal otherGroup = findOtherOwnGroup(zip);
    if (otherGroup == null) {
      return; // the user is in one group only
    }
    Files.getFileAttributeView(zip.toPath(), PosixFileAttributeView.class).setGroup(otherGroup);

    ZipUtil.addEntry(zip, "added.txt", "added".getBytes("UTF-8"));

    assertEquals(otherGroup, groupOf(zip));
    assertEquals(perms("rw-r-----"), permissionsOf(zip));
  }

  /*
   * Replacing an archive on another file system than java.io.tmpdir copies the result next to it first.
   */
  public void testInPlaceOnOtherFileSystem() throws Exception {
    File shm = new File("/dev/shm");
    if (!posixSupported() || !shm.isDirectory() || !shm.canWrite()
        || Files.getFileStore(shm.toPath()).equals(Files.getFileStore(workDir.toPath()))) {
      return; // no second file system to test with
    }
    File dir = Files.createTempDirectory(shm.toPath(), "zt-zip-perms").toFile();
    try {
      File zip = new File(dir, "archive.zip");
      ZipUtil.pack(new ZipEntrySource[] { new ByteSource("secret.txt", "secret".getBytes("UTF-8")) }, zip);
      Files.setPosixFilePermissions(zip.toPath(), perms("rw-r-----"));

      ZipUtil.addEntry(zip, "added.txt", "added".getBytes("UTF-8"));

      assertTrue(ZipUtil.containsEntry(zip, "added.txt"));
      assertEquals(perms("rw-r-----"), permissionsOf(zip));
      assertEquals(Arrays.asList("archive.zip"), Arrays.asList(dir.list()));
    }
    finally {
      FileUtils.deleteQuietly(dir);
    }
  }

  /*
   * As in earlier versions, an in-place operation on a link to an archive replaces the link with the new archive.
   */
  public void testInPlaceOnSymbolicLinkReplacesLink() throws Exception {
    if (!posixSupported()) {
      return;
    }
    File original = createZip("rw-------");
    File link = new File(workDir, "link.zip");
    Files.createSymbolicLink(link.toPath(), original.toPath());

    ZipUtil.addEntry(link, "added.txt", "added".getBytes("UTF-8"));

    assertFalse(Files.isSymbolicLink(link.toPath()));
    assertTrue(ZipUtil.containsEntry(link, "added.txt"));
    assertEquals(OWNER_ONLY_FILE, permissionsOf(link));
    assertFalse(ZipUtil.containsEntry(original, "added.txt"));
  }

  public void testExplodeOfMissingArchiveLeavesNothingBehind() throws Exception {
    try {
      ZipUtil.explode(new File(workDir, "missing.zip"));
      fail("Exploding a missing archive must fail");
    }
    catch (ZipException expected) {
      // nothing must be left behind
    }
    assertEquals(0, workDir.list().length);
  }

  public void testFileZipEntryTransformerTempFilesAreOwnerOnly() throws Exception {
    if (!posixSupported()) {
      return;
    }
    File zip = createZip("rw-------");
    File destZip = new File(workDir, "dest.zip");
    final List<Set<PosixFilePermission>> tempFilePermissions = new ArrayList<Set<PosixFilePermission>>();

    ZipUtil.transformEntry(zip, "secret.txt", new FileZipEntryTransformer() {
      protected void transform(ZipEntry zipEntry, File in, File out) throws IOException {
        FileUtils.copyFile(in, out);
        tempFilePermissions.add(permissionsOf(in));
        tempFilePermissions.add(permissionsOf(out));
      }
    }, destZip);

    assertEquals(2, tempFilePermissions.size());
    assertEquals(OWNER_ONLY_FILE, tempFilePermissions.get(0));
    assertEquals(OWNER_ONLY_FILE, tempFilePermissions.get(1));
  }

  private File createZip(String permissions) throws IOException {
    File zip = new File(workDir, "archive.zip");
    ZipUtil.pack(new ZipEntrySource[] { new ByteSource("secret.txt", "secret".getBytes("UTF-8")) }, zip);
    if (posixSupported()) {
      Files.setPosixFilePermissions(zip.toPath(), PosixFilePermissions.fromString(permissions));
    }
    return zip;
  }

  private static Set<PosixFilePermission> permissionsOf(File file) throws IOException {
    Path path = file.toPath();
    return Files.getPosixFilePermissions(path);
  }

  /*
   * A new directory gets its permissions when it is created, so the umask can remove some of them.
   */
  private Set<PosixFilePermission> allowedByUmask(String permissions) throws IOException {
    Path probe = Files.createDirectory(new File(workDir, "umask-probe").toPath(),
        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx")));
    Set<PosixFilePermission> allowed = Files.getPosixFilePermissions(probe);
    Files.delete(probe);
    Set<PosixFilePermission> result = PosixFilePermissions.fromString(permissions);
    result.retainAll(allowed);
    return result;
  }

  private static Set<PosixFilePermission> perms(String permissions) {
    return PosixFilePermissions.fromString(permissions);
  }

  private static final String[] GROUP_NAMES = { "admin", "everyone", "staff", "wheel", "users", "adm", "sudo", "docker",
      "systemd-journal", "plugdev", "lpadmin", "_developer" };

  /** A group the user belongs to, other than the given file's group, or null. */
  private GroupPrincipal findOtherOwnGroup(File file) throws IOException {
    GroupPrincipal current = groupOf(file);
    for (String name : GROUP_NAMES) {
      GroupPrincipal group;
      try {
        group = file.toPath().getFileSystem().getUserPrincipalLookupService().lookupPrincipalByGroupName(name);
      }
      catch (IOException e) {
        continue; // no such group
      }
      if (!group.equals(current) && isMember(group)) {
        return group;
      }
    }
    return null;
  }

  /**
   * A file of another user (root) in a group the user belongs to, other than the group of new files in the work
   * directory, or null.
   */
  private Path findOtherUsersFileInOwnGroup() throws IOException {
    String[] candidates = { "/Library/Receipts/InstallHistory.plist", "/var/log/syslog", "/var/log/auth.log",
        "/var/log/messages", "/etc/shadow" };
    List<String> paths = new ArrayList<String>(Arrays.asList(candidates));
    File[] journals = new File("/var/log/journal").listFiles();
    if (journals != null) {
      for (File journal : journals) {
        paths.add(new File(journal, "system.journal").getPath());
      }
    }
    GroupPrincipal workDirGroup = groupOf(workDir);
    for (String candidate : paths) {
      Path path = new File(candidate).toPath();
      if (Files.isRegularFile(path) && belongsToAnotherUser(path)) {
        GroupPrincipal group = Files.readAttributes(path, PosixFileAttributes.class).group();
        if (!group.equals(workDirGroup) && isMember(group)) {
          return path;
        }
      }
    }
    return null;
  }

  private boolean isMember(GroupPrincipal group) throws IOException {
    Path probe = Files.createTempFile(workDir.toPath(), "group-probe", null);
    try {
      Files.getFileAttributeView(probe, PosixFileAttributeView.class).setGroup(group);
      return true;
    }
    catch (IOException e) {
      return false;
    }
    finally {
      Files.delete(probe);
    }
  }

  private static boolean belongsToAnotherUser(Path path) throws IOException {
    Path probe = Files.createTempFile("zt-zip-owner", null);
    try {
      return !Files.getOwner(path).equals(Files.getOwner(probe));
    }
    finally {
      Files.delete(probe);
    }
  }

  private static GroupPrincipal groupOf(File file) throws IOException {
    return Files.readAttributes(file.toPath(), PosixFileAttributes.class).group();
  }

  private static boolean isRoot() {
    return "root".equals(System.getProperty("user.name"));
  }

  private static boolean posixSupported() {
    return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
  }
}
