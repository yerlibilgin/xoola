package org.interop.xoola.tcpcom.connmanager.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.apache.log4j.Logger;
import org.interop.xoola.core.XoolaInvocationHandler;
import org.interop.xoola.core.XoolaProperty;
import org.interop.xoola.exception.XIOException;
import org.interop.xoola.tcpcom.connmanager.ChannelGuard;
import org.interop.xoola.tcpcom.connmanager.XoolaNettyHandler;
import org.interop.xoola.tcpcom.handshake.ServerHandshakeHandler;

import java.net.InetSocketAddress;
import java.util.Properties;

@ChannelHandler.Sharable
public class NettyServer extends XoolaNettyHandler {
  private static final Logger LOGGER = Logger.getLogger(NettyServer.class);
  private static final int _1M = 1024*1024;
  private ServerBootstrap bootstrap;
  private ChannelFuture acceptor;
  private ServerRegistry serverRegistry;
  private IClassLoaderProvider provider;
 private  EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
  EventLoopGroup workerGroup = new NioEventLoopGroup();

  // private final ServerListener listener;
  public NettyServer(Properties properties, XoolaInvocationHandler xoolaHandler) {
    super(properties, xoolaHandler);

    String classLoaderProviderClassName = properties.getProperty(XoolaProperty.CLASS_LOADER_PROVIDER_CLASS);
    if (classLoaderProviderClassName != null) {
      try {
        this.provider = (IClassLoaderProvider) Thread.currentThread().getContextClassLoader().loadClass(classLoaderProviderClassName).newInstance();
      } catch (Exception e) {
        LOGGER.error(e.getMessage(), e);
      }
    }
    if (provider == null) {
      provider = new IClassLoaderProvider() {
        @Override
        public ClassLoader getClassLoader() {
          return Thread.currentThread().getContextClassLoader();
        }
      };
    }
    serverRegistry = new ServerRegistry(properties);
  }

  public void start() {
    this.createMainServer();
  }

  /**
   * @return
   */
  private boolean createMainServer() {
    this.bootstrap = new ServerBootstrap();
    bootstrap.group(bossGroup, workerGroup);
    bootstrap.channel(NioServerSocketChannel.class);

    bootstrap.childHandler(new ChannelInitializer<SocketChannel>() { // (4)
      @Override
      public void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new ObjectEncoder());
        ch.pipeline().addLast(new ObjectDecoder(101*_1M, ClassResolvers.weakCachingConcurrentResolver(provider.getClassLoader())));
        ch.pipeline().addLast(new ChannelGuard());
        ch.pipeline().addLast(new ServerHandshakeHandler(NettyServer.this, handshakeTimeout));
        ch.pipeline().addLast(NettyServer.this);
      }
    });

    // Create channel
    try {
      this.acceptor = this.bootstrap.bind(new InetSocketAddress(this.serverPort));
      LOGGER.info("Server bound to *:" + this.serverPort + " " + this.acceptor);
      return true;
    } catch (ChannelException ex) {
      ex.printStackTrace();
      LOGGER.error("Failed to bind to *:" + this.serverPort);
      return false;
    }
  }

  @Override
  public void stop() {
    this.acceptor.channel().close();
    serverRegistry.clear();
    workerGroup.shutdownGracefully();
    bossGroup.shutdownGracefully();
    LOGGER.info("Server Stopped.");
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
    LOGGER.error(e.getMessage(), e);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    LOGGER.error("Channel inactive");

    notifyClientDisconnect(ctx);
  }

  @Override
  public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
    notifyClientDisconnect(ctx);
  }

  @Override
  public void send(String remoteName, Object message) {
    if (serverRegistry.hasUser(remoteName)) {
      serverRegistry.getChannel(remoteName).writeAndFlush(message);
      return;
    }

    throw new XIOException("Remote client not connected");
  }

  public void addClient(String receivedClientId, Channel channel) {
    serverRegistry.addUser(receivedClientId, channel);
  }

  public ServerRegistry getServerRegistry() {
    return serverRegistry;
  }

  public void setServerRegistry(ServerRegistry serverRegistry) {
    this.serverRegistry = serverRegistry;
  }

  private void notifyClientDisconnect(ChannelHandlerContext ctx) {
    Channel channel = ctx.channel();
    if (this.invocationHandler != null && serverRegistry.hasChannel(channel)) {
      String remoteId = serverRegistry.getUser(channel);
      serverRegistry.removeUser(remoteId);
      this.invocationHandler.disconnected(remoteId);
    }
  }
}
