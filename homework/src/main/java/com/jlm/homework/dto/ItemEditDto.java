package com.jlm.homework.dto;


import jakarta.persistence.Column;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
@Data
public class ItemEditDto implements Serializable {
    private static final long serialVersionUID = -6102908862934554862L;

    private Long groupId;
    private String name;
    private Long studentId;
    private String studentName;
    private String generateType; 
    private List<ItemSaveDto> itemSaveDtoList;
}
