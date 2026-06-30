package com.cqu.controller;

import com.cqu.service.IDevicesService;
import com.cqu.vo.Result;
import com.cqu.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
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

    /**
     * 硬件状态回传（硬件执行开关指令后回传最终状态）
     */
    @PostMapping("/status-callback")
    public Result<String> statusCallback(@RequestBody Map<String, Object> body) {
        Long deviceId = body.get("deviceId") != null
                ? Long.valueOf(body.get("deviceId").toString()) : null;
        String status = (String) body.get("status");
        log.info("硬件状态回传: deviceId={}, status={}", deviceId, status);
        devicesService.updateDeviceStatus(deviceId, status);
        return Result.success("状态更新成功");
    }

    /**
     * 设备心跳上报（硬件定期发送心跳信号，响应中带回指令）
     */
    @PostMapping("/heartbeat")
    public Result<Map<String, String>> heartbeat(@RequestBody Map<String, Object> body) {
        Long deviceId = body.get("deviceId") != null
                ? Long.valueOf(body.get("deviceId").toString()) : null;
        log.info("设备心跳上报: deviceId={}", deviceId);
        devicesService.updateHeartbeat(deviceId);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("command", "NONE");
        return Result.success(response);
    }

    /**
     * 手动开关灯控制（前端下发 → 后端更新状态 → 预留硬件通知通道）
     */
    @PostMapping("/{id}/switch")
    public Result<Map<String, String>> switchDevice(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        log.info("手动开关灯: deviceId={}, status={}", id, status);
        String command = devicesService.switchDevice(id, status);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("command", command);
        return Result.success(response);
    }
}
