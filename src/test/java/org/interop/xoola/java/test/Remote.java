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
package org.interop.xoola.java.test;

import org.apache.log4j.Logger;

public class Remote implements RemoteInterface {
  private static final long serialVersionUID = -247299387843009939L;
  private static final Logger LOGGER = Logger.getLogger(Remote.class);

  /**
   * @param server
   * @throws Throwable
   */
  public Remote() {
  }

  @Override
  public String strangeAddMethod(int a, double b) {
    try {
      return "" + (a + b);
    } finally {
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.interop.xoola.java.test.RemoteInterface#strangeAddMethod3()
   */
  @Override
  public void strangeAddMethod3() {
    LOGGER.debug("m3");
  }

  @Override
  public void strangeVoidMethod(Long appId) {
    LOGGER.debug(" strange void method " + appId);
  }

}
