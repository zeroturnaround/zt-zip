ZIP - convenience methods
=========================

### Quick Overview

The project was started and coded by Rein Raudjärv when he needed to process a large set of large ZIP archives for
LiveRebel internals. Soon after we started using the utility in other projects because of the ease of use and it
*just worked*.

The project is built using java.util.zip.* packages for stream based access. Most convenience methods for filesystem
usage is also supported.

### Installation

The project artifact can be downloaded from the
[snapshot](http://repos.zeroturnaround.com/nexus/content/repositories/zt-public-snapshots/) or [release](http://repos.zeroturnaround.com/nexus/content/repositories/zt-public-releases/) maven repository.

To include it in your maven project then you need to define the repository and the dependency.

```xml
<repositories>
    <repository>
      <id>zt-public-releases</id>
      <url>http://repos.zeroturnaround.com/nexus/content/groups/zt-public/</url>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
    </repository>
</repositories>

...
<dependency>
    <groupId>org.zeroturnaround</groupId>
    <artifactId>zt-zip</artifactId>
    <version>1.0</version>
    <type>jar</type>
    <scope>provided</scope>
</dependency>
...
```

### Examples

#### Check if an entry exists in a ZIP archive
```java
boolean exists = ZipUtil.containsEntry(new File("/tmp/demo"), "foo.txt");
```

#### Extract an entry from a ZIP archive into a byte array
```java
byte[] bytes = ZipUtil.unpackEntry(new File("/tmp/demo.zip"), "foo.txt");
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
