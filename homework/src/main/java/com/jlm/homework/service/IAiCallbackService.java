package com.jlm.homework.service;

import com.jlm.homework.dto.AiCallbackRequest;
import com.jlm.homework.dto.AiCallbackResult;

public interface IAiCallbackService {
    AiCallbackResult aicallback(AiCallbackRequest aiCallbackRequest);
}
