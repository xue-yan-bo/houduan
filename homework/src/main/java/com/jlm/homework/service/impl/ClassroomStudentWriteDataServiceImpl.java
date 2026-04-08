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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        if(classroomStudentWriteData.getStudentsWriteRecords()==null||classroomStudentWriteData.getStudentsWriteRecords().size()<=0){
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
    @Transactional(rollbackFor = Exception.class)
    public void saveAll(List<ClassroomStudentWriteData> list) {
        if(list == null || list.isEmpty()){
            return;
        }

        List<ClassroomStudentWriteData> toSave = new ArrayList<>();
        List<ClassroomStudentWriteData> toDelete = new ArrayList<>();
        List<ClassroomExercisesStudentRecord> recordsToUpdate = new ArrayList<>();

        List<Long> studentRecordIds = list.stream()
                .map(ClassroomStudentWriteData::getStudentRecordId)
                .filter(id -> id != null && id != 0)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, ClassroomExercisesStudentRecord> recordMap = new HashMap<>();
        if (!studentRecordIds.isEmpty()) {
            List<ClassroomExercisesStudentRecord> records = classroomExercisesStudentRecordRepository.findAllById(studentRecordIds);
            for (ClassroomExercisesStudentRecord record : records) {
                recordMap.put(record.getId(), record);
            }
        }

        for (ClassroomStudentWriteData classroomStudentWriteData : list) {
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
            if(classroomStudentWriteData.getStudentsWriteRecords()==null||classroomStudentWriteData.getStudentsWriteRecords().size()<=0){
                continue;
            }
            if(classroomStudentWriteData.getStudentsWriteRecords().size()<StudentWriteData_Size){
                toSave.add(classroomStudentWriteData);
            }else {
                ClassroomStudentWriteData data=new ClassroomStudentWriteData();
                data.setStudentRecordId(classroomStudentWriteData.getStudentRecordId());
                data.setPageNum(classroomStudentWriteData.getPageNum());
                toDelete.add(data);

                for(int i=0;i<=classroomStudentWriteData.getStudentsWriteRecords().size()/StudentWriteData_Size;i++){
                    ClassroomStudentWriteData writeData=new ClassroomStudentWriteData();
                    writeData.setStudentRecordId(classroomStudentWriteData.getStudentRecordId());
                    writeData.setStudentId(classroomStudentWriteData.getStudentId());
                    writeData.setIndexN(i);
                    writeData.setPageNum(classroomStudentWriteData.getPageNum());
                    int end=(i+1)*StudentWriteData_Size>classroomStudentWriteData.getStudentsWriteRecords().size()?classroomStudentWriteData.getStudentsWriteRecords().size():(i+1)*StudentWriteData_Size;
                    writeData.setStudentsWriteRecords(classroomStudentWriteData.getStudentsWriteRecords().subList(i*StudentWriteData_Size,end));
                    writeData.setCreateTime(new Date());
                    toSave.add(writeData);
                }
            }

            if(classroomStudentWriteData.getStudentRecordId() != null) {
                ClassroomExercisesStudentRecord studentRecord = recordMap.get(classroomStudentWriteData.getStudentRecordId());
                if(studentRecord!=null&&(studentRecord.getHavaWrite()==null||studentRecord.getHavaWrite()!=1)){
                    studentRecord.setHavaWrite(1);
                    recordsToUpdate.add(studentRecord);
                    recordMap.put(studentRecord.getId(), studentRecord);
                }
            }
        }

        if (!toDelete.isEmpty()) {
            for (ClassroomStudentWriteData data : toDelete) {
                classroomStudentWriteDataRepository.delete(data);
            }
        }

        if (!toSave.isEmpty()) {
            classroomStudentWriteDataRepository.saveAll(toSave);
        }

        if (!recordsToUpdate.isEmpty()) {
            List<ClassroomExercisesStudentRecord> uniqueRecords = new ArrayList<>(new HashSet<>(recordsToUpdate));
            classroomExercisesStudentRecordRepository.saveAll(uniqueRecords);
        }
    }

    @Override
    public List<ClassroomStudentWriteData> findByStudentRecordId(Long studentRecordId) {
        ClassroomStudentWriteData data=new ClassroomStudentWriteData();
        data.setStudentRecordId(studentRecordId);
        Sort sort = Sort.by(Sort.Direction.ASC,"pageNum","indexN");
        List<ClassroomStudentWriteData> list=classroomStudentWriteDataRepository.findAll(Example.of(data),sort);
        return groupWriteDataByPageNum(list);
    }

    @Override
    public List<ClassroomStudentWriteData> findByStudentRecordIds(List<Long> studentRecordIds) {
        if (studentRecordIds == null || studentRecordIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<ClassroomStudentWriteData> allWriteData = classroomStudentWriteDataRepository.findByStudentRecordIdIn(studentRecordIds);

        allWriteData.sort((d1, d2) -> {
            int pageNumCompare = Integer.compare(d1.getPageNum() != null ? d1.getPageNum() : 0,
                                               d2.getPageNum() != null ? d2.getPageNum() : 0);
            if (pageNumCompare != 0) return pageNumCompare;
            return Integer.compare(d1.getIndexN() != null ? d1.getIndexN() : 0,
                                 d2.getIndexN() != null ? d2.getIndexN() : 0);
        });

        return groupWriteDataByPageNum(allWriteData);
    }

    /**
     * 按页码分组写数据
     * @param list 写数据列表
     * @return 分组后的数据列表
     */
    private List<ClassroomStudentWriteData> groupWriteDataByPageNum(List<ClassroomStudentWriteData> list) {
        List<ClassroomStudentWriteData> dataList = new ArrayList<>();
        if (list == null || list.isEmpty()) {
            return dataList;
        }
        
        // 按学生记录 ID 分组
        Map<Long, List<ClassroomStudentWriteData>> recordIdMap = new HashMap<>();
        for (ClassroomStudentWriteData writeData : list) {
            recordIdMap.computeIfAbsent(writeData.getStudentRecordId(), k -> new ArrayList<>()).add(writeData);
        }
        
        // 对每个学生记录的数据进行分页分组
        for (List<ClassroomStudentWriteData> recordWriteData : recordIdMap.values()) {
            int pageNum = 1;
            ClassroomStudentWriteData studentWriteData = null;
            List<StudentsWriteRecord> studentsWriteRecords = new ArrayList<>();
            
            for (ClassroomStudentWriteData writeData : recordWriteData) {
                if (writeData.getPageNum() == pageNum) {
                    if (studentWriteData == null) {
                        studentWriteData = new ClassroomStudentWriteData();
                        studentWriteData.setId(writeData.getId());
                        studentWriteData.setStudentRecordId(writeData.getStudentRecordId());
                        studentWriteData.setStudentId(writeData.getStudentId());
                        studentWriteData.setPageNum(pageNum);
                    }
                    studentsWriteRecords.addAll(writeData.getStudentsWriteRecords());
                } else {
                    if (studentWriteData == null) {
                        studentWriteData = new ClassroomStudentWriteData();
                        studentWriteData.setId(writeData.getId());
                        studentWriteData.setStudentRecordId(writeData.getStudentRecordId());
                        studentWriteData.setStudentId(writeData.getStudentId());
                        studentWriteData.setPageNum(writeData.getPageNum());
                        studentsWriteRecords.addAll(writeData.getStudentsWriteRecords());
                        pageNum = writeData.getPageNum();
                    } else {
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
                // 最后一个元素，添加到列表
                if (recordWriteData.indexOf(writeData) == recordWriteData.size() - 1) {
                    if (studentsWriteRecords != null && studentsWriteRecords.size() > 0) {
                        studentWriteData.setStudentsWriteRecords(studentsWriteRecords);
                        dataList.add(studentWriteData);
                    }
                }
            }
        }
        
        return dataList;
    }
}
