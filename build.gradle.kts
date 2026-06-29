/**
 *    Copyright (C) 2012 ZeroTurnaround LLC <support@zeroturnaround.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
plugins {
  java
  alias(libs.plugins.maven.publish)
}

group = "org.zeroturnaround"
version = "1.18-SNAPSHOT"
description =
  "The project is intended to have a small, easy and fast library to process ZIP archives. " +
    "Either create, modify or explode them. On disk or in memory."

repositories {
  mavenCentral()
}

java {
  // Java 8 is the floor: slf4j-api 2.x is compiled for Java 8, and the runtime
  // strategy classes (Java7Nio2ApiPermissionsStrategy, Java8TimestampStrategy)
  // reference java.nio.file APIs. The toolchain is auto-provisioned via the
  // foojay resolver in settings.gradle.kts when no JDK 8 is installed.
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
  }
}

dependencies {
  // slf4j-api is used only for internal logging and never appears in the public
  // API, so it is an implementation dependency: consumers get it transitively at
  // runtime but not on their compile classpath.
  implementation(libs.slf4j.api)

  testImplementation(libs.junit)
  // slf4j-simple is the slf4j binding for tests: no transitive dependencies and
  // released in lockstep with slf4j-api, so there is no separate version to track.
  testRuntimeOnly(libs.slf4j.simple)
}

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
  // The test sources are JUnit 4; Gradle picks JUnit 4 up automatically from
  // the test classpath, so no useJUnitPlatform() here.
  testLogging {
    events("passed", "skipped", "failed")
  }
}

tasks.jar {
  // Match the historical Maven build: no generated Maven descriptor in the jar
  // and no JRebel descriptor.
  manifest {
    attributes("Implementation-Title" to project.name, "Implementation-Version" to project.version)
  }
  exclude("**/rebel.xml")
}

// The Javadoc predates the strict doclint in JDK 8+; don't fail the build on it.
tasks.withType<Javadoc>().configureEach {
  (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

// Sign only when in-memory keys are provided (CI release). Local builds skip signing.
tasks.withType<Sign>().configureEach {
  enabled = project.findProperty("signingInMemoryKey") != null
}

mavenPublishing {
  // The release workflow invokes `publishAndReleaseToMavenCentral`, which both
  // uploads and releases; no need to also auto-release from the plain task.
  publishToMavenCentral()
  signAllPublications()

  coordinates(group.toString(), "zt-zip", version.toString())

  pom {
    name.set("ZT Zip")
    description.set(project.description)
    url.set("https://github.com/zeroturnaround/zt-zip")
    organization {
      name.set("ZeroTurnaround")
      url.set("https://zeroturnaround.com/")
    }
    licenses {
      license {
        name.set("The Apache Software License, Version 2.0")
        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
        comments.set("A business-friendly OSS license")
      }
    }
    developers {
      developer {
        id.set("nemecec")
        name.set("Neeme Praks")
        email.set("neeme@praks.net")
        url.set("https://github.com/nemecec")
      }
      developer {
        id.set("rein")
        name.set("Rein")
        email.set("rein@zeroturnaround.com")
        organization.set("ZeroTurnaround")
        organizationUrl.set("https://zeroturnaround.com")
      }
      developer {
        id.set("toomasr")
        name.set("Toomas")
        email.set("toomas@zeroturnaround.com")
        organization.set("ZeroTurnaround")
        organizationUrl.set("https://zeroturnaround.com")
      }
    }
    scm {
      url.set("https://github.com/zeroturnaround/zt-zip")
      connection.set("scm:git:git://github.com/zeroturnaround/zt-zip.git")
      developerConnection.set("scm:git:ssh://git@github.com/zeroturnaround/zt-zip.git")
    }
  }
}
