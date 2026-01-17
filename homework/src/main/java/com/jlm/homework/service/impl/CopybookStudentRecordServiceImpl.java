package com.jlm.homework.service.impl;

import com.jlm.homework.dto.Copybook2Board;
import com.jlm.homework.entity.Copybook;
import com.jlm.homework.entity.CopybookStudentRecord;
import com.jlm.homework.entity.CopybookStudentWriteData;
import com.jlm.homework.repository.CopybookStudentRecordRepository;
import com.jlm.homework.service.ICopybookStudentRecordService;
import com.jlm.homework.service.ICopybookStudentWriteDataService;
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

@Service
public class CopybookStudentRecordServiceImpl implements ICopybookStudentRecordService {
    @Resource
    private CopybookStudentRecordRepository copybookStudentRecordRepository;
    @Autowired
    private ICopybookStudentWriteDataService copybookStudentWriteDataService;

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
}
