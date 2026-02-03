package com.jlm.homework.service.impl;

import com.jlm.homework.entity.ClassroomExercisesStudentRecord;
import com.jlm.homework.entity.ClassroomStudentWriteData;
import com.jlm.homework.entity.StudentsWriteRecord;
import com.jlm.homework.repository.ClassroomExercisesStudentRecordRepository;
import com.jlm.homework.repository.ClassroomStudentWriteDataRepository;
import com.jlm.homework.service.IClassroomStudentWriteDataService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ClassroomStudentWriteDataServiceImpl implements IClassroomStudentWriteDataService {
    private static Integer StudentWriteData_Size=800;
    @Resource
    private ClassroomStudentWriteDataRepository classroomStudentWriteDataRepository;
    @Resource
    private ClassroomExercisesStudentRecordRepository classroomExercisesStudentRecordRepository;
    @Override
    public void save(ClassroomStudentWriteData classroomStudentWriteData) {
        if(classroomStudentWriteData.getStudentRecordId()==null||classroomStudentWriteData.getStudentRecordId()==0){
            Long studentId =classroomStudentWriteData.getStudentId();
            ClassroomExercisesStudentRecord record = new ClassroomExercisesStudentRecord();
            record.setStudentId(studentId);
            Sort sort = Sort.by(Sort.Direction.DESC,"classroomExercisesId","startTime","createTime");
            List<ClassroomExercisesStudentRecord> recordList=classroomExercisesStudentRecordRepository.findAll(Example.of(record),sort);
            if(recordList!=null&&recordList.size()>0){
                classroomStudentWriteData.setStudentRecordId(recordList.get(0).getId());
            }
        }
        if(classroomStudentWriteData.getStudentsWriteRecords()==null||classroomStudentWriteData.getStudentsWriteRecords().size()==0){
            return;
        }
        if(classroomStudentWriteData.getStudentsWriteRecords().size()<StudentWriteData_Size){
            classroomStudentWriteDataRepository.save(classroomStudentWriteData);
        }else {
            ClassroomStudentWriteData data=new ClassroomStudentWriteData();
            data.setStudentRecordId(classroomStudentWriteData.getStudentRecordId());
            data.setPageNum(classroomStudentWriteData.getPageNum());
            classroomStudentWriteDataRepository.delete(data);
            for(int i=0;i<=classroomStudentWriteData.getStudentsWriteRecords().size()/StudentWriteData_Size;i++){
                ClassroomStudentWriteData writeData=new ClassroomStudentWriteData();
                writeData.setStudentRecordId(classroomStudentWriteData.getStudentRecordId());
                writeData.setStudentId(classroomStudentWriteData.getStudentId());
                writeData.setIndexN(i);
                writeData.setPageNum(classroomStudentWriteData.getPageNum());
                int end=(i+1)*StudentWriteData_Size>classroomStudentWriteData.getStudentsWriteRecords().size()?classroomStudentWriteData.getStudentsWriteRecords().size():(i+1)*StudentWriteData_Size;
                writeData.setStudentsWriteRecords(classroomStudentWriteData.getStudentsWriteRecords().subList(i*StudentWriteData_Size,end));
                writeData.setCreateTime(new Date());
                classroomStudentWriteDataRepository.save(writeData);
            }
        }
        ClassroomExercisesStudentRecord studentRecord=classroomExercisesStudentRecordRepository.findById(classroomStudentWriteData.getStudentRecordId()).orElse(null);
        if(studentRecord!=null&&(studentRecord.getHavaWrite()==null||studentRecord.getHavaWrite()!=1)){
            studentRecord.setHavaWrite(1);
            classroomExercisesStudentRecordRepository.save(studentRecord);
        }

    }

    @Override
    public List<ClassroomStudentWriteData> findByStudentRecordId(Long studentRecordId) {
        ClassroomStudentWriteData data=new ClassroomStudentWriteData();
        data.setStudentRecordId(studentRecordId);
        Sort sort = Sort.by(Sort.Direction.ASC,"pageNum","indexN");
        List<ClassroomStudentWriteData> list=classroomStudentWriteDataRepository.findAll(Example.of(data),sort);
        List<ClassroomStudentWriteData> dataList=new ArrayList<>();
        int pageNum=1;
        ClassroomStudentWriteData  studentWriteData= null;
        List<StudentsWriteRecord> studentsWriteRecords = new ArrayList<>();
        for(ClassroomStudentWriteData writeData:list){
            if(writeData.getPageNum()==pageNum){
                if(studentWriteData==null){
                    studentWriteData = new ClassroomStudentWriteData();
                    studentWriteData.setId(writeData.getId());
                    studentWriteData.setStudentRecordId(writeData.getStudentRecordId());
                    studentWriteData.setStudentId(writeData.getStudentId());
                    studentWriteData.setPageNum(pageNum);
                }
                studentsWriteRecords.addAll(writeData.getStudentsWriteRecords());
            }else{
                if(studentWriteData==null){
                    studentWriteData = new ClassroomStudentWriteData();
                    studentWriteData.setId(writeData.getId());
                    studentWriteData.setStudentRecordId(writeData.getStudentRecordId());
                    studentWriteData.setStudentId(writeData.getStudentId());
                    studentWriteData.setPageNum(writeData.getPageNum());
                    studentsWriteRecords.addAll(writeData.getStudentsWriteRecords());
                    pageNum = writeData.getPageNum();
                }else {
                    studentWriteData.setStudentsWriteRecords(studentsWriteRecords);
                    dataList.add(studentWriteData);
                    pageNum++;
                    studentWriteData = new ClassroomStudentWriteData();
                    studentWriteData.setId(writeData.getId());
                    studentWriteData.setStudentRecordId(writeData.getStudentRecordId());
                    studentWriteData.setStudentId(writeData.getStudentId());
                    studentWriteData.setPageNum(writeData.getPageNum());
                    studentsWriteRecords = new ArrayList<>();
                    studentsWriteRecords.addAll(writeData.getStudentsWriteRecords());
                    studentWriteData.setStudentsWriteRecords(studentsWriteRecords);
                }
            }
            //最后一个元素，list增加
            if(list.indexOf(writeData)==list.size()-1){
                if(studentsWriteRecords!=null&&studentsWriteRecords.size()>0){
                    studentWriteData.setStudentsWriteRecords(studentsWriteRecords);
                    dataList.add(studentWriteData);
                }
            }
        }
        return dataList;
    }
}
