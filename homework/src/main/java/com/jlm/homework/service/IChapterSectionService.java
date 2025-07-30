package com.jlm.homework.service;

import com.jlm.homework.entity.ChapterSection;
import org.springframework.data.domain.Page;

public interface IChapterSectionService {
    Long create(ChapterSection chapterSection);

    ChapterSection getById(Long id);

    ChapterSection update(ChapterSection chapterSection);

    Page<ChapterSection> selectList(Integer pageNum, Integer pageSize, ChapterSection chapterSection);

    void deleteById(Long id);
}
