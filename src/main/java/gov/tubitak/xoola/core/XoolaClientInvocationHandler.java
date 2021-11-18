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

import gov.tubitak.xoola.transport.Invocation;
import java.util.Properties;
import gov.tubitak.xoola.exception.XCommunicationException;
import gov.tubitak.xoola.tcpcom.connmanager.client.NettyClient;
import gov.tubitak.xoola.util.ObjectUtils;

/**
 * The type Xoola client invocation handler.
 * @author yerlibilgin
 */
public class XoolaClientInvocationHandler extends XoolaInvocationHandler {

  private NettyClient nettyClient;
  private String id;

  /**
   * Instantiates a new Xoola client invocation handler.
   *
   * @param properties the properties
   */
  public XoolaClientInvocationHandler(Properties properties) {
    super(properties);
    this.nettyClient = new NettyClient(properties, this);
    this.id = ObjectUtils.getOrDefault(properties.get(XoolaProperty.CLIENTID), "xoolaClient");
  }

  @Override
  protected void sendMessage(String remoteName, Invocation message) {
    nettyClient.send(remoteName, message);
  }

  @Override
  public <T> T get(Class<T> interfaze, String remoteName, String remoteObjectName, boolean async) {
    if (nettyClient.isAvailable()) {
      return createProxyForClass(interfaze, remoteName, remoteObjectName, async);
    }

    throw new XCommunicationException("Not connected to any servers");
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void stop() {
    nettyClient.stop();
  }

  @Override
  public void start() {
   nettyClient.start();
  }
}
