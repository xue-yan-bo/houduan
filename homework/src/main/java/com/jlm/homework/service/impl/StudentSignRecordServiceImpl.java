package com.jlm.homework.service.impl;

import com.jlm.homework.entity.StudentSignRecord;
import com.jlm.homework.entity.TeacherAttendanceRecord;
import com.jlm.homework.repository.StudentSignRecordRepository;
import com.jlm.homework.repository.TeacherAttendanceRecordRepository;
import com.jlm.homework.service.IStudentSignRecordService;
import com.jlm.homework.util.SseManagerUtil;
import com.jlm.homework.util.StringUtils;
import jakarta.annotation.Resource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class StudentSignRecordServiceImpl  implements IStudentSignRecordService {
    @Resource
    private StudentSignRecordRepository studentSignRecordRepository;
    @Resource
    private TeacherAttendanceRecordRepository teacherAttendanceRecordRepository;
    @Resource
    private SseManagerUtil sseManagerUtil;
    @Override
    public StudentSignRecord sign(StudentSignRecord studentSignRecord) {
        Date now = new Date();
        if(studentSignRecord.getAttendanceRecordId()==null){
            throw new RuntimeException("老师考勤ID不能为空！");
        }
        Optional<TeacherAttendanceRecord> optional =teacherAttendanceRecordRepository.findById(studentSignRecord.getAttendanceRecordId());
        TeacherAttendanceRecord attendanceRecord= null;
        if(optional != null && optional.isPresent()){
            attendanceRecord=optional.get();
        }
        if(attendanceRecord==null){
            throw new RuntimeException("老师考勤记录不存在！");
        }
        if(attendanceRecord.getEndTime()!=null&&attendanceRecord.getEndTime().before(now)){
            throw new RuntimeException("考勤已经结束，不能签到了！");
        }
        if(studentSignRecord.getId()==null){
            StudentSignRecord search=new StudentSignRecord();
            search.setAttendanceRecordId(studentSignRecord.getAttendanceRecordId());
            search.setStudentId(studentSignRecord.getStudentId());
            StudentSignRecord old=studentSignRecordRepository.findOne(Example.of(search)).get();
            studentSignRecord.setId(old.getId());
        }

        studentSignRecord.setSignTime(now);
        studentSignRecord.setSignFlag(1);
        studentSignRecord.setCreateTime(now);

        Integer signNum=attendanceRecord.getSignNum()+1;
        Integer unsignNum=attendanceRecord.getUnsignNum()-1;
        attendanceRecord.setSignNum(signNum);
        attendanceRecord.setUnsignNum(unsignNum);
        attendanceRecord.setTeacherId(studentSignRecord.getTeacherId());
        attendanceRecord.setTeacherName(studentSignRecord.getTeacherName());
        studentSignRecord =studentSignRecordRepository.save(studentSignRecord);
        teacherAttendanceRecordRepository.save(attendanceRecord);
        //sseManagerUtil.sendMsgToClient("qiandao"+studentSignRecord.getAttendanceRecordId(), studentSignRecord.getStudentId().toString());
        return studentSignRecord;
    }

    @Override
    public Page<StudentSignRecord> queryList(Integer pageNum, Integer pageSize, Long attendanceRecordId,String studentName,Integer signFlag) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.ASC, "signTime");
        Pageable pageable;
        pageable = PageRequest.of(pageNum, pageSize, sort);
        StudentSignRecord record = new StudentSignRecord();
        record.setAttendanceRecordId(attendanceRecordId);
        if(StringUtils.isNotEmpty(studentName)) {
            record.setStudentName(studentName);
        }
        if(signFlag!=null){
            record.setSignFlag(signFlag);
        }
        return studentSignRecordRepository.findAll(Example.of(record),pageable);
    }
}
