package com.jlm.homework.service;

import com.jlm.homework.dto.StudentVo;
import com.jlm.homework.entity.QuestionType;
import com.jlm.homework.entity.SmartDeviceUserRelation;
import com.jlm.homework.entity.Student;
import org.springframework.data.domain.Page;

public interface ISmartDeviceUserRelationService {
    Long create(SmartDeviceUserRelation deviceUserRelation);

    SmartDeviceUserRelation update(SmartDeviceUserRelation deviceUserRelation);

    void deleteById(Long id);

    Page<StudentVo> selectList(Integer pageNum, Integer pageSize, Student student);

    SmartDeviceUserRelation getById(Long id);

    Page<StudentVo> wxBindList(Integer pageNum, Integer pageSize, Student student);

    SmartDeviceUserRelation selectByDeviceCode(String mac);

    SmartDeviceUserRelation selectByIpAddress(String clientAddress);
}
