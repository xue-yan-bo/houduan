package com.jlm.homework.socket;

import java.net.InetSocketAddress;

/**
 * 代理头解析结果封装类
 */
public class ProxyHeaderResult {
    private final InetSocketAddress realAddress;
    private final byte[] preReadBytes;
    
    public ProxyHeaderResult(InetSocketAddress realAddress, byte[] preReadBytes) {
        this.realAddress = realAddress;
        this.preReadBytes = preReadBytes;
    }
    
    public InetSocketAddress getRealAddress() {
        return realAddress;
    }
    
    public byte[] getPreReadBytes() {
        return preReadBytes;
    }
}