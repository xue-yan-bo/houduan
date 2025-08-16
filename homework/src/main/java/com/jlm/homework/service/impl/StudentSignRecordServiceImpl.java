package com.jlm.homework.service.impl;

import com.jlm.homework.entity.StudentSignRecord;
import com.jlm.homework.entity.TeacherAttendanceRecord;
import com.jlm.homework.repository.StudentSignRecordRepository;
import com.jlm.homework.repository.TeacherAttendanceRecordRepository;
import com.jlm.homework.service.IStudentSignRecordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class StudentSignRecordServiceImpl  implements IStudentSignRecordService {
    @Resource
    private StudentSignRecordRepository studentSignRecordRepository;
    @Resource
    private TeacherAttendanceRecordRepository teacherAttendanceRecordRepository;
    @Override
    public StudentSignRecord sign(StudentSignRecord studentSignRecord) {
        Date now = new Date();
        TeacherAttendanceRecord attendanceRecord=teacherAttendanceRecordRepository.findById(studentSignRecord.getAttendanceRecordId()).get();
        if(attendanceRecord.getEndTime()!=null&&attendanceRecord.getEndTime().before(now)){
            throw new RuntimeException("考勤已经结束，不能签到了！");
        }
        studentSignRecord.setSignTime(now);
        studentSignRecord.setSignFlag(1);

        Integer signNum=attendanceRecord.getSignNum()+1;
        Integer unsignNum=attendanceRecord.getUnsignNum()-1;
        attendanceRecord.setSignNum(signNum);
        attendanceRecord.setUnsignNum(unsignNum);
        studentSignRecord =studentSignRecordRepository.save(studentSignRecord);
        teacherAttendanceRecordRepository.save(attendanceRecord);
        return studentSignRecord;
    }
}
