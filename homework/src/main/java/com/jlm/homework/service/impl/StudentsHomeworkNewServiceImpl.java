package com.jlm.homework.service.impl;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.shaded.com.google.gson.JsonArray;
import com.alibaba.nacos.shaded.com.google.gson.JsonObject;
import com.jlm.homework.dto.Result;
import com.jlm.homework.dto.StudentsHomeworkRequest;
import com.jlm.homework.entity.ExerciseBookEntity;
import com.jlm.homework.entity.HomeworkPublish;
import com.jlm.homework.entity.Student;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.feign.StudentFeginClient;
import com.jlm.homework.repository.HomeworkPublishRepository;
import com.jlm.homework.repository.StudentsHomeworkNewRepository;
import com.jlm.homework.service.ExerciseBookServer;
import com.jlm.homework.service.IStudentsHomeworkNewService;
import com.jlm.homework.service.UserService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Example;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@AllArgsConstructor
public class StudentsHomeworkNewServiceImpl implements IStudentsHomeworkNewService {
    @Resource
    private StudentsHomeworkNewRepository studentsHomeworkNewRepository;
    @Resource
    private HomeworkPublishRepository homeworkPublishRepository;
    @Autowired
    private StudentFeginClient studentFeginClient;
    @Autowired
    private ExerciseBookServer exerciseBookServer;
    @Override
    public void createStudentsHomeworkByHomeworkPublish(HomeworkPublish homeworkPublish) {
        if(!homeworkPublish.getClassId().isEmpty()){
            String subject = null;
            if(homeworkPublish.getExerciseBookId() != null){
                ExerciseBookEntity exerciseBook = exerciseBookServer.findById(homeworkPublish.getExerciseBookId());
                subject =  exerciseBook.getSubject();
            }

            for(Long classId:homeworkPublish.getClassId()){
                try {
                    Long schoolId=homeworkPublish.getSchoolId();
                    Result<Student> result = studentFeginClient.getStudentList(1,100,schoolId,classId,0);
                    if(result.getCode()!=200){
                        throw new RuntimeException(result.getMsg());
                    }
                    List<Student> studentList=result.getRows();
                    for(Student student:studentList){
                        StudentsHomeworkNew studentsHomework = new StudentsHomeworkNew();
                        studentsHomework.setClassesId(classId);
                        studentsHomework.setHomeworkType(2);
                        studentsHomework.setHomeworkPublishId(homeworkPublish.getId());
                        studentsHomework.setSchoolId(student.getSchoolId());
                        studentsHomework.setStudentId(student.getStudentId());
                        studentsHomework.setStudentName(student.getStudentName());
                        studentsHomework.setStudentUuid(student.getLinkUuid());
                        studentsHomework.setCreateTime(new Date());
                        studentsHomework.setSubmitStatus(0);
                        studentsHomework.setAuditStatus("0");
                        studentsHomework.setSubject(subject);
                        studentsHomeworkNewRepository.save(studentsHomework);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public List<StudentsHomeworkNew> getByHomeworkPublishId(Long homeworkPublishId, StudentsHomeworkRequest studentsHomeworkRequest) {
        StudentsHomeworkNew homeworkNew = new StudentsHomeworkNew();

            Specification<StudentsHomeworkNew> specification = new Specification<StudentsHomeworkNew>() {

                @Override
                public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                    if (studentsHomeworkRequest != null) {
                        Predicate condition0 = criteriaBuilder.equal(root.get("homeworkPublishId"), homeworkPublishId);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        Calendar calendar = Calendar.getInstance();
                        Predicate condition1 = null;
                        if (StringUtils.isNotEmpty(studentsHomeworkRequest.getStudentName())) {
                            condition1 = criteriaBuilder.like(root.get("studentName").as(String.class), "%" + studentsHomeworkRequest.getStudentName() + "%");
                        }else {
                            condition1 = criteriaBuilder.conjunction();
                        }
                        Predicate condition2 = null;
                        if (studentsHomeworkRequest.getSubmitStatus() != null) {
                            condition2 = criteriaBuilder.equal(root.get("submitStatus").as(String.class), studentsHomeworkRequest.getSubmitStatus());
                        }else {
                            condition2 = criteriaBuilder.conjunction();
                        }
                        try {
                            Predicate condition3 = null;
                            if (StringUtils.isNotEmpty(studentsHomeworkRequest.getSubmitTime())) {
                                Date submitTime = sdf.parse(studentsHomeworkRequest.getSubmitTime());
                                calendar.setTime(submitTime);
                                calendar.add(Calendar.DAY_OF_MONTH, 1);
                                Date submitTime1 = calendar.getTime();
                                condition3 = criteriaBuilder.between(root.get("submitTime").as(Date.class), submitTime, submitTime1);

                            }else {
                                condition3 = criteriaBuilder.conjunction();
                            }
                            Predicate condition4 = null;
                            if (StringUtils.isNotEmpty(studentsHomeworkRequest.getAuditTime())) {
                                Date auditTime = sdf.parse(studentsHomeworkRequest.getAuditTime());
                                calendar.setTime(auditTime);
                                calendar.add(Calendar.DAY_OF_MONTH, 1);
                                Date auditTime1 = calendar.getTime();
                                condition4 = criteriaBuilder.between(root.get("auditTime").as(Date.class), auditTime, auditTime1);
                            }else {
                                condition4 = criteriaBuilder.conjunction();
                            }
                            Predicate condition5 = null;
                            if (studentsHomeworkRequest.getAuditStatus() != null) {
                                condition5 = criteriaBuilder.equal(root.get("auditStatus"), studentsHomeworkRequest.getAuditStatus());
                            }else {
                                condition5 = criteriaBuilder.conjunction();
                            }

                            query.where(condition0, condition1, condition2, condition3, condition4, condition5);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }

                    }
                    return null;
                }


            };
            List<StudentsHomeworkNew> studentsHomeworkList = studentsHomeworkNewRepository.findAll(specification);
            return studentsHomeworkList;
    }

    @Override
    public StudentsHomeworkNew update(StudentsHomeworkNew studentsHomework) {
        studentsHomework = studentsHomeworkNewRepository.save(studentsHomework);
        StudentsHomeworkNew newSerach=new StudentsHomeworkNew();
        newSerach.setHomeworkPublishId(studentsHomework.getHomeworkPublishId());
        newSerach.setAuditStatus("1");
        Example<StudentsHomeworkNew> example = Example.of(studentsHomework);
        long count =studentsHomeworkNewRepository.count(example);
        if(count==0){
            HomeworkPublish homeworkPublish=homeworkPublishRepository.getById(studentsHomework.getHomeworkPublishId());
            homeworkPublish.setAuditStatus(1);
            homeworkPublishRepository.save(homeworkPublish);
        }
        return studentsHomework;
    }

    @Override
    public Page<StudentsHomeworkNew> getListByHomeworkPublishId(Long homeworkPublishId, Integer pageNum, Integer pageSize, StudentsHomeworkRequest studentsHomeworkRequest) {
        StudentsHomeworkNew homeworkNew = new StudentsHomeworkNew();
        Pageable pageable = PageRequest.of(pageNum-1, pageSize);
        Specification<StudentsHomeworkNew> specification= new Specification<StudentsHomeworkNew>() {

            @Override
            public Predicate toPredicate(Root<StudentsHomeworkNew> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                Predicate condition0 = criteriaBuilder.equal(root.get("homeworkPublishId"), homeworkPublishId);
                if(studentsHomeworkRequest!=null){
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    Calendar calendar = Calendar.getInstance();
                    Predicate condition1 = null;
                    if(StringUtils.isNotEmpty(studentsHomeworkRequest.getStudentName())){
                        condition1 = criteriaBuilder.like(root.get("studentName").as(String.class), "%" + studentsHomeworkRequest.getStudentName() + "%");
                    }else {
                        condition1 = criteriaBuilder.conjunction();
                    }
                    Predicate condition2 = null;
                    if(studentsHomeworkRequest.getSubmitStatus()!=null){
                        condition2 =criteriaBuilder.equal(root.get("submitStatus").as(String.class), studentsHomeworkRequest.getSubmitStatus());
                    }else {
                        condition2 = criteriaBuilder.conjunction();
                    }
                    try {
                        Predicate condition3 = null;
                        if(StringUtils.isNotEmpty(studentsHomeworkRequest.getSubmitTime())){
                            Date submitTime = sdf.parse(studentsHomeworkRequest.getSubmitTime());
                            calendar.setTime(submitTime);
                            calendar.add(Calendar.DAY_OF_MONTH,1);
                            Date submitTime1 = calendar.getTime();
                            condition3 = criteriaBuilder.between(root.get("submitTime").as(Date.class),submitTime,submitTime1);

                        }else {
                            condition3 = criteriaBuilder.conjunction();
                        }
                        Predicate condition4 = null;
                        if(StringUtils.isNotEmpty(studentsHomeworkRequest.getAuditTime())){
                            Date auditTime=sdf.parse(studentsHomeworkRequest.getAuditTime());
                            calendar.setTime(auditTime);
                            calendar.add(Calendar.DAY_OF_MONTH,1);
                            Date auditTime1 = calendar.getTime();
                            condition4 = criteriaBuilder.between(root.get("auditTime").as(Date.class),auditTime,auditTime1 );
                        }else {
                            condition4 = criteriaBuilder.conjunction();
                        }
                        Predicate condition5 = null;
                        if(StringUtils.isNotEmpty(studentsHomeworkRequest.getAuditStatus())){
                            condition5 =criteriaBuilder.equal(root.get("auditStatus"), studentsHomeworkRequest.getAuditStatus());
                        }else {
                            condition5 = criteriaBuilder.conjunction();
                        }

                        query.where(condition0,condition1,condition2,condition3,condition4,condition5);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }

                }
                return null;
            }


        };
        Page<StudentsHomeworkNew> studentsHomeworkList=studentsHomeworkNewRepository.findAll(specification,pageable);
        return studentsHomeworkList;
    }

    @Override
    public List<StudentsHomeworkNew> getClassHomeworkStatistics(String subject, Long classId, String startDate) {

        Specification<HomeworkPublish> specification= new Specification<HomeworkPublish>() {

            @Override
            public Predicate toPredicate(Root<HomeworkPublish> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                try {
                    Predicate condition0 = null;
                    if(StringUtils.isNotEmpty(subject)){
                        condition0 =criteriaBuilder.equal(root.get("subject"), subject);
                    }else {
                        condition0 = criteriaBuilder.conjunction();
                    }
                    Predicate condition1 = null;
                    if(classId!=null){
                        condition1 =criteriaBuilder.like(root.get("classId"), "%"+classId+"%");
                    }else {
                        condition1 = criteriaBuilder.conjunction();
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    Calendar calendar = Calendar.getInstance();
                    Predicate condition2 = null;
                    if(StringUtils.isNotEmpty(startDate)){
                        Date start = null;

                            start = sdf.parse(startDate);

                        calendar.setTime(start);
                        calendar.add(Calendar.DAY_OF_MONTH,1);
                        Date end = calendar.getTime();
                        condition2 = criteriaBuilder.between(root.get("publishTime"),start,end);
                    }else {
                        condition2 = criteriaBuilder.conjunction();
                    }
                    query.where(condition0,condition1,condition2);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        };
        Optional<HomeworkPublish> optional=homeworkPublishRepository.findOne(specification);
        HomeworkPublish homeworkPublish=optional.get();
        StudentsHomeworkNew  studentsHomeworkNew=new StudentsHomeworkNew();
        studentsHomeworkNew.setHomeworkPublishId(homeworkPublish.getId());
        studentsHomeworkNew.setClassesId(classId);
        studentsHomeworkNew.setSubject(subject);
        List<StudentsHomeworkNew> studentsHomeworkNewList = studentsHomeworkNewRepository.findAll(Example.of(studentsHomeworkNew));
        return studentsHomeworkNewList;
    }
}
