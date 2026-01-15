package com.jlm.homework.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class Copybook2Board implements Serializable {
    private Long copybookId;
    private String copybookName;
    private Integer pageSize;
}
