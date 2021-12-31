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
package gov.tubitak.xoola.internal.tcpcom.connmanager;

import gov.tubitak.xoola.internal.Response;
import gov.tubitak.xoola.internal.XoolaInvocationHandler;
import gov.tubitak.xoola.core.XoolaProperty;
import gov.tubitak.xoola.core.XoolaPropertyDefaults;
import gov.tubitak.xoola.transport.Invocation;
import gov.tubitak.xoola.internal.ObjectUtils;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Netty transport handler
 *
 * @author yerlibilgin
 */
public abstract class XoolaNettyHandler extends ChannelDuplexHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(XoolaNettyHandler.class);
  /**
   * The Invocation handler.
   */
// Xoola
  public XoolaInvocationHandler invocationHandler;
  /**
   * The Server port.
   */
  protected int serverPort;
  private String serverId;
  /**
   * The Response timeout.
   */
  protected int responseTimeout;
  /**
   * The Handshake timeout.
   */
  protected int handshakeTimeout;

  /**
   * Instantiates a new Xoola netty handler.
   *
   * @param properties the properties
   * @param handler    the handler
   */
  public XoolaNettyHandler(Properties properties, XoolaInvocationHandler handler) {
    this.invocationHandler = handler;
    this.serverPort = ObjectUtils.getOrDefault(properties.get(XoolaProperty.PORT), XoolaPropertyDefaults.PORT);
    this.serverId = ObjectUtils.getOrDefault(properties.get(XoolaProperty.SERVERID), XoolaPropertyDefaults.SERVERID);
    this.responseTimeout = ObjectUtils.getOrDefault(properties.get(XoolaProperty.NETWORK_RESPONSE_TIMEOUT), XoolaPropertyDefaults.NETWORK_RESPONSE_TIMEOUT);
    this.handshakeTimeout = ObjectUtils.getOrDefault(properties.get(XoolaProperty.HANDSHAKE_TIMEOUT), XoolaPropertyDefaults.HANDSHAKE_TIMEOUT);
  }

  /**
   * The Executor service.
   */
  final ExecutorService executorService = Executors.newCachedThreadPool();

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object message) {
    if (message instanceof Invocation) {
      //I received an invocation i have to execute it
      executorService.submit(new Runnable() {
        private final Object actualMessage = message;
        private final ChannelHandlerContext actualContext = ctx;

        @Override
        public void run() {
          final Invocation invocation = (Invocation) this.actualMessage;
          Object result = XoolaNettyHandler.this.getHandler().receiveInvocation(invocation);
          actualContext.channel().writeAndFlush(result);
        }
      });
    } else if (message instanceof Response) {
      //I received a response.
      this.getHandler().receiveResponse((Response) message);
    } else {
      LOGGER.warn("Invalid message " + message);
    }
  }

  /**
   * Gets handler.
   *
   * @return the handler
   */
  public XoolaInvocationHandler getHandler() {
    return this.invocationHandler;
  }

  /**
   * Sets handler.
   *
   * @param handler the handler to set
   */
  public void setHandler(XoolaInvocationHandler handler) {
    this.invocationHandler = handler;
  }

  /**
   * Gets server id.
   *
   * @return the server id
   */
  public String getServerId() {
    return serverId;
  }

  /**
   * Sets server id.
   *
   * @param serverId the server id
   */
  public void setServerId(String serverId) {
    this.serverId = serverId;
  }

  /**
   * Send.
   *
   * @param id      the id
   * @param message the message
   */
  public abstract void send(String id, Object message);

  /**
   * Start.
   */
  public abstract void start();

  /**
   * Stop.
   */
  public abstract void stop();


}
