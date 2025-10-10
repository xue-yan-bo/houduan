package com.jlm.homework.service.impl;

import com.jlm.homework.dto.Result;
import com.jlm.homework.entity.CurrentUserInfo;
import com.jlm.homework.entity.Student;
import com.jlm.homework.entity.StudentSignRecord;
import com.jlm.homework.entity.TeacherAttendanceRecord;
import com.jlm.homework.feign.StudentFeignClient;
import com.jlm.homework.repository.StudentSignRecordRepository;
import com.jlm.homework.repository.TeacherAttendanceRecordRepository;

import com.jlm.homework.service.ITeacherAttendanceRecordService;
import com.jlm.homework.service.IUserService;
import com.jlm.homework.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class TeacherAttendanceRecordServiceImpl implements ITeacherAttendanceRecordService {
    @Resource
    private TeacherAttendanceRecordRepository teacherAttendanceRecordRepository;
    @Resource
    private StudentSignRecordRepository  studentSignRecordRepository;
    @Autowired
    private StudentFeignClient studentFeignClient;
    @Autowired
    private IUserService userService;
    @Override
    public TeacherAttendanceRecord startAttendance(Long classId,Long schoolId,String subject) {
        TeacherAttendanceRecord record = new TeacherAttendanceRecord();
        try {
            CurrentUserInfo userInfo = userService.getCurrentUserInfo();
            if(schoolId==null){
                schoolId=userService.getCurrentSchoolIdSafely();
            }
            record.setClassId(classId);
            if(userInfo!=null&&userService.isCurrentUserTeacher()){
                record.setTeacherId(userInfo.getUserUuid());
                record.setTeacherName(userInfo.getTeacherName());
            }
            Date now = new Date();
            record.setCreateTime(now);
            Result<Student> result = studentFeignClient.getStudentList(1,200,schoolId,null,classId,"0");
            if(result.getCode()!=200){
                throw new RuntimeException(result.getMsg());
            }
            List<Student> studentList=result.getRows();
            if(studentList.size()==0){
                throw new RuntimeException("该班级还没有学生呢，请检查！");
            }
            record.setStudentSum(studentList.size());
            record.setDay(now);
            record.setClassName(studentList.get(0).getClassesName());
            record.setSubject(subject);
            record.setSignNum(0);
            record.setUnsignNum(studentList.size());
            record.setStartTime(now);
            record.setCreateTime(now);
            record = teacherAttendanceRecordRepository.save(record);
            List<StudentSignRecord> studentSignRecordList = new ArrayList();
            for(Student student:studentList){
                StudentSignRecord studentSignRecord=new StudentSignRecord();
                studentSignRecord.setAttendanceRecordId(record.getId());
                studentSignRecord.setClassId(classId);
                studentSignRecord.setClassName(record.getClassName());
                studentSignRecord.setTeacherId(record.getTeacherId());
                studentSignRecord.setTeacherName(record.getTeacherName());
                studentSignRecord.setStudentId(student.getStudentId());
                studentSignRecord.setStudentName(student.getStudentName());
                studentSignRecord.setSignFlag(0);
                studentSignRecord.setCreateTime(now);
                studentSignRecord=studentSignRecordRepository.save(studentSignRecord);

                studentSignRecordList.add(studentSignRecord);
            }
            record.setStudentSignRecordList(studentSignRecordList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return record;
    }

    @Override
    public TeacherAttendanceRecord endAttendance(Long attendanceRecordId) {
        TeacherAttendanceRecord record = teacherAttendanceRecordRepository.findById(attendanceRecordId).get();
        Date now = new Date();
        record.setEndTime(now);
        return record;
    }

    @Override
    public Page<TeacherAttendanceRecord> queryList(Integer pageNum, Integer pageSize, TeacherAttendanceRecord teacherAttendanceRecord) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "startTime");
        Pageable pageable;
        pageable = PageRequest.of(pageNum, pageSize, sort);
        return teacherAttendanceRecordRepository.findAll(Example.of(teacherAttendanceRecord),pageable);
    }
}
