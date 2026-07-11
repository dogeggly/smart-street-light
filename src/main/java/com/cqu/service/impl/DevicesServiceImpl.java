package com.cqu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cqu.entity.AlarmLogs;
import com.cqu.entity.Devices;
import com.cqu.entity.LightReadings;
import com.cqu.mapper.AlarmLogsMapper;
import com.cqu.mapper.DevicesMapper;
import com.cqu.mapper.LightReadingsMapper;
import com.cqu.config.MqttConfig;
import com.cqu.service.IControlLogsService;
import com.cqu.service.IDevicesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqu.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 路灯设备表 服务实现类
 * </p>
 *
 * @author
 * @since 2026-06-29
 */
@Slf4j
@Service
public class DevicesServiceImpl extends ServiceImpl<DevicesMapper, Devices> implements IDevicesService {

    @Autowired
    private LightReadingsMapper lightReadingsMapper;

    @Autowired
    private AlarmLogsMapper alarmLogsMapper;

    @Autowired
    private IControlLogsService controlLogsService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MqttConfig mqttConfig;

    @Override
    public PageResult<DeviceVO> pageDevices(int page, int pageSize, String deviceName, String status, String onlineStatus) {
        LambdaQueryWrapper<Devices> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(deviceName != null && !deviceName.isBlank(), Devices::getDeviceName, deviceName);
        wrapper.eq(status != null && !status.isBlank(), Devices::getStatus, status);
        wrapper.eq(onlineStatus != null && !onlineStatus.isBlank(), Devices::getOnlineStatus, onlineStatus);
        wrapper.orderByDesc(Devices::getCreatedAt);

        Page<Devices> pageResult = this.page(new Page<>(page, pageSize), wrapper);

        List<DeviceVO> records = pageResult.getRecords().stream()
                .map(this::toDeviceVO)
                .collect(Collectors.toList());

        return PageResult.of(pageResult.getTotal(), records);
    }

    @Override
    public DeviceDetailVO getDeviceDetail(Long id) {
        Devices device = this.getById(id);
        if (device == null) {
            throw new RuntimeException("设备不存在");
        }

        // 查询最新光照值
        LambdaQueryWrapper<LightReadings> lightWrapper = new LambdaQueryWrapper<>();
        lightWrapper.eq(LightReadings::getDeviceId, id)
                .orderByDesc(LightReadings::getCreatedAt)
                .last("LIMIT 1");
        LightReadings latestLight = lightReadingsMapper.selectOne(lightWrapper);

        // 查询活跃告警数
        LambdaQueryWrapper<AlarmLogs> alarmWrapper = new LambdaQueryWrapper<>();
        alarmWrapper.eq(AlarmLogs::getDeviceId, id)
                .eq(AlarmLogs::getStatus, "ACTIVE");
        Long activeAlarmCount = alarmLogsMapper.selectCount(alarmWrapper);

        return DeviceDetailVO.builder()
                .id(String.valueOf(device.getId()))
                .deviceName(device.getDeviceName())
                .deviceSn(device.getDeviceSn())
                .status(device.getStatus())
                .onlineStatus(device.getOnlineStatus())
                .lastHeartbeatTime(device.getLastHeartbeatTime())
                .latestLightIntensity(latestLight != null ? latestLight.getLightIntensity() : null)
                .activeAlarmCount(String.valueOf(activeAlarmCount))
                .createdAt(device.getCreatedAt())
                .build();
    }

    @Override
    public void addDevice(String deviceName, String deviceSn) {
        if (deviceName == null || deviceName.isBlank()) {
            throw new RuntimeException("设备名称不能为空");
        }
        if (deviceSn == null || deviceSn.isBlank()) {
            throw new RuntimeException("设备序列号不能为空");
        }

        // 检查序列号是否已存在
        LambdaQueryWrapper<Devices> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Devices::getDeviceSn, deviceSn);
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("设备序列号已存在");
        }

        Devices device = new Devices();
        device.setDeviceName(deviceName);
        device.setDeviceSn(deviceSn);
        this.save(device);
        controlLogsService.recordLog(device.getId(), "ADD_DEVICE", "SUCCESS");
    }

    @Override
    public void updateDevice(Long id, String deviceName) {
        if (deviceName == null || deviceName.isBlank()) {
            throw new RuntimeException("设备名称不能为空");
        }

        Devices device = this.getById(id);
        if (device == null) {
            throw new RuntimeException("设备不存在");
        }

        device.setDeviceName(deviceName);
        this.updateById(device);
        controlLogsService.recordLog(id, "UPDATE_DEVICE", "SUCCESS");
    }

    @Override
    public void deleteDevice(Long id) {
        Devices device = this.getById(id);
        if (device == null) {
            throw new RuntimeException("设备不存在");
        }

        // 删除关联的光照记录
        LambdaQueryWrapper<LightReadings> lightWrapper = new LambdaQueryWrapper<>();
        lightWrapper.eq(LightReadings::getDeviceId, id);
        lightReadingsMapper.delete(lightWrapper);

        // 删除关联的告警日志
        LambdaQueryWrapper<AlarmLogs> alarmWrapper = new LambdaQueryWrapper<>();
        alarmWrapper.eq(AlarmLogs::getDeviceId, id);
        alarmLogsMapper.delete(alarmWrapper);

        this.removeById(id);
        controlLogsService.recordLog(id, "DELETE_DEVICE", "SUCCESS");
    }

    @Override
    public DeviceStatisticsVO getStatistics() {
        Long totalCount = this.count();
        Long onlineCount = this.lambdaQuery().eq(Devices::getOnlineStatus, "ONLINE").count();
        Long offlineCount = this.lambdaQuery().eq(Devices::getOnlineStatus, "OFFLINE").count();
        Long onCount = this.lambdaQuery().eq(Devices::getStatus, "ON").count();
        Long offCount = this.lambdaQuery().eq(Devices::getStatus, "OFF").count();

        return DeviceStatisticsVO.builder()
                .totalCount(String.valueOf(totalCount))
                .onlineCount(String.valueOf(onlineCount))
                .offlineCount(String.valueOf(offlineCount))
                .onCount(String.valueOf(onCount))
                .offCount(String.valueOf(offCount))
                .build();
    }

    @Override
    public void updateDeviceStatus(Long deviceId, String status) {
        if (deviceId == null || status == null || status.isBlank()) {
            throw new RuntimeException("设备ID和状态不能为空");
        }
        if (!"ON".equals(status) && !"OFF".equals(status)) {
            throw new RuntimeException("状态值只能为 ON 或 OFF");
        }

        Devices device = this.getById(deviceId);
        if (device == null) {
            throw new RuntimeException("设备不存在");
        }

        String oldStatus = device.getStatus();
        device.setStatus(status);
        this.updateById(device);

        // 记录控制日志（硬件回传，来源为 AUTO）
        controlLogsService.recordLog(deviceId, "STATUS_CALLBACK", "SUCCESS", "AUTO");

        // WebSocket 推送设备状态变更
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceId", deviceId);
        data.put("deviceName", device.getDeviceName());
        data.put("oldStatus", oldStatus);
        data.put("status", status);
        WebSocketMessage msg = WebSocketMessage.builder()
                .type("DEVICE_STATUS_CHANGED")
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
        log.info("WebSocket 推送 → /topic/device-status: 设备 {} ({}) 硬件回传状态 {} → {}", deviceId, device.getDeviceName(), oldStatus, status);
        messagingTemplate.convertAndSend("/topic/device-status", msg);
    }

    @Override
    public void updateHeartbeat(Long deviceId) {
        if (deviceId == null) {
            throw new RuntimeException("设备ID不能为空");
        }

        Devices device = this.getById(deviceId);
        if (device == null) {
            throw new RuntimeException("设备不存在");
        }

        boolean wasOffline = !"ONLINE".equals(device.getOnlineStatus());

        device.setOnlineStatus("ONLINE");
        device.setLastHeartbeatTime(LocalDateTime.now());
        this.updateById(device);

        // 超时离线检测已由 HeartbeatCheckTask 定时任务实现（每30秒扫描）

        // WebSocket 推送在线状态变更（仅状态变更时推送：从离线恢复到在线）
        if (wasOffline) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("deviceId", deviceId);
            data.put("deviceName", device.getDeviceName());
            data.put("onlineStatus", "ONLINE");
            data.put("lastHeartbeatTime", device.getLastHeartbeatTime());
            WebSocketMessage msg = WebSocketMessage.builder()
                    .type("DEVICE_ONLINE_STATUS_CHANGED")
                    .timestamp(LocalDateTime.now())
                    .data(data)
                    .build();
            log.info("WebSocket 推送 → /topic/device-online: 设备 {} ({}) 上线（心跳恢复触发）", deviceId, device.getDeviceName());
            messagingTemplate.convertAndSend("/topic/device-online", msg);
        }
    }

    @Override
    public String switchDevice(Long deviceId, String status) {
        if (deviceId == null || status == null || status.isBlank()) {
            throw new RuntimeException("设备ID和状态不能为空");
        }
        if (!"ON".equals(status) && !"OFF".equals(status)) {
            throw new RuntimeException("状态值只能为 ON 或 OFF");
        }

        Devices device = this.getById(deviceId);
        if (device == null) {
            throw new RuntimeException("设备不存在");
        }

        String oldStatus = device.getStatus();
        device.setStatus(status);
        this.updateById(device);

        // 记录控制日志（手动控制）
        String command = "MANUAL_" + status;
        controlLogsService.recordLog(deviceId, command, "SUCCESS", "MANUAL");

        // WebSocket 推送设备状态变更
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceId", deviceId);
        data.put("deviceName", device.getDeviceName());
        data.put("oldStatus", oldStatus);
        data.put("status", status);
        WebSocketMessage msg = WebSocketMessage.builder()
                .type("DEVICE_STATUS_CHANGED")
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
        log.info("WebSocket 推送 → /topic/device-status: 设备 {} ({}) 手动开关 {} → {}", deviceId, device.getDeviceName(), oldStatus, status);
        messagingTemplate.convertAndSend("/topic/device-status", msg);

        // 通过 MQTT 下发开关指令给硬件
        mqttConfig.publishCommand(device.getDeviceSn(), command);

        return command;
    }

    private DeviceVO toDeviceVO(Devices device) {
        return DeviceVO.builder()
                .id(String.valueOf(device.getId()))
                .deviceName(device.getDeviceName())
                .deviceSn(device.getDeviceSn())
                .status(device.getStatus())
                .onlineStatus(device.getOnlineStatus())
                .lastHeartbeatTime(device.getLastHeartbeatTime())
                .createdAt(device.getCreatedAt())
                .build();
    }
}
