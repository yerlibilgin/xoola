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
package gov.tubitak.xoola.internal.tcpcom.handshake;

import gov.tubitak.xoola.internal.tcpcom.connmanager.client.NettyClient;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Original author: <a href="mailto:bruno@factor45.org">Bruno de Carvalho</a>
 * <p>
 * modifier by: yerlibilgin
 */
public class ClientHandshakeHandler extends ChannelDuplexHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandshakeHandler.class);

  private final long timeoutInMillis;
  private final AtomicBoolean handshakeComplete;
  private final AtomicBoolean handshakeFailed;
  private final CountDownLatch latch = new CountDownLatch(1);
  private final Queue<Object> messages = new ArrayDeque<Object>();
  private final Object handshakeMutex = new Object();

  private final NettyClient nettyClient;

  /**
   * Instantiates a new Client handshake handler.
   *
   * @param nettyClient     the netty client
   * @param timeoutInMillis the timeout in millis
   */
  public ClientHandshakeHandler(NettyClient nettyClient, long timeoutInMillis) {
    this.nettyClient = nettyClient;
    this.timeoutInMillis = timeoutInMillis;
    this.handshakeComplete = new AtomicBoolean(false);
    this.handshakeFailed = new AtomicBoolean(false);
  }

  // SimpleChannelHandler ---------------------------------------------------
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
    if (this.handshakeFailed.get()) {
      // Bail LOGGER.info fast if handshake already failed
      return;
    }

    if (this.handshakeComplete.get()) {
      // If handshake succeeded but message still came through this
      // handler, then immediately send it upwards.
      // Chances are it's the last time a message passes through
      // this handler...
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

      // Parse the challenge.
      String recvId = ((HandshakeMessage) message).message;
      String expectedServerId = nettyClient.getServerId();
      if (!expectedServerId.equals(recvId)) {
        LOGGER.info("Handshake failed: expected remote id is " + expectedServerId + " but received '" + recvId + "'");
        this.fireHandshakeFailed(ctx);
        return;
      }

      LOGGER.info("Handshake ok. Removing handshake handler from pipeline.");
      ctx.pipeline().remove(this);

      LOGGER.info("Flush messages.");
      for (Object msg : this.messages) {
        ctx.writeAndFlush(msg);
      }

      LOGGER.debug("this.fireHandshakeSucceeded(ctx);");
      this.fireHandshakeSucceeded(ctx);
    }
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) throws Exception {
    LOGGER.info("Outgoing connection established to: " + ctx.channel().remoteAddress());
    // Write the handshake & add a timeout listener.
    ctx.channel().newSucceededFuture().addListener(new ChannelFutureListener() {
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
                LOGGER.info("Handshake timeout checker: timed out, killing connection.");
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
    // kendi id'ni server'a yolla.
    ctx.channel().writeAndFlush(new HandshakeMessage(nettyClient.getClientId()));
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    // Before doing anything, ensure that noone else is working by
    // acquiring a lock on the handshakeMutex.
    synchronized (this.handshakeMutex) {
      //if this is a handshake message, let it go
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
        LOGGER.info("Handshake already completed, not " + "appending '" + msg.toString().trim()
            + "' to queue!");
        super.write(ctx, msg, promise);
      } else {
        // Otherwise, queue messages in order until the handshake
        // completes.
        this.messages.offer(msg);
      }
    }
  }

  private void fireHandshakeFailed(ChannelHandlerContext ctx) {
    this.handshakeComplete.set(true);
    this.handshakeFailed.set(true);
    this.latch.countDown();
    ctx.close();
  }

  private void fireHandshakeSucceeded(final ChannelHandlerContext ctx) {
    this.handshakeComplete.set(true);
    this.handshakeFailed.set(false);
    this.latch.countDown();
    ClientHandshakeHandler.this.nettyClient.setChannel(ctx.channel());

    if (this.nettyClient.invocationHandler != null) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            Thread.sleep(100);
          } catch (Exception ignored) {
          }

          ClientHandshakeHandler.this.nettyClient.invocationHandler.connected(nettyClient.getServerId());
        }
      }).start();
    } else {
      LOGGER.debug("Malesef incovation handler null");
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
    e.printStackTrace();
  }
}
