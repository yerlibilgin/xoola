package org.interop.xoola.tcpcom.handshake;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.interop.xoola.tcpcom.connmanager.client.NettyClient;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
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
public class ClientHandshakeHandler extends SimpleChannelHandler {

  private static final Logger LOGGER = Logger.getLogger(ClientHandshakeHandler.class);

  private final long timeoutInMillis;
  private final AtomicBoolean handshakeComplete;
  private final AtomicBoolean handshakeFailed;
  private final CountDownLatch latch = new CountDownLatch(1);
  private final Queue<MessageEvent> messages = new ArrayDeque<MessageEvent>();
  private final Object handshakeMutex = new Object();

  private NettyClient nettyClient;

  public ClientHandshakeHandler(NettyClient nettyClient, long timeoutInMillis) {
    this.nettyClient = nettyClient;
    this.timeoutInMillis = timeoutInMillis;
    this.handshakeComplete = new AtomicBoolean(false);
    this.handshakeFailed = new AtomicBoolean(false);
  }

  // SimpleChannelHandler ---------------------------------------------------
  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    if (this.handshakeFailed.get()) {
      // Bail LOGGER.info fast if handshake already failed
      return;
    }

    if (this.handshakeComplete.get()) {
      // If handshake succeeded but message still came through this
      // handler, then immediately send it upwards.
      // Chances are it's the last time a message passes through
      // this handler...
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

      // Parse the challenge.
      String recvId = ((HandshakeMessage) e.getMessage()).message;
      String expectedServerId = nettyClient.getServerId();
      if (!expectedServerId.equals(recvId)) {
        LOGGER.info("Handshake failed: expected remote id is " + expectedServerId + " but received '" + recvId + "'");
        this.fireHandshakeFailed(ctx);
        return;
      }

      LOGGER.info("Flush messages.");
      for (MessageEvent message : this.messages) {
        ctx.sendDownstream(message);
      }

      LOGGER.info("Handshake ok. Removing handshake handler from pipeline.");
      ctx.getPipeline().remove(this);

      LOGGER.debug("this.fireHandshakeSucceeded(ctx);");
      this.fireHandshakeSucceeded(ctx);
    }
  }

  @Override
  public void channelConnected(final ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    LOGGER.info("Outgoing connection established to: " + e.getChannel().getRemoteAddress());
    // Write the handshake & add a timeout listener.
    ChannelFuture f = Channels.future(ctx.getChannel());
    f.addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        // Once this message is sent, start the timeout checker.
        new Thread() {
          @Override
          public void run() {
            // Wait until either handshake completes (releases the
            // latch) or this latch times out.
            try {
              ClientHandshakeHandler.this.latch.await(ClientHandshakeHandler.this.timeoutInMillis,
                  TimeUnit.MILLISECONDS);
            } catch (InterruptedException e1) {
              LOGGER.info("Handshake timeout checker: " + "interrupted!");
            }

            // Informative output, do nothing...
            if (ClientHandshakeHandler.this.handshakeFailed.get()) {
              LOGGER.info("Handshake " + "timeout checker: discarded " + "(handshake failed)");
              return;
            }

            // More informative output, do nothing...
            if (ClientHandshakeHandler.this.handshakeComplete.get()) {
              LOGGER.info("Handshake " + "timeout checker: discarded" + "(handshake completed)");
              return;
            }

            // Handshake has neither failed nor completed, time
            // to do something! (trigger failure).
            // Lock on the mutex first...
            synchronized (ClientHandshakeHandler.this.handshakeMutex) {
              // Same checks as before, conditions might have
              // changed while waiting to get a lock on the
              // mutex.
              if (ClientHandshakeHandler.this.handshakeFailed.get()) {
                LOGGER.info("Handshake timeout checker: already failed.");
                return;
              }

              if (!ClientHandshakeHandler.this.handshakeComplete.get()) {
                // If handshake wasn't completed meanwhile,
                // time to mark the handshake as having failed.
                LOGGER.info("Handshake timeout checker: timed LOGGER.info, killing connection.");
                ClientHandshakeHandler.this.fireHandshakeFailed(ctx);
              } else {
                // Informative output; the handshake was
                // completed while this thread was waiting
                // for a lock on the handshakeMutex.
                // Do nothing...
                LOGGER.info("Handshake timeout checker: discarded  (handshake OK)");
              }
            }
          }
        }.start();
      }
    });

    Channel c = ctx.getChannel();
    // kendi id'ni server'a yolla.
    ctx.sendDownstream(new DownstreamMessageEvent(c, f, new HandshakeMessage(nettyClient.getClientId()), null));
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
        LOGGER.info("Handshake already completed, not " + "appending '" + e.getMessage().toString().trim()
            + "' to queue!");
        super.writeRequested(ctx, e);
      } else {
        // Otherwise, queue messages in order until the handshake
        // completes.
        this.messages.offer(e);
      }
    }
  }

  private void fireHandshakeFailed(ChannelHandlerContext ctx) {
    this.handshakeComplete.set(true);
    this.handshakeFailed.set(true);
    this.latch.countDown();
    ctx.getChannel().close();
  }

  private void fireHandshakeSucceeded(final ChannelHandlerContext ctx) {
    this.handshakeComplete.set(true);
    this.handshakeFailed.set(false);
    this.latch.countDown();
    ClientHandshakeHandler.this.nettyClient.setChannel(ctx.getChannel());

    if (this.nettyClient.invocationHandler != null) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            Thread.sleep(100);
          } catch (Exception ex) {
          }

          ClientHandshakeHandler.this.nettyClient.invocationHandler.connected(null);
        }
      }).start();
    } else {
      LOGGER.debug("Malesef incovation handler null");
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
    e.getCause().printStackTrace();
  }
}
