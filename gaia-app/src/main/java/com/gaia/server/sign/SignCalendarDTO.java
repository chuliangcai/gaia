package com.gaia.server.sign;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 签到日历。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignCalendarDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 年月 yyyyMM */
    private String yearMonth;
    /** 已签到的日期集合（yyyy-MM-dd） */
    private List<String> signedDates;
    /** 当月总天数 */
    private int daysInMonth;
}
