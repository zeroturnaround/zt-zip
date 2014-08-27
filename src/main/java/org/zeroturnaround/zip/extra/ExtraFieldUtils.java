/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.zeroturnaround.zip.extra;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipException;

/**
 * This is a class that has been made significantly smaller (deleted a bunch of methods) and originally
 * is from the Apache Ant Project (http://ant.apache.org), org.apache.tools.zip package.
 * All license and other documentation is intact.
 * 
 * ZipExtraField related methods
 * 
 */
// CheckStyle:HideUtilityClassConstructorCheck OFF (bc)
public class ExtraFieldUtils {

  private static final int WORD = 4;

  /**
   * Static registry of known extra fields.
   * 
   * @since 1.1
   */
  private static final Map<ZipShort, Class<?>> implementations;

  static {
    implementations = new ConcurrentHashMap<ZipShort, Class<?>>();
    register(AsiExtraField.class);
  }

  /**
   * Register a ZipExtraField implementation.
   * 
   * <p>
   * The given class must have a no-arg constructor and implement the {@link ZipExtraField ZipExtraField interface}.
   * </p>
   * 
   * @param c the class to register
   * 
   * @since 1.1
   */
  public static void register(Class<?> c) {
    try {
      ZipExtraField ze = (ZipExtraField) c.newInstance();
      implementations.put(ze.getHeaderId(), c);
    }
    catch (ClassCastException cc) {
      throw new RuntimeException(c + " doesn\'t implement ZipExtraField");
    }
    catch (InstantiationException ie) {
      throw new RuntimeException(c + " is not a concrete class");
    }
    catch (IllegalAccessException ie) {
      throw new RuntimeException(c + "\'s no-arg constructor is not public");
    }
  }

  /**
   * Create an instance of the appropriate ExtraField, falls back to {@link UnrecognizedExtraField UnrecognizedExtraField}.
   * 
   * @param headerId the header identifier
   * @return an instance of the appropriate ExtraField
   * @exception InstantiationException if unable to instantiate the class
   * @exception IllegalAccessException if not allowed to instantiate the class
   * @since 1.1
   */
  public static ZipExtraField createExtraField(ZipShort headerId)
      throws InstantiationException, IllegalAccessException {
    Class<?> c = implementations.get(headerId);
    if (c != null) {
      return (ZipExtraField) c.newInstance();
    }
    UnrecognizedExtraField u = new UnrecognizedExtraField();
    u.setHeaderId(headerId);
    return u;
  }

  public static ZipExtraField[] parseA(byte[] data) throws ZipException {
    List<ZipExtraField> v = parse(data);
    ZipExtraField[] result = new ZipExtraField[v.size()];
    return v.toArray(result);
  }

  /**
   * Split the array into ExtraFields and populate them with the
   * given data as local file data, throwing an exception if the
   * data cannot be parsed.
   * 
   * @param data an array of bytes as it appears in local file data
   * @return an array of ExtraFields
   * @throws ZipException on error
   */
  public static List<ZipExtraField> parse(byte[] data) throws ZipException {
    List<ZipExtraField> v = new ArrayList<ZipExtraField>();
    if (data == null) {
      return v;
    }
    int start = 0;
    while (start <= data.length - WORD) {
      ZipShort headerId = new ZipShort(data, start);
      int length = (new ZipShort(data, start + 2)).getValue();
      if (start + WORD + length > data.length) {
        throw new ZipException("bad extra field starting at "
            + start + ".  Block length of "
            + length + " bytes exceeds remaining"
            + " data of "
            + (data.length - start - WORD)
            + " bytes.");
      }
      try {
        ZipExtraField ze = createExtraField(headerId);
        ze.parseFromLocalFileData(data, start + WORD, length);
        v.add(ze);
      }
      catch (InstantiationException ie) {
        throw new ZipException(ie.getMessage());
      }
      catch (IllegalAccessException iae) {
        throw new ZipException(iae.getMessage());
      }
      start += (length + WORD);
    }
    return v;
  }

  /**
   * Merges the local file data fields of the given ZipExtraFields.
   * 
   * @param data an array of ExtraFiles
   * @return an array of bytes
   * @since 1.1
   */
  public static byte[] mergeLocalFileDataData(List<ZipExtraField> data) {
    int regularExtraFieldCount = data.size();

    int sum = WORD * regularExtraFieldCount;
    for (ZipExtraField element : data) {
      sum += element.getLocalFileDataLength().getValue();
    }

    byte[] result = new byte[sum];
    int start = 0;
    for (ZipExtraField element : data) {
      System.arraycopy(element.getHeaderId().getBytes(),
          0, result, start, 2);
      System.arraycopy(element.getLocalFileDataLength().getBytes(),
          0, result, start + 2, 2);
      byte[] local = element.getLocalFileDataData();
      System.arraycopy(local, 0, result, start + WORD, local.length);
      start += (local.length + WORD);
    }
    return result;
  }
}
