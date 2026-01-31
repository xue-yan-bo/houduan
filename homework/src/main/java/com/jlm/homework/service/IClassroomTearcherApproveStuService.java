package com.jlm.homework.service;

import com.jlm.homework.entity.ClassroomStudentWriteData;
import com.jlm.homework.entity.ClassroomTeacherWriteData;
import com.jlm.homework.entity.ClassroomTearcherApproveStu;

import java.util.List;

public interface IClassroomTearcherApproveStuService {
    void save(ClassroomTearcherApproveStu classroomTearcherApproveStu);

    List<ClassroomTearcherApproveStu> findByStudentRecordId(Long studentRecordId);

    void clearTeacherApprove(Long studentRecordId, Long studentId, Integer pageNum);
}
