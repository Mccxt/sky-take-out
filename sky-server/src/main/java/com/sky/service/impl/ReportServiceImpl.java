package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    /*
    统计指定时间区间内的营业额数据
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放从 begin到end范围内的每天的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)){
            //日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);  //plusDays(1) 每次都 new 一个新对象
            dateList.add(begin);
        }
        // 存放每天的营业额
        List<Double> turnoverList = new ArrayList<>();
        for(LocalDate date : dateList){
            //查询date日期对应的营业额数据，营业额是指：状态为“已完成” 的订单金额合计
            LocalDateTime beginTime= LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);
            Map map = new HashMap<>();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover; //
            turnoverList.add(turnover);
        }
        // 数据封装返回结果
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    /*
    统计指定时间内的用户数据
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放从 begin到end范围内的每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (! begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //存放每天的新增用户数量
        List<Integer> newUserList = new ArrayList<>();
        //存放每天的用户总数量
        List<Integer> totalUserList = new ArrayList<>();
        for (LocalDate date : dateList){
            LocalDateTime beginTime= LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);
            Map map = new HashMap<>();
            map.put("end",endTime);
            //总用户数量
            Integer totalUser = userMapper.countByMap(map);
            totalUserList.add(totalUser);
            map.put("begin",beginTime);
            //新增用户数量
            Integer newUser = userMapper.countByMap(map);
            newUserList.add(newUser);
        }
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .build();
    }

    /*
    统计指定时间内的订单数据
     */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放从 begin到end范围内的每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (! begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        // 遍历 dateList集合，查询每天的有效订单数和订单总数
        for(LocalDate date : dateList){
            LocalDateTime beginTime= LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);
            // 查询每天的订单总数
             orderCountList.add(getOrderCount(beginTime, endTime, null));
            // 查询每天的订单有效数量
             validOrderCountList.add(getOrderCount(beginTime, endTime, Orders.COMPLETED));
        }
        // 计算时间区间内的订单总数, 有效订单总量
        Integer totalOrderCount = orderCountList.stream().reduce(Integer :: sum).get();
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer :: sum).get();
        //订单完成率
        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0){ // 订单数为零时不能除
            orderCompletionRate = validOrderCount.doubleValue()/totalOrderCount;
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }/*
    根据条件统计订单数量
     */
    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end,Integer status){
        Map map = new HashMap<>();
        map.put("begin",begin);
        map.put("end",end);
        map.put("status",status);
        return orderMapper.countByMap(map);
    }

    /*
    统计指定时间区间内的销量排名top10
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {

        LocalDateTime beginTime= LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end,LocalTime.MAX);
        List<GoodsSalesDTO> salesTop = orderMapper.getSalesTop(beginTime, endTime);
        List<String> names = salesTop.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String name = StringUtils.join(names, ",");
        List<Integer> numbers = salesTop.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String number = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO.builder()
                .nameList(name)
                .numberList(number)
                .build();
    }

    /*
    导出近30天运行数据报表
     */
    public void exportBusinessData(HttpServletResponse response) {
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);
        //查询数据报表,提供给excel模板
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        if(inputStream == null){
            throw new RuntimeException("Excel 模板未找到：template/运营数据报表模板.xlsx");
        }
        try {
            //基于提供好的模板对象创建一个新的excel表格
            XSSFWorkbook excel = new XSSFWorkbook(inputStream);
            //获得excel文件中的一个Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");
            sheet.getRow(1).getCell(1).setCellValue(begin + "至" + end);
            //获得第四行
            XSSFRow row = sheet.getRow(3);
            //获取单元格
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());

            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getUnitPrice());
            for(int i = 0; i< 30;i++){
                LocalDate date = begin.plusDays(i);
                //准备明细数据
                businessData = workspaceService.getBusinessData(LocalDateTime.of(date,LocalTime.MIN),LocalDateTime.of(date,LocalTime.MAX));
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }
            //通过输出文件流见文件下载到客户端浏览器中
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            //关闭连接
            out.flush();
            out.close();
            excel.close();
        } catch (IOException e) {
            //throw new RuntimeException(e);
            e.printStackTrace();
        }
    }
}
