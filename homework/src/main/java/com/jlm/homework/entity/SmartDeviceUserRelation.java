package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

/**
 * 智能设备用户关系 实体类
 */
@Data
@Entity
@Table(name = "smart_device_user_relation")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SmartDeviceUserRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     *用户用户ID
     */
    @Column(name = "user_id")
    private String userId;
    /**
     *用户名称
     */
    @Column(name = "user_name")
    private String userName;
    /**
     *设备编码
     */
    @Column(name = "device_code")
    private String deviceCode;
    /**
     *用户身份，1学生、2老师、3家长
     */
    @Column(name = "user_type")
    private Integer userType=1;
    /**
     *ip地址
     */
    @Column(name = "ip_address")
    private String ipAddress;
}
