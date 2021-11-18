package gov.tubitak.xoola.tcpcom.connmanager.client;

import gov.tubitak.xoola.core.XoolaInvocationHandler;
import gov.tubitak.xoola.core.XoolaProperty;
import gov.tubitak.xoola.exception.XCommunicationException;
import gov.tubitak.xoola.tcpcom.handshake.ClientHandshakeHandler;
import gov.tubitak.xoola.util.ObjectUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import java.net.InetSocketAddress;
import java.util.Properties;
import org.slf4j.Logger;
import gov.tubitak.xoola.core.XoolaInvocationHandler;
import gov.tubitak.xoola.core.XoolaProperty;
import gov.tubitak.xoola.core.XoolaPropertyDefaults;
import gov.tubitak.xoola.exception.XCommunicationException;
import gov.tubitak.xoola.tcpcom.connmanager.ChannelGuard;
import gov.tubitak.xoola.tcpcom.connmanager.XoolaNettyHandler;
import gov.tubitak.xoola.tcpcom.handshake.ClientHandshakeHandler;
import gov.tubitak.xoola.util.ObjectUtils;
import org.slf4j.LoggerFactory;

/**
 * @author dogan, muhammet
 */
@ChannelHandler.Sharable
public class NettyClient extends XoolaNettyHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(NettyClient.class);
  private static final int _1M = 1024 * 1024;

  private String serverHost;
  private InetSocketAddress remoteAddress;
  private int connectTimeout;
  private ChannelGuard channelGuard;
  protected Channel channel;
  private String clientId;
  public int pingTimeout;
  public int reconnectRetryTimeout;

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
    Channel channel = bootstrap.connect(this.remoteAddress).awaitUninterruptibly().channel();
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
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOGGER.warn(cause.getMessage(), cause);
    cause.getCause().printStackTrace();
  }

  @Override
  public void close(ChannelHandlerContext ctx, ChannelPromise promise) {
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
   * @param channel
   *     the channel to set
   */
  public void setChannel(Channel channel) {
    this.channel = channel;
    this.channel.closeFuture().addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        LOGGER.debug("Connection lost");
      }
    });
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    super.channelInactive(ctx);
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
