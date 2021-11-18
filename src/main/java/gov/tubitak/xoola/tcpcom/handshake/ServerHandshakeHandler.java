package gov.tubitak.xoola.tcpcom.handshake;

import gov.tubitak.xoola.tcpcom.connmanager.server.NettyServer;
import gov.tubitak.xoola.tcpcom.connmanager.server.ServerRegistry;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import gov.tubitak.xoola.tcpcom.connmanager.server.NettyServer;
import gov.tubitak.xoola.tcpcom.connmanager.server.ServerRegistry;
import org.slf4j.LoggerFactory;


/**
 * original author: <a href="mailto:bruno@factor45.org">Bruno de Carvalho</a>
 * modifier by: myildiz
 */
public class ServerHandshakeHandler extends ChannelDuplexHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerHandshakeHandler.class);

  private final long timeoutInMillis;
  private final NettyServer nettyServer;
  private final AtomicBoolean handshakeComplete;
  private final AtomicBoolean handshakeFailed;
  private final Object handshakeMutex = new Object();
  private final Queue<Object> messages = new ArrayDeque<Object>();
  private final CountDownLatch latch = new CountDownLatch(1);

  private String receivedClientId = "";

  public ServerHandshakeHandler(NettyServer nettyHandler, long timeoutInMillis) {
    this.nettyServer = nettyHandler;
    this.timeoutInMillis = timeoutInMillis;
    this.handshakeComplete = new AtomicBoolean(false);
    this.handshakeFailed = new AtomicBoolean(false);
  }

  // SimpleChannelHandler ---------------------------------------------------

  //@Override

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
    if (this.handshakeFailed.get()) {
      // Bail out fast if handshake already failed
      return;
    }

    if (this.handshakeComplete.get()) {
      // If handshake succeeded but message still came through this
      // handler, then immediately send it upwards.
      super.channelRead(ctx, message);
      return;
    }

    synchronized (this.handshakeMutex) {
      // Recheck conditions after locking the mutex.
      // Things might have changed while waiting for the lock.
      if (this.handshakeFailed.get()) {
        return;
      }

      if (this.handshakeComplete.get()) {
        super.channelRead(ctx, message);
        return;
      }
      String errorMessage = null;
      if (message instanceof HandshakeMessage) {
        receivedClientId = ((HandshakeMessage) message).message;

        LOGGER.debug("Received client id: " + receivedClientId);

        ServerRegistry registry = nettyServer.getServerRegistry();
        if (!registry.isAllowed(receivedClientId)) {
          errorMessage = "The client id is not in the registry";
          } else if (registry.hasUser(receivedClientId)) {
            errorMessage = "Handshake failed: '" + receivedClientId + "' is already connected";
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
    ctx.channel().writeAndFlush(new HandshakeMessage(nettyServer.getServerId()));

    LOGGER.debug("Removing server handshake handler from pipeline.");
    ctx.pipeline().remove(this);

    LOGGER.debug(this.messages.size() + " messages in queue to be flushed.");
    for (Object message : this.messages) {
      ctx.writeAndFlush(message);
    }

    LOGGER.debug("this.fireHandshakeSucceeded(ctx);");

    this.fireHandshakeSucceeded(receivedClientId, ctx);
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) throws Exception {
    LOGGER.debug("Incoming connection established from: " + ctx.channel().remoteAddress());

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
            ctx.channel().close();
          } else {
            LOGGER.debug("Handshake timeout checker: discarded (handshake OK)");
          }
        }
      }
    }.start();
  }

  @Override
  public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
    LOGGER.debug("Channel closed.");
    if (!this.handshakeComplete.get()) {
      this.fireHandshakeFailed(ctx, "Channel closed before handshake");
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
    LOGGER.debug("Exception caught.");
    e.getCause().printStackTrace();
    if (ctx.channel().isActive()) {
      // Closing the channel will trigger handshake failure.
      ctx.channel().close();
    } else {
      // Channel didn't open, so we must fire handshake failure directly.
      this.fireHandshakeFailed(ctx, "Channel couldn't be opened");
    }
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    // Before doing anything, ensure that noone else is working by
    // acquiring a lock on the handshakeMutex.
    synchronized (this.handshakeMutex) {
      if (msg instanceof HandshakeMessage) {
        super.write(ctx, msg, promise);
        return;
      }

      if (this.handshakeFailed.get()) {
        // If the handshake failed meanwhile, discard any messages.
        return;
      }

      // If the handshake hasn't failed but completed meanwhile and
      // messages still passed through this handler, then forward
      // them downwards.
      if (this.handshakeComplete.get()) {
        LOGGER.debug("Handshake already completed, not appending '" + msg.toString().trim() + "' to queue!");
        super.write(ctx, msg, promise);
      } else {
        // Otherwise, queue messages in order until the handshake
        // completes.
        this.messages.offer(msg);
      }
    }
  }

  private void fireHandshakeFailed(ChannelHandlerContext ctx, String message) {
    LOGGER.error("Handshake failed, " + message);
    this.handshakeComplete.set(true);
    this.handshakeFailed.set(true);
    this.latch.countDown();
    ctx.channel().close();
  }

  private void fireHandshakeSucceeded(final String receivedClientId, final ChannelHandlerContext ctx) {
    this.handshakeComplete.set(true);
    this.handshakeFailed.set(false);
    this.latch.countDown();
    nettyServer.addClient(receivedClientId, ctx.channel());
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
