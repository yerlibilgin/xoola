package org.interop.xoola.tcpcom.connmanager.client;

import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.interop.xoola.core.XoolaInvocationHandler;
import org.interop.xoola.core.XoolaProperty;
import org.interop.xoola.exception.XCommunicationException;
import org.interop.xoola.tcpcom.connmanager.ChannelGuard;
import org.interop.xoola.tcpcom.connmanager.XoolaNettyHandler;
import org.interop.xoola.tcpcom.handshake.ClientHandshakeHandler;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ClassResolvers;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;

/**
 * @author dogan, muhammet
 */
public class NettyClient extends XoolaNettyHandler {
 private static final Logger LOGGER = Logger.getLogger(NettyClient.class);
 private ClientBootstrap bootstrap;

 private String serverHost;
 private InetSocketAddress remoteAddress;
 private Long connectTimeout;
 private ChannelGuard channelGuard;
 protected Channel channel;
 private String clientId;

 public NettyClient(Properties properties, XoolaInvocationHandler xoolaHandler) {
  super(properties, xoolaHandler);
  this.clientId = (String) properties.get(XoolaProperty.CLIENTID);
  this.serverHost = (String) properties.get(XoolaProperty.HOST);
  this.connectTimeout = Long.parseLong(properties.get(XoolaProperty.NETWORK_RESPONSE_TIMEOUT).toString());
  this.remoteAddress = new InetSocketAddress(this.serverHost, this.serverPort);
 }

 @Override
 public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
  super.messageReceived(ctx, e);
 }

 @Override
 public void start() {
  this.createMainClient();
 }

 private void createMainClient() {
  // Standard netty bootstrapping stuff.
  Executor bossPool = Executors.newFixedThreadPool(120);
  Executor workerPool = Executors.newFixedThreadPool(120);
  ChannelFactory factory = new NioClientSocketChannelFactory(bossPool, workerPool);
  this.bootstrap = new ClientBootstrap(factory);
  channelGuard = new ChannelGuard(pingTimeout, bootstrap, remoteAddress);
  // Pipeline
  this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
   @Override
   public ChannelPipeline getPipeline() throws Exception {
    final ClientHandshakeHandler handshakeHandler = new ClientHandshakeHandler(NettyClient.this, connectTimeout);
    return Channels.pipeline(new ObjectEncoder(), new ObjectDecoder(ClassResolvers.weakCachingConcurrentResolver(null)), channelGuard,
      handshakeHandler, NettyClient.this);
   }
  });

  // Set the connection timeout
  this.bootstrap.setOption("connectTimeoutMillis", this.connectTimeout);
  this.bootstrap.setOption("keepAlive", true);
  this.bootstrap.setOption("tcpNoDelay", true);
  this.bootstrap.connect(this.remoteAddress);
 }

 @Override
 public void stop() {
  try {
   this.channelGuard.kill();
  } catch (Exception ex) {
  }
  if (this.channel != null) {
   this.channel.close().awaitUninterruptibly();
  }
  try {
   this.bootstrap.releaseExternalResources();
  } catch (Exception ex) {
  }
  LOGGER.info("CLIENT - Stopped.");
 }

 public boolean isAvailable() {
  return this.channel != null && this.channel.isConnected();
 }

 @Override
 public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
  LOGGER.warn("Exception " + e.getCause().getMessage());
  LOGGER.error(e.getCause().getCause(), e.getCause());
  e.getCause().printStackTrace();
 }

 @Override
 public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
  try {
   ctx.getPipeline().remove(this);
  } catch (Exception ex) {
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
  *          the channel to set
  */
 public void setChannel(Channel channel) {
  this.channel = channel;
 }

 @Override
 public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
  LOGGER.debug("Channel disconnected");
  try {
   ctx.getPipeline().remove(this);
  } catch (Exception ex) {
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
  this.channel.write(message);
 }

 public String getClientId() {
  return clientId;
 }
}
