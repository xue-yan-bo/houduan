package com.jlm.homework.service.impl;

import com.jlm.homework.entity.QuestionBank;
import com.jlm.homework.entity.WrongTitleBook;
import com.jlm.homework.repository.WrongTitleBookRepository;
import com.jlm.homework.service.IWrongTitleBookService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.*;
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
        wrongTitleBook.setClassId(classId);
        Example example = Example.of(wrongTitleBook);
        List<WrongTitleBook> wrongTitleBooks=wrongTitleBookRepository.findAll(example);
        return wrongTitleBooks;
    }

    @Override
    public Page<WrongTitleBook> getPage(Integer pageNum, Integer pageSize, WrongTitleBook wrongTitleBook) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable;
        pageable = PageRequest.of(pageNum, pageSize, sort);
        return wrongTitleBookRepository.findAll(Example.of(wrongTitleBook),pageable);
    }
}
