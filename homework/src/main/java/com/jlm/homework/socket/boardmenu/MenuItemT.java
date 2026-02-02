package com.jlm.homework.socket.boardmenu;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

@Data
public class MenuItemT implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer id;
    private Long objectId;
    private String desc;
    @JsonIgnore
    private MenuT  pSubMenu;

    // 默认构造函数，用于Redis反序列化
    public MenuItemT() {
    }

    public MenuItemT(Integer id,Long objectId ,String desc, MenuT pSubMenu) {
        this.id = id;
        this.objectId = objectId;
        this.desc = desc;
        this.pSubMenu = pSubMenu;
    }
    // 将数字描述转换为可读字符串
    /*public String getDescriptionString() {
        StringBuilder sb = new StringBuilder();
        for (int code : desc) {
            if (code != 0x0d && code != 0x0) { // 跳过回车符和结束符
                sb.append((char)code);
            }
        }
        return sb.toString();
    }*/

    @JsonIgnore
    public byte[] getDesc2Byte() {
        String name = " "+desc + " \n\0";
        //System.out.println("_______"+name);
        byte[] bytes = name.getBytes(StandardCharsets.UTF_16LE);
        return bytes;
    }
}
