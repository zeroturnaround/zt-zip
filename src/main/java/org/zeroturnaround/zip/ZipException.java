package org.zeroturnaround.zip;

public class ZipException extends RuntimeException {
  public ZipException(String msg) {
    super(msg);
  }

  public ZipException(Exception e) {
    super(e);
  }
}
