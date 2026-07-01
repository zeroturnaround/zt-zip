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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.zeroturnaround.zip.transform.ZipEntryTransformer;

import junit.framework.TestCase;

public class DirectoryTraversalMaliciousTest extends TestCase {
  /*
   * This is the contents of the file. There is one evil file that tries to get out of the
   * target.
   *
   * $ unzip -t zip-malicious-traversal.zip
   * Archive: zip-malicious-traversal.zip
   * testing: good.txt OK
   * testing: ../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../tmp/evil.txt OK
   * No errors detected in compressed data of zip-malicious-traversal.zip.
   */
  private static final File badFile = new File("src/test/resources/zip-malicious-traversal.zip");

  /*
   * This is the contents of the file. There is one evil file that tries to get out of the
   * target.
   *
   * $ unzip -t zip-malicious-traversal-root.zip
   * Archive: zip-malicious-traversal-root.zip
   * testing: someroot/good.txt OK
   * testing: someroot/../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../home/evil.txt OK
   * No errors detected in compressed data of zip-malicious-traversal-root.zip.
   */
  private static final File badFileWithRoot = new File("src/test/resources/zip-malicious-traversal-root.zip");

  /*
   * This is the contents of the file. There is one evil file that tries to get out of the
   * target.
   *
   * $ unzip -t zip-malicious-traversal-backslashes.zip
   * Archive: zip-malicious-traversal-backslashes.zip
   * testing: someroot/good.txt OK
   * testing: ..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\..\home\evil.txt OK
   * No errors detected in compressed data of zip-malicious-traversal-backslashes.zip.
   */
  private static final File badFileBackslashes = new File("src/test/resources/zip-malicious-traversal-backslashes.zip");

  public void testUnpackDoesntLeaveTarget() throws Exception {
    File file = File.createTempFile("temp", null);
    File tmpDir = file.getParentFile();

    try {
      ZipUtil.unpack(badFile, tmpDir);
      fail();
    }
    catch (MaliciousZipException e) {
      assertTrue(true);
    }
  }

  public void testUnwrapDoesntLeaveTarget() throws Exception {
    File file = File.createTempFile("temp", null);
    File tmpDir = file.getParentFile();

    try {
      ZipUtil.unwrap(badFileWithRoot, tmpDir);
      fail();
    }
    catch (MaliciousZipException e) {
      assertTrue(true);
    }
  }

  public void testBackslashUnpackerDoesntLeaveTarget() throws Exception {
    File file = File.createTempFile("temp", null);
    File tmpDir = file.getParentFile();

    try {
      ZipUtil.iterate(badFileBackslashes, new ZipUtil.BackslashUnpacker(tmpDir));
      fail();
    }
    catch (MaliciousZipException e) {
      assertTrue(true);
    }
  }

  /*
   * The BackslashUnpacker splits an entry on "\" and walks the components. The traversal guard must
   * run before any directory is created, otherwise a "..\" entry creates directories outside the
   * target even though the file write is ultimately blocked.
   */
  public void testBackslashUnpackerDoesntCreateDirectoriesOutsideTarget() throws Exception {
    File parent = Files.createTempDirectory("zt-zip-bs-traversal").toFile();
    File target = new File(parent, "target");
    target.mkdir();
    File escaped = new File(parent, "escapedir");
    File zip = createTraversalZip("..\\escapedir\\evil.txt");

    try {
      ZipUtil.iterate(zip, new ZipUtil.BackslashUnpacker(target));
      fail();
    }
    catch (MaliciousZipException e) {
      assertFalse(escaped.exists());
    }
  }

  /*
   * An entry can leave the target without escaping its parent: a name like
   * "../targetX" resolves to a sibling directory whose path shares a prefix with
   * the target. A plain string prefix check treats that as inside the target; the
   * traversal guard must compare path components instead.
   */
  public void testUnpackDoesntLeaveTargetForSiblingWithSharedPrefix() throws Exception {
    File parent = Files.createTempDirectory("zt-zip-traversal").toFile();
    File target = new File(parent, "target");
    target.mkdir();

    File zip = File.createTempFile("sibling-prefix-traversal", ".zip");
    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
    try {
      out.putNextEntry(new ZipEntry("good.txt"));
      out.closeEntry();
      out.putNextEntry(new ZipEntry("../targetX/evil.txt"));
      out.closeEntry();
    }
    finally {
      out.close();
    }

    try {
      ZipUtil.unpack(zip, target);
      fail();
    }
    catch (MaliciousZipException e) {
      assertFalse(new File(parent, "targetX/evil.txt").exists());
    }
  }

  /*
   * The Zips fluent API has its own unpack implementation (Zips.UnpackingCallback)
   * and must apply the same traversal guard as ZipUtil.unpack(...).
   */
  public void testZipsUnpackDoesntLeaveTarget() throws Exception {
    File parent = Files.createTempDirectory("zt-zip-zips-traversal").toFile();
    File target = new File(parent, "target");
    target.mkdir();
    File escaped = new File(parent, "escape/evil.txt");
    File zip = createTraversalZip("../escape/evil.txt");

    try {
      Zips.get(zip).unpack().destination(target).process();
      fail();
    }
    catch (MaliciousZipException e) {
      assertFalse(escaped.exists());
    }
  }

  public void testZipsUnpackWithTransformerDoesntLeaveTarget() throws Exception {
    File parent = Files.createTempDirectory("zt-zip-zips-transform-traversal").toFile();
    File target = new File(parent, "target");
    target.mkdir();
    File escaped = new File(parent, "escape/evil.txt");
    final String entryName = "../escape/evil.txt";
    File zip = createTraversalZip(entryName);

    try {
      Zips.get(zip)
          .unpack()
          .destination(target)
          .addTransformer(entryName, new ZipEntryTransformer() {
            public void transform(InputStream in, ZipEntry entry, ZipOutputStream out) throws IOException {
              out.putNextEntry(new ZipEntry(entry.getName()));
              out.closeEntry();
            }
          })
          .process();
      fail();
    }
    catch (MaliciousZipException e) {
      assertFalse(escaped.exists());
    }
  }

  /*
   * A directory entry (trailing slash) escapes through forceMkdir rather than a
   * file write, so the guard must run before the isDirectory() branch too.
   */
  public void testZipsUnpackDirectoryEntryDoesntLeaveTarget() throws Exception {
    File parent = Files.createTempDirectory("zt-zip-zips-dir-traversal").toFile();
    File target = new File(parent, "target");
    target.mkdir();
    File escaped = new File(parent, "escape");
    File zip = createTraversalZip("../escape/");

    try {
      Zips.get(zip).unpack().destination(target).process();
      fail();
    }
    catch (MaliciousZipException e) {
      assertFalse(escaped.exists());
    }
  }

  /*
   * A ZIP entry whose name resolves to the output directory itself (e.g. "/") passes the traversal
   * guard because it does not leave the target. Historically the Unpacker then applied the entry's
   * file permissions to the output directory, which could widen them (e.g. make an owner-only
   * directory world-accessible). See the advisory GHSA-v2g6-7r9j-v6px. Unpacking such an entry must
   * complete without changing the output directory's permissions.
   */
  public void testUnpackDoesntChangeOutputDirPermissions() throws Exception {
    assertUnpackDoesntChangeOutputDirPermissions("/");
  }

  /*
   * "./" also resolves to the output directory itself, without containing "..", so it is not caught
   * by looking for ".." alone.
   */
  public void testUnpackDoesntChangeOutputDirPermissionsForDotSlashEntry() throws Exception {
    assertUnpackDoesntChangeOutputDirPermissions("./");
  }

  private void assertUnpackDoesntChangeOutputDirPermissions(String selfReferencingEntryName) throws Exception {
    if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
      return; // POSIX permissions not supported on this platform; nothing to verify
    }
    File outputDir = Files.createTempDirectory("zt-zip-selfref-perms").toFile();
    Path outputDirPath = outputDir.toPath();
    Set<PosixFilePermission> ownerOnly = EnumSet.of(
        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);
    Files.setPosixFilePermissions(outputDirPath, ownerOnly);
    File zip = createZipWithPosixMode(selfReferencingEntryName, 0777);

    ZipUtil.unpack(zip, outputDir);

    assertEquals("Unpacking '" + selfReferencingEntryName + "' must not change the output directory permissions",
        ownerOnly, Files.getPosixFilePermissions(outputDirPath));
  }

  /*
   * Fixing the self-referencing case must not stop permissions from being applied to genuine entries
   * inside the output directory.
   */
  public void testUnpackStillAppliesPermissionsToRegularEntry() throws Exception {
    if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
      return; // POSIX permissions not supported on this platform; nothing to verify
    }
    File outputDir = Files.createTempDirectory("zt-zip-perms-applied").toFile();
    File zip = createZipWithPosixMode("file.txt", 0750);

    ZipUtil.unpack(zip, outputDir);

    Set<PosixFilePermission> expected = EnumSet.of(
        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
        PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE);
    assertEquals(expected, Files.getPosixFilePermissions(new File(outputDir, "file.txt").toPath()));
  }

  /*
   * Directly exercises the output-directory detection used to guard permission application: names
   * that resolve to the output directory itself must be recognised (whether or not they contain
   * ".."), while strict descendants must not.
   */
  public void testDestinationIsOutputDirDetection() throws Exception {
    File outputDir = Files.createTempDirectory("zt-zip-isoutdir").toFile();
    String[] selfReferencing = { "/", "//", "", ".", "./", "/.", "/./", "a/..", "a/../" };
    for (String name : selfReferencing) {
      assertTrue("'" + name + "' resolves to the output directory",
          ZipUtil.destinationIsOutputDir(outputDir, name, new File(outputDir, name)));
    }
    String[] descendants = { "good.txt", "sub/file", "dir/file.txt", "a/b/c", "a/../b", "x/./y", "foo..bar" };
    for (String name : descendants) {
      assertFalse("'" + name + "' is a strict descendant",
          ZipUtil.destinationIsOutputDir(outputDir, name, new File(outputDir, name)));
    }
  }

  /*
   * The guard rejects names that escape the output directory, whether via a leading ".." or a ".."
   * that only escapes after a real segment (e.g. "sub/../../evil").
   */
  public void testGuardRejectsEscapingNames() throws Exception {
    File outputDir = Files.createTempDirectory("zt-zip-guard-escape").toFile();
    String[] escaping = { "..", "../evil", "../../evil", "a/../../evil", "sub/../../evil" };
    for (String name : escaping) {
      try {
        ZipUtil.checkDestinationFileForTraversal(outputDir, name, new File(outputDir, name));
        fail("Expected MaliciousZipException for entry name '" + name + "'");
      }
      catch (MaliciousZipException expected) {
        // expected: the name escapes the output directory
      }
    }
  }

  /*
   * The guard lets through names that resolve to a strict descendant of the output directory
   * (including ones with "." or a cancelled ".." segment) as well as names that resolve to the
   * output directory itself, which are harmless no-ops for extraction.
   */
  public void testGuardAllowsDescendantAndSelfReferenceNames() throws Exception {
    File outputDir = Files.createTempDirectory("zt-zip-guard-good").toFile();
    String[] allowedNames = { "good.txt", "sub/file", "dir/file.txt", "a/b/c", "a/../b", "x/./y", "foo..bar",
        "/", "./", ".", "a/.." };
    for (String name : allowedNames) {
      File destFile = new File(outputDir, name);
      assertSame("Entry name '" + name + "' should be allowed",
          destFile, ZipUtil.checkDestinationFileForTraversal(outputDir, name, destFile));
    }
  }

  private static File createZipWithPosixMode(String entryName, int posixMode) throws IOException {
    File zip = File.createTempFile("zips-selfref", ".zip");
    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
    try {
      ZipEntry entry = new ZipEntry(entryName);
      ZipEntryUtil.setZTFilePermissions(entry, ZTFilePermissionsUtil.fromPosixFileMode(posixMode));
      out.putNextEntry(entry);
      out.closeEntry();
    }
    finally {
      out.close();
    }
    return zip;
  }

  private static File createTraversalZip(String entryName) throws IOException {
    File zip = File.createTempFile("zips-traversal", ".zip");
    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
    try {
      out.putNextEntry(new ZipEntry(entryName));
      out.closeEntry();
    }
    finally {
      out.close();
    }
    return zip;
  }
}
