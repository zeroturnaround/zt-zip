package example;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.commons.IOUtils;


public class UnpackExample {
  private UnpackExample() {}
  
  public static void usual() throws IOException {
    ZipFile zf = new ZipFile("demo.zip");
    try {
      Enumeration en = zf.entries();
      while (en.hasMoreElements()) {
        ZipEntry e = (ZipEntry) en.nextElement();

        InputStream in = null;
        OutputStream out = null; 
        try {
          in = zf.getInputStream(e);
          final File zipEntryFile = new File("demo", e.getName());
          if (!zipEntryFile.toPath().normalize().startsWith("demo")) {
            throw new IOException("Bad zip entry");
          }
          out = new FileOutputStream(zipEntryFile);
          IOUtils.copy(in, out);
        }
        finally {
          IOUtils.closeQuietly(in);
          IOUtils.closeQuietly(out);
        }
      }
    }
    finally {
      zf.close();
    }
    
    zf = new ZipFile("demo.zip");
    byte[] bytes = new byte[0];
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
    ZipUtil.unpack(new File("demo.zip"), new File("demo"));
  }
  
}
