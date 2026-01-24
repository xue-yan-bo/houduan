package com.jlm.homework.service.impl;

import com.jlm.homework.dto.FeedbackDto;
import com.jlm.homework.dto.StudentFeedbackReq;
import com.jlm.homework.entity.StudentFeedback;
import com.jlm.homework.repository.StudentFeedbackRepository;
import com.jlm.homework.service.IStudentFeedbackService;
import com.jlm.homework.service.UserService;
import com.jlm.homework.util.StringUtils;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.annotation.Reference;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class StudentFeedbackServiceImpl implements IStudentFeedbackService {
    @Resource
    private StudentFeedbackRepository studentFeedbackRepository;
    @Autowired
    private UserService userService;

    @Override
    public void save(StudentFeedback studentFeedback) {
        studentFeedbackRepository.save(studentFeedback);
    }

    @Override
    public StudentFeedback findById(long id) {
        Optional<StudentFeedback> optional=studentFeedbackRepository.findById(id);
        if(optional!=null&&optional.isPresent()){
            return optional.get();
        }
        return null;
    }

    @Override
    public Page<StudentFeedback> findPage(Integer pageNum, Integer pageSize, StudentFeedbackReq studentFeedback) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable;
        pageable = PageRequest.of(pageNum, pageSize, sort);
        Specification<StudentFeedback> specification= new Specification<StudentFeedback>() {

            @Override
            public Predicate toPredicate(Root<StudentFeedback> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                if(studentFeedback.getSchoolId()==null){
                    Predicate con=criteriaBuilder.equal(root.get("schoolId").as(Long.class),userService.getCurrentSchoolId());
                    list.add(con);
                }else {
                    Predicate con=criteriaBuilder.equal(root.get("schoolId").as(Long.class),studentFeedback.getSchoolId());
                    list.add(con);
                }
                if(studentFeedback.getStudentId()!=null){
                    Predicate con=criteriaBuilder.equal(root.get("studentId"),studentFeedback.getStudentId());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(studentFeedback.getStudentName())){
                    Predicate con=criteriaBuilder.like(root.get("studentName"),"%"+studentFeedback.getStudentName()+"%");
                    list.add(con);
                }
                if(studentFeedback.getClassId()!=null){
                    Predicate con=criteriaBuilder.equal(root.get("classId"),studentFeedback.getClassId());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(studentFeedback.getClassName())){
                    Predicate con=criteriaBuilder.like(root.get("className"),"%"+studentFeedback.getClassName()+"%");
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(studentFeedback.getSubject())){
                    Predicate con=criteriaBuilder.like(root.get("subject"),"%"+studentFeedback.getSubject()+"%");
                    list.add(con);
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                if(StringUtils.isNotEmpty(studentFeedback.getFeedbackTimeStart())&&StringUtils.isNotEmpty(studentFeedback.getFeedbackTimeEnd())){
                    try {
                        Date start = sdf.parse(studentFeedback.getFeedbackTimeStart() + " 00:00:00");
                        Date end = sdf.parse(studentFeedback.getFeedbackTimeEnd() + " 23:59:59");
                        Predicate con=criteriaBuilder.between(root.get("feedbackTime"),start,end);
                        list.add(con);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                }
                Predicate[] p = new Predicate[list.size()];
                return  criteriaBuilder.and(list.toArray(p));
            }
        };

        return studentFeedbackRepository.findAll(specification,pageable);
    }

    @Override
    public List<FeedbackDto> getFeedbackDto(Long schoolId, String feedbackTimeStart, String feedbackTimeEnd) {
        List<FeedbackDto> result = new ArrayList<>();
        if(StringUtils.isNotEmpty(feedbackTimeStart)&&StringUtils.isNotEmpty(feedbackTimeEnd)){
            String start = feedbackTimeStart + " 00:00:00";
            String end = feedbackTimeEnd + " 23:59:59";
            List<Object[]> rawList = studentFeedbackRepository.getFeedbackDto(schoolId, start, end);
            
            // 手动转换Object[]到FeedbackDto
            for (Object[] obj : rawList) {
                FeedbackDto dto = new FeedbackDto();
                dto.setFeedbackDate(obj[0] != null ? obj[0].toString() : null);
                dto.setClassId(obj[1] != null ? Long.valueOf(obj[1].toString()) : null);
                dto.setClassName(obj[2] != null ? obj[2].toString() : null);
                dto.setNum(obj[3] != null ? Integer.valueOf(obj[3].toString()) : 0);
                result.add(dto);
            }
        }
        return result;
    }
}
