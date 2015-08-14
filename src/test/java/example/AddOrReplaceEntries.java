package example;

import java.io.File;

import org.zeroturnaround.zip.FileSource;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;

public class AddOrReplaceEntries {
  public static void main(String[] args) {
    ZipEntrySource[] addedEntries = new ZipEntrySource[] {
        new FileSource("/some/path/File1.txt", new File("/tmp/file1.txt")),
        new FileSource("/some/path/File2.txt", new File("/tmp/file2.txt")),
        new FileSource("/some/path/File3.txt", new File("/tmp/file2.txt")),
    };
    ZipUtil.addOrReplaceEntries(new File("my-zip-archive.zip"), addedEntries);
  }
}
