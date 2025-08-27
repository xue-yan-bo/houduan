package com.jlm.homework.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.core.util.Json;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class Student implements Serializable {

    private String classesCode;;
    private Long classesId;
    private String classesIds;
    private String classesName;
    private String createBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private String delFlag;

    private String enrolledData;
    private String formerName;
    private Long gradeId;
    private String gradeName;
    private String homeAddress;
    private String linkUuid;
    private String mainLinkmanName;
    private String mainLinkmanPhone;
    private String nationality;
    private String nativePlace;
    private Long orgId;
    private Integer pageNum;
    private Integer pageSize;
    private String politicsStatus;
    private String remark;
    private Long schoolId;
    private String schoolName;
    private String searchValue;
    private String sex;
    private String studentCode;
    private Long studentId;
    private String studentIdCard;
    private String studentIdCards;
    private String studentIds;
    private String studentImage;
    private String studentName;
    private String studentStatus;
    private String studentStatusNumber;
    private String studentType;
    private String updateBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    private String userId;
    private String userIds;
}
