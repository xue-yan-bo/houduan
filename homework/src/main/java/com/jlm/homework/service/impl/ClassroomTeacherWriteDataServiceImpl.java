package com.jlm.homework.service.impl;

import com.jlm.homework.entity.*;
import com.jlm.homework.repository.ClassroomTeacherWriteDataRepository;
import com.jlm.homework.service.IClassroomTeacherWriteDataService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
@Service
public class ClassroomTeacherWriteDataServiceImpl implements IClassroomTeacherWriteDataService {
    private static Integer TeacherWriteData_Size=500;
    @Resource
    private ClassroomTeacherWriteDataRepository classroomTeacherWriteDataRepository;
    @Override
    public void save(ClassroomTeacherWriteData classroomTeacherWriteData) {

        if(classroomTeacherWriteData.getTeacherWriteRecords()==null||classroomTeacherWriteData.getTeacherWriteRecords().size()==0){
            return;
        }
        if(classroomTeacherWriteData.getTeacherWriteRecords().size()<TeacherWriteData_Size){
            classroomTeacherWriteDataRepository.save(classroomTeacherWriteData);
        }else {
            ClassroomTeacherWriteData data=new ClassroomTeacherWriteData();
            data.setClassroomExercisesId(classroomTeacherWriteData.getClassroomExercisesId());
            data.setPageNum(classroomTeacherWriteData.getPageNum());
            classroomTeacherWriteDataRepository.delete(data);
            for(int i=0;i<=classroomTeacherWriteData.getTeacherWriteRecords().size()/TeacherWriteData_Size;i++){
                ClassroomTeacherWriteData writeData=new ClassroomTeacherWriteData();
                writeData.setClassroomExercisesId(classroomTeacherWriteData.getClassroomExercisesId());
                writeData.setTeacherId(classroomTeacherWriteData.getTeacherId());
                writeData.setTeacherName(classroomTeacherWriteData.getTeacherName());
                writeData.setIndexN(i);
                writeData.setPageNum(classroomTeacherWriteData.getPageNum());
                int end=(i+1)*TeacherWriteData_Size>classroomTeacherWriteData.getTeacherWriteRecords().size()?classroomTeacherWriteData.getTeacherWriteRecords().size():(i+1)*TeacherWriteData_Size;
                writeData.setTeacherWriteRecords(classroomTeacherWriteData.getTeacherWriteRecords().subList(i*TeacherWriteData_Size,end));
                writeData.setCreateTime(new Date());
                classroomTeacherWriteDataRepository.save(writeData);
            }
        }
    }

    @Override
    public List<ClassroomTeacherWriteData> findByClassroomExercisesId(Long classroomExercisesId) {
        ClassroomTeacherWriteData data=new ClassroomTeacherWriteData();
        data.setClassroomExercisesId(classroomExercisesId);
        Sort sort = Sort.by(Sort.Direction.ASC,"pageNum","indexN");
        List<ClassroomTeacherWriteData> list=classroomTeacherWriteDataRepository.findAll(Example.of(data),sort);
        List<ClassroomTeacherWriteData> dataList=new ArrayList<>();
        int pageNum=1;
        ClassroomTeacherWriteData teacherWriteData= null;
        List<StudentsWriteRecord> teacherWriteRecords = new ArrayList<>();
        for(ClassroomTeacherWriteData writeData:list){
            if(writeData.getPageNum()==pageNum){
                if(teacherWriteData==null){
                    teacherWriteData = new ClassroomTeacherWriteData();
                    teacherWriteData.setId(writeData.getId());
                    teacherWriteData.setClassroomExercisesId(writeData.getClassroomExercisesId());
                    teacherWriteData.setTeacherId(writeData.getTeacherId());
                    teacherWriteData.setTeacherName(writeData.getTeacherName());
                    teacherWriteData.setPageNum(pageNum);
                }
                teacherWriteRecords.addAll(writeData.getTeacherWriteRecords());
            }else{
                if(teacherWriteData==null){
                    teacherWriteData = new ClassroomTeacherWriteData();
                    teacherWriteData.setId(writeData.getId());
                    teacherWriteData.setClassroomExercisesId(writeData.getClassroomExercisesId());
                    teacherWriteData.setTeacherId(writeData.getTeacherId());
                    teacherWriteData.setTeacherName(writeData.getTeacherName());
                    teacherWriteData.setPageNum(pageNum);
                    teacherWriteRecords.addAll(writeData.getTeacherWriteRecords());
                }
                teacherWriteData.setTeacherWriteRecords(teacherWriteRecords);
                dataList.add(teacherWriteData);
                pageNum++;
                teacherWriteData= new ClassroomTeacherWriteData();
                teacherWriteData.setId(writeData.getId());
                teacherWriteData.setClassroomExercisesId(writeData.getClassroomExercisesId());
                teacherWriteData.setTeacherId(writeData.getTeacherId());
                teacherWriteData.setTeacherName(writeData.getTeacherName());
                teacherWriteData.setPageNum(pageNum);
                teacherWriteRecords = new ArrayList<>();
                teacherWriteRecords.addAll(writeData.getTeacherWriteRecords());
                teacherWriteData.setTeacherWriteRecords(teacherWriteRecords);
            }
            //最后一个元素，list增加
            if(list.indexOf(writeData)==list.size()-1){
                if(teacherWriteRecords!=null&&teacherWriteRecords.size()>0){
                    teacherWriteData.setTeacherWriteRecords(teacherWriteRecords);
                    dataList.add(teacherWriteData);
                }
            }
        }
        return dataList;
    }
}
