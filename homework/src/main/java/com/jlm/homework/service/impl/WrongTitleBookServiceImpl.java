package com.jlm.homework.service.impl;

import com.jlm.homework.entity.WrongTitleBook;
import com.jlm.homework.repository.WrongTitleBookRepository;
import com.jlm.homework.service.IWrongTitleBookService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WrongTitleBookServiceImpl implements IWrongTitleBookService {
    @Resource
    private WrongTitleBookRepository wrongTitleBookRepository;

    @Override
    public WrongTitleBook save(WrongTitleBook wrongTitleBook) {
        return wrongTitleBookRepository.save(wrongTitleBook);
    }

    @Override
    public List<WrongTitleBook> findByStudentId(Long studentId) {
        WrongTitleBook search = new WrongTitleBook();
        search.setStudentId(studentId);
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        return wrongTitleBookRepository.findAll(Example.of(search),sort);
    }
}
