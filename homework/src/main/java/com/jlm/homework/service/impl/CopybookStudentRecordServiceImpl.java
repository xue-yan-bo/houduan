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
        return copybookStudentRecordRepository.findAll(Example.of(copybookStudentRecord),pageable);
    }

    @Override
    public List<Copybook2Board> getCopybookBoards(Long studentId) {
        List<CopybookStudentRecord> studentRecords= new ArrayList<>();
        Specification<CopybookStudentRecord> specification = new Specification<CopybookStudentRecord>() {
            @Override
            public Predicate toPredicate(Root<CopybookStudentRecord> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<>();
                Predicate condition =criteriaBuilder.equal(root.get("studentId"), studentId);
                list.add(condition);
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                Date start = calendar.getTime();
                Date end = new Date();
                Predicate condition1 = criteriaBuilder.between(root.<Date>get("createTime").as(Date.class),start,end);
                list.add(condition1);
                Predicate[] p =  new Predicate[list.size()];
                return criteriaBuilder.and(list.toArray(p));
            }
        };
        studentRecords =copybookStudentRecordRepository.findAll(specification);
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
}
