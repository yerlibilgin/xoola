package org.interop.xoola.tcpcom.connmanager.server;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.interop.xoola.core.XoolaInvocationHandler;
import org.interop.xoola.core.XoolaProperty;
import org.interop.xoola.exception.XIOException;
import org.interop.xoola.tcpcom.connmanager.ChannelGuard;
import org.interop.xoola.tcpcom.connmanager.XoolaNettyHandler;
import org.interop.xoola.tcpcom.handshake.ServerHandshakeHandler;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ClassResolvers;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;

public class NettyServer extends XoolaNettyHandler {
 private static final Logger LOGGER = Logger.getLogger(NettyServer.class);
 private ServerBootstrap bootstrap;
 private Channel acceptor;
 private ServerRegistry serverRegistry;
 private IClassLoaderProvider provider;

 // private final ServerListener listener;
 public NettyServer(Properties properties, XoolaInvocationHandler xoolaHandler) {
  super(properties, xoolaHandler);

  String classLoaderProviderClassName = properties.getProperty(XoolaProperty.CLASS_LOADER_PROVIDER_CLASS);
  if (classLoaderProviderClassName != null){
   try {
    this.provider = (IClassLoaderProvider) Thread.currentThread().getContextClassLoader().loadClass(classLoaderProviderClassName).newInstance();
   } catch (Exception e) {
    LOGGER.error(e.getMessage(), e);
    provider = new IClassLoaderProvider() {
     @Override
     public ClassLoader getClassLoader() {
      return Thread.currentThread().getContextClassLoader();
     }
    };
   }
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
  Executor bossPool = Executors.newCachedThreadPool();
  Executor workerPool = Executors.newCachedThreadPool();
  ChannelFactory factory = new NioServerSocketChannelFactory(bossPool, workerPool);
  this.bootstrap = new ServerBootstrap(factory);
  final ChannelGuard channelGuard = new ChannelGuard(pingTimeout);

  this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
   @Override
   public ChannelPipeline getPipeline() throws Exception {
    return Channels.pipeline(new ObjectEncoder(), new ObjectDecoder(ClassResolvers.weakCachingConcurrentResolver(provider.getClassLoader())),
      new ServerHandshakeHandler(NettyServer.this, responseTimeout), channelGuard, NettyServer.this);
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
   this.bootstrap.releaseExternalResources();
   return false;
  }
 }

 @Override
 public void stop() {
  this.bootstrap.releaseExternalResources();
  this.acceptor.close();
  serverRegistry.clear();
  LOGGER.info("Server Stopped.");
 }

 @Override
 public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
  LOGGER.error(e.getCause().getMessage(), e.getCause());
 }

 @Override
 public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
  LOGGER.error("Channel closed");
 }

 @Override
 public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
  Channel channel = ctx.getChannel();
  if (this.invocationHandler != null && serverRegistry.hasChannel(channel)) {
   String remoteId = serverRegistry.getUser(channel);
   serverRegistry.removeUser(remoteId);
   this.invocationHandler.disconnected(remoteId);
  }
 }

 @Override
 public void send(String remoteName, Object message) {
  if (serverRegistry.hasUser(remoteName)) {
   serverRegistry.getChannel(remoteName).write(message);
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
}
