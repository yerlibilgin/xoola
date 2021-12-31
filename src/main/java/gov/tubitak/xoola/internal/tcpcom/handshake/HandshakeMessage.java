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
package gov.tubitak.xoola.internal.tcpcom.handshake;

import gov.tubitak.xoola.transport.TransportObject;

/**
 * A POJO for client-server handshake
 */
public class HandshakeMessage implements TransportObject {
  /**
   *
   */
  private static final long serialVersionUID = -6054120973034178121L;
  public final String message;

  public HandshakeMessage(String message) {
    this.message = message;
  }

}
