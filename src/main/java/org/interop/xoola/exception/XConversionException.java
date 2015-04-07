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
package org.interop.xoola.exception;

/**
 * @author dogan, muhammet
 * 
 */
public class XConversionException extends RuntimeException {

  /**
   * @param string
   */
  public XConversionException(String string) {
    super(string);
  }

  /**
   * @param string
   * @param e
   */
  public XConversionException(String string, Exception e) {
    super(string, e);
  }

  /**
     *
     */
  private static final long serialVersionUID = -9015175835219722491L;

}
