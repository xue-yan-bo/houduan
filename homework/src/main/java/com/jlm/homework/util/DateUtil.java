package com.jlm.homework.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 解析日期字符串（yyyy-MM-dd）
     */
    public static Date parseDate(String dateStr) throws ParseException {
        return DATE_FORMAT.parse(dateStr);
    }
    
    /**
     * 解析日期时间字符串（yyyy-MM-dd HH:mm:ss）
     */
    public static Date parseDateTime(String dateTimeStr) throws ParseException {
        return DATETIME_FORMAT.parse(dateTimeStr);
    }
    
    /**
     * 格式化日期为字符串（yyyy-MM-dd）
     */
    public static String formatDate(Date date) {
        return DATE_FORMAT.format(date);
    }
    
    /**
     * 格式化日期时间为字符串（yyyy-MM-dd HH:mm:ss）
     */
    public static String formatDateTime(Date date) {
        return DATETIME_FORMAT.format(date);
    }
    
    /**
     * 获取日期的开始时间（00:00:00）
     */
    public static Date getStartOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
    
    /**
     * 获取日期的结束时间（23:59:59）
     */
    public static Date getEndOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }
    
    /**
     * 获取日期范围（开始时间和结束时间）
     */
    public static Date[] getDateRange(String startDateStr, String endDateStr) throws ParseException {
        Date startDate = parseDate(startDateStr);
        Date endDate = parseDate(endDateStr);
        
        Date start = getStartOfDay(startDate);
        Date end = getEndOfDay(endDate);
        
        return new Date[]{start, end};
    }
    
    /**
     * 获取日期加一天
     */
    public static Date addOneDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTime();
    }
}