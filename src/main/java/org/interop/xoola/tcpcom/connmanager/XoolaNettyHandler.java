/*
 * XoolA is a remote method call bridge between java and dotnet platforms.
 * Copyright (C) 2010 Muhammet YILDIZ, Doğan ERSÖZ
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.interop.xoola.tcpcom.connmanager;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.apache.log4j.Logger;
import org.interop.xoola.core.XoolaInvocationHandler;
import org.interop.xoola.core.XoolaProperty;
import org.interop.xoola.core.XoolaPropertyDefaults;
import org.interop.xoola.transport.Invocation;
import org.interop.xoola.transport.Response;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author dogan, muhammet
 */
public abstract class XoolaNettyHandler extends ChannelDuplexHandler {
  private static final Logger LOGGER = Logger.getLogger(XoolaNettyHandler.class);
  // Xoola
  public XoolaInvocationHandler invocationHandler;
  protected int serverPort;
  private String serverId;
  protected long responseTimeout;
  protected long handshakeTimeout;

  public XoolaNettyHandler(Properties properties, XoolaInvocationHandler handler) {
    this.invocationHandler = handler;
    this.serverPort = Integer.parseInt(properties.getProperty(XoolaProperty.PORT, XoolaPropertyDefaults.PORT));
    this.serverId = properties.getProperty(XoolaProperty.SERVERID, XoolaPropertyDefaults.SERVERID);
    this.responseTimeout = Long.parseLong(properties.getProperty(XoolaProperty.NETWORK_RESPONSE_TIMEOUT, XoolaPropertyDefaults.NETWORK_RESPONSE_TIMEOUT));
    this.handshakeTimeout = Long.parseLong(properties.getProperty(XoolaProperty.HANDSHAKE_TIMEOUT, XoolaPropertyDefaults.HANDSHAKE_TIMEOUT));
  }

  final ExecutorService executorService = Executors.newCachedThreadPool();

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object message) {

    if (message instanceof Invocation) {
      executorService.submit(new Runnable() {
        private Object actualMessage = message;
        private ChannelHandlerContext actualContext = ctx;
        @Override
        public void run() {
          Object result = XoolaNettyHandler.this.getHandler().receiveInvocation((Invocation) actualMessage);
          actualContext.channel().writeAndFlush(result);
        }
      });
    } else if (message instanceof Response) {
      this.getHandler().receiveResponse((Response) message);
    } else {
      LOGGER.warn("Invalid message " + message);
    }
  }

  /**
   * @return the handler
   */
  public XoolaInvocationHandler getHandler() {
    return this.invocationHandler;
  }

  /**
   * @param handler the handler to set
   */
  public void setHandler(XoolaInvocationHandler handler) {
    this.invocationHandler = handler;
  }

  public String getServerId() {
    return serverId;
  }

  public void setServerId(String serverId) {
    this.serverId = serverId;
  }

  public abstract void send(String id, Object message);

  public abstract void start();

  public abstract void stop();
}
