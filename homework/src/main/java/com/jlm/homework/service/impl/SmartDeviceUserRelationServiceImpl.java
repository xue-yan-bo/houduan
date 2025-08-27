package com.jlm.homework.service.impl;

import com.jlm.homework.dto.Result;
import com.jlm.homework.dto.StudentVo;
import com.jlm.homework.entity.QuestionType;
import com.jlm.homework.entity.SmartDeviceUserRelation;
import com.jlm.homework.entity.Student;
import com.jlm.homework.feign.StudentFeginClient;
import com.jlm.homework.repository.SmartDeviceUserRelationRepository;
import com.jlm.homework.service.ISmartDeviceUserRelationService;
import com.jlm.homework.service.UserService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SmartDeviceUserRelationServiceImpl implements ISmartDeviceUserRelationService {
    @Resource
    private SmartDeviceUserRelationRepository smartDeviceUserRelationRepository;
    @Autowired
    private StudentFeginClient studentFeginClient;
    @Autowired
    private UserService userService;
    @Override
    public Long create(SmartDeviceUserRelation deviceUserRelation) {
        if(StringUtils.isNotEmpty(deviceUserRelation.getDeviceCode())){
            SmartDeviceUserRelation relation=new SmartDeviceUserRelation();
            relation.setDeviceCode(deviceUserRelation.getDeviceCode());
            List<SmartDeviceUserRelation> list=smartDeviceUserRelationRepository.findAll(Example.of(relation));
            if(list.size()>0){
                throw new RuntimeException("该设备"+deviceUserRelation.getDeviceCode()+"已绑定"+list.get(0).getUserName()+"，请检测！");
            }
        }
        if(StringUtils.isNotEmpty(deviceUserRelation.getUserId())){
            SmartDeviceUserRelation relation=new SmartDeviceUserRelation();
            relation.setUserId(deviceUserRelation.getUserId());
            List<SmartDeviceUserRelation> list=smartDeviceUserRelationRepository.findAll(Example.of(relation));
            if(list.size()>0){
                throw new RuntimeException("该用户"+deviceUserRelation.getUserName()+"已绑定设备"+list.get(0).getDeviceCode()+"，请检测！");
            }
        }
        return smartDeviceUserRelationRepository.save(deviceUserRelation).getId();
    }

    @Override
    public SmartDeviceUserRelation update(SmartDeviceUserRelation deviceUserRelation) {
        return smartDeviceUserRelationRepository.save(deviceUserRelation);
    }

    @Override
    public void deleteById(Long id) {
        smartDeviceUserRelationRepository.deleteById(id);
    }

    @Override
    public Page<StudentVo> selectList(Integer pageNum, Integer pageSize, Student student) {
        pageNum= pageNum==null?1:pageNum;
        pageSize= pageSize==null?10:pageSize;
        if(student==null){
            student=new Student();
            //student.setSchoolId(userService.getCurrentSchoolIdSafely());
            student.setSchoolId(224l);
        }
        if(student.getSchoolId()==null){
            //student.setSchoolId(userService.getCurrentSchoolIdSafely());
            student.setSchoolId(224l);
        }
        Result<Student> result= studentFeginClient.getStudentList(pageNum,pageSize,student.getSchoolId(),student.getGradeId(),student.getClassesId(),student.getStudentStatus());
        List<Student> studentList=result.getRows();
        List<StudentVo> studentVoList=new ArrayList<>();
        Pageable pageable = pageable = PageRequest.of(pageNum, pageSize);
        if(studentList==null&&studentList.isEmpty()){
            return null;
        }
        for(Student student1:studentList){
            StudentVo studentVo=StudentVo.studentToVo(student1);
            SmartDeviceUserRelation deviceUserRelation=new SmartDeviceUserRelation();
            deviceUserRelation.setUserId(student1.getStudentId().toString());
            deviceUserRelation.setUserType(1);
            Optional<SmartDeviceUserRelation> optional=smartDeviceUserRelationRepository.findOne(Example.of(deviceUserRelation));
            if(!optional.isEmpty()) {
                SmartDeviceUserRelation relation=optional.get();
                if (relation != null) {
                    studentVo.setDeviceCode(relation.getDeviceCode());
                    studentVo.setDeviceUserRelationId(relation.getId());
                }

            }
            studentVoList.add(studentVo);
        }
        Page<StudentVo> page = new PageImpl(studentVoList,pageable,result.getTotal());
        return page;
    }

    @Override
    public SmartDeviceUserRelation getById(Long id) {
        return smartDeviceUserRelationRepository.getById(id);
    }

    @Override
    public Page<StudentVo> wxBindList(Integer pageNum, Integer pageSize, Student student) {
        Result<Student> result= studentFeginClient.getStudentList(pageNum,pageSize,student.getSchoolId(),student.getGradeId(),student.getClassesId(),student.getStudentStatus());
        List<Student> studentList=result.getRows();
        List<StudentVo> studentVoList=new ArrayList<>();
        Pageable pageable = pageable = PageRequest.of(pageNum, pageSize);
        for(Student student1:studentList){
            StudentVo studentVo=StudentVo.studentToVo(student1);


            studentVoList.add(studentVo);


        }
        Page<StudentVo> page = new PageImpl(studentVoList,pageable,result.getTotal());
        return page;
    }

    @Override
    public SmartDeviceUserRelation selectByDeviceCode(String mac) {
        SmartDeviceUserRelation relation=new SmartDeviceUserRelation();
        relation.setDeviceCode(mac);
        SmartDeviceUserRelation deviceUserRelation=smartDeviceUserRelationRepository.findOne(Example.of(relation)).get();
        return deviceUserRelation;
    }

    @Override
    public SmartDeviceUserRelation selectByIpAddress(String clientAddress) {
        SmartDeviceUserRelation relation=new SmartDeviceUserRelation();
        relation.setIpAddress(clientAddress);
        Optional<SmartDeviceUserRelation> optional=smartDeviceUserRelationRepository.findOne(Example.of(relation));
        SmartDeviceUserRelation deviceUserRelation= null;
        if(!optional.isEmpty()){
             deviceUserRelation=optional.get();
        }
        return deviceUserRelation;
    }
}
