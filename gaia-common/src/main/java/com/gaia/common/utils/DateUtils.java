package com.gaia.common.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 日期工具类。
 *
 * <p>统一使用系统默认时区（Asia/Shanghai），所有 Redis Key 与 DB
 * 计算都基于 {@link #today()} 输出，避免跨时区错位。</p>
 */
public final class DateUtils {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    public static final String PATTERN_DATE = "yyyy-MM-dd";
    public static final String PATTERN_DATETIME = "yyyy-MM-dd HH:mm:ss";
    public static final String PATTERN_YEAR_MONTH = "yyyyMM";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_DATE);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_DATETIME);
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_YEAR_MONTH);

    private DateUtils() {
    }

    /** 当前日期，格式 yyyy-MM-dd */
    public static String today() {
        return LocalDate.now(DEFAULT_ZONE).format(DATE_FORMATTER);
    }

    /** 当前时间，格式 yyyy-MM-dd HH:mm:ss */
    public static String now() {
        return LocalDateTime.now(DEFAULT_ZONE).format(DATETIME_FORMATTER);
    }

    /** 当前年月，格式 yyyyMM */
    public static String currentYearMonth() {
        return LocalDate.now(DEFAULT_ZONE).format(YEAR_MONTH_FORMATTER);
    }

    /**
     * 计算指定日期在 BitMap 中的偏移量（从 1 开始）。
     */
    public static int dayOfMonth(LocalDate date) {
        return date.getDayOfMonth();
    }

    public static int dayOfMonth(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FORMATTER).getDayOfMonth();
    }

    public static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(DEFAULT_ZONE).toLocalDate();
    }

    public static LocalDate parse(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }
}