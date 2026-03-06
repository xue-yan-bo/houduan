package com.jlm.homework.socket.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class PacketFrameDecoder extends ByteToMessageDecoder {

    private static final byte HEADER_FIRST = 0x55;
    private static final byte HEADER_SECOND = 0x56;
    private static final int MIN_PACKET_SIZE = 4;
    private static final int MAX_PACKET_SIZE = 65535;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) 
            throws Exception {
        
        in.markReaderIndex();
        
        int headerIndex = findHeader(in);
        if (headerIndex == -1) {
            return;
        }
        
        if (headerIndex > 0) {
            if (headerIndex > 10) {
                log.warn("Skipped {} invalid bytes before header", headerIndex);
            }
            in.skipBytes(headerIndex);
        }
        
        if (in.readableBytes() < MIN_PACKET_SIZE) {
            in.resetReaderIndex();
            return;
        }
        
        int length = in.getUnsignedByte(in.readerIndex() + 2);
        int totalPacketSize = MIN_PACKET_SIZE + length;
        
        if (totalPacketSize > MAX_PACKET_SIZE) {
            log.error("Packet too large: {}, dropping", totalPacketSize);
            in.skipBytes(2);
            return;
        }
        
        if (in.readableBytes() < totalPacketSize) {
            in.resetReaderIndex();
            return;
        }
        
        ByteBuf packet = in.readRetainedSlice(totalPacketSize);
        out.add(packet);
    }

    private int findHeader(ByteBuf in) {
        int readable = in.readableBytes();
        if (readable < 2) {
            return -1;
        }
        
        for (int i = 0; i < readable - 1; i++) {
            if (in.getByte(in.readerIndex() + i) == HEADER_FIRST &&
                in.getByte(in.readerIndex() + i + 1) == HEADER_SECOND) {
                return i;
            }
        }
        
        return readable > 1 ? readable - 1 : -1;
    }
}
