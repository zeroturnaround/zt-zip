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
import java.nio.file.Files;
import java.util.zip.ZipEntry;

import junit.framework.TestCase;

import org.zeroturnaround.zip.commons.FileUtils;
import org.zeroturnaround.zip.commons.IOUtils;
import org.zeroturnaround.zip.transform.StringZipEntryTransformer;

public final class MainExamplesTest extends TestCase {

  /* Unpacking */

  public static final String DEMO_ZIP = "src/test/resources/demo.zip";
  public static final String DUPLICATE_ZIP = "src/test/resources/duplicate.zip";
  public static final String DEMO_COPY_ZIP = "src/test/resources/demo-copy.zip";
  public static final String FOO_TXT = "foo.txt";

  public static void testContains() {
    boolean exists = ZipUtil.containsEntry(new File(DEMO_ZIP), FOO_TXT);
    assertTrue(exists);
  }

  public static void testUnpackEntryImMemory() {
    byte[] bytes = ZipUtil.unpackEntry(new File(DEMO_ZIP), FOO_TXT);
    assertEquals(bytes.length, 12);
  }

  public static void testUnpackEntry() throws IOException {
    File tmpFile = File.createTempFile("prefix", "suffix");
    ZipUtil.unpackEntry(new File(DEMO_ZIP), FOO_TXT, tmpFile);
    assertTrue(tmpFile.length() > 0);
  }

  public static void testUnpack() throws IOException {
    File tmpDir = Files.createTempDirectory("prefix" + "suffix").toFile();
    ZipUtil.unpack(new File(DEMO_ZIP), tmpDir);
    File fooFile = new File(tmpDir, FOO_TXT);
    assertTrue(fooFile.exists());
  }

  public static void testUnpackInPlace() throws Exception{
    File demoFile = new File(DEMO_ZIP);
    File outDir = Files.createTempDirectory("prefix" + "suffix").toFile();

    File outFile = new File(outDir, "demo");
    FileOutputStream fio = new FileOutputStream(outFile);

    // so the zip file will be outDir/demo <- this is a zip archive
    FileUtils.copy(demoFile, fio);

    // close the stream so that locks on the file can be released (on windows, not doing this prevents the file from being moved)
    fio.close();

    // we explode the zip archive
    ZipUtil.explode(outFile);

    // we expect the outDir/demo/foo.txt to exist now
    assertTrue((new File(outFile, FOO_TXT)).exists());
  }

  public static void unpackDocOnly() {
    ZipUtil.unpack(new File("/tmp/demo.zip"), new File("/tmp/demo"), new NameMapper() {
      public String map(String name) {
        return name.startsWith("doc/") ? name : null;
      }
    });
  }

  public static void unpackWithoutPrefix() {
    final String prefix = "doc/";
    ZipUtil.unpack(new File("/tmp/demo.zip"), new File("/tmp/demo"), new NameMapper() {
      public String map(String name) {
        return name.startsWith(prefix) ? name.substring(prefix.length()) : name;
      }
    });
  }

  public static void listClasses() {
    ZipUtil.iterate(new File("/tmp/demo.zip"), new ZipInfoCallback() {
      public void process(ZipEntry zipEntry) throws IOException {
        if (zipEntry.getName().endsWith(".class")) {
          System.out.println("Found " + zipEntry.getName());
        }
      }
    });
  }

  public static void printTexts() {
    ZipUtil.iterate(new File("/tmp/demo.zip"), new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        if (zipEntry.getName().endsWith(".txt")) {
          System.out.println("Found " + zipEntry.getName());
          IOUtils.copy(in, System.out);
        }
      }
    });
  }
  
  /* Comparison */

  public static void testEntryEquals() {
    boolean equals = ZipUtil.entryEquals(new File(DEMO_ZIP), new File(DEMO_COPY_ZIP), FOO_TXT);
    assertTrue(equals);
  }

  public static void testEntryEqualsDifferentNames() {
    boolean equals = ZipUtil
        .entryEquals(new File(DEMO_ZIP), new File(DEMO_COPY_ZIP), "foo1.txt", "foo2.txt");
    assertTrue(equals);
  }

  public void testArchiveEquals() {
    boolean result = ZipUtil.archiveEquals(new File(DEMO_ZIP), new File(DEMO_COPY_ZIP));
    assertTrue(result);
  }
  
  public void testArchiveEqualsNo() {
    boolean result = ZipUtil.archiveEquals(new File(DEMO_ZIP), new File(DUPLICATE_ZIP));
    assertFalse(result);
  }

  /* Packing */

  public static void pack() {
    ZipUtil.pack(new File("/tmp/demo"), new File("/tmp/demo.zip"));
  }

  public static void packInPlace() {
    ZipUtil.unexplode(new File("/tmp/demo.zip"));
  }

  public static void packWithPrefix() {
    ZipUtil.pack(new File("/tmp/demo"), new File("/tmp/demo.zip"), new NameMapper() {
      public String map(String name) {
        return "doc/" + name;
      }
    });
  }

  public static void addEntry() {
    ZipUtil.addEntry(new File("/tmp/demo.zip"), "doc/readme.txt", new File("f/tmp/oo.txt"), new File("/tmp/new.zip"));
  }

  public static void addEntryInMemory() {
    ZipUtil.addEntry(new File("/tmp/demo.zip"), "doc/readme.txt", "bar".getBytes(), new File("/tmp/new.zip"));
  }

  public static void addEntryCustom() {
    ZipEntrySource[] entries = new ZipEntrySource[] { new FileSource("doc/readme.txt", new File(FOO_TXT)),
        new ByteSource("sample.txt", "bar".getBytes()) };
    ZipUtil.addEntries(new File("/tmp/demo.zip"), entries, new File("/tmp/new.zip"));
  }

  public static void replaceEntry() {
    boolean replaced = ZipUtil.replaceEntry(new File("/tmp/demo.zip"), "doc/readme.txt", new File("/tmp/foo.txt"),
        new File("/tmp/new.zip"));
    System.out.println("Replaced: " + replaced);
  }

  public static void replaceEntryInPlace() {
    boolean replaced = ZipUtil.replaceEntry(new File("/tmp/demo.zip"), "doc/readme.txt", "bar".getBytes(), new File(
        "/tmp/new.zip"));
    System.out.println("Replaced: " + replaced);
  }

  public static void replaceEntryCustom() {
    ZipEntrySource[] entries = new ZipEntrySource[] { new FileSource("doc/readme.txt", new File(FOO_TXT)),
        new ByteSource("sample.txt", "bar".getBytes()) };
    boolean replaced = ZipUtil.replaceEntries(new File("/tmp/demo.zip"), entries, new File("/tmp/new.zip"));
    System.out.println("Replaced: " + replaced);
  }

  /* Transforming */

  public static void transformEntry() {
    ZipUtil.transformEntry(new File("/tmp/demo"), "sample.txt", new StringZipEntryTransformer() {
      protected String transform(ZipEntry zipEntry, String input) throws IOException {
        return input.toUpperCase();
      }
    }, new File("/tmp/demo.zip"));
  }

}
