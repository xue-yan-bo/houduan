package com.jlm.homework.repository;

import com.jlm.homework.entity.Copybook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CopybookRepository extends JpaRepository<Copybook, Long> {
}
