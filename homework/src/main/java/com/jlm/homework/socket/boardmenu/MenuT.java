package com.jlm.homework.socket.boardmenu;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.List;
@Data
public class MenuT implements Serializable {
    private static final long serialVersionUID = 1L;
    //父菜单
    @JsonIgnore
    public MenuT  parentMenu;
    //菜单内容列表
    private List<MenuItemT> pItems;

    private Integer selectItem;
    private Integer showStartItem;
    private Integer showEndItem;
    private Integer maxItems;

    // 默认构造函数，用于Redis反序列化
    public MenuT() {
    }

    public MenuT(MenuT parentMenu, List<MenuItemT> pItems, Integer selectItem, Integer showStartItem, Integer showEndItem, Integer maxItems){
        this.parentMenu = parentMenu;
        this.pItems = pItems;
        this.selectItem = selectItem;
        this.showStartItem = showStartItem;
        this.showEndItem = showEndItem;
        this.maxItems = maxItems;
    }

    @JsonIgnore
    public void setSelectItem(Integer selectItem, MenuT menuT) {
        this.selectItem = selectItem;
    }

    @JsonIgnore
    public void setShowEndItem(Integer showEndItem, MenuT menuT) {
        this.showEndItem = showEndItem;
    }
}
