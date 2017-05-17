ZIP - convenience methods
=========================

### Continuous Integration 
[![Build Status](https://travis-ci.org/zeroturnaround/zt-zip.png)](https://travis-ci.org/zeroturnaround/zt-zip)

### Quick Overview

The project was started and coded by Rein Raudjärv when he needed to process a large set of large ZIP archives for
LiveRebel internals. Soon after we started using the utility in other projects because of the ease of use and it
*just worked*.

The project is built using java.util.zip.* packages for stream based access. Most convenience methods for filesystem
usage is also supported.

### Installation
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.zeroturnaround/zt-zip/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.zeroturnaround/zt-zip)

The project artifacts are available in [Maven Central Repository](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22zt-zip%22).
To include it in your maven project then you have to specify the dependency.

```xml
...
<dependency>
    <groupId>org.zeroturnaround</groupId>
    <artifactId>zt-zip</artifactId>
    <version>1.11</version>
    <type>jar</type>
</dependency>
...
```

Notice that [1.8](https://oss.sonatype.org/content/repositories/releases/org/zeroturnaround/zt-zip/1.8/) is the last Java 1.4 compatible release. 
Since then Java 1.5 is required.

## Background

We had the following functional requirements:

1. pack and unpack directories recursively
  1. include/exclude entries
  2. rename entries
  3. packing/unpacking in place - ZIP becomes directory and vice versa
2. iterate through ZIP entries
3. add or replace entries from files or byte arrays
4. transform ZIP entries
5. compare two archives - compare all entries ignoring time stamps

and these non-functional requirements:

1. use existing APIs as much as possible
2. be simple to use
3. be effective to use - do not traverse an entire ZIP file if only a single entry is needed
4. be safe to use - do not enable user to leave streams open and keep files locked
5. do not declare exceptions
6. be compatible with Java 1.5

## Examples

### Unpacking

#### Check if an entry exists in a ZIP archive
```java
boolean exists = ZipUtil.containsEntry(new File("/tmp/demo.zip"), "foo.txt");
```

#### Extract an entry from a ZIP archive into a byte array
```java
byte[] bytes = ZipUtil.unpackEntry(new File("/tmp/demo.zip"), "foo.txt");
```

#### Extract an entry from a ZIP archive with a specific Charset into a byte array
```java
byte[] bytes = ZipUtil.unpackEntry(new File("/tmp/demo.zip"), "foo.txt", Charset.forName("IBM437"));
```

#### Extract an entry from a ZIP archive into file system
```java
ZipUtil.unpackEntry(new File("/tmp/demo.zip"), "foo.txt", new File("/tmp/bar.txt"));
```

#### Extract a ZIP archive
```java
ZipUtil.unpack(new File("/tmp/demo.zip"), new File("/tmp/demo"));
```

#### Extract a ZIP archive which becomes a directory
```java
ZipUtil.explode(new File("/tmp/demo.zip"));
```

#### Extract a directory from a ZIP archive including the directory name
```java
ZipUtil.unpack(new File("/tmp/demo.zip"), new File("/tmp/demo"), new NameMapper() {
  public String map(String name) {
    return name.startsWith("doc/") ? name : null;
  }
});
```

#### Extract a directory from a ZIP archive excluding the directory name
```java
final String prefix = "doc/"; 
ZipUtil.unpack(new File("/tmp/demo.zip"), new File("/tmp/demo"), new NameMapper() {
  public String map(String name) {
    return name.startsWith(prefix) ? name.substring(prefix.length()) : name;
  }
});
```

#### Extract files from a ZIP archive that match a name pattern
```java
ZipUtil.unpack(new File("/tmp/demo.zip"), new File("/tmp/demo"), new NameMapper() {
  public String map(String name) {
    if (name.contains("/doc")) {
      return name;
    }
    else {
      // returning null from the map method will disregard the entry
      return null;
    }
  }
});
```

#### Print .class entry names in a ZIP archive
```java
ZipUtil.iterate(new File("/tmp/demo.zip"), new ZipInfoCallback() {
  public void process(ZipEntry zipEntry) throws IOException {
    if (zipEntry.getName().endsWith(".class"))
      System.out.println("Found " + zipEntry.getName());
  }
});
```

#### Print .txt entries in a ZIP archive (uses IoUtils from Commons IO)
```java
ZipUtil.iterate(new File("/tmp/demo.zip"), new ZipEntryCallback() {
  public void process(InputStream in, ZipEntry zipEntry) throws IOException {
    if (zipEntry.getName().endsWith(".txt")) {
      System.out.println("Found " + zipEntry.getName());
      IOUtils.copy(in, System.out);
    }
  }
});
```

### Packing

#### Compress a directory into a ZIP archive
```java
ZipUtil.pack(new File("/tmp/demo"), new File("/tmp/demo.zip"));
```

#### Compress a directory which becomes a ZIP archive
```java
ZipUtil.unexplode(new File("/tmp/demo.zip"));
```

#### Compress a directory into a ZIP archive with a parent directory
```java
ZipUtil.pack(new File("/tmp/demo"), new File("/tmp/demo.zip"), new NameMapper() {
  public String map(String name) {
    return "foo/" + name;
  }
});
```

#### Add an entry from file to a ZIP archive
```java
ZipUtil.addEntry(new File("/tmp/demo.zip"), "doc/readme.txt", new File("f/tmp/oo.txt"), new File("/tmp/new.zip"));
```

#### Add an entry from byte array to a ZIP archive
```java
ZipUtil.addEntry(new File("/tmp/demo.zip"), "doc/readme.txt", "bar".getBytes(), new File("/tmp/new.zip"));
```

#### Add an entry from file and from byte array to a ZIP archive
```java
ZipEntrySource[] entries = new ZipEntrySource[] {
    new FileSource("doc/readme.txt", new File("foo.txt")),
    new ByteSource("sample.txt", "bar".getBytes())
};
ZipUtil.addEntries(new File("/tmp/demo.zip"), entries, new File("/tmp/new.zip"));
```

#### Add an entry from file and from byte array to a output stream
```java
ZipEntrySource[] entries = new ZipEntrySource[] {
    new FileSource("doc/readme.txt", new File("foo.txt")),
    new ByteSource("sample.txt", "bar".getBytes())
};
OutputStream out = null;
try {
  out = new BufferedOutputStream(new FileOutputStream(new File("/tmp/new.zip")));
  ZipUtil.addEntries(new File("/tmp/demo.zip"), entries, out);
}
finally {
  IOUtils.closeQuietly(out);
}
```

#### Replace a ZIP archive entry from file 
```java
boolean replaced = ZipUtil.replaceEntry(new File("/tmp/demo.zip"), "doc/readme.txt", new File("/tmp/foo.txt"), new File("/tmp/new.zip"));
```

#### Replace a ZIP archive entry from byte array 
```java
boolean replaced = ZipUtil.replaceEntry(new File("/tmp/demo.zip"), "doc/readme.txt", "bar".getBytes(), new File("/tmp/new.zip"));
```

#### Replace a ZIP archive entry from file and byte array 
```java
ZipEntrySource[] entries = new ZipEntrySource[] {
    new FileSource("doc/readme.txt", new File("foo.txt")),
    new ByteSource("sample.txt", "bar".getBytes())
};
boolean replaced = ZipUtil.replaceEntries(new File("/tmp/demo.zip"), entries, new File("/tmp/new.zip"));
```

#### Add or replace entries in a ZIP archive
```java
ZipEntrySource[] addedEntries = new ZipEntrySource[] {
        new FileSource("/path/in/zip/File1.txt", new File("/tmp/file1.txt")),
        new FileSource("/path/in/zip/File2.txt", new File("/tmp/file2.txt")),
        new FileSource("/path/in/zip/File3.txt", new File("/tmp/file2.txt")),
    };
ZipUtil.addOrReplaceEntries(new File("/tmp/demo.zip"), addedEntries);
```

### Transforming

#### Transform a ZIP archive entry into uppercase
```java
boolean transformed = ZipUtil.transformEntry(new File("/tmp/demo"), "sample.txt", new StringZipEntryTransformer() {
    protected String transform(ZipEntry zipEntry, String input) throws IOException {
        return input.toUpperCase();
    }
}, new File("/tmp/demo.zip"));
```

### Comparison

#### Compare two ZIP archives (ignoring timestamps of the entries) 
```java
boolean equals = ZipUtil.archiveEquals(new File("/tmp/demo1.zip"), new File("/tmp/demo2.zip"));
```

#### Compare two ZIP archive entries with same name (ignoring timestamps of the entries) 
```java
boolean equals = ZipUtil.entryEquals(new File("/tmp/demo1.zip"), new File("/tmp/demo2.zip"), "foo.txt");
```

#### Compare two ZIP archive entries with different names (ignoring timestamps of the entries) 
```java
boolean equals = ZipUtil.entryEquals(new File("/tmp/demo1.zip"), new File("/tmp/demo2.zip"), "foo1.txt", "foo2.txt");
```

## Debugging

The library is using the [slf4j-api](http://www.slf4j.org/) logging framework. All the log statements are either **DEBUG** or **TRACE** level. Depending on the logging framework you are using a simple ```-Dorg.slf4j.simpleLogger.defaultLogLevel=debug``` or ```System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");``` will do the trick of showing log statements from the **zt-zip** library. You can further fine tune the levels and inclusion of log messages per package with your logging framework.
