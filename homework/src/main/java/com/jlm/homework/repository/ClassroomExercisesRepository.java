package com.jlm.homework.repository;

import com.jlm.homework.entity.ClassroomExercises;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ClassroomExercisesRepository extends JpaRepository<ClassroomExercises, Long>, JpaSpecificationExecutor<ClassroomExercises> {
}
