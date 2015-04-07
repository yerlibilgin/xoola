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

import java.util.Properties;

import org.apache.log4j.Logger;
import org.interop.xoola.core.XoolaInvocationHandler;
import org.interop.xoola.core.XoolaProperty;
import org.interop.xoola.transport.Invocation;
import org.interop.xoola.transport.Response;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

/**
 * @author dogan, muhammet
 * 
 */
public abstract class XoolaNettyHandler extends SimpleChannelHandler {
  private static final Logger LOGGER = Logger.getLogger(XoolaNettyHandler.class);
  // Xoola
  public XoolaInvocationHandler invocationHandler;
  public int pingTimeout;
  protected int serverPort;
  private String serverId;
  protected long responseTimeout;

  public XoolaNettyHandler(Properties properties, XoolaInvocationHandler handler) {
    this.invocationHandler = handler;
    this.serverPort = Integer.parseInt(properties.get(XoolaProperty.PORT).toString());
    this.serverId = (String) properties.get(XoolaProperty.SERVERID);
    this.pingTimeout = Integer.parseInt(properties.get(XoolaProperty.PING_TIMEOUT).toString());
    this.responseTimeout = Long.parseLong(properties.getProperty(XoolaProperty.NETWORK_RESPONSE_TIMEOUT));
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    Object message = e.getMessage();
    if (message instanceof Invocation) {
      Object result = this.getHandler().receiveInvocation((Invocation) message);
      ctx.getChannel().write(result);
    } else if (message instanceof Response) {
      this.getHandler().receiveResponse((Response) message);
    } else {
      System.err.println("----------------------- invalid instance " + message);
    }
  }

  @Override
  public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    super.channelClosed(ctx, e);
    LOGGER.info("Channel closed " + e.getChannel());
  }

  /**
   * @return the handler
   */
  public XoolaInvocationHandler getHandler() {
    return this.invocationHandler;
  }

  /**
   * @param handler
   *          the handler to set
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
