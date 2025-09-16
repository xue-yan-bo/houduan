package com.jlm.homework.socket;

import com.jlm.homework.socket.boardmenu.MenuItemT;
import com.jlm.homework.socket.boardmenu.MenuT;
import com.jlm.homework.util.ParseTcpDataUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * 菜单更新工具类
 * 用于将C++的NMenuUpdate功能翻译为Java实现
 */
public class MenuUtil {
    
    /**
     * 更新菜单并发送到客户端
     * @param out 输出流，用于发送数据
     * @throws IOException IO异常
     */
    public static void nMenuUpdate(OutputStream out, PrintWriter writer) throws IOException {
        // 对应C++的char buff[128] = {0}
        byte[] buff = new byte[128];
        int len = 0; // 对应C++的uint8_t len = 0
        
        // 获取当前菜单
        MenuT pCurrentMenu = ClientHandler.pCurrentMenu;
        System.out.println("菜单内容个数:"+pCurrentMenu.getPItems().size());
        if (pCurrentMenu != null) {
            // 对应C++的for循环
            for (int i = pCurrentMenu.getShowStartItem(); i <= pCurrentMenu.getShowEndItem(); i++) {
                // 获取菜单项
                MenuItemT menuItem = pCurrentMenu.getPItems().get(i);
                // 获取描述并转换为UTF-16LE编码字节数组
                byte[] desc = menuItem.getDesc();
                byte[] descBytes = desc;
                
                // 对应C++的memcpy
                System.arraycopy(descBytes, 0, buff, len, descBytes.length);
                
                // 检查是否是选中项
                if (pCurrentMenu.getSelectItem() == i) {
                    len += descBytes.length;
                    // 添加特殊标记 0x92 0x21
                    if (len < buff.length - 1) {
                        buff[len] = (byte) 0x92;
                        buff[len + 1] = (byte) 0x21;
                        len += 2;
                    }
                } else {
                    // 设置默认值
                    buff[0] = 0x00;
                    buff[1] = 0x30;
                    len = 2;
                }
            }
        } else {
            // 没有当前菜单时的默认值
            buff[0] = 0x00;
            buff[1] = 0x30;
            len = 2;
        }
        
        // 创建长度为len的子数组并发送
        // 对应C++的tcp_send(sl, buff, len)
        if (len > 0 && out != null) {
            byte[] sendData = new byte[len];
            System.arraycopy(buff, 0, sendData, 0, len);
            System.out.println(new String(sendData, StandardCharsets.UTF_16LE));
            writer.println("++++++++++"+new String(sendData, StandardCharsets.UTF_16LE));
            ParseTcpDataUtil.sendLcdDisplayData(out,sendData,len);
            out.write(sendData);
            out.flush();
        }
    }

    
    /**
     * 将字节数组转换为十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex).append(' ');
        }
        return hexString.toString();
    }
    
    /**
     * 在ClientHandler中使用的示例方法
     * 这在实际应用中会被添加到ClientHandler类中
     */
    public static void updateMenuInClientHandler(OutputStream out,PrintWriter writer) throws IOException {
        try {
            nMenuUpdate(out,writer);
        } catch (IOException e) {
            System.err.println("更新菜单失败: " + e.getMessage());
        }
    }
}
