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

import gov.tubitak.xoola.internal.XoolaClientInvocationHandler;
import gov.tubitak.xoola.internal.XoolaInvocationHandler;
import gov.tubitak.xoola.internal.XoolaServerInvocationHandler;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * The main entry point of the Xoola System
 *
 * @author yerlibilgin
 */
public class Xoola {


  /**
   * Init xoola.
   *
   * @param properties the properties
   * @return xoola
   */
  public static Xoola init(Properties properties) {

    prepareParameters(properties);

    Object tierMode = properties.get(XoolaProperty.MODE);
    if (tierMode == null) {
      throw new IllegalArgumentException("Xoola properties do not contain MODE");
    }
    if (tierMode.equals(XoolaTierMode.CLIENT)) {
      return new Xoola(new XoolaClientInvocationHandler(properties));
    }
    if (tierMode.equals(XoolaTierMode.SERVER)) {
      return new Xoola(new XoolaServerInvocationHandler(properties));
    }
    throw new IllegalArgumentException("Illegal Xoola tier mode " + tierMode);
  }


  /**
   * The Handler.
   */
  public XoolaInvocationHandler handler;

  /**
   * Instantiates a new Xoola.
   *
   * @param xoolaHandler the xoola handler
   */
  Xoola(XoolaInvocationHandler xoolaHandler) {
    this.handler = xoolaHandler;
  }

  /**
   * A proxy method for providing a cancelling mechanism for the underlying method call.
   */
  public void cancel() {
    this.handler.cancel();
  }

  /**
   * This method is used to get a proxy for the given remote object.
   *
   * @param <T>              the type parameter
   * @param interfaze        the interfaze
   * @param remoteObjectName the remote object name
   * @return the t
   */
  public <T> T get(Class<T> interfaze, String remoteObjectName) {
    return this.handler.get(interfaze, remoteObjectName);
  }

  /**
   * Get t.
   *
   * @param <T>              the type parameter
   * @param interfaze        the interfaze
   * @param remoteObjectName the remote object name
   * @param async            the async
   * @return the t
   */
  public <T> T get(Class<T> interfaze, String remoteObjectName, boolean async) {
    return this.handler.get(interfaze, remoteObjectName, async);
  }

  /**
   * Get t.
   *
   * @param <T>              the type parameter
   * @param interfaze        the interfaze
   * @param remoteName       the remote name
   * @param remoteObjectName the remote object name
   * @return the t
   */
  public <T> T get(Class<T> interfaze, String remoteName, String remoteObjectName) {
    return this.handler.get(interfaze, remoteName, remoteObjectName);
  }

  /**
   * Get t.
   *
   * @param <T>              the type parameter
   * @param interfaze        the interfaze
   * @param remoteName       the remote name
   * @param remoteObjectName the remote object name
   * @param async            the async
   * @return the t
   */
  public <T> T get(Class<T> interfaze, String remoteName, String remoteObjectName, boolean async) {
    return this.handler.get(interfaze, remoteName, remoteObjectName, async);
  }

  /**
   * Check {@link XoolaInvocationHandler#unregister(String name)}
   *
   * @param name the name
   */
  public void unregisterObject(String name) {
    this.handler.unregister(name);
  }

  /**
   * Check {@link XoolaInvocationHandler#registerObject(String name, Object object)}
   *
   * @param name   the name
   * @param object the object
   */
  public void registerObject(String name, Object object) {
    this.handler.registerObject(name, object);
  }

  /**
   * Close.
   */
  public void close() {
    this.handler.stop();
  }

  /**
   * Scan through the parameters and detect the parameters that are loaded using 'setProperty' convert them to the
   * actual types
   */
  private static void prepareParameters(Properties properties) {
    for (String name : properties.stringPropertyNames()) {
      //@formatter:off
      if (name.equalsIgnoreCase(XoolaProperty.PORT) ||
          name.equalsIgnoreCase(XoolaProperty.PING_TIMEOUT) ||
          name.equalsIgnoreCase(XoolaProperty.HANDSHAKE_TIMEOUT) ||
          name.equalsIgnoreCase(XoolaProperty.NETWORK_RESPONSE_TIMEOUT) ||
          name.equalsIgnoreCase(XoolaProperty.RECONNECT_RETRY_TIMEOUT)) {
        //@formatter:on
        properties.put(name, Integer.parseInt(properties.getProperty(name)));
      } else {
        properties.put(name, properties.getProperty(name));
      }
    }
  }

  /**
   * Add connection listener.
   *
   * @param connectionListener the connection listener
   */
  public void addConnectionListener(XoolaConnectionListener connectionListener) {
    this.handler.addConnectionListener(connectionListener);
  }

  /**
   * Gets id.
   *
   * @return the id
   */
  public String getId() {
    return handler.getId();
  }

  /**
   * Start.
   */
  public void start() {
    this.handler.start();
  }

  /**
   * Stop.
   */
  public void stop() {
    this.handler.stop();
  }

  /**
   * Wait for connection.
   *
   * @throws InterruptedException the interrupted exception
   */
  public void waitForConnection() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    addConnectionListener(new XoolaConnectionListener() {
      @Override
      public void disconnected(XoolaInvocationHandler xoolaInvocationHandler, XoolaChannelState xcs) {
      }

      @Override
      public void connected(XoolaInvocationHandler xoolaInvocationHandler, XoolaChannelState xcs) {
        latch.countDown();
      }
    });
    latch.await();
  }
}
