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

package gov.tubitak.xoola.tcpcom.connmanager.server;

import io.netty.channel.Channel;
import java.util.HashMap;
import java.util.Properties;
import gov.tubitak.xoola.core.XoolaProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Server registry.
 * @author yerlibilgin
 */
public class ServerRegistry {

  /**
   * The constant classLoader.
   */
  public static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerRegistry.class);
  private ClientAccessController clientAccessController;

  /**
   * The Client map.
   */
  public HashMap<String, Channel> clientMap;
  /**
   * The Inverse client map.
   */
  public HashMap<Channel, String> inverseClientMap;

  /**
   * Instantiates a new Server registry.
   *
   * @param properties the properties
   */
  public ServerRegistry(Properties properties) {
    String className = (String) properties.get(XoolaProperty.CLIENT_ACCESS_CONTROLLER_CLASS);
    inverseClientMap = new HashMap<>();
    clientMap = new HashMap<>();

    if (className == null) {
      LOGGER.warn("Client access controller null. Will allow everyone");
      // do the default configuration
      this.clientAccessController = new ClientAccessController() {
        /**
         * Allow everyone
         */
        @Override
        public boolean clientIsAllowed(String id) {
          return true;
        }
      };
    } else {
      try {
        LOGGER.info("Load class {} as access controller", className);
        clientMap = new HashMap<>();
        inverseClientMap = new HashMap<>();
        this.clientAccessController = (ClientAccessController) classLoader.loadClass(className).newInstance();
      } catch (Exception ex) {
        throw new IllegalArgumentException(ex);
      }
    }
  }

  /**
   * Using the underlying client access provider, check if the user is allowed
   * in the system
   *
   * @param id the id
   * @return boolean
   */
  public boolean isAllowed(String id) {
    return clientAccessController.clientIsAllowed(id);
  }

  /**
   * Has user boolean.
   *
   * @param remoteObjectName the remote object name
   * @return the boolean
   */
  public boolean hasUser(String remoteObjectName) {
    return clientMap.containsKey(remoteObjectName);
  }

  /**
   * When a user is connected, add him.
   *
   * @param userId  the user id
   * @param channel the channel
   */
  public void addUser(String userId, Channel channel) {
    this.clientMap.put(userId, channel);
    this.inverseClientMap.put(channel, userId);
  }

  /**
   * Clear.
   */
  public void clear() {
    clientMap.clear();
    inverseClientMap.clear();
  }

  /**
   * Has channel boolean.
   *
   * @param channel the channel
   * @return the boolean
   */
  public boolean hasChannel(Channel channel) {
    return inverseClientMap.containsKey(channel);
  }

  /**
   * Gets user.
   *
   * @param channel the channel
   * @return the user
   */
  public String getUser(Channel channel) {
    return inverseClientMap.get(channel);
  }

  /**
   * Remove user.
   *
   * @param remoteId the remote id
   */
  public void removeUser(String remoteId) {
    try {
      // fancy syntax
      inverseClientMap.remove(clientMap.remove(remoteId));
    } catch (Exception ex) {
      LOGGER.error("Warning: " + ex.getClass() + ": " + ex.getMessage());
    }
  }

  /**
   * Gets channel.
   *
   * @param remoteName the remote name
   * @return the channel
   */
  public Channel getChannel(String remoteName) {
    return clientMap.get(remoteName);
  }
}
