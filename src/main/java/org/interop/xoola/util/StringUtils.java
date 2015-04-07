/*
 * XoolA is a remote method call bridge between java and dotnet platforms.
 * Copyright (C) 2010 Muhammet YILDIZ, Doğan ERSÖZ
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.interop.xoola.util;

import java.nio.charset.Charset;

/**
 * @author dogan, muhammet
 * 
 */
public final class StringUtils {
  /**
    *
    */
  private static final Charset CHARSET = Charset.forName("utf-8");

  /**
   * @param string
   * @return
   */
  public static byte[] getBytes(String string) {
    return string.getBytes(CHARSET);
  }

  /**
   * @param message
   * @return
   */
  public static String getString(byte[] message) {
    return new String(message, CHARSET);
  }

}
