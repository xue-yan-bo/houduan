package com.jlm.homework.service.impl;

import com.jlm.homework.entity.HomeworkPublish;
import com.jlm.homework.entity.Student;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.feign.StudentFeginClient;
import com.jlm.homework.repository.HomeworkPublishRepository;
import com.jlm.homework.repository.StudentsHomeworkNewRepository;
import com.jlm.homework.service.IStudentsHomeworkNewService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
                List<Student> studentList = studentFeginClient.getStudentList(1,100,classId);
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
                    studentsHomework.setAuditStatus("0");
                    studentsHomeworkNewRepository.save(studentsHomework);
                }
            }
        }
    }

    @Override
    public List<StudentsHomeworkNew> getByHomeworkPublishId(Long homeworkPublishId) {
        StudentsHomeworkNew homeworkNew = new StudentsHomeworkNew();
        homeworkNew.setHomeworkPublishId(homeworkPublishId);
        Example<StudentsHomeworkNew> example = Example.of(homeworkNew);
        List<StudentsHomeworkNew> studentsHomeworkList=studentsHomeworkNewRepository.findAll(example);
        return studentsHomeworkList;
    }
}
