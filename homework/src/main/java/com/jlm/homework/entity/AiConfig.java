package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

/**
 * 作业发布 实体类
 */
@Data
@Entity
@Table(name = "ai_config")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AiConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     *ai名称
     */
    @Column(name = "ai_name")
    private String aiName;

    /**
     *ai名称
     */
    @Column(name = "use_flag")
    private Integer useFlag;
}
