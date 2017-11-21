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
public class ChannelGuard extends ChannelDuplexHandler {
  private static final Logger LOGGER = Logger.getLogger(ChannelGuard.class);
  private Timer pingTimer;
  private Timer reconnectTimer;

  AtomicBoolean connectedFlag = new AtomicBoolean(false);
  private Bootstrap bootstrap;
  private InetSocketAddress remoteAddress;
  private ChannelHandlerContext ctx;

  public ChannelGuard(long pingTimeout, long reconnectRetryTimeout, Bootstrap bootstrap, InetSocketAddress remoteAddress) {
    this.bootstrap = bootstrap;
    this.remoteAddress = remoteAddress;
    pingTimer = new Timer("Ping Timer", true);
    pingTimer.scheduleAtFixedRate(pingTask, pingTimeout, pingTimeout);
    reconnectTimer = new Timer("Reconnect Timer", true);
    reconnectTimer.scheduleAtFixedRate(lostChannelReconnector, reconnectRetryTimeout, reconnectRetryTimeout);
  }

  public ChannelGuard() {
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    this.ctx = ctx;
    connectedFlag.set(true);
  }

  @Override
  public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
    super.close(ctx, promise);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    super.channelInactive(ctx);
    this.ctx = null;
    connectedFlag.set(false);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
    // if we received a ping, then we are the server, pong it
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
          LOGGER.debug("Connection lost, reconnect!");
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
      pingTimer.cancel();
      reconnectTimer.cancel();
    } catch (Exception ex) {
    }
  }

}
