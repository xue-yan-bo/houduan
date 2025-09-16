package com.jlm.homework.socket.boardmenu;

import lombok.Data;

@Data
public class MenuItemT {
    private Integer id;
    private String desc;
    private MenuT  pSubMenu;

    public MenuItemT(Integer id, String desc, MenuT pSubMenu) {
        this.id = id;
        this.desc = desc;
        this.pSubMenu = pSubMenu;
    }
}
