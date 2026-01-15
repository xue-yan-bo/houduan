package com.jlm.homework.service.impl;

import com.jlm.homework.dto.Result;
import com.jlm.homework.entity.Copybook;
import com.jlm.homework.entity.CopybookStudentRecord;
import com.jlm.homework.entity.Student;
import com.jlm.homework.feign.StudentFeignClient;
import com.jlm.homework.repository.CopybookRepository;
import com.jlm.homework.repository.CopybookStudentRecordRepository;
import com.jlm.homework.service.ICopybookService;
import com.jlm.homework.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class CopybookServiceImpl implements ICopybookService {
    @Resource
    private CopybookRepository copybookRepository;
    @Resource
    private CopybookStudentRecordRepository copybookStudentRecordRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private StudentFeignClient studentFeignClient;
    @Override
    public Long create(Copybook copybook) {
        copybook.setCreateTime(new Date());
        if(copybook.getSchoolId()==null){
            copybook.setSchoolId(userService.getCurrentSchoolIdSafely());
        }
        return copybookRepository.save(copybook).getId();
    }

    @Override
    public Copybook getById(Long id) {
        return copybookRepository.findById(id).orElse(null);
    }

    @Override
    public Copybook update(Copybook copybook) {
        if(copybook!=null&&copybook.getStatus()!=null&&copybook.getStatus()==1){
            throw new RuntimeException("字帖已发布，不可以修改！");
        }
        return copybookRepository.save(copybook);
    }

    @Override
    public Page<Copybook> selectList(Integer pageNum, Integer pageSize, Copybook copybook) {
        if (pageNum == null || pageNum <= 0 || pageSize == null || pageSize <= 0) {
            pageNum = 1;
            pageSize = 10;
        }
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);
        return copybookRepository.findAll(Example.of(copybook),pageable);
    }

    @Override
    public void deleteById(Long id) {
        Copybook copybook = copybookRepository.findById(id).orElse(null);
        if(copybook!=null&&copybook.getStatus()!=null&&copybook.getStatus()==1){
            throw new RuntimeException("字帖已发布，不可以删除！");
        }
        copybookRepository.deleteById(id);
    }

    @Override
    public Boolean publish(Long id) {
        Boolean flag = false;
        Copybook copybook = copybookRepository.findById(id).orElse(null);
        if(copybook!=null){
            if(copybook.getClassId()!=null){
                Result<Student> result = studentFeignClient.getStudentList(1,200,copybook.getSchoolId(),null,copybook.getClassId(),"0");
                if(result.getCode()!=200){
                    throw new RuntimeException(result.getMsg());
                }
                List<Student> studentList=result.getRows();
                if(studentList.size()==0){
                    throw new RuntimeException("该班级还没有学生呢，请检查！");
                }
                for(Student student:studentList){
                    CopybookStudentRecord record = new CopybookStudentRecord();
                    BeanUtils.copyProperties(copybook,record);
                    record.setId(null);
                    record.setCopybookId(copybook.getId());
                    record.setCopybookName(copybook.getName());
                    record.setStudentId(student.getStudentId());
                    record.setStudentName(student.getStudentName());
                    record.setCreateTime(new Date());
                    copybookStudentRecordRepository.save(record);
                }
            }
            copybook.setStatus(1);
            copybookRepository.save(copybook);
        }
        return flag;
    }
}
