package com.jlm.homework.service;

import com.jlm.homework.entity.WrongTitleBook;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IWrongTitleBookService {
    List<WrongTitleBook> getWrongTitleBooks(Long homeworkPublishId,Long classId);

    Page<WrongTitleBook> getPage(Integer pageNum, Integer pageSize, WrongTitleBook wrongTitleBook);
}
