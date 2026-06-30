package com.cqu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cqu.entity.AlarmLogs;
import com.cqu.entity.Devices;
import com.cqu.entity.LightReadings;
import com.cqu.mapper.AlarmLogsMapper;
import com.cqu.mapper.DevicesMapper;
import com.cqu.mapper.LightReadingsMapper;
import com.cqu.service.IDevicesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqu.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 路灯设备表 服务实现类
 * </p>
 *
 * @author
 * @since 2026-06-29
 */
@Service
public class DevicesServiceImpl extends ServiceImpl<DevicesMapper, Devices> implements IDevicesService {

    @Autowired
    private LightReadingsMapper lightReadingsMapper;

    @Autowired
    private AlarmLogsMapper alarmLogsMapper;

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
                .id(device.getId())
                .deviceName(device.getDeviceName())
                .deviceSn(device.getDeviceSn())
                .status(device.getStatus())
                .onlineStatus(device.getOnlineStatus())
                .lastHeartbeatTime(device.getLastHeartbeatTime())
                .latestLightIntensity(latestLight != null ? latestLight.getLightIntensity() : null)
                .activeAlarmCount(activeAlarmCount)
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
    }

    @Override
    public DeviceStatisticsVO getStatistics() {
        Long totalCount = this.count();
        Long onlineCount = this.lambdaQuery().eq(Devices::getOnlineStatus, "ONLINE").count();
        Long offlineCount = this.lambdaQuery().eq(Devices::getOnlineStatus, "OFFLINE").count();
        Long onCount = this.lambdaQuery().eq(Devices::getStatus, "ON").count();
        Long offCount = this.lambdaQuery().eq(Devices::getStatus, "OFF").count();

        return DeviceStatisticsVO.builder()
                .totalCount(totalCount)
                .onlineCount(onlineCount)
                .offlineCount(offlineCount)
                .onCount(onCount)
                .offCount(offCount)
                .build();
    }

    private DeviceVO toDeviceVO(Devices device) {
        return DeviceVO.builder()
                .id(device.getId())
                .deviceName(device.getDeviceName())
                .deviceSn(device.getDeviceSn())
                .status(device.getStatus())
                .onlineStatus(device.getOnlineStatus())
                .lastHeartbeatTime(device.getLastHeartbeatTime())
                .createdAt(device.getCreatedAt())
                .build();
    }
}
