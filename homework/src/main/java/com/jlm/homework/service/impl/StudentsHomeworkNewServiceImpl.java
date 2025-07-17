package com.jlm.homework.service.impl;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.shaded.com.google.gson.JsonArray;
import com.alibaba.nacos.shaded.com.google.gson.JsonObject;
import com.jlm.homework.dto.Result;
import com.jlm.homework.dto.StudentsHomeworkRequest;
import com.jlm.homework.entity.HomeworkPublish;
import com.jlm.homework.entity.Student;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.feign.StudentFeginClient;
import com.jlm.homework.repository.HomeworkPublishRepository;
import com.jlm.homework.repository.StudentsHomeworkNewRepository;
import com.jlm.homework.service.IStudentsHomeworkNewService;
import com.jlm.homework.service.UserService;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@AllArgsConstructor
public class StudentsHomeworkNewServiceImpl implements IStudentsHomeworkNewService {
    @Resource
    private StudentsHomeworkNewRepository studentsHomeworkNewRepository;
    @Autowired
    private StudentFeginClient studentFeginClient;

    @Override
    public void createStudentsHomeworkByHomeworkPublish(HomeworkPublish homeworkPublish) {
        if(!homeworkPublish.getClassId().isEmpty()){
            for(Long classId:homeworkPublish.getClassId()){
                try {
                    Long schoolId=homeworkPublish.getSchoolId();
                    Result<Student> result = studentFeginClient.getStudentList(1,100,schoolId,classId,0);
                    if(result.getCode()!=200){
                        throw new RuntimeException(result.getMsg());
                    }
                    List<Student> studentList=result.getRows();
                    for(Student student:studentList){
                        StudentsHomeworkNew studentsHomework = new StudentsHomeworkNew();
                        studentsHomework.setClassesId(classId);
                        studentsHomework.setHomeworkType(2);
                        studentsHomework.setHomeworkPublishId(homeworkPublish.getId());
                        studentsHomework.setSchoolId(student.getSchoolId());
                        studentsHomework.setStudentId(student.getStudentId());
                        studentsHomework.setStudentName(student.getStudentName());
                        studentsHomework.setStudentUuid(student.getLinkUuid());
                        studentsHomework.setCreateTime(new Date());
                        studentsHomework.setSubmitStatus(0);
                        studentsHomework.setAuditStatus("0");
                        studentsHomeworkNewRepository.save(studentsHomework);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public List<StudentsHomeworkNew> getByHomeworkPublishId(Long homeworkPublishId, StudentsHomeworkRequest studentsHomeworkRequest) {
        StudentsHomeworkNew homeworkNew = new StudentsHomeworkNew();
        if(studentsHomeworkRequest!=null){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if(StringUtils.isNotEmpty(studentsHomeworkRequest.getStudentName())){
                homeworkNew.setStudentName(studentsHomeworkRequest.getStudentName());
            }
            if(studentsHomeworkRequest.getSubmitStatus()!=null){
                homeworkNew.setSubmitStatus(studentsHomeworkRequest.getSubmitStatus());
            }
            try {
                if(StringUtils.isNotEmpty(studentsHomeworkRequest.getSubmitTime())){
                    homeworkNew.setSubmitTime(sdf.parse(studentsHomeworkRequest.getSubmitTime()));
                }
                if(StringUtils.isNotEmpty(studentsHomeworkRequest.getAuditTime())){
                    homeworkNew.setAuditTime(sdf.parse(studentsHomeworkRequest.getAuditTime()));
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        homeworkNew.setHomeworkPublishId(homeworkPublishId);
        Example<StudentsHomeworkNew> example = Example.of(homeworkNew);
        List<StudentsHomeworkNew> studentsHomeworkList=studentsHomeworkNewRepository.findAll(example);
        return studentsHomeworkList;
    }

    @Override
    public StudentsHomeworkNew update(StudentsHomeworkNew studentsHomework) {
        return studentsHomeworkNewRepository.save(studentsHomework);
    }
}
