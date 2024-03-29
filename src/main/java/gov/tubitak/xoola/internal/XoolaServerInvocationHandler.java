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

package gov.tubitak.xoola.internal;

import gov.tubitak.xoola.core.XoolaProperty;
import gov.tubitak.xoola.exception.XIOException;
import gov.tubitak.xoola.internal.tcpcom.connmanager.server.NettyServer;
import gov.tubitak.xoola.transport.Invocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class XoolaServerInvocationHandler extends XoolaInvocationHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(XoolaServerInvocationHandler.class);
  private final NettyServer nettyServer;
  private final String id;

  public XoolaServerInvocationHandler(Properties properties) {
    super(properties);
    this.nettyServer = new NettyServer(properties, this);
    this.id = properties.get(XoolaProperty.SERVERID).toString();
  }

  @Override
  protected void sendMessage(String remoteName, Invocation message) {
    this.nettyServer.send(remoteName, message);
  }

  @Override
  public <T> T get(Class<T> interfaze, String remoteName, String remoteObjectName, boolean async) {
    LOGGER.info("Get remote interface for client: {}, object: {}", remoteName, remoteObjectName);
    if (remoteObjectName == null && !nettyServer.getServerRegistry().hasUser(remoteObjectName))
      throw new XIOException("Remote client not connected");

    return createProxyForClass(interfaze, remoteName, remoteObjectName, async);
  }

  @Override
  public void stop() {
    try {
      this.nettyServer.stop();
    } catch (Exception ex) {
    }

    nettyServer.getServerRegistry().clear();
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void start() {
    this.nettyServer.start();
  }
}
