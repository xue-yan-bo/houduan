package com.jlm.homework.repository;


import com.jlm.homework.entity.Microlecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IMicrolectureRepository extends JpaRepository<Microlecture,Long> , JpaSpecificationExecutor<Microlecture> {
}
