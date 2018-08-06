package org.zeroturnaround.zip;

import java.nio.file.Path;

public class MaliciousZipException extends ZipException {

  public MaliciousZipException(Path outputDir, String name) {
    super("The file " + name + " is trying to leave the target output directory of " + outputDir);
  }

}
