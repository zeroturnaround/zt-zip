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

#### Print all entries in a ZIP archive
```java
ZipUtil.iterate(new File("/tmp/archive.zip"), new ZipEntryCallback() {
      public void process(InputStream is, ZipEntry entry) throws IOException {
        String name = entry.getName();
        System.out.println(name);
      }
    });
```

