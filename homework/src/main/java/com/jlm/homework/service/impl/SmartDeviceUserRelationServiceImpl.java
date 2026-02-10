package com.jlm.homework.service.impl;

import com.jlm.homework.dto.Result;
import com.jlm.homework.dto.StudentVo;
import com.jlm.homework.entity.SmartDeviceUserRelation;
import com.jlm.homework.entity.Student;
import com.jlm.homework.feign.StudentFeignClient;
import com.jlm.homework.repository.SmartDeviceUserRelationRepository;
import com.jlm.homework.service.ISmartDeviceUserRelationService;
import com.jlm.homework.service.IUserService;
import jakarta.annotation.Resource;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SmartDeviceUserRelationServiceImpl implements ISmartDeviceUserRelationService {
    @Resource
    private SmartDeviceUserRelationRepository smartDeviceUserRelationRepository;
    @Autowired
    private StudentFeignClient studentFeignClient;
    @Autowired
    private IUserService userService;
    @Override
    public Long create(SmartDeviceUserRelation deviceUserRelation) {
        if(StringUtils.isNotEmpty(deviceUserRelation.getDeviceCode())){
            SmartDeviceUserRelation relation=new SmartDeviceUserRelation();
            relation.setDeviceCode(deviceUserRelation.getDeviceCode());
            relation.setUserType(null);
            List<SmartDeviceUserRelation> list=smartDeviceUserRelationRepository.findAll(Example.of(relation));
            if(list.size()>0){
                throw new RuntimeException("该设备"+deviceUserRelation.getDeviceCode()+"已绑定"+list.get(0).getUserName()+"，请检测！");
            }
        }
        if(StringUtils.isNotEmpty(deviceUserRelation.getUserId())){
            SmartDeviceUserRelation relation=new SmartDeviceUserRelation();
            relation.setUserId(deviceUserRelation.getUserId());
            relation.setUserType(null);
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
            student.setSchoolId(userService.getCurrentSchoolIdSafely());
            //student.setSchoolId(224l);
        }
        if(student.getSchoolId()==null){
            student.setSchoolId(userService.getCurrentSchoolIdSafely());
            //student.setSchoolId(224l);
        }
        Result<Student> result= studentFeignClient.getStudentList(pageNum,pageSize,student.getSchoolId(),student.getGradeId(),student.getClassesId(),student.getStudentStatus());
        List<Student> studentList=result.getRows();
        List<StudentVo> studentVoList=new ArrayList<>();
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        if(studentList==null||studentList.isEmpty()){
            return new PageImpl<>(studentVoList, pageable, 0);
        }
        // 批量查询所有学生的设备绑定关系，避免 N+1 查询
        List<String> studentIds = studentList.stream()
                .map(s -> s.getStudentId().toString())
                .collect(Collectors.toList());
        Map<String, SmartDeviceUserRelation> relationMap = smartDeviceUserRelationRepository
                .findByUserIdInAndUserType(studentIds, 1)
                .stream()
                .collect(Collectors.toMap(SmartDeviceUserRelation::getUserId, r -> r, (a, b) -> a));
        for(Student student1:studentList){
            StudentVo studentVo=StudentVo.studentToVo(student1);
            SmartDeviceUserRelation relation = relationMap.get(student1.getStudentId().toString());
            if (relation != null) {
                studentVo.setDeviceCode(relation.getDeviceCode());
                studentVo.setDeviceUserRelationId(relation.getId());
            }
            studentVoList.add(studentVo);
        }
        Page<StudentVo> page = new PageImpl<>(studentVoList,pageable,result.getTotal());
        return page;
    }

    @Override
    public SmartDeviceUserRelation getById(Long id) {
        return smartDeviceUserRelationRepository.getById(id);
    }

    @Override
    public Page<StudentVo> wxBindList(Integer pageNum, Integer pageSize, Student student) {
        Result<Student> result= studentFeignClient.getStudentList(pageNum,pageSize,student.getSchoolId(),student.getGradeId(),student.getClassesId(),student.getStudentStatus());
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
        relation.setUserType(null);
        Optional<SmartDeviceUserRelation> optional=smartDeviceUserRelationRepository.findOne(Example.of(relation));
        SmartDeviceUserRelation deviceUserRelation= null;
        if(optional!=null&&!optional.isEmpty()){
            deviceUserRelation =  optional.get();
        }
        return deviceUserRelation;
    }

    @Override
    public SmartDeviceUserRelation selectByIpAddress(String clientAddress) {
        SmartDeviceUserRelation relation=new SmartDeviceUserRelation();
        relation.setIpAddress(clientAddress);
        relation.setUserType(null);
        Optional<SmartDeviceUserRelation> optional=smartDeviceUserRelationRepository.findOne(Example.of(relation));
        SmartDeviceUserRelation deviceUserRelation= null;
        if(!optional.isEmpty()){
             deviceUserRelation=optional.get();
        }
        return deviceUserRelation;
    }

    @Override
    public SmartDeviceUserRelation getByUseId(String userId) {
        SmartDeviceUserRelation relation=new SmartDeviceUserRelation();
        relation.setUserId(userId);
        relation.setUserType(null);
        Optional<SmartDeviceUserRelation> optional=smartDeviceUserRelationRepository.findOne(Example.of(relation));
        SmartDeviceUserRelation deviceUserRelation= null;
        if(!optional.isEmpty()){
            deviceUserRelation=optional.get();
        }
        return deviceUserRelation;
    }
}
