package com.jlm.homework.repository;

import com.jlm.homework.entity.ChapterSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChapterSectionRepository extends JpaRepository<ChapterSection, Long> {
}
