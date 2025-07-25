package com.jlm.homework.service.impl;

import com.jlm.homework.entity.WrongTitleBook;
import com.jlm.homework.repository.WrongTitleBookRepository;
import com.jlm.homework.service.IWrongTitleBookService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WrongTitleBookServiceImpl implements IWrongTitleBookService {
    @Resource
    private WrongTitleBookRepository wrongTitleBookRepository;

    @Override
    public List<WrongTitleBook> getWrongTitleBooks(Long homeworkPublishId, Long classId) {
        WrongTitleBook wrongTitleBook = new WrongTitleBook();
        wrongTitleBook.setHomeworkPublishId(homeworkPublishId);
        Example example = Example.of(wrongTitleBook);
        List<WrongTitleBook> wrongTitleBooks=wrongTitleBookRepository.findAll();
        return wrongTitleBooks;
    }
}
