package gov.tubitak.xoola.tcpcom.handshake2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class ProtocolTracker extends ChannelDuplexHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolTracker.class);
  private String myId;
  private String hisId;
  private byte[] identifierBuffer;

  public ProtocolTracker(String myId, String hisId) {
    this.myId = myId;
    this.hisId = hisId;
    if (this.hisId == null) {
      this.hisId = "GENERIC_CLIENT";
    }
    LOGGER.debug("Protocol Tracker {}:{}", this.myId, this.hisId);
    byte[] _tmp = (this.myId + ":" + this.hisId).getBytes(StandardCharsets.UTF_8);
    byte[] _xoolaBytes = "XOOLA".getBytes(StandardCharsets.UTF_8);
    //XOOLA
    //two bytes length
    //the rest IDS.
    final int identifierStringLength = _tmp.length;
    final int _xoolaHeaderLen = _xoolaBytes.length;
    identifierBuffer = new byte[_xoolaHeaderLen + 2 + identifierStringLength];
    int destPos = 0;
    System.arraycopy(_xoolaBytes, 0, identifierBuffer, destPos, _xoolaHeaderLen);
    destPos += _xoolaHeaderLen;
    identifierBuffer[destPos++] = (byte) ((identifierStringLength >> 8) & 0xFF);
    identifierBuffer[destPos++] = (byte) (identifierStringLength & 0xFF);
    System.arraycopy(_tmp, 0, identifierBuffer, destPos, identifierStringLength);
  }

  byte[] readBuff = new byte[100];

  @Override
  public synchronized void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    LOGGER.debug("----   ------- ----- READ {}", msg.getClass());
    ByteBuf buff = (ByteBuf) msg;
    LOGGER.debug("BUFFER SIZE {}", buff.readableBytes());

    final int i = buff.readShort();
    LOGGER.debug("I: {}", i);
    buff.readBytes(readBuff, 0, i);
    LOGGER.debug("HEADER {}", new String(readBuff, 0, i, StandardCharsets.UTF_8));
    super.channelRead(ctx, buff);
  }

  @Override
  public synchronized void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    try {
      final ByteBuf byteBuf = Unpooled.wrappedBuffer(identifierBuffer, 0, identifierBuffer.length);
      super.write(ctx, Unpooled.wrappedBuffer(byteBuf, (ByteBuf) msg), promise);
    } catch (Exception ex) {
      LOGGER.error(ex.getMessage(), ex);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    super.exceptionCaught(ctx, cause);

    LOGGER.error(cause.getMessage(), cause);
  }
}
