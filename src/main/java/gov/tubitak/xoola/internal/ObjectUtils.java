/*
 * Copyright 2021-TUBITAK BILGEM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.tubitak.xoola.internal;

/**
 * The type Object utils.
 */
public class ObjectUtils {

  /**
   * Check  if <code>object</code> is not null and return it, if it is null return the default value.
   *
   * @param <T>          the type parameter
   * @param object       the object
   * @param defaultValue the default value
   * @return or default
   */
  public static <T> T getOrDefault(Object object, T defaultValue) {
    if (object != null) {
      return (T) object;
    }

    return defaultValue;
  }
}
