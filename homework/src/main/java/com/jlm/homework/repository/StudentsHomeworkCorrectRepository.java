package com.jlm.homework.repository;

import com.jlm.homework.entity.StudentsHomeworkCorrect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentsHomeworkCorrectRepository extends JpaRepository<StudentsHomeworkCorrect, Long> {
    List<StudentsHomeworkCorrect> findByStudentsHomeworkIdInAndType(List<Long> studentsHomeworkIds, Integer type);
}
