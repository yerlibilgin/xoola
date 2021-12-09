package gov.tubitak.xoola.java.test;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;

public class CloseVsDisconnectVsChannelInactive {
  @Test
  public void vs() throws InterruptedException {

    {
      ServerBootstrap serverBootstrap = new ServerBootstrap();
      serverBootstrap.group(new NioEventLoopGroup(), new NioEventLoopGroup());
      serverBootstrap.channel(NioServerSocketChannel.class);
      serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        public void initChannel(SocketChannel ch) throws Exception {
          ch.pipeline().addLast(new DummyHandler("Server"));
        }
      });

      serverBootstrap.bind("localhost", 5555);
    }

    Thread.sleep(1000);
    Channel channel;
    {
      EventLoopGroup workerGroup = new NioEventLoopGroup();
      Bootstrap bootstrap = new Bootstrap(); // (1)
      bootstrap.group(workerGroup); // (2)
      bootstrap.channel(NioSocketChannel.class); // (3)
      bootstrap.option(ChannelOption.SO_KEEPALIVE, true); // (4)

      bootstrap.handler(new ChannelInitializer<SocketChannel>() {
        @Override
        public void initChannel(SocketChannel ch) throws Exception {
          ch.pipeline().addLast(new DummyHandler("Client"));
        }
      });

      // Set the connection timeout
      bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
      bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
      bootstrap.option(ChannelOption.TCP_NODELAY, true);
      channel = bootstrap.connect("localhost", 5555).awaitUninterruptibly().channel();
    }

    channel.write("Something");
    channel.flush();
    Thread.sleep(2000);
    channel.disconnect();
    Thread.sleep(1000);
  }

  private class DummyHandler extends ChannelDuplexHandler {
    private String name;

    public DummyHandler(String name) {
      this.name = name;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
      super.channelRegistered(ctx);
      System.out.println(name + " channelRegistered");
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
      System.out.println(name + " Close called");
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
      System.out.println(name + " disconnect called");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      System.out.println(name + " inactive called");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      System.out.println(name + " active called");
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
      super.write(ctx, msg, promise);
      System.out.println(name + " Write called");
    }

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
      super.read(ctx);

      System.out.println(name + " Read called");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      super.exceptionCaught(ctx, cause);
      cause.printStackTrace();
    }
  }
}
