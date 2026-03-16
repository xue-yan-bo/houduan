package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "ai_callback")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AiCallback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     *业务Id
     */
    @Column(name = "business_id")
    private String businessId;   //'业务Id'
    /**
     *业务类型，作业批改、随堂检测
     */
    @Column(name = "business_type")
    private String businessType; //'业务类型，作业批改、随堂检测'
    /**
     *中台任务id
     */
    @Column(name = "ai_task_id")
    private String aiTaskId;     //'中台任务id'
    /**
     *大模型返回结果
     */
    @Column(name = "ai_result")
    private String aiResult;      //'大模型返回结果'


    /**
     *处理结果，0处理成功，1处理失败
     */
    @Column(name = "status")
    private Integer status;
    /**
     *处理信息
     */
    @Column(name = "msg")
    private String msg;
    /**
     *处理时间
     */
    @Column(name = "deal_time")
    private Date dealTime;
    /**
     *创建时间
     */
    @Column(name = "create_time")
    private Date createTime;

}
