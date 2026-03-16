package com.jlm.homework.service;

import com.jlm.homework.dto.AIMidDto;
import com.jlm.homework.dto.AiFile;
import com.jlm.homework.dto.AiPollStatusDto;

import java.util.List;

public interface IAiMidService {
    AIMidDto apiReview(String businessId, List<AiFile> fileList, String businessType,String prompt);

    AiPollStatusDto getStatusByTask(String taskId);
}
