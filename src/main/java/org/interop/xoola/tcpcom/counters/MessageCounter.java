package org.interop.xoola.tcpcom.counters;

import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

/**
 * @author <a href="mailto:bruno@factor45.org">Bruno de Carvalho</a>
 */
public class MessageCounter extends SimpleChannelHandler {

    // internal vars ----------------------------------------------------------

    private final String id;
    private final AtomicLong writtenMessages;
    private final AtomicLong readMessages;

    // constructors -----------------------------------------------------------

    public MessageCounter(String id) {
        this.id = id;
        writtenMessages = new AtomicLong();
        readMessages = new AtomicLong();
    }

    // SimpleChannelHandler ---------------------------------------------------

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        readMessages.incrementAndGet();
        super.messageReceived(ctx, e);
    }

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        writtenMessages.incrementAndGet();
        super.writeRequested(ctx, e);
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelClosed(ctx, e);
        System.out.println(id + ctx.getChannel() + " -> sent: " + getWrittenMessages() + ", recv: " + getReadMessages());
    }

    // getters & setters ------------------------------------------------------

    public long getWrittenMessages() {
        return writtenMessages.get();
    }

    public long getReadMessages() {
        return readMessages.get();
    }
}
