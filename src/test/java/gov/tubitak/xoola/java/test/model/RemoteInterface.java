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
package gov.tubitak.xoola.java.test.model;

import java.io.Serializable;

public interface RemoteInterface extends Serializable {
  /**
   * @param a
   * @param b
   * @return
   */
  String strangeAddMethod(int a, double b);

  /**
   * @return
   */
  void strangeAddMethod3();

  /**
   * @param appId
   */
  void strangeVoidMethod(Long appId);


  /**
   * When called, waits for years.
   */
  void tooLongMethod();
}
