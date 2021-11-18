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

package gov.tubitak.xoola.tcpcom.connmanager.client;

import gov.tubitak.xoola.transport.TransportObject;

/**
 * The type Ping pong.
 * @author yerlibilgin
 */
public class PingPong implements TransportObject {
 private static final long serialVersionUID = 6757028768408150419L;

 /**
  * The P.
  */
 public int p;

 /**
  * The constant PING.
  */
 public static final int PING = 0;
 /**
  * The constant PONG.
  */
 public static final int PONG = 1;

 /**
  * Instantiates a new Ping pong.
  */
 public PingPong() {
 }

 /**
  * Instantiates a new Ping pong.
  *
  * @param p the p
  */
 public PingPong(int p) {
  super();
  this.p = p;
 }
}
