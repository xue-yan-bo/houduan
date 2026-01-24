package com.jlm.homework.service.impl;

import com.jlm.homework.dto.Result;
import com.jlm.homework.dto.ResultDto;
import com.jlm.homework.entity.*;
import com.jlm.homework.feign.School;
import com.jlm.homework.feign.SchoolFeignClient;
import com.jlm.homework.feign.StudentFeignClient;
import com.jlm.homework.repository.IMicrolectureRepository;
import com.jlm.homework.repository.IStudentMicrolectureRepository;
import com.jlm.homework.service.IMicroPurchaseService;
import com.jlm.homework.service.IMicrolectureService;
import com.jlm.homework.service.IStudentMicrolectureService;
import com.jlm.homework.service.UserService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
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
    private IMicrolectureRepository microlectureRepository;
    @Autowired
    private StudentFeignClient studentFeignClient;
    @Autowired
    private SchoolFeignClient schoolFeignClient;
    @Autowired
    private UserService userService;
    @Autowired
    private IMicroPurchaseService microPurchaseService;
   /* @Override
    public void createStudentMicrolecture(Microlecture microlecture) {
        StudentMicrolecture search = new StudentMicrolecture();
        search.setMicrolectureId(microlecture.getId());
        List<StudentMicrolecture>  list=studentMicrolectureRepository.findAll(Example.of(search));
        if(list==null||list.size()>0){
            return;
        }
        School school=userService.getCurrentSchool();
        if(school!=null&&StringUtils.isNotEmpty(school.getSchoolName())
                &&StringUtils.isEmpty(microlecture.getSchoolName())){
            microlecture.setSchoolName(school.getSchoolName());
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
                studentMicrolecture.setKnowledgePoint(microlecture.getKnowledgePoint());
                studentMicrolecture.setSubject(microlecture.getSubject());
                studentMicrolecture.setSchoolId(microlecture.getSchoolId());
                studentMicrolecture.setSchoolName(microlecture.getSchoolName());
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
    }*/

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
    public Page<StudentMicrolecture> page(Integer pageNum, Integer pageSize, Long microlectureId, String microlectureName, Long studentId ,String subject,String teacherName, String chapter,String knowledgePoint,Integer searchType) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        Pageable pageable;
        pageable = PageRequest.of(pageNum, pageSize, sort);
        Long classId = null;
        if(searchType!=0){
            if(studentId==null) {
                studentId = userService.getCurrentUserId();
            }
            ResultDto<Student> studentResultDto=studentFeignClient.getStudentInfo(studentId);
            if(studentResultDto!=null&&studentResultDto.getData()!=null){
                Student student = studentResultDto.getData();
                classId = student.getClassesId();
            }
        }
        List<MicroPurchase> microPurchaseList;
        Long schoolId = userService.getCurrentSchoolIdSafely();
        ResultDto<SysSchool> schoolR = schoolFeignClient.getInfo(schoolId);
        boolean hxyFlag;
        if(schoolR!=null&&schoolR.getData()!=null){
            SysSchool sysSchool = schoolR.getData();
            if("huaxiayuan".equals(sysSchool.getStyle())){
                microPurchaseList = microPurchaseService.listByStudentId(studentId);
                hxyFlag = true;
            } else {
                hxyFlag = false;
                microPurchaseList = null;
            }
        } else {
            hxyFlag = false;
            microPurchaseList = null;
        }
        List<String> subjectList = new ArrayList<>();
        List<Long> gradeList = new ArrayList<>();

        if(hxyFlag&&(microPurchaseList==null||microPurchaseList.isEmpty())){
            return null;
        }else if(hxyFlag){
            Date now =new Date();
            boolean isOver = true;
            for(MicroPurchase micro:microPurchaseList) {
                if(micro.getStartDate()==null&&micro.getEndDate()==null){
                    if(micro.getSubject().contains(",")){
                        subjectList.addAll(Arrays.asList(micro.getSubject().split(",")));
                    }else {
                        subjectList.add(micro.getSubject());
                    }
                    gradeList.add(micro.getMicroGradeId());
                    isOver = false;
                }else if(micro.getStartDate()!=null&&micro.getEndDate()!=null&&
                        micro.getStartDate().before(now)&&micro.getEndDate().after(now)){
                    if(micro.getSubject().contains(",")){
                        subjectList.addAll(Arrays.asList(micro.getSubject().split(",")));
                    }else {
                        subjectList.add(micro.getSubject());
                    }
                    gradeList.add(micro.getMicroGradeId());
                    isOver = false;
                }
            }
            if(isOver){
                return null;
            }
        }
        //暂时华夏园不区分，m默认false
        //hxyFlag = false;
        Long finalClassId = classId;
        boolean finalHxyFlag = hxyFlag;
        Specification<Microlecture> specification1 = new Specification<Microlecture>() {

            @Override
            public Predicate toPredicate(Root<Microlecture> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                try {
                    if(finalClassId != null){
                        Predicate condition = criteriaBuilder.equal(root.get("classId"), finalClassId);
                        list.add(condition);
                    }
                    if(StringUtils.isNotEmpty(microlectureName)) {
                        Predicate condition = criteriaBuilder.like(root.get("name"), "%"+microlectureName+"%");
                        list.add(condition);
                    }
                    if(StringUtils.isNotEmpty(subject)) {
                        Predicate condition = criteriaBuilder.like(root.get("subject"), "%"+subject+"%");
                        list.add(condition);
                    }
                    if(StringUtils.isNotEmpty(teacherName)) {
                        Predicate condition = criteriaBuilder.like(root.get("teacherName"), "%"+teacherName+"%");
                        list.add(condition);
                    }
                    if(StringUtils.isEmpty(chapter)&&StringUtils.isNotEmpty(knowledgePoint)) {
                        List<Predicate> list1 = new ArrayList<>();
                        Predicate condition = criteriaBuilder.like(root.get("knowledgePoint"), "%"+knowledgePoint+"%");
                        list1.add(condition);
                        if(knowledgePoint.length()>2) {
                            Predicate condition1 = criteriaBuilder.like(root.get("knowledgePoint"), "%" + knowledgePoint.substring(2) + "%");
                            list1.add(condition1);
                        }
                        if(knowledgePoint.length()>4) {
                            Predicate condition1 = criteriaBuilder.like(root.get("knowledgePoint"), "%" + knowledgePoint.substring(4) + "%");
                            list1.add(condition1);
                        }
                        Predicate condit = criteriaBuilder.or(list1.toArray(new Predicate[0]));
                        list.add(condit);
                    }else if(StringUtils.isNotEmpty(chapter)&&StringUtils.isNotEmpty(knowledgePoint)) {
                        List<Predicate> list1 = new ArrayList<>();
                        Predicate cond1 = criteriaBuilder.like(root.get("knowledgePoint"), "%"+knowledgePoint+"%");
                        list1.add(cond1);
                        if(knowledgePoint.length()>2) {
                            Predicate condition1 = criteriaBuilder.like(root.get("knowledgePoint"), "%" + knowledgePoint.substring(2) + "%");
                            list1.add(condition1);
                        }
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
                        List<Predicate> list1 = new ArrayList<>();
                        String chapterSub = chapter.substring(chapter.lastIndexOf("/")+1).trim();
                        Predicate condition1 = criteriaBuilder.like(root.get("chapter"), "%"+chapterSub+"%");
                        list1.add(condition1);
                        if(chapterSub.length()>4){
                            String chapterSub1 = chapterSub.substring(4);
                            Predicate cond4= criteriaBuilder.like(root.get("chapter"), "%"+chapterSub1+"%");
                            list1.add(cond4);
                        }
                        Predicate condit = criteriaBuilder.or(list1.toArray(new Predicate[0]));
                        list.add(condit);
                    }
                    if(finalHxyFlag){
                        if(subjectList.size()>0){
                            Predicate condition = criteriaBuilder.in(root.get("subject")).value(subjectList);
                            list.add(condition);
                        }
                        if(gradeList.size()>0){
                            Predicate condition = criteriaBuilder.in(root.get("gradeId")).value(gradeList);
                            list.add(condition);
                        }
                    }
                    /*Predicate condition = criteriaBuilder.equal(root.get("schoolId"), userService.getCurrentSchoolIdSafely());
                    list.add(condition);*/

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }

        };
        Page<Microlecture> microlecturePage = microlectureRepository.findAll(specification1,pageable);
        List<Microlecture> microlectures = microlecturePage.getContent();
        List<StudentMicrolecture> studentMicrolectureList = new ArrayList<>();
        for(int i=0;i<microlectures.size()&&i<pageSize;i++){
            Microlecture microlecture = microlectures.get(i);
            StudentMicrolecture studentMicrolecture = new StudentMicrolecture();
            BeanUtils.copyProperties(microlecture,studentMicrolecture);
            studentMicrolecture.setId(null);
            studentMicrolecture.setMicrolectureId(microlecture.getId());
            studentMicrolecture.setStudentId(studentId);
            studentMicrolecture.setMicrolectureName(microlecture.getName());
            studentMicrolectureList.add(studentMicrolecture);
        }
        Page<StudentMicrolecture> page2 = new PageImpl<>(studentMicrolectureList,pageable,microlecturePage.getTotalElements());
        return page2;

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
                    /*Predicate condit = criteriaBuilder.equal(root.get("schoolId"), userService.getCurrentSchoolIdSafely());
                    list.add(condit);*/

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

    @Override
    public void deleteByMicrolectureId(Long microlectureId) {
        StudentMicrolecture deleteM = new  StudentMicrolecture();
        deleteM.setMicrolectureId(microlectureId);
        studentMicrolectureRepository.delete(deleteM);
    }

    @Override
    public List<StudentMicrolecture> findByMicrolectureId(Long microlectureId) {
        StudentMicrolecture search = new  StudentMicrolecture();
        search.setMicrolectureId(microlectureId);
        return studentMicrolectureRepository.findAll(Example.of(search));

    }

    @Override
    public void deleteById(Long id) {
        studentMicrolectureRepository.deleteById(id);
    }

    @Override
    public StudentMicrolecture createStudMicroOne(Microlecture microlecture, Long studentId) {
        StudentMicrolecture search = new StudentMicrolecture();
        search.setMicrolectureId(microlecture.getId());
        search.setStudentId(studentId);
        StudentMicrolecture studMicro = studentMicrolectureRepository.findOne(Example.of(search)).orElse(null);
        if(studMicro!=null){
            return studMicro;
        }
        ResultDto<Student> resultDto= studentFeignClient.getStudentInfo(studentId);
        if(resultDto!=null&&resultDto.getData()!=null){
            Student student = resultDto.getData();
            StudentMicrolecture studentMicrolecture=new StudentMicrolecture();
            studentMicrolecture.setSchoolName(student.getSchoolName());
            studentMicrolecture.setStudentId(student.getStudentId());
            studentMicrolecture.setStudentName(student.getStudentName());
            studentMicrolecture.setChapter(microlecture.getChapter());
            studentMicrolecture.setKnowledgePoint(microlecture.getKnowledgePoint());
            studentMicrolecture.setSubject(microlecture.getSubject());
            studentMicrolecture.setSchoolId(microlecture.getSchoolId());
            studentMicrolecture.setSchoolName(microlecture.getSchoolName());
            studentMicrolecture.setGradeId(microlecture.getGradeId());
            studentMicrolecture.setGradeName(microlecture.getGradeName());
            studentMicrolecture.setClassId(microlecture.getClassId());
            studentMicrolecture.setClassName(microlecture.getClassName());

            studentMicrolecture.setMicrolectureId(microlecture.getId());
            studentMicrolecture.setMicrolectureName(microlecture.getName());
            studentMicrolecture.setFileUrl(microlecture.getFileUrl());
            studentMicrolecture.setStatus(0);
            studentMicrolecture.setCreateTime(new Date());
            studentMicrolecture = studentMicrolectureRepository.save(studentMicrolecture);
            return studentMicrolecture;
        }
        return null;
    }
}
