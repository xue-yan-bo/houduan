package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author zzq
 * @version 1.0
 * @interfaceName ItemEntity
 * @description 题目实体
 * @date 2023年2月28日 17:51:55
 */
@Data
@Entity
@Table(name = "wrong_group_item")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class WrongGroupItem implements Serializable {

    private static final long serialVersionUID = -3827020083273065637L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "wrong_group_id")
    private Long wrongGroupId;

    @Column(name = "title_type")
    private String titleType;


    @Column(name = "content")
    private String content;


    @Column(name = "solution")
    private String solution;


    @Column(name = "parse")
    private String parse;


    @Column(name = "sort")
    private Integer sort;

    @JsonIgnore
    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @JsonIgnore
    @Column(name = "create_time")
    private LocalDateTime createTime;

}
