package com.jlm.homework.socket.boardmenu;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
@Data
public class MenuT implements Serializable {
    private static final long serialVersionUID = 1L;
    //父菜单
    public MenuT  parentMenu;
    //菜单内容列表
    private List<MenuItemT> pItems;

    private Integer selectItem;
    private Integer showStartItem;
    private Integer showEndItem;
    private Integer maxItems;

    public MenuT(MenuT parentMenu, List<MenuItemT> pItems, Integer selectItem, Integer showStartItem, Integer showEndItem, Integer maxItems){
        this.parentMenu = parentMenu;
        this.pItems = pItems;
        this.selectItem = selectItem;
        this.showStartItem = showStartItem;
        this.showEndItem = showEndItem;
        this.maxItems = maxItems;
    }

    public void setSelectItem(Integer selectItem, MenuT menuT) {
        this.selectItem = selectItem;
    }

    public void setShowEndItem(Integer showEndItem, MenuT menuT) {
        this.showEndItem = showEndItem;
    }
}
