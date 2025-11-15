package com.jlm.homework.repository;


import com.jlm.homework.entity.StudentMicrolecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IStudentMicrolectureRepository extends JpaRepository<StudentMicrolecture,Long>, JpaSpecificationExecutor<StudentMicrolecture> {
}
