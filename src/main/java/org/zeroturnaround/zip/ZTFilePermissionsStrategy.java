package org.zeroturnaround.zip;

import java.io.File;

public interface ZTFilePermissionsStrategy {
  ZTFilePermissions getPermissions(File file);
  void setPermissions(File file, ZTFilePermissions permissions);
}
