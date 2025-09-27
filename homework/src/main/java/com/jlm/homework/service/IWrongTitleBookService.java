package com.jlm.homework.service;

import com.jlm.homework.entity.WrongTitleBook;

import java.util.List;

public interface IWrongTitleBookService {

    WrongTitleBook save(WrongTitleBook wrongTitleBook);

    List<WrongTitleBook> findByStudentId(Long studentId);

    void createWrongBook(Long studentsHomeworkId);

    void addWrongBook(WrongTitleBook wrongTitleBook);
}
