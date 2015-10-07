package org.interop.xoola.tcpcom.connmanager;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import org.apache.log4j.Logger;
import org.interop.xoola.tcpcom.connmanager.client.PingPong;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

@ChannelHandler.Sharable
public class ChannelGuard extends ChannelHandlerAdapter {
  private static final Logger LOGGER = Logger.getLogger(ChannelGuard.class);
  private Timer timer;

  AtomicBoolean connectedFlag = new AtomicBoolean(false);
  private Bootstrap bootstrap;
  private InetSocketAddress remoteAddress;
  private ChannelHandlerContext ctx;
  private long lastTalkTime;

  public ChannelGuard(long pingTimeout, long reconnectRetryTimeout, Bootstrap bootstrap, InetSocketAddress remoteAddress) {
    this.bootstrap = bootstrap;
    this.remoteAddress = remoteAddress;
    timer = new Timer("Status listener", true);
    timer.scheduleAtFixedRate(pingTask, pingTimeout, pingTimeout);
    timer.scheduleAtFixedRate(lostChannelReconnector, reconnectRetryTimeout, reconnectRetryTimeout);
  }

  /**
   * This constructor is for the server. It does not ping but it receives pongs.
   */
  public ChannelGuard(final long idleChannelKillTimeout) {
    timer = new Timer("CHANNEL GUARD", true);
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        if (System.currentTimeMillis() - lastTalkTime > idleChannelKillTimeout) {
          //a long gap detected, kill the connection.
          LOGGER.warn("Long inactive period. Kill connection");
          try {
            ctx.close();
          } catch (Exception ex) {
          }
          timer.cancel();
        }
      }
    }, idleChannelKillTimeout, idleChannelKillTimeout);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    this.ctx = ctx;
    connectedFlag.set(true);
    super.channelActive(ctx);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    connectedFlag.set(false);
    this.ctx = null;
    super.channelInactive(ctx);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
    // if we received a ping, then we are the server, pong it
    lastTalkTime = System.currentTimeMillis();
    if (message instanceof PingPong) {
      PingPong pp = (PingPong) message;
      if (pp.p == PingPong.PING && this.ctx != null) {
        LOGGER.trace("Received PING, Send PONG");
        try {
          pp.p = PingPong.PONG;
          this.ctx.writeAndFlush(pp);

        } catch (Exception ex) {
        }
      } else {
        LOGGER.trace("Received PONG");
      }
    } else {
      super.channelRead(ctx, message);
    }
  }

  private TimerTask lostChannelReconnector = new TimerTask() {
    @Override
    public void run() {
      try {
        if (!connectedFlag.get()) {
          bootstrap.connect(remoteAddress);
        }
      } catch (Exception e) {
        LOGGER.error(e.getMessage());
      }
    }
  };

  private TimerTask pingTask = new TimerTask() {
    final PingPong pingPong = new PingPong(PingPong.PING);

    @Override
    public void run() {
      if (ctx != null) {
        LOGGER.trace("Send PING");
        ctx.writeAndFlush(pingPong);
      }
    }
  };

  public void kill() {
    try {
      timer.cancel();
    } catch (Exception ex) {
    }
  }

}
