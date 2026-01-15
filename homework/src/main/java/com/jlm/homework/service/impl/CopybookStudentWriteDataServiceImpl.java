package com.jlm.homework.service.impl;


import com.jlm.homework.entity.CopybookStudentRecord;
import com.jlm.homework.entity.CopybookStudentWriteData;
import com.jlm.homework.entity.StudentsWriteRecord;

import com.jlm.homework.repository.CopybookStudentRecordRepository;
import com.jlm.homework.repository.CopybookStudentWriteDataRepository;
import com.jlm.homework.service.ICopybookStudentWriteDataService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class CopybookStudentWriteDataServiceImpl implements ICopybookStudentWriteDataService {
    private static Integer StudentWriteData_Size=800;
    @Resource
    private CopybookStudentWriteDataRepository copybookStudentWriteDataRepository;
    @Resource
    private CopybookStudentRecordRepository copybookStudentRecordRepository;
    @Override
    public void save(CopybookStudentWriteData copybookStudentWriteData) {
        if(copybookStudentWriteData.getStudentRecordId()==null||copybookStudentWriteData.getStudentRecordId()==0){
            Long studentId =copybookStudentWriteData.getStudentId();
            CopybookStudentRecord record = new CopybookStudentRecord();
            record.setStudentId(studentId);
            Sort sort = Sort.by(Sort.Direction.DESC,"copybookExercisesId","startTime","createTime");
            List<CopybookStudentRecord> recordList=copybookStudentRecordRepository.findAll(Example.of(record),sort);
            if(recordList!=null&&recordList.size()>0){
                copybookStudentWriteData.setStudentRecordId(recordList.get(0).getId());
            }
        }
        if(copybookStudentWriteData.getStudentsWriteRecords()==null||copybookStudentWriteData.getStudentsWriteRecords().size()==0){
            return;
        }
        if(copybookStudentWriteData.getStudentsWriteRecords().size()<StudentWriteData_Size){
            copybookStudentWriteDataRepository.save(copybookStudentWriteData);
        }else {
            CopybookStudentWriteData data=new CopybookStudentWriteData();
            data.setStudentRecordId(copybookStudentWriteData.getStudentRecordId());
            data.setPageNum(copybookStudentWriteData.getPageNum());
            copybookStudentWriteDataRepository.delete(data);
            for(int i=0;i<=copybookStudentWriteData.getStudentsWriteRecords().size()/StudentWriteData_Size;i++){
                CopybookStudentWriteData writeData=new CopybookStudentWriteData();
                writeData.setStudentRecordId(copybookStudentWriteData.getStudentRecordId());
                writeData.setStudentId(copybookStudentWriteData.getStudentId());
                writeData.setIndexN(i);
                writeData.setPageNum(copybookStudentWriteData.getPageNum());
                int end=(i+1)*StudentWriteData_Size>copybookStudentWriteData.getStudentsWriteRecords().size()?copybookStudentWriteData.getStudentsWriteRecords().size():(i+1)*StudentWriteData_Size;
                writeData.setStudentsWriteRecords(copybookStudentWriteData.getStudentsWriteRecords().subList(i*StudentWriteData_Size,end));
                writeData.setCreateTime(new Date());
                copybookStudentWriteDataRepository.save(writeData);
            }
        }

    }

    @Override
    public List<CopybookStudentWriteData> findByStudentRecordId(Long studentRecordId) {
        CopybookStudentWriteData data=new CopybookStudentWriteData();
        data.setStudentRecordId(studentRecordId);
        Sort sort = Sort.by(Sort.Direction.ASC,"pageNum","indexN");
        List<CopybookStudentWriteData> list=copybookStudentWriteDataRepository.findAll(Example.of(data),sort);
        List<CopybookStudentWriteData> dataList=new ArrayList<>();
        int pageNum=1;
        CopybookStudentWriteData  studentWriteData= null;
        List<StudentsWriteRecord> studentsWriteRecords = new ArrayList<>();
        for(CopybookStudentWriteData writeData:list){
            if(writeData.getPageNum()==pageNum){
                if(studentWriteData==null){
                    studentWriteData = new CopybookStudentWriteData();
                    studentWriteData.setId(writeData.getId());
                    studentWriteData.setStudentRecordId(writeData.getStudentRecordId());
                    studentWriteData.setStudentId(writeData.getStudentId());
                    studentWriteData.setPageNum(pageNum);
                }
                studentsWriteRecords.addAll(writeData.getStudentsWriteRecords());
            }else{
                if(studentWriteData==null){
                    studentWriteData = new CopybookStudentWriteData();
                    studentWriteData.setId(writeData.getId());
                    studentWriteData.setStudentRecordId(writeData.getStudentRecordId());
                    studentWriteData.setStudentId(writeData.getStudentId());
                    studentWriteData.setPageNum(pageNum);
                    studentsWriteRecords.addAll(writeData.getStudentsWriteRecords());
                }
                studentWriteData.setStudentsWriteRecords(studentsWriteRecords);
                dataList.add(studentWriteData);
                pageNum++;
                studentWriteData= new CopybookStudentWriteData();
                studentWriteData.setId(writeData.getId());
                studentWriteData.setStudentRecordId(writeData.getStudentRecordId());
                studentWriteData.setStudentId(writeData.getStudentId());
                studentWriteData.setPageNum(pageNum);
                studentsWriteRecords = new ArrayList<>();
                studentsWriteRecords.addAll(writeData.getStudentsWriteRecords());
                studentWriteData.setStudentsWriteRecords(studentsWriteRecords);
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
