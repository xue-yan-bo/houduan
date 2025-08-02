package com.jlm.homework.service.impl;

import com.jlm.homework.entity.ChapterSection;
import com.jlm.homework.repository.ChapterSectionRepository;
import com.jlm.homework.service.IChapterSectionService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
public class ChapterSectionServiceImpl implements IChapterSectionService {
    @Resource
    private ChapterSectionRepository chapterSectionRepository;

    @Override
    public Long create(ChapterSection chapterSection) {
        return chapterSectionRepository.save(chapterSection).getId();
    }

    @Override
    public ChapterSection getById(Long id) {
        return chapterSectionRepository.findById(id).get();
    }

    @Override
    public ChapterSection update(ChapterSection chapterSection) {
        return chapterSectionRepository.save(chapterSection);
    }

    @Override
    public Page<ChapterSection> selectList(Integer pageNum, Integer pageSize, ChapterSection chapterSection) {
        pageNum = pageNum == null ? 0 : pageNum-1;
        pageSize = pageSize == null ? 10 : pageSize;
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable;
        pageable = PageRequest.of(pageNum, pageSize, sort);
        return chapterSectionRepository.findAll(Example.of(chapterSection),pageable);
    }

    @Override
    public void deleteById(Long id) {
        chapterSectionRepository.deleteById(id);
    }
}
