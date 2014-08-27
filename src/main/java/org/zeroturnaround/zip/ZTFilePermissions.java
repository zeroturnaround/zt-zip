package org.zeroturnaround.zip;

/**
 * This class holds POSIX file permissions.
 * 
 * @author Viktor Karabut
 */
class ZTFilePermissions {
  private boolean isDirectory;

  private boolean ownerCanRead;
  private boolean ownerCanWrite;
  private boolean ownerCanExecute;

  private boolean groupCanRead;
  private boolean groupCanWrite;
  private boolean groupCanExecute;

  private boolean othersCanRead;
  private boolean othersCanWrite;
  private boolean othersCanExecute;

  boolean isDirectory() {
    return isDirectory;
  }

  void setDirectory(boolean isDirectory) {
    this.isDirectory = isDirectory;
  }

  boolean isOwnerCanRead() {
    return ownerCanRead;
  }

  void setOwnerCanRead(boolean ownerCanRead) {
    this.ownerCanRead = ownerCanRead;
  }

  boolean isOwnerCanWrite() {
    return ownerCanWrite;
  }

  void setOwnerCanWrite(boolean ownerCanWrite) {
    this.ownerCanWrite = ownerCanWrite;
  }

  boolean isOwnerCanExecute() {
    return ownerCanExecute;
  }

  void setOwnerCanExecute(boolean ownerCanExecute) {
    this.ownerCanExecute = ownerCanExecute;
  }

  boolean isGroupCanRead() {
    return groupCanRead;
  }

  void setGroupCanRead(boolean groupCanRead) {
    this.groupCanRead = groupCanRead;
  }

  boolean isGroupCanWrite() {
    return groupCanWrite;
  }

  void setGroupCanWrite(boolean groupCanWrite) {
    this.groupCanWrite = groupCanWrite;
  }

  boolean isGroupCanExecute() {
    return groupCanExecute;
  }

  void setGroupCanExecute(boolean groupCanExecute) {
    this.groupCanExecute = groupCanExecute;
  }

  boolean isOthersCanRead() {
    return othersCanRead;
  }

  void setOthersCanRead(boolean othersCanRead) {
    this.othersCanRead = othersCanRead;
  }

  boolean isOthersCanWrite() {
    return othersCanWrite;
  }

  void setOthersCanWrite(boolean othersCanWrite) {
    this.othersCanWrite = othersCanWrite;
  }

  boolean isOthersCanExecute() {
    return othersCanExecute;
  }

  void setOthersCanExecute(boolean othersCanExecute) {
    this.othersCanExecute = othersCanExecute;
  }
}