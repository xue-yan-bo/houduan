package com.jlm.homework.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.jlm.homework.entity.*;
import com.jlm.homework.repository.HomeworkStudentWriteDataRepository;
import com.jlm.homework.service.IHomeworkStudentWriteDataService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HomeworkStudentWriteDataServiceImpl implements IHomeworkStudentWriteDataService {
    private static Integer StudentWriteData_Size=500;
    @Resource
    private HomeworkStudentWriteDataRepository homeworkStudentWriteDataRepository;
    @Override
    public void save(HomeworkStudentWriteData homeworkStudentWriteData) {
        if(homeworkStudentWriteData.getStudentHomeworkId()==null||homeworkStudentWriteData.getStudentHomeworkId()==0){
            Long studentId =homeworkStudentWriteData.getStudentId();
            HomeworkStudentWriteData record = new HomeworkStudentWriteData();
            record.setStudentId(studentId);
            Sort sort = Sort.by(Sort.Direction.DESC,"startTime","createTime");
            List<HomeworkStudentWriteData> recordList=homeworkStudentWriteDataRepository.findAll(Example.of(record),sort);
            if(recordList!=null&&recordList.size()>0){
                homeworkStudentWriteData.setStudentHomeworkId(recordList.get(0).getId());
            }
        }
        if(homeworkStudentWriteData.getStudentsWriteRecords()==null||homeworkStudentWriteData.getStudentsWriteRecords().size()==0){
            return;
        }
        if(homeworkStudentWriteData.getStudentsWriteRecords().size()<StudentWriteData_Size){
            homeworkStudentWriteData.setIndexN(1);
            homeworkStudentWriteDataRepository.save(homeworkStudentWriteData);
        }else {
            HomeworkStudentWriteData data=new HomeworkStudentWriteData();
            data.setStudentHomeworkId(homeworkStudentWriteData.getStudentHomeworkId());
            data.setPageNum(homeworkStudentWriteData.getPageNum());
            data.setType(homeworkStudentWriteData.getType());
            homeworkStudentWriteDataRepository.delete(data);
            for(int i=0;i<=homeworkStudentWriteData.getStudentsWriteRecords().size()/StudentWriteData_Size;i++){
                HomeworkStudentWriteData writeData=new HomeworkStudentWriteData();
                writeData.setStudentHomeworkId(homeworkStudentWriteData.getStudentHomeworkId());
                writeData.setStudentId(homeworkStudentWriteData.getStudentId());
                writeData.setIndexN(i);
                writeData.setType(homeworkStudentWriteData.getType());
                writeData.setPageNum(homeworkStudentWriteData.getPageNum());
                int end=(i+1)*StudentWriteData_Size>homeworkStudentWriteData.getStudentsWriteRecords().size()?homeworkStudentWriteData.getStudentsWriteRecords().size():(i+1)*StudentWriteData_Size;
                writeData.setStudentsWriteRecords(homeworkStudentWriteData.getStudentsWriteRecords().subList(i*StudentWriteData_Size,end));
                writeData.setCreateTime(new Date());
                homeworkStudentWriteDataRepository.save(writeData);
            }
        }

    }

    @Override
    public List<HomeworkStudentWriteData> findByStudentRecordId(Long studentHomeworkId,String type) {
        HomeworkStudentWriteData data=new HomeworkStudentWriteData();
        data.setStudentHomeworkId(studentHomeworkId);
        data.setType(type);
        Sort sort = Sort.by(Sort.Direction.ASC,"pageNum","indexN");
        List<HomeworkStudentWriteData> list=homeworkStudentWriteDataRepository.findAll(Example.of(data),sort);
        List<HomeworkStudentWriteData> dataList=new ArrayList<>();
        int pageNum=1;
        HomeworkStudentWriteData  studentWriteData= null;
        List<StudentsWriteRecord> studentsWriteRecords = new ArrayList<>();
        for(HomeworkStudentWriteData writeData:list){
            if(writeData.getPageNum()==pageNum){
                if(studentWriteData==null){
                    studentWriteData = new HomeworkStudentWriteData();
                    studentWriteData.setId(writeData.getId());
                    studentWriteData.setStudentHomeworkId(writeData.getStudentHomeworkId());
                    studentWriteData.setStudentId(writeData.getStudentId());
                    studentWriteData.setPageNum(pageNum);
                    if(studentWriteData.getOffset()==null&&writeData.getOffset()!=null){
                        studentWriteData.setOffset(writeData.getOffset());
                    }
                }
                studentsWriteRecords.addAll(writeData.getStudentsWriteRecords());
            }else{
                if(studentWriteData==null){
                    studentWriteData = new HomeworkStudentWriteData();
                    studentWriteData.setId(writeData.getId());
                    studentWriteData.setStudentHomeworkId(writeData.getStudentHomeworkId());
                    studentWriteData.setStudentId(writeData.getStudentId());
                    studentWriteData.setPageNum(pageNum);
                    if(studentWriteData.getOffset()==null&&writeData.getOffset()!=null){
                        studentWriteData.setOffset(writeData.getOffset());
                    }
                    studentsWriteRecords.addAll(writeData.getStudentsWriteRecords());
                }
                studentWriteData.setStudentsWriteRecords(studentsWriteRecords);
                dataList.add(studentWriteData);
                pageNum++;
                studentWriteData= new HomeworkStudentWriteData();
                studentWriteData.setId(writeData.getId());
                studentWriteData.setStudentHomeworkId(writeData.getStudentHomeworkId());
                studentWriteData.setStudentId(writeData.getStudentId());
                studentWriteData.setPageNum(pageNum);
                if(writeData.getOffset()!=null) {
                    studentWriteData.setOffset(writeData.getOffset());
                }
                studentsWriteRecords = new ArrayList<>();
                studentsWriteRecords.addAll(writeData.getStudentsWriteRecords());
                studentWriteData.setStudentsWriteRecords(studentsWriteRecords);
            }
            //最后一个元素，list增加
            if(list.indexOf(writeData)==list.size()-1){
                if(studentsWriteRecords!=null&&studentsWriteRecords.size()>0){
                    if(studentWriteData.getOffset()==null&&writeData.getOffset()!=null){
                        studentWriteData.setOffset(writeData.getOffset());
                    }
                    studentWriteData.setStudentsWriteRecords(studentsWriteRecords);
                    dataList.add(studentWriteData);
                }
            }
        }
        return dataList;
    }

    @Override
    public Map<Long, List<HomeworkStudentWriteData>> findByStudentRecordIds(List<Long> studentHomeworkIds, String type) {
        Map<Long, List<HomeworkStudentWriteData>> resultMap = new HashMap<>();
        if (CollectionUtils.isEmpty(studentHomeworkIds)) {
            return resultMap;
        }
        
        // 按学生作业ID分组处理
        for (Long studentHomeworkId : studentHomeworkIds) {
            HomeworkStudentWriteData search = new HomeworkStudentWriteData();
            search.setStudentHomeworkId(studentHomeworkId);
            search.setType(type);
            Sort sort = Sort.by(Sort.Direction.ASC, "pageNum", "indexN");
            List<HomeworkStudentWriteData> records = homeworkStudentWriteDataRepository.findAll(Example.of(search), sort);
            
            // 合并数据
            List<HomeworkStudentWriteData> mergedDataList = new ArrayList<>();
            if (!records.isEmpty()) {
                int pageNum = 1;
                HomeworkStudentWriteData studentWriteData = null;
                List<StudentsWriteRecord> studentsWriteRecords = new ArrayList<>();
                
                for (HomeworkStudentWriteData writeData : records) {
                    if (writeData.getPageNum() == pageNum) {
                        if (studentWriteData == null) {
                            studentWriteData = new HomeworkStudentWriteData();
                            studentWriteData.setId(writeData.getId());
                            studentWriteData.setStudentHomeworkId(writeData.getStudentHomeworkId());
                            studentWriteData.setStudentId(writeData.getStudentId());
                            studentWriteData.setPageNum(pageNum);
                            if (studentWriteData.getOffset() == null && writeData.getOffset() != null) {
                                studentWriteData.setOffset(writeData.getOffset());
                            }
                        }
                        studentsWriteRecords.addAll(writeData.getStudentsWriteRecords());
                    } else {
                        if (studentWriteData != null) {
                            studentWriteData.setStudentsWriteRecords(studentsWriteRecords);
                            mergedDataList.add(studentWriteData);
                        }
                        pageNum++;
                        studentWriteData = new HomeworkStudentWriteData();
                        studentWriteData.setId(writeData.getId());
                        studentWriteData.setStudentHomeworkId(writeData.getStudentHomeworkId());
                        studentWriteData.setStudentId(writeData.getStudentId());
                        studentWriteData.setPageNum(pageNum);
                        if (writeData.getOffset() != null) {
                            studentWriteData.setOffset(writeData.getOffset());
                        }
                        studentsWriteRecords = new ArrayList<>();
                        studentsWriteRecords.addAll(writeData.getStudentsWriteRecords());
                    }
                    
                    // 最后一个元素，添加到列表
                    if (records.indexOf(writeData) == records.size() - 1) {
                        if (studentsWriteRecords != null && studentsWriteRecords.size() > 0) {
                            if (studentWriteData != null) {
                                if (studentWriteData.getOffset() == null && writeData.getOffset() != null) {
                                    studentWriteData.setOffset(writeData.getOffset());
                                }
                                studentWriteData.setStudentsWriteRecords(studentsWriteRecords);
                                mergedDataList.add(studentWriteData);
                            }
                        }
                    }
                }
            }
            
            resultMap.put(studentHomeworkId, mergedDataList);
        }
        
        return resultMap;
    }

    @Override
    public void updateOffset(HomeworkStudentWriteData writeData) {
        HomeworkStudentWriteData search =  new HomeworkStudentWriteData();
        search.setStudentHomeworkId(writeData.getStudentHomeworkId());
        search.setPageNum(writeData.getPageNum());
        search.setType(writeData.getType());
        List<HomeworkStudentWriteData> writeDataList=homeworkStudentWriteDataRepository.findAll(Example.of(search));
        for(HomeworkStudentWriteData studentWriteData:writeDataList){
            studentWriteData.setOffset(writeData.getOffset());
            homeworkStudentWriteDataRepository.save(studentWriteData);
        }
    }
}
