package org.interop.xoola.tcpcom.counters;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.WriteCompletionEvent;

/**
 * @author <a href="mailto:bruno@factor45.org">Bruno de Carvalho</a>
 */
public class ByteCounter extends SimpleChannelUpstreamHandler {

    private static final Logger LOGGER = Logger.getLogger(ByteCounter.class);

    // internal vars ----------------------------------------------------------

    private final String id;
    private final AtomicLong writtenBytes;
    private final AtomicLong readBytes;

    // constructors -----------------------------------------------------------

    public ByteCounter(String id) {
        this.id = id;
        writtenBytes = new AtomicLong();
        readBytes = new AtomicLong();
    }

    // SimpleChannelUpstreamHandler -------------------------------------------

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof ChannelBuffer) {
            readBytes.addAndGet(((ChannelBuffer) e.getMessage()).readableBytes());
        }

        super.messageReceived(ctx, e);
    }

    @Override
    public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e) throws Exception {
        super.writeComplete(ctx, e);
        writtenBytes.addAndGet(e.getWrittenAmount());
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelClosed(ctx, e);
        LOGGER.info(id + ctx.getChannel() + " -> sent: " + getWrittenBytes() + "b, recv: " + getReadBytes() + "b");
    }

    // getters & setters ------------------------------------------------------

    public long getWrittenBytes() {
        return writtenBytes.get();
    }

    public long getReadBytes() {
        return readBytes.get();
    }

}
