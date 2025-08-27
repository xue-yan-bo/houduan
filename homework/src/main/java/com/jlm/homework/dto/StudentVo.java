package com.jlm.homework.dto;

import com.jlm.homework.entity.Student;
import lombok.Data;

import java.io.Serializable;

@Data
public class StudentVo extends Student implements Serializable {
    private String deviceCode;
    private String openId;
    private Long deviceUserRelationId;


    public static StudentVo studentToVo(Student student) {
        StudentVo studentVo=new StudentVo();
        studentVo.setClassesCode(student.getClassesCode());
        studentVo.setClassesName(student.getClassesName());
        studentVo.setStudentId(student.getStudentId());
        studentVo.setStudentName(student.getStudentName());
        studentVo.setStudentCode(student.getStudentCode());
        studentVo.setStudentImage(student.getStudentImage());
        studentVo.setStudentIdCard(student.getStudentIdCard());
        studentVo.setStudentStatus(student.getStudentStatus());
        studentVo.setStudentIdCards(student.getStudentIdCards());
        studentVo.setStudentIds(student.getStudentIds());
        studentVo.setStudentStatusNumber(student.getStudentStatusNumber());
        studentVo.setStudentType(student.getStudentType());
        studentVo.setClassesId(student.getClassesId());
        studentVo.setClassesIds(student.getClassesIds());
        studentVo.setCreateBy(student.getCreateBy());
        studentVo.setCreateTime(student.getCreateTime());
        studentVo.setUpdateBy(student.getUpdateBy());
        studentVo.setUpdateTime(student.getUpdateTime());
        studentVo.setEnrolledData(student.getEnrolledData());
        studentVo.setFormerName(student.getFormerName());
        studentVo.setGradeId(student.getGradeId());
        studentVo.setGradeName(student.getGradeName());
        studentVo.setHomeAddress(student.getHomeAddress());
        studentVo.setLinkUuid(student.getLinkUuid());
        studentVo.setMainLinkmanName(student.getMainLinkmanName());
        studentVo.setMainLinkmanPhone(student.getMainLinkmanPhone());
        studentVo.setNationality(student.getNationality());
        studentVo.setNativePlace(student.getNativePlace());
        studentVo.setPoliticsStatus(student.getPoliticsStatus());
        studentVo.setOrgId(student.getOrgId());
        studentVo.setRemark(student.getRemark());
        studentVo.setSex(student.getSex());
        studentVo.setUserId(student.getUserId());
        studentVo.setUserIds(student.getUserIds());
        return studentVo;
    }
}
