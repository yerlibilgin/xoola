/*
 * XoolA is a remote method call bridge between java and dotnet platforms.
 * Copyright (C) 2010 Muhammet YILDIZ, Recep Doğan ERSÖZ
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

/**
 * Provides methods for threading issues, like mutex wait, notify. etc...
 * 
 * @author yerlibilgin
 * 
 */
public class ConcurrencyUtils {

  /**
   * Notifies all threads that wait for the given mutex
   * 
   * @param mutex
   */
  public static void mutexNotifyAll(Object mutex) {
    synchronized (mutex) {
      mutex.notifyAll();
    }
  }

  /**
   * Notifies threads that wait for the given mutex.
   * 
   * @param mutex
   */
  public static void mutexNotify(Object mutex) {
    synchronized (mutex) {
      mutex.notify();
    }
  }

  /**
   * Wait until another thread calls notify on the given mutex or the timeout
   * expires
   * 
   * @param timeout
   * @param connectionTimeout
   */
  public static void mutexWait(Object mutex, long timeout) {
    synchronized (mutex) {
      waitLoop: while (true)
        try {
          mutex.wait(timeout);
          break waitLoop;
        } catch (InterruptedException e) {
          continue waitLoop;
        }
    }
  }

}
