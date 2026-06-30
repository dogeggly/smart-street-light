package com.cqu.controller;

import com.cqu.service.IDevicesService;
import com.cqu.utils.Result;
import com.cqu.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/devices")
public class DevicesController {

    @Autowired
    private IDevicesService devicesService;

    /**
     * 设备分页列表
     */
    @GetMapping
    public Result<PageResult<DeviceVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String onlineStatus) {
        log.info("查询设备列表: page={}, pageSize={}, deviceName={}, status={}, onlineStatus={}",
                page, pageSize, deviceName, status, onlineStatus);
        PageResult<DeviceVO> result = devicesService.pageDevices(page, pageSize, deviceName, status, onlineStatus);
        return Result.success(result);
    }

    /**
     * 设备详情
     */
    @GetMapping("/{id}")
    public Result<DeviceDetailVO> detail(@PathVariable Long id) {
        log.info("查询设备详情: id={}", id);
        DeviceDetailVO detail = devicesService.getDeviceDetail(id);
        return Result.success(detail);
    }

    /**
     * 添加设备
     */
    @PostMapping
    public Result<String> add(@RequestBody Map<String, Object> body) {
        String deviceName = (String) body.get("deviceName");
        String deviceSn = (String) body.get("deviceSn");
        log.info("添加设备: deviceName={}, deviceSn={}", deviceName, deviceSn);
        devicesService.addDevice(deviceName, deviceSn);
        return Result.success("添加成功");
    }

    /**
     * 编辑设备
     */
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String deviceName = (String) body.get("deviceName");
        log.info("编辑设备: id={}, deviceName={}", id, deviceName);
        devicesService.updateDevice(id, deviceName);
        return Result.success("修改成功");
    }

    /**
     * 删除设备
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除设备: id={}", id);
        devicesService.deleteDevice(id);
        return Result.success("删除成功");
    }

    /**
     * 设备概览统计
     */
    @GetMapping("/statistics")
    public Result<DeviceStatisticsVO> statistics() {
        log.info("查询设备概览统计");
        DeviceStatisticsVO statistics = devicesService.getStatistics();
        return Result.success(statistics);
    }
}
