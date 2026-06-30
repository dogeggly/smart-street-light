package com.cqu.controller;

import com.cqu.service.IAlarmLogsService;
import com.cqu.utils.Result;
import com.cqu.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/alarm-logs")
public class AlarmLogsController {

    @Autowired
    private IAlarmLogsService alarmLogsService;

    /**
     * 告警分页列表
     */
    @GetMapping
    public Result<PageResult<AlarmLogVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) String alarmType,
            @RequestParam(required = false) String status) {
        log.info("查询告警列表: page={}, pageSize={}, deviceId={}, alarmType={}, status={}",
                page, pageSize, deviceId, alarmType, status);
        PageResult<AlarmLogVO> result = alarmLogsService.pageAlarms(page, pageSize, deviceId, alarmType, status);
        return Result.success(result);
    }

    /**
     * 告警详情
     */
    @GetMapping("/{id}")
    public Result<AlarmLogVO> detail(@PathVariable Long id) {
        log.info("查询告警详情: id={}", id);
        AlarmLogVO detail = alarmLogsService.getAlarmDetail(id);
        return Result.success(detail);
    }

    /**
     * 解决告警
     */
    @PutMapping("/{id}/resolve")
    public Result<String> resolve(@PathVariable Long id) {
        log.info("解决告警: id={}", id);
        alarmLogsService.resolveAlarm(id);
        return Result.success("处理成功");
    }

    /**
     * 告警统计
     */
    @GetMapping("/statistics")
    public Result<AlarmStatisticsVO> statistics() {
        log.info("查询告警统计");
        AlarmStatisticsVO statistics = alarmLogsService.getStatistics();
        return Result.success(statistics);
    }
}
