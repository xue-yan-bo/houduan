package com.jlm.homework.repository;

import com.jlm.homework.entity.ClassroomTearcherApproveStu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassroomTearcherApproveStuRepository extends JpaRepository<ClassroomTearcherApproveStu,Long> {
}
