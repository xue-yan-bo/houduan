package com.jlm.homework.entity;

import lombok.Data;

@Data
public class Warn {
    /**
     * 省
     */
    private String province;
    /**
     * 市
     */
    private String city;
    /**
     * 地址详情
     */
    private String adresse;
    /**
     * 经度
     */
    private String lon;
    /**
     * 纬度
     */
    private String lat;
}
