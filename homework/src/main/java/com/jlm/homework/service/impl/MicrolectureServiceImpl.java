package com.jlm.homework.service.impl;



import com.jlm.homework.entity.Microlecture;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.repository.IMicrolectureRepository;
import com.jlm.homework.service.IMicrolectureService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MicrolectureServiceImpl implements IMicrolectureService {
    @Resource
    private IMicrolectureRepository microlectureRepository;
    @Override
    public Page<Microlecture> selectPage(Integer pageNum, Integer pageSize, Long schoolId, String name, Long teacherId, String teacherName, Long gradeId, String gradeName, Long classId, String className, String subject, String chapter,String knowledgePoint, LocalDateTime startTime, LocalDateTime endTime) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        Pageable pageable;
        pageable = PageRequest.of(pageNum, pageSize, sort);
        Specification<Microlecture> specification = new Specification<Microlecture>() {

            @Override
            public Predicate toPredicate(Root<Microlecture> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                try {
                    if(schoolId!=null) {
                        Predicate condition = criteriaBuilder.equal(root.get("schoolId"), schoolId);
                        list.add(condition);
                    }
                    if(StringUtils.isNotEmpty(name)) {
                        Predicate condition = criteriaBuilder.like(root.get("name"), "%"+name+"%");
                        list.add(condition);
                    }
                    if(teacherId!=null) {
                        Predicate condition = criteriaBuilder.equal(root.get("teacherId"), teacherId);
                        list.add(condition);
                    }
                    if(StringUtils.isNotEmpty(teacherName)) {
                        Predicate condition = criteriaBuilder.equal(root.get("teacherName"), teacherName);
                        list.add(condition);
                    }
                    if(gradeId!=null) {
                        Predicate condition = criteriaBuilder.equal(root.get("gradeId"), gradeId);
                        list.add(condition);
                    }
                    if(StringUtils.isNotEmpty(gradeName)) {
                        Predicate condition = criteriaBuilder.equal(root.get("gradeName"), gradeName);
                        list.add(condition);
                    }
                    if(classId!=null) {
                        Predicate condition = criteriaBuilder.equal(root.get("classId"), classId);
                        list.add(condition);
                    }
                    if(StringUtils.isNotEmpty(className)) {
                        Predicate condition = criteriaBuilder.equal(root.get("className"), className);
                        list.add(condition);
                    }
                    if(StringUtils.isNotEmpty(subject)) {
                        Predicate condition = criteriaBuilder.equal(root.get("subject"), subject);
                        list.add(condition);
                    }
                    if(StringUtils.isNotEmpty(chapter)) {
                        Predicate condition1 = criteriaBuilder.like(root.get("chapter"), "%"+chapter+"%");
                        list.add(condition1);
                    }
                    if(StringUtils.isNotEmpty(knowledgePoint)) {
                        Predicate condition = criteriaBuilder.like(root.get("knowledgePoint"), "%"+knowledgePoint+"%");
                        list.add(condition);
                    }
                    if(startTime != null) {
                        Predicate condition = criteriaBuilder.greaterThan(root.get("createTime"), startTime);
                        list.add(condition);
                    }
                    if(endTime != null) {
                        Predicate condition = criteriaBuilder.lessThan(root.get("createTime"), endTime);
                        list.add(condition);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }

        };
        return microlectureRepository.findAll(specification,pageable);
    }

    @Override
    public void save(Microlecture microlecture) {
        microlectureRepository.save(microlecture);
    }

    @Override
    public void delete(Long microlectureId) {
        microlectureRepository.deleteById(microlectureId);
    }
}
