package com.jlm.homework.entity;

import lombok.*;

/**
 * 班级对象 educ_classes
 *
 * @author zhiyong
 * @date 2021-10-18
 */
@Data
@Getter
@Setter
@ToString
public class Classes
{
    private static final long serialVersionUID = 1L;

    /** 班级ID */
    private Long classesId;

    /** 班级IDS */
    private Long[] classesIds;

    /** 学校ID */
    private Long schoolId;

    /** 学期ID */
    private Long termId;

    /** 学年 */
    private Long schoolYear;

    /** 年级ID */
    private Long gradeId;
    private Long[] gradeIds;

    /** 年级名字 */
    @Setter(AccessLevel.NONE)
    private String gradeName;

    private String gradeNameOri;

    /** 班号 */
    private String code;

    /** 学校班型id */
    private Long classesTypeId;

    /** 学校班型名字 */
    private String classesTypeName;

    /** 班级名称 */
    private String name;

    /** 限招人数 */
    private Long limitedStudentCount;

    /** 建班时间 **/
    private String establishDate;

    /** 状态（0正常 1毕业） */
    private String status;

    /** 删除标志（0正常 1删除） */
    private String delFlag;

    /**
     * 班主任ID
     */
    private Long teacherId;
    /**
     * 班主任名字
     */
    private String teacherName;


    /**
     * 学生总数
     */
    private Long studentSum;
    private String establishDateStr;
    private String establishDateEnd;

    public String getGradeName(){
//        if(this.gradeId!=null){
//            List<SysDictData> sysDictData = DictUtils.getDictCache("sys_grade_lists");
//            List<SysDictData> sysDictDataList = sysDictData.stream().filter(s -> s.getDictCode().equals(this.gradeId)).collect(Collectors.toList());
//            if(sysDictDataList!=null && sysDictDataList.size()>0)
//                this.gradeName = sysDictDataList.get(0).getDictLabel();
//        }
        return this.gradeName;
    }
    public void setGradeName(String gradeName) {
        this.gradeName = gradeName;
    }

    public Classes() {
    }

    public Classes(Long schoolId, String status, String delFlag) {
        this.schoolId = schoolId;
        this.status = status;
        this.delFlag = delFlag;
    }
}
