package gov.tubitak.xoola.tcpcom.connmanager;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PingPongHandler extends ChannelDuplexHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(PingPongHandler.class);
  private boolean initiator;

  private final Executor PONG_POOL = Executors.newSingleThreadExecutor();

  /**
   * If initiator==true, we send a ping upon connection
   *
   * @param initiator
   */
  PingPongHandler(boolean initiator) {
    this.initiator = initiator;
  }

  public static PingPongHandler createPinger() {
    return new PingPongHandler(true);
  }

  public static PingPongHandler createPonger() {
    return new PingPongHandler(false);
  }

  @Override
  public void read(ChannelHandlerContext ctx) throws Exception {
    super.read(ctx);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    if (initiator) {
      LOGGER.debug("channelActive, start pinging");
      ctx.writeAndFlush(PingPong.PING);
    }
    super.channelActive(ctx);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof PingPong) {
      final PingPong ping = (PingPong) msg;
      final PingPong pong = ping.hitBack();

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Received {}, send {}", ping, pong);
      }

      PONG_POOL.execute(() -> {
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
        }

        ctx.writeAndFlush(pong);
      });
    } else {
      ctx.fireChannelRead(msg);
    }
  }
}
