package com.cqu.controller;

import com.cqu.service.ILightReadingsService;
import com.cqu.vo.Result;
import com.cqu.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/light-readings")
public class LightReadingsController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ILightReadingsService lightReadingsService;

    /**
     * 光照记录分页列表
     */
    @GetMapping
    public Result<PageResult<LightReadingsVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        log.info("查询光照记录: page={}, pageSize={}, deviceId={}, startTime={}, endTime={}",
                page, pageSize, deviceId, startTime, endTime);

        LocalDateTime start = parseTime(startTime);
        LocalDateTime end = parseTime(endTime);

        PageResult<LightReadingsVO> result = lightReadingsService.pageReadings(page, pageSize, deviceId, start, end);
        return Result.success(result);
    }

    /**
     * 设备最新光照
     */
    @GetMapping("/latest/{deviceId}")
    public Result<LatestLightVO> latest(@PathVariable Long deviceId) {
        log.info("查询设备最新光照: deviceId={}", deviceId);
        LatestLightVO latest = lightReadingsService.getLatestLight(deviceId);
        return Result.success(latest);
    }

    /**
     * 历史光照趋势
     */
    @GetMapping("/trend")
    public Result<List<TrendPointVO>> trend(
            @RequestParam Long deviceId,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        log.info("查询光照趋势: deviceId={}, startTime={}, endTime={}", deviceId, startTime, endTime);

        LocalDateTime start = LocalDateTime.parse(startTime, FORMATTER);
        LocalDateTime end = LocalDateTime.parse(endTime, FORMATTER);

        List<TrendPointVO> trend = lightReadingsService.getTrend(deviceId, start, end);
        return Result.success(trend);
    }

    /**
     * 光照数据上报
     */
    @PostMapping
    public Result<String> report(@RequestBody Map<String, Object> body) {
        Long deviceId = body.get("deviceId") != null
                ? Long.valueOf(body.get("deviceId").toString()) : null;
        BigDecimal lightIntensity = body.get("lightIntensity") != null
                ? new BigDecimal(body.get("lightIntensity").toString()) : null;
        log.info("光照数据上报: deviceId={}, lightIntensity={}", deviceId, lightIntensity);
        lightReadingsService.reportReading(deviceId, lightIntensity);
        return Result.success("上报成功");
    }

    private LocalDateTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(timeStr, FORMATTER);
    }
}
