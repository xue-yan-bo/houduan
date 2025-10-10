package com.jlm.homework.repository;


import com.jlm.homework.entity.StudentFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentFeedbackRepository  extends JpaRepository<StudentFeedback, Long> {
}
