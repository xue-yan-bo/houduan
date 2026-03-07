package com.jlm.homework.socket.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
public class ProxyProtocolDecoder extends ByteToMessageDecoder {

    private static final byte[] PROXY_V2_SIGNATURE = {
        0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A
    };

    private static final int MIN_PACKET_SIZE = 16;
    private static final int MAX_PACKET_SIZE = 65535;

    private boolean proxyHeaderParsed = false;
    private InetSocketAddress realAddress;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (proxyHeaderParsed) {
            // 代理头已解析，直接传递数据
            int readableBytes = in.readableBytes();
            if (readableBytes > 0) {
                ByteBuf buf = in.readBytes(readableBytes);
                out.add(buf);
            }
            return;
        }

        in.markReaderIndex();

        if (in.readableBytes() < MIN_PACKET_SIZE) {
            in.resetReaderIndex();
            return;
        }

        // 检查是否是 PROXY v2 协议
        if (isProxyV2Signature(in)) {
            parseProxyV2(ctx, in, out);
        } else {
            // 检查是否是 PROXY v1 协议
            if (isProxyV1Signature(in)) {
                parseProxyV1(ctx, in, out);
            } else {
                // 不是代理协议，使用原始地址
                realAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                proxyHeaderParsed = true;
                
                // 传递所有数据
                int readableBytes = in.readableBytes();
                if (readableBytes > 0) {
                    ByteBuf buf = in.readBytes(readableBytes);
                    out.add(buf);
                }
            }
        }
    }

    private boolean isProxyV2Signature(ByteBuf in) {
        for (int i = 0; i < PROXY_V2_SIGNATURE.length; i++) {
            if (in.getByte(in.readerIndex() + i) != PROXY_V2_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean isProxyV1Signature(ByteBuf in) {
        if (in.readableBytes() < 5) {
            return false;
        }
        byte[] signature = new byte[5];
        in.getBytes(in.readerIndex(), signature);
        String sigStr = new String(signature);
        return sigStr.startsWith("PROXY");
    }

    private void parseProxyV2(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        try {
            // 跳过签名
            in.skipBytes(12);

            // 读取 version/command
            int versionCommand = in.readUnsignedByte();
            int version = (versionCommand >> 4) & 0x0F;
            int command = versionCommand & 0x0F;

            if (version != 2) {
                log.warn("Invalid PROXY v2 version: {}, expected 2", version);
                fallbackToOriginalAddress(ctx, in, out);
                return;
            }

            if (command != 0x01) {
                log.debug("PROXY v2 command is LOCAL (0x00), using original address");
                fallbackToOriginalAddress(ctx, in, out);
                return;
            }

            // 读取 protocol/family
            int protocolFamily = in.readUnsignedByte();
            int family = protocolFamily & 0x0F;

            // 读取 address length
            int addressLength = in.readUnsignedShort();

            if (family == 0x01) {
                // IPv4
                if (in.readableBytes() < 12) {
                    log.warn("Insufficient data for PROXY v2 IPv4 address");
                    fallbackToOriginalAddress(ctx, in, out);
                    return;
                }

                byte[] srcAddress = new byte[4];
                in.readBytes(srcAddress);
                int srcPort = in.readUnsignedShort();

                // 跳过目标地址和端口
                in.skipBytes(6);

                String clientIp = String.format("%d.%d.%d.%d",
                        srcAddress[0] & 0xff,
                        srcAddress[1] & 0xff,
                        srcAddress[2] & 0xff,
                        srcAddress[3] & 0xff);
                realAddress = new InetSocketAddress(clientIp, srcPort);

            } else if (family == 0x02) {
                // IPv6
                if (in.readableBytes() < 36) {
                    log.warn("Insufficient data for PROXY v2 IPv6 address");
                    fallbackToOriginalAddress(ctx, in, out);
                    return;
                }

                byte[] srcAddress = new byte[16];
                in.readBytes(srcAddress);
                int srcPort = in.readUnsignedShort();

                // 跳过目标地址和端口
                in.skipBytes(18);

                // 构建 IPv6 地址
                StringBuilder ipBuilder = new StringBuilder();
                for (int i = 0; i < 16; i += 2) {
                    if (i > 0) ipBuilder.append(":");
                    int high = (srcAddress[i] & 0xff) << 8;
                    int low = srcAddress[i + 1] & 0xff;
                    ipBuilder.append(String.format("%04x", high | low));
                }
                realAddress = new InetSocketAddress(ipBuilder.toString(), srcPort);

            } else {
                log.warn("Unsupported PROXY v2 address family: {}", family);
                fallbackToOriginalAddress(ctx, in, out);
                return;
            }

            proxyHeaderParsed = true;
            log.info("Parsed PROXY v2 header, real client address: {}", realAddress);

            // 传递剩余数据
            int readableBytes = in.readableBytes();
            if (readableBytes > 0) {
                ByteBuf buf = in.readBytes(readableBytes);
                out.add(buf);
            }

        } catch (Exception e) {
            log.warn("Failed to parse PROXY v2 header: {}", e.getMessage());
            fallbackToOriginalAddress(ctx, in, out);
        }
    }

    private void parseProxyV1(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        try {
            // 读取 PROXY v1 头
            StringBuilder lineBuilder = new StringBuilder();
            while (in.readableBytes() > 0) {
                byte b = in.readByte();
                if (b == '\n') {
                    break;
                }
                if (b != '\r') {
                    lineBuilder.append((char) b);
                }
            }

            String line = lineBuilder.toString();
            if (!line.startsWith("PROXY")) {
                fallbackToOriginalAddress(ctx, in, out);
                return;
            }

            String[] parts = line.split(" ");
            if (parts.length < 6) {
                fallbackToOriginalAddress(ctx, in, out);
                return;
            }

            String clientIp = parts[2];
            int clientPort = Integer.parseInt(parts[4]);
            realAddress = new InetSocketAddress(clientIp, clientPort);

            proxyHeaderParsed = true;
            log.info("Parsed PROXY v1 header, real client address: {}", realAddress);

            // 传递剩余数据
            int readableBytes = in.readableBytes();
            if (readableBytes > 0) {
                ByteBuf buf = in.readBytes(readableBytes);
                out.add(buf);
            }

        } catch (Exception e) {
            log.warn("Failed to parse PROXY v1 header: {}", e.getMessage());
            fallbackToOriginalAddress(ctx, in, out);
        }
    }

    private void fallbackToOriginalAddress(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        realAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        proxyHeaderParsed = true;

        // 重置到开始位置
        in.resetReaderIndex();

        // 传递所有数据
        int readableBytes = in.readableBytes();
        if (readableBytes > 0) {
            ByteBuf buf = in.readBytes(readableBytes);
            out.add(buf);
        }
    }

    public InetSocketAddress getRealAddress() {
        return realAddress;
    }
}
