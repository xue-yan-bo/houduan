package com.jlm.homework.service;

import com.jlm.homework.dto.Copybook2Board;
import com.jlm.homework.entity.Copybook;
import com.jlm.homework.entity.CopybookStudentRecord;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ICopybookStudentRecordService {
    CopybookStudentRecord getById(Long id);

    Page<CopybookStudentRecord> selectList(Integer pageNum, Integer pageSize, CopybookStudentRecord copybook);

    List<Copybook2Board> getCopybookBoards(Long studentId);

    CopybookStudentRecord update(CopybookStudentRecord record);

    Page<CopybookStudentRecord> queryByCopybookId(Long copybookId, Integer pageNum, Integer pageSize, CopybookStudentRecord copybook);
}
