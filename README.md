ZIP - convenience methods
=========================

### Quick Overview

The project was started and coded by Rein Raudjärv when he needed to process a large set of large ZIP archives for
LiveRebel internals. Soon after we started using the utility in other projects because of the ease of use and it
*just worked*.


### Installation

The project artifact can be downloaded from the
[snapshot](http://repos.zeroturnaround.com/nexus/content/repositories/zt-public-snapshots/) or [release](http://repos.zeroturnaround.com/nexus/content/repositories/zt-public-releases/) maven repository.

To include it in your maven project then you need to define the repository and the dependency.

```xml
<repository>
  <id>zt-releases</id>
  <url>http://repos.zeroturnaround.com/nexus/content/groups/zt-public/</url>
  <snapshots>
    <enabled>false</enabled>
  </snapshots>
</repository>

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

```java
package org.zeroturnaround
```
