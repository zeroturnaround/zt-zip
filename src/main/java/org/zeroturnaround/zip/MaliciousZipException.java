package org.zeroturnaround.zip;

import java.io.File;

public class MaliciousZipException extends ZipException {

  public MaliciousZipException(File outputDir, String name) {
    super("The file " + name + " is trying to leave the target output directory of " + outputDir);
  }

}
