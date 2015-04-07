package org.interop.xoola.tcpcom.handshake;

import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.interop.xoola.tcpcom.connmanager.server.NettyServer;
import org.interop.xoola.tcpcom.connmanager.server.ServerRegistry;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

/**
 * @author <a href="mailto:bruno@factor45.org">Bruno de Carvalho</a>
 */
public class ServerHandshakeHandler extends SimpleChannelHandler {

 private static final Logger LOGGER = Logger.getLogger(ServerHandshakeHandler.class);

 private final long timeoutInMillis;
 private final NettyServer nettyServer;
 private final AtomicBoolean handshakeComplete;
 private final AtomicBoolean handshakeFailed;
 private final Object handshakeMutex = new Object();
 private final Queue < MessageEvent > messages = new ArrayDeque < MessageEvent >();
 private final CountDownLatch latch = new CountDownLatch(1);

 private String receivedClientId = "";

 public ServerHandshakeHandler(NettyServer nettyHandler, long timeoutInMillis) {
  this.nettyServer = nettyHandler;
  this.timeoutInMillis = timeoutInMillis;
  this.handshakeComplete = new AtomicBoolean(false);
  this.handshakeFailed = new AtomicBoolean(false);
 }

 // SimpleChannelHandler ---------------------------------------------------
 @Override
 public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
  if (this.handshakeFailed.get()) {
   // Bail out fast if handshake already failed
   return;
  }

  if (this.handshakeComplete.get()) {
   // If handshake succeeded but message still came through this
   // handler, then immediately send it upwards.
   super.messageReceived(ctx, e);
   return;
  }

  synchronized (this.handshakeMutex) {
   // Recheck conditions after locking the mutex.
   // Things might have changed while waiting for the lock.
   if (this.handshakeFailed.get()) {
    return;
   }

   if (this.handshakeComplete.get()) {
    super.messageReceived(ctx, e);
    return;
   }
   String errorMessage = null;
   if (e.getMessage() instanceof HandshakeMessage) {
    receivedClientId = ((HandshakeMessage) e.getMessage()).message;

    LOGGER.debug("Received client id: " + receivedClientId);

    ServerRegistry registry = nettyServer.getServerRegistry();
    if (!registry.isAllowed(receivedClientId)) {
     errorMessage = "The client id is not in the registry";
    //} else if (registry.hasUser(receivedClientId)) {
   //  errorMessage = "Handshake failed: '" + receivedClientId + "' is already connected";
    } else {
     challengeSuccess(ctx, receivedClientId);
     return;
    }
   }
   this.fireHandshakeFailed(ctx, errorMessage);
  }
 }

 private void challengeSuccess(ChannelHandlerContext ctx, String receivedClientId) {
  LOGGER.debug("Challenge validated, flushing messages & removing handshake handler from  pipeline.");
  this.writeDownstream(ctx, nettyServer.getServerId());

  LOGGER.debug(this.messages.size() + " messages in queue to be flushed.");
  for (MessageEvent message : this.messages) {
   ctx.sendDownstream(message);
  }

  LOGGER.debug("Removing handshake handler from pipeline.");
  ctx.getPipeline().remove(this);
  LOGGER.debug("this.fireHandshakeSucceeded(ctx);");

  this.fireHandshakeSucceeded(receivedClientId, ctx);
 }

 @Override
 public void channelConnected(final ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
  LOGGER.debug("Incoming connection established from: " + e.getChannel().getRemoteAddress());

  LOGGER.debug("Handshake timeout: " + timeoutInMillis);
  new Thread() {
   @Override
   public void run() {
    try {
     ServerHandshakeHandler.this.latch.await(ServerHandshakeHandler.this.timeoutInMillis, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
     LOGGER.debug("Handshake timeout checker: interrupted!");
     e.printStackTrace();
    }

    if (ServerHandshakeHandler.this.handshakeFailed.get()) {
     LOGGER.debug("Handshake timeout checker: discarded (handshake failed)");
     return;
    }

    if (ServerHandshakeHandler.this.handshakeComplete.get()) {
     LOGGER.debug("Handshake timeout checker: discarded (handshake complete)");
     return;
    }

    synchronized (ServerHandshakeHandler.this.handshakeMutex) {
     if (ServerHandshakeHandler.this.handshakeFailed.get()) {
      LOGGER.debug("Handshake timeout checker: already failed.");
      return;
     }

     if (!ServerHandshakeHandler.this.handshakeComplete.get()) {
      LOGGER.debug("Handshake timeout checker: timed out, killing connection.");
      ServerHandshakeHandler.this.handshakeFailed.set(true);
      ctx.getChannel().close();
     } else {
      LOGGER.debug("Handshake timeout checker: discarded (handshake OK)");
     }
    }
   }
  }.start();
 }

 @Override
 public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
  LOGGER.debug("Channel closed.");
  if (!this.handshakeComplete.get()) {
   this.fireHandshakeFailed(ctx, "Channel closed before handshake");
  }
 }

 @Override
 public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
  LOGGER.debug("Exception caught.");
  e.getCause().printStackTrace();
  if (e.getChannel().isConnected()) {
   // Closing the channel will trigger handshake failure.
   e.getChannel().close();
  } else {
   // Channel didn't open, so we must fire handshake failure directly.
   this.fireHandshakeFailed(ctx, "Channel couldn't be opened");
  }
 }

 @Override
 public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
  // Before doing anything, ensure that noone else is working by
  // acquiring a lock on the handshakeMutex.
  synchronized (this.handshakeMutex) {
   if (this.handshakeFailed.get()) {
    // If the handshake failed meanwhile, discard any messages.
    return;
   }

   // If the handshake hasn't failed but completed meanwhile and
   // messages still passed through this handler, then forward
   // them downwards.
   if (this.handshakeComplete.get()) {
    LOGGER.debug("Handshake already completed, not appending '" + e.getMessage().toString().trim() + "' to queue!");
    super.writeRequested(ctx, e);
   } else {
    // Otherwise, queue messages in order until the handshake
    // completes.
    this.messages.offer(e);
   }
  }
 }

 private void writeDownstream(ChannelHandlerContext ctx, String handshakeData) {
  ChannelFuture f = Channels.succeededFuture(ctx.getChannel());
  SocketAddress address = ctx.getChannel().getRemoteAddress();
  Channel c = ctx.getChannel();

  ctx.sendDownstream(new DownstreamMessageEvent(c, f, new HandshakeMessage(handshakeData), address));
 }

 private void fireHandshakeFailed(ChannelHandlerContext ctx, String message) {
   LOGGER.error("Handshake failed, " + message);
  this.handshakeComplete.set(true);
  this.handshakeFailed.set(true);
  this.latch.countDown();
  ctx.getChannel().close();
 }

 private void fireHandshakeSucceeded(final String receivedClientId, final ChannelHandlerContext ctx) {
  this.handshakeComplete.set(true);
  this.handshakeFailed.set(false);
  this.latch.countDown();
  nettyServer.addClient(receivedClientId, ctx.getChannel());
  if (this.nettyServer.invocationHandler != null) {
   new Thread(new Runnable() {
    @Override
    public void run() {
     nettyServer.invocationHandler.connected(receivedClientId); //
    }
   }).start();
  } else {
   LOGGER.debug("Malesef incovation handler null");
  }
 }
}
