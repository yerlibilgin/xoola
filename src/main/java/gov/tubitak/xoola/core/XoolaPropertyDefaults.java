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
package gov.tubitak.xoola.core;

/**
 * @author yerlibilgin
 */
public interface XoolaPropertyDefaults {
  String HOST = "localhost";
  String SERVERID = "xoolaServer";
  String CLIENTID = "xoolaClient";

  int PORT = 25000;
  int NETWORK_RESPONSE_TIMEOUT = 50000;
  int PING_TIMEOUT = 50000;
  int RECONNECT_RETRY_TIMEOUT = 10000;
  int HANDSHAKE_TIMEOUT = 20000;

  String CLIENT_ACCESS_CONTROLLER_CLASS = "CLIENT_ACCESS_CONTROLLER_CLASS";
  String CLASS_LOADER_PROVIDER_CLASS = "CLASS_LOADER_PROVIDER_CLASS";
}
