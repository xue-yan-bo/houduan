package com.jlm.homework.dto;

import lombok.Data;

import java.util.List;

@Data
public class RongMsgResult {
    private String code;
    private List<MessageUID> messageUIDs;
}
