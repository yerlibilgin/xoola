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
 * @author muhammet
 */
public interface XoolaProperty {
  String MODE = "MODE";
  /**
   * Default value: {@link XoolaPropertyDefaults#HOST}
   */
  String HOST = "HOST";
  /**
   * Default value: {@link XoolaPropertyDefaults#PORT}
   */
  String PORT = "PORT";
  /**
   * Default value: {@link XoolaPropertyDefaults#SERVERID}
   */
  String SERVERID = "SERVERID";
  /**
   * Default value: {@link XoolaPropertyDefaults#CLIENTID}
   */
  String CLIENTID = "CLIENTID";
  /**
   * Default value: {@link XoolaPropertyDefaults#NETWORK_RESPONSE_TIMEOUT}
   */
  String NETWORK_RESPONSE_TIMEOUT = "NETWORK_RESPONSE_TIMEOUT";
  /**
   * Default value {@link XoolaPropertyDefaults#PING_TIMEOUT}
   */
  String PING_TIMEOUT = "PING_TIMEOUT";
  /**
   * Default value: {@link XoolaPropertyDefaults#RECONNECT_RETRY_TIMEOUT}
   */
  String RECONNECT_RETRY_TIMEOUT = "RECONNECT_RETRY_TIMEOUT";
  /**
   * Default value: {@link XoolaPropertyDefaults#HANDSHAKE_TIMEOUT}
   */
  String HANDSHAKE_TIMEOUT = "HANDSHAKE_TIMEOUT";

  String CLIENT_ACCESS_CONTROLLER_CLASS = "CLIENT_ACCESS_CONTROLLER_CLASS";
  String CLASS_LOADER_PROVIDER_CLASS = "CLASS_LOADER_PROVIDER_CLASS";
}
