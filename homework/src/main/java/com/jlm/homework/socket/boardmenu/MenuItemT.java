package com.jlm.homework.socket.boardmenu;

import lombok.Data;

@Data
public class MenuItemT {
    private Integer id;
    private byte[] desc;
    private MenuT  pSubMenu;

    public MenuItemT(Integer id, byte[] desc, MenuT pSubMenu) {
        this.id = id;
        this.desc = desc;
        this.pSubMenu = pSubMenu;
    }
    // 将数字描述转换为可读字符串
    public String getDescriptionString() {
        StringBuilder sb = new StringBuilder();
        for (int code : desc) {
            if (code != 0x0d && code != 0x0) { // 跳过回车符和结束符
                sb.append((char)code);
            }
        }
        return sb.toString();
    }
}
