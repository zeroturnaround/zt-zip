package example;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;
import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.FileSource;
import org.zeroturnaround.zip.NameMapper;
import org.zeroturnaround.zip.ZipEntryCallback;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipInfoCallback;
import org.zeroturnaround.zip.ZipUtil;

public class Examples {
  
  /* Unpacking */
  
  public static void contains() {
    boolean exists = ZipUtil.containsEntry(new File("/tmp/demo"), "foo.txt");
    System.out.println("Exists: " + exists);
  }
  
  public static void unpackEntryImMemory() {
    byte[] bytes = ZipUtil.unpackEntry(new File("/tmp/demo.zip"), "foo.txt");
    System.out.println("Read " + bytes.length + " bytes.");
  }
  
  public static void unpackEntry() {
    ZipUtil.unpackEntry(new File("/tmp/demo.zip"), "foo.txt", new File("/tmp/bar.txt"));
  }
  
  public static void unpack() {
    ZipUtil.unpack(new File("/tmp/demo.zip"), new File("/tmp/demo"));
  }
  
  public static void upnackInPlace() {
    ZipUtil.explode(new File("/tmp/demo.zip"));
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
        if (zipEntry.getName().endsWith(".class"))
          System.out.println("Found " + zipEntry.getName());
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
    ZipEntrySource[] entries = new ZipEntrySource[] {
        new FileSource("doc/readme.txt", new File("foo.txt")),
        new ByteSource("sample.txt", "bar".getBytes())
    };
    ZipUtil.addEntries(new File("/tmp/demo.zip"), entries, new File("/tmp/new.zip"));
  }
  
  public static void replaceEntry() {
    boolean replaced = ZipUtil.replaceEntry(new File("/tmp/demo.zip"), "doc/readme.txt", new File("/tmp/foo.txt"), new File("/tmp/new.zip"));
    System.out.println("Replaced: " + replaced);
  }
  
  public static void replaceEntryInPlace() {
    boolean replaced = ZipUtil.replaceEntry(new File("/tmp/demo.zip"), "doc/readme.txt", "bar".getBytes(), new File("/tmp/new.zip"));
    System.out.println("Replaced: " + replaced);
  }
  
  public static void replaceEntryCustom() {
    ZipEntrySource[] entries = new ZipEntrySource[] {
        new FileSource("doc/readme.txt", new File("foo.txt")),
        new ByteSource("sample.txt", "bar".getBytes())
    };
    boolean replaced = ZipUtil.replaceEntries(new File("/tmp/demo.zip"), entries, new File("/tmp/new.zip"));
    System.out.println("Replaced: " + replaced);
  }
  
  /* Comparison */
  
  public static void archiveEquals() {
    boolean equals = ZipUtil.archiveEquals(new File("/tmp/demo1.zip"), new File("/tmp/demo2.zip"));
    System.out.println("Archives are equal: " + equals);
  }
  
  public static void entryEquals() {
    boolean equals = ZipUtil.entryEquals(new File("/tmp/demo1.zip"), new File("/tmp/demo2.zip"), "foo.txt");
    System.out.println("Entries are equal: " + equals);
  }
  
  public static void entryEqualsDifferentNames() {
    boolean equals = ZipUtil.entryEquals(new File("/tmp/demo1.zip"), new File("/tmp/demo2.zip"), "foo1.txt", "foo2.txt");
    System.out.println("Entries are equal: " + equals);
  }

}
