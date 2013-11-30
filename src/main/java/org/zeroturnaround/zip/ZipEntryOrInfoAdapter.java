package org.zeroturnaround.zip;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

class ZipEntryOrInfoAdapter implements ZipEntryCallback, ZipInfoCallback {

  private final ZipEntryCallback entryCallback;
  private final ZipInfoCallback infoCallback;

  public ZipEntryOrInfoAdapter(ZipEntryCallback entryCallback, ZipInfoCallback infoCallback) {
    if (entryCallback != null && infoCallback != null || entryCallback == null && infoCallback == null) {
      throw new IllegalArgumentException("Only one of ZipEntryCallback and ZipInfoCallback must be specified together");
    }
    this.entryCallback = entryCallback;
    this.infoCallback = infoCallback;
  }

  public void process(ZipEntry zipEntry) throws IOException {
    infoCallback.process(zipEntry);
  }

  public void process(InputStream in, ZipEntry zipEntry) throws IOException {
    if (entryCallback != null) {
      entryCallback.process(in, zipEntry);
    }
    else {
      process(zipEntry);
    }
  }

}