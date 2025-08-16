package com.jlm.homework.service.impl;

import com.jlm.homework.entity.ClassroomExercisesStudentAnswer;
import com.jlm.homework.entity.ClassroomExercisesStudentRecord;
import com.jlm.homework.entity.StudentsHomeworkNew;
import com.jlm.homework.repository.ClassroomExercisesStudentRecordRepository;
import com.jlm.homework.service.IClassroomExercisesStudentRecordService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class ClassroomExercisesStudentRecordServiceImpl implements IClassroomExercisesStudentRecordService {
    @Resource
    private ClassroomExercisesStudentRecordRepository classroomExercisesStudentRecordRepository;
    @Override
    public List<ClassroomExercisesStudentRecord> selectByClassroomExercisesId(Long classroomExercisesId) {
        ClassroomExercisesStudentRecord record = new ClassroomExercisesStudentRecord();
        record.setClassroomExercisesId(classroomExercisesId);
        Sort sort = Sort.by(Sort.Direction.ASC, "createTime");
        return classroomExercisesStudentRecordRepository.findAll(Example.of(record),sort);
    }

    @Override
    public ClassroomExercisesStudentRecord save(ClassroomExercisesStudentRecord studentRecord) {
        if(studentRecord.getId()==null){
            studentRecord.setCreateTime(new Date());
        }
        return classroomExercisesStudentRecordRepository.save(studentRecord);
    }

    @Override
    public List<ClassroomExercisesStudentRecord> selectByClassroomExercisesIdAndClass(Long classroomExercisesId, Long classId) {
        ClassroomExercisesStudentRecord record = new ClassroomExercisesStudentRecord();
        record.setClassroomExercisesId(classroomExercisesId);
        record.setClassId(classId);
        Sort sort = Sort.by(Sort.Direction.ASC, "answerDuration","createTime");
        return classroomExercisesStudentRecordRepository.findAll(Example.of(record),sort);
    }

    @Override
    public void endAllAnswer(Long classroomExercisesId, Long classId) {
        Date now = new Date();
        List<ClassroomExercisesStudentRecord> recordList = new ArrayList<>();
        if(classroomExercisesId == 0){
            recordList = selectByClassAndDate(classroomExercisesId, classId,now);
        }else  {
            recordList = selectByClassroomExercisesIdAndClass(classroomExercisesId, classId);
        }
        for (ClassroomExercisesStudentRecord record : recordList) {
            if(record.getEndFlag()==null||record.getEndFlag().equals("0")){
                record.setEndFlag(1);
                record.setEndTime(now);
                if(record.getStartTime()!=null){
                    record.setAnswerDuration(now.getTime() - record.getStartTime().getTime());
                }
                classroomExercisesStudentRecordRepository.save(record);
            }
        }
    }

    public List<ClassroomExercisesStudentRecord> selectByClassAndDate(Long classroomExercisesId, Long classId, Date date) {
        ClassroomExercisesStudentRecord record = new ClassroomExercisesStudentRecord();
        record.setClassroomExercisesId(classroomExercisesId);
        record.setClassId(classId);
        Specification<ClassroomExercisesStudentRecord> specification = new Specification<ClassroomExercisesStudentRecord>() {

            @Override
            public Predicate toPredicate(Root<ClassroomExercisesStudentRecord> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list =  new ArrayList<>();

                if(classroomExercisesId!=null){
                    Predicate condition1 = criteriaBuilder.equal(root.get("classroomExercisesId"), classroomExercisesId);
                    list.add(condition1);
                }

                if(classId!=null){
                    Predicate condition2 = criteriaBuilder.equal(root.get("classId"), classId);
                    list.add(condition2);
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Calendar calendar = Calendar.getInstance();

                try {
                    Predicate condition3 = null;
                    String dateStr = sdf.format(date);
                    if(StringUtils.isNotEmpty(dateStr)){
                        Date startDate1 = sdf.parse(dateStr);
                        calendar.setTime(startDate1);
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                        Date endDate1 = calendar.getTime();
                        condition3 = criteriaBuilder.between(root.<Date>get("createTime"),startDate1,endDate1);
                        list.add(condition3);
                    }



                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };

        Sort sort = Sort.by(Sort.Direction.ASC, "answerDuration","createTime");
        return classroomExercisesStudentRecordRepository.findAll(specification,sort);
    }
}
