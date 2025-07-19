package com.jlm.homework.repository;

import com.jlm.homework.entity.HomeworkPublish;
import com.jlm.homework.entity.StudentsHomeworkNew;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface StudentsHomeworkNewRepository extends JpaRepository<StudentsHomeworkNew, Long>, JpaSpecificationExecutor<StudentsHomeworkNew> {

}
