package com.jlm.homework.socket;

import java.io.BufferedInputStream;
import java.net.InetSocketAddress;

/**
 * 代理头解析结果封装类
 */
public class ProxyHeaderResult {
    private final InetSocketAddress realAddress;
    private final byte[] preReadBytes;
    private final BufferedInputStream bufferedInputStream;
    
    public ProxyHeaderResult(InetSocketAddress realAddress, byte[] preReadBytes) {
        this.realAddress = realAddress;
        this.preReadBytes = preReadBytes;
        this.bufferedInputStream = null;
    }
    
    public ProxyHeaderResult(InetSocketAddress realAddress, byte[] preReadBytes, BufferedInputStream bufferedInputStream) {
        this.realAddress = realAddress;
        this.preReadBytes = preReadBytes;
        this.bufferedInputStream = bufferedInputStream;
    }
    
    public InetSocketAddress getRealAddress() {
        return realAddress;
    }
    
    public byte[] getPreReadBytes() {
        return preReadBytes;
    }
    
    public BufferedInputStream getBufferedInputStream() {
        return bufferedInputStream;
    }
}