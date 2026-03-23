package com.jlm.homework.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * JPA谓词构建工具类
 */
public class PredicateBuilderUtil {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * 构建等于条件谓词
     */
    public static <T> Predicate buildEqualPredicate(CriteriaBuilder cb, Root<T> root, String field, Object value) {
        if (value == null) {
            return cb.conjunction();
        }
        return cb.equal(root.get(field), value);
    }

    /**
     * 构建模糊查询谓词
     */
    public static <T> Predicate buildLikePredicate(CriteriaBuilder cb, Root<T> root, String field, String value) {
        if (isNotEmpty(value)) {
            return cb.like(root.get(field).as(String.class), "%" + value + "%");
        }
        return cb.conjunction();
    }

    /**
     * 构建日期范围谓词（当天）
     */
    public static <T> Predicate buildDateRangePredicate(CriteriaBuilder cb, Root<T> root, String field, String dateStr) {
        if (isNotEmpty(dateStr)) {
            try {
                Date date = SDF.parse(dateStr);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                Date nextDay = calendar.getTime();
                return cb.between(root.get(field).as(Date.class), date, nextDay);
            } catch (Exception e) {
                // 日期解析失败，返回空条件
                return cb.conjunction();
            }
        }
        return cb.conjunction();
    }

    /**
     * 组合多个谓词
     */
    public static Predicate combinePredicates(CriteriaBuilder cb, Predicate... predicates) {
        if (predicates == null || predicates.length == 0) {
            return cb.conjunction();
        }
        return cb.and(predicates);
    }

    /**
     * 构建规范的谓词列表
     */
    public static <T> List<Predicate> buildPredicateList(CriteriaBuilder cb, Root<T> root) {
        return new ArrayList<>();
    }

    /**
     * 构建学生作业查询的通用谓词
     */
    public static <T> Predicate buildStudentHomeworkPredicate(
            CriteriaBuilder cb, Root<T> root, 
            Long homeworkPublishId, 
            String studentName, 
            Integer submitStatus, 
            String submitTime, 
            String auditTime, 
            String auditStatus) {

        List<Predicate> predicates = new ArrayList<>();

        // 作业发布ID
        if (homeworkPublishId != null) {
            predicates.add(cb.equal(root.get("homeworkPublishId"), homeworkPublishId));
        }

        // 学生姓名
        if (isNotEmpty(studentName)) {
            predicates.add(cb.like(root.get("studentName").as(String.class), "%" + studentName + "%"));
        }

        // 提交状态
        if (submitStatus != null) {
            predicates.add(cb.equal(root.get("submitStatus"), submitStatus));
        }

        // 提交时间
        if (isNotEmpty(submitTime)) {
            try {
                Date date = SDF.parse(submitTime);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                Date nextDay = calendar.getTime();
                predicates.add(cb.between(root.get("submitTime").as(Date.class), date, nextDay));
            } catch (Exception e) {
                // 日期解析失败，忽略
            }
        }

        // 审核时间
        if (isNotEmpty(auditTime)) {
            try {
                Date date = SDF.parse(auditTime);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                Date nextDay = calendar.getTime();
                predicates.add(cb.between(root.get("auditTime").as(Date.class), date, nextDay));
            } catch (Exception e) {
                // 日期解析失败，忽略
            }
        }

        // 审核状态
        if (isNotEmpty(auditStatus)) {
            predicates.add(cb.equal(root.get("auditStatus"), auditStatus));
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
