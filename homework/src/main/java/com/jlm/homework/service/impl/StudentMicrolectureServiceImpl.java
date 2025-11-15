package com.jlm.homework.service.impl;

import com.jlm.homework.dto.Result;
import com.jlm.homework.entity.Microlecture;
import com.jlm.homework.entity.Student;
import com.jlm.homework.entity.StudentMicrolecture;
import com.jlm.homework.feign.StudentFeignClient;
import com.jlm.homework.repository.IStudentMicrolectureRepository;
import com.jlm.homework.service.IStudentMicrolectureService;
import com.jlm.homework.service.UserService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;


@Service
public class StudentMicrolectureServiceImpl  implements IStudentMicrolectureService {
    @Resource
    private IStudentMicrolectureRepository studentMicrolectureRepository;
    @Autowired
    private StudentFeignClient studentFeignClient;

    @Autowired
    private UserService userService;
    @Override
    public void createStudentMicrolecture(Microlecture microlecture) {
        StudentMicrolecture search = new StudentMicrolecture();
        search.setMicrolectureId(microlecture.getId());
        List<StudentMicrolecture>  list=studentMicrolectureRepository.findAll(Example.of(search));
        if(list==null||list.size()>0){
            return;
        }
        Result<Student> result = studentFeignClient.getStudentList(1,100,microlecture.getSchoolId(),microlecture.getGradeId(),microlecture.getClassId(),"0");
        if(result.getCode()!=200){
            throw new RuntimeException(result.getMsg());
        }
        List<Student> studentList=result.getRows();
        if(studentList!=null&&studentList.size()>0){
            for(Student student:studentList){
                StudentMicrolecture studentMicrolecture=new StudentMicrolecture();

                studentMicrolecture.setStudentId(student.getStudentId());
                studentMicrolecture.setStudentName(student.getStudentName());
                studentMicrolecture.setChapter(microlecture.getChapter());
                studentMicrolecture.setSubject(microlecture.getSubject());
                studentMicrolecture.setSchoolId(microlecture.getSchoolId());
                studentMicrolecture.setGradeId(microlecture.getGradeId());
                studentMicrolecture.setGradeName(microlecture.getGradeName());
                studentMicrolecture.setClassId(microlecture.getClassId());
                studentMicrolecture.setClassName(microlecture.getClassName());

                studentMicrolecture.setMicrolectureId(microlecture.getId());
                studentMicrolecture.setMicrolectureName(microlecture.getName());
                studentMicrolecture.setFileUrl(microlecture.getFileUrl());
                studentMicrolecture.setStatus(0);
                studentMicrolecture.setCreateTime(new Date());
                studentMicrolectureRepository.save(studentMicrolecture);
            }
        }
    }

    @Override
    public void save(StudentMicrolecture studentMicrolecture) {
        studentMicrolectureRepository.save(studentMicrolecture);
    }

    @Override
    public StudentMicrolecture selectByMicrolectureAndStudent(Long microlectureId, Long studentId) {
        StudentMicrolecture search = new StudentMicrolecture();
        search.setMicrolectureId(microlectureId);
        search.setStudentId(studentId);
        Optional<StudentMicrolecture> optional =studentMicrolectureRepository.findOne(Example.of(search));
        if(optional!=null&&optional.isPresent()){
            return optional.get();
        }
        return null;
    }

    @Override
    public void updateById(StudentMicrolecture studentMicrolecture) {
        studentMicrolectureRepository.save(studentMicrolecture);
    }

    @Override
    public Page<StudentMicrolecture> page(Integer pageNum, Integer pageSize, Long microlectureId, String microlecturename, Long studentId, String studentName, Integer status,Integer searchType) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        Pageable pageable;
        pageable = PageRequest.of(pageNum, pageSize, sort);
        if(searchType==0&&studentId==null){
            studentId = userService.getCurrentUserId();
        }
        Long finalStudentId = studentId;
        Specification<StudentMicrolecture> specification = new Specification<StudentMicrolecture>() {

            @Override
            public Predicate toPredicate(Root<StudentMicrolecture> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                try {
                    if(microlectureId!=null) {
                        Predicate condition = criteriaBuilder.equal(root.get("microlectureId"), microlectureId);
                        list.add(condition);
                    }
                    if(StringUtils.isNotEmpty(microlecturename)) {
                        Predicate condition = criteriaBuilder.like(root.get("microlecturename"), "%"+microlecturename+"%");
                        list.add(condition);
                    }
                    if(finalStudentId!=null) {
                        Predicate condition1 = criteriaBuilder.equal(root.get("studentId"), finalStudentId);
                        list.add(condition1);
                    }
                    if(StringUtils.isNotEmpty(studentName)) {
                        Predicate condition = criteriaBuilder.equal(root.get("studentName"), studentName);
                        list.add(condition);
                    }
                    if(status!=null) {
                        Predicate condition = criteriaBuilder.equal(root.get("status"), status);
                        list.add(condition);
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }

        };
        return studentMicrolectureRepository.findAll(specification,pageable);
    }

    @Override
    public Page<StudentMicrolecture> recordPage(Integer pageNum, Integer pageSize, Long microlectureId,Long studentId) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "updateTime","createTime");
        Pageable pageable;
        pageable = PageRequest.of(pageNum, pageSize, sort);
        if(studentId==null){
            studentId = userService.getCurrentUserId();
        }

        Long finalStudentId = studentId;
        Specification<StudentMicrolecture> specification = new Specification<StudentMicrolecture>() {

            @Override
            public Predicate toPredicate(Root<StudentMicrolecture> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                try {
                    if(microlectureId!=null) {
                        Predicate condition = criteriaBuilder.equal(root.get("microlectureId"), microlectureId);
                        list.add(condition);
                    }

                    if(finalStudentId !=null) {
                        Predicate condition1 = criteriaBuilder.equal(root.get("studentId"), finalStudentId);
                        list.add(condition1);
                    }


                    Predicate condition = criteriaBuilder.notEqual(root.get("status"), 0);
                    list.add(condition);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }

        };
        return studentMicrolectureRepository.findAll(specification,pageable);
    }
}
