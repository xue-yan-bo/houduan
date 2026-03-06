package com.jlm.homework.socket.netty;

import com.jlm.homework.socket.boardmenu.MenuItemT;
import com.jlm.homework.socket.boardmenu.MenuT;
import com.jlm.homework.socket.context.SessionContext;
import com.jlm.homework.socket.handler.ResponseSender;
import com.jlm.homework.util.ParseTcpDataUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class NettyResponseSender extends ResponseSender {

    private final ChannelHandlerContext ctx;
    private final SessionContext sessionContext;

    public NettyResponseSender(ChannelHandlerContext ctx, SessionContext sessionContext) {
        super(null);
        this.ctx = ctx;
        this.sessionContext = sessionContext;
    }

    @Override
    public void sendMenuUpdate(SessionContext context) throws IOException {
        byte[] buff = new byte[128];
        int len = 0;
        
        MenuT pCurrentMenu = context.getCurrentMenu();
        if (pCurrentMenu != null) {
            for (int i = pCurrentMenu.getShowStartItem(); i < pCurrentMenu.getShowEndItem(); i++) {
                if (pCurrentMenu.getPItems() == null || pCurrentMenu.getPItems().isEmpty()) {
                    break;
                }
                MenuItemT menuItem = pCurrentMenu.getPItems().get(i);
                byte[] desc = menuItem.getDesc2Byte();

                System.arraycopy(desc, 0, buff, len, desc.length);

                if (pCurrentMenu.getSelectItem() == i) {
                    len += desc.length;
                    if (len < buff.length - 1) {
                        buff[len] = (byte) 0x92;
                        buff[len + 1] = (byte) 0x21;
                        len += 2;
                    }
                }
            }
        } else {
            buff[0] = 0x00;
            buff[1] = 0x30;
            len = 2;
        }

        if (len > 0) {
            try {
                byte[] sendData = new byte[len];
                System.arraycopy(buff, 0, sendData, 0, len);
                
                if (context.isBluetooth() && context.getMac() != null) {
                    sendLcdDisplayDataBluetooth(sendData, len, context.getMac());
                } else {
                    sendLcdDisplayData(sendData, len);
                }
            } catch (Exception e) {
                log.error("Failed to send menu update: {}", e.getMessage());
            }
        }
    }

    private void sendLcdDisplayData(byte[] data, int length) {
        byte[] buff = new byte[length + 5];
        buff[0] = 0x55;
        buff[1] = 0x56;
        buff[2] = (byte)(length + 1);
        buff[3] = 0x04;
        System.arraycopy(data, 0, buff, 4, length);
        
        int checksum = 0;
        for (int i = 0; i < length + 4; i++) {
            checksum += (buff[i] & 0xFF);
        }
        buff[length + 4] = (byte)(checksum & 0xFF);
        
        ctx.writeAndFlush(Unpooled.wrappedBuffer(buff));
    }

    private void sendLcdDisplayDataBluetooth(byte[] data, int length, String mac) {
        byte[] buff = new byte[length + 11];
        buff[0] = 0x55;
        buff[1] = 0x56;
        buff[2] = (byte)(length + 7);
        buff[3] = 0x04;
        
        byte[] macByte = ParseTcpDataUtil.hexStringToByteArray(mac);
        System.arraycopy(macByte, 0, buff, 4, 6);
        System.arraycopy(data, 0, buff, 10, length);
        
        int checksum = 0;
        for (int i = 0; i < length + 10; i++) {
            checksum += (buff[i] & 0xFF);
        }
        buff[length + 10] = (byte)(checksum & 0xFF);
        
        ctx.writeAndFlush(Unpooled.wrappedBuffer(buff));
    }

    @Override
    public void sendRaw(byte[] data) throws IOException {
        ctx.writeAndFlush(Unpooled.wrappedBuffer(data));
    }

    @Override
    public void sendText(String text) {
        try {
            byte[] data = (text + "\n").getBytes();
            ctx.writeAndFlush(Unpooled.wrappedBuffer(data));
        } catch (Exception e) {
            log.error("Error sending text", e);
        }
    }
}
