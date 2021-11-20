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


public class Local implements LocalInterface {
  private static final long serialVersionUID = -2263511643711634304L;

  /*
   * (non-Javadoc)
   *
   * @see gov.tubitak.xoola.java.test.LocalInterface#strangeAddMethod(int,
   * java.lang.Double)
   */
  @Override
  public String strangeAddMethod2(Integer a, Double b, String kanka) {
    try {
      return "localllll" + (a + b);
    } finally {
    }
  }
}