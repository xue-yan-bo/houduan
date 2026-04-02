package com.jlm.homework.service;

import com.jlm.homework.entity.ClassroomStudentWriteData;
import com.jlm.homework.entity.ClassroomTeacherWriteData;
import com.jlm.homework.entity.ClassroomTearcherApproveStu;

import java.util.List;

public interface IClassroomTearcherApproveStuService {
    void save(ClassroomTearcherApproveStu classroomTearcherApproveStu);
    
    void saveAll(List<ClassroomTearcherApproveStu> classroomTearcherApproveStuList);

    List<ClassroomTearcherApproveStu> findByStudentRecordId(Long studentRecordId);
    
    List<ClassroomTearcherApproveStu> findByStudentRecordIds(List<Long> studentRecordIds);

    void clearTeacherApprove(Long studentRecordId, Long studentId, Integer pageNum);

    List<ClassroomTearcherApproveStu> getTeacherApproveStuListByExercisesId(Long classroomExercisesId,Long teacherId);

    void clearTeacher2Approve(Long classroomExercisesId, Long teacherId, Integer pageNum);
}
