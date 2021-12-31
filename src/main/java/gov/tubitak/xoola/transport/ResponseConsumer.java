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
package gov.tubitak.xoola.transport;

import gov.tubitak.xoola.exception.XCommunicationException;

/**
 * The type Response.
 *
 * @author yerlibilgin
 */
public class ResponseConsumer<T> {
  /**
   * The Return value.
   */
  public T value;

  public ResponseConsumer() {
  }

  public Object tryConsume(long timeout) {
    synchronized (this) {
      try {
        this.wait(timeout);
      } catch (InterruptedException ex) {
        throw new IllegalStateException("Wait interrupted");
      }
      try {
        if (value == null) {
          throw new XCommunicationException("Couldn't receive response within timeout");
        }

        if (value instanceof Throwable) {
          throw new XCommunicationException((Throwable) value);
        }
        return value;
      } finally {
        this.value = null;
      }
    }
  }

  public void reset() {
    this.value = null;
  }

  public void fireResponseReceived(T response) {
    synchronized (this) {
      this.value = response;
      this.notifyAll();
    }
  }
}
