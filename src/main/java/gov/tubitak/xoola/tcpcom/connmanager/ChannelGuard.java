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

package gov.tubitak.xoola.tcpcom.connmanager;

import gov.tubitak.xoola.tcpcom.connmanager.client.PingPong;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import gov.tubitak.xoola.tcpcom.connmanager.client.PingPong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A guard (waiter) to check the channel state
 * @author yerlibilgin
 */
@ChannelHandler.Sharable
public class ChannelGuard extends ChannelDuplexHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(ChannelGuard.class);
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
