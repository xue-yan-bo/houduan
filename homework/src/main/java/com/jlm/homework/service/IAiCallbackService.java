package com.jlm.homework.service;

import com.jlm.homework.dto.AiCallbackRequest;
import com.jlm.homework.dto.AiCallbackResult;
import com.jlm.homework.dto.AiFile;

import java.util.List;

public interface IAiCallbackService {
    AiCallbackResult aicallback(AiCallbackRequest aiCallbackRequest);


}
