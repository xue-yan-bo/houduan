package com.jlm.homework.service.impl;



import com.jlm.homework.entity.Microlecture;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.feign.School;
import com.jlm.homework.repository.IMicrolectureRepository;
import com.jlm.homework.service.IMicrolectureService;
import com.jlm.homework.service.IUserService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private IUserService userService;
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
                    }/*else{
                        Predicate condition = criteriaBuilder.equal(root.get("schoolId"), userService.getCurrentSchoolIdSafely());
                        list.add(condition);
                    }*/
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
                    if(StringUtils.isEmpty(chapter)&&StringUtils.isNotEmpty(knowledgePoint)) {
                        Predicate condition = criteriaBuilder.like(root.get("knowledgePoint"), "%"+knowledgePoint+"%");
                        list.add(condition);
                    }else if(StringUtils.isNotEmpty(chapter)&&StringUtils.isNotEmpty(knowledgePoint)) {
                        List<Predicate> list1 = new ArrayList<>();
                        Predicate cond1 = criteriaBuilder.like(root.get("knowledgePoint"), "%"+knowledgePoint+"%");
                        list1.add(cond1);
                        Predicate cond2= criteriaBuilder.like(root.get("chapter"), "%"+chapter+"%");
                        list1.add(cond2);
                        if(chapter.contains("/")){
                            String chapterSub = chapter.substring(chapter.lastIndexOf("/")+1);
                            Predicate cond3= criteriaBuilder.like(root.get("chapter"), "%"+chapterSub+"%");
                            list1.add(cond3);
                            if(chapterSub.length()>4){
                                String chapterSub1 = chapterSub.substring(4);
                                Predicate cond4= criteriaBuilder.like(root.get("chapter"), "%"+chapterSub1+"%");
                                list1.add(cond4);
                            }

                        }
                        Predicate condition = criteriaBuilder.or(list1.toArray(new Predicate[0]));
                        list.add(condition);
                    }else if(StringUtils.isNotEmpty(chapter)) {
                        String chapterSub = chapter.substring(chapter.lastIndexOf("/")+1).trim();
                        Predicate condition1 = criteriaBuilder.like(root.get("chapter"), "%"+chapterSub+"%");
                        list.add(condition1);
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
        School school=userService.getCurrentSchool();
        if(school!=null&&StringUtils.isNotEmpty(school.getSchoolName())
                &&StringUtils.isEmpty(microlecture.getSchoolName())){
            microlecture.setSchoolName(school.getSchoolName());
        }
        microlectureRepository.save(microlecture);
    }

    @Override
    public void delete(Long microlectureId) {
        microlectureRepository.deleteById(microlectureId);
    }

    @Override
    public Microlecture getById(Long microlectureId) {
        return microlectureRepository.findById(microlectureId).orElse(null);
    }
}
