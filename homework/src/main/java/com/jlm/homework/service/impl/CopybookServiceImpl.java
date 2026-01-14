package com.jlm.homework.service.impl;

import com.jlm.homework.repository.CopybookRepository;
import com.jlm.homework.service.ICopybookService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class CopybookServiceImpl implements ICopybookService {
    @Resource
    private CopybookRepository copybookRepository;
}
