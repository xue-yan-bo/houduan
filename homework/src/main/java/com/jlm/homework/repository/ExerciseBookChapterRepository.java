package com.jlm.homework.repository;

import com.jlm.homework.entity.ExerciseBookChapter;
import org.hibernate.annotations.processing.SQL;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Repository
public interface ExerciseBookChapterRepository extends JpaRepository<ExerciseBookChapter,Long> {
    @Modifying
    @Query(value = "select * from exercise_book_chapter c  WHERE c.chapter_name like CONCAT('%',1?,'%') ", nativeQuery = true)
    List<ExerciseBookChapter> findChapterByName(String chapter);
}
