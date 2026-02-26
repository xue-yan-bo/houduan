package com.jlm.homework.service.impl;

import com.jlm.homework.entity.HomeworkStudentWriteData;
import com.jlm.homework.entity.WrongTitleWriteData;
import com.jlm.homework.repository.WrongTitleWriteDataRepository;
import com.jlm.homework.service.IWrongTitleWriteDataService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class WrongTitleWriteDataServiceImpl implements IWrongTitleWriteDataService {
    private static Integer StudentWriteData_Size=500;
    @Resource
    private WrongTitleWriteDataRepository wrongTitleWriteDataRepository;
    @Override
    public void save(WrongTitleWriteData wrongTitleWriteData) {
        wrongTitleWriteDataRepository.save(wrongTitleWriteData);

    }

    @Override
    public WrongTitleWriteData getById(Long wrongTitleId) {
        return wrongTitleWriteDataRepository.findById(wrongTitleId).orElse(null);
    }
}
