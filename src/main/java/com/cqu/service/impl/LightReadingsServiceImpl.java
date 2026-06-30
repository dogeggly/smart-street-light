package com.cqu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cqu.entity.Devices;
import com.cqu.entity.LightReadings;
import com.cqu.entity.ThresholdConfig;
import com.cqu.mapper.DevicesMapper;
import com.cqu.mapper.LightReadingsMapper;
import com.cqu.mapper.ThresholdConfigMapper;
import com.cqu.service.IControlLogsService;
import com.cqu.service.ILightReadingsService;
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
 * 光照强度采集记录（时序数据） 服务实现类
 * </p>
 *
 * @author
 * @since 2026-06-29
 */
@Slf4j
@Service
public class LightReadingsServiceImpl extends ServiceImpl<LightReadingsMapper, LightReadings> implements ILightReadingsService {

    @Autowired
    private DevicesMapper devicesMapper;

    @Autowired
    private ThresholdConfigMapper thresholdConfigMapper;

    @Autowired
    private IControlLogsService controlLogsService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public PageResult<LightReadingsVO> pageReadings(int page, int pageSize, Long deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<LightReadings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(deviceId != null, LightReadings::getDeviceId, deviceId);
        wrapper.ge(startTime != null, LightReadings::getCreatedAt, startTime);
        wrapper.le(endTime != null, LightReadings::getCreatedAt, endTime);
        wrapper.orderByDesc(LightReadings::getCreatedAt);

        Page<LightReadings> pageResult = this.page(new Page<>(page, pageSize), wrapper);

        // 批量获取设备名称
        Map<Long, String> deviceNameMap = buildDeviceNameMap(pageResult.getRecords());

        List<LightReadingsVO> records = pageResult.getRecords().stream()
                .map(r -> toLightReadingsVO(r, deviceNameMap.get(r.getDeviceId())))
                .collect(Collectors.toList());

        return PageResult.of(pageResult.getTotal(), records);
    }

    @Override
    public LatestLightVO getLatestLight(Long deviceId) {
        LambdaQueryWrapper<LightReadings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LightReadings::getDeviceId, deviceId)
                .orderByDesc(LightReadings::getCreatedAt)
                .last("LIMIT 1");
        LightReadings reading = this.getOne(wrapper);

        if (reading == null) {
            throw new RuntimeException("该设备暂无光照数据");
        }

        return LatestLightVO.builder()
                .deviceId(reading.getDeviceId())
                .lightIntensity(reading.getLightIntensity())
                .createdAt(reading.getCreatedAt())
                .build();
    }

    @Override
    public List<TrendPointVO> getTrend(Long deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<LightReadings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LightReadings::getDeviceId, deviceId)
                .ge(startTime != null, LightReadings::getCreatedAt, startTime)
                .le(endTime != null, LightReadings::getCreatedAt, endTime)
                .orderByAsc(LightReadings::getCreatedAt);

        List<LightReadings> list = this.list(wrapper);

        return list.stream()
                .map(r -> TrendPointVO.builder()
                        .time(r.getCreatedAt())
                        .value(r.getLightIntensity())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public String reportReading(Long deviceId, BigDecimal lightIntensity) {
        if (lightIntensity == null) {
            throw new RuntimeException("光照强度不能为空");
        }

        // 光照上报即视为心跳，刷新设备在线状态
        Devices device = devicesMapper.selectById(deviceId);
        if (device != null) {
            boolean wasOffline = !"ONLINE".equals(device.getOnlineStatus());
            device.setOnlineStatus("ONLINE");
            device.setLastHeartbeatTime(LocalDateTime.now());
            devicesMapper.updateById(device);

            if (wasOffline) {
                // 首次光照 → 设备上线，推送 /topic/device-online
                Map<String, Object> onlineData = new LinkedHashMap<>();
                onlineData.put("deviceId", deviceId);
                onlineData.put("deviceName", device.getDeviceName());
                onlineData.put("onlineStatus", "ONLINE");
                onlineData.put("lastHeartbeatTime", device.getLastHeartbeatTime());
                WebSocketMessage onlineMsg = WebSocketMessage.builder()
                        .type("DEVICE_ONLINE_STATUS_CHANGED")
                        .timestamp(LocalDateTime.now())
                        .data(onlineData)
                        .build();
                messagingTemplate.convertAndSend("/topic/device-online", onlineMsg);
            }
        }

        LightReadings reading = new LightReadings();
        reading.setDeviceId(deviceId);
        reading.setLightIntensity(lightIntensity);
        this.save(reading);

        // WebSocket 推送光照数据到前端
        LatestLightVO vo = LatestLightVO.builder()
                .deviceId(reading.getDeviceId())
                .lightIntensity(reading.getLightIntensity())
                .createdAt(reading.getCreatedAt())
                .build();
        WebSocketMessage msg = WebSocketMessage.builder()
                .type("LIGHT_REPORTED")
                .timestamp(LocalDateTime.now())
                .data(vo)
                .build();
        messagingTemplate.convertAndSend("/topic/light-readings", msg);

        // 光照阈值自动开关灯判定，返回下发给硬件的指令
        return checkAndAutoControl(deviceId, lightIntensity);
    }

    /**
     * 光照阈值自动开关灯判定（事件驱动：光照数据来一条判一条）
     * 光照 < 开灯阈值 → 自动开灯；光照 > 关灯阈值 → 自动关灯
     */
    private String checkAndAutoControl(Long deviceId, BigDecimal lightIntensity) {
        // 获取设备当前状态
        Devices device = devicesMapper.selectById(deviceId);
        if (device == null) {
            log.warn("自动开关判定：设备 {} 不存在", deviceId);
            return "NONE";
        }

        // 获取阈值配置
        ThresholdConfig config = thresholdConfigMapper.selectById(1L);
        if (config == null) {
            log.warn("自动开关判定：阈值配置不存在");
            return "NONE";
        }

        BigDecimal thresholdOn = config.getLightThresholdOn();
        BigDecimal thresholdOff = config.getLightThresholdOff();

        String currentStatus = device.getStatus();
        String targetStatus = null;
        String command = null;

        // 光照低于开灯阈值 → 自动开灯
        if (lightIntensity.compareTo(thresholdOn) < 0 && !"ON".equals(currentStatus)) {
            targetStatus = "ON";
            command = "AUTO_ON";
        }
        // 光照高于关灯阈值 → 自动关灯
        else if (lightIntensity.compareTo(thresholdOff) > 0 && !"OFF".equals(currentStatus)) {
            targetStatus = "OFF";
            command = "AUTO_OFF";
        }

        if (targetStatus == null) {
            return "NONE"; // 无需操作
        }

        // 更新设备开关状态
        String oldStatus = currentStatus;
        device.setStatus(targetStatus);
        devicesMapper.updateById(device);

        // 记录控制日志（自动控制）
        controlLogsService.recordLog(deviceId, command, "SUCCESS", "AUTO");

        log.info("自动开关: 设备 {} 光照={}, {} → {}", deviceId, lightIntensity, oldStatus, targetStatus);

        // WebSocket 推送设备状态变更
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceId", deviceId);
        data.put("deviceName", device.getDeviceName());
        data.put("oldStatus", oldStatus);
        data.put("status", targetStatus);
        WebSocketMessage msg = WebSocketMessage.builder()
                .type("DEVICE_STATUS_CHANGED")
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
        messagingTemplate.convertAndSend("/topic/device-status", msg);

        return command;
    }

    /**
     * 从光照记录列表中提取所有设备ID，批量查询设备名称
     */
    private Map<Long, String> buildDeviceNameMap(List<LightReadings> readings) {
        List<Long> deviceIds = readings.stream()
                .map(LightReadings::getDeviceId)
                .distinct()
                .collect(Collectors.toList());

        if (deviceIds.isEmpty()) {
            return Map.of();
        }

        return devicesMapper.selectBatchIds(deviceIds).stream()
                .collect(Collectors.toMap(Devices::getId, Devices::getDeviceName));
    }

    private LightReadingsVO toLightReadingsVO(LightReadings reading, String deviceName) {
        return LightReadingsVO.builder()
                .id(reading.getId())
                .deviceId(reading.getDeviceId())
                .deviceName(deviceName)
                .lightIntensity(reading.getLightIntensity())
                .createdAt(reading.getCreatedAt())
                .build();
    }
}
