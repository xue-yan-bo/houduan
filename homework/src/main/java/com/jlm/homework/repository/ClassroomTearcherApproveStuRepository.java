package com.jlm.homework.repository;

import com.jlm.homework.entity.ClassroomTearcherApproveStu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassroomTearcherApproveStuRepository extends JpaRepository<ClassroomTearcherApproveStu,Long> {
    List<ClassroomTearcherApproveStu> findByStudentRecordIdIn(List<Long> studentRecordIds);
}
