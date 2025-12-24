package com.jlm.homework.socket.handler;

import com.jlm.homework.socket.boardmenu.MenuItemT;
import com.jlm.homework.socket.boardmenu.MenuT;
import com.jlm.homework.socket.context.SessionContext;
import com.jlm.homework.util.ParseTcpDataUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;

@Slf4j
public class ResponseSender {
    private final OutputStream out;

    public ResponseSender(OutputStream out) {
        this.out = out;
    }

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
                } else {
                    buff[0] = 0x00;
                    buff[1] = 0x30;
                    len += 2;
                }
            }
        } else {
            buff[0] = 0x00;
            buff[1] = 0x30;
            len = 2;
        }

        if (len > 0 && out != null) {
            try {
                byte[] sendData = new byte[len];
                System.arraycopy(buff, 0, sendData, 0, len);
                
                if (context.isBluetooth() && context.getMac() != null) {
                    ParseTcpDataUtil.sendLcdDisplayDataBluetooth(out, sendData, len, context.getMac());
                } else {
                    ParseTcpDataUtil.sendLcdDisplayData(out, sendData, len);
                }
                // 注意：sendLcdDisplayData已经包含了flush操作，这里不需要重复调用
            } catch (IOException e) {
                log.error("Failed to send menu update: {}", e.getMessage());
                // 连接可能已关闭，记录日志但不抛出异常，避免程序崩溃
            }
        }
    }

    public void sendRaw(byte[] data) throws IOException {
        out.write(data);
        out.flush();
    }

    public void sendText(String text) {
        // Simple text sending, assuming PrintWriter-like behavior is needed or wrap it
        try {
            out.write((text + "\n").getBytes());
            out.flush();
        } catch (IOException e) {
            log.error("Error sending text", e);
        }
    }
}
