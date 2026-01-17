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
import com.jlm.homework.util.StringUtils;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
        if(copybook.getStatus()==null){
            copybook.setStatus(0);
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
        Specification<Copybook> specification = new Specification<Copybook>() {
            @Override
            public Predicate toPredicate(Root<Copybook> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                if(copybook.getSchoolId()!=null){
                    Predicate con = criteriaBuilder.equal(root.get("schoolId"),copybook.getSchoolId());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybook.getName())){
                    Predicate con = criteriaBuilder.like(root.get("name"),"%"+copybook.getName()+"%");
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybook.getFont())){
                    Predicate con = criteriaBuilder.equal(root.get("font"),copybook.getFont());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybook.getFormat())){
                    Predicate con = criteriaBuilder.equal(root.get("format"),copybook.getFormat());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybook.getSubject())){
                    Predicate con = criteriaBuilder.equal(root.get("subject"),copybook.getSubject());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybook.getContent())){
                    Predicate con = criteriaBuilder.like(root.get("content"),"%"+copybook.getContent()+"%");
                    list.add(con);
                }
                if(copybook.getTeacherId()!=null){
                    Predicate con = criteriaBuilder.equal(root.get("teacherId"),copybook.getTeacherId());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybook.getTeacherName())){
                    Predicate con = criteriaBuilder.like(root.get("teacherName"),"%"+copybook.getTeacherName()+"%");
                    list.add(con);
                }
                if(copybook.getStatus()!=null){
                    Predicate con = criteriaBuilder.equal(root.get("status"),copybook.getStatus());
                    list.add(con);
                }
                if(copybook.getCreateTime()!=null){
                    Predicate con = criteriaBuilder.between(root.get("createTime").as(Date.class),copybook.getCreateTime(),new Date());
                    list.add(con);
                }
                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        return copybookRepository.findAll(specification,pageable);
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
                    record.setCopybookId(id);
                    record.setFont(copybook.getFont());
                    record.setCopybookName(copybook.getName());
                    record.setStudentId(student.getStudentId());
                    record.setStudentName(student.getStudentName());
                    record.setSubmitStatus(0);
                    record.setCreateTime(new Date());
                    copybookStudentRecordRepository.save(record);
                }
            }
            copybook.setStatus(1);
            copybookRepository.save(copybook);
            flag = true;
        }
        return flag;
    }
}
