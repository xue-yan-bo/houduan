package com.jlm.homework.service.impl;

import com.jlm.homework.dto.Copybook2Board;
import com.jlm.homework.dto.CopybookStatistics;
import com.jlm.homework.entity.Copybook;
import com.jlm.homework.entity.CopybookStudentRecord;
import com.jlm.homework.entity.CopybookStudentWriteData;
import com.jlm.homework.repository.CopybookRepository;
import com.jlm.homework.repository.CopybookStudentRecordRepository;
import com.jlm.homework.service.ICopybookStudentRecordService;
import com.jlm.homework.service.ICopybookStudentWriteDataService;
import com.jlm.homework.service.UserService;
import com.jlm.homework.util.StringUtils;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CopybookStudentRecordServiceImpl implements ICopybookStudentRecordService {
    @Resource
    private CopybookStudentRecordRepository copybookStudentRecordRepository;
    @Autowired
    private ICopybookStudentWriteDataService copybookStudentWriteDataService;
    @Autowired
    private UserService userService;

    @Override
    public CopybookStudentRecord getById(Long id) {
        CopybookStudentRecord record=copybookStudentRecordRepository.findById(id).orElse(null);
        if(record!=null){
            List<CopybookStudentWriteData>  writeDataList=copybookStudentWriteDataService.findByStudentRecordId(id);
            record.setStudentWriteDataList(writeDataList);
        }
        return record;
    }

    @Override
    public Page<CopybookStudentRecord> selectList(Integer pageNum, Integer pageSize, CopybookStudentRecord copybookStudentRecord) {
        if (pageNum == null || pageNum <= 0 || pageSize == null || pageSize <= 0) {
            pageNum = 1;
            pageSize = 10;
        }
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);
        Specification<CopybookStudentRecord> specification = new Specification<CopybookStudentRecord>() {
            @Override
            public Predicate toPredicate(Root<CopybookStudentRecord> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                if(copybookStudentRecord.getSchoolId()!=null){
                    Predicate con = criteriaBuilder.equal(root.get("schoolId"),copybookStudentRecord.getSchoolId());
                    list.add(con);
                }else {
                    Predicate con = criteriaBuilder.equal(root.get("schoolId"),userService.getCurrentSchoolId());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybookStudentRecord.getCopybookName())){
                    Predicate con = criteriaBuilder.like(root.get("copybookName"),"%"+copybookStudentRecord.getCopybookName()+"%");
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybookStudentRecord.getFont())){
                    Predicate con = criteriaBuilder.equal(root.get("font"),copybookStudentRecord.getFont());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybookStudentRecord.getFormat())){
                    Predicate con = criteriaBuilder.equal(root.get("format"),copybookStudentRecord.getFormat());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybookStudentRecord.getSubject())){
                    Predicate con = criteriaBuilder.equal(root.get("subject"),copybookStudentRecord.getSubject());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybookStudentRecord.getContent())){
                    Predicate con = criteriaBuilder.like(root.get("content"),"%"+copybookStudentRecord.getContent()+"%");
                    list.add(con);
                }
                if(copybookStudentRecord.getStudentId()!=null){
                    Predicate con = criteriaBuilder.equal(root.get("studentId"),copybookStudentRecord.getStudentId());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybookStudentRecord.getStudentName())){
                    Predicate con = criteriaBuilder.like(root.get("studentName"),"%"+copybookStudentRecord.getStudentName()+"%");
                    list.add(con);
                }
                if(copybookStudentRecord.getGradeId()!=null){
                    Predicate con = criteriaBuilder.equal(root.get("gradeId"),copybookStudentRecord.getGradeId());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybookStudentRecord.getGradeName())){
                    Predicate con = criteriaBuilder.like(root.get("gradeName"),"%"+copybookStudentRecord.getGradeName()+"%");
                    list.add(con);
                }
                if(copybookStudentRecord.getClassId()!=null){
                    Predicate con = criteriaBuilder.equal(root.get("classId"),copybookStudentRecord.getClassId());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybookStudentRecord.getClassName())){
                    Predicate con = criteriaBuilder.like(root.get("className"),"%"+copybookStudentRecord.getClassName()+"%");
                    list.add(con);
                }
                if(copybookStudentRecord.getSubmitStatus()!=null){
                    Predicate con = criteriaBuilder.equal(root.get("submitStatus"),copybookStudentRecord.getSubmitStatus());
                    list.add(con);
                }
                if(copybookStudentRecord.getCreateTime()!=null){
                    Predicate con = criteriaBuilder.between(root.get("createTime").as(Date.class),copybookStudentRecord.getCreateTime(),new Date());
                    list.add(con);
                }
                if(copybookStudentRecord.getSubmitTime()!=null){
                    Predicate con = criteriaBuilder.between(root.get("submitTime").as(Date.class),copybookStudentRecord.getSubmitTime(),new Date());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybookStudentRecord.getCreateTimeStr())){

                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String startdate = copybookStudentRecord.getCreateTimeStr() + " 00:00:00";
                        String enddate = copybookStudentRecord.getCreateTimeStr() + " 23:59:59";
                        Date startdateDate = sdf.parse(startdate);
                        Date enddateDate = sdf.parse(enddate);
                        Predicate con = criteriaBuilder.between(root.get("createTime").as(Date.class),startdateDate,enddateDate);
                        list.add(con);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                }
                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        return copybookStudentRecordRepository.findAll(Example.of(copybookStudentRecord),pageable);
    }

    @Override
    public List<Copybook2Board> getCopybookBoards(Long studentId) {
        List<CopybookStudentRecord> studentRecords= new ArrayList<>();
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Specification<CopybookStudentRecord> specification = new Specification<CopybookStudentRecord>() {
            @Override
            public Predicate toPredicate(Root<CopybookStudentRecord> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                Predicate condition =criteriaBuilder.equal(root.get("studentId"), studentId);
                list.add(condition);
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                Date start = calendar.getTime();
                calendar.setTime(new Date());
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                Date end = calendar.getTime();
                Predicate condition1 = criteriaBuilder.between(root.<Date>get("createTime").as(Date.class),start,end);
                list.add(condition1);
                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        studentRecords =copybookStudentRecordRepository.findAll(specification,sort);
        List<Copybook2Board> boardList = new ArrayList<>();
        if(!studentRecords.isEmpty()){
            for(CopybookStudentRecord record:studentRecords){
                Copybook2Board board=new Copybook2Board();
                board.setCopybookId(record.getId());
                board.setCopybookName(record.getCopybookName());
                if(StringUtils.isNotEmpty(record.getUrl())&&record.getUrl().contains(",")){
                    board.setPageSize(record.getUrl().split(",").length);
                }else {
                    board.setPageSize(1);
                }
                boardList.add(board);
            }

        }
        return boardList;
    }

    @Override
    public CopybookStudentRecord update(CopybookStudentRecord record) {
        return copybookStudentRecordRepository.save(record);
    }

    @Override
    public Page<CopybookStudentRecord> queryByCopybookId(Long copybookId, Integer pageNum, Integer pageSize, CopybookStudentRecord copybookStudentRecord) {
        if (pageNum == null || pageNum <= 0 || pageSize == null || pageSize <= 0) {
            pageNum = 1;
            pageSize = 10;
        }
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);
        Specification<CopybookStudentRecord> specification = new Specification<CopybookStudentRecord>() {
            @Override
            public Predicate toPredicate(Root<CopybookStudentRecord> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                Predicate condition = criteriaBuilder.equal(root.get("copybookId"),copybookId);
                list.add(condition);
                if(copybookStudentRecord.getSchoolId()!=null){
                    Predicate con = criteriaBuilder.equal(root.get("schoolId"),copybookStudentRecord.getSchoolId());
                    list.add(con);
                }else {
                    Predicate con = criteriaBuilder.equal(root.get("schoolId"),userService.getCurrentSchoolId());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybookStudentRecord.getCopybookName())){
                    Predicate con = criteriaBuilder.like(root.get("copybookName"),"%"+copybookStudentRecord.getCopybookName()+"%");
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybookStudentRecord.getFont())){
                    Predicate con = criteriaBuilder.equal(root.get("font"),copybookStudentRecord.getFont());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybookStudentRecord.getFormat())){
                    Predicate con = criteriaBuilder.equal(root.get("format"),copybookStudentRecord.getFormat());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybookStudentRecord.getSubject())){
                    Predicate con = criteriaBuilder.equal(root.get("subject"),copybookStudentRecord.getSubject());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybookStudentRecord.getContent())){
                    Predicate con = criteriaBuilder.like(root.get("content"),"%"+copybookStudentRecord.getContent()+"%");
                    list.add(con);
                }
                if(copybookStudentRecord.getStudentId()!=null){
                    Predicate con = criteriaBuilder.equal(root.get("studentId"),copybookStudentRecord.getStudentId());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybookStudentRecord.getStudentName())){
                    Predicate con = criteriaBuilder.like(root.get("studentName"),"%"+copybookStudentRecord.getStudentName()+"%");
                    list.add(con);
                }
                if(copybookStudentRecord.getGradeId()!=null){
                    Predicate con = criteriaBuilder.equal(root.get("gradeId"),copybookStudentRecord.getGradeId());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybookStudentRecord.getGradeName())){
                    Predicate con = criteriaBuilder.like(root.get("gradeName"),"%"+copybookStudentRecord.getGradeName()+"%");
                    list.add(con);
                }
                if(copybookStudentRecord.getClassId()!=null){
                    Predicate con = criteriaBuilder.equal(root.get("classId"),copybookStudentRecord.getClassId());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybookStudentRecord.getClassName())){
                    Predicate con = criteriaBuilder.like(root.get("className"),"%"+copybookStudentRecord.getClassName()+"%");
                    list.add(con);
                }
                if(copybookStudentRecord.getSubmitStatus()!=null){
                    Predicate con = criteriaBuilder.equal(root.get("submitStatus"),copybookStudentRecord.getSubmitStatus());
                    list.add(con);
                }
                if(copybookStudentRecord.getCreateTime()!=null){
                    Predicate con = criteriaBuilder.between(root.get("createTime").as(Date.class),copybookStudentRecord.getCreateTime(),new Date());
                    list.add(con);
                }
                if(copybookStudentRecord.getSubmitTime()!=null){
                    Predicate con = criteriaBuilder.between(root.get("submitTime").as(Date.class),copybookStudentRecord.getSubmitTime(),new Date());
                    list.add(con);
                }
                if(StringUtils.isNotEmpty(copybookStudentRecord.getCreateTimeStr())){

                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String startdate = copybookStudentRecord.getCreateTimeStr() + " 00:00:00";
                        String enddate = copybookStudentRecord.getCreateTimeStr() + " 23:59:59";
                        Date startdateDate = sdf.parse(startdate);
                        Date enddateDate = sdf.parse(enddate);
                        Predicate con = criteriaBuilder.between(root.get("createTime").as(Date.class),startdateDate,enddateDate);
                        list.add(con);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                }

                if(StringUtils.isNotEmpty(copybookStudentRecord.getSubmitTimeStr())){

                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String startdate = copybookStudentRecord.getSubmitTimeStr() + " 00:00:00";
                        String enddate = copybookStudentRecord.getSubmitTimeStr() + " 23:59:59";
                        Date startdateDate = sdf.parse(startdate);
                        Date enddateDate = sdf.parse(enddate);
                        Predicate con = criteriaBuilder.between(root.get("submitTime").as(Date.class),startdateDate,enddateDate);
                        list.add(con);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                }
                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        return copybookStudentRecordRepository.findAll(Example.of(copybookStudentRecord),pageable);
    }

    @Autowired
    private CopybookRepository copybookRepository;

    @Override
    public List<CopybookStatistics.ClassCopybookStatistics> getClassCopybookStatistics(Long classId) {
        // 查询该班级的所有学生字帖记录
        Specification<CopybookStudentRecord> specification = new Specification<CopybookStudentRecord>() {
            @Override
            public Predicate toPredicate(Root<CopybookStudentRecord> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                Predicate condition = criteriaBuilder.equal(root.get("classId"), classId);
                list.add(condition);
                Predicate[] p = new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        List<CopybookStudentRecord> records = copybookStudentRecordRepository.findAll(specification);

        // 按字帖ID分组统计
        Map<Long, List<CopybookStudentRecord>> copybookMap = records.stream()
                .collect(Collectors.groupingBy(CopybookStudentRecord::getCopybookId));

        List<CopybookStatistics.ClassCopybookStatistics> result = new ArrayList<>();
        for (Map.Entry<Long, List<CopybookStudentRecord>> entry : copybookMap.entrySet()) {
            Long copybookId = entry.getKey();
            List<CopybookStudentRecord> copybookRecords = entry.getValue();

            int totalStudents = copybookRecords.size();
            int completedStudents = (int) copybookRecords.stream()
                    .filter(record -> record.getSubmitStatus() != null && record.getSubmitStatus() == 1)
                    .count();
            double completionPercentage = totalStudents > 0 ? (double) completedStudents / totalStudents * 100 : 0;

            CopybookStatistics.ClassCopybookStatistics stats = new CopybookStatistics.ClassCopybookStatistics();
            stats.setCopybookId(copybookId);
            stats.setCopybookName(copybookRecords.get(0).getCopybookName());
            stats.setTotalStudents(totalStudents);
            stats.setCompletedStudents(completedStudents);
            stats.setCompletionPercentage(completionPercentage);
            result.add(stats);
        }

        // 按字帖创建时间倒序排序
        result.sort((s1, s2) -> {
            Copybook copybook1 = copybookRepository.findById(s1.getCopybookId()).orElse(null);
            Copybook copybook2 = copybookRepository.findById(s2.getCopybookId()).orElse(null);
            if (copybook1 == null || copybook2 == null) {
                return 0;
            }
            return copybook2.getCreateTime().compareTo(copybook1.getCreateTime());
        });

        return result;
    }

    @Override
    public CopybookStatistics.CopybookStudentStatistics getCopybookStudentStatistics(Long copybookId, Long classId) {
        // 查询该班级该字帖的所有学生记录
        Specification<CopybookStudentRecord> specification = new Specification<CopybookStudentRecord>() {
            @Override
            public Predicate toPredicate(Root<CopybookStudentRecord> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                Predicate condition1 = criteriaBuilder.equal(root.get("copybookId"), copybookId);
                Predicate condition2 = criteriaBuilder.equal(root.get("classId"), classId);
                list.add(condition1);
                list.add(condition2);
                Predicate[] p = new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        List<CopybookStudentRecord> records = copybookStudentRecordRepository.findAll(specification);

        int totalStudents = records.size();
        int completedStudents = (int) records.stream()
                .filter(record -> record.getSubmitStatus() != null && record.getSubmitStatus() == 1)
                .count();
        double completionPercentage = totalStudents > 0 ? (double) completedStudents / totalStudents * 100 : 0;

        List<CopybookStatistics.StudentCompletion> studentCompletions = new ArrayList<>();
        for (CopybookStudentRecord record : records) {
            CopybookStatistics.StudentCompletion completion = new CopybookStatistics.StudentCompletion();
            completion.setStudentId(record.getStudentId());
            completion.setStudentName(record.getStudentName());
            completion.setCompleted(record.getSubmitStatus() != null && record.getSubmitStatus() == 1);
            studentCompletions.add(completion);
        }

        CopybookStatistics.CopybookStudentStatistics stats = new CopybookStatistics.CopybookStudentStatistics();
        stats.setCopybookId(copybookId);
        if (!records.isEmpty()) {
            stats.setCopybookName(records.get(0).getCopybookName());
        }
        stats.setTotalStudents(totalStudents);
        stats.setCompletedStudents(completedStudents);
        stats.setCompletionPercentage(completionPercentage);
        stats.setStudentCompletions(studentCompletions);
        return stats;
    }
}
