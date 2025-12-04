package com.jlm.homework.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author QingYang
 * @version 1.0
 * @description
 * @date 2023/3/8 0008
 */
@Data
public class ItemSaveDto implements Serializable {

    private static final long serialVersionUID = -3555919652163215660L;
    private Long id;

    private String titleType;


    private String content;

    private String solution;

    private String parse;

    private Integer sort;


}
