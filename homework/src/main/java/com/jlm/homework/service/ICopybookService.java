package com.jlm.homework.service;

import com.jlm.homework.entity.Copybook;
import org.springframework.data.domain.Page;

public interface ICopybookService {
    Long create(Copybook copybook);

    Copybook getById(Long id);

    Copybook update(Copybook copybook);

    Page<Copybook> selectList(Integer pageNum, Integer pageSize, Copybook copybook);

    void deleteById(Long id);

    Boolean publish(Long id);

    Page<Copybook> queryListByTeacher(Integer pageNum, Integer pageSize, Long teacherId);
}
