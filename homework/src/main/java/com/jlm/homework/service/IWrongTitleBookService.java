package com.jlm.homework.service;

import com.jlm.homework.entity.WrongTitleBook;

import java.util.List;

public interface IWrongTitleBookService {
    List<WrongTitleBook> getWrongTitleBooks(Long homeworkPublishId,Long classId);
}
