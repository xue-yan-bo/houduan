package com.jlm.homework.service.impl;

import com.jlm.homework.entity.WrongGroupItem;
import com.jlm.homework.repository.WrongGroupItemRepository;
import com.jlm.homework.service.IWrongGroupItemService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WrongGroupItemServiceImpl implements IWrongGroupItemService {
    @Resource
    private WrongGroupItemRepository wrongGroupItemRepository;

    @Override
    public List<WrongGroupItem> listByGroupId(Long wrongGroupId) {
        WrongGroupItem searchItem = new WrongGroupItem();
        searchItem.setWrongGroupId(wrongGroupId);
        Sort sort = Sort.by(Sort.Direction.ASC,"sort");
        return wrongGroupItemRepository.findAll(Example.of(searchItem),sort);
    }

    @Override
    public WrongGroupItem findById(Long itemId) {
        Optional<WrongGroupItem> optional =wrongGroupItemRepository.findById(itemId);
        if(optional.isPresent()){
            return optional.get();
        }
        return null;
    }

    @Override
    public void deleteById(Long itemId) {
        wrongGroupItemRepository.deleteById(itemId);
    }

    @Override
    public void saveOrUpdateBatch(List<WrongGroupItem> itemEntities) {
        wrongGroupItemRepository.saveAll(itemEntities);
    }
}
