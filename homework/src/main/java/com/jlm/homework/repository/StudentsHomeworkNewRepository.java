package com.jlm.homework.repository;

import com.jlm.homework.entity.HomeworkPublish;
import com.jlm.homework.entity.StudentsHomeworkNew;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentsHomeworkNewRepository extends JpaRepository<StudentsHomeworkNew, Long> {

}
