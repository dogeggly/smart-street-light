package com.cqu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cqu.entity.Devices;
import com.cqu.entity.LightReadings;
import com.cqu.mapper.DevicesMapper;
import com.cqu.mapper.LightReadingsMapper;
import com.cqu.service.ILightReadingsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqu.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
@Service
public class LightReadingsServiceImpl extends ServiceImpl<LightReadingsMapper, LightReadings> implements ILightReadingsService {

    @Autowired
    private DevicesMapper devicesMapper;

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
    public void reportReading(Long deviceId, BigDecimal lightIntensity) {
        if (lightIntensity == null) {
            throw new RuntimeException("光照强度不能为空");
        }

        LightReadings reading = new LightReadings();
        reading.setDeviceId(deviceId);
        reading.setLightIntensity(lightIntensity);
        this.save(reading);
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
