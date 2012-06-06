package org.zeroturnaround.zip.transform;

/**
 * A transformer assigned to a certain ZIP entry.
 * 
 * @author Rein Raudjärv
 */
public class ZipEntryTransformerEntry {

  private final String path;
  
  private final ZipEntryTransformer transformer;

  public ZipEntryTransformerEntry(String path, ZipEntryTransformer transformer) {
    this.path = path;
    this.transformer = transformer;
  }
  
  public String getPath() {
    return path;
  }
  
  public ZipEntryTransformer getTransformer() {
    return transformer;
  }

  public String toString() {
    return path + "=" + transformer;
  }

}
