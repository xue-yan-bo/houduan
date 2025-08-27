package com.jlm.homework.socket;

import lombok.Data;

@Data
public class ParseResult {
    private byte[] header;      // 包头（[0x55, 0x56]）
    private int length;         // 数据长度（Type+Packet长度）
    private byte type;          // 数据类型（0x01=手写数据）
    private byte checksum;      // 校验和（数据最后1字节）
    private boolean checksumValid; // 校验和是否有效

    private String userId;
}
