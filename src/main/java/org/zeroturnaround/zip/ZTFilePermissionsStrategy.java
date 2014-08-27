package org.zeroturnaround.zip;

import java.io.File;


/**
 * Maps {@link ZTFilePermissions} to real filesystem-specific file attributes.
 * 
 * @author Viktor Karabut
 */
public interface ZTFilePermissionsStrategy {
  
  /**
   * Get {@link ZTFilePermissions} from file.
   * 
   * @param file
   * @return permissions or <code>null</null> if cannot retrieve permissions info by some reason.
   */
  ZTFilePermissions getPermissions(File file);
  
  /**
   * Set {@link ZTFilePermissions} to file
   * 
   * @param file file
   * @param permissions permission
   */
  void setPermissions(File file, ZTFilePermissions permissions);
}
