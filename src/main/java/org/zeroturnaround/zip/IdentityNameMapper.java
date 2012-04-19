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
package org.zeroturnaround.zip;

/**
 * NOP implementation of the name mapper.
 * 
 * @author Rein Raudjärv
 * 
 * @see NameMapper
 */
final class IdentityNameMapper implements NameMapper {
  
  public static final NameMapper INSTANCE = new IdentityNameMapper();

  private IdentityNameMapper() {}
  
  public String map(String name) {
    return name;
  }

}
