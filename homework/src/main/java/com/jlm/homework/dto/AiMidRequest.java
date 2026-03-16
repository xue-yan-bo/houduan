package com.jlm.homework.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiMidRequest {
    private String requestId;
    private String role;
    private String mode;
    private String provider;
    private List<AiFile> file_list;
    private String prompt;

    private AiCallbackConfig callback;
}
