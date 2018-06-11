package org.interop.xoola.tcpcom.connmanager.server;

import java.util.HashMap;
import java.util.Properties;

import io.netty.channel.Channel;
import org.apache.log4j.Logger;
import org.interop.xoola.core.XoolaProperty;

public class ServerRegistry {

  public static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
  private static final Logger LOGGER = Logger.getLogger(ServerRegistry.class);
  private ClientAccessController clientAccessController;

  public HashMap<String, Channel> clientMap;
  public HashMap<Channel, String> inverseClientMap;

  public ServerRegistry(Properties properties) {
    String className = (String) properties.get(XoolaProperty.CLIENT_ACCESS_CONTROLLER_CLASS);
    inverseClientMap = new HashMap<Channel, String>();
    clientMap = new HashMap<String, Channel> ();

    if (className == null) {
      LOGGER.warn("Client access controller null. Will allow everyone");
      // do the default configuration
      this.clientAccessController = new ClientAccessController() {
        /**
         * Allow everyone :-)
         */
        @Override
        public boolean clientIsAllowed(String id) {
          return true;
        }
      };
    } else {
      try {
        LOGGER.info("Load class \"" + className + "\" as access controller");
        clientMap = new HashMap<String, Channel>();
        inverseClientMap = new HashMap<Channel, String>();
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
   * @param id
   * @return
   */
  public boolean isAllowed(String id) {
    return clientAccessController.clientIsAllowed(id);
  }

  public boolean hasUser(String remoteObjectName) {
    return clientMap.containsKey(remoteObjectName);
  }

  /**
   * When a user is connected, add him.
   *
   * @param userId
   * @param channel
   */
  public void addUser(String userId, Channel channel) {
    this.clientMap.put(userId, channel);
    this.inverseClientMap.put(channel, userId);
  }

  public void clear() {
    clientMap.clear();
    inverseClientMap.clear();
  }

  public boolean hasChannel(Channel channel) {
    return inverseClientMap.containsKey(channel);
  }

  public String getUser(Channel channel) {
    return inverseClientMap.get(channel);
  }

  public void removeUser(String remoteId) {
    try {
      // fancy syntax
      inverseClientMap.remove(clientMap.remove(remoteId));
    } catch (Exception ex) {
      System.err.println("Warning: " + ex.getClass() + ": " + ex.getMessage());
    }
  }

  public Channel getChannel(String remoteName) {
    return clientMap.get(remoteName);
  }
}
