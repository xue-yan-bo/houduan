package com.jlm.homework.service.impl;

import com.jlm.homework.entity.*;
import com.jlm.homework.repository.ClassroomExercisesStudentRecordRepository;
import com.jlm.homework.repository.ClassroomStudentWriteDataRepository;
import com.jlm.homework.repository.ClassroomTearcherApproveStuRepository;
import com.jlm.homework.service.IClassroomTearcherApproveStuService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClassroomTearcherApproveStuServiceImpl implements IClassroomTearcherApproveStuService {
    private static Integer StudentWriteData_Size=800;
    @Resource
    private ClassroomTearcherApproveStuRepository classroomTearcherApproveStuRepository;
    @Resource
    private ClassroomExercisesStudentRecordRepository classroomExercisesStudentRecordRepository;
    @Override
    public void save(ClassroomTearcherApproveStu classroomTearcherApproveStu) {
        if(classroomTearcherApproveStu.getStudentRecordId()==null||classroomTearcherApproveStu.getStudentRecordId()==0){
            Long studentId =classroomTearcherApproveStu.getStudentId();
            ClassroomExercisesStudentRecord record = new ClassroomExercisesStudentRecord();
            record.setStudentId(studentId);
            Sort sort = Sort.by(Sort.Direction.DESC,"classroomExercisesId","startTime","createTime");
            List<ClassroomExercisesStudentRecord> recordList=classroomExercisesStudentRecordRepository.findAll(Example.of(record),sort);
            if(recordList!=null&&recordList.size()>0){
                classroomTearcherApproveStu.setStudentRecordId(recordList.get(0).getId());
            }
        }
        if(classroomTearcherApproveStu.getTearcherApprStuData()==null||classroomTearcherApproveStu.getTearcherApprStuData().size()==0){
            return;
        }
        if(classroomTearcherApproveStu.getTearcherApprStuData().size()<StudentWriteData_Size){
            classroomTearcherApproveStu.setIndexN(0);
            classroomTearcherApproveStu.setCreateTime(new Date());
            classroomTearcherApproveStuRepository.save(classroomTearcherApproveStu);
        }else {
            ClassroomTearcherApproveStu data=new ClassroomTearcherApproveStu();
            data.setStudentRecordId(classroomTearcherApproveStu.getStudentRecordId());
            data.setPageNum(classroomTearcherApproveStu.getPageNum());
            classroomTearcherApproveStuRepository.delete(data);
            for(int i=0;i<=classroomTearcherApproveStu.getTearcherApprStuData().size()/StudentWriteData_Size;i++){
                ClassroomTearcherApproveStu writeData=new ClassroomTearcherApproveStu();
                writeData.setStudentRecordId(classroomTearcherApproveStu.getStudentRecordId());
                writeData.setStudentId(classroomTearcherApproveStu.getStudentId());
                writeData.setIndexN(i);
                writeData.setPageNum(classroomTearcherApproveStu.getPageNum());
                int end=(i+1)*StudentWriteData_Size>classroomTearcherApproveStu.getTearcherApprStuData().size()?classroomTearcherApproveStu.getTearcherApprStuData().size():(i+1)*StudentWriteData_Size;
                writeData.setTearcherApprStuData(classroomTearcherApproveStu.getTearcherApprStuData().subList(i*StudentWriteData_Size,end));
                writeData.setCreateTime(new Date());
                classroomTearcherApproveStuRepository.save(writeData);
            }
        }

    }

    @Override
    public void saveAll(List<ClassroomTearcherApproveStu> classroomTearcherApproveStuList) {
        if (classroomTearcherApproveStuList == null || classroomTearcherApproveStuList.isEmpty()) {
            return;
        }
        
        for (ClassroomTearcherApproveStu classroomTearcherApproveStu : classroomTearcherApproveStuList) {
            save(classroomTearcherApproveStu);
        }
    }

    @Override
    public List<ClassroomTearcherApproveStu> findByStudentRecordId(Long studentRecordId) {
        ClassroomTearcherApproveStu data=new ClassroomTearcherApproveStu();
        data.setStudentRecordId(studentRecordId);
        Sort sort = Sort.by(Sort.Direction.ASC,"pageNum","indexN","createTime");
        List<ClassroomTearcherApproveStu> list=classroomTearcherApproveStuRepository.findAll(Example.of(data),sort);
        return groupApproveStuByPageNum(list);
    }

    @Override
    public List<ClassroomTearcherApproveStu> findByStudentRecordIds(List<Long> studentRecordIds) {
        if (studentRecordIds == null || studentRecordIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 构建查询条件
        List<ClassroomTearcherApproveStu> allApproveStu = new ArrayList<>();
        for (Long studentRecordId : studentRecordIds) {
            ClassroomTearcherApproveStu data = new ClassroomTearcherApproveStu();
            data.setStudentRecordId(studentRecordId);
            Sort sort = Sort.by(Sort.Direction.ASC, "pageNum", "indexN", "createTime");
            List<ClassroomTearcherApproveStu> list = classroomTearcherApproveStuRepository.findAll(Example.of(data), sort);
            allApproveStu.addAll(list);
        }
        
        return groupApproveStuByPageNum(allApproveStu);
    }

    /**
     * 按页码分组审批数据
     * @param list 审批数据列表
     * @return 分组后的数据列表
     */
    private List<ClassroomTearcherApproveStu> groupApproveStuByPageNum(List<ClassroomTearcherApproveStu> list) {
        List<ClassroomTearcherApproveStu> dataList = new ArrayList<>();
        if (list == null || list.isEmpty()) {
            return dataList;
        }
        
        // 按学生记录 ID 分组
        Map<Long, List<ClassroomTearcherApproveStu>> recordIdMap = new HashMap<>();
        for (ClassroomTearcherApproveStu writeData : list) {
            recordIdMap.computeIfAbsent(writeData.getStudentRecordId(), k -> new ArrayList<>()).add(writeData);
        }
        
        // 对每个学生记录的数据进行分页分组
        for (List<ClassroomTearcherApproveStu> recordApproveStu : recordIdMap.values()) {
            int pageNum = 1;
            ClassroomTearcherApproveStu studentWriteData = null;
            List<TeacherApprWriteRecord> studentsWriteRecords = new ArrayList<>();
            
            for (ClassroomTearcherApproveStu writeData : recordApproveStu) {
                if (writeData.getPageNum() == pageNum) {
                    if (studentWriteData == null) {
                        studentWriteData = new ClassroomTearcherApproveStu();
                        studentWriteData.setId(writeData.getId());
                        studentWriteData.setStudentRecordId(writeData.getStudentRecordId());
                        studentWriteData.setStudentId(writeData.getStudentId());
                        studentWriteData.setPageNum(pageNum);
                    }
                    studentsWriteRecords.addAll(writeData.getTearcherApprStuData());
                } else {
                    if (studentWriteData == null) {
                        studentWriteData = new ClassroomTearcherApproveStu();
                        studentWriteData.setId(writeData.getId());
                        studentWriteData.setStudentRecordId(writeData.getStudentRecordId());
                        studentWriteData.setStudentId(writeData.getStudentId());
                        studentWriteData.setPageNum(writeData.getPageNum());
                        studentsWriteRecords.addAll(writeData.getTearcherApprStuData());
                        pageNum = writeData.getPageNum();
                    } else {
                        studentWriteData.setTearcherApprStuData(studentsWriteRecords);
                        dataList.add(studentWriteData);
                        pageNum++;
                        studentWriteData = new ClassroomTearcherApproveStu();
                        studentWriteData.setId(writeData.getId());
                        studentWriteData.setStudentRecordId(writeData.getStudentRecordId());
                        studentWriteData.setStudentId(writeData.getStudentId());
                        studentWriteData.setPageNum(writeData.getPageNum());
                        studentsWriteRecords = new ArrayList<>();
                        studentsWriteRecords.addAll(writeData.getTearcherApprStuData());
                        studentWriteData.setTearcherApprStuData(studentsWriteRecords);
                    }
                }
                // 最后一个元素，添加到列表
                if (recordApproveStu.indexOf(writeData) == recordApproveStu.size() - 1) {
                    if (studentsWriteRecords != null && studentsWriteRecords.size() > 0) {
                        studentWriteData.setTearcherApprStuData(studentsWriteRecords);
                        dataList.add(studentWriteData);
                    }
                }
            }
        }
        
        return dataList;
    }

    @Override
    public void clearTeacherApprove(Long classroomExercisesId, Long studentId, Integer pageNum) {
        ClassroomExercisesStudentRecord search = new ClassroomExercisesStudentRecord();
        search.setClassroomExercisesId(classroomExercisesId);
        search.setStudentId(studentId);
        ClassroomExercisesStudentRecord record=classroomExercisesStudentRecordRepository.findOne(Example.of(search)).orElse(null);
        if(record==null){
            return;
        }
        ClassroomTearcherApproveStu data=new ClassroomTearcherApproveStu();
        data.setStudentRecordId(record.getId());
        data.setStudentId(studentId);
        data.setPageNum(pageNum);
        Sort sort = Sort.by(Sort.Direction.ASC,"pageNum","indexN","createTime");
        List<ClassroomTearcherApproveStu> list=classroomTearcherApproveStuRepository.findAll(Example.of(data),sort);
        if(list!=null&&list.size()>0){
            for(ClassroomTearcherApproveStu approveStu:list){
                classroomTearcherApproveStuRepository.deleteById(approveStu.getId());
            }
        }
    }

    @Override
    public List<ClassroomTearcherApproveStu> getTeacherApproveStuListByExercisesId(Long classroomExercisesId,Long teacherId) {
        ClassroomTearcherApproveStu data=new ClassroomTearcherApproveStu();
        data.setClassroomExercisesId(classroomExercisesId);
        data.setTeacherId(teacherId);
        Sort sort = Sort.by(Sort.Direction.ASC,"pageNum","indexN","createTime");
        List<ClassroomTearcherApproveStu> list=classroomTearcherApproveStuRepository.findAll(Example.of(data),sort);
        return list;
    }

    @Override
    public void clearTeacher2Approve(Long classroomExercisesId, Long teacherId, Integer pageNum) {
        ClassroomTearcherApproveStu data=new ClassroomTearcherApproveStu();
        data.setClassroomExercisesId(classroomExercisesId);
        data.setTeacherId(teacherId);
        data.setPageNum(pageNum);
        Sort sort = Sort.by(Sort.Direction.ASC,"pageNum","indexN","createTime");
        List<ClassroomTearcherApproveStu> list=classroomTearcherApproveStuRepository.findAll(Example.of(data),sort);
        if(list!=null&&list.size()>0){
            for(ClassroomTearcherApproveStu approveStu:list){
                classroomTearcherApproveStuRepository.deleteById(approveStu.getId());
            }
        }
    }
}
