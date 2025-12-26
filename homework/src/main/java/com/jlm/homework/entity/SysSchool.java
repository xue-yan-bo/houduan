package com.jlm.homework.entity;

import lombok.Data;

/**
 * 学校对象
 */
@Data
public class SysSchool {

    /**学校id**/
    private Long schoolId;
    /**学校名称**/
    private String schoolName;
    /**学校类型**/
    private String schoolType;
    /**学校简称**/
    private String schoolAbbreviation;
    /**学校logo**/
    private String schoolLogo;
    /**所属区域**/
    private Long areaId;
    /**学校地址**/
    private String address;
    /**上级机构**/
    private Long orgId;
    /**联系人**/
    private String contact;
    /**联系人电话**/
    private String contactNumber;
    /**管理员id**/
    private String adminId;
    /**教育机构Id**/
    private Long educOrgId;
    /**学校状态**/
    private String status;
    /**删除状态**/
    private int delFlag;
    /**学校性质；1公立、2私立、3公参民**/
    private String nature;
    /**系统模块**/
    private String module;
    /**
     *  版本
     */
    private String version;
    /**
     *  样式
     */
    private String style;
}
