package com.jlm.homework.service;

import com.jlm.homework.entity.WrongGroup;
import com.jlm.homework.entity.WrongGroupItem;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IWrongGroupService {
    WrongGroup addGroup(WrongGroup wrongGroup);

    WrongGroup getById(Long id);

    WrongGroup update(WrongGroup wrongGroup);

    Page<WrongGroup> selectList(Integer pageNum, Integer pageSize, WrongGroup wrongGroup);

    void deleteById(Long id);

    void toWord(WrongGroup wrongGroup, List<WrongGroupItem> itemList);
}
