package com.jlm.homework.service;

import com.jlm.homework.entity.WrongTitleBook;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IWrongTitleBookService {

    WrongTitleBook save(WrongTitleBook wrongTitleBook);

    Page<WrongTitleBook> findByStudentId(Long studentId,Integer pageNum,Integer pageSize);

    void createWrongBook(Long studentsHomeworkId);

    void addWrongBook(WrongTitleBook wrongTitleBook);

    Page<WrongTitleBook> getPage(Integer pageNum, Integer pageSize, WrongTitleBook wrongTitleBook);


}
