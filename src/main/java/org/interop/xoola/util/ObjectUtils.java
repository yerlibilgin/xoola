package org.interop.xoola.util;

/**
 * @author yerlibilgin
 */
public class ObjectUtils {

  /**
   * Check  if <code>object</code> is not null and return it, if it is null return the default value.
   * @param object
   * @param defVal
   * @param <T>
   * @return
   */
  public static <T> T getOrDefault(Object object, T defVal) {
    if (object != null) {
      return (T) object;
    }

    return defVal;
  }
}
