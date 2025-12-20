package com.jlm.homework.socket.protocol;

import lombok.Data;

@Data
public class Packet {
    private byte[] rawData;
    private byte type;
    private int length;
    private byte[] payload;
    private byte[] mac; // For Bluetooth packets
    private boolean isBluetooth;

    public Packet(byte type, byte[] payload) {
        this.type = type;
        this.payload = payload;
        this.isBluetooth = (type == (byte) 0x81 || type == (byte) 0x82);
    }
}
