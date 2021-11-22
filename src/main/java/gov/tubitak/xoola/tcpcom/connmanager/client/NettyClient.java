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

package gov.tubitak.xoola.tcpcom.connmanager.client;

import gov.tubitak.xoola.core.XoolaInvocationHandler;
import gov.tubitak.xoola.core.XoolaProperty;
import gov.tubitak.xoola.core.XoolaPropertyDefaults;
import gov.tubitak.xoola.exception.XCommunicationException;
import gov.tubitak.xoola.tcpcom.connmanager.PingPongHandler;
import gov.tubitak.xoola.tcpcom.connmanager.XoolaNettyHandler;
import gov.tubitak.xoola.tcpcom.handshake2.ProtocolTracker;
import gov.tubitak.xoola.util.ObjectUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * The client side of the p2p connection.
 *
 * @author yerlibilgin
 */
@ChannelHandler.Sharable
public class NettyClient extends XoolaNettyHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(NettyClient.class);
  private static final int _1M = 1024 * 1024;

  private String serverHost;
  private InetSocketAddress remoteAddress;
  private int connectTimeout;
  /**
   * The Channel.
   */
  protected Channel channel;
  private String clientId;
  /**
   * The Ping timeout.
   */
  public int pingTimeout;
  /**
   * The Reconnect retry timeout.
   */
  public int reconnectRetryTimeout;

  private Bootstrap bootstrap; // (1)

  /**
   * Instantiates a new Netty client.
   *
   * @param properties   the properties
   * @param xoolaHandler the xoola handler
   */
  public NettyClient(Properties properties, XoolaInvocationHandler xoolaHandler) {
    super(properties, xoolaHandler);
    this.clientId = ObjectUtils.getOrDefault(properties.get(XoolaProperty.CLIENTID), XoolaPropertyDefaults.CLIENTID);
    this.serverHost = ObjectUtils.getOrDefault(properties.get(XoolaProperty.HOST), XoolaPropertyDefaults.HOST);
    this.pingTimeout = ObjectUtils
        .getOrDefault(properties.get(XoolaProperty.PING_TIMEOUT), XoolaPropertyDefaults.PING_TIMEOUT);
    this.reconnectRetryTimeout = ObjectUtils.getOrDefault(properties.get(XoolaProperty.RECONNECT_RETRY_TIMEOUT),
        XoolaPropertyDefaults.RECONNECT_RETRY_TIMEOUT);
    this.connectTimeout = ObjectUtils.getOrDefault(properties.get(XoolaProperty.NETWORK_RESPONSE_TIMEOUT),
        XoolaPropertyDefaults.NETWORK_RESPONSE_TIMEOUT);
    this.remoteAddress = new InetSocketAddress(this.serverHost, this.serverPort);

    EventLoopGroup workerGroup = new NioEventLoopGroup();

    bootstrap = new Bootstrap();
    bootstrap.group(workerGroup); // (2)
    bootstrap.channel(NioSocketChannel.class); // (3)
    bootstrap.option(ChannelOption.SO_KEEPALIVE, true); // (4)
    bootstrap.handler(new ChannelInitializer<SocketChannel>() {
      @Override
      public void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(new ProtocolTracker(clientId, getServerId()));
        ch.pipeline().addLast(new ObjectEncoder());
        ch.pipeline().addLast(new ObjectDecoder(101 * _1M, ClassResolvers.weakCachingConcurrentResolver(null)));
        ch.pipeline().addLast(PingPongHandler.createPinger());
        ch.pipeline().addLast(NettyClient.this);
      }
    });

    // Set the connection timeout
    bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.connectTimeout);
    bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
    bootstrap.option(ChannelOption.TCP_NODELAY, true);
  }


  @Override
  public void start() {
    doConnect();
  }

  private void doConnect() {
    LOGGER.debug("Try to connect");
    bootstrap.connect(this.remoteAddress).addListener((ChannelFutureListener) channelFuture -> {
      LOGGER.debug("Future complete");

      final Channel _channel = channelFuture.channel();
      if (channelFuture.isSuccess()) {
        this.channel = _channel;
        LOGGER.debug("Channel connection OK");
        _channel.closeFuture().addListener(new ChannelFutureListener() {
          @Override
          public void operationComplete(ChannelFuture channelFuture) {
            LOGGER.debug("Channel closed");
            scheduleReconnect(_channel);
          }
        });
      } else {
        scheduleReconnect(_channel);
      }
    });
  }

  private void scheduleReconnect(Channel _channel) {
    _channel.eventLoop().schedule(() -> {
      doConnect();
    }, 5, TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    if (this.channel != null) {
      this.channel.close();
    }
    LOGGER.warn("CLIENT - Stopped. ");
  }

  /**
   * Is available boolean.
   *
   * @return the boolean
   */
  public boolean isAvailable() {
    return this.channel != null && this.channel.isActive();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOGGER.warn(cause.getMessage(), cause);
    cause.getCause().printStackTrace();
  }

  @Override
  public void close(ChannelHandlerContext ctx, ChannelPromise promise) {
    LOGGER.warn("close called");
    this.channel = null;
    if (this.invocationHandler != null) {
      this.invocationHandler.disconnected(null);
    }
  }

  @Override
  public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) {
    LOGGER.warn("disconnect called");
    this.channel = null;
    if (this.invocationHandler != null) {
      this.invocationHandler.disconnected(null);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    LOGGER.warn("channelInactive called");
    this.channel = null;
    if (this.invocationHandler != null) {
      this.invocationHandler.disconnected(null);
    }
  }

  @Override
  public void send(String remoteName, Object message) {
    if (this.channel == null) {
      throw new XCommunicationException("Not connected to any servers");
    }

    this.channel.writeAndFlush(message);
  }

  /**
   * Gets client id.
   *
   * @return the client id
   */
  public String getClientId() {
    return clientId;
  }
}
