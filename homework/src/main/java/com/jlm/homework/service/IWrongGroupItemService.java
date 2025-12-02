package com.jlm.homework.service;

import com.jlm.homework.entity.WrongGroupItem;

import java.util.List;

public interface IWrongGroupItemService {
    List<WrongGroupItem> listByGroupId(Long wrongGroupId);

    WrongGroupItem findById(Long itemId);

    void deleteById(Long itemId);

    void saveOrUpdateBatch(List<WrongGroupItem> itemEntities);
}
