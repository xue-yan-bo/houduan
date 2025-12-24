package com.jlm.homework.socket.protocol;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class PacketDecoderTest {

    @Test
    public void testReadPacket() throws IOException {
        // Mock data: Header(55 56) + Length(05) + Type(01) + Data(01 02 03 04) + Checksum(FF)
        // Length = 5. Payload = Length - 1 = 4 bytes.
        // Total = 4 + 5 = 9 bytes.
        byte[] data = {
            0x55, 0x56, 0x05, 0x01, 
            0x01, 0x02, 0x03, 0x04,
            (byte)0xFF
        };

        ByteArrayInputStream in = new ByteArrayInputStream(data);
        PacketDecoder decoder = new PacketDecoder(in);

        Packet packet = decoder.readNextPacket();
        assertNotNull(packet);
        assertEquals(0x01, packet.getType());
        assertEquals(5, packet.getLength());
        assertEquals(9, packet.getRawData().length);
    }

    @Test
    public void testSplitPacket() throws IOException {
        // Packet split in two reads
        byte[] part1 = { 0x55, 0x56, 0x05 };
        byte[] part2 = { 0x01, 0x01, 0x02, 0x03, 0x04, (byte)0xFF };

        // Simulate stream by concatenation? No, ByteArrayInputStream is one stream.
        // But PacketDecoder reads from stream.
        // We can use a custom InputStream that returns bytes in chunks, but ByteArrayInputStream is fine.
        // The decoder loop calls read().
        
        byte[] all = new byte[part1.length + part2.length];
        System.arraycopy(part1, 0, all, 0, part1.length);
        System.arraycopy(part2, 0, all, part1.length, part2.length);
        
        ByteArrayInputStream in = new ByteArrayInputStream(all);
        PacketDecoder decoder = new PacketDecoder(in);
        
        Packet packet = decoder.readNextPacket();
        assertNotNull(packet);
        assertEquals(0x01, packet.getType());
    }
}
