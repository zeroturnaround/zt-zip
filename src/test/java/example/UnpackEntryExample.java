package example;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.zeroturnaround.zip.ZipUtil;


public class UnpackEntryExample {
  private UnpackEntryExample() {}
  
  public static void usual() throws IOException {
    byte[] bytes = null;
    ZipFile zf = new ZipFile("demo.zip");
    try {
      ZipEntry ze = zf.getEntry("foo.txt");
      if (ze != null) {
        InputStream is = zf.getInputStream(ze);
        try {
          bytes = IOUtils.toByteArray(is);
        }
        finally {
          IOUtils.closeQuietly(is);
        }
      }
    }
    finally {
      zf.close();
    }
    
    System.out.println("Read " + bytes.length + " bytes.");
  }
  
  public static void withUs() {
    byte[] bytes = ZipUtil.unpackEntry(new File("demo.zip"), "foo.txt");
    
    System.out.println("Read " + bytes.length + " bytes.");
  }
  
}
