package com.jlm.homework.service.impl;

import com.jlm.homework.entity.ClassroomVideoRecord;
import com.jlm.homework.repository.ClassroomVideoRecordRepository;
import com.jlm.homework.service.IClassroomVideoRecordService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
public class ClassroomVideoRecordServiceImpl implements IClassroomVideoRecordService {
    @Resource
    private ClassroomVideoRecordRepository  classroomVideoRecordRepository;

    @Override
    public Long create(ClassroomVideoRecord classroomVideoRecord) {
        return classroomVideoRecordRepository.save(classroomVideoRecord).getId();
    }

    @Override
    public ClassroomVideoRecord getById(Long id) {
        return classroomVideoRecordRepository.getReferenceById(id);
    }

    @Override
    public ClassroomVideoRecord update(ClassroomVideoRecord classroomVideoRecord) {
        return classroomVideoRecordRepository.save(classroomVideoRecord);
    }

    @Override
    public Page<ClassroomVideoRecord> selectList(Integer pageNum, Integer pageSize, ClassroomVideoRecord classroomVideoRecord) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "startTime");
        Pageable pageable;
        pageable = PageRequest.of(pageNum, pageSize, sort);
        return classroomVideoRecordRepository.findAll(Example.of(classroomVideoRecord),pageable);
    }

    @Override
    public void deleteById(Long id) {
        classroomVideoRecordRepository.deleteById(id);
    }
}
