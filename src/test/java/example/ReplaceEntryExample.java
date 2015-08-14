package example;

import java.io.File;
import java.io.FileWriter;

import org.zeroturnaround.zip.ZipUtil;

public class ReplaceEntryExample {
  private ReplaceEntryExample() {
  }

  public static void main(String[] args) throws Exception {
    replaceEntry();
  }

  public static void replaceEntry() throws Exception {
    // lets unpack a file
    File zipArchive = new File("src/test/resources/demo.zip");
    File resultingFile = new File("foo.txt");
    ZipUtil.unpackEntry(zipArchive, "foo.txt", resultingFile);

    // lets work with the file a bit
    FileWriter fw = new FileWriter(resultingFile);
    fw.write("Hello World!\n");
    fw.close();

    ZipUtil.replaceEntry(zipArchive, "foo.txt", resultingFile);
  }

}
