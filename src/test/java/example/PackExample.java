package example;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.zeroturnaround.zip.ZipUtil;


public class PackExample {

  private PackExample() {}
  
  public static void usual() throws IOException {
    File dir = new File("demo");
    
    ZipOutputStream out = new ZipOutputStream(new FileOutputStream("demo.zip"));
    try {
      File[] files = dir.listFiles();
      for (int i = 0; i < files.length; i++) {
        File file = files[i];
        
        ZipEntry entry = new ZipEntry(file.getName());
        entry.setSize(file.length());
        entry.setTime(file.lastModified());
        
        out.putNextEntry(entry);
        
        FileInputStream in = new FileInputStream(file);
        try {
          IOUtils.copy(in, out);
        }
        finally {
          IOUtils.closeQuietly(in);
        }
        
        out.closeEntry();
      }
    }
    finally {
      IOUtils.closeQuietly(out);
    }
  }
  
  public static void withUs() {
    ZipUtil.pack(new File("demo"), new File("demo.zip"));
  }
  
}
