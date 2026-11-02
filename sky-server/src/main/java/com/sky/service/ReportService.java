package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import java.time.LocalDate;

public interface ReportService {

    /*
    统计指定时间区间内的营业额数据
     */
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);

    /*
    用户数据统计
     */
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);

    /*
    订单数据统计
     */
    OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end);
}
