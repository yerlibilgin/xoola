package org.interop.xoola.tcpcom.connmanager.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.apache.log4j.Logger;
import org.interop.xoola.core.XoolaInvocationHandler;
import org.interop.xoola.core.XoolaProperty;
import org.interop.xoola.exception.XCommunicationException;
import org.interop.xoola.tcpcom.connmanager.ChannelGuard;
import org.interop.xoola.tcpcom.connmanager.XoolaNettyHandler;
import org.interop.xoola.tcpcom.handshake.ClientHandshakeHandler;

import java.net.InetSocketAddress;
import java.util.Properties;

/**
 * @author dogan, muhammet
 */
@ChannelHandler.Sharable
public class NettyClient extends XoolaNettyHandler {
  private static final Logger LOGGER = Logger.getLogger(NettyClient.class);
  private static final int _1M = 1024 * 1024;

  private String serverHost;
  private InetSocketAddress remoteAddress;
  private Integer connectTimeout;
  private ChannelGuard channelGuard;
  protected Channel channel;
  private String clientId;
  public int pingTimeout;
  public int reconnectRetryTimeout;

  public NettyClient(Properties properties, XoolaInvocationHandler xoolaHandler) {
    super(properties, xoolaHandler);
    this.clientId = (String) properties.get(XoolaProperty.CLIENTID);
    this.serverHost = (String) properties.get(XoolaProperty.HOST);
    this.pingTimeout = Integer.parseInt(properties.getProperty(XoolaProperty.PING_TIMEOUT, "15000"));
    this.reconnectRetryTimeout = Integer.parseInt(properties.getProperty(XoolaProperty.RECONNECT_RETRY_TIMEOUT, "" + pingTimeout));
    this.connectTimeout = Integer.parseInt(properties.getProperty(XoolaProperty.NETWORK_RESPONSE_TIMEOUT, "50000"));
    this.remoteAddress = new InetSocketAddress(this.serverHost, this.serverPort);
  }

  @Override
  public void start() {
    this.createMainClient();
  }

  private void createMainClient() {
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    Bootstrap bootstrap = new Bootstrap(); // (1)
    bootstrap.group(workerGroup); // (2)
    bootstrap.channel(NioSocketChannel.class); // (3)
    bootstrap.option(ChannelOption.SO_KEEPALIVE, true); // (4)

    channelGuard = new ChannelGuard(pingTimeout, reconnectRetryTimeout, bootstrap, remoteAddress);

    bootstrap.handler(new ChannelInitializer<SocketChannel>() {
      @Override
      public void initChannel(SocketChannel ch) throws Exception {
        final ClientHandshakeHandler handshakeHandler = new ClientHandshakeHandler(NettyClient.this, handshakeTimeout);
        ch.pipeline().addLast(new ObjectEncoder());
        ch.pipeline().addLast(new ObjectDecoder(101 * _1M, ClassResolvers.weakCachingConcurrentResolver(null)));
        ch.pipeline().addLast(channelGuard);
        ch.pipeline().addLast(handshakeHandler);
        ch.pipeline().addLast(NettyClient.this);
      }
    });

    // Set the connection timeout
    bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.connectTimeout);
    bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
    bootstrap.option(ChannelOption.TCP_NODELAY, true);
    bootstrap.connect(this.remoteAddress);
  }

  @Override
  public void stop() {
    try {
      this.channelGuard.kill();
    } catch (Exception ignored) {
    }
    if (this.channel != null) {
      this.channel.close();
    }
    LOGGER.info("CLIENT - Stopped.");
  }

  public boolean isAvailable() {
    return this.channel != null && this.channel.isActive();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    LOGGER.warn("Exception " + cause.getCause().getMessage());
    LOGGER.error(cause.getCause().getCause(), cause.getCause());
    cause.getCause().printStackTrace();
  }

  @Override
  public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
    try {
      ctx.pipeline().remove(this);
    } catch (Exception ignored) {
    }
  }

  /**
   * @return the channel
   */
  public Channel getChannel() {
    return this.channel;
  }

  /**
   * @param channel the channel to set
   */
  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  @Override
  public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
    LOGGER.debug("Channel disconnected");
    try {
      ctx.pipeline().remove(this);
    } catch (Exception ignored) {
    }

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

  public String getClientId() {
    return clientId;
  }
}
