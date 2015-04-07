package org.interop.xoola.tcpcom.connmanager;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.interop.xoola.tcpcom.connmanager.client.PingPong;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.WriteCompletionEvent;

public class ChannelGuard extends SimpleChannelHandler {
 private static final Logger LOGGER = Logger.getLogger(ChannelGuard.class);
 private Timer timer;

 AtomicBoolean connectedFlag = new AtomicBoolean(false);
 private ClientBootstrap bootstrap;
 private InetSocketAddress remoteAddress;
 ExecutorService reconnectPool = Executors.newFixedThreadPool(2);
 private Channel channel;
 private long lastTalkTime = System.currentTimeMillis();
 private long pingTimeout;

 public ChannelGuard(long pingTimeout, ClientBootstrap bootstrap, InetSocketAddress remoteAddress) {
  this.pingTimeout = pingTimeout;
  this.bootstrap = bootstrap;
  this.remoteAddress = remoteAddress;
  timer = new Timer("Status listener", true);
  timer.scheduleAtFixedRate(idleChannelDestroyer, pingTimeout * 1000, pingTimeout * 1000);
  timer.scheduleAtFixedRate(pingTask, pingTimeout * 1000, pingTimeout * 1000);
  timer.scheduleAtFixedRate(lostChannelReconnector, pingTimeout * 1000, pingTimeout * 1000);
 }

 /**
  * This constructor is for the server. It does not ping. If the channel was
  * idle for a long time it just destroys it.
  * 
  * @param pingTimeout
  */
 public ChannelGuard(long pingTimeout) {
  this.pingTimeout = pingTimeout;
  timer = new Timer("Status listener", true);
  timer.scheduleAtFixedRate(idleChannelDestroyer, pingTimeout * 1000, pingTimeout * 1000);
 }

 @Override
 public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
  super.channelConnected(ctx, e);
  connectedFlag.set(true);
  this.channel = ctx.getChannel();
  lastTalkTime = System.currentTimeMillis();
 }

 @Override
 public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
  super.channelDisconnected(ctx, e);
  try {
   ctx.getPipeline().remove(this);
  } catch (Exception ex) {

  }

  connectedFlag.set(false);
  this.channel = null;
 }

 @Override
 public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
  super.channelClosed(ctx, e);

  try {
   ctx.getPipeline().remove(this);
  } catch (Exception ex) {

  }
  connectedFlag.set(false);
  this.channel = null;
 }

 @Override
 public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
  // if we received a ping, then we are the server, pong it
  if (e.getMessage() instanceof PingPong) {
   PingPong pp = (PingPong) e.getMessage();
   if (pp.p == PingPong.PING && this.channel != null) {
    try {
     pp.p = PingPong.PONG;
     this.channel.write(pp);
    } catch (Exception ex) {
    }
   }
  } else {
   super.messageReceived(ctx, e);
  }

  lastTalkTime = System.currentTimeMillis();
 }

 @Override
 public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e) throws Exception {
  super.writeComplete(ctx, e);
  lastTalkTime = System.currentTimeMillis();
 }

 private TimerTask idleChannelDestroyer = new TimerTask() {
  @Override
  public void run() {
   long diff = System.currentTimeMillis() - lastTalkTime;
   if (diff > pingTimeout * 10000) {
    // LOGGER.warn("A very long idle period detected. Closing all channels");
    try {
     channel.close();
    } catch (Exception ex) {
    }
   }
  }
 };

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
  @Override
  public void run() {
   if (channel != null) {
    channel.write(new PingPong(PingPong.PING));
    // dotPrinter.printDots("PING");
   } else {
    // System.out.println("Cannot ping. no connection");
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
