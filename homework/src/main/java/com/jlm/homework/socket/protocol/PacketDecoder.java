package com.jlm.homework.socket.protocol;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * 负责解析TCP数据包，处理粘包/拆包
 */
@Slf4j
public class PacketDecoder {
    private final InputStream inputStream;
    private byte[] buffer;
    private int bufferPos;

    public PacketDecoder(InputStream inputStream) {
        this.inputStream = inputStream;
        this.buffer = new byte[2048];
        this.bufferPos = 0;
    }

    /**
     * 将预读取的数据（如代理头读取时剩余的数据）放入缓冲区
     */
    public void pushPreReadBytes(byte[] bytes) {
        if (bytes != null && bytes.length > 0) {
            ensureCapacity(bufferPos + bytes.length);
            System.arraycopy(bytes, 0, buffer, bufferPos, bytes.length);
            bufferPos += bytes.length;
        }
    }

    /**
     * 读取下一个完整的数据包
     * @return Packet 对象，如果流结束则返回 null
     * @throws IOException
     */
    public Packet readNextPacket() throws IOException {
        while (true) {
            // 1. 确保缓冲区至少有4字节（Header + Length + Type）
            if (bufferPos < 4) {
                if (!readMoreData(4 - bufferPos)) {
                    return null; // 流结束
                }
            }

            // 2. 查找包头 0x55 0x56
            int headerIndex = findHeader();
            if (headerIndex == -1) {
                // 没找到包头，丢弃无效数据（保留最后1字节防跨包）
                if (bufferPos > 0) {
                    // 记录丢弃的数据量
                    if (bufferPos > 10) {
                        log.warn("Header not found, discarding {} bytes of invalid data", bufferPos - 1);
                    }
                    // 保留最后1字节，防止跨包的包头被丢弃
                    buffer[0] = buffer[bufferPos - 1];
                    bufferPos = 1;
                }
                // 继续读取
                if (!readMoreData(1)) {
                    return null;
                }
                continue;
            }

            // 3. 移动数据对齐包头
            if (headerIndex > 0) {
                //log.debug("Found header at index {}, moving {} bytes", headerIndex, bufferPos - headerIndex);
                System.arraycopy(buffer, headerIndex, buffer, 0, bufferPos - headerIndex);
                bufferPos -= headerIndex;
            }

            // 4. 再次检查长度（因为移动后可能不足4字节）
            if (bufferPos < 4) {
                if (!readMoreData(4 - bufferPos)) {
                    return null;
                }
            }

            // 5. 解析长度和类型
            int dataLength = buffer[2] & 0xFF;
            byte dataType = buffer[3];

            // 6. 计算包总长
            // Header(2) + Length(1) + Type(1) + Payload(Length-1) + Checksum(1) = 4 + Length
            int packetTotalLength = 2 + 1 + 1 + (dataLength - 1) + 1;
            
            boolean isBluetooth = (dataType == (byte) 0x81 || dataType == (byte) 0x82);
            if (isBluetooth) {
                // Header(2) + Length(1) + Type(1) + MAC(6) + Payload(Length-1) + Checksum(1) = 4 + 6 + Length
                packetTotalLength += 6; 
            }

            // 7. 确保缓冲区有完整包
            if (bufferPos < packetTotalLength) {
                ensureCapacity(packetTotalLength);
                if (!readMoreData(packetTotalLength - bufferPos)) {
                    return null;
                }
            }

            // 8. 提取包数据
            byte[] fullPacketBuffer = new byte[packetTotalLength];
            System.arraycopy(buffer, 0, fullPacketBuffer, 0, packetTotalLength);

            // 9. 移除已处理数据
            System.arraycopy(buffer, packetTotalLength, buffer, 0, bufferPos - packetTotalLength);
            bufferPos -= packetTotalLength;

            // 10. 验证包头（双重检查）
            if ((fullPacketBuffer[0] & 0xFF) != 0x55 || (fullPacketBuffer[1] & 0xFF) != 0x56) {
                log.error("Invalid header in packet: expected 0x55 0x56, got 0x{:02X} 0x{:02X}, packet length: {}, data: {}", 
                        fullPacketBuffer[0] & 0xFF, fullPacketBuffer[1] & 0xFF, packetTotalLength, 
                        bytesToHex(fullPacketBuffer, Math.min(16, packetTotalLength)));
                continue;
            }

            // 11. 构建Packet对象
            Packet packet = new Packet(dataType, fullPacketBuffer);
            packet.setRawData(fullPacketBuffer);
            packet.setLength(dataLength);
            
            if (isBluetooth) {
                byte[] macBytes = Arrays.copyOfRange(fullPacketBuffer, 4, 10);
                packet.setMac(macBytes);
                // 蓝牙包Payload在MAC之后? 
                // 原代码：ParseTcpDataUtil.parseHandwritingTcpPacketsBluetooth(fullPacketBuffer)
                // 似乎解析工具类直接处理了fullPacketBuffer，所以我们只需传递raw buffer即可。
                // 但为了封装性，我们也可以提取payload。
                // 这里为了兼容原有的Util，我们主要保留RawData。
            }

            return packet;
        }
    }

    private int findHeader() {
        for (int i = 0; i <= bufferPos - 2; i++) {
            if ((buffer[i] & 0xFF) == 0x55 && (buffer[i + 1] & 0xFF) == 0x56) {
                return i;
            }
        }
        return -1;
    }

    private boolean readMoreData(int minBytes) throws IOException {
        int readTotal = 0;
        int attempts = 0;
        final int MAX_ATTEMPTS = 5;
        
        while (readTotal < minBytes && attempts < MAX_ATTEMPTS) {
            int remaining = minBytes - readTotal;
            int available = Math.min(inputStream.available(), buffer.length - bufferPos);
            
            // 计算实际要读取的字节数
            int toRead = Math.min(remaining, Math.max(1, available > 0 ? available : 1024));
            
            int read = inputStream.read(buffer, bufferPos, toRead);
            if (read == -1) {
                return false;
            }
            
            bufferPos += read;
            readTotal += read;
            attempts++;
            
            // 如果读取速度太慢，短暂休眠一下
            if (read < toRead) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        return readTotal >= minBytes;
    }

    private void ensureCapacity(int requiredSize) {
        if (buffer.length < requiredSize) {
            byte[] newBuffer = new byte[Math.max(requiredSize, buffer.length * 2)];
            System.arraycopy(buffer, 0, newBuffer, 0, bufferPos);
            buffer = newBuffer;
        }
    }

    private String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, length); i++) {
            sb.append(String.format("%02X ", bytes[i] & 0xFF));
        }
        return sb.toString().trim();
    }
}
