package org.zeroturnaround.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import junit.framework.TestCase;

public class UmlautTest extends TestCase {
  private static final File file = new File("src/test/resources/umlauts-öäš.zip");
  // See StackOverFlow post why I'm not using just unicode
  // http://stackoverflow.com/questions/6153345/different-utf8-encoding-in-filenames-os-x/6153713#6153713
  private static final List fileContents = new ArrayList() {
    {
      add("umlauts-öäš/");
      add("umlauts-öäš/Ro\u0308mer.txt"); // Römer - but using the escape code that HFS uses
      add("umlauts-öäš/Raudja\u0308rv.txt"); // Raudjärv - but escape code from HFS
      add("umlauts-öäš/S\u030celajev.txt"); // Šelajev - but escape code from HFS
    }
  };

  public void testIterateWithCharset() throws Exception {
    FileInputStream fis = new FileInputStream(file);

    ZipUtil.iterate(fis, new ZipEntryCallback() {
      public void process(InputStream in, ZipEntry zipEntry) throws IOException {
        assertTrue(zipEntry.getName(), fileContents.contains(zipEntry.getName()));
      }
    }, Charset.forName("UTF8"));
  }

  public void testZipFileGetEntriesWithCharset() throws Exception {
    ZipFile zf = ZipFileUtil.getZipFile(file, Charset.forName("UTF8"));
    Enumeration entries = zf.entries();
    while (entries.hasMoreElements()) {
      ZipEntry ze = (ZipEntry) entries.nextElement();
      assertTrue(ze.getName(), fileContents.contains(ze.getName()));
    }
  }
}
